package com.biddingagency.domain.notification.service;

import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.entity.BidRequestState;
import com.biddingagency.domain.bid.repository.BidRequestRepository;
import com.biddingagency.domain.event.BidRequestCreatedEvent;
import com.biddingagency.domain.event.BidRequestStateChangedEvent;
import com.biddingagency.domain.event.CollectionFailedEvent;
import com.biddingagency.domain.event.DeadlineApproachingEvent;
import com.biddingagency.domain.event.OpportunitiesCollectedEvent;
import com.biddingagency.domain.event.OpportunityApprovedEvent;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.member.repository.MemberRepository;
import com.biddingagency.domain.member.service.MembershipService;
import com.biddingagency.domain.notification.entity.NotificationType;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
    @Mock
    private MembershipService membershipService;

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
    @DisplayName("공고수집완료_관리자둘_회차와관리자별멱등키로각각발송")
    void onOpportunitiesCollected_관리자별_멱등키() {
        // CR-163: 키가 회차 ID 하나뿐이면 첫 관리자만 받고, 회차 ID 가 비면 첫 회차 뒤로 모두 막힌다
        UUID runId = UUID.fromString("00000000-0000-0000-0000-000000000163");
        Member first = createAdmin();
        Member second = createAdmin();
        ReflectionTestUtils.setField(first, "id", UUID.fromString("00000000-0000-0000-0000-00000000000a"));
        ReflectionTestUtils.setField(second, "id", UUID.fromString("00000000-0000-0000-0000-00000000000b"));
        given(memberRepository.findByRole(Member.Role.ADMIN)).willReturn(List.of(first, second));

        listener.onOpportunitiesCollected(new OpportunitiesCollectedEvent(runId, 5, 5, 2, 7));

        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
        then(notificationService).should(times(2)).sendNotification(
                eq(NotificationType.COLLECTION_COMPLETE), anyString(), any(), anyString(), any(),
                eq(runId), eq("CollectorRun"), keys.capture());
        assertThat(keys.getAllValues()).containsExactly(
                "COLLECTION_" + runId + "_" + first.getId(),
                "COLLECTION_" + runId + "_" + second.getId());
    }

    // CR-126: 수집 실패 알림 — 성공 이벤트의 0건 스킵에 묻히지 않아야 한다

    @Test
    @DisplayName("수집중_401발생_실패알림발행됨")
    void onCollectionFailed_401_실패알림발송() {
        // given: 2026-08-03 키 만료 재현 — search 가 401 Invalid Credentials
        Member admin = createAdmin();
        given(memberRepository.findByRole(Member.Role.ADMIN)).willReturn(List.of(admin));

        CollectionFailedEvent event = new CollectionFailedEvent(
                "411TH CSB", 4, 401,
                "{\"code\":\"900901\",\"message\":\"Invalid Credentials\"}", true);

        // when
        listener.onCollectionFailed(event);

        // then: 키 확인이 필요하다는 제목으로 관리자에게 발송
        then(notificationService).should().sendNotification(
                eq(NotificationType.COLLECTION_FAILED),
                eq("admin@test.com"), any(),
                contains("API 키 확인 필요"),
                any(),
                any(), eq("CollectorRun"),
                contains("COLLECTION_FAIL_411TH CSB_")
        );
    }

    @Test
    @DisplayName("수집중_401발생_키만료문구포함됨")
    @SuppressWarnings("unchecked")
    void onCollectionFailed_401_재발급절차_문구포함() {
        // given
        given(memberRepository.findByRole(Member.Role.ADMIN)).willReturn(List.of(createAdmin()));
        CollectionFailedEvent event = new CollectionFailedEvent("411TH CSB", 4, 401, "body", true);

        // when
        listener.onCollectionFailed(event);

        // then: 사람이 바로 조치할 수 있도록 재발급 절차가 변수에 담긴다
        ArgumentCaptor<Map<String, Object>> varsCaptor = ArgumentCaptor.forClass(Map.class);
        then(notificationService).should().sendNotification(
                any(), anyString(), any(), anyString(), varsCaptor.capture(),
                any(), anyString(), anyString());

        Map<String, Object> vars = varsCaptor.getValue();
        assertThat((String) vars.get("reason")).contains("만료");
        assertThat((String) vars.get("actionGuide"))
                .contains("SAM_GOV_API_KEY")
                .contains(".env");
        assertThat(vars.get("lastStatus")).isEqualTo("401");
    }

    @Test
    @DisplayName("같은날_수집2회실패_알림1통만발송됨")
    void onCollectionFailed_동일일자_멱등키동일() {
        // given: 하루 4회 수집이 모두 실패해도 멱등키가 같아야 NotificationService(BIZ-012)가 2통째를 막는다
        given(memberRepository.findByRole(Member.Role.ADMIN)).willReturn(List.of(createAdmin()));
        CollectionFailedEvent first = new CollectionFailedEvent("411TH CSB", 4, 401, "body", true);
        CollectionFailedEvent second = new CollectionFailedEvent("411TH CSB", 4, 401, "body", true);

        // when
        listener.onCollectionFailed(first);
        listener.onCollectionFailed(second);

        // then
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        then(notificationService).should(times(2)).sendNotification(
                any(), anyString(), any(), anyString(), any(),
                any(), anyString(), keyCaptor.capture());

        List<String> keys = keyCaptor.getAllValues();
        assertThat(keys.get(0)).isEqualTo(keys.get(1));
    }

    @Test
    @DisplayName("수집중_네트워크오류_일반오류문구발송됨")
    @SuppressWarnings("unchecked")
    void onCollectionFailed_status없음_일반오류문구() {
        // given: UnknownHostException 등 — 응답을 못 받아 status 가 null
        given(memberRepository.findByRole(Member.Role.ADMIN)).willReturn(List.of(createAdmin()));
        CollectionFailedEvent event = new CollectionFailedEvent("411TH CSB", 1, null, null, false);

        // when
        listener.onCollectionFailed(event);

        // then: 키 만료로 오인시키지 않는다
        ArgumentCaptor<Map<String, Object>> varsCaptor = ArgumentCaptor.forClass(Map.class);
        then(notificationService).should().sendNotification(
                any(), anyString(), any(), contains("수집 오류"), varsCaptor.capture(),
                any(), anyString(), anyString());

        Map<String, Object> vars = varsCaptor.getValue();
        assertThat((String) vars.get("reason")).doesNotContain("만료");
        assertThat(vars.get("lastStatus")).isEqualTo("-");
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

    // CR-156: 마감 알림은 관리자 전원과 그 요청을 낸 고객에게, 받는 사람마다 한 번

    private static final LocalDateTime DEADLINE = LocalDateTime.of(2026, 10, 19, 17, 0);

    private Member withId(Member m) {
        org.springframework.test.util.ReflectionTestUtils.setField(m, "id", UUID.randomUUID());
        return m;
    }

    private Member admin(String email) {
        return withId(Member.builder().email(email).passwordHash("hash").companyName("Agency")
                .role(Member.Role.ADMIN).build());
    }

    private BidRequest deadlineRequest(UUID id, Member customer) {
        BidRequest br = BidRequest.builder()
                .member(customer)
                .opportunity(Opportunity.builder().title("USFK Facility Maintenance").noticeId("W912-26-R-0001")
                        .responseDeadline(DEADLINE).build())
                .state(BidRequestState.DOCS_PENDING)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(br, "id", id);
        return br;
    }

    private ArgumentCaptor<String> sentTo() {
        ArgumentCaptor<String> emails = ArgumentCaptor.forClass(String.class);
        then(notificationService).should(atLeastOnce()).sendNotification(eq(NotificationType.DEADLINE_ALERT),
                emails.capture(), any(), anyString(), any(), any(), eq("BidRequest"), anyString());
        return emails;
    }

    @Test
    @DisplayName("TC-DEADLINE-010 마감알림_관리자전원과고객에게_발송")
    void 마감알림_관리자전원과고객_각각발송() {
        UUID bidRequestId = UUID.randomUUID();
        given(memberRepository.findByRole(Member.Role.ADMIN))
                .willReturn(List.of(admin("a1@test.com"), admin("a2@test.com")));
        given(bidRequestRepository.findByIdWithDetails(bidRequestId))
                .willReturn(Optional.of(deadlineRequest(bidRequestId, withId(createCustomer()))));

        listener.onDeadlineApproaching(new DeadlineApproachingEvent(bidRequestId, 7, DEADLINE));

        assertThat(sentTo().getAllValues()).containsExactly("a1@test.com", "a2@test.com", "customer@test.com");
    }

    @Test
    @DisplayName("TC-DEADLINE-004 마감알림_받는사람마다_마감일과시점을담은멱등키")
    void 마감알림_받는사람마다_다른멱등키() {
        UUID bidRequestId = UUID.randomUUID();
        Member a1 = admin("a1@test.com");
        Member a2 = admin("a2@test.com");
        Member customer = withId(createCustomer());
        given(memberRepository.findByRole(Member.Role.ADMIN)).willReturn(List.of(a1, a2));
        given(bidRequestRepository.findByIdWithDetails(bidRequestId))
                .willReturn(Optional.of(deadlineRequest(bidRequestId, customer)));

        listener.onDeadlineApproaching(new DeadlineApproachingEvent(bidRequestId, 3, DEADLINE));

        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
        then(notificationService).should(times(3)).sendNotification(any(), anyString(), any(), anyString(), any(),
                any(), anyString(), keys.capture());
        String prefix = "DEADLINE_" + bidRequestId + "_2026-10-19_3_";
        assertThat(keys.getAllValues())
                .containsExactly(prefix + a1.getId(), prefix + a2.getId(), prefix + customer.getId());
    }

    @Test
    @DisplayName("TC-DEADLINE-010 요청한고객이관리자_한번만발송")
    void 요청한고객이관리자_마감알림_한번만발송() {
        UUID bidRequestId = UUID.randomUUID();
        Member a1 = admin("a1@test.com");
        given(memberRepository.findByRole(Member.Role.ADMIN)).willReturn(List.of(a1));
        given(bidRequestRepository.findByIdWithDetails(bidRequestId))
                .willReturn(Optional.of(deadlineRequest(bidRequestId, a1)));

        listener.onDeadlineApproaching(new DeadlineApproachingEvent(bidRequestId, 1, DEADLINE));

        assertThat(sentTo().getAllValues()).containsExactly("a1@test.com");
    }

    @Test
    @DisplayName("TC-DEADLINE-010 입찰요청없음_알림미발송")
    void 입찰요청없음_마감알림_미발송() {
        UUID bidRequestId = UUID.randomUUID();
        given(bidRequestRepository.findByIdWithDetails(bidRequestId)).willReturn(Optional.empty());

        listener.onDeadlineApproaching(new DeadlineApproachingEvent(bidRequestId, 7, DEADLINE));

        then(notificationService).shouldHaveNoInteractions();
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
                contains("제안서가 준비"),
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
    @DisplayName("상태전이AWARDED_고객에게낙찰알림_CR018")
    void onBidRequestStateChanged_AWARDED_낙찰알림() {
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
                contains("낙찰"),
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

    @Test
    @DisplayName("공고노출승인_고객알림_상세주소는원본공고상세_CR158")
    void onOpportunityApproved_상세주소_원본공고상세() {
        // given
        UUID opportunityId = UUID.randomUUID();
        Opportunity opp = Opportunity.builder()
                .title("USFK Facility Maintenance")
                .noticeId("W912-26-R-0001")
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(opp, "id", opportunityId);
        org.springframework.test.util.ReflectionTestUtils.setField(listener, "customerPortalUrl", "https://portal.test");
        given(opportunityRepository.findById(opportunityId)).willReturn(Optional.of(opp));
        Member customer = withId(createCustomer());
        given(memberRepository.findByRole(Member.Role.CUSTOMER)).willReturn(List.of(customer));
        given(membershipService.currentEndsOn(any())).willReturn(Map.of(customer.getId(), LocalDate.of(2026, 12, 31)));
        ArgumentCaptor<Map<String, Object>> vars = ArgumentCaptor.forClass(Map.class);

        // when
        listener.onOpportunityApproved(new OpportunityApprovedEvent(opportunityId, UUID.randomUUID()));

        // then
        then(notificationService).should().sendNotification(
                eq(NotificationType.OPPORTUNITY_APPROVED), eq("customer@test.com"), any(),
                any(), vars.capture(), eq(opportunityId), eq("Opportunity"), any());
        assertThat(vars.getValue().get("detailUrl")).isEqualTo("https://portal.test/search/origin/" + opportunityId);
    }

    private UUID approvedOpportunity() {
        UUID opportunityId = UUID.randomUUID();
        Opportunity opp = Opportunity.builder().title("USFK Facility Maintenance").noticeId("W912-26-R-0001").build();
        org.springframework.test.util.ReflectionTestUtils.setField(opp, "id", opportunityId);
        given(opportunityRepository.findById(opportunityId)).willReturn(Optional.of(opp));
        return opportunityId;
    }

    @Test
    @DisplayName("공고노출승인_무료회원만있음_신규공고알림미발송_CR161")
    void 무료회원만있음_신규공고알림_미발송() {
        UUID opportunityId = approvedOpportunity();
        given(memberRepository.findByRole(Member.Role.CUSTOMER)).willReturn(List.of(withId(createCustomer())));
        given(membershipService.currentEndsOn(any())).willReturn(Map.of());

        listener.onOpportunityApproved(new OpportunityApprovedEvent(opportunityId, UUID.randomUUID()));

        then(notificationService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("공고노출승인_회원과무료회원섞임_회원에게만발송_CR161")
    void 회원과무료회원섞임_신규공고알림_회원에게만발송() {
        UUID opportunityId = approvedOpportunity();
        Member paid = withId(Member.builder().email("paid@test.com").passwordHash("hash")
                .companyName("PaidCo").role(Member.Role.CUSTOMER).build());
        Member free = withId(Member.builder().email("free@test.com").passwordHash("hash")
                .companyName("FreeCo").role(Member.Role.CUSTOMER).build());
        given(memberRepository.findByRole(Member.Role.CUSTOMER)).willReturn(List.of(paid, free));
        given(membershipService.currentEndsOn(any())).willReturn(Map.of(paid.getId(), LocalDate.of(2026, 12, 31)));
        ArgumentCaptor<String> emails = ArgumentCaptor.forClass(String.class);

        listener.onOpportunityApproved(new OpportunityApprovedEvent(opportunityId, UUID.randomUUID()));

        then(notificationService).should(atLeastOnce()).sendNotification(eq(NotificationType.OPPORTUNITY_APPROVED),
                emails.capture(), any(), any(), any(), eq(opportunityId), eq("Opportunity"), any());
        assertThat(emails.getAllValues()).containsExactly("paid@test.com");
    }
}
