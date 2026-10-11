package com.biddingagency.domain.bid.service;

import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.entity.BidRequestState;
import com.biddingagency.domain.bid.BidRequestNotFoundException;
import com.biddingagency.domain.bid.repository.BidRequestRepository;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.member.repository.MemberRepository;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import com.biddingagency.domain.event.BidRequestCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * TC-BID-001 ~ TC-BID-003: BidRequestService 테스트
 */
@ExtendWith(MockitoExtension.class)
class BidRequestServiceTest {

    @Mock
    private BidRequestRepository bidRequestRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private OpportunityRepository opportunityRepository;
    @Mock
    private BidFSMService fsmService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BidRequestService bidRequestService;

    // TC-BID-001: 입찰 참여 신청
    @Test
    @DisplayName("유효입력_입찰신청_DOCS_PENDING상태로반환_BidRequest생성")
    void 유효입력_createBidRequest_DOCS_PENDING() {
        // given
        UUID memberId = UUID.randomUUID();
        UUID opportunityId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();

        Member member = Member.builder()
                .email("test@test.com").passwordHash("h").companyName("Co").build();
        Opportunity opportunity = Opportunity.builder()
                .noticeId("N-1").title("T").active(true)
                .responseDeadline(LocalDateTime.now().plusDays(30))
                .firstSeenAt(LocalDateTime.now()).lastModifiedAt(LocalDateTime.now()).build();

        given(bidRequestRepository.existsByMemberIdAndOpportunityId(memberId, opportunityId)).willReturn(false);
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(opportunityRepository.findById(opportunityId)).willReturn(Optional.of(opportunity));
        given(bidRequestRepository.save(any(BidRequest.class))).willAnswer(inv -> {
            BidRequest br = inv.getArgument(0);
            return br;
        });
        // 신청 즉시 CREATED → DOCS_PENDING 으로 전이하고 그 결과를 반환한다(고객이 바로 서류 업로드 가능하도록).
        given(fsmService.transition(any(), eq(BidRequestState.DOCS_PENDING), any(), anyString(), anyString()))
                .willAnswer(inv -> {
                    BidRequest br = BidRequest.builder()
                            .member(member)
                            .opportunity(opportunity)
                            .state(BidRequestState.DOCS_PENDING)
                            .build();
                    return br;
                });

        // when
        BidRequest result = bidRequestService.createBidRequest(memberId, opportunityId, createdBy, "admin");

        // then
        assertThat(result.getState()).isEqualTo(BidRequestState.DOCS_PENDING);
        assertThat(result.getMember()).isEqualTo(member);
        assertThat(result.getOpportunity()).isEqualTo(opportunity);
        then(bidRequestRepository).should().save(any(BidRequest.class));
        then(eventPublisher).should().publishEvent(any(BidRequestCreatedEvent.class));
    }

    // TC-BID-002: 동일 공고 중복 신청 (BIZ-006)
    @Test
    @DisplayName("동일공고중복신청_예외발생_BIZ006")
    void 중복신청_createBidRequest_IllegalStateException() {
        // given
        UUID memberId = UUID.randomUUID();
        UUID opportunityId = UUID.randomUUID();
        given(bidRequestRepository.existsByMemberIdAndOpportunityId(memberId, opportunityId)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> bidRequestService.createBidRequest(memberId, opportunityId, UUID.randomUUID(), "admin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already exists");
    }

    // 마감 지난 공고 신청 불가
    @Test
    @DisplayName("마감지난공고_신청시도_예외발생")
    void 마감지남_createBidRequest_예외() {
        // given
        UUID memberId = UUID.randomUUID();
        UUID opportunityId = UUID.randomUUID();
        Member member = Member.builder()
                .email("t@t.com").passwordHash("h").companyName("Co").build();
        Opportunity opportunity = Opportunity.builder()
                .noticeId("N-2").title("T").active(true)
                .responseDeadline(LocalDateTime.now().minusDays(1))
                .firstSeenAt(LocalDateTime.now()).lastModifiedAt(LocalDateTime.now()).build();

        given(bidRequestRepository.existsByMemberIdAndOpportunityId(memberId, opportunityId)).willReturn(false);
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(opportunityRepository.findById(opportunityId)).willReturn(Optional.of(opportunity));

        // when & then
        assertThatThrownBy(() -> bidRequestService.createBidRequest(memberId, opportunityId, UUID.randomUUID(), "admin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deadline has passed");
    }

    // 비활성 공고 신청 불가
    @Test
    @DisplayName("비활성공고_신청시도_예외발생")
    void 비활성공고_createBidRequest_예외() {
        // given
        UUID memberId = UUID.randomUUID();
        UUID opportunityId = UUID.randomUUID();
        Member member = Member.builder()
                .email("t@t.com").passwordHash("h").companyName("Co").build();
        Opportunity opportunity = Opportunity.builder()
                .noticeId("N-3").title("T").active(false)
                .firstSeenAt(LocalDateTime.now()).lastModifiedAt(LocalDateTime.now()).build();

        given(bidRequestRepository.existsByMemberIdAndOpportunityId(memberId, opportunityId)).willReturn(false);
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(opportunityRepository.findById(opportunityId)).willReturn(Optional.of(opportunity));

        // when & then
        assertThatThrownBy(() -> bidRequestService.createBidRequest(memberId, opportunityId, UUID.randomUUID(), "admin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not active");
    }

    // TC-BID-003: 내 입찰 목록 조회
    @Test
    @DisplayName("회원ID_내입찰목록조회_해당고객건만반환")
    void 멤버ID_findByMember_해당건반환() {
        // given
        UUID memberId = UUID.randomUUID();
        Page<BidRequest> page = new PageImpl<>(List.of());
        given(bidRequestRepository.findByMemberId(eq(memberId), any())).willReturn(page);

        // when
        Page<BidRequest> result = bidRequestService.findByMember(memberId, PageRequest.of(0, 20));

        // then
        assertThat(result).isNotNull();
        then(bidRequestRepository).should().findByMemberId(eq(memberId), any());
    }

    // TC-OWN-001 ~ TC-OWN-004: 입찰 건은 낸 회원과 관리자만 (BIZ-060)
    @Test
    @DisplayName("TC-OWN-001 본인건_열기_그입찰건")
    void 본인건_findAccessible_열림() {
        // given
        UUID id = UUID.randomUUID();
        Member owner = member(Member.Role.CUSTOMER);
        BidRequest bidRequest = BidRequest.builder().member(owner).build();
        given(bidRequestRepository.findByIdWithDetails(id)).willReturn(Optional.of(bidRequest));

        // when & then
        assertThat(bidRequestService.findAccessible(id, owner)).isSameAs(bidRequest);
    }

    @Test
    @DisplayName("TC-OWN-002 남의건_열기_404")
    void 남의건_findAccessible_막힘() {
        // given
        UUID id = UUID.randomUUID();
        BidRequest bidRequest = BidRequest.builder().member(member(Member.Role.CUSTOMER)).build();
        given(bidRequestRepository.findByIdWithDetails(id)).willReturn(Optional.of(bidRequest));

        // when & then
        assertThatThrownBy(() -> bidRequestService.findAccessible(id, member(Member.Role.CUSTOMER)))
                .isInstanceOf(BidRequestNotFoundException.class);
    }

    @Test
    @DisplayName("TC-OWN-003 관리자_남의건열기_그입찰건")
    void 관리자_findAccessible_열림() {
        // given
        UUID id = UUID.randomUUID();
        BidRequest bidRequest = BidRequest.builder().member(member(Member.Role.CUSTOMER)).build();
        given(bidRequestRepository.findByIdWithDetails(id)).willReturn(Optional.of(bidRequest));

        // when & then
        assertThat(bidRequestService.findAccessible(id, member(Member.Role.ADMIN))).isSameAs(bidRequest);
    }

    @Test
    @DisplayName("TC-OWN-004 없는건_열기_404")
    void 없는건_findAccessible_막힘() {
        // given
        UUID id = UUID.randomUUID();
        given(bidRequestRepository.findByIdWithDetails(id)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> bidRequestService.findAccessible(id, member(Member.Role.CUSTOMER)))
                .isInstanceOf(BidRequestNotFoundException.class);
    }

    private static Member member(Member.Role role) {
        Member member = Member.builder().role(role).build();
        ReflectionTestUtils.setField(member, "id", UUID.randomUUID());
        return member;
    }
}
