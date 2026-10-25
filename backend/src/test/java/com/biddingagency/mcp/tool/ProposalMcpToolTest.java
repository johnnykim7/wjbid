package com.biddingagency.mcp.tool;

import com.biddingagency.TestHelper;
import com.biddingagency.domain.bid.repository.BidRequestRepository;
import com.biddingagency.domain.bid.service.BidRequestService;
import com.biddingagency.domain.compliance.entity.FulfillmentStatus;
import com.biddingagency.domain.compliance.entity.FulfillmentType;
import com.biddingagency.domain.compliance.entity.RequirementFulfillmentMap;
import com.biddingagency.domain.compliance.repository.RequirementFulfillmentMapRepository;
import com.biddingagency.domain.compliance.service.ComplianceFixture;
import com.biddingagency.domain.compliance.service.ComplianceService;
import com.biddingagency.domain.document.entity.BidDocument;
import com.biddingagency.domain.document.entity.DocumentType;
import com.biddingagency.domain.document.repository.BidDocumentRepository;
import com.biddingagency.domain.document.service.BidDocumentService;
import com.biddingagency.domain.notice.entity.Notice;
import com.biddingagency.domain.notice.entity.NoticeGenerationStatus;
import com.biddingagency.domain.notice.repository.NoticeRepository;
import com.biddingagency.domain.opportunity.entity.OpportunityRequirementItem;
import com.biddingagency.domain.opportunity.entity.OpportunityVisibility;
import com.biddingagency.domain.opportunity.repository.OpportunityRequirementItemRepository;
import com.biddingagency.domain.proposal.entity.ProposalSection;
import com.biddingagency.domain.proposal.service.ProposalService;
import com.biddingagency.domain.proposal.service.VerificationLogService;
import com.biddingagency.domain.reading.service.DocumentReadingService;
import com.biddingagency.domain.rfp.service.ReferenceSampleService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

/**
 * TC-COMP-013 ~ TC-COMP-020: 제안서 도구가 컴플라이언스 매트릭스를 쓴다 (CR-159, BIZ-076, BIZ-077)
 * TC-DOC-288, 289, TC-COMP-028, 029: 평가받지 않는 문서는 평가 항목과 대응 REQ ID 를 받지 않는다 (CR-178, BIZ-095)
 */
@ExtendWith(MockitoExtension.class)
class ProposalMcpToolTest {

    @Mock private BidRequestService bidRequestService;
    @Mock private BidDocumentService bidDocumentService;
    @Mock private NoticeRepository noticeRepository;
    @Mock private ProposalService proposalService;
    @Mock private ReferenceSampleService referenceSampleService;
    @Mock private VerificationLogService verificationLogService;
    @Mock private BidRequestRepository bidRequestRepository;
    @Mock private OpportunityRequirementItemRepository requirementRepository;
    @Mock private RequirementFulfillmentMapRepository fulfillmentMapRepository;
    @Mock private BidDocumentRepository documentRepository;
    @Mock private DocumentReadingService documentReadingService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ComplianceFixture f = new ComplianceFixture();
    private ProposalMcpTool tool;
    private OpportunityRequirementItem req1;
    private OpportunityRequirementItem req2;

    @BeforeEach
    void setUp() {
        ComplianceService complianceService = new ComplianceService(bidRequestRepository, requirementRepository,
                fulfillmentMapRepository, documentRepository, noticeRepository, proposalService, Clock.systemDefaultZone());
        tool = new ProposalMcpTool(bidRequestService, bidDocumentService, noticeRepository, proposalService,
                referenceSampleService, verificationLogService, complianceService, documentReadingService, objectMapper);
        req1 = f.shallPlatform("REQ-001", ComplianceFixture.QC);
        req2 = f.shallPlatform("REQ-002", ComplianceFixture.PP);
        lenient().when(documentRepository.findById(f.document.getId())).thenReturn(Optional.of(f.document));
        lenient().when(bidDocumentService.findById(f.document.getId())).thenReturn(f.document);
    }

    private void givenMatrix(OpportunityRequirementItem... lines) {
        given(requirementRepository.findByOpportunityIdAndReqIdIsNotNullOrderBySortOrderAsc(f.opportunityId()))
                .willReturn(List.of(lines));
    }

    private Map<String, Object> structureArgs(List<String> refs) {
        return Map.of("documentId", f.document.getId().toString(), "chapters", List.of(Map.of(
                "factorLabel", "1", "factorTitle", "Technical",
                "sections", List.of(Map.of("title", "Quality", "requirementRefs", refs)))));
    }

    private void givenSavedStructure(ProposalSection section) {
        given(proposalService.saveStructure(any(), any())).willReturn(List.of(f.chapter));
        given(proposalService.getSections(f.chapter.getId())).willReturn(List.of(section));
        given(proposalService.getAllSections(f.document.getId())).willReturn(List.of(section));
    }

    private void givenSection(ProposalSection section) {
        given(proposalService.getSection(section.getId())).willReturn(section);
        given(proposalService.getChapter(f.chapter.getId())).willReturn(f.chapter);
    }

    private Map<String, Object> verifyArgs(ProposalSection section, Map<String, Object> check) {
        return Map.of("targetType", "PROPOSAL_SECTION", "targetId", section.getId().toString(), "verdict", "PASS",
                "attempt", 1, "requirementChecks", List.of(check));
    }

    private RequirementFulfillmentMap savedMap() {
        ArgumentCaptor<RequirementFulfillmentMap> captor = ArgumentCaptor.forClass(RequirementFulfillmentMap.class);
        then(fulfillmentMapRepository).should().save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("TC-COMP-013 설계저장_매트릭스에없는ID_전체거부")
    void 설계저장_매트릭스에없는ID_전체거부() {
        givenMatrix(req1, req2);

        assertThatThrownBy(() -> tool.saveProposalStructure(structureArgs(List.of("REQ-001", "REQ-099"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("REQ-099");
        then(proposalService).should(never()).saveStructure(any(), any());
    }

    @Test
    @DisplayName("TC-COMP-014 설계저장_안걸린대상_응답에")
    void 설계저장_안걸린대상_응답에() throws Exception {
        given(requirementRepository.findByOpportunityIdAndReqIdIsNotNullOrderBySortOrderAsc(f.opportunityId()))
                .willReturn(List.of(req1, req2));
        givenSavedStructure(f.section("Quality", List.of("REQ-001")));

        JsonNode result = objectMapper.readTree(tool.saveProposalStructure(structureArgs(List.of("REQ-001"))));

        assertThat(result.get("unassignedReqIds").toString()).isEqualTo("[\"REQ-002\"]");
    }

    @Test
    @DisplayName("TC-COMP-015 설계저장_매트릭스없는공고_검사안함")
    void 설계저장_매트릭스없는공고_검사안함() throws Exception {
        given(requirementRepository.findByOpportunityIdAndReqIdIsNotNullOrderBySortOrderAsc(f.opportunityId()))
                .willReturn(List.of());
        givenSavedStructure(f.section("Quality", List.of("L.4.1")));

        JsonNode result = objectMapper.readTree(tool.saveProposalStructure(structureArgs(List.of("L.4.1"))));

        assertThat(result.get("unassignedReqIds").size()).isZero();
    }

    @Test
    @DisplayName("TC-COMP-016 절정보_대응REQ_원문같이")
    void 절정보_대응REQ_원문같이() throws Exception {
        givenMatrix(req1, req2);
        ProposalSection section = f.section("Quality", List.of("REQ-001"));
        givenSection(section);
        given(bidDocumentService.findById(f.document.getId())).willReturn(f.document);
        given(bidRequestService.findByIdWithDetails(f.bidRequest.getId())).willReturn(f.bidRequest);

        JsonNode requirements = objectMapper.readTree(tool.getSectionContext(Map.of("sectionId", section.getId().toString())))
                .get("requirements");

        assertThat(List.<Object>of(requirements.size(), requirements.get(0).get("reqId").asText(), requirements.get(0).get("text").asText(),
                requirements.get(0).get("translation").asText(), requirements.get(0).get("verb").asText(),
                requirements.get(0).get("level").asText()))
                .containsExactly(1, "REQ-001", ComplianceFixture.QC, "번역 REQ-001", "describe", "SHALL");
    }

    @Test
    @DisplayName("TC-COMP-017 검증저장_답함_제안서절충족")
    void 검증저장_답함_제안서절충족() {
        givenMatrix(req1, req2);
        ProposalSection section = f.section("Quality", List.of("REQ-001"));
        givenSection(section);
        given(fulfillmentMapRepository.findByBidRequestIdAndRequirementItemId(f.bidRequest.getId(), req1.getId()))
                .willReturn(Optional.empty());

        tool.saveVerificationResult(verifyArgs(section, Map.of("reqId", "REQ-001", "answered", true,
                "evidenceQuote", "Our quality control plan")));

        RequirementFulfillmentMap map = savedMap();
        assertThat(List.<Object>of(map.getFulfillmentType(), map.getStatus(), map.getDocument().getId(), map.getDocumentSectionPath()))
                .containsExactly(FulfillmentType.DOCUMENT_SECTION, FulfillmentStatus.FULFILLED, f.document.getId(),
                        section.getId().toString());
    }

    @Test
    @DisplayName("TC-COMP-018 검증저장_답안함_다른절충족매핑_안바꿈")
    void 검증저장_답안함_다른절충족매핑_안바꿈() {
        givenMatrix(req1, req2);
        ProposalSection sectionB = f.section("Staffing", List.of("REQ-001"));
        givenSection(sectionB);
        String sectionA = UUID.randomUUID().toString();
        RequirementFulfillmentMap existing = RequirementFulfillmentMap.builder().bidRequest(f.bidRequest).requirementItem(req1)
                .fulfillmentType(FulfillmentType.DOCUMENT_SECTION).document(f.document).documentSectionPath(sectionA)
                .status(FulfillmentStatus.FULFILLED).build();
        given(fulfillmentMapRepository.findByBidRequestIdAndRequirementItemId(f.bidRequest.getId(), req1.getId()))
                .willReturn(Optional.of(existing));

        tool.saveVerificationResult(verifyArgs(sectionB, Map.of("reqId", "REQ-001", "answered", false, "reason", "not covered")));

        assertThat(List.of(existing.getDocumentSectionPath(), existing.getStatus().name())).containsExactly(sectionA, "FULFILLED");
    }

    @Test
    @DisplayName("TC-COMP-019 검증저장_답안함_매핑없음_누락")
    void 검증저장_답안함_매핑없음_누락() {
        givenMatrix(req1, req2);
        ProposalSection section = f.section("Past Performance", List.of("REQ-002"));
        givenSection(section);
        given(fulfillmentMapRepository.findByBidRequestIdAndRequirementItemId(f.bidRequest.getId(), req2.getId()))
                .willReturn(Optional.empty());

        tool.saveVerificationResult(verifyArgs(section, Map.of("reqId", "REQ-002", "answered", false, "reason", "no references")));

        RequirementFulfillmentMap map = savedMap();
        assertThat(List.<Object>of(map.getFulfillmentType(), map.getStatus(), map.getDocumentSectionPath()))
                .containsExactly(FulfillmentType.DOCUMENT_SECTION, FulfillmentStatus.MISSING, section.getId().toString());
    }

    @Test
    @DisplayName("TC-COMP-020 검증저장_대응아닌번호_버림")
    void 검증저장_대응아닌번호_버림() {
        OpportunityRequirementItem req3 = f.shallPlatform("REQ-003", "The offeror shall demonstrate key personnel.");
        givenMatrix(req1, req2, req3);
        ProposalSection section = f.section("Quality", List.of("REQ-001"));
        givenSection(section);

        tool.saveVerificationResult(verifyArgs(section, Map.of("reqId", "REQ-003", "answered", true, "evidenceQuote", "x")));

        then(fulfillmentMapRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("TC-DOC-233 설계저장_읽음아닌근거_전체거부(도구)")
    void 설계저장_읽음아닌근거_전체거부() {
        given(requirementRepository.findByOpportunityIdAndReqIdIsNotNullOrderBySortOrderAsc(f.opportunityId()))
                .willReturn(List.of());
        String failedId = UUID.randomUUID().toString();
        willThrow(new IllegalArgumentException("unknownReadingIds: [" + failedId + "]"))
                .given(documentReadingService).requireReadableSources(f.bidRequest.getId(), List.of(failedId));
        Map<String, Object> args = Map.of("documentId", f.document.getId().toString(), "chapters", List.of(Map.of(
                "factorLabel", "1", "factorTitle", "Technical",
                "sections", List.of(Map.of("title", "Quality", "requirementRefs", List.of(),
                        "sourceReadingIds", List.of(failedId))))));

        assertThatThrownBy(() -> tool.saveProposalStructure(args)).isInstanceOf(IllegalArgumentException.class);
        then(proposalService).should(never()).saveStructure(any(), any());
    }

    @Test
    @DisplayName("TC-DOC-236 검증입력_근거서류같이")
    void 검증입력_근거서류같이() throws Exception {
        String readingId = UUID.randomUUID().toString();
        ProposalSection section = TestHelper.withId(ProposalSection.builder().chapterId(f.chapter.getId())
                .title("Quality").requirementRefs(List.of()).sourceReadingIds(List.of(readingId)).build());
        givenSection(section);
        given(bidRequestService.findByIdWithDetails(f.bidRequest.getId())).willReturn(f.bidRequest);
        given(documentReadingService.sourceDocuments(f.bidRequest.getId(), List.of(readingId))).willReturn(List.of(Map.of(
                "readingId", readingId, "fileName", "a.pdf", "facts", List.of(Map.of("key", "상호", "value", "우진", "page", "1")))));
        Map<String, Object> args = Map.of("sectionId", section.getId().toString());

        JsonNode context = objectMapper.readTree(tool.getSectionContext(args)).get("sourceDocuments");
        JsonNode verify = objectMapper.readTree(tool.getSectionVerifyInput(args)).get("sourceDocuments");

        assertThat(List.<Object>of(context.size(), verify)).containsExactly(1, context);
    }

    private BidDocument coverLetter() {
        BidDocument cover = TestHelper.withId(BidDocument.builder()
                .bidRequest(f.bidRequest).documentType(DocumentType.COVER_LETTER).build());
        lenient().when(documentRepository.findById(cover.getId())).thenReturn(Optional.of(cover));
        lenient().when(bidDocumentService.findById(cover.getId())).thenReturn(cover);
        return cover;
    }

    private JsonNode designInput(BidDocument document) throws Exception {
        Notice notice = Notice.builder().opportunity(f.opportunity)
                .generationStatus(NoticeGenerationStatus.COMPLETED)
                .factorTreeJsonRaw("{\"factors\":[{\"label\":\"1\",\"title\":\"Technical\"}]}")
                .priceItemsJsonRaw("{\"items\":[]}")
                .build();
        given(bidRequestService.findByIdWithDetails(f.bidRequest.getId())).willReturn(f.bidRequest);
        given(noticeRepository.findFirstByOpportunityIdAndVisibilityOrderByAnalyzedAtDesc(f.opportunityId(),
                OpportunityVisibility.VISIBLE)).willReturn(Optional.of(notice));
        lenient().when(requirementRepository.findByOpportunityIdAndReqIdIsNotNullOrderBySortOrderAsc(f.opportunityId()))
                .thenReturn(List.of(req1, req2));
        return objectMapper.readTree(tool.getProposalDesignInput(Map.of("documentId", document.getId().toString())));
    }

    @Test
    @DisplayName("TC-DOC-288 커버레터_설계입력_평가항목과매트릭스없음")
    void 커버레터_설계입력_평가항목과매트릭스없음() throws Exception {
        JsonNode input = designInput(coverLetter());

        assertThat(List.<Object>of(input.get("evaluatedDocument").asBoolean(), input.get("factorTree").isNull(),
                input.get("priceItems").isNull(), input.get("complianceMatrix").size()))
                .containsExactly(false, true, true, 0);
    }

    @Test
    @DisplayName("TC-DOC-289 기술제안서_설계입력_평가항목과매트릭스있음")
    void 기술제안서_설계입력_평가항목과매트릭스있음() throws Exception {
        JsonNode input = designInput(f.document);

        assertThat(List.<Object>of(input.get("evaluatedDocument").asBoolean(), input.get("factorTree").isNull(),
                input.get("complianceMatrix").size()))
                .containsExactly(true, false, 2);
    }

    @Test
    @DisplayName("TC-COMP-028 커버레터_설계저장_대응REQ있으면_전체거부")
    void 커버레터_설계저장_대응REQ있으면_전체거부() {
        BidDocument cover = coverLetter();
        lenient().when(requirementRepository.findByOpportunityIdAndReqIdIsNotNullOrderBySortOrderAsc(f.opportunityId()))
                .thenReturn(List.of(req1, req2));
        Map<String, Object> args = Map.of("documentId", cover.getId().toString(), "chapters", List.of(Map.of(
                "factorLabel", "1", "factorTitle", "Cover Letter",
                "sections", List.of(Map.of("title", "Cover Letter", "requirementRefs", List.of("REQ-001"))))));

        assertThatThrownBy(() -> tool.saveProposalStructure(args))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("평가받지 않는 문서")
                .hasMessageContaining("REQ-001");
        then(proposalService).should(never()).saveStructure(any(), any());
    }

    @Test
    @DisplayName("TC-COMP-029 커버레터_설계저장_REQ없음_저장되고미배정빈배열")
    void 커버레터_설계저장_REQ없음_저장되고미배정빈배열() throws Exception {
        BidDocument cover = coverLetter();
        lenient().when(requirementRepository.findByOpportunityIdAndReqIdIsNotNullOrderBySortOrderAsc(f.opportunityId()))
                .thenReturn(List.of(req1, req2));
        given(proposalService.saveStructure(any(), any())).willReturn(List.of(f.chapter));
        given(proposalService.getSections(f.chapter.getId())).willReturn(List.of(f.section("Cover Letter", List.of())));
        Map<String, Object> args = Map.of("documentId", cover.getId().toString(), "chapters", List.of(Map.of(
                "factorLabel", "1", "factorTitle", "Cover Letter",
                "sections", List.of(Map.of("title", "Cover Letter", "requirementRefs", List.of())))));

        JsonNode result = objectMapper.readTree(tool.saveProposalStructure(args));

        assertThat(result.get("unassignedReqIds").size()).isZero();
    }
}
