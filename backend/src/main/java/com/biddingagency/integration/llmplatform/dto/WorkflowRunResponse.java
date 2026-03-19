package com.biddingagency.integration.llmplatform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * LLM Platform 워크플로우 실행 결과 DTO
 * POST /api/v1/workflows/{id}/run 과 GET /api/v1/workflows/{id}/runs/{runId} 에서 공통으로 사용
 */
@Data
@NoArgsConstructor
public class WorkflowRunResponse {

    /** 워크플로우 실행 ID */
    private String id;

    /** 실행 상태: running | completed | failed */
    private String status;

    /** 워크플로우 출력 (status=completed 일 때 채워짐) */
    private Map<String, Object> output;

    /** 오류 메시지 (status=failed 일 때 채워짐) */
    private String error;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("completed_at")
    private String completedAt;

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
