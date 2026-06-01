package com.biddingagency.domain.opportunity.service;

import com.biddingagency.domain.opportunity.NoticeTypeTranslator;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import com.biddingagency.integration.llmplatform.LLMPlatformClient;
import com.biddingagency.integration.samgov.client.SAMGovApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * 원본 공고 본문(description) 한글 번역 (CR-022 재구현).
 *
 * - 자동: 수집 시 NEW 행에 대해 비동기 트리거 (translateAsync)
 * - 수동: 관리자 "한글 번역하기" 버튼이 호출 (translate, 동기)
 * - 실패 시: 영문 fallback (DB에 null 유지). 예외 안 던지고 false 반환.
 *
 * 본문 출처:
 *  1) raw_json.description 이 URL 형태(`https://api.sam.gov/.../noticedesc?...`)면 SAMGovApiClient.fetchNoticeDescription 호출
 *  2) 평문이면 그대로 사용
 *  3) HTML 태그가 박혀있으면 평문화 후 LLM에 전달
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpportunityTranslationService {

    private final OpportunityRepository opportunityRepository;
    private final LLMPlatformClient llmPlatformClient;
    private final SAMGovApiClient samGovApiClient;

    /**
     * 비동기 본문 번역 — 수집 NEW 행에 대해 호출.
     * 별도 스레드에서 실행하므로 수집 흐름을 막지 않음.
     */
    @Async("llmTaskExecutor")
    public void translateAsync(UUID opportunityId) {
        translate(opportunityId);
    }

    /**
     * 동기 번역 — 관리자 "한글 번역하기" 버튼이 호출. 제목 + 본문 둘 다 시도.
     * - 제목 번역: LLM 호출 1회. 실패는 본문 번역 시도와 무관.
     * - 본문 번역: SAM noticedesc fetch + LLM. 실패/빈 본문이면 skip.
     * @return 둘 중 하나라도 성공하면 true.
     */
    @Transactional
    public boolean translate(UUID opportunityId) {
        Opportunity opp = opportunityRepository.findById(opportunityId).orElse(null);
        if (opp == null) {
            log.warn("[CR-022] 번역: 공고 없음 id={}", opportunityId);
            return false;
        }
        // type 한글 라벨은 코드 매핑(LLM 불필요) — 비어있으면 채움
        if (opp.getTypeKo() == null || opp.getTypeKo().isBlank()) {
            opp.applyTypeKo(NoticeTypeTranslator.toKorean(opp.getType()));
        }
        boolean titleOk = translateTitleOf(opp);
        boolean descOk = translateDescriptionOf(opp);
        return titleOk || descOk;
    }

    /** 제목 번역 — 1회 시도. 실패해도 예외 안 던짐. 이미 한글이면 skip. */
    private boolean translateTitleOf(Opportunity opp) {
        if (opp.getTitleKo() != null && !opp.getTitleKo().isBlank()) {
            log.info("[CR-022] 제목 이미 번역됨 — skip noticeId={}", opp.getNoticeId());
            return false;
        }
        try {
            String titleKo = llmPlatformClient.translateOpportunityTitle(opp.getTitle(), opp.getOrganizationName());
            if (titleKo == null || titleKo.isBlank()) return false;
            opp.applyTitleTranslation(titleKo);
            log.info("[CR-022] 제목 번역 완료 noticeId={}, titleKo={}", opp.getNoticeId(), titleKo);
            return true;
        } catch (RuntimeException e) {
            log.warn("[CR-022] 제목 번역 실패 noticeId={}: {}", opp.getNoticeId(), e.getMessage());
            return false;
        }
    }

    /** 본문 번역 — noticedesc fetch + LLM. 빈 본문이면 skip. */
    private boolean translateDescriptionOf(Opportunity opp) {
        String body = resolveDescription(opp);
        if (body == null || body.isBlank()) {
            log.info("[CR-022] 본문 없음 — 번역 skip noticeId={}", opp.getNoticeId());
            return false;
        }
        try {
            String translated = llmPlatformClient.translateOpportunityDescription(opp.getTitle(), body);
            if (translated == null || translated.isBlank()) return false;
            opp.applyDescriptionTranslation(translated);
            log.info("[CR-022] 본문 번역 완료 noticeId={}, len={}", opp.getNoticeId(), translated.length());
            return true;
        } catch (RuntimeException e) {
            log.warn("[CR-022] 본문 번역 실패 noticeId={}: {}", opp.getNoticeId(), e.getMessage());
            return false;
        }
    }

    /**
     * raw_json.description에서 원문 본문을 결정한다.
     * URL이면 SAM noticedesc API 호출 후 본문 받아옴, 평문이면 그대로.
     * HTML 태그/연속 공백은 정리.
     */
    private String resolveDescription(Opportunity opp) {
        Map<String, Object> raw = opp.getRawJson();
        if (raw == null) return null;
        Object desc = raw.get("description");
        if (!(desc instanceof String s) || s.isBlank()) return null;

        String body;
        if (s.startsWith("http://") || s.startsWith("https://")) {
            body = samGovApiClient.fetchNoticeDescription(s);
        } else {
            body = s;
        }
        return stripHtml(body);
    }

    /** HTML 태그 단순 제거 + 연속 공백 정리. 빈 결과면 null. */
    private String stripHtml(String s) {
        if (s == null) return null;
        String plain = s.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
        return plain.isEmpty() ? null : plain;
    }
}
