package com.biddingagency.domain.compliance.service;

import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.repository.BidRequestRepository;
import com.biddingagency.domain.compliance.entity.FulfillmentStatus;
import com.biddingagency.domain.compliance.entity.RequirementFulfillmentMap;
import com.biddingagency.domain.compliance.repository.RequirementFulfillmentMapRepository;
import com.biddingagency.domain.document.repository.BidDocumentRepository;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.OpportunityRequirementItem;
import com.biddingagency.domain.opportunity.entity.RequirementCategory;
import com.biddingagency.domain.opportunity.repository.OpportunityRequirementItemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * CR-010 제출 게이트 (getUnfulfilledBlockerSlots). 요구 서류 칸 조회와 올리기는 CR-141 PreCheckServiceTest
 */
@ExtendWith(MockitoExtension.class)
class ComplianceServiceTest {

    @Mock private BidRequestRepository bidRequestRepository;
    @Mock private OpportunityRequirementItemRepository requirementRepository;
    @Mock private RequirementFulfillmentMapRepository fulfillmentMapRepository;
    @Mock private BidDocumentRepository documentRepository;

    @InjectMocks private ComplianceService complianceService;

    private static void setId(Object entity, UUID id) {
        ReflectionTestUtils.setField(entity, "id", id);
    }

    private BidRequest bidRequest(UUID bidRequestId, Opportunity opportunity) {
        BidRequest br = BidRequest.builder().opportunity(opportunity).build();
        setId(br, bidRequestId);
        return br;
    }

    private Opportunity opportunity(UUID opportunityId) {
        Opportunity opp = Opportunity.builder().build();
        setId(opp, opportunityId);
        return opp;
    }

    private OpportunityRequirementItem requirement(UUID reqId, Opportunity opp, boolean blocker) {
        OpportunityRequirementItem req = OpportunityRequirementItem.builder()
                .opportunity(opp)
                .title("Business License")
                .description("Valid license")
                .isBlocker(blocker)
                .category(RequirementCategory.ELIGIBILITY)
                .build();
        setId(req, reqId);
        return req;
    }

    @Test
    @DisplayName("게이트_BLOCKER일부미충족_미충족목록반환_CR010_BIZ015")
    void 게이트_일부미충족_목록반환() {
        UUID bidRequestId = UUID.randomUUID();
        UUID opportunityId = UUID.randomUUID();
        UUID reqFulfilled = UUID.randomUUID();
        UUID reqMissing = UUID.randomUUID();
        Opportunity opp = opportunity(opportunityId);

        RequirementFulfillmentMap fulfilled = RequirementFulfillmentMap.builder()
                .status(FulfillmentStatus.FULFILLED).build();

        given(bidRequestRepository.findById(bidRequestId)).willReturn(Optional.of(bidRequest(bidRequestId, opp)));
        given(requirementRepository.findByOpportunityIdAndIsBlocker(opportunityId, true))
                .willReturn(List.of(requirement(reqFulfilled, opp, true), requirement(reqMissing, opp, true)));
        given(fulfillmentMapRepository.findByBidRequestIdAndRequirementItemId(bidRequestId, reqFulfilled))
                .willReturn(Optional.of(fulfilled));
        given(fulfillmentMapRepository.findByBidRequestIdAndRequirementItemId(bidRequestId, reqMissing))
                .willReturn(Optional.empty());

        List<OpportunityRequirementItem> unfulfilled =
                complianceService.getUnfulfilledBlockerSlots(bidRequestId);

        assertThat(unfulfilled).hasSize(1);
        assertThat(unfulfilled.get(0).getId()).isEqualTo(reqMissing);
    }

    @Test
    @DisplayName("게이트_BLOCKER전부충족_빈목록반환_전이가능_CR010_BIZ015")
    void 게이트_전부충족_빈목록() {
        UUID bidRequestId = UUID.randomUUID();
        UUID opportunityId = UUID.randomUUID();
        UUID reqId = UUID.randomUUID();
        Opportunity opp = opportunity(opportunityId);

        RequirementFulfillmentMap fulfilled = RequirementFulfillmentMap.builder()
                .status(FulfillmentStatus.FULFILLED).build();

        given(bidRequestRepository.findById(bidRequestId)).willReturn(Optional.of(bidRequest(bidRequestId, opp)));
        given(requirementRepository.findByOpportunityIdAndIsBlocker(opportunityId, true))
                .willReturn(List.of(requirement(reqId, opp, true)));
        given(fulfillmentMapRepository.findByBidRequestIdAndRequirementItemId(bidRequestId, reqId))
                .willReturn(Optional.of(fulfilled));

        assertThat(complianceService.getUnfulfilledBlockerSlots(bidRequestId)).isEmpty();
    }
}
