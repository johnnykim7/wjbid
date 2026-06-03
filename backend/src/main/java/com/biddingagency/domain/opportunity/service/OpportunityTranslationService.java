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
     * 비동기 자동 번역 — 수집 NEW 행에 대해 호출. 별도 스레드라 수집 흐름을 막지 않음.
     * CR-035: 제목(+type 라벨)만 번역한다. 본문(noticedesc) fetch는 SAM 쿼터를 추가로 소진하므로
     * 수집 시점에 일괄 호출하지 않고, 관리자가 상세에서 "한글 번역하기" 누를 때 그 1건만 fetch한다.
     */
    @Async("llmTaskExecutor")
    public void translateAsync(UUID opportunityId) {
        Opportunity opp = opportunityRepository.findById(opportunityId).orElse(null);
        if (opp == null) {
            log.warn("[CR-022] 자동번역: 공고 없음 id={}", opportunityId);
            return;
        }
        // type 한글 라벨은 코드 매핑(LLM 불필요) — 비어있으면 채움
        if (opp.getTypeKo() == null || opp.getTypeKo().isBlank()) {
            opp.applyTypeKo(NoticeTypeTranslator.toKorean(opp.getType()));
        }
        translateTitleOf(opp);
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

    /** CR-022 2차: 제목만 번역 (Enricher 비동기 단계가 호출). id로 조회 후 제목 번역 1회. */
    @Transactional
    public void translateTitle(UUID opportunityId) {
        Opportunity opp = opportunityRepository.findById(opportunityId).orElse(null);
        if (opp == null) return;
        translateTitleOf(opp);
    }

    /**
     * CR-038: 공고문 만들기(한글화 WF) 직전 호출 — descriptionBody(원문 본문)가 비어있으면
     * noticedesc fetch해 채운다. 번역 흐름과 독립적으로 본문을 보장(번역 안 돌린 공고도 WF가 본문 받게).
     * 이미 채워져 있으면 SAM 쿼터 소진 없이 즉시 반환. 실패해도 예외 안 던짐(WF는 폴백으로 진행).
     * REQUIRES_NEW: 호출자(generateAsync)가 클래스레벨 readOnly 트랜잭션이라, 독립 쓰기 트랜잭션으로
     * 커밋해야 dirty checking flush가 되고 직후 재조회가 보강된 본문을 읽는다.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void ensureDescriptionBody(UUID opportunityId) {
        Opportunity opp = opportunityRepository.findById(opportunityId).orElse(null);
        if (opp == null) return;
        if (opp.getDescriptionBody() != null && !opp.getDescriptionBody().isBlank()) return;
        try {
            String body = resolveDescription(opp);
            if (body == null || body.isBlank()) {
                log.info("[CR-038] descriptionBody 보강 — 본문 없음(skip) noticeId={}", opp.getNoticeId());
                return;
            }
            opp.applyDescriptionBody(body);
            log.info("[CR-038] descriptionBody 보강 완료 noticeId={}, len={}", opp.getNoticeId(), body.length());
        } catch (RuntimeException e) {
            log.warn("[CR-038] descriptionBody 보강 실패(무시) noticeId={}: {}", opp.getNoticeId(), e.getMessage());
        }
    }

    /** CR-022 2차: 본문 요약 (Enricher 비동기 단계가 호출). 평문 body를 받아 LLM 요약 후 반영. */
    @Transactional
    public void summarizeDescription(UUID opportunityId, String body) {
        if (body == null || body.isBlank()) return;
        Opportunity opp = opportunityRepository.findById(opportunityId).orElse(null);
        if (opp == null) return;
        // CR-032: LLM 번역 성패와 무관하게 원문 본문을 먼저 저장(관리자 상세 노출용)
        opp.applyDescriptionBody(body);
        try {
            String translated = llmPlatformClient.translateOpportunityDescription(opp.getTitle(), body);
            if (translated == null || translated.isBlank()) return;
            opp.applyDescriptionTranslation(translated);
            log.info("[CR-022-2] 본문 요약 완료 noticeId={}, len={}", opp.getNoticeId(), translated.length());
        } catch (RuntimeException e) {
            log.warn("[CR-022-2] 본문 요약 실패 noticeId={}: {}", opp.getNoticeId(), e.getMessage());
        }
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

    /** 본문 번역 — noticedesc fetch + 원문 본문 저장 + LLM 번역. 빈 본문이면 skip. */
    private boolean translateDescriptionOf(Opportunity opp) {
        String body = resolveDescription(opp);
        if (body == null || body.isBlank()) {
            log.info("[CR-022] 본문 없음 — 번역 skip noticeId={}", opp.getNoticeId());
            return false;
        }
        // CR-035: fetch한 원문 본문을 먼저 저장(관리자 상세 본문 노출용 — 깨진 noticedesc API 링크 대체).
        // LLM 번역 성패와 무관하게 본문은 남긴다.
        opp.applyDescriptionBody(body);
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

    /**
     * HTML 태그 제거 + 문단 줄바꿈 보존. 빈 결과면 null.
     * CR-035: 블록 태그(&lt;br&gt;,&lt;/p&gt;,&lt;/div&gt;,&lt;li&gt;)는 줄바꿈으로 바꿔 문단을 살린다.
     * 기존엔 \s+를 공백 1개로 합쳐 줄바꿈이 전부 사라졌다(번역문이 한 줄로 보이던 원인).
     */
    private String stripHtml(String s) {
        if (s == null) return null;
        String plain = s
                // 블록 경계 → 줄바꿈
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>|</div>|</li>", "\n")
                .replaceAll("(?i)<li[^>]*>", "\n• ")
                // 나머지 태그 제거
                .replaceAll("<[^>]+>", " ")
                // 줄바꿈은 보존하되, 각 줄 안의 연속 공백/탭만 1칸으로
                .replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                // 3줄 이상 연속 빈 줄은 2줄로 축약
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
        return plain.isEmpty() ? null : plain;
    }
}
