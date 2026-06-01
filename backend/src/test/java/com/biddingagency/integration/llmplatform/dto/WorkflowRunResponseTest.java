package com.biddingagency.integration.llmplatform.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CR-029: WorkflowRunResponse 토큰 합산 헬퍼 단위 테스트.
 * stepResults 의 각 step 결과맵에서 input_tokens/output_tokens 를 합산하는 로직 검증.
 */
class WorkflowRunResponseTest {

    private WorkflowRunResponse withStepResults(Map<String, Object> stepResults) {
        WorkflowRunResponse r = new WorkflowRunResponse();
        r.setStepResults(stepResults);
        return r;
    }

    @Test
    @DisplayName("토큰합산_여러step_input_output각각합산됨")
    void 토큰합산_여러step_합산됨() {
        // given — 두 step 각각 토큰 보유
        Map<String, Object> steps = new LinkedHashMap<>();
        steps.put("step1", Map.of("input_tokens", 100, "output_tokens", 50));
        steps.put("step2", Map.of("input_tokens", 30, "output_tokens", 20));
        WorkflowRunResponse run = withStepResults(steps);

        // when / then
        assertThat(run.totalInputTokens()).isEqualTo(130);
        assertThat(run.totalOutputTokens()).isEqualTo(70);
    }

    @Test
    @DisplayName("토큰합산_stepResults없음_0반환")
    void 토큰합산_stepResults없음_0반환() {
        WorkflowRunResponse run = withStepResults(null);
        assertThat(run.totalInputTokens()).isZero();
    }

    @Test
    @DisplayName("토큰합산_토큰키없는step_0으로무시됨")
    void 토큰합산_토큰키없는step_무시됨() {
        // given — 토큰 없는 step 은 합산에서 빠진다 (Aimbase 가 토큰 미제공한 경우)
        Map<String, Object> steps = new LinkedHashMap<>();
        steps.put("noToken", Map.of("output", "some text"));
        steps.put("withToken", Map.of("input_tokens", 42));
        WorkflowRunResponse run = withStepResults(steps);

        // then
        assertThat(run.totalInputTokens()).isEqualTo(42);
    }

    @Test
    @DisplayName("토큰합산_step값이Map아님_안전하게무시됨")
    void 토큰합산_step값이Map아님_무시됨() {
        // given — stepResults 값이 비정형(문자열)이어도 NPE/ClassCast 없이 처리
        Map<String, Object> steps = new LinkedHashMap<>();
        steps.put("weird", "not a map");
        steps.put("ok", Map.of("input_tokens", 5));
        WorkflowRunResponse run = withStepResults(steps);

        // then
        assertThat(run.totalInputTokens()).isEqualTo(5);
    }
}
