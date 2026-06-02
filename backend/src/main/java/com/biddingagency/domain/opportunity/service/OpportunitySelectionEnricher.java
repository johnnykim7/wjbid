package com.biddingagency.domain.opportunity.service;

import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import com.biddingagency.integration.samgov.client.SAMGovApiClient;
import com.biddingagency.integration.samgov.dto.SAMOpportunityResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * CR-022 2차: 원본공고 선별용 정보 풍부화.
 *
 * 동기:
 *  - SAM type → typeKo (SamTypeKoMapper, 기존 createOrUpdate에서 이미 처리)
 *  - setAside → set_aside_ko
 *  - NAICS → naics_label_ko
 *  - award.amount → award_amount
 *  - officeAddress / placeOfPerformance → place_of_performance_short
 *
 * 비동기 (별도 LLM 큐):
 *  - title → titleKo (CR-022 1차 OpportunityTranslationService)
 *  - description URL → noticedesc fetch → 한 줄 요약 → description_summary_ko
 *
 * 호출측: OpportunityCollectorService가 NEW upsert 직후 enrich(opp, data) 호출.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpportunitySelectionEnricher {

    private final OpportunityRepository opportunityRepository;
    private final SamSetAsideKoMapper setAsideMapper;
    private final NaicsTopLevelKoMapper naicsMapper;
    private final SAMGovApiClient samGovApiClient;
    private final OpportunityTranslationService translationService;

    /**
     * 수집 동기 단계 — 코드 매핑·단순 추출만 한 트랜잭션에 반영.
     * 본문 fetch + LLM은 별도 async에서.
     */
    @Transactional
    public void applyMeta(UUID opportunityId, SAMOpportunityResponse.OpportunityData data) {
        Opportunity opp = opportunityRepository.findById(opportunityId).orElse(null);
        if (opp == null) return;

        String setAsideKo = setAsideMapper.toKo(data.getTypeOfSetAsideDescription());
        String naicsLabelKo = naicsMapper.toKo(data.getNaicsCode());
        String awardAmount = extractAwardAmount(data.getAward());
        String placeShort = extractPlaceShort(data.getPlaceOfPerformance(), data.getOfficeAddress());

        opp.applySelectionMeta(awardAmount, placeShort, setAsideKo, naicsLabelKo);
    }

    /**
     * 비동기 단계 — title 번역 + description 요약을 트리거.
     * NEW 행에 대해서만 호출.
     */
    @Async("llmTaskExecutor")
    public void enrichAsync(UUID opportunityId, String descriptionRaw) {
        // 1) title 번역 (CR-022 1차) — translateTitleAsync는 별도 @Async라 이미 백그라운드. 여기선 동기 호출(이 메서드 자체가 async).
        try {
            translationService.translateTitle(opportunityId);
        } catch (Exception e) {
            log.warn("[CR-022-2] 제목 번역 비동기 실패 id={}: {}", opportunityId, e.getMessage());
        }

        // 2) 본문 요약 — descriptionRaw가 URL이면 noticedesc fetch, 평문이면 그대로
        String body = resolveBody(descriptionRaw);
        if (body == null || body.isBlank()) {
            log.info("[CR-022-2] 본문 없음 — 요약 skip id={}", opportunityId);
            return;
        }
        try {
            translationService.summarizeDescription(opportunityId, body);
        } catch (Exception e) {
            log.warn("[CR-022-2] 본문 요약 실패 id={}: {}", opportunityId, e.getMessage());
        }
    }

    private String resolveBody(String descriptionRaw) {
        if (descriptionRaw == null || descriptionRaw.isBlank()) return null;
        if (descriptionRaw.startsWith("http://") || descriptionRaw.startsWith("https://")) {
            String fetched = samGovApiClient.fetchNoticeDescription(descriptionRaw);
            return stripHtml(fetched);
        }
        return stripHtml(descriptionRaw);
    }

    /** HTML 태그 단순 제거 + 연속 공백 정리 */
    private String stripHtml(String s) {
        if (s == null) return null;
        String plain = s.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
        return plain.isEmpty() ? null : plain;
    }

    /** award.amount → "$443,466.20" 같은 표시 문자열 */
    private String extractAwardAmount(Map<String, Object> award) {
        if (award == null) return null;
        Object amt = award.get("amount");
        if (amt == null) return null;
        try {
            double v = Double.parseDouble(String.valueOf(amt));
            return String.format("$%,.2f", v);
        } catch (NumberFormatException e) {
            return "$" + amt;
        }
    }

    /** placeOfPerformance.city.name / .state.name 또는 officeAddress.city/state */
    private String extractPlaceShort(Map<String, Object> pop, Map<String, Object> office) {
        String city = null;
        String state = null;
        if (pop != null) {
            city = mapName(pop.get("city"));
            state = mapName(pop.get("state"));
        }
        if ((city == null || city.isBlank()) && office != null) {
            Object oc = office.get("city");
            Object os = office.get("state");
            if (oc instanceof String s) city = s;
            if (os instanceof String s) state = s;
        }
        boolean hasCity = city != null && !city.isBlank();
        boolean hasState = state != null && !state.isBlank();
        if (hasCity && hasState) return city + ", " + state;
        if (hasCity) return city;
        if (hasState) return state;
        return null;
    }

    /** SAM 응답이 {city: {name: "..."}} 형태 vs city: "..." 양쪽 모두 대응 */
    private String mapName(Object node) {
        if (node == null) return null;
        if (node instanceof String s) return s;
        if (node instanceof Map<?, ?> m) {
            Object name = m.get("name");
            return name instanceof String s ? s : null;
        }
        return null;
    }
}
