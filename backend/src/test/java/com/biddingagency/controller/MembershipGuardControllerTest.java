package com.biddingagency.controller;

import com.biddingagency.TestHelper;
import com.biddingagency.domain.bid.service.BidFSMService;
import com.biddingagency.domain.bid.service.BidRequestService;
import com.biddingagency.domain.document.service.DocumentVersionService;
import com.biddingagency.domain.member.MembershipError;
import com.biddingagency.domain.member.MembershipException;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.member.service.MembershipService;
import com.biddingagency.domain.notice.service.NoticeService;
import com.biddingagency.domain.opportunity.repository.OpportunityAttachmentRepository;
import com.biddingagency.domain.opportunity.repository.OpportunityRequirementItemRepository;
import com.biddingagency.domain.opportunity.service.OpportunityService;
import com.biddingagency.dto.BidRequestCreateRequest;
import com.biddingagency.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * TC-MEM-012, TC-MEM-014 (CR-157): 무료회원이면 공고문 조회와 입찰 건 만들기에 닿지 않는다(BIZ-071).
 */
@ExtendWith(MockitoExtension.class)
class MembershipGuardControllerTest {

    @Mock private OpportunityService opportunityService;
    @Mock private NoticeService noticeService;
    @Mock private OpportunityRequirementItemRepository requirementItemRepository;
    @Mock private OpportunityAttachmentRepository attachmentRepository;
    @Mock private BidRequestService bidRequestService;
    @Mock private BidFSMService fsmService;
    @Mock private DocumentVersionService documentVersionService;
    @Mock private MembershipService membershipService;

    private final UUID memberId = UUID.randomUUID();
    private CustomUserDetails freeMember;

    @BeforeEach
    void setUp() {
        Member member = TestHelper.withId(Member.builder()
                .email("free@example.com")
                .companyName("시험 회사")
                .role(Member.Role.CUSTOMER)
                .build(), memberId);
        freeMember = new CustomUserDetails(member, List.of());
        doThrow(new MembershipException(MembershipError.MEMBERSHIP_REQUIRED))
                .when(membershipService).requireMember(memberId);
    }

    @Test
    @DisplayName("TC-MEM-012 무료회원_공고문상세_공고문조회안함")
    void 무료회원_공고문상세_공고문조회안함() {
        OpportunityController controller = new OpportunityController(
                opportunityService, noticeService, requirementItemRepository, attachmentRepository, membershipService);

        catchThrowable(() -> controller.getOpportunity(UUID.randomUUID(), freeMember));

        verify(noticeService, never()).findVisibleDetail(any());
    }

    @Test
    @DisplayName("TC-MEM-014 무료회원_입찰신청_입찰건만들지않음")
    void 무료회원_입찰신청_입찰건만들지않음() {
        BidRequestController controller = new BidRequestController(
                bidRequestService, fsmService, documentVersionService, noticeService, membershipService);
        BidRequestCreateRequest request = new BidRequestCreateRequest();
        request.setOpportunityId(UUID.randomUUID());

        catchThrowable(() -> controller.createBidRequest(request, freeMember));

        verify(bidRequestService, never()).createBidRequest(any(), any(), any(), anyString());
    }
}
