package com.biddingagency.domain.reading.service;

import com.biddingagency.TestHelper;
import com.biddingagency.common.ContentHash;
import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.entity.BidRequestState;
import com.biddingagency.domain.bid.entity.ClientDocument;
import com.biddingagency.domain.bid.repository.BidRequestRepository;
import com.biddingagency.domain.bid.repository.ClientDocumentRepository;
import com.biddingagency.domain.bid.service.AIWorkflowService;
import com.biddingagency.domain.compliance.entity.FulfillmentType;
import com.biddingagency.domain.compliance.entity.RequirementFulfillmentMap;
import com.biddingagency.domain.compliance.repository.RequirementFulfillmentMapRepository;
import com.biddingagency.domain.member.entity.DocumentCategory;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.member.entity.MemberDocument;
import com.biddingagency.domain.member.repository.MemberDocumentRepository;
import com.biddingagency.domain.opportunity.entity.OpportunityRequirementItem;
import com.biddingagency.domain.reading.DocumentReadingException;
import com.biddingagency.domain.reading.dto.DocumentReadingDto.Origin;
import com.biddingagency.domain.reading.dto.DocumentReadingDto.Overview;
import com.biddingagency.domain.reading.entity.DocumentReading;
import com.biddingagency.domain.reading.entity.ReadAs;
import com.biddingagency.domain.reading.entity.ReadingStatus;
import com.biddingagency.domain.reading.repository.DocumentReadingRepository;
import com.biddingagency.integration.storage.StorageService;
import com.biddingagency.security.McpAccessGuard;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

/** TC-DOC-210~213, 217, 218, 220~225, 227~235: 고객 서류 읽기 (CR-169) */
@ExtendWith(MockitoExtension.class)
class DocumentReadingServiceTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-24T03:00:00Z"), SEOUL);
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    @Mock private DocumentReadingRepository readingRepository;
    @Mock private BidRequestRepository bidRequestRepository;
    @Mock private ClientDocumentRepository clientDocumentRepository;
    @Mock private MemberDocumentRepository memberDocumentRepository;
    @Mock private RequirementFulfillmentMapRepository fulfillmentMapRepository;
    @Mock private StorageService storageService;
    @Mock private McpAccessGuard mcpAccessGuard;
    @Mock private AIWorkflowService aiWorkflowService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<UUID, DocumentReading> store = new LinkedHashMap<>();
    private final List<ClientDocument> bidDocuments = new ArrayList<>();
    private final List<MemberDocument> vaultDocuments = new ArrayList<>();
    private final List<RequirementFulfillmentMap> fulfillmentMaps = new ArrayList<>();

    private DocumentReadingService service;
    private Member member;
    private BidRequest bid;

    @BeforeEach
    void setUp() {
        service = new DocumentReadingService(readingRepository, bidRequestRepository, clientDocumentRepository,
                memberDocumentRepository, fulfillmentMapRepository, storageService, mcpAccessGuard, aiWorkflowService,
                objectMapper, CLOCK, 5000, 360);
        member = TestHelper.withId(Member.builder().build());
        bid = bidIn(BidRequestState.ANALYZING);

        lenient().when(clientDocumentRepository.findByBidRequestId(any())).thenAnswer(inv -> bidDocuments);
        lenient().when(memberDocumentRepository.findByMemberIdOrderByCreatedAtDesc(any())).thenAnswer(inv -> vaultDocuments);
        lenient().when(fulfillmentMapRepository.findByBidRequestId(any())).thenAnswer(inv -> fulfillmentMaps);
        lenient().when(readingRepository.save(any(DocumentReading.class))).thenAnswer(inv -> {
            DocumentReading r = inv.getArgument(0);
            if (r.getId() == null) TestHelper.withId(r);
            store.put(r.getId(), r);
            return r;
        });
        lenient().when(readingRepository.findByContentSha256In(any())).thenAnswer(inv -> {
            Collection<String> shas = inv.getArgument(0);
            return store.values().stream().filter(r -> shas.contains(r.getContentSha256())).toList();
        });
        lenient().when(readingRepository.findAllById(any())).thenAnswer(inv -> {
            Iterable<UUID> ids = inv.getArgument(0);
            List<DocumentReading> out = new ArrayList<>();
            for (UUID id : ids) if (store.containsKey(id)) out.add(store.get(id));
            return out;
        });
        lenient().when(readingRepository.findById(any())).thenAnswer(inv -> Optional.ofNullable(store.get(inv.<UUID>getArgument(0))));
        lenient().when(mcpAccessGuard.documentReadingUrl(any())).thenAnswer(inv -> "http://test/mcp/document-readings/" + inv.getArgument(0) + "/download");
    }

    // ─── 준비 ───────────────────────────────────────────────────────────

    private BidRequest bidIn(BidRequestState state) {
        BidRequest b = TestHelper.withId(BidRequest.builder().member(member).state(state).stateHistory(new ArrayList<>()).build());
        lenient().when(bidRequestRepository.findByIdWithDetails(b.getId())).thenReturn(Optional.of(b));
        return b;
    }

    private ClientDocument bidDocument(String fileName, String sha) {
        ClientDocument d = TestHelper.withId(ClientDocument.builder()
                .bidRequest(bid).fileName(fileName).storageUrl("bid/" + fileName)
                .documentCategory("BUSINESS_REGISTRATION").contentSha256(sha).build());
        bidDocuments.add(d);
        return d;
    }

    private MemberDocument vaultDocument(String fileName, String sha, LocalDate expiresOn) {
        MemberDocument d = TestHelper.withId(MemberDocument.builder()
                .memberId(member.getId()).category(DocumentCategory.BUSINESS_REGISTRATION)
                .fileName(fileName).storageUrl("vault/" + fileName).expiresOn(expiresOn).contentSha256(sha).build());
        vaultDocuments.add(d);
        return d;
    }

    private DocumentReading reading(String sha, String fileName, ReadingStatus status, LocalDateTime statusAt) {
        DocumentReading r = TestHelper.withId(DocumentReading.builder()
                .contentSha256(sha).fileName(fileName).readAs(ReadableForm.readAs(fileName, null))
                .status(status).statusAt(statusAt).build());
        store.put(r.getId(), r);
        return r;
    }

    private DocumentReading done(String sha, String fileName, List<Map<String, Object>> facts) throws Exception {
        DocumentReading r = reading(sha, fileName, ReadingStatus.DONE, NOW.minusDays(1));
        r.markDone(objectMapper.writeValueAsString(facts), facts.size(), false, NOW.minusDays(1));
        return r;
    }

    private static Map<String, Object> fact(String key, String value) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("category", "COMPANY");
        f.put("key", key);
        f.put("value", value);
        f.put("page", "1");
        f.put("anchor", "표 1");
        f.put("confidence", "HIGH");
        f.put("readMode", "TEXT");
        return f;
    }

    private static Map<String, Object> target(DocumentReading r, String path) {
        return Map.of("readingId", r.getId().toString(), "targetPath", path);
    }

    private static Map<String, Object> element(Integer index, List<Map<String, Object>> facts) {
        Map<String, Object> e = new LinkedHashMap<>();
        if (index != null) e.put("index", index);
        e.put("status", "ok");
        e.put("structured_data", Map.of("facts", facts));
        return e;
    }

    // ─── 후보 모으기 (BIZ-085) ──────────────────────────────────────────

    @Test
    @DisplayName("TC-DOC-210 분석진입_후보모음_만료서류뺌")
    void 분석진입_후보모음_만료서류뺌() {
        bidDocument("a.pdf", "a");
        bidDocument("b.pdf", "b");
        vaultDocument("c.pdf", "c", null);
        vaultDocument("d.pdf", "d", LocalDate.now(CLOCK));
        vaultDocument("e.pdf", "e", LocalDate.now(CLOCK).minusDays(1));

        assertThat(service.overview(bid.getId()).items()).hasSize(4);
    }

    @Test
    @DisplayName("TC-DOC-211 서류함복사본_같은해시_한후보")
    void 서류함복사본_같은해시_한후보() {
        vaultDocument("사업자등록증.pdf", "x", null);
        ClientDocument copy = bidDocument("공고용_사업자등록증.pdf", "x");

        Overview overview = service.overview(bid.getId());

        assertThat(overview.items()).extracting("origin", "fileName", "documentId")
                .containsExactly(org.assertj.core.groups.Tuple.tuple(Origin.BID_DOCUMENT, "공고용_사업자등록증.pdf", copy.getId()));
    }

    @Test
    @DisplayName("TC-DOC-212 다른건에서읽은해시_다시안읽음")
    void 다른건에서읽은해시_다시안읽음() throws Exception {
        bidDocument("a.pdf", "a");
        DocumentReading readElsewhere = done("a", "a.pdf", List.of(fact("상호", "우진")));

        List<UUID> targets = service.queueForAnalysis(bid.getId());

        assertThat(List.<Object>of(targets, readElsewhere.getStatus())).containsExactly(List.of(), ReadingStatus.DONE);
    }

    @Test
    @DisplayName("TC-DOC-213 해시빈옛줄_읽기시작때채움")
    void 해시빈옛줄_읽기시작때채움() {
        byte[] bytes = "old file".getBytes(StandardCharsets.UTF_8);
        ClientDocument old = bidDocument("old.pdf", null);
        given(storageService.load("bid/old.pdf")).willReturn(bytes);

        service.queueForAnalysis(bid.getId());

        assertThat(old.getContentSha256()).isEqualTo(ContentHash.of(bytes));
    }

    // ─── 읽는 꼴 (BIZ-086) ──────────────────────────────────────────────

    @Test
    @DisplayName("TC-DOC-217 HWP_변환대상에넣음")
    @SuppressWarnings("unchecked")
    void HWP_변환대상에넣음() {
        bidDocument("profile.hwp", "h");
        List<UUID> ids = service.queueForAnalysis(bid.getId());

        Map<String, Object> input = service.beginReading(bid.getId(), ids);

        String id = ids.get(0).toString();
        assertThat(List.<Object>of(((List<?>) input.get("hwpFiles")).size(),
                ((List<Map<String, Object>>) input.get("targets")).get(0).get("targetPath")))
                .containsExactly(1, "docs/" + id + ".txt");
    }

    @Test
    @DisplayName("TC-DOC-218 DOC옛형식_읽을수없는형식")
    void DOC옛형식_읽을수없는형식() {
        bidDocument("old.doc", "d");

        List<UUID> targets = service.queueForAnalysis(bid.getId());

        assertThat(List.<Object>of(targets, store.values().iterator().next().getStatus()))
                .containsExactly(List.of(), ReadingStatus.UNSUPPORTED);
    }

    // ─── 결과 저장 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-DOC-220 결과저장_경로맞춤_읽음")
    void 결과저장_경로맞춤_읽음() {
        bidDocument("a.pdf", "a");
        bidDocument("b.pdf", "b");
        DocumentReading a = reading("a", "a.pdf", ReadingStatus.READING, NOW);
        DocumentReading b = reading("b", "b.pdf", ReadingStatus.READING, NOW);
        Map<String, Object> fa = fact("상호", "우진");
        fa.put("sourceFile", "docs/b.pdf");
        Map<String, Object> fb = fact("대표", "김");
        fb.put("sourceFile", "docs/a.pdf");

        service.saveFacts(bid.getId(), List.of(target(a, "docs/a.pdf"), target(b, "docs/b.pdf")),
                List.of(element(0, List.of(fa, fb))));

        assertThat(store.values()).extracting("status", "factCount", "readAt")
                .containsExactly(org.assertj.core.groups.Tuple.tuple(ReadingStatus.DONE, 1, NOW),
                        org.assertj.core.groups.Tuple.tuple(ReadingStatus.DONE, 1, NOW));
    }

    @Test
    @DisplayName("TC-DOC-221 사실0개_읽음")
    void 사실0개_읽음() {
        bidDocument("a.pdf", "a");
        DocumentReading a = reading("a", "a.pdf", ReadingStatus.READING, NOW);

        service.saveFacts(bid.getId(), List.of(target(a, "docs/a.pdf")), List.of(element(0, List.of())));

        assertThat(List.<Object>of(a.getStatus(), a.getFactCount())).containsExactly(ReadingStatus.DONE, 0);
    }

    @Test
    @DisplayName("TC-DOC-222 VISION사실있음_글자인식표시")
    void VISION사실있음_글자인식표시() {
        bidDocument("scan.pdf", "s");
        DocumentReading s = reading("s", "scan.pdf", ReadingStatus.READING, NOW);
        Map<String, Object> vision = fact("면허번호", "123");
        vision.put("readMode", "VISION");

        service.saveFacts(bid.getId(), List.of(target(s, "docs/s.pdf")),
                List.of(element(0, List.of(fact("상호", "우진"), fact("대표", "김"), vision))));

        assertThat(s.isVisionUsed()).isTrue();
    }

    @Test
    @DisplayName("TC-DOC-223 대상밖경로_버림")
    void 대상밖경로_버림() {
        bidDocument("a.pdf", "a");
        DocumentReading a = reading("a", "a.pdf", ReadingStatus.READING, NOW);
        Map<String, Object> stray = fact("상호", "우진");
        stray.put("sourceFile", "docs/unknown.pdf");

        DocumentReadingService.SaveResult result = service.saveFacts(bid.getId(),
                List.of(target(a, "docs/a.pdf")), List.of(element(null, List.of(stray))));

        assertThat(List.<Object>of(result.saved(), result.ignored(), a.getStatus()))
                .containsExactly(0, 1, ReadingStatus.READING);
    }

    @Test
    @DisplayName("TC-DOC-224 실행끝_결과없는줄_읽지못함")
    void 실행끝_결과없는줄_읽지못함() {
        bidDocument("a.pdf", "a");
        bidDocument("b.pdf", "b");
        DocumentReading a = reading("a", "a.pdf", ReadingStatus.READING, NOW);
        DocumentReading b = reading("b", "b.pdf", ReadingStatus.READING, NOW);
        service.saveFacts(bid.getId(), List.of(target(a, "docs/a.pdf"), target(b, "docs/b.pdf")),
                List.of(element(0, List.of(fact("상호", "우진")))));

        service.closeUnfinished(List.of(a.getId(), b.getId()), "읽기 결과가 오지 않음");

        assertThat(List.<Object>of(a.getStatus(), b.getStatus(), b.getFailureReason()))
                .containsExactly(ReadingStatus.DONE, ReadingStatus.FAILED, "읽기 결과가 오지 않음");
    }

    @Test
    @DisplayName("TC-DOC-225 읽는중30분지남_읽지못함")
    void 읽는중30분지남_읽지못함() {
        bidDocument("a.pdf", "a");
        DocumentReading a = reading("a", "a.pdf", ReadingStatus.READING, NOW.minusMinutes(31));

        service.overview(bid.getId());

        assertThat(List.<Object>of(a.getStatus(), a.getFailureReason()))
                .containsExactly(ReadingStatus.FAILED, DocumentReadingService.TIMEOUT_REASON);
    }

    // ─── 전환 막기와 다시 읽기 (BIZ-087) ────────────────────────────────

    @Test
    @DisplayName("TC-DOC-227 읽는중_생성전환_거부(서비스)")
    void 읽는중_생성전환_거부() {
        bidDocument("a.pdf", "a");
        reading("a", "a.pdf", ReadingStatus.READING, NOW.minusMinutes(5));

        assertThatThrownBy(() -> service.requireNoReadingInProgress(bid.getId()))
                .isInstanceOf(DocumentReadingException.class)
                .extracting("code").isEqualTo("DOCUMENT_READING_IN_PROGRESS");
    }

    @Test
    @DisplayName("TC-DOC-228 읽지못함과형식밖만_생성전환_허용")
    void 읽지못함과형식밖만_생성전환_허용() throws Exception {
        bidDocument("a.pdf", "a");
        bidDocument("old.doc", "d");
        bidDocument("c.pdf", "c");
        reading("a", "a.pdf", ReadingStatus.FAILED, NOW);
        reading("d", "old.doc", ReadingStatus.UNSUPPORTED, NOW);
        done("c", "c.pdf", List.of());

        service.requireNoReadingInProgress(bid.getId());

        assertThat(service.overview(bid.getId()).inProgress()).isFalse();
    }

    @Test
    @DisplayName("TC-DOC-229 분석단계아님_다시읽기_거부")
    void 분석단계아님_다시읽기_거부() {
        BidRequest generating = bidIn(BidRequestState.GENERATING);

        assertThatThrownBy(() -> service.rerun(generating.getId(), null, false))
                .isInstanceOf(DocumentReadingException.class)
                .extracting("code").isEqualTo("NOT_ANALYZING");
    }

    @Test
    @DisplayName("TC-DOC-230 force없음_읽음은건너뜀")
    void force없음_읽음은건너뜀() throws Exception {
        bidDocument("a.pdf", "a");
        bidDocument("b.pdf", "b");
        done("a", "a.pdf", List.of());
        DocumentReading failed = reading("b", "b.pdf", ReadingStatus.FAILED, NOW);

        service.rerun(bid.getId(), null, false);

        then(aiWorkflowService).should().readClientDocumentsAsync(bid.getId(), List.of(failed.getId()));
    }

    @Test
    @DisplayName("TC-DOC-231 남의건서류_다시읽기_거부")
    void 남의건서류_다시읽기_거부() {
        bidDocument("a.pdf", "a");
        DocumentReading others = reading("z", "z.pdf", ReadingStatus.FAILED, NOW);

        assertThatThrownBy(() -> service.rerun(bid.getId(), List.of(others.getId()), false))
                .isInstanceOf(DocumentReadingException.class)
                .extracting("code").isEqualTo("NOT_CANDIDATE");
        then(aiWorkflowService).should(never()).readClientDocumentsAsync(any(), anyList());
    }

    // ─── 제안서 도구 (BID-DOC-005) ──────────────────────────────────────

    @Test
    @DisplayName("TC-DOC-232 설계입력_고객서류_충족항목과사실요약")
    @SuppressWarnings("unchecked")
    void 설계입력_고객서류_충족항목과사실요약() throws Exception {
        ClientDocument licence = bidDocument("면허.pdf", "a");
        bidDocument("b.pdf", "b");
        done("a", "면허.pdf", List.of(fact("면허 범위", "가".repeat(400))));
        reading("b", "b.pdf", ReadingStatus.FAILED, NOW);
        OpportunityRequirementItem item = TestHelper.withId(OpportunityRequirementItem.builder()
                .title("건설업 면허").reqId("REQ-001").build());
        fulfillmentMaps.add(RequirementFulfillmentMap.builder().bidRequest(bid).requirementItem(item)
                .fulfillmentType(FulfillmentType.CLIENT_DOCUMENT).clientDocument(licence).build());

        List<Map<String, Object>> docs = service.designInputDocuments(bid.getId());

        Map<String, Object> first = docs.get(0);
        String value = (String) ((List<Map<String, Object>>) first.get("facts")).get(0).get("value");
        assertThat(List.<Object>of(docs.size(), ((List<?>) first.get("fulfills")).size(), value.length(),
                docs.get(1).get("status"), docs.get(1).containsKey("facts")))
                .containsExactly(2, 1, 300, "FAILED", false);
    }

    @Test
    @DisplayName("TC-DOC-233 설계저장_읽음아닌근거_전체거부")
    void 설계저장_읽음아닌근거_전체거부() {
        bidDocument("b.pdf", "b");
        DocumentReading failed = reading("b", "b.pdf", ReadingStatus.FAILED, NOW);

        assertThatThrownBy(() -> service.requireReadableSources(bid.getId(), List.of(failed.getId().toString())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknownReadingIds: [" + failed.getId() + "]");
    }

    @Test
    @DisplayName("TC-DOC-234 설계저장_안쓴읽음서류_응답에")
    void 설계저장_안쓴읽음서류_응답에() throws Exception {
        bidDocument("a.pdf", "a");
        bidDocument("b.pdf", "b");
        DocumentReading used = done("a", "a.pdf", List.of());
        DocumentReading unused = done("b", "b.pdf", List.of());

        assertThat(service.unusedReadingIds(bid.getId(), List.of(used.getId().toString())))
                .containsExactly(unused.getId().toString());
    }

    @Test
    @DisplayName("TC-DOC-235 절정보_근거서류사실전체")
    @SuppressWarnings("unchecked")
    void 절정보_근거서류사실전체() throws Exception {
        bidDocument("a.pdf", "a");
        DocumentReading a = done("a", "a.pdf", List.of(fact("상호", "우진"), fact("대표", "김"),
                fact("설립", "2001"), fact("주소", "서울"), fact("자본금", "10억")));

        List<Map<String, Object>> docs = service.sourceDocuments(bid.getId(), List.of(a.getId().toString()));

        List<Map<String, Object>> facts = (List<Map<String, Object>>) docs.get(0).get("facts");
        assertThat(List.<Object>of(docs.size(), facts.size(), facts.get(0).get("page"), facts.get(0).get("anchor")))
                .containsExactly(1, 5, "1", "표 1");
    }

    @Test
    @DisplayName("다시읽기_대상줄_대기로바꿈")
    void 다시읽기_대상줄_대기로바꿈() {
        bidDocument("a.pdf", "a");
        DocumentReading failed = reading("a", "a.pdf", ReadingStatus.FAILED, NOW.minusHours(1));

        service.rerun(bid.getId(), List.of(failed.getId()), false);

        assertThat(failed.getStatus()).isEqualTo(ReadingStatus.WAITING);
    }

    @Test
    @DisplayName("분석중_새로올린서류_다시읽기가맡음")
    void 분석중_새로올린서류_다시읽기가맡음() {
        bidDocument("a.pdf", "a");

        service.rerun(bid.getId(), null, false);

        DocumentReading row = store.values().iterator().next();
        then(aiWorkflowService).should().readClientDocumentsAsync(bid.getId(), List.of(row.getId()));
    }

    @Test
    @DisplayName("읽기시작_대기줄만_읽는중으로")
    void 읽기시작_대기줄만_읽는중으로() {
        bidDocument("a.pdf", "a");
        DocumentReading waiting = reading("a", "a.pdf", ReadingStatus.WAITING, NOW);
        DocumentReading reading = reading("b", "b.pdf", ReadingStatus.READING, NOW);

        Map<String, Object> input = service.beginReading(bid.getId(), List.of(waiting.getId(), reading.getId()));

        assertThat(List.<Object>of(((List<?>) input.get("targets")).size(), waiting.getStatus()))
                .containsExactly(1, ReadingStatus.READING);
    }

    @Test
    @DisplayName("처음본내용_대기줄만듦_분석진입때읽기시작")
    void 처음본내용_대기줄만듦_분석진입때읽기시작() {
        bidDocument("a.pdf", "a");

        List<UUID> targets = service.queueForAnalysis(bid.getId());

        DocumentReading row = store.values().iterator().next();
        assertThat(List.<Object>of(row.getReadAs(), row.getStatus(), targets))
                .containsExactly(ReadAs.PDF, ReadingStatus.WAITING, List.of(row.getId()));
        then(aiWorkflowService).should().readClientDocumentsAsync(bid.getId(), targets);
    }

    @Test
    @DisplayName("서버파일없음_해시못냄_후보는남김")
    void 서버파일없음_해시못냄_후보는남김() {
        bidDocument("lost.pdf", null);
        given(storageService.load(anyString())).willThrow(new IllegalStateException("없음"));

        assertThat(service.overview(bid.getId()).items()).extracting("readingId").containsExactly((Object) null);
    }
}
