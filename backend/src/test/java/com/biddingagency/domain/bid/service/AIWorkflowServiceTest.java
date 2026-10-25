package com.biddingagency.domain.bid.service;

import com.biddingagency.TestHelper;
import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.entity.BidRequestState;
import com.biddingagency.domain.bid.repository.ClientDocumentRepository;
import com.biddingagency.domain.document.entity.BidDocument;
import com.biddingagency.domain.document.entity.DocumentType;
import com.biddingagency.domain.document.service.BidDocumentService;
import com.biddingagency.domain.document.service.DocumentVersionService;
import com.biddingagency.domain.notice.repository.NoticeRepository;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.OpportunityVisibility;
import com.biddingagency.domain.opportunity.repository.OpportunityRequirementItemRepository;
import com.biddingagency.domain.proposal.entity.ProposalSection;
import com.biddingagency.domain.proposal.service.ProposalAssembleService;
import com.biddingagency.domain.proposal.service.ProposalService;
import com.biddingagency.domain.rfp.entity.IndustryType;
import com.biddingagency.domain.rfp.entity.PatternGuide;
import com.biddingagency.domain.rfp.repository.PatternGuideRepository;
import com.biddingagency.domain.rfp.service.ReferenceSampleService;
import com.biddingagency.integration.llmplatform.LLMPlatformClient;
import com.biddingagency.integration.llmplatform.LLMPlatformException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * AIWorkflowService.build3PipelineContext 테스트 (CR-014).
 * P3 교체 검증 — industry_type=null이면 빈 처리, 비-null이면 successGuide+referenceSamples 채움.
 */
@ExtendWith(MockitoExtension.class)
class AIWorkflowServiceTest {

    @Mock private LLMPlatformClient llmPlatformClient;
    @Mock private OpportunityRequirementItemRepository requirementItemRepository;
    @Mock private NoticeRepository noticeRepository;
    @Mock private ClientDocumentRepository clientDocumentRepository;
    @Mock private PatternGuideRepository patternGuideRepository;
    @Mock private ReferenceSampleService referenceSampleService;
    @Mock private BidDocumentService bidDocumentService;
    @Mock private DocumentVersionService documentVersionService;
    @Mock private ProposalService proposalService;
    @Mock private ProposalAssembleService proposalAssembleService;
    @Mock private BidFSMService bidFSMService;
    @Mock private com.biddingagency.domain.reading.service.DocumentReadingService documentReadingService;
    @Mock private com.biddingagency.domain.analysis.service.BidAnalysisService bidAnalysisService;

    @InjectMocks
    private AIWorkflowService aiWorkflowService;

    private BidRequest bidRequest(Opportunity opp) {
        UUID bidRequestId = UUID.randomUUID();
        BidRequest br = BidRequest.builder().opportunity(opp).build();
        ReflectionTestUtils.setField(br, "id", bidRequestId);
        return br;
    }

    private Opportunity opportunity(IndustryType type) {
        Opportunity opp = Opportunity.builder()
                .noticeId("N-1").title("Ground Maintenance at Kunsan AB").industryType(type)
                .build();
        ReflectionTestUtils.setField(opp, "id", UUID.randomUUID());
        return opp;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> invokeContext(BidRequest br, Opportunity opp) {
        // P1/P2 공통 stub (조건분기 없음 — 항상 조회). CR-016: 노출 공고문 없음 가정
        lenient().when(noticeRepository.findFirstByOpportunityIdAndVisibilityOrderByAnalyzedAtDesc(
                        opp.getId(), OpportunityVisibility.VISIBLE))
                .thenReturn(Optional.empty());
        lenient().when(clientDocumentRepository.findByBidRequestId(br.getId()))
                .thenReturn(List.of());
        return (Map<String, Object>) ReflectionTestUtils.invokeMethod(
                aiWorkflowService, "build3PipelineContext", br, opp);
    }

    @Test
    @DisplayName("미분류공고_P3빈처리_successGuide_null_referenceSamples_빈")
    void context_미분류_빈처리() {
        Opportunity opp = opportunity(null);
        BidRequest br = bidRequest(opp);
        given(referenceSampleService.collect(null)).willReturn(List.of());

        Map<String, Object> context = invokeContext(br, opp);

        assertThat(context).containsKey("successGuide");
        assertThat(context.get("successGuide")).isNull();
        assertThat((List<?>) context.get("referenceSamples")).isEmpty();
        assertThat(context.get("referenceUsagePolicy")).asString().contains("복사하지 마세요");
        // P3 교체 확인 — 구 pastSubmissions 키는 더 이상 없음
        assertThat(context).doesNotContainKey("pastSubmissions");
    }

    @Test
    @DisplayName("유형분류공고_가이드와원본메타_채움")
    void context_유형분류_채움() {
        Opportunity opp = opportunity(IndustryType.GROUND_MAINTENANCE);
        BidRequest br = bidRequest(opp);

        PatternGuide guide = PatternGuide.builder()
                .industryType(IndustryType.GROUND_MAINTENANCE).build();
        guide.markCompleted(Map.of("checklist", List.of("실적은 표로 12건+")), 2);

        given(patternGuideRepository.findByIndustryType(IndustryType.GROUND_MAINTENANCE))
                .willReturn(Optional.of(guide));
        given(referenceSampleService.collect(IndustryType.GROUND_MAINTENANCE))
                .willReturn(List.of(Map.of("rfpSampleId", "s1", "files", List.of())));

        Map<String, Object> context = invokeContext(br, opp);

        assertThat(context.get("successGuide")).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> sg = (Map<String, Object>) context.get("successGuide");
        assertThat(sg).containsKey("checklist");
        assertThat((List<?>) context.get("referenceSamples")).hasSize(1);
    }

    @Test
    @DisplayName("제안서작성시작_빈문서생성_GENERATING으로넘긴관리자가편집자")
    void 제안서작성시작_빈문서_편집자는작성시킨관리자() {
        BidRequest br = bidRequest(opportunity(IndustryType.GROUND_MAINTENANCE));
        UUID admin = UUID.randomUUID();
        br.transitionTo(BidRequestState.GENERATING, admin, "admin@example.com", null);
        BidDocument doc = BidDocument.builder().build();
        ReflectionTestUtils.setField(doc, "id", UUID.randomUUID());
        given(bidDocumentService.findByBidRequestAndType(br.getId(), DocumentType.TECHNICAL_PROPOSAL))
                .willThrow(new IllegalArgumentException("없음"));
        given(documentVersionService.createDocument(eq(br.getId()), eq(DocumentType.TECHNICAL_PROPOSAL), any(), eq(admin)))
                .willReturn(doc);

        ReflectionTestUtils.invokeMethod(aiWorkflowService, "ensureDocument", br, DocumentType.TECHNICAL_PROPOSAL);

        verify(documentVersionService).createDocument(eq(br.getId()), eq(DocumentType.TECHNICAL_PROPOSAL), any(), eq(admin));
    }

    private BidRequest generatingBy(UUID admin) {
        BidRequest br = bidRequest(opportunity(IndustryType.GROUND_MAINTENANCE));
        br.transitionTo(BidRequestState.GENERATING, admin, "admin@example.com", null);
        ReflectionTestUtils.setField(aiWorkflowService, "autoGenerateDocumentTypes", "COVER_LETTER,TECHNICAL_PROPOSAL");
        return br;
    }

    private UUID givenDocument(BidRequest br, DocumentType type, List<ProposalSection> sections) {
        BidDocument doc = TestHelper.withId(BidDocument.builder().build());
        given(bidDocumentService.findByBidRequestAndType(br.getId(), type)).willReturn(doc);
        given(proposalService.getAllSections(doc.getId())).willReturn(sections);
        return doc.getId();
    }

    @Test
    @DisplayName("TC-DOC-243 문서종류둘중하나만묶임_작성끝_REVIEW로넘기고전환한사람은작성시킨관리자")
    void 하나라도묶이면_작성끝_REVIEW() {
        UUID admin = UUID.randomUUID();
        BidRequest br = generatingBy(admin);
        givenDocument(br, DocumentType.COVER_LETTER, List.of());
        UUID technical = givenDocument(br, DocumentType.TECHNICAL_PROPOSAL,
                List.of(TestHelper.withId(ProposalSection.builder().build())));

        aiWorkflowService.generateDocumentsAsync(br);

        verify(proposalAssembleService).assemble(technical, admin);
        verify(bidFSMService).transition(br.getId(), BidRequestState.REVIEW, admin, "admin@example.com", null);
    }

    @Test
    @DisplayName("TC-DOC-244 두종류모두설계절0_작성끝_전환없이GENERATING유지")
    void 하나도못묶으면_작성끝_전환없음() {
        BidRequest br = generatingBy(UUID.randomUUID());
        givenDocument(br, DocumentType.COVER_LETTER, List.of());
        givenDocument(br, DocumentType.TECHNICAL_PROPOSAL, List.of());

        aiWorkflowService.generateDocumentsAsync(br);

        verify(bidFSMService, never()).transition(any(), any(), any(), any(), any());
    }

    // TC-DOC-247: 읽기 회차가 결과를 마감한 뒤 분석 단계이고 회차가 없는 건마다 입찰 분석 시작을 본다 (CR-170, BIZ-090)
    @Test
    @DisplayName("TC-DOC-247 읽기회차끝_마감뒤_입찰분석시작확인")
    void 읽기회차끝_입찰분석시작확인() {
        UUID bidRequestId = UUID.randomUUID();
        UUID readingId = UUID.randomUUID();
        UUID otherBid = UUID.randomUUID();
        given(documentReadingService.beginReading(bidRequestId, List.of(readingId)))
                .willReturn(Map.of("targets", List.of(Map.of("readingId", readingId.toString()))));
        given(bidAnalysisService.pendingBidRequestIds()).willReturn(List.of(bidRequestId, otherBid));

        aiWorkflowService.readClientDocumentsAsync(bidRequestId, List.of(readingId));

        org.mockito.InOrder order = inOrder(documentReadingService, bidAnalysisService);
        order.verify(documentReadingService).closeUnfinished(eq(List.of(readingId)), any());
        order.verify(bidAnalysisService).startIfReady(bidRequestId);
        order.verify(bidAnalysisService).startIfReady(otherBid);
    }

    // TC-DOC-255: 워크플로우가 실패해도 회차를 마감한다 (CR-170, BID-DOC-006)
    @Test
    @DisplayName("TC-DOC-255 입찰분석워크플로우실패_회차마감")
    void 입찰분석워크플로우실패_회차마감() {
        UUID bidRequestId = UUID.randomUUID();
        UUID analysisId = UUID.randomUUID();
        given(llmPlatformClient.runBidAnalysis(any(), any())).willThrow(new RuntimeException("Aimbase 연결 실패"));

        aiWorkflowService.runBidAnalysisAsync(bidRequestId, analysisId);

        then(bidAnalysisService).should().closeUnfinished(analysisId);
    }

    @Test
    @DisplayName("IT-10-002 2 미분류공고_문서작성_설계와절작성입력에성공가이드와참조샘플비어있음")
    void 미분류공고_문서작성입력_가이드샘플비어있음() {
        BidRequest br = bidRequest(opportunity(null));
        br.transitionTo(BidRequestState.GENERATING, UUID.randomUUID(), "admin@example.com", null);
        ReflectionTestUtils.setField(aiWorkflowService, "autoGenerateDocumentTypes", "TECHNICAL_PROPOSAL");
        givenDocument(br, DocumentType.TECHNICAL_PROPOSAL, List.of(TestHelper.withId(ProposalSection.builder().build())));
        List<Map<String, Object>> inputs = new ArrayList<>();
        given(llmPlatformClient.runProposalDesign(any())).willAnswer(call -> { inputs.add(call.getArgument(0)); return null; });
        given(llmPlatformClient.runWriteSection(any())).willAnswer(call -> { inputs.add(call.getArgument(0)); return null; });

        aiWorkflowService.generateDocumentsAsync(br);

        assertThat(inputs).hasSize(2).allSatisfy(input -> assertThat(input.get("successGuide") == null
                && ((List<?>) input.getOrDefault("referenceSamples", List.of())).isEmpty()).isTrue());
    }

    @Test
    @DisplayName("IT-14-030 2 설계워크플로우실패_작성끝_REVIEW로바뀌지않음")
    void 설계워크플로우실패_REVIEW아님() {
        BidRequest br = generatingBy(UUID.randomUUID());
        for (DocumentType type : List.of(DocumentType.COVER_LETTER, DocumentType.TECHNICAL_PROPOSAL)) {
            given(bidDocumentService.findByBidRequestAndType(br.getId(), type))
                    .willReturn(TestHelper.withId(BidDocument.builder().build()));
        }
        given(llmPlatformClient.runProposalDesign(any())).willThrow(new LLMPlatformException("워크플로우 실행 실패: runId=r-1"));

        aiWorkflowService.generateDocumentsAsync(br);

        verify(bidFSMService, never()).transition(any(), eq(BidRequestState.REVIEW), any(), any(), any());
    }
}
