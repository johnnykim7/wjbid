package com.biddingagency.domain.compliance.service;

import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.entity.ClientDocument;
import com.biddingagency.domain.bid.repository.BidRequestRepository;
import com.biddingagency.domain.bid.repository.ClientDocumentRepository;
import com.biddingagency.domain.compliance.dto.RequiredDocumentSlot;
import com.biddingagency.domain.compliance.dto.RequiredDocumentSlotsResponse;
import com.biddingagency.domain.compliance.entity.FulfillmentStatus;
import com.biddingagency.domain.compliance.entity.FulfillmentType;
import com.biddingagency.domain.compliance.entity.RequirementFulfillmentMap;
import com.biddingagency.domain.compliance.repository.RequirementFulfillmentMapRepository;
import com.biddingagency.domain.document.repository.BidDocumentRepository;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.OpportunityRequirementItem;
import com.biddingagency.domain.opportunity.entity.RequirementCategory;
import com.biddingagency.domain.opportunity.repository.OpportunityRequirementItemRepository;
import com.biddingagency.integration.storage.StorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * CR-010 요구사항 슬롯 (getRequiredDocumentSlots / uploadToSlot / getUnfulfilledBlockerSlots)
 */
@ExtendWith(MockitoExtension.class)
class ComplianceServiceTest {

    @Mock private BidRequestRepository bidRequestRepository;
    @Mock private OpportunityRequirementItemRepository requirementRepository;
    @Mock private RequirementFulfillmentMapRepository fulfillmentMapRepository;
    @Mock private BidDocumentRepository documentRepository;
    @Mock private ClientDocumentRepository clientDocumentRepository;
    @Mock private StorageService storageService;

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
    @DisplayName("슬롯조회_BLOCKER요구사항매핑없음_PENDING상태_전이불가_CR010")
    void 슬롯조회_미충족_전이불가() {
        UUID bidRequestId = UUID.randomUUID();
        UUID opportunityId = UUID.randomUUID();
        UUID reqId = UUID.randomUUID();
        Opportunity opp = opportunity(opportunityId);

        given(bidRequestRepository.findById(bidRequestId)).willReturn(Optional.of(bidRequest(bidRequestId, opp)));
        given(requirementRepository.findByOpportunityIdAndIsBlocker(opportunityId, true))
                .willReturn(List.of(requirement(reqId, opp, true)));
        given(fulfillmentMapRepository.findByBidRequestIdAndRequirementItemId(bidRequestId, reqId))
                .willReturn(Optional.empty());

        RequiredDocumentSlotsResponse res = complianceService.getRequiredDocumentSlots(bidRequestId);

        assertThat(res.getData()).hasSize(1);
        assertThat(res.getData().get(0).getStatus()).isEqualTo("PENDING");
        assertThat(res.getSummary().getTotalBlocker()).isEqualTo(1);
        assertThat(res.getSummary().getFulfilledBlocker()).isZero();
        assertThat(res.getSummary().isCanTransitionToDocsReceived()).isFalse();
    }

    @Test
    @DisplayName("슬롯조회_BLOCKER요구사항충족_FULFILLED상태_전이가능_CR010")
    void 슬롯조회_충족_전이가능() {
        UUID bidRequestId = UUID.randomUUID();
        UUID opportunityId = UUID.randomUUID();
        UUID reqId = UUID.randomUUID();
        Opportunity opp = opportunity(opportunityId);

        ClientDocument doc = ClientDocument.builder()
                .fileName("license.pdf").fileSize(1024L).contentType("application/pdf")
                .storageUrl("client-docs/x").build();
        RequirementFulfillmentMap map = RequirementFulfillmentMap.builder()
                .fulfillmentType(FulfillmentType.CLIENT_DOCUMENT)
                .status(FulfillmentStatus.FULFILLED)
                .clientDocument(doc)
                .build();

        given(bidRequestRepository.findById(bidRequestId)).willReturn(Optional.of(bidRequest(bidRequestId, opp)));
        given(requirementRepository.findByOpportunityIdAndIsBlocker(opportunityId, true))
                .willReturn(List.of(requirement(reqId, opp, true)));
        given(fulfillmentMapRepository.findByBidRequestIdAndRequirementItemId(bidRequestId, reqId))
                .willReturn(Optional.of(map));

        RequiredDocumentSlotsResponse res = complianceService.getRequiredDocumentSlots(bidRequestId);

        assertThat(res.getData().get(0).getStatus()).isEqualTo("FULFILLED");
        assertThat(res.getData().get(0).getFulfillmentType()).isEqualTo("CLIENT_DOCUMENT");
        assertThat(res.getData().get(0).getMappedClientDocument().getFileName()).isEqualTo("license.pdf");
        assertThat(res.getSummary().isCanTransitionToDocsReceived()).isTrue();
    }

    @Test
    @DisplayName("슬롯업로드_신규매핑생성_CLIENT_DOCUMENT_FULFILLED_저장_CR010")
    void 슬롯업로드_신규매핑_생성() {
        UUID bidRequestId = UUID.randomUUID();
        UUID opportunityId = UUID.randomUUID();
        UUID reqId = UUID.randomUUID();
        Opportunity opp = opportunity(opportunityId);
        Member member = Member.builder().build();
        setId(member, UUID.randomUUID());

        MultipartFile file = new MockMultipartFile("file", "license.pdf",
                "application/pdf", "data".getBytes());

        given(bidRequestRepository.findById(bidRequestId)).willReturn(Optional.of(bidRequest(bidRequestId, opp)));
        given(requirementRepository.findById(reqId)).willReturn(Optional.of(requirement(reqId, opp, true)));
        given(storageService.store(anyString(), eq(file))).willReturn("client-docs/" + bidRequestId + "/uuid_license.pdf");
        given(clientDocumentRepository.save(any(ClientDocument.class))).willAnswer(inv -> inv.getArgument(0));
        given(fulfillmentMapRepository.findByBidRequestIdAndRequirementItemId(bidRequestId, reqId))
                .willReturn(Optional.empty());
        given(fulfillmentMapRepository.save(any(RequirementFulfillmentMap.class))).willAnswer(inv -> inv.getArgument(0));

        RequiredDocumentSlot slot = complianceService.uploadToSlot(bidRequestId, reqId, file, member);

        assertThat(slot.getStatus()).isEqualTo("FULFILLED");
        assertThat(slot.getFulfillmentType()).isEqualTo("CLIENT_DOCUMENT");
        assertThat(slot.getMappedClientDocument().getFileName()).isEqualTo("license.pdf");

        then(storageService).should().store(anyString(), eq(file));
        then(clientDocumentRepository).should().save(any(ClientDocument.class));
        then(fulfillmentMapRepository).should().save(argThat(m ->
                m.getFulfillmentType() == FulfillmentType.CLIENT_DOCUMENT
                        && m.getStatus() == FulfillmentStatus.FULFILLED
                        && m.getClientDocument() != null));
    }

    @Test
    @DisplayName("슬롯업로드_다른공고의요구사항_검증실패_CR010")
    void 슬롯업로드_타공고요구사항_예외() {
        UUID bidRequestId = UUID.randomUUID();
        UUID reqId = UUID.randomUUID();
        Opportunity bidOpp = opportunity(UUID.randomUUID());
        Opportunity otherOpp = opportunity(UUID.randomUUID());
        Member member = Member.builder().build();

        MultipartFile file = new MockMultipartFile("file", "x.pdf", "application/pdf", "d".getBytes());

        given(bidRequestRepository.findById(bidRequestId)).willReturn(Optional.of(bidRequest(bidRequestId, bidOpp)));
        given(requirementRepository.findById(reqId)).willReturn(Optional.of(requirement(reqId, otherOpp, true)));

        assertThatThrownBy(() -> complianceService.uploadToSlot(bidRequestId, reqId, file, member))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong");
        then(clientDocumentRepository).should(never()).save(any());
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
