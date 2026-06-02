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
import java.util.concurrent.ConcurrentHashMap;

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

    @Value("${app.aimbase.workflows.opportunity-analysis:opportunity-analysis}")
    private String opportunityAnalysisWorkflowId;

    @Value("${app.aimbase.workflows.type-pattern-extraction:type-pattern-extraction}")
    private String typePatternExtractionWorkflowId;

    // CR-028: 제안서 파이프라인 3단계
    @Value("${app.aimbase.workflows.proposal-design:proposal-design}")
    private String proposalDesignWorkflowId;

    @Value("${app.aimbase.workflows.proposal-write-section:proposal-write-section}")
    private String proposalWriteSectionWorkflowId;

    @Value("${app.aimbase.workflows.proposal-assemble:proposal-assemble}")
    private String proposalAssembleWorkflowId;

    // CR-031: 충실성 검증 (Haiku)
    @Value("${app.aimbase.workflows.proposal-verify-fidelity:proposal-verify-fidelity}")
    private String proposalVerifyFidelityWorkflowId;

    // CR-022 (재구현): 공고 본문 영→한 번역. 결과는 MCP 콜백 없이 stepResults에서 직접 추출.
    @Value("${app.aimbase.workflows.opportunity-description-translation:translate-opportunity-description}")
    private String opportunityDescriptionTranslationWorkflowId;

    // CR-022 (재구현): 공고 제목 영→한 번역. 어제부터 Aimbase에 등록되어있던 워크플로우.
    @Value("${app.aimbase.workflows.opportunity-title-translation:translate-opportunity-title}")
    private String opportunityTitleTranslationWorkflowId;

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
     * 공고 사전 분석 — opportunity-analysis 워크플로우 실행 (CR-003).
     * Aimbase가 MCP save_opportunity_analysis 도구를 콜백하여 결과를 직접 저장.
     */
    public WorkflowRunResponse analyzeOpportunity(Map<String, Object> input) {
        log.info("Aimbase: 공고 사전 분석 시작 opportunityId={}", input.get("opportunityId"));
        return runWorkflowAndWait(opportunityAnalysisWorkflowId, input);
    }

    /**
     * 성공 제안서 공고유형 패턴 추출 — type-pattern-extraction 워크플로우 실행 (CR-013 재설계).
     * Aimbase가 MCP get_reference_samples로 원본 파일을 직접 파싱하고, save_pattern_guide로 결과를 콜백 저장.
     */
    public WorkflowRunResponse extractTypePattern(Map<String, Object> input) {
        log.info("Aimbase: 유형 패턴 추출 시작 industryType={}", input.get("industryType"));
        return runWorkflowAndWait(typePatternExtractionWorkflowId, input);
    }

    /**
     * 공고 제목 영→한 번역 (CR-022 재구현).
     * 워크플로우 입력: { title, organizationName? }
     * 워크플로우 출력: stepResults.{step}.structured_data.titleKo
     */
    public String translateOpportunityTitle(String title, String organizationName) {
        if (title == null || title.isBlank()) return null;
        Map<String, Object> input = new HashMap<>();
        input.put("title", title);
        if (organizationName != null && !organizationName.isBlank()) {
            input.put("organizationName", organizationName);
        }
        WorkflowRunResponse response = runWorkflowAndWait(opportunityTitleTranslationWorkflowId, input);
        String translated = extractKoreanText(response);
        if (translated == null || translated.isBlank()) {
            throw new LLMPlatformException("제목 번역 결과가 비어있음: runId=" + response.getId());
        }
        return translated.trim();
    }

    /**
     * 공고 본문 영→한 번역 (CR-022 재구현).
     * 워크플로우 입력: { title, body, connection_id? }
     * 워크플로우 출력: stepResults.{step}.structured_data.descriptionKo 등에서 추출.
     * 빈 본문 입력이면 그대로 null 반환(예외 없음).
     */
    public String translateOpportunityDescription(String title, String body) {
        if (body == null || body.isBlank()) return null;
        Map<String, Object> input = new HashMap<>();
        input.put("title", title);
        input.put("body", body);

        WorkflowRunResponse response = runWorkflowAndWait(opportunityDescriptionTranslationWorkflowId, input);
        String translated = extractKoreanText(response);
        if (translated == null || translated.isBlank()) {
            throw new LLMPlatformException("본문 번역 결과가 비어있음: runId=" + response.getId());
        }
        return translated.trim();
    }

    /**
     * WorkflowRunResponse에서 한글 텍스트 추출.
     * Aimbase LLM_CALL은 response_schema 적용 시 stepResults.{step}.structured_data.{key}에 결과를 담음.
     * 우선순위: output > stepResults 마지막 step의 structured_data > stepResults 평면.
     */
    private static final List<String> KO_TEXT_KEYS = List.of(
            "descriptionKo", "description_ko", "summaryKo", "summary_ko", "summary",
            "titleKo", "title_ko", "translated", "text", "result", "output", "answer", "content");

    private String extractKoreanText(WorkflowRunResponse response) {
        if (response == null) return null;
        if (response.getOutput() != null) {
            String hit = pickFromMap(response.getOutput());
            if (hit != null) return hit;
        }
        if (response.getStepResults() != null && !response.getStepResults().isEmpty()) {
            Object lastValue = null;
            for (Object v : response.getStepResults().values()) lastValue = v;
            if (lastValue instanceof Map<?, ?> stepMap) {
                Object sd = stepMap.get("structured_data");
                if (sd instanceof Map<?, ?> sdMap) {
                    String hit = pickFromMap(sdMap);
                    if (hit != null) return hit;
                }
                String hit = pickFromMap(stepMap);
                if (hit != null) return hit;
            }
            if (lastValue instanceof String s && !s.isBlank()) return s;
        }
        return null;
    }

    private String pickFromMap(Map<?, ?> map) {
        for (String key : KO_TEXT_KEYS) {
            Object v = map.get(key);
            if (v instanceof String s && !s.isBlank()) return s;
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CR-028: 제안서 파이프라인 3단계
    // 모두 MCP 콜백으로 결과를 저장한다 (BE 는 실행/폴링만):
    //   design        → save_proposal_structure (chapter/section 트리)
    //   write-section → save_section_blocks      (해당 section block)
    //   assemble      → save_document_version    (통합 contentJson, 기존 도구 재사용)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 1단계 design — proposal-design 워크플로우 실행.
     * 입력: documentId, bidRequestId, factorTree/priceItems (NULL 가능 → WF 가 본문 발췌), 공고 텍스트.
     * Aimbase 가 MCP save_proposal_structure 로 chapter/section 트리를 직접 저장.
     */
    public WorkflowRunResponse runProposalDesign(Map<String, Object> input) {
        log.info("Aimbase: 제안서 design 시작 documentId={}", input.get("documentId"));
        return runWorkflowAndWait(proposalDesignWorkflowId, input);
    }

    /**
     * 2단계 write-section — proposal-write-section 워크플로우 실행 (section 1개 = 1회).
     * 입력: sectionId (그 section 만). WF 는 get_section_context 로 scope/요구사항/샘플 조회 후 본문 작성.
     * Aimbase 가 MCP save_section_blocks 로 block 을 직접 저장.
     */
    public WorkflowRunResponse runWriteSection(Map<String, Object> input) {
        log.info("Aimbase: 제안서 write-section 시작 sectionId={}", input.get("sectionId"));
        return runWorkflowAndWait(proposalWriteSectionWorkflowId, input);
    }

    /**
     * 3단계 assemble — proposal-assemble 워크플로우 실행.
     * 입력: documentId, bidRequestId, documentType. WF 는 트리 전체를 합쳐 문체 통일.
     * Aimbase 가 MCP save_document_version 으로 통합 contentJson 을 저장.
     */
    public WorkflowRunResponse runProposalAssemble(Map<String, Object> input) {
        log.info("Aimbase: 제안서 assemble 시작 documentId={}", input.get("documentId"));
        return runWorkflowAndWait(proposalAssembleWorkflowId, input);
    }

    /**
     * CR-031 충실성 검증 — proposal-verify-fidelity 워크플로우 실행 (Haiku).
     * 입력: targetType(NOTICE/PROPOSAL_SECTION), targetId, attempt, (선택)hallucinations(이전 회차 negative example).
     * WF 는 get_section_verify_input 으로 원문/첨부/결과물을 조회하고 문장 단위 환각·누락을 추출,
     * Aimbase 가 MCP save_verification_result 로 verification_log 에 직접 저장한다.
     */
    public WorkflowRunResponse runVerifyFidelity(Map<String, Object> input) {
        log.info("Aimbase: 충실성 검증 시작 target={}/{}, attempt={}",
                input.get("targetType"), input.get("targetId"), input.get("attempt"));
        return runWorkflowAndWait(proposalVerifyFidelityWorkflowId, input);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CR-029: 워크플로우 실측 모델 해석 (워크플로우 → step connection_id → connection.config.model)
    //   stepResults·agents API 둘 다 model 을 노출하지 않으므로 connection 에서 실측한다.
    //   워크플로우별 모델은 고정이므로 1회 조회 후 캐싱.
    // ─────────────────────────────────────────────────────────────────────────

    private final Map<String, String> workflowModelCache = new ConcurrentHashMap<>();

    private static final ParameterizedTypeReference<AimbaseApiResponse<Map<String, Object>>> MAP_RESPONSE_TYPE =
        new ParameterizedTypeReference<>() {};

    public String resolveDesignModel()       { return resolveWorkflowModel(proposalDesignWorkflowId); }
    public String resolveWriteSectionModel() { return resolveWorkflowModel(proposalWriteSectionWorkflowId); }
    public String resolveAssembleModel()     { return resolveWorkflowModel(proposalAssembleWorkflowId); }
    public String resolveVerifyModel()       { return resolveWorkflowModel(proposalVerifyFidelityWorkflowId); }

    /**
     * 워크플로우의 첫 AGENT_CALL/LLM_CALL step 이 쓰는 connection 의 모델명을 반환.
     * 조회 실패 시 null (호출부는 null 이면 단가 폴백·model NULL 기록).
     */
    public String resolveWorkflowModel(String workflowId) {
        return workflowModelCache.computeIfAbsent(workflowId, this::fetchWorkflowModel);
    }

    @SuppressWarnings("unchecked")
    private String fetchWorkflowModel(String workflowId) {
        try {
            Map<String, Object> wf = getMap(baseUrl + "/api/v1/workflows/" + workflowId);
            if (wf == null) return null;

            List<Map<String, Object>> steps = (List<Map<String, Object>>) wf.get("steps");
            if (steps == null || steps.isEmpty()) return null;

            // 첫 step 의 config.connection_id (3개 워크플로우 모두 단일 step)
            String connectionId = null;
            for (Map<String, Object> step : steps) {
                Map<String, Object> config = (Map<String, Object>) step.get("config");
                if (config != null && config.get("connection_id") != null) {
                    connectionId = String.valueOf(config.get("connection_id"));
                    break;
                }
            }
            if (connectionId == null) return null;

            Map<String, Object> conn = getMap(baseUrl + "/api/v1/connections/" + connectionId);
            if (conn == null) return null;
            Map<String, Object> connConfig = (Map<String, Object>) conn.get("config");
            String model = connConfig != null ? (String) connConfig.get("model") : null;
            log.info("[CR-029] 워크플로우 모델 해석: workflowId={} → connection={} → model={}",
                    workflowId, connectionId, model);
            return model;
        } catch (Exception e) {
            log.warn("[CR-029] 워크플로우 모델 해석 실패 workflowId={}: {}", workflowId, e.getMessage());
            return null;
        }
    }

    private Map<String, Object> getMap(String url) {
        ResponseEntity<AimbaseApiResponse<Map<String, Object>>> resp =
                llmPlatformRestTemplate.exchange(url, HttpMethod.GET, null, MAP_RESPONSE_TYPE);
        AimbaseApiResponse<Map<String, Object>> body = resp.getBody();
        return (body != null && body.getData() != null) ? body.getData() : null;
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
        // Aimbase WorkflowController.run()은 Map<String,Object>를 직접 받음 (래핑 없음)
        WorkflowRunResponse runResponse;
        try {
            ResponseEntity<AimbaseApiResponse<WorkflowRunResponse>> responseEntity =
                llmPlatformRestTemplate.exchange(runUrl, HttpMethod.POST,
                    new HttpEntity<>(input), WORKFLOW_RESPONSE_TYPE);

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
                throw new LLMPlatformException("워크플로우 실행 실패: runId=" + runId + ", error=" + pollResponse.getErrorAsString());
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
