package com.biddingagency.domain.proposal.service;

import com.biddingagency.domain.proposal.entity.GenerationLog;
import com.biddingagency.domain.proposal.entity.GenerationTargetType;
import com.biddingagency.domain.proposal.repository.GenerationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
}
