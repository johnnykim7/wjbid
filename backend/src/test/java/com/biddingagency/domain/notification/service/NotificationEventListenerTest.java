package com.biddingagency.domain.notification.service;

import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.entity.BidRequestState;
import com.biddingagency.domain.bid.repository.BidRequestRepository;
import com.biddingagency.domain.event.BidRequestCreatedEvent;
import com.biddingagency.domain.event.BidRequestStateChangedEvent;
import com.biddingagency.domain.event.DeadlineApproachingEvent;
import com.biddingagency.domain.event.OpportunitiesCollectedEvent;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.member.repository.MemberRepository;
import com.biddingagency.domain.notification.entity.NotificationType;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private OpportunityRepository opportunityRepository;
    @Mock
    private BidRequestRepository bidRequestRepository;

    @InjectMocks
    private NotificationEventListener listener;

    private Member createAdmin() {
        return Member.builder()
                .email("admin@test.com")
                .passwordHash("hash")
                .companyName("Agency")
                .role(Member.Role.ADMIN)
                .build();
    }

    @Test
    @DisplayName("공고수집완료이벤트_어드민에게알림발송")
    void onOpportunitiesCollected_어드민알림() {
        // given
        Member admin = createAdmin();
        given(memberRepository.findByRole(Member.Role.ADMIN)).willReturn(List.of(admin));

        OpportunitiesCollectedEvent event = new OpportunitiesCollectedEvent(null, 5, 5, 2, 7);

        // when
        listener.onOpportunitiesCollected(event);

        // then
        then(notificationService).should().sendNotification(
                eq(NotificationType.COLLECTION_COMPLETE),
                eq("admin@test.com"), any(),
                contains("수집 완료"),
                any(),
                any(), eq("CollectorRun"),
                anyString()
        );
    }

    @Test
    @DisplayName("공고수집완료_타깃신규0건_알림미발송")
    void onOpportunitiesCollected_타깃신규0건_스킵() {
        // given (CR-015): 전체 신규는 12건이지만 전부 시설관리 무관 부품조달(타깃 신규 0)인 경우 — 노이즈 메일 차단
        OpportunitiesCollectedEvent event = new OpportunitiesCollectedEvent(null, 12, 0, 5, 17);

        // when
        listener.onOpportunitiesCollected(event);

        // then: ADMIN 조회조차 하지 않고 즉시 리턴, 알림 미발송
        then(memberRepository).shouldHaveNoInteractions();
        then(notificationService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("입찰신청이벤트_어드민에게알림발송")
    void onBidRequestCreated_어드민알림() {
        // given
        Member admin = createAdmin();
        given(memberRepository.findByRole(Member.Role.ADMIN)).willReturn(List.of(admin));

        UUID bidRequestId = UUID.randomUUID();
        BidRequestCreatedEvent event = new BidRequestCreatedEvent(
                bidRequestId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        // when
        listener.onBidRequestCreated(event);

        // then
        then(notificationService).should().sendNotification(
                eq(NotificationType.BID_REQUEST_CREATED),
                eq("admin@test.com"), any(),
                contains("입찰 신청"),
                any(),
                eq(bidRequestId), eq("BidRequest"),
                anyString()
        );
    }

    @Test
    @DisplayName("마감D7이벤트_DEADLINE_D7타입으로알림")
    void onDeadlineApproaching_D7_타입매핑() {
        // given
        Member admin = createAdmin();
        given(memberRepository.findByRole(Member.Role.ADMIN)).willReturn(List.of(admin));

        UUID bidRequestId = UUID.randomUUID();
        DeadlineApproachingEvent event = new DeadlineApproachingEvent(
                bidRequestId, 7, LocalDateTime.now().plusDays(7));

        // when
        listener.onDeadlineApproaching(event);

        // then
        then(notificationService).should().sendNotification(
                eq(NotificationType.DEADLINE_D7),
                eq("admin@test.com"), any(),
                contains("D-7"),
                any(),
                eq(bidRequestId), eq("BidRequest"),
                contains("DEADLINE_D7")
        );
    }

    @Test
    @DisplayName("마감D1이벤트_DEADLINE_D1타입으로알림")
    void onDeadlineApproaching_D1_타입매핑() {
        // given
        Member admin = createAdmin();
        given(memberRepository.findByRole(Member.Role.ADMIN)).willReturn(List.of(admin));

        UUID bidRequestId = UUID.randomUUID();
        DeadlineApproachingEvent event = new DeadlineApproachingEvent(
                bidRequestId, 1, LocalDateTime.now().plusDays(1));

        // when
        listener.onDeadlineApproaching(event);

        // then
        then(notificationService).should().sendNotification(
                eq(NotificationType.DEADLINE_D1),
                anyString(), any(),
                contains("D-1"),
                any(),
                eq(bidRequestId), eq("BidRequest"),
                contains("DEADLINE_D1")
        );
    }

    @Test
    @DisplayName("어드민없음_알림미발송")
    void 어드민없음_onBidRequestCreated_알림미발송() {
        // given
        given(memberRepository.findByRole(Member.Role.ADMIN)).willReturn(List.of());

        BidRequestCreatedEvent event = new BidRequestCreatedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        // when
        listener.onBidRequestCreated(event);

        // then
        then(notificationService).shouldHaveNoInteractions();
    }

    // CR-018: BidRequest 상태 전이 → 고객 알림

    private Member createCustomer() {
        return Member.builder()
                .email("customer@test.com")
                .passwordHash("hash")
                .companyName("ClientCo")
                .contactPerson("김고객")
                .role(Member.Role.CUSTOMER)
                .build();
    }

    private BidRequest createBidRequestWithCustomer(UUID id, BidRequestState state) {
        Opportunity opp = Opportunity.builder()
                .title("USFK Facility Maintenance")
                .noticeId("W912-26-R-0001")
                .responseDeadline(LocalDateTime.now().plusDays(10))
                .build();
        BidRequest br = BidRequest.builder()
                .member(createCustomer())
                .opportunity(opp)
                .state(state)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(br, "id", id);
        return br;
    }

    @Test
    @DisplayName("상태전이REVIEW_고객에게생성완료알림_CR018")
    void onBidRequestStateChanged_REVIEW_생성완료알림() {
        // given
        UUID bidRequestId = UUID.randomUUID();
        given(bidRequestRepository.findByIdWithDetails(bidRequestId))
                .willReturn(Optional.of(createBidRequestWithCustomer(bidRequestId, BidRequestState.REVIEW)));
        BidRequestStateChangedEvent event = new BidRequestStateChangedEvent(
                bidRequestId, BidRequestState.GENERATING, BidRequestState.REVIEW, UUID.randomUUID());

        // when
        listener.onBidRequestStateChanged(event);

        // then
        then(notificationService).should().sendNotification(
                eq(NotificationType.PROPOSAL_READY_CUSTOMER),
                eq("customer@test.com"), any(),
                contains("제안서 생성"),
                any(),
                eq(bidRequestId), eq("BidRequest"),
                eq("BR_STATE_" + bidRequestId + "_REVIEW")
        );
    }

    @Test
    @DisplayName("상태전이SUBMITTED_고객에게접수알림_CR018")
    void onBidRequestStateChanged_SUBMITTED_접수알림() {
        // given
        UUID bidRequestId = UUID.randomUUID();
        given(bidRequestRepository.findByIdWithDetails(bidRequestId))
                .willReturn(Optional.of(createBidRequestWithCustomer(bidRequestId, BidRequestState.SUBMITTED)));
        BidRequestStateChangedEvent event = new BidRequestStateChangedEvent(
                bidRequestId, BidRequestState.CONFIRMED, BidRequestState.SUBMITTED, UUID.randomUUID());

        // when
        listener.onBidRequestStateChanged(event);

        // then
        then(notificationService).should().sendNotification(
                eq(NotificationType.BID_SUBMITTED_CUSTOMER),
                eq("customer@test.com"), any(),
                contains("접수"),
                any(),
                eq(bidRequestId), eq("BidRequest"),
                eq("BR_STATE_" + bidRequestId + "_SUBMITTED")
        );
    }

    @Test
    @DisplayName("상태전이AWARDED_고객에게합격알림_CR018")
    void onBidRequestStateChanged_AWARDED_합격알림() {
        // given
        UUID bidRequestId = UUID.randomUUID();
        given(bidRequestRepository.findByIdWithDetails(bidRequestId))
                .willReturn(Optional.of(createBidRequestWithCustomer(bidRequestId, BidRequestState.AWARDED)));
        BidRequestStateChangedEvent event = new BidRequestStateChangedEvent(
                bidRequestId, BidRequestState.SUBMITTED, BidRequestState.AWARDED, UUID.randomUUID());

        // when
        listener.onBidRequestStateChanged(event);

        // then
        then(notificationService).should().sendNotification(
                eq(NotificationType.BID_AWARDED_CUSTOMER),
                eq("customer@test.com"), any(),
                contains("합격"),
                any(),
                eq(bidRequestId), eq("BidRequest"),
                eq("BR_STATE_" + bidRequestId + "_AWARDED")
        );
    }

    @Test
    @DisplayName("상태전이NOT_AWARDED_고객알림미발송_MVP_CR018")
    void onBidRequestStateChanged_NOT_AWARDED_미발송() {
        // given
        BidRequestStateChangedEvent event = new BidRequestStateChangedEvent(
                UUID.randomUUID(), BidRequestState.SUBMITTED, BidRequestState.NOT_AWARDED, UUID.randomUUID());

        // when
        listener.onBidRequestStateChanged(event);

        // then: 미대상 상태는 BidRequest 조회조차 안 하고 즉시 리턴
        then(bidRequestRepository).shouldHaveNoInteractions();
        then(notificationService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("상태전이DOCS_RECEIVED_고객알림미발송_CR018")
    void onBidRequestStateChanged_DOCS_RECEIVED_미발송() {
        // given — 알림 대상이 아닌 중간 상태
        BidRequestStateChangedEvent event = new BidRequestStateChangedEvent(
                UUID.randomUUID(), BidRequestState.DOCS_PENDING, BidRequestState.DOCS_RECEIVED, UUID.randomUUID());

        // when
        listener.onBidRequestStateChanged(event);

        // then
        then(bidRequestRepository).shouldHaveNoInteractions();
        then(notificationService).shouldHaveNoInteractions();
    }
}
