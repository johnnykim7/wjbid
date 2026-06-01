package com.biddingagency.domain.proposal.service;

import com.biddingagency.domain.proposal.entity.GenerationLog;
import com.biddingagency.domain.proposal.entity.GenerationTargetType;
import com.biddingagency.domain.proposal.repository.GenerationLogRepository;
import com.biddingagency.integration.llmplatform.dto.WorkflowRunResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * CR-029: generation_log append-only 기록 + recordStage(단계 라우팅·토큰·cost) 검증.
 */
@ExtendWith(MockitoExtension.class)
class GenerationLogServiceTest {

    @Mock private GenerationLogRepository generationLogRepository;
    @InjectMocks private GenerationLogService generationLogService;

    private WorkflowRunResponse runWith(int in, int out) {
        WorkflowRunResponse r = new WorkflowRunResponse();
        Map<String, Object> steps = new LinkedHashMap<>();
        steps.put("s1", Map.of("input_tokens", in, "output_tokens", out));
        r.setStepResults(steps);
        return r;
    }

    private GenerationLog captureSaved() {
        ArgumentCaptor<GenerationLog> captor = ArgumentCaptor.forClass(GenerationLog.class);
        then(generationLogRepository).should().save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("기록_전달한필드그대로저장")
    void record_필드보존() {
        // given
        UUID targetId = UUID.randomUUID();
        given(generationLogRepository.save(any(GenerationLog.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        generationLogService.record(GenerationTargetType.SECTION, targetId,
                "wf-1", "run-1", "claude-sonnet-4-6", 1000, 2000,
                new BigDecimal("0.012345"), "hash-abc");

        // then
        GenerationLog saved = captureSaved();
        assertThat(saved.getTargetType()).isEqualTo(GenerationTargetType.SECTION);
        assertThat(saved.getTargetId()).isEqualTo(targetId);
        assertThat(saved.getModelName()).isEqualTo("claude-sonnet-4-6");
        assertThat(saved.getCostUsd()).isEqualByComparingTo("0.012345");
    }

    // ── CR-029 recordStage (모델은 호출부가 실측 조회해 인자로 전달) ──────────────

    @Test
    @DisplayName("전달된모델_그대로기록됨")
    void 전달모델_기록() {
        given(generationLogRepository.save(any())).willAnswer(i -> i.getArgument(0));
        generationLogService.recordStage(GenerationTargetType.DESIGN, UUID.randomUUID(),
                runWith(1000, 500), "claude-sonnet-4-20250514");
        assertThat(captureSaved().getModelName()).isEqualTo("claude-sonnet-4-20250514");
    }

    @Test
    @DisplayName("토큰합산_stepResults값이로그에반영됨")
    void 토큰합산_로그반영() {
        given(generationLogRepository.save(any())).willAnswer(i -> i.getArgument(0));
        generationLogService.recordStage(GenerationTargetType.DESIGN, UUID.randomUUID(), runWith(1234, 567), "x");
        GenerationLog log = captureSaved();
        assertThat(log.getTokensIn()).isEqualTo(1234);
        assertThat(log.getTokensOut()).isEqualTo(567);
    }

    @Test
    @DisplayName("비용계산_Haiku모델명_1M입력1M출력_6달러")
    void 비용계산_Haiku() {
        // model 명에 'haiku' 포함 → input $1/MTok + output $5/MTok → $6
        given(generationLogRepository.save(any())).willAnswer(i -> i.getArgument(0));
        generationLogService.recordStage(GenerationTargetType.DESIGN, UUID.randomUUID(),
                runWith(1_000_000, 1_000_000), "claude-haiku-4-5-20251001");
        assertThat(captureSaved().getCostUsd()).isEqualByComparingTo(new BigDecimal("6.000000"));
    }

    @Test
    @DisplayName("비용계산_Sonnet모델명_1M입력1M출력_18달러")
    void 비용계산_Sonnet() {
        // model 명에 'sonnet' 포함 → input $3/MTok + output $15/MTok → $18
        given(generationLogRepository.save(any())).willAnswer(i -> i.getArgument(0));
        generationLogService.recordStage(GenerationTargetType.ASSEMBLE, UUID.randomUUID(),
                runWith(1_000_000, 1_000_000), "claude-sonnet-4-20250514");
        assertThat(captureSaved().getCostUsd()).isEqualByComparingTo(new BigDecimal("18.000000"));
    }

    @Test
    @DisplayName("model null_Sonnet단가폴백_18달러")
    void 비용계산_modelNull_Sonnet폴백() {
        // 모델 조회 실패(null) → 단가는 Sonnet 폴백, model_name 은 null 기록
        given(generationLogRepository.save(any())).willAnswer(i -> i.getArgument(0));
        generationLogService.recordStage(GenerationTargetType.DESIGN, UUID.randomUUID(),
                runWith(1_000_000, 1_000_000), null);
        GenerationLog log = captureSaved();
        assertThat(log.getModelName()).isNull();
        assertThat(log.getCostUsd()).isEqualByComparingTo(new BigDecimal("18.000000"));
    }

    @Test
    @DisplayName("응답null_토큰0비용0으로기록됨")
    void 응답null_0으로기록() {
        given(generationLogRepository.save(any())).willAnswer(i -> i.getArgument(0));
        generationLogService.recordStage(GenerationTargetType.DESIGN, UUID.randomUUID(), null, "x");
        GenerationLog log = captureSaved();
        assertThat(log.getTokensIn()).isZero();
        assertThat(log.getCostUsd()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
