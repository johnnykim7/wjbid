package com.biddingagency.domain.proposal.service;

import com.biddingagency.domain.proposal.entity.GenerationLog;
import com.biddingagency.domain.proposal.entity.GenerationTargetType;
import com.biddingagency.domain.proposal.repository.GenerationLogRepository;
import com.biddingagency.integration.llmplatform.dto.WorkflowRunResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * generation_log 기록 서비스 (CR-029 선반영 — CR-027 시점부터 비용 데이터 누적).
 *
 * 라우팅 본격은 CR-029. 여기서는 LLM 호출 결과(토큰/모델/비용)를 append-only 로 적재만.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationLogService {

    private final GenerationLogRepository generationLogRepository;

    @Transactional
    public GenerationLog record(GenerationTargetType targetType, UUID targetId,
                                String workflowId, String runId, String modelName,
                                Integer tokensIn, Integer tokensOut, BigDecimal costUsd,
                                String promptHash) {
        GenerationLog log = GenerationLog.builder()
                .targetType(targetType)
                .targetId(targetId)
                .workflowId(workflowId)
                .runId(runId)
                .modelName(modelName)
                .tokensIn(tokensIn)
                .tokensOut(tokensOut)
                .costUsd(costUsd)
                .promptHash(promptHash)
                .build();
        return generationLogRepository.save(log);
    }

    /**
     * CR-029 — 파이프라인 단계 1회 실행 결과를 비용 로그로 적재.
     *
     * <p>토큰은 Aimbase 응답 stepResults 에서 합산, 모델은 라우팅 정책값, cost 는 모델 단가 × 토큰.
     * 토큰이 0(응답에 토큰 미포함)이어도 model/단가는 기록 — 누락 자체가 데이터.
     * append-only 보조 작업이므로 실패해도 본 파이프라인을 막지 않는다(상위에서 try/catch).
     *
     * @param stage    DESIGN / WRITE_SECTION / ASSEMBLE
     * @param targetId 단계 대상 (documentId 또는 sectionId)
     */
    @Transactional
    public GenerationLog recordStage(GenerationTargetType stage, UUID targetId,
                                     WorkflowRunResponse run) {
        String model = ModelRouting.modelFor(stage);
        int tokensIn = run != null ? run.totalInputTokens() : 0;
        int tokensOut = run != null ? run.totalOutputTokens() : 0;
        BigDecimal cost = computeCost(model, tokensIn, tokensOut);

        GenerationLog saved = record(
                stage, targetId,
                run != null ? run.getWorkflowId() : null,
                run != null ? run.getId() : null,
                model, tokensIn, tokensOut, cost, null);

        log.info("[CR-029] generation_log: stage={}, target={}, model={}, in={}, out={}, cost=${}",
                stage, targetId, model, tokensIn, tokensOut, cost);
        return saved;
    }

    /** 모델 단가(USD/1M tok) × 토큰. 소수 6자리 반올림. */
    private BigDecimal computeCost(String model, int tokensIn, int tokensOut) {
        double usd = (tokensIn  / 1_000_000.0) * ModelRouting.inputPricePerMTok(model)
                   + (tokensOut / 1_000_000.0) * ModelRouting.outputPricePerMTok(model);
        return BigDecimal.valueOf(usd).setScale(6, RoundingMode.HALF_UP);
    }
}
