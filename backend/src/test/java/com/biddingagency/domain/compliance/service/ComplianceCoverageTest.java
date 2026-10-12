package com.biddingagency.domain.compliance.service;

import com.biddingagency.TestHelper;
import com.biddingagency.domain.bid.repository.BidRequestRepository;
import com.biddingagency.domain.compliance.dto.CoverageResult;
import com.biddingagency.domain.compliance.entity.FulfillmentParty;
import com.biddingagency.domain.compliance.entity.FulfillmentStatus;
import com.biddingagency.domain.compliance.entity.FulfillmentType;
import com.biddingagency.domain.compliance.entity.RequirementFulfillmentMap;
import com.biddingagency.domain.compliance.repository.RequirementFulfillmentMapRepository;
import com.biddingagency.domain.document.entity.BidDocument;
import com.biddingagency.domain.document.entity.DocumentType;
import com.biddingagency.domain.document.repository.BidDocumentRepository;
import com.biddingagency.domain.notice.repository.NoticeRepository;
import com.biddingagency.domain.opportunity.entity.OpportunityRequirementItem;
import com.biddingagency.domain.opportunity.entity.RequirementLevel;
import com.biddingagency.domain.opportunity.repository.OpportunityRequirementItemRepository;
import com.biddingagency.domain.proposal.entity.ProposalSection;
import com.biddingagency.domain.proposal.service.ProposalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

/**
 * TC-COMP-021 ~ TC-COMP-025: 커버리지 검증 (CR-159, BIZ-078)
 */
@ExtendWith(MockitoExtension.class)
class ComplianceCoverageTest {

    @Mock private BidRequestRepository bidRequestRepository;
    @Mock private OpportunityRequirementItemRepository requirementRepository;
    @Mock private RequirementFulfillmentMapRepository fulfillmentMapRepository;
    @Mock private BidDocumentRepository documentRepository;
    @Mock private NoticeRepository noticeRepository;
    @Mock private ProposalService proposalService;

    private ComplianceService complianceService;
    private final ComplianceFixture f = new ComplianceFixture();

    @BeforeEach
    void setUp() {
        complianceService = new ComplianceService(bidRequestRepository, requirementRepository, fulfillmentMapRepository,
                documentRepository, noticeRepository, proposalService, Clock.systemDefaultZone());
        lenient().when(documentRepository.findById(f.document.getId())).thenReturn(Optional.of(f.document));
        lenient().when(fulfillmentMapRepository.findByBidRequestId(f.bidRequest.getId())).thenReturn(List.of());
    }

    private void givenMatrix(OpportunityRequirementItem... lines) {
        given(requirementRepository.findByOpportunityIdAndReqIdIsNotNullOrderBySortOrderAsc(f.opportunityId()))
                .willReturn(List.of(lines));
    }

    private void givenSections(ProposalSection... sections) {
        given(proposalService.getTree(f.document.getId()))
                .willReturn(List.of(new ProposalService.ChapterWithSections(f.chapter, List.of(sections))));
    }

    @Test
    @DisplayName("TC-COMP-021 대상_Shall이면서플랫폼생성만")
    void 대상_Shall이면서플랫폼생성만() {
        givenMatrix(f.shallPlatform("REQ-001", ComplianceFixture.QC),
                f.shallPlatform("REQ-002", ComplianceFixture.PP),
                f.line("REQ-003", RequirementLevel.SHOULD, FulfillmentParty.PLATFORM_GENERATED, "Should text"),
                f.line("REQ-004", RequirementLevel.SHALL, FulfillmentParty.CLIENT_UPLOAD, "License text"));
        givenSections(f.section("Technical", List.of("REQ-001", "REQ-002")));

        CoverageResult result = complianceService.coverage(f.document.getId());

        assertThat(result.targetCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("TC-COMP-022 매핑안된REQ_누락에포함")
    void 매핑안된REQ_누락에포함() {
        givenMatrix(f.shallPlatform("REQ-001", ComplianceFixture.QC));
        givenSections(f.section("Technical", List.of()));

        CoverageResult result = complianceService.coverage(f.document.getId());

        assertThat(result.missing()).extracting(CoverageResult.Missing::reqId, CoverageResult.Missing::unmapped)
                .containsExactly(tuple("REQ-001", true));
    }

    @Test
    @DisplayName("TC-COMP-023 다른문서로충족_이문서누락")
    void 다른문서로충족_이문서누락() {
        OpportunityRequirementItem req1 = f.shallPlatform("REQ-001", ComplianceFixture.QC);
        givenMatrix(req1);
        ProposalSection section = f.section("Technical", List.of("REQ-001"));
        givenSections(section);
        BidDocument other = TestHelper.withId(BidDocument.builder()
                .bidRequest(f.bidRequest).documentType(DocumentType.PAST_PERFORMANCE).build());
        given(fulfillmentMapRepository.findByBidRequestId(f.bidRequest.getId())).willReturn(List.of(
                RequirementFulfillmentMap.builder().bidRequest(f.bidRequest).requirementItem(req1)
                        .fulfillmentType(FulfillmentType.DOCUMENT_SECTION).document(other)
                        .documentSectionPath(java.util.UUID.randomUUID().toString())
                        .status(FulfillmentStatus.FULFILLED).build()));

        CoverageResult result = complianceService.coverage(f.document.getId());

        assertThat(result.missing()).extracting(CoverageResult.Missing::reqId, CoverageResult.Missing::unmapped)
                .containsExactly(tuple("REQ-001", false));
    }

    @Test
    @DisplayName("TC-COMP-024 매트릭스에없는번호_따로")
    void 매트릭스에없는번호_따로() {
        givenMatrix(f.shallPlatform("REQ-001", ComplianceFixture.QC));
        ProposalSection section = f.section("Staffing", List.of("REQ-001", "REQ-009"));
        givenSections(section);

        CoverageResult result = complianceService.coverage(f.document.getId());

        assertThat(result.unknownIds()).extracting(CoverageResult.UnknownId::reqId, CoverageResult.UnknownId::sectionId,
                        CoverageResult.UnknownId::sectionTitle)
                .containsExactly(tuple("REQ-009", section.getId(), "Staffing"));
    }

    @Test
    @DisplayName("TC-COMP-025 매트릭스없음_matrixReady거짓")
    void 매트릭스없음_matrixReady거짓() {
        givenMatrix();

        CoverageResult result = complianceService.coverage(f.document.getId());

        assertThat(result).isEqualTo(CoverageResult.notReady());
    }
}
