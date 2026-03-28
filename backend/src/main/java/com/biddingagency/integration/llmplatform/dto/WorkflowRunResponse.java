package com.biddingagency.integration.llmplatform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Aimbase 워크플로우 실행 결과 DTO (CR-002)
 * POST /api/v1/workflows/{id}/run 과 GET /api/v1/workflows/{id}/runs/{runId} 에서 공통으로 사용.
 * Aimbase 응답은 AimbaseApiResponse.data에 이 객체가 들어옴.
 */
@Data
@NoArgsConstructor
public class WorkflowRunResponse {

    /** 워크플로우 실행 ID */
    private String id;

    /** 워크플로우 ID */
    private String workflowId;

    /** 실행 상태: running | completed | failed | pending_approval */
    private String status;

    /** 워크플로우 출력 (레거시 호환) */
    private Map<String, Object> output;

    /** 스텝별 실행 결과 (Aimbase 형식) */
    private Map<String, Object> stepResults;

    /** 오류 메시지 (status=failed 일 때 채워짐) */
    private String error;

    @JsonProperty("startedAt")
    private String createdAt;

    @JsonProperty("completedAt")
    private String completedAt;

    /** 실행 시간(ms) */
    private Long durationMs;

    public boolean isCompleted() {
        return "completed".equalsIgnoreCase(status);
    }

    public boolean isFailed() {
        return "failed".equalsIgnoreCase(status);
    }

    public boolean isRunning() {
        return "running".equalsIgnoreCase(status) || "pending".equalsIgnoreCase(status);
    }
}
