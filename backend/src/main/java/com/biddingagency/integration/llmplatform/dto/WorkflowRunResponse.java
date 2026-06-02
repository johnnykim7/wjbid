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

    /**
     * 오류 정보 (status=failed 일 때 채워짐).
     * Aimbase는 문자열 또는 {step, message, ...} 객체 양쪽으로 보냄 — Object로 받아 호환.
     * (String 고정 시 객체 응답에서 Jackson 역직렬화 실패 → 폴링이 매번 깨져 무한 재시도)
     */
    private Object error;

    /** error를 사람이 읽기 좋은 문자열로 변환. 객체면 message 필드 우선, 없으면 toString. */
    @SuppressWarnings("unchecked")
    public String getErrorAsString() {
        if (error == null) return null;
        if (error instanceof String s) return s;
        if (error instanceof Map<?, ?> m) {
            Object msg = ((Map<String, Object>) m).get("message");
            return msg != null ? msg.toString() : error.toString();
        }
        return error.toString();
    }

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

    // ── CR-029 비용 추적: stepResults 의 토큰을 합산 ─────────────────────────
    // Aimbase AGENT_CALL step 결과 map 은 "input_tokens"/"output_tokens" 키를 담는다
    // (aimbase AgentCallStepExecutor). stepResults = { stepId -> resultMap } 전체를 순회해 합산.

    /** 전체 step 의 input 토큰 합. 데이터 없으면 0. */
    public int totalInputTokens() {
        return sumTokens("input_tokens");
    }

    /** 전체 step 의 output 토큰 합. 데이터 없으면 0. */
    public int totalOutputTokens() {
        return sumTokens("output_tokens");
    }

    @SuppressWarnings("unchecked")
    private int sumTokens(String key) {
        if (stepResults == null || stepResults.isEmpty()) {
            return 0;
        }
        int sum = 0;
        for (Object stepResult : stepResults.values()) {
            if (stepResult instanceof Map<?, ?> resultMap) {
                Object val = ((Map<String, Object>) resultMap).get(key);
                if (val instanceof Number num) {
                    sum += num.intValue();
                }
            }
        }
        return sum;
    }
}
