package com.biddingagency.integration.llmplatform;

import com.biddingagency.integration.ai.client.dto.*;
import com.biddingagency.integration.llmplatform.dto.WorkflowRunResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM Platform Workflow 기반 클라이언트
 * AIServiceClient(Python FastAPI)를 대체하며 동일한 인터페이스를 제공합니다.
 *
 * 흐름:
 *   1. POST /api/v1/workflows/{workflowId}/run → 실행 시작, runId 반환
 *   2. GET  /api/v1/workflows/{workflowId}/runs/{runId} → 완료될 때까지 폴링
 *   3. output 필드에서 결과 추출 → 기존 DTO로 변환
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LLMPlatformClient {

    private final RestTemplate llmPlatformRestTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.llm-platform.base-url}")
    private String baseUrl;

    @Value("${app.llm-platform.workflows.requirement-extraction:requirement-extraction}")
    private String requirementExtractionWorkflowId;

    @Value("${app.llm-platform.workflows.document-generation:bid-document-generation}")
    private String documentGenerationWorkflowId;

    @Value("${app.llm-platform.polling.interval-ms:2000}")
    private long pollingIntervalMs;

    @Value("${app.llm-platform.polling.max-attempts:90}")
    private int maxPollingAttempts;

    // ─────────────────────────────────────────────────────────────────────────
    // Public API (기존 AIServiceClient와 동일한 시그니처)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 요구사항 추출 — requirement-extraction 워크플로우 실행
     */
    public RequirementExtractionResponse extractRequirements(RequirementExtractionRequest request) {
        log.info("LLM Platform: 요구사항 추출 시작 opportunityId={}", request.getOpportunityId());

        Map<String, Object> input = new HashMap<>();
        input.put("opportunityId", request.getOpportunityId() != null ? request.getOpportunityId().toString() : null);
        input.put("opportunityText", request.getOpportunityText());
        input.put("metadata", request.getMetadata());

        WorkflowRunResponse result = runWorkflowAndWait(requirementExtractionWorkflowId, input);
        return toRequirementExtractionResponse(result);
    }

    /**
     * 문서 생성 — bid-document-generation 워크플로우 실행
     */
    public DocumentGenerationResponse generateDocument(DocumentGenerationRequest request) {
        log.info("LLM Platform: 문서 생성 시작 bidRequestId={}, documentType={}",
            request.getBidRequestId(), request.getDocumentType());

        Map<String, Object> input = new HashMap<>();
        input.put("bidRequestId", request.getBidRequestId() != null ? request.getBidRequestId().toString() : null);
        input.put("documentType", request.getDocumentType());
        input.put("opportunityText", request.getOpportunityText());
        input.put("requirements", request.getRequirements());
        input.put("context", request.getContext());

        WorkflowRunResponse result = runWorkflowAndWait(documentGenerationWorkflowId, input);
        return toDocumentGenerationResponse(result);
    }

    /**
     * LLM Platform 헬스체크
     */
    public boolean isHealthy() {
        try {
            llmPlatformRestTemplate.getForEntity(baseUrl + "/health", String.class);
            return true;
        } catch (Exception e) {
            log.warn("LLM Platform health check failed", e);
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 워크플로우 실행 + 폴링
    // ─────────────────────────────────────────────────────────────────────────

    private WorkflowRunResponse runWorkflowAndWait(String workflowId, Map<String, Object> input) {
        String runUrl = baseUrl + "/api/v1/workflows/" + workflowId + "/run";
        String pollUrlTemplate = baseUrl + "/api/v1/workflows/" + workflowId + "/runs/";

        // 1단계: 워크플로우 실행 시작
        WorkflowRunResponse runResponse;
        try {
            runResponse = llmPlatformRestTemplate.postForObject(runUrl, input, WorkflowRunResponse.class);
        } catch (RestClientException e) {
            log.error("LLM Platform workflow 실행 요청 실패: workflowId={}", workflowId, e);
            throw new LLMPlatformException("LLM Platform 연결 실패: " + e.getMessage(), e);
        }

        if (runResponse == null || runResponse.getId() == null) {
            throw new LLMPlatformException("LLM Platform에서 runId를 반환하지 않았습니다: workflowId=" + workflowId);
        }

        String runId = runResponse.getId();
        log.info("LLM Platform: 워크플로우 실행 시작됨 workflowId={}, runId={}", workflowId, runId);

        // 이미 완료된 경우 (동기 실행)
        if (runResponse.isCompleted()) {
            log.info("LLM Platform: 워크플로우 즉시 완료 runId={}", runId);
            return runResponse;
        }

        // 2단계: 완료될 때까지 폴링
        String pollUrl = pollUrlTemplate + runId;
        long startTime = System.currentTimeMillis();

        for (int attempt = 1; attempt <= maxPollingAttempts; attempt++) {
            sleep(pollingIntervalMs);

            WorkflowRunResponse pollResponse;
            try {
                pollResponse = llmPlatformRestTemplate.getForObject(pollUrl, WorkflowRunResponse.class);
            } catch (RestClientException e) {
                log.warn("LLM Platform 폴링 실패 (attempt={}/{}): runId={}", attempt, maxPollingAttempts, runId, e);
                continue;
            }

            if (pollResponse == null) {
                continue;
            }

            log.debug("LLM Platform 폴링 attempt={}, status={}, runId={}", attempt, pollResponse.getStatus(), runId);

            if (pollResponse.isCompleted()) {
                long elapsed = System.currentTimeMillis() - startTime;
                log.info("LLM Platform: 워크플로우 완료 runId={}, 소요={}ms", runId, elapsed);
                return pollResponse;
            }

            if (pollResponse.isFailed()) {
                throw new LLMPlatformException("워크플로우 실행 실패: runId=" + runId + ", error=" + pollResponse.getError());
            }
        }

        throw new LLMPlatformException("워크플로우 타임아웃: runId=" + runId + " (" + maxPollingAttempts + "회 폴링 초과)");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // output → 기존 DTO 변환
    // ─────────────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private RequirementExtractionResponse toRequirementExtractionResponse(WorkflowRunResponse result) {
        Map<String, Object> output = result.getOutput();
        if (output == null) {
            throw new LLMPlatformException("워크플로우 output이 비어 있습니다 (요구사항 추출)");
        }

        try {
            // output 전체가 RequirementExtractionResponse 형식이라고 가정
            return objectMapper.convertValue(output, RequirementExtractionResponse.class);
        } catch (Exception e) {
            log.error("RequirementExtractionResponse 변환 실패, output={}", output, e);
            throw new LLMPlatformException("요구사항 추출 결과 파싱 실패: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private DocumentGenerationResponse toDocumentGenerationResponse(WorkflowRunResponse result) {
        Map<String, Object> output = result.getOutput();
        if (output == null) {
            throw new LLMPlatformException("워크플로우 output이 비어 있습니다 (문서 생성)");
        }

        try {
            return objectMapper.convertValue(output, DocumentGenerationResponse.class);
        } catch (Exception e) {
            log.error("DocumentGenerationResponse 변환 실패, output={}", output, e);
            throw new LLMPlatformException("문서 생성 결과 파싱 실패: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 유틸
    // ─────────────────────────────────────────────────────────────────────────

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LLMPlatformException("폴링 대기 중 인터럽트 발생");
        }
    }
}
