package com.biddingagency.domain.compliance.service;

import com.biddingagency.domain.analysis.service.BidAnalysisService;
import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.repository.ClientDocumentRepository;
import com.biddingagency.domain.bid.service.BidPriceService;
import com.biddingagency.domain.bid.service.BidRequestService;
import com.biddingagency.domain.compliance.entity.FulfillmentStatus;
import com.biddingagency.domain.compliance.entity.FulfillmentType;
import com.biddingagency.domain.compliance.entity.RequirementFulfillmentMap;
import com.biddingagency.domain.compliance.entity.SubmissionCheckNote;
import com.biddingagency.domain.compliance.entity.SubmissionDisposition;
import com.biddingagency.domain.compliance.entity.SubmissionNoteTarget;
import com.biddingagency.domain.compliance.repository.RequirementFulfillmentMapRepository;
import com.biddingagency.domain.compliance.repository.SubmissionCheckNoteRepository;
import com.biddingagency.domain.document.entity.BidDocument;
import com.biddingagency.domain.document.repository.BidDocumentRepository;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.notice.entity.Notice;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.OpportunityRequirementItem;
import com.biddingagency.domain.opportunity.entity.RequirementCategory;
import com.biddingagency.domain.opportunity.entity.RequirementLevel;
import com.biddingagency.domain.proposal.entity.ProposalChapter;
import com.biddingagency.domain.proposal.entity.ProposalSection;
import com.biddingagency.domain.proposal.entity.Verdict;
import com.biddingagency.domain.proposal.entity.VerificationLog;
import com.biddingagency.domain.proposal.entity.VerificationMethod;
import com.biddingagency.domain.proposal.entity.VerificationTargetType;
import com.biddingagency.domain.proposal.service.DocumentCompositionService;
import com.biddingagency.domain.proposal.service.ProposalService;
import com.biddingagency.domain.proposal.service.VerificationLogService;
import com.biddingagency.domain.reading.service.DocumentReadingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** TC-COMP-036 ~ 047: 제출 검사 (CR-194, BID-COMP-004, BIZ-105 ~ 107, BIZ-109) */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubmissionCheckServiceTest {

    private static final LocalDateTime ANALYZED_AT = LocalDateTime.of(2026, 11, 8, 18, 35);
    private static final UUID ANALYSIS_ID = UUID.randomUUID();
    private static final UUID READING_ID = UUID.randomUUID();

    @Mock private BidRequestService bidRequestService;
    @Mock private BidDocumentRepository documentRepository;
    @Mock private ClientDocumentRepository clientDocumentRepository;
    @Mock private RequirementFulfillmentMapRepository fulfillmentMapRepository;
    @Mock private SubmissionCheckNoteRepository noteRepository;
    @Mock private ComplianceService complianceService;
    @Mock private ProposalService proposalService;
    @Mock private VerificationLogService verificationLogService;
    @Mock private DocumentCompositionService documentCompositionService;
    @Mock private DocumentReadingService documentReadingService;
    @Mock private BidAnalysisService bidAnalysisService;
    @Mock private BidPriceService bidPriceService;
    @Mock private Notice notice;

    private SubmissionCheckService service;
    private BidRequest bidRequest;
    private UUID bidRequestId;
    private ProposalSection section;
    private final List<OpportunityRequirementItem> matrix = new ArrayList<>();
    private final List<RequirementFulfillmentMap> maps = new ArrayList<>();
    private final List<SubmissionCheckNote> notes = new ArrayList<>();

    @BeforeEach
    void setUp() {
        service = new SubmissionCheckService(bidRequestService, documentRepository, clientDocumentRepository,
                fulfillmentMapRepository, noteRepository, complianceService, proposalService, verificationLogService,
                documentCompositionService, documentReadingService, bidAnalysisService, bidPriceService,
                new ObjectMapper(), Clock.systemDefaultZone());
        Opportunity opportunity = Opportunity.builder().build();
        ReflectionTestUtils.setField(opportunity, "id", UUID.randomUUID());
        Member member = Member.builder().companyName("NOKHWA").build();
        bidRequest = BidRequest.builder().opportunity(opportunity).member(member).build();
        bidRequestId = UUID.randomUUID();
        ReflectionTestUtils.setField(bidRequest, "id", bidRequestId);

        BidDocument document = BidDocument.builder().bidRequest(bidRequest).build();
        ReflectionTestUtils.setField(document, "id", UUID.randomUUID());
        ProposalChapter chapter = ProposalChapter.builder().documentId(document.getId()).build();
        ReflectionTestUtils.setField(chapter, "id", UUID.randomUUID());
        section = ProposalSection.builder().chapterId(chapter.getId()).title("Quality")
                .requirementRefs(List.of("REQ-001"))
                .sourceReadingIds(List.of(READING_ID.toString())).build();
        ReflectionTestUtils.setField(section, "id", UUID.randomUUID());

        given(bidRequestService.findByIdWithDetails(bidRequestId)).willReturn(bidRequest);
        given(documentRepository.findByBidRequestId(bidRequestId)).willReturn(List.of(document));
        given(documentRepository.findById(document.getId())).willReturn(Optional.of(document));
        given(proposalService.getTree(document.getId()))
                .willReturn(List.of(new ProposalService.ChapterWithSections(chapter, List.of(section))));
        given(proposalService.getSection(section.getId())).willReturn(section);
        given(proposalService.getChapter(chapter.getId())).willReturn(chapter);
        given(notice.getAnalyzedAt()).willReturn(ANALYZED_AT);
        given(documentCompositionService.designNotice(opportunity.getId())).willReturn(Optional.of(notice));
        given(complianceService.matrixLines(opportunity.getId())).willReturn(matrix);
        given(fulfillmentMapRepository.findByBidRequestId(bidRequestId)).willReturn(maps);
        given(noteRepository.findByBidRequestId(bidRequestId)).willReturn(notes);
        given(clientDocumentRepository.findByBidRequestId(bidRequestId)).willReturn(List.of());
        given(documentReadingService.contentsOf(any())).willReturn(Set.of("hash-1"));
        given(bidAnalysisService.decisionConditions(bidRequestId))
                .willReturn(new BidAnalysisService.DecisionConditions(ANALYSIS_ID, List.of()));
        givenPrice(BidPriceService.COMPLETE);
    }

    private void givenPrice(String status) {
        given(bidPriceService.adminTable(bidRequestId))
                .willReturn(new BidPriceService.PriceTable(status, false, List.of(), BigDecimal.ZERO));
    }

    private OpportunityRequirementItem line(String reqId, RequirementLevel level, String party) {
        OpportunityRequirementItem item = OpportunityRequirementItem.builder()
                .title(reqId).category(RequirementCategory.ELIGIBILITY).reqId(reqId).reqLevel(level)
                .requirementJson(Map.of(RequirementSyncService.KEY_FULFILLMENT_PARTY, party)).build();
        ReflectionTestUtils.setField(item, "id", UUID.randomUUID());
        matrix.add(item);
        return item;
    }

    private OpportunityRequirementItem shall() {
        return line("REQ-001", RequirementLevel.SHALL, "PLATFORM_GENERATED");
    }

    private void record(OpportunityRequirementItem item, FulfillmentStatus status) {
        maps.add(RequirementFulfillmentMap.builder().requirementItem(item)
                .fulfillmentType(FulfillmentType.DOCUMENT_SECTION)
                .documentSectionPath(section.getId().toString())
                .status(status).notes("quote").build());
    }

    private void givenVerdict(Verdict verdict) {
        givenVerdict(verdict, service.currentBundle(section.getId()));
    }

    private void givenVerdict(Verdict verdict, String bundle) {
        given(verificationLogService.findLatest(VerificationTargetType.PROPOSAL_SECTION, section.getId()))
                .willReturn(Optional.of(VerificationLog.builder().method(VerificationMethod.LLM).verdict(verdict)
                        .inputBundleJson(bundle).build()));
    }

    private void givenLineNote(String reqId, SubmissionDisposition disposition, LocalDateTime analyzedAt) {
        notes.add(SubmissionCheckNote.builder().bidRequestId(bidRequestId).targetType(SubmissionNoteTarget.REQ_LINE)
                .targetKey(reqId).disposition(disposition).reason("근거").sourceRef("p.1")
                .noticeAnalyzedAt(analyzedAt).decidedBy("admin").decidedAt(ANALYZED_AT).build());
    }

    private String statusOfFirstLine() {
        return service.check(bidRequestId).lines().get(0).status();
    }

    @Test
    @DisplayName("TC-COMP-036 제출검사_SHALL줄_채우는쪽모두포함")
    void SHALL줄_채우는쪽_모두() {
        line("REQ-001", RequirementLevel.SHALL, "CLIENT_UPLOAD");
        line("REQ-002", RequirementLevel.SHALL, "PLATFORM_GENERATED");
        line("REQ-003", RequirementLevel.SHALL, "SYSTEM_FORM");
        line("REQ-004", RequirementLevel.SHOULD, "PLATFORM_GENERATED");

        assertThat(service.check(bidRequestId).lines()).hasSize(3);
    }

    @Test
    @DisplayName("TC-COMP-037 제출검사_답함기록과통과_충족")
    void 답함과_통과_충족() {
        record(shall(), FulfillmentStatus.FULFILLED);
        givenVerdict(Verdict.PASS);

        assertThat(statusOfFirstLine()).isEqualTo("MET");
    }

    @Test
    @DisplayName("TC-COMP-038 제출검사_미충족기록_미충족")
    void 미충족기록() {
        record(shall(), FulfillmentStatus.MISSING);
        givenVerdict(Verdict.PASS);

        SubmissionCheckService.Check check = service.check(bidRequestId);

        assertThat(check.lines().get(0).status()).isEqualTo("NOT_MET");
        assertThat(check.submittable()).isFalse();
    }

    @Test
    @DisplayName("TC-COMP-039 제출검사_기록없음_확인안됨")
    void 기록없음() {
        line("REQ-009", RequirementLevel.SHALL, "CLIENT_UPLOAD");
        givenVerdict(Verdict.PASS);

        SubmissionCheckService.Check check = service.check(bidRequestId);

        assertThat(check.lines().get(0).status()).isEqualTo("NOT_CHECKED");
        assertThat(check.submittable()).isFalse();
    }

    @Test
    @DisplayName("TC-COMP-040 제출검사_절검증못함_줄도검증못함")
    void 절검증못함() {
        record(shall(), FulfillmentStatus.FULFILLED);
        givenVerdict(Verdict.UNVERIFIED, null);

        assertThat(statusOfFirstLine()).isEqualTo("UNVERIFIED");
    }

    @Test
    @DisplayName("TC-COMP-041 제출검사_입력바뀜_검증못함")
    void 입력바뀜() {
        record(shall(), FulfillmentStatus.FULFILLED);
        givenVerdict(Verdict.PASS);
        given(documentReadingService.contentsOf(any())).willReturn(Set.of("hash-2"));

        SubmissionCheckService.Check check = service.check(bidRequestId);

        assertThat(check.lines().get(0).status()).isEqualTo("UNVERIFIED");
        assertThat(check.sections().get(0).inputChanged()).isTrue();
    }

    @Test
    @DisplayName("TC-COMP-042 줄처리_답하지않는줄_근거빠지면거절")
    void 답하지않는줄_근거없음_거절() {
        shall();

        assertThatThrownBy(() -> service.saveLineNote(bidRequestId, "REQ-001", SubmissionDisposition.NOT_ANSWERED, "사유", null, "admin"))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
        verify(noteRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-COMP-043 줄처리_동등인정_근거있어야풀림")
    void 동등인정() {
        shall();
        givenVerdict(Verdict.PASS);
        givenLineNote("REQ-001", SubmissionDisposition.EQUIVALENT, ANALYZED_AT);

        SubmissionCheckService.Check check = service.check(bidRequestId);

        assertThat(check.lines().get(0).status()).isEqualTo("EQUIVALENT");
        assertThat(check.blockers()).noneMatch(b -> b.code().startsWith("LINE_"));
    }

    @Test
    @DisplayName("TC-COMP-044 줄처리_공고다시분석_무효")
    void 공고다시분석_처리무효() {
        shall();
        givenLineNote("REQ-001", SubmissionDisposition.EQUIVALENT, ANALYZED_AT.minusDays(1));

        assertThat(statusOfFirstLine()).isEqualTo("UNVERIFIED");
    }

    @Test
    @DisplayName("TC-COMP-045 결정조건_해소근거없음_미해결")
    void 결정조건_미해결() {
        record(shall(), FulfillmentStatus.FULFILLED);
        givenVerdict(Verdict.PASS);
        given(bidAnalysisService.decisionConditions(bidRequestId))
                .willReturn(new BidAnalysisService.DecisionConditions(ANALYSIS_ID, List.of("조건 하나")));

        SubmissionCheckService.Check check = service.check(bidRequestId);

        assertThat(check.conditions().get(0).resolved()).isFalse();
        assertThat(check.submittable()).isFalse();
    }

    @Test
    @DisplayName("TC-COMP-046 제출검사_전부맞음_제출가능")
    void 전부맞음() {
        record(shall(), FulfillmentStatus.FULFILLED);
        line("REQ-002", RequirementLevel.SHALL, "CLIENT_UPLOAD");
        givenLineNote("REQ-002", SubmissionDisposition.NOT_ANSWERED, ANALYZED_AT);
        given(bidAnalysisService.decisionConditions(bidRequestId))
                .willReturn(new BidAnalysisService.DecisionConditions(ANALYSIS_ID, List.of("조건 하나")));
        givenVerdict(Verdict.PASS);
        notes.add(SubmissionCheckNote.builder().bidRequestId(bidRequestId).targetType(SubmissionNoteTarget.DECISION_CONDITION)
                .targetKey(ANALYSIS_ID + ":0").disposition(SubmissionDisposition.RESOLVED).reason("해소")
                .noticeAnalyzedAt(ANALYZED_AT).decidedBy("admin").decidedAt(ANALYZED_AT).build());

        SubmissionCheckService.Check check = service.check(bidRequestId);

        assertThat(check.submittable()).isTrue();
        assertThat(check.blockers()).isEmpty();
    }

    @Test
    @DisplayName("TC-COMP-047 제출검사_산출물그대로")
    void 산출물_그대로() {
        record(shall(), FulfillmentStatus.FULFILLED);
        givenVerdict(Verdict.PASS);

        service.check(bidRequestId);
        service.saveLineNote(bidRequestId, "REQ-001", SubmissionDisposition.APPLICABILITY_UNCONFIRMED, null, null, "admin");

        verify(fulfillmentMapRepository, never()).save(any());
        verify(verificationLogService, never()).recordLlm(any(), any(), any(), any(), any(), any(), any(), eq(1));
        verify(proposalService, never()).markDrafting(any());
        verify(noteRepository).findByBidRequestIdAndTargetTypeAndTargetKey(eq(bidRequestId), eq(SubmissionNoteTarget.REQ_LINE), anyString());
    }
}
