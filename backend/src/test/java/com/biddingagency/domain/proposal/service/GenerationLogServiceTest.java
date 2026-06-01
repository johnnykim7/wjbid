package com.biddingagency.domain.proposal.service;

import com.biddingagency.domain.proposal.entity.GenerationLog;
import com.biddingagency.domain.proposal.entity.GenerationTargetType;
import com.biddingagency.domain.proposal.repository.GenerationLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * CR-029 선반영: generation_log append-only 기록 검증.
 */
@ExtendWith(MockitoExtension.class)
class GenerationLogServiceTest {

    @Mock private GenerationLogRepository generationLogRepository;
    @InjectMocks private GenerationLogService generationLogService;

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
        ArgumentCaptor<GenerationLog> captor = ArgumentCaptor.forClass(GenerationLog.class);
        then(generationLogRepository).should().save(captor.capture());
        GenerationLog saved = captor.getValue();
        assertThat(saved.getTargetType()).isEqualTo(GenerationTargetType.SECTION);
        assertThat(saved.getTargetId()).isEqualTo(targetId);
        assertThat(saved.getModelName()).isEqualTo("claude-sonnet-4-6");
        assertThat(saved.getCostUsd()).isEqualByComparingTo("0.012345");
    }
}
