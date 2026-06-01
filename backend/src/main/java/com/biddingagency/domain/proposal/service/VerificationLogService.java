package com.biddingagency.domain.proposal.service;

import com.biddingagency.domain.proposal.entity.*;
import com.biddingagency.domain.proposal.repository.VerificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 충실성·분량 검증 로그 서비스 (CR-031).
 *
 * <p>두 경로의 검증 결과를 verification_log 에 적재하고, 관리자 콘솔용 최신 조회를 제공한다:
 * <ul>
 *   <li>BE 정형 룰(method=RULE) — {@link #recordRule}. 분량/첨부 0건/block 0개/min_words.
 *       정형 룰 평가 자체는 호출부(NoticeService/AIWorkflowService)가 수행하고 결과만 적재.</li>
 *   <li>LLM 검증(method=LLM) — {@link #recordLlm}. proposal-verify-fidelity 워크플로우가
 *       save_verification_result 콜백으로 환각/누락을 넘긴다.</li>
 * </ul>
 *
 * <p>append-only — 재시도 회차마다 새 row. 최신 1건이 현재 상태.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationLogService {

    private final VerificationLogRepository repository;

    // ─── 저장 ──────────────────────────────────────────────────

    /** BE 정형 룰 결과 적재. findings 비면 PASS, 있으면 FAIL. */
    @Transactional
    public VerificationLog recordRule(VerificationTargetType targetType, UUID targetId,
                                      List<String> findings, Integer wordCount, int attempt) {
        Verdict verdict = (findings == null || findings.isEmpty()) ? Verdict.PASS : Verdict.FAIL;
        VerificationLog saved = repository.save(VerificationLog.builder()
                .targetType(targetType)
                .targetId(targetId)
                .method(VerificationMethod.RULE)
                .verdict(verdict)
                .ruleFindings(findings != null ? findings : List.of())
                .wordCount(wordCount)
                .attempt(attempt)
                .build());
        log.info("[CR-031] RULE 검증 적재: target={}/{}, verdict={}, findings={}, attempt={}",
                targetType, targetId, verdict, findings != null ? findings.size() : 0, attempt);
        return saved;
    }

    /** LLM(verify-fidelity) 결과 적재. 환각 ≥1 또는 verdict=FAIL 이면 FAIL. */
    @Transactional
    public VerificationLog recordLlm(VerificationTargetType targetType, UUID targetId,
                                     String workflowRunId, Verdict verdict,
                                     List<Map<String, Object>> hallucinations,
                                     List<Map<String, Object>> missingFromSource,
                                     Integer wordCount, int attempt) {
        VerificationLog saved = repository.save(VerificationLog.builder()
                .targetType(targetType)
                .targetId(targetId)
                .method(VerificationMethod.LLM)
                .verdict(verdict)
                .workflowRunId(workflowRunId)
                .hallucinations(hallucinations != null ? hallucinations : List.of())
                .missingFromSource(missingFromSource != null ? missingFromSource : List.of())
                .wordCount(wordCount)
                .attempt(attempt)
                .build());
        log.info("[CR-031] LLM 검증 적재: target={}/{}, verdict={}, 환각={}, 누락={}, attempt={}",
                targetType, targetId, verdict,
                saved.hallucinationCount(),
                missingFromSource != null ? missingFromSource.size() : 0, attempt);
        return saved;
    }

    // ─── 조회 ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Optional<VerificationLog> findLatest(VerificationTargetType targetType, UUID targetId) {
        return repository.findFirstByTargetTypeAndTargetIdOrderByVerifiedAtDesc(targetType, targetId);
    }

    @Transactional(readOnly = true)
    public List<VerificationLog> findHistory(VerificationTargetType targetType, UUID targetId) {
        return repository.findByTargetTypeAndTargetIdOrderByVerifiedAtDesc(targetType, targetId);
    }

    /** 다음 재시도 회차 = (최신 attempt) + 1. 최초면 1. */
    @Transactional(readOnly = true)
    public int nextAttempt(VerificationTargetType targetType, UUID targetId) {
        return findLatest(targetType, targetId).map(v -> v.getAttempt() + 1).orElse(1);
    }

    // ─── BE 정형 룰 평가기 (LLM 0콜) ─────────────────────────────

    /**
     * 공고문 정제 결과 분량/첨부 정형 룰 (CR-031 ❶~❹).
     *
     * @param attachmentCount SUCCESS 첨부 수
     * @param descriptionLen  원문 description 길이
     * @param overviewLen     summary.overview 길이
     * @param contentLen      contentJson 직렬화 길이 (양식 미등록이면 0)
     * @param hasFactorTree   factor_tree_json.factors 1건 이상 여부
     * @param hasPriceItems   price_items_json.items 1건 이상 여부 (없어도 오류 아님 — 경고만)
     * @return 위반 항목 라벨 목록 (비면 통과)
     */
    public List<String> evaluateNoticeRules(int attachmentCount, int descriptionLen,
                                            int overviewLen, int contentLen,
                                            boolean hasFactorTree, boolean hasPriceItems) {
        List<String> findings = new ArrayList<>();

        // ❶ 첨부 0건 + description ≤ 300자 → 원문 부족. 풍부한 본문 작성 자체가 환각 위험.
        if (attachmentCount == 0 && descriptionLen <= 300 && contentLen > 0) {
            findings.add("원문 정보 부족(첨부 0건, description " + descriptionLen + "자)인데 본문이 작성됨 — 환각 위험");
        }
        // ❷ overview 최소치
        if (overviewLen < 50) {
            findings.add("summary.overview 분량 미달(" + overviewLen + "자, 최소 50자)");
        }
        // ❷ 양식 등록 시 contentJson 분량 최소치
        if (contentLen > 0 && contentLen < 800) {
            findings.add("contentJson 분량 미달(" + contentLen + "자, 최소 800자)");
        }
        // ❹ price_items 없음은 오류 아님 — 룰 위반으로 보지 않음(명시는 호출부 본문 처리)
        return findings;
    }

    /**
     * 제안서 section 분량/구조 정형 룰 (CR-031 ❺❻).
     *
     * @param blockCount block 개수
     * @param wordCount  본문 단어 수
     * @param minWords   section.min_words (null 이면 분량 룰 미적용)
     * @return 위반 항목 라벨 목록 (비면 통과)
     */
    public List<String> evaluateSectionRules(int blockCount, int wordCount, Integer minWords) {
        List<String> findings = new ArrayList<>();
        // ❻ block 0개 → 작성 실패
        if (blockCount == 0) {
            findings.add("block 0개 — section 본문 작성 실패");
        }
        // ❺ min_words 미달
        if (minWords != null && minWords > 0 && wordCount < minWords) {
            findings.add("분량 미달(" + wordCount + " words, 최소 " + minWords + ")");
        }
        return findings;
    }
}
