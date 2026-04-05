package com.biddingagency.domain.bid.service;

import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.entity.BidRequestState;
import com.biddingagency.domain.bid.repository.BidRequestRepository;
import com.biddingagency.domain.event.BidRequestStateChangedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * TC-FSM-001 ~ TC-FSM-014: BidFSMService 테스트 (화이트리스트 FSM)
 */
@ExtendWith(MockitoExtension.class)
class BidFSMServiceTest {

    @Mock
    private BidRequestRepository bidRequestRepository;

    @Mock
    private AIWorkflowService aiWorkflowService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BidFSMService fsmService;

    // TC-FSM-001 ~ TC-FSM-010: 허용 전이 (화이트리스트)
    @ParameterizedTest(name = "허용전이: {0} → {1}")
    @CsvSource({
            "CREATED, DOCS_PENDING",
            "DOCS_PENDING, DOCS_RECEIVED",
            "DOCS_RECEIVED, ANALYZING",
            "ANALYZING, GENERATING",
            "GENERATING, REVIEW",
            "REVIEW, CONFIRMED",
            "CONFIRMED, SUBMITTED",
            "REVIEW, GENERATING",
            "CONFIRMED, REVIEW",
            "CREATED, CLOSED",
            "DOCS_PENDING, CLOSED",
            "DOCS_RECEIVED, CLOSED"
    })
    @DisplayName("허용전이_canTransition_true_BIZ001")
    void 허용전이_canTransition_true(BidRequestState from, BidRequestState to) {
        assertThat(fsmService.canTransition(from, to)).isTrue();
    }

    // TC-FSM-011 ~ TC-FSM-013: 금지 전이
    @ParameterizedTest(name = "금지전이: {0} → {1}")
    @CsvSource({
            "SUBMITTED, REVIEW",
            "CLOSED, CREATED",
            "ANALYZING, CLOSED",
            "GENERATING, CLOSED",
            "SUBMITTED, CLOSED",
            "CLOSED, DOCS_PENDING",
            "DOCS_PENDING, ANALYZING",
            "CREATED, REVIEW",
            "CREATED, SUBMITTED"
    })
    @DisplayName("금지전이_canTransition_false_BIZ001")
    void 금지전이_canTransition_false(BidRequestState from, BidRequestState to) {
        assertThat(fsmService.canTransition(from, to)).isFalse();
    }

    // TC-FSM-001: 전이 실행 → 상태 변경
    @Test
    @DisplayName("CREATED에서DOCS_PENDING_전이실행_상태변경_stateHistory추가")
    void CREATED_DOCS_PENDING_전이_상태변경() {
        // given
        UUID bidRequestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BidRequest bidRequest = BidRequest.builder()
                .state(BidRequestState.CREATED)
                .stateHistory(new ArrayList<>())
                .build();
        given(bidRequestRepository.findById(bidRequestId)).willReturn(Optional.of(bidRequest));
        given(bidRequestRepository.save(any(BidRequest.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        BidRequest result = fsmService.transition(bidRequestId, BidRequestState.DOCS_PENDING, userId, "admin", "문서 요청");

        // then
        assertThat(result.getState()).isEqualTo(BidRequestState.DOCS_PENDING);
        assertThat(result.getStateHistory()).isNotEmpty();
        then(eventPublisher).should().publishEvent(any(BidRequestStateChangedEvent.class));
    }

    // TC-FSM-007: CONFIRMED → SUBMITTED 전이 → submittedAt 기록
    @Test
    @DisplayName("CONFIRMED에서SUBMITTED_전이_submittedAt기록")
    void CONFIRMED_SUBMITTED_전이_submittedAt기록() {
        // given
        UUID bidRequestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BidRequest bidRequest = BidRequest.builder()
                .state(BidRequestState.CONFIRMED)
                .stateHistory(new ArrayList<>())
                .build();
        given(bidRequestRepository.findById(bidRequestId)).willReturn(Optional.of(bidRequest));
        given(bidRequestRepository.save(any(BidRequest.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        BidRequest result = fsmService.transition(bidRequestId, BidRequestState.SUBMITTED, userId, "admin", "제출");

        // then
        assertThat(result.getState()).isEqualTo(BidRequestState.SUBMITTED);
        assertThat(result.getSubmittedAt()).isNotNull();
    }

    // TC-FSM-011: 금지 전이 실행 시 예외
    @Test
    @DisplayName("SUBMITTED에서REVIEW_전이시도_IllegalStateException_BIZ001")
    void SUBMITTED_REVIEW_전이_예외() {
        // given
        UUID bidRequestId = UUID.randomUUID();
        BidRequest bidRequest = BidRequest.builder()
                .state(BidRequestState.SUBMITTED)
                .stateHistory(new ArrayList<>())
                .build();
        given(bidRequestRepository.findById(bidRequestId)).willReturn(Optional.of(bidRequest));

        // when & then
        assertThatThrownBy(() -> fsmService.transition(bidRequestId, BidRequestState.REVIEW, UUID.randomUUID(), "admin", ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid state transition");
    }

    // TC-FSM-012: CLOSED → CREATED 금지
    @Test
    @DisplayName("CLOSED에서CREATED_전이시도_IllegalStateException")
    void CLOSED_CREATED_전이_예외() {
        // given
        UUID bidRequestId = UUID.randomUUID();
        BidRequest bidRequest = BidRequest.builder()
                .state(BidRequestState.CLOSED)
                .stateHistory(new ArrayList<>())
                .build();
        given(bidRequestRepository.findById(bidRequestId)).willReturn(Optional.of(bidRequest));

        // when & then
        assertThatThrownBy(() -> fsmService.transition(bidRequestId, BidRequestState.CREATED, UUID.randomUUID(), "admin", ""))
                .isInstanceOf(IllegalStateException.class);
    }

    // 터미널 상태 검증
    @Test
    @DisplayName("SUBMITTED_터미널상태_다음전이없음")
    void SUBMITTED_getValidNextStates_빈목록() {
        assertThat(fsmService.getValidNextStates(BidRequestState.SUBMITTED)).isEmpty();
    }

    @Test
    @DisplayName("CLOSED_터미널상태_다음전이없음")
    void CLOSED_getValidNextStates_빈목록() {
        assertThat(fsmService.getValidNextStates(BidRequestState.CLOSED)).isEmpty();
    }
}
