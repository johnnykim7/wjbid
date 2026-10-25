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
import com.biddingagency.domain.opportunity.entity.OpportunityVisibility;
import com.biddingagency.domain.opportunity.entity.RequirementLevel;
import com.biddingagency.domain.opportunity.repository.OpportunityRequirementItemRepository;
import com.biddingagency.domain.proposal.entity.ProposalSection;
import com.biddingagency.domain.proposal.service.DocumentCompositionService;
import com.biddingagency.domain.proposal.service.ProposalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

/**
 * TC-COMP-021 ~ TC-COMP-025: 커버리지 검증 (CR-159, BIZ-078)
 * TC-COMP-030, 031: 평가받지 않는 문서는 커버리지 검증 대상이 아니다 (CR-178, BIZ-095)
 * TC-COMP-034, 035: 커버리지 대상은 그 문서가 받는 줄이다 (CR-179, BIZ-096)
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
                documentRepository, noticeRepository, proposalService,
                new DocumentCompositionService(noticeRepository, "COVER_LETTER,TECHNICAL_PROPOSAL"), Clock.systemDefaultZone());
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

    @Test
    @DisplayName("TC-COMP-030 커버레터_커버리지_평가대상아님")
    void 커버레터_커버리지_평가대상아님() {
        BidDocument cover = TestHelper.withId(BidDocument.builder()
                .bidRequest(f.bidRequest).documentType(DocumentType.COVER_LETTER).build());
        given(documentRepository.findById(cover.getId())).willReturn(Optional.of(cover));
        givenMatrix(f.shallPlatform("REQ-001", ComplianceFixture.QC));

        CoverageResult result = complianceService.coverage(cover.getId());

        assertThat(result).isEqualTo(CoverageResult.notEvaluated(true));
    }

    @Test
    @DisplayName("TC-COMP-031 기술제안서_커버리지_평가받는문서참")
    void 기술제안서_커버리지_평가받는문서참() {
        givenMatrix(f.shallPlatform("REQ-001", ComplianceFixture.QC));
        givenSections(f.section("Technical", List.of("REQ-001")));

        CoverageResult result = complianceService.coverage(f.document.getId());

        assertThat(result.evaluatedDocument()).isTrue();
    }

    /** F1 대상 8, F2 대상 3, 평가 항목 없는 대상 2 */
    private void givenFactorMatrix(String factorTreeJson) {
        List<OpportunityRequirementItem> lines = new ArrayList<>();
        for (int i = 1; i <= 13; i++) {
            lines.add(f.shallPlatformOf(String.format("REQ-%03d", i), i <= 8 ? "F1" : i <= 11 ? "F2" : null));
        }
        given(requirementRepository.findByOpportunityIdAndReqIdIsNotNullOrderBySortOrderAsc(f.opportunityId())).willReturn(lines);
        given(noticeRepository.findFirstByOpportunityIdAndVisibilityOrderByAnalyzedAtDesc(f.opportunityId(), OpportunityVisibility.VISIBLE))
                .willReturn(Optional.of(f.notice(factorTreeJson)));
    }

    private int targetCountOf(BidDocument document) {
        lenient().when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        lenient().when(proposalService.getTree(document.getId())).thenReturn(List.of());
        return complianceService.coverage(document.getId()).targetCount();
    }

    @Test
    @DisplayName("TC-COMP-034 문서별커버리지_자기줄로만대상")
    void 문서별커버리지_자기줄로만대상() {
        givenFactorMatrix(ComplianceFixture.COMPOSED_TREE);

        assertThat(List.of(targetCountOf(f.document), targetCountOf(f.document(DocumentType.PAST_PERFORMANCE))))
                .containsExactly(10, 3);
    }

    @Test
    @DisplayName("TC-COMP-035 옛공고_커버리지_대상전부기술제안서")
    void 옛공고_커버리지_대상전부기술제안서() {
        givenFactorMatrix(ComplianceFixture.OLD_TREE);

        assertThat(targetCountOf(f.document)).isEqualTo(13);
    }
}
