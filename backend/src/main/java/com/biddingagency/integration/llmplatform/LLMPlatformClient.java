package com.biddingagency.integration.llmplatform;

import com.biddingagency.integration.ai.client.dto.*;
import com.biddingagency.integration.llmplatform.dto.AimbaseApiResponse;
import com.biddingagency.integration.llmplatform.dto.WorkflowRunResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aimbase Workflow 기반 클라이언트 (CR-002)
 *
 * 흐름:
 *   1. POST /api/v1/workflows/{workflowId}/run → 비동기 실행 시작, runId 반환
 *   2. GET  /api/v1/workflows/{workflowId}/runs/{runId} → 완료될 때까지 폴링
 *   3. Aimbase가 워크플로우 실행 중 MCP 도구를 콜백하여 데이터 저장
 *   4. 플랫폼은 상태 확인만 (데이터는 MCP 콜백으로 이미 저장됨)
 *
 * Aimbase 응답 형식: { success: true, data: { id, status, stepResults, ... } }
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LLMPlatformClient {

    private final RestTemplate llmPlatformRestTemplate;

    @Value("${app.aimbase.base-url}")
    private String baseUrl;

    @Value("${app.aimbase.workflows.requirement-extraction:requirement-extraction}")
    private String requirementExtractionWorkflowId;

    @Value("${app.aimbase.workflows.document-generation:bid-document-generation}")
    private String documentGenerationWorkflowId;

    @Value("${app.aimbase.polling.interval-ms:3000}")
    private long pollingIntervalMs;

    @Value("${app.aimbase.polling.max-attempts:60}")
    private int maxPollingAttempts;

    private static final ParameterizedTypeReference<AimbaseApiResponse<WorkflowRunResponse>> WORKFLOW_RESPONSE_TYPE =
        new ParameterizedTypeReference<>() {};

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 요구사항 추출 — requirement-extraction 워크플로우 실행.
     * Aimbase가 MCP save_requirements 도구를 콜백하여 결과를 직접 저장.
     */
    public RequirementExtractionResponse extractRequirements(RequirementExtractionRequest request) {
        log.info("Aimbase: 요구사항 추출 시작 opportunityId={}", request.getOpportunityId());

        Map<String, Object> input = new HashMap<>();
        input.put("opportunityId", request.getOpportunityId() != null ? request.getOpportunityId().toString() : null);
        input.put("opportunityText", request.getOpportunityText());
        input.put("metadata", request.getMetadata());

        runWorkflowAndWait(requirementExtractionWorkflowId, input);

        // Aimbase 워크플로우가 MCP save_requirements를 직접 호출하므로
        // 여기서는 성공 상태만 반환 (데이터는 이미 DB에 저장됨)
        return RequirementExtractionResponse.builder()
            .status("success")
            .requirements(List.of())
            .build();
    }

    /**
     * 문서 생성 — bid-document-generation 워크플로우 실행.
     * Aimbase가 MCP save_document_version 도구를 콜백하여 결과를 직접 저장.
     */
    public DocumentGenerationResponse generateDocument(DocumentGenerationRequest request) {
        log.info("Aimbase: 문서 생성 시작 bidRequestId={}, documentType={}",
            request.getBidRequestId(), request.getDocumentType());

        Map<String, Object> input = new HashMap<>();
        input.put("bidRequestId", request.getBidRequestId() != null ? request.getBidRequestId().toString() : null);
        input.put("documentType", request.getDocumentType());
        input.put("opportunityText", request.getOpportunityText());
        input.put("requirements", request.getRequirements());
        input.put("context", request.getContext());

        runWorkflowAndWait(documentGenerationWorkflowId, input);

        // Aimbase 워크플로우가 MCP save_document_version을 직접 호출하므로
        // 여기서는 성공 상태만 반환
        return DocumentGenerationResponse.builder()
            .status("success")
            .build();
    }

    /**
     * Aimbase 헬스체크
     */
    public boolean isHealthy() {
        try {
            llmPlatformRestTemplate.getForEntity(baseUrl + "/api/v1/connections", String.class);
            return true;
        } catch (Exception e) {
            log.warn("Aimbase health check failed: {}", e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 워크플로우 실행 + 폴링 (Aimbase API 규격)
    // ─────────────────────────────────────────────────────────────────────────

    private WorkflowRunResponse runWorkflowAndWait(String workflowId, Map<String, Object> input) {
        String runUrl = baseUrl + "/api/v1/workflows/" + workflowId + "/run";
        String pollUrlTemplate = baseUrl + "/api/v1/workflows/" + workflowId + "/runs/";

        // 1단계: 워크플로우 실행 시작
        WorkflowRunResponse runResponse;
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("inputData", input);

            ResponseEntity<AimbaseApiResponse<WorkflowRunResponse>> responseEntity =
                llmPlatformRestTemplate.exchange(runUrl, HttpMethod.POST,
                    new HttpEntity<>(requestBody), WORKFLOW_RESPONSE_TYPE);

            AimbaseApiResponse<WorkflowRunResponse> apiResponse = responseEntity.getBody();
            if (apiResponse == null || !apiResponse.isSuccess() || apiResponse.getData() == null) {
                String error = apiResponse != null ? apiResponse.getError() : "null response";
                throw new LLMPlatformException("Aimbase 워크플로우 실행 실패: " + error);
            }
            runResponse = apiResponse.getData();
        } catch (RestClientException e) {
            log.error("Aimbase workflow 실행 요청 실패: workflowId={}", workflowId, e);
            throw new LLMPlatformException("Aimbase 연결 실패: " + e.getMessage(), e);
        }

        if (runResponse.getId() == null) {
            throw new LLMPlatformException("Aimbase에서 runId를 반환하지 않았습니다: workflowId=" + workflowId);
        }

        String runId = runResponse.getId();
        log.info("Aimbase: 워크플로우 실행 시작됨 workflowId={}, runId={}", workflowId, runId);

        if (runResponse.isCompleted()) {
            log.info("Aimbase: 워크플로우 즉시 완료 runId={}", runId);
            return runResponse;
        }

        // 2단계: 완료될 때까지 폴링
        String pollUrl = pollUrlTemplate + runId;
        long startTime = System.currentTimeMillis();

        for (int attempt = 1; attempt <= maxPollingAttempts; attempt++) {
            sleep(pollingIntervalMs);

            WorkflowRunResponse pollResponse;
            try {
                ResponseEntity<AimbaseApiResponse<WorkflowRunResponse>> responseEntity =
                    llmPlatformRestTemplate.exchange(pollUrl, HttpMethod.GET,
                        null, WORKFLOW_RESPONSE_TYPE);

                AimbaseApiResponse<WorkflowRunResponse> apiResponse = responseEntity.getBody();
                if (apiResponse == null || apiResponse.getData() == null) {
                    log.warn("Aimbase 폴링 null 응답 (attempt={}/{}): runId={}", attempt, maxPollingAttempts, runId);
                    continue;
                }
                pollResponse = apiResponse.getData();
            } catch (RestClientException e) {
                log.warn("Aimbase 폴링 실패 (attempt={}/{}): runId={}", attempt, maxPollingAttempts, runId, e);
                continue;
            }

            log.debug("Aimbase 폴링 attempt={}, status={}, runId={}", attempt, pollResponse.getStatus(), runId);

            if (pollResponse.isCompleted()) {
                long elapsed = System.currentTimeMillis() - startTime;
                log.info("Aimbase: 워크플로우 완료 runId={}, 소요={}ms", runId, elapsed);
                return pollResponse;
            }

            if (pollResponse.isFailed()) {
                throw new LLMPlatformException("워크플로우 실행 실패: runId=" + runId + ", error=" + pollResponse.getError());
            }
        }

        throw new LLMPlatformException("워크플로우 타임아웃: runId=" + runId + " (" + maxPollingAttempts + "회 폴링 초과)");
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
