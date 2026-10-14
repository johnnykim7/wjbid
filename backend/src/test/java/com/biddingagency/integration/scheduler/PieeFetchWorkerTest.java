package com.biddingagency.integration.scheduler;

import com.biddingagency.domain.opportunity.entity.AttachmentDownloadStatus;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.PieeFetchStatus;
import com.biddingagency.domain.opportunity.repository.OpportunityAttachmentRepository;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import com.biddingagency.domain.opportunity.service.PieeFetchService;
import com.biddingagency.domain.opportunity.service.PieeSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * TC-PIEE-016 ~ TC-PIEE-019 (CR-160): PIEE 받기 일꾼이 받을 공고를 고르는 규칙(BIZ-079)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PieeFetchWorkerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 14, 13, 0);

    @Mock
    private OpportunityRepository opportunityRepository;
    @Mock
    private OpportunityAttachmentRepository attachmentRepository;
    @Mock
    private PieeFetchService pieeFetchService;
    @Mock
    private PieeSettings settings;
    @InjectMocks
    private PieeFetchWorker worker;

    private Opportunity waiting;

    @BeforeEach
    void setUp() {
        given(settings.now()).willReturn(NOW);
        given(settings.timeout()).willReturn(Duration.ofSeconds(900));
        given(settings.samWait()).willReturn(Duration.ofMinutes(10));
        waiting = Opportunity.builder().noticeId("N-1").solicitationNumber("W1").title("t").active(true)
                .firstSeenAt(NOW).lastModifiedAt(NOW).build();
        ReflectionTestUtils.setField(waiting, "id", UUID.randomUUID());
        given(opportunityRepository.findByPieeFetchStatusOrderByPieeFetchStatusAtAsc(PieeFetchStatus.WAITING))
                .willReturn(List.of(waiting));
        given(attachmentRepository.countByOpportunityIdAndDownloadStatus(waiting.getId(), AttachmentDownloadStatus.PENDING))
                .willReturn(1L);
    }

    private void waitingSince(LocalDateTime at) {
        waiting.waitPieeFetch(at);
    }

    // TC-PIEE-016
    @Test
    @DisplayName("SAM첨부받는중_미룸")
    void SAM첨부받는중_미룸() {
        waitingSince(NOW.minusMinutes(3));

        worker.tick();

        verify(opportunityRepository, never()).changePieeFetchStatus(any(), any(), any(), any());
        verify(pieeFetchService, never()).fetchAsync(any());
    }

    // TC-PIEE-017
    @Test
    @DisplayName("SAM기다림상한지남_받기시작")
    void SAM기다림상한지남_받기시작() {
        waitingSince(NOW.minusMinutes(11));
        given(opportunityRepository.changePieeFetchStatus(waiting.getId(), PieeFetchStatus.WAITING,
                PieeFetchStatus.FETCHING, NOW)).willReturn(1);

        worker.tick();

        verify(pieeFetchService).fetchAsync(waiting.getId());
    }

    // TC-PIEE-018
    @Test
    @DisplayName("받는중멈춤_받기실패")
    void 받는중멈춤_받기실패() {
        waitingSince(NOW.minusMinutes(3));

        worker.tick();

        verify(opportunityRepository).failStalePieeFetch(PieeFetchStatus.FETCHING, PieeFetchStatus.FAILED,
                NOW.minusSeconds(1800), NOW, "받는 중 멈춤");
    }

    // TC-PIEE-019
    @Test
    @DisplayName("다른일꾼이먼저잡음_받지않음")
    void 다른일꾼이먼저잡음_받지않음() {
        waitingSince(NOW.minusMinutes(11));
        given(opportunityRepository.changePieeFetchStatus(eq(waiting.getId()), any(), any(), any())).willReturn(0);

        worker.tick();

        verify(pieeFetchService, never()).fetchAsync(any());
        assertThat(waiting.getPieeFetchStatus()).isEqualTo(PieeFetchStatus.WAITING);
    }
}
