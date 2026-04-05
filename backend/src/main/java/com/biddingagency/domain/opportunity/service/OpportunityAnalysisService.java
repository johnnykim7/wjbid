package com.biddingagency.domain.opportunity.service;

import com.biddingagency.domain.event.OpportunityAnalysisCompletedEvent;
import com.biddingagency.domain.opportunity.entity.AnalysisStatus;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.OpportunityAnalysis;
import com.biddingagency.domain.opportunity.repository.OpportunityAnalysisRepository;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import com.biddingagency.integration.llmplatform.LLMPlatformClient;
import com.biddingagency.integration.llmplatform.LLMPlatformException;
import com.biddingagency.integration.llmplatform.dto.WorkflowRunResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OpportunityAnalysisService {

    private final OpportunityAnalysisRepository analysisRepository;
    private final OpportunityRepository opportunityRepository;
    private final LLMPlatformClient llmPlatformClient;
    private final ApplicationEventPublisher eventPublisher;

    public Optional<OpportunityAnalysis> findByOpportunityId(UUID opportunityId) {
        return analysisRepository.findByOpportunityId(opportunityId);
    }

    @Transactional
    public OpportunityAnalysis triggerAnalysis(UUID opportunityId) {
        Opportunity opportunity = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new IllegalArgumentException("Opportunity not found: " + opportunityId));

        OpportunityAnalysis analysis = analysisRepository.findByOpportunityId(opportunityId)
                .orElseGet(() -> {
                    OpportunityAnalysis newAnalysis = OpportunityAnalysis.builder()
                            .opportunity(opportunity)
                            .build();
                    return analysisRepository.save(newAnalysis);
                });

        analysis.markAnalyzing(null);
        analysisRepository.save(analysis);

        log.info("[분석] 공고 사전 분석 트리거: opportunityId={}, analysisId={}", opportunityId, analysis.getId());

        analyzeAsync(opportunityId, analysis.getId());

        return analysis;
    }

    @Async("llmTaskExecutor")
    public void analyzeAsync(UUID opportunityId, UUID analysisId) {
        log.info("[분석] 공고 사전 분석 시작: opportunityId={}", opportunityId);

        try {
            Opportunity opp = opportunityRepository.findById(opportunityId)
                    .orElseThrow(() -> new IllegalArgumentException("Opportunity not found: " + opportunityId));

            Map<String, Object> input = new HashMap<>();
            input.put("opportunityId", opportunityId.toString());
            input.put("opportunityText", buildOpportunityText(opp));
            input.put("analysisId", analysisId.toString());

            WorkflowRunResponse response = llmPlatformClient.analyzeOpportunity(input);

            // Aimbase 워크플로우가 MCP save_opportunity_analysis를 콜백하여 결과 저장
            log.info("[분석] 공고 사전 분석 완료 (Aimbase MCP 콜백으로 저장됨): opportunityId={}", opportunityId);

            eventPublisher.publishEvent(
                    new OpportunityAnalysisCompletedEvent(analysisId, opportunityId, true, null));

        } catch (LLMPlatformException e) {
            log.error("[분석] Aimbase 오류 (공고 분석): opportunityId={}", opportunityId, e);
            markAnalysisFailed(analysisId, opportunityId, e.getMessage());
        } catch (Exception e) {
            log.error("[분석] 예상치 못한 오류 (공고 분석): opportunityId={}", opportunityId, e);
            markAnalysisFailed(analysisId, opportunityId, e.getMessage());
        }
    }

    @Transactional
    public void markAnalysisFailed(UUID analysisId, UUID opportunityId, String errorMessage) {
        analysisRepository.findById(analysisId).ifPresent(analysis -> {
            analysis.markFailed(errorMessage);
            analysisRepository.save(analysis);
        });
        eventPublisher.publishEvent(
                new OpportunityAnalysisCompletedEvent(analysisId, opportunityId, false, errorMessage));
    }

    @Transactional
    public OpportunityAnalysis saveAnalysisResult(UUID opportunityId,
                                                   Map<String, Object> summaryJson,
                                                   Map<String, Object> documentFormatsJson,
                                                   Map<String, Object> requiredDocumentsJson,
                                                   Map<String, Object> llmPromptPresetJson) {
        OpportunityAnalysis analysis = analysisRepository.findByOpportunityId(opportunityId)
                .orElseThrow(() -> new IllegalArgumentException("OpportunityAnalysis not found for opportunity: " + opportunityId));

        analysis.markCompleted(summaryJson, documentFormatsJson, requiredDocumentsJson, llmPromptPresetJson);
        return analysisRepository.save(analysis);
    }

    @Transactional
    public OpportunityAnalysis updateAnalysisResult(UUID opportunityId,
                                                     Map<String, Object> summaryJson,
                                                     Map<String, Object> documentFormatsJson,
                                                     Map<String, Object> requiredDocumentsJson,
                                                     Map<String, Object> llmPromptPresetJson) {
        OpportunityAnalysis analysis = analysisRepository.findByOpportunityId(opportunityId)
                .orElseThrow(() -> new IllegalArgumentException("OpportunityAnalysis not found for opportunity: " + opportunityId));

        analysis.updateAnalysisResult(summaryJson, documentFormatsJson, requiredDocumentsJson, llmPromptPresetJson);
        return analysisRepository.save(analysis);
    }

    private String buildOpportunityText(Opportunity opp) {
        StringBuilder sb = new StringBuilder();
        sb.append("공고 제목: ").append(opp.getTitle()).append("\n");
        if (opp.getOrganizationName() != null)
            sb.append("기관: ").append(opp.getOrganizationName()).append("\n");
        if (opp.getType() != null)
            sb.append("유형: ").append(opp.getType()).append("\n");
        if (opp.getResponseDeadline() != null)
            sb.append("마감일: ").append(opp.getResponseDeadline()).append("\n");
        if (opp.getSolicitationNumber() != null)
            sb.append("공고번호: ").append(opp.getSolicitationNumber()).append("\n");
        if (opp.getUiLink() != null)
            sb.append("링크: ").append(opp.getUiLink()).append("\n");
        if (opp.getRawJson() != null && !opp.getRawJson().isEmpty()) {
            Object description = opp.getRawJson().get("description");
            if (description != null) {
                sb.append("\n상세 설명:\n").append(description);
            }
        }
        return sb.toString();
    }
}
