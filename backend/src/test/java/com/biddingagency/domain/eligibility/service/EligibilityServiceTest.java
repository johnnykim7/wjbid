package com.biddingagency.domain.eligibility.service;

import com.biddingagency.TestHelper;
import com.biddingagency.domain.eligibility.entity.EligibilityAnswer;
import com.biddingagency.domain.eligibility.entity.EligibilityAnswerValue;
import com.biddingagency.domain.eligibility.entity.EligibilityBlockerProblem;
import com.biddingagency.domain.eligibility.entity.EligibilityVerdict;
import com.biddingagency.domain.eligibility.exception.EligibilityAnswerInvalidException;
import com.biddingagency.domain.eligibility.exception.EligibilityDocumentNotAllowedException;
import com.biddingagency.domain.eligibility.exception.EligibilityDocumentNotFoundException;
import com.biddingagency.domain.eligibility.exception.EligibilityItemInvalidException;
import com.biddingagency.domain.eligibility.exception.EligibilityNoticeNotFoundException;
import com.biddingagency.domain.eligibility.repository.EligibilityAnswerRepository;
import com.biddingagency.domain.member.entity.DocumentCategory;
import com.biddingagency.domain.member.entity.MemberDocument;
import com.biddingagency.domain.member.repository.MemberDocumentRepository;
import com.biddingagency.domain.notice.entity.Notice;
import com.biddingagency.domain.notice.entity.NoticeGenerationStatus;
import com.biddingagency.domain.notice.repository.NoticeRepository;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.OpportunityVisibility;
import com.biddingagency.dto.EligibilityResponses.Blocker;
import com.biddingagency.dto.EligibilityResponses.Detail;
import com.biddingagency.dto.EligibilityResponses.Item;
import com.biddingagency.dto.EligibilityResponses.NoticeRow;
import com.biddingagency.dto.EligibilityResponses.Saved;
import com.biddingagency.dto.EligibilityResponses.Summary;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.lenient;

/**
 * TC-QUAL-001 ~ TC-QUAL-014 (CR-137): 적격성 진단 목록, 상세, 답 저장(BID-QUAL-001).
 * TC-QUAL-016 ~ TC-QUAL-034 (CR-140): 공고별 판정과 증빙 서류 붙이기(BID-MKT-015, BID-MKT-018, BID-MKT-019).
 *
 * TC-QUAL-015(공고문을 지우면 답도 지워짐)는 DB 외래 키(ON DELETE CASCADE)가 하는 일이라 여기서 보지 않는다.
 * 저장소는 목록에 담아 흉내 내 행 수와 저장된 값을 본다.
 */
@ExtendWith(MockitoExtension.class)
class EligibilityServiceTest {

    /** 시험 기준 시각 2026-10-11 10:00 KST */
    private static final Clock NOW = Clock.fixed(Instant.parse("2026-10-11T01:00:00Z"), ZoneId.of("Asia/Seoul"));
    private static final LocalDateTime DEADLINE = LocalDateTime.of(2026, 10, 20, 17, 0);
    private static final ObjectMapper JSON = new ObjectMapper();

    @Mock private NoticeRepository noticeRepository;
    @Mock private EligibilityAnswerRepository answerRepository;
    @Mock private MemberDocumentRepository memberDocumentRepository;

    private EligibilityService service;
    private final UUID me = UUID.randomUUID();
    private final UUID other = UUID.randomUUID();
    private final List<Notice> notices = new ArrayList<>();
    private final List<EligibilityAnswer> rows = new ArrayList<>();
    private final List<MemberDocument> vault = new ArrayList<>();

    @BeforeEach
    void setUp() {
        service = new EligibilityService(noticeRepository, answerRepository, memberDocumentRepository, NOW);
        lenient().when(noticeRepository.findByIdAndVisibility(any(), any())).thenAnswer(inv -> notices.stream()
                .filter(n -> n.getId().equals(inv.getArgument(0)) && n.getVisibility() == inv.getArgument(1))
                .findFirst());
        lenient().when(noticeRepository.findVisible(any(), anyBoolean(), any(), any())).thenAnswer(inv ->
                new PageImpl<>(notices.stream().filter(n -> n.getVisibility() == inv.getArgument(0)).toList()));
        lenient().when(answerRepository.findByMemberIdAndNoticeId(any(), any())).thenAnswer(inv -> rows.stream()
                .filter(a -> a.getMemberId().equals(inv.getArgument(0)) && a.getNoticeId().equals(inv.getArgument(1)))
                .toList());
        lenient().when(answerRepository.findByMemberIdAndNoticeIdIn(any(), any())).thenAnswer(inv -> {
            Collection<UUID> ids = inv.getArgument(1);
            return rows.stream()
                    .filter(a -> a.getMemberId().equals(inv.getArgument(0)) && ids.contains(a.getNoticeId()))
                    .toList();
        });
        lenient().when(answerRepository.findByMemberIdAndNoticeIdAndItemNo(any(), any(), any())).thenAnswer(inv ->
                rows.stream().filter(a -> a.getMemberId().equals(inv.getArgument(0))
                        && a.getNoticeId().equals(inv.getArgument(1)) && a.getItemNo().equals(inv.getArgument(2)))
                        .findFirst());
        lenient().when(answerRepository.saveAndFlush(any(EligibilityAnswer.class))).thenAnswer(inv -> {
            EligibilityAnswer a = inv.getArgument(0);
            if (!rows.contains(a)) {
                rows.add(TestHelper.withId(a));
            }
            return a;
        });
        lenient().when(memberDocumentRepository.findByMemberIdOrderByCreatedAtDesc(any())).thenAnswer(inv ->
                vault.stream().filter(d -> d.getMemberId().equals(inv.getArgument(0))).toList());
        lenient().when(memberDocumentRepository.findById(any())).thenAnswer(inv ->
                vault.stream().filter(d -> d.getId().equals(inv.getArgument(0))).findFirst());
    }

    private static Map<String, Object> item(String title, boolean gate, String evidenceBy) {
        Map<String, Object> m = new HashMap<>();
        m.put("title", title);
        m.put("isGate", gate);
        m.put("sourceRef", "Section K " + title);
        m.put("evidenceBy", evidenceBy);
        return m;
    }

    private static Map<String, Object> gate(String title) {
        return item(title, true, null);
    }

    private Notice notice(LocalDateTime deadline, NoticeGenerationStatus status, OpportunityVisibility visibility,
                          List<Map<String, Object>> items) {
        Opportunity o = Opportunity.builder().solicitationNumber("SN-" + notices.size())
                .organizationName("411TH CSB").responseDeadline(deadline).build();
        String raw;
        try {
            raw = JSON.writeValueAsString(Map.of("eligibility", items));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        Notice n = TestHelper.withId(Notice.builder().opportunity(o).generationStatus(status).visibility(visibility)
                .koreanTitle("공고 " + notices.size()).requiredDocumentsJsonRaw(raw).build());
        notices.add(n);
        return n;
    }

    @SafeVarargs
    private Notice notice(Map<String, Object>... items) {
        return notice(DEADLINE, NoticeGenerationStatus.COMPLETED, OpportunityVisibility.VISIBLE, List.of(items));
    }

    private EligibilityAnswer answered(UUID memberId, Notice n, int itemNo, String title,
                                       EligibilityAnswerValue value, MemberDocument document) {
        EligibilityAnswer a = TestHelper.withId(EligibilityAnswer.builder().memberId(memberId).noticeId(n.getId())
                .itemNo(itemNo).itemTitle(title).answer(value)
                .documentId(document == null ? null : document.getId()).build());
        rows.add(a);
        return a;
    }

    private EligibilityAnswer answered(Notice n, int itemNo, String title, EligibilityAnswerValue value) {
        return answered(me, n, itemNo, title, value, null);
    }

    private EligibilityAnswer have(Notice n, int itemNo, String title, MemberDocument document) {
        return answered(me, n, itemNo, title, EligibilityAnswerValue.HAVE, document);
    }

    private MemberDocument doc(UUID memberId, DocumentCategory category, LocalDate expiresOn) {
        MemberDocument d = TestHelper.withId(MemberDocument.builder().memberId(memberId).category(category)
                .fileName(category.name() + ".pdf").displayName(category.getDisplayName()).expiresOn(expiresOn).build());
        vault.add(d);
        return d;
    }

    private MemberDocument doc() {
        return doc(me, DocumentCategory.SAM_REGISTRATION, null);
    }

    // ---- CR-137 진단 목록과 상세 ----

    @Test
    @DisplayName("TC-QUAL-001 분석이 끝나지 않은 공고문은 진단할 수 없다")
    void 분석안끝난공고_공고별진단_없음() {
        Notice n = notice(DEADLINE, NoticeGenerationStatus.ANALYZING, OpportunityVisibility.VISIBLE,
                List.of(gate("SAM registration")));

        assertThatThrownBy(() -> service.detail(me, n.getId())).isInstanceOf(EligibilityNoticeNotFoundException.class);
    }

    @Test
    @DisplayName("TC-QUAL-002 숨긴 공고문은 진단할 수 없다")
    void 숨김공고_공고별진단_없음() {
        Notice n = notice(DEADLINE, NoticeGenerationStatus.COMPLETED, OpportunityVisibility.HIDDEN,
                List.of(gate("SAM registration")));

        assertThatThrownBy(() -> service.detail(me, n.getId())).isInstanceOf(EligibilityNoticeNotFoundException.class);
    }

    @Test
    @DisplayName("TC-QUAL-003 참가 자격이 없는 공고문은 진단 목록에서 빠진다")
    void 참가자격0개_진단목록_빠짐() {
        Notice kept = notice(gate("SAM registration"));
        notice(DEADLINE, NoticeGenerationStatus.COMPLETED, OpportunityVisibility.VISIBLE, List.of());

        assertThat(service.listNotices(me)).extracting(NoticeRow::noticeId).containsExactly(kept.getId());
    }

    @Test
    @DisplayName("TC-QUAL-004 마감 전은 마감이 가까운 순, 지난 공고는 최근에 마감한 순")
    void 마감전과지난공고_진단목록_마감전가까운순다음지난최근순() {
        LocalDateTime d1020 = LocalDateTime.of(2026, 10, 20, 17, 0);
        LocalDateTime d1015 = LocalDateTime.of(2026, 10, 15, 17, 0);
        LocalDateTime d1001 = LocalDateTime.of(2026, 10, 1, 17, 0);
        LocalDateTime d0901 = LocalDateTime.of(2026, 9, 1, 17, 0);
        for (LocalDateTime d : List.of(d1020, d1015, d1001, d0901)) {
            notice(d, NoticeGenerationStatus.COMPLETED, OpportunityVisibility.VISIBLE, List.of(gate("SAM registration")));
        }

        assertThat(service.listNotices(me)).extracting(NoticeRow::responseDeadline, NoticeRow::open)
                .containsExactly(tuple(d1015, true), tuple(d1020, true), tuple(d1001, false), tuple(d0901, false));
    }

    @Test
    @DisplayName("TC-QUAL-005 요약은 통과 조건 자격만 센다")
    void 통과조건섞임_공고별진단_요약은통과조건만셈() {
        Notice n = notice(gate("SAM registration"), gate("CAGE code"), item("Bonding", false, null));
        answered(n, 0, "SAM registration", EligibilityAnswerValue.HAVE);
        answered(n, 2, "Bonding", EligibilityAnswerValue.LACK);

        assertThat(service.detail(me, n.getId()).summary()).isEqualTo(new Summary(2, 1, 0, 0, 1, 3));
    }

    @Test
    @DisplayName("TC-QUAL-006 다른 회원의 답은 보이지 않는다")
    void 남의답_공고별진단_안보임() {
        Notice n = notice(gate("SAM registration"));
        answered(other, n, 0, "SAM registration", EligibilityAnswerValue.HAVE, null);

        assertThat(service.detail(me, n.getId()).items().get(0).answer()).isNull();
    }

    @Test
    @DisplayName("TC-QUAL-007 증빙 서류 글에 서류함 분류 낱말이 있으면 그 분류와 내 서류를 보인다")
    void 증빙글에서류함낱말_공고별진단_분류와내서류() {
        Notice n = notice(item("Active SAM", true, "SAM.gov registration printout"));
        doc();

        Item i = service.detail(me, n.getId()).items().get(0);

        assertThat(tuple(i.vaultCategory(), i.vaultDocuments().size()))
                .isEqualTo(tuple(DocumentCategory.SAM_REGISTRATION, 1));
    }

    @Test
    @DisplayName("TC-QUAL-008 증빙 서류 글과 자격 이름 모두 맞는 분류가 없으면 분류가 없다")
    void 증빙글맞는분류없음_공고별진단_분류없음() {
        Notice n = notice(item("폐기물 허가증", true, "Waste permit copy"));
        doc(me, DocumentCategory.PAST_PERFORMANCE, null);

        Item i = service.detail(me, n.getId()).items().get(0);

        assertThat(tuple(i.vaultCategory(), i.vaultDocuments())).isEqualTo(tuple(null, List.of()));
    }

    // ---- CR-137 답 저장 ----

    @Test
    @DisplayName("TC-QUAL-009 처음 답하면 행 하나를 자격 이름과 함께 만든다")
    void 처음답함_답저장_행생성과자격이름저장() {
        Notice n = notice(gate("SAM registration"));

        Saved s = service.answer(me, n.getId(), 0, "HAVE");

        assertThat(tuple(rows.size(), rows.get(0).getItemTitle(), s.summary().have()))
                .isEqualTo(tuple(1, "SAM registration", 1));
    }

    @Test
    @DisplayName("TC-QUAL-010 같은 자격에 다시 답하면 같은 행을 고친다")
    void 같은자격다른답_답저장_같은행고침() {
        Notice n = notice(gate("SAM registration"));
        service.answer(me, n.getId(), 0, "HAVE");

        service.answer(me, n.getId(), 0, "LACK");

        assertThat(rows).extracting(EligibilityAnswer::getAnswer).containsExactly(EligibilityAnswerValue.LACK);
    }

    @Test
    @DisplayName("TC-QUAL-011 자격 이름이 바뀌면 옛 답은 답 안 함으로 보고 다시 답하라고 한다")
    void 자격이름바뀜_공고별진단_옛답은답안함() {
        Notice n = notice(gate("Active SAM registration"));
        answered(n, 0, "SAM registration", EligibilityAnswerValue.HAVE);

        Item i = service.detail(me, n.getId()).items().get(0);

        assertThat(tuple(i.answer(), i.stale(), service.listNotices(me).get(0).answeredCount()))
                .isEqualTo(tuple(null, true, 0));
    }

    @Test
    @DisplayName("TC-QUAL-012 옛 답 자격에 다시 답하면 같은 행에 지금 이름과 답을 쓴다")
    void 옛답자격에다시답함_답저장_이름과답새로씀() {
        Notice n = notice(gate("Active SAM registration"));
        answered(n, 0, "SAM registration", EligibilityAnswerValue.HAVE);

        Saved s = service.answer(me, n.getId(), 0, "UNSURE");

        assertThat(tuple(rows.size(), rows.get(0).getItemTitle(), rows.get(0).getAnswer(), s.item().stale()))
                .isEqualTo(tuple(1, "Active SAM registration", EligibilityAnswerValue.UNSURE, false));
    }

    @Test
    @DisplayName("TC-QUAL-013 자격 목록 범위 밖 순번은 거부하고 저장하지 않는다")
    void 범위밖순번_답저장_거부() {
        Notice n = notice(gate("A"), gate("B"), gate("C"));

        Throwable t = catchThrowable(() -> service.answer(me, n.getId(), 3, "HAVE"));

        assertThat(tuple(t.getClass(), rows.size())).isEqualTo(tuple(EligibilityItemInvalidException.class, 0));
    }

    @Test
    @DisplayName("TC-QUAL-014 있음, 없음, 확인 필요가 아닌 답은 거부하고 저장하지 않는다")
    void 셋이아닌답_답저장_거부() {
        Notice n = notice(gate("SAM registration"));

        Throwable t = catchThrowable(() -> service.answer(me, n.getId(), 0, "OK"));

        assertThat(tuple(t.getClass(), rows.size())).isEqualTo(tuple(EligibilityAnswerInvalidException.class, 0));
    }

    // ---- CR-140 판정 ----

    @Test
    @DisplayName("TC-QUAL-016 통과 조건에 없음이 하나라도 있으면 어려움, 걸린 자격은 없음만")
    void 통과조건에없음있음_판정_어려움() {
        Notice n = notice(gate("A"), gate("B"), gate("C"));
        have(n, 0, "A", doc());
        answered(n, 1, "B", EligibilityAnswerValue.LACK);

        Detail d = service.detail(me, n.getId());

        assertThat(tuple(d.verdict(), d.blockers().stream().map(b -> tuple(b.itemNo(), b.problem())).toList()))
                .isEqualTo(tuple(EligibilityVerdict.DIFFICULT, List.of(tuple(1, EligibilityBlockerProblem.LACK))));
    }

    @Test
    @DisplayName("TC-QUAL-017 확인 필요와 답 안 함이 있으면 판정 전, 걸린 자격은 둘 다")
    void 확인필요와답안함_판정_판정전() {
        Notice n = notice(gate("A"), gate("B"), gate("C"));
        have(n, 0, "A", doc());
        answered(n, 1, "B", EligibilityAnswerValue.UNSURE);

        Detail d = service.detail(me, n.getId());

        assertThat(tuple(d.verdict(), d.blockers().stream().map(b -> tuple(b.itemNo(), b.problem())).toList()))
                .isEqualTo(tuple(EligibilityVerdict.UNDECIDED, List.of(
                        tuple(1, EligibilityBlockerProblem.UNSURE), tuple(2, EligibilityBlockerProblem.UNANSWERED))));
    }

    @Test
    @DisplayName("TC-QUAL-018 옛 이름의 있음 답은 서류가 붙어 있어도 판정 전")
    void 옛답_판정_판정전() {
        Notice n = notice(gate("Active SAM registration"));
        have(n, 0, "SAM registration", doc());

        Detail d = service.detail(me, n.getId());

        assertThat(tuple(d.verdict(), d.blockers().stream().map(Blocker::problem).toList()))
                .isEqualTo(tuple(EligibilityVerdict.UNDECIDED, List.of(EligibilityBlockerProblem.STALE)));
    }

    @Test
    @DisplayName("TC-QUAL-019 모두 있음인데 서류를 붙이지 않은 자격이 있으면 서류 부족")
    void 모두있음서류하나안붙임_판정_서류부족() {
        Notice n = notice(gate("A"), gate("B"));
        have(n, 0, "A", doc());
        have(n, 1, "B", null);

        Detail d = service.detail(me, n.getId());

        assertThat(tuple(d.verdict(), d.blockers().stream().map(b -> tuple(b.itemNo(), b.problem())).toList()))
                .isEqualTo(tuple(EligibilityVerdict.DOCS_MISSING, List.of(tuple(1, EligibilityBlockerProblem.NO_DOCUMENT))));
    }

    @Test
    @DisplayName("TC-QUAL-020 붙인 서류의 만료일이 어제면 서류 부족, 걸린 자격에 서류 이름과 만료일")
    void 붙인서류만료_판정_서류부족() {
        Notice n = notice(gate("A"));
        LocalDate expired = LocalDate.of(2026, 10, 10);
        MemberDocument d0 = doc(me, DocumentCategory.CAGE, expired);
        have(n, 0, "A", d0);

        Detail d = service.detail(me, n.getId());

        assertThat(tuple(d.verdict(), d.blockers(), d.items().get(0).attachedDocument().expired()))
                .isEqualTo(tuple(EligibilityVerdict.DOCS_MISSING, List.of(new Blocker(0, "A", "Section K A",
                        EligibilityBlockerProblem.EXPIRED, d0.getDisplayName(), expired)), true));
    }

    @Test
    @DisplayName("TC-QUAL-021 만료일 당일과 만료일 없는 서류는 쓸 수 있어 참여 가능")
    void 만료일당일과만료일없음_판정_참여가능() {
        Notice n = notice(gate("A"), gate("B"));
        have(n, 0, "A", doc(me, DocumentCategory.CAGE, LocalDate.of(2026, 10, 11)));
        have(n, 1, "B", doc(me, DocumentCategory.SAM_REGISTRATION, null));

        Detail d = service.detail(me, n.getId());

        assertThat(tuple(d.verdict(), d.blockers())).isEqualTo(tuple(EligibilityVerdict.ELIGIBLE, List.of()));
    }

    @Test
    @DisplayName("TC-QUAL-022 통과 조건이 아닌 자격의 없음은 판정에 들지 않는다")
    void 통과조건아닌자격없음_판정_영향없음() {
        Notice n = notice(gate("A"), item("Bonding", false, null));
        have(n, 0, "A", doc());
        answered(n, 1, "Bonding", EligibilityAnswerValue.LACK);

        assertThat(service.detail(me, n.getId()).verdict()).isEqualTo(EligibilityVerdict.ELIGIBLE);
    }

    @Test
    @DisplayName("TC-QUAL-023 통과 조건이 하나도 없으면 판정 전, 걸린 자격 없음")
    void 통과조건0개_판정_판정전() {
        Notice n = notice(item("A", false, null), item("B", false, null));
        have(n, 0, "A", doc());
        have(n, 1, "B", doc());

        assertThat(service.listNotices(me)).extracting(NoticeRow::verdict, NoticeRow::blockers, NoticeRow::gateCount)
                .containsExactly(tuple(EligibilityVerdict.UNDECIDED, List.of(), 0));
    }

    @Test
    @DisplayName("TC-QUAL-024 붙인 서류가 서류함에 없으면 서류 없음으로 본다")
    void 붙인서류가서류함에없음_판정_서류부족() {
        Notice n = notice(gate("A"));
        EligibilityAnswer a = have(n, 0, "A", null);
        a.attach(UUID.randomUUID());

        Detail d = service.detail(me, n.getId());

        assertThat(tuple(d.verdict(), d.blockers().stream().map(Blocker::problem).toList(),
                d.items().get(0).attachedDocument()))
                .isEqualTo(tuple(EligibilityVerdict.DOCS_MISSING, List.of(EligibilityBlockerProblem.NO_DOCUMENT), null));
    }

    @Test
    @DisplayName("TC-QUAL-025 진단 목록 줄마다 판정과 통과 조건 수를 싣는다")
    void 진단목록_줄마다판정() {
        Notice lack = notice(LocalDateTime.of(2026, 10, 15, 17, 0), NoticeGenerationStatus.COMPLETED,
                OpportunityVisibility.VISIBLE, List.of(gate("A")));
        notice(LocalDateTime.of(2026, 10, 20, 17, 0), NoticeGenerationStatus.COMPLETED,
                OpportunityVisibility.VISIBLE, List.of(gate("A"), gate("B")));
        answered(lack, 0, "A", EligibilityAnswerValue.LACK);

        assertThat(service.listNotices(me)).extracting(NoticeRow::verdict, NoticeRow::gateCount)
                .containsExactly(tuple(EligibilityVerdict.DIFFICULT, 1), tuple(EligibilityVerdict.UNDECIDED, 2));
    }

    @Test
    @DisplayName("TC-QUAL-026 증빙 서류 글이 맞지 않으면 자격 이름으로 서류함 분류를 맞춘다")
    void 증빙글맞춤없고자격이름맞음_공고별진단_이름으로분류() {
        Notice n = notice(item("사업자등록증", true, "CLIENT_UPLOAD"));

        assertThat(service.detail(me, n.getId()).items().get(0).vaultCategory())
                .isEqualTo(DocumentCategory.BUSINESS_REGISTRATION);
    }

    // ---- CR-140 증빙 서류 붙이기 ----

    @Test
    @DisplayName("TC-QUAL-027 있음 답에 내 서류를 붙이면 저장하고 판정이 참여 가능으로 바뀐다")
    void 있음답에내서류_서류붙이기_저장과판정바뀜() {
        Notice n = notice(gate("A"));
        EligibilityAnswer a = have(n, 0, "A", null);
        MemberDocument d0 = doc();

        Saved s = service.attach(me, n.getId(), 0, d0.getId());

        assertThat(tuple(a.getDocumentId(), s.verdict())).isEqualTo(tuple(d0.getId(), EligibilityVerdict.ELIGIBLE));
    }

    @Test
    @DisplayName("TC-QUAL-028 다른 회원의 서류는 붙이지 않는다")
    void 남의서류_서류붙이기_거부() {
        Notice n = notice(gate("A"));
        MemberDocument mine = doc();
        EligibilityAnswer a = have(n, 0, "A", mine);
        MemberDocument theirs = doc(other, DocumentCategory.SAM_REGISTRATION, null);

        Throwable t = catchThrowable(() -> service.attach(me, n.getId(), 0, theirs.getId()));

        assertThat(tuple(t.getClass(), a.getDocumentId()))
                .isEqualTo(tuple(EligibilityDocumentNotFoundException.class, mine.getId()));
    }

    @Test
    @DisplayName("TC-QUAL-029 없음, 답 없음, 옛 답에는 서류를 붙이지 않는다")
    void 있음이아닌답_서류붙이기_거부() {
        Notice n = notice(gate("A"), gate("B"), gate("Active C"));
        answered(n, 0, "A", EligibilityAnswerValue.LACK);
        have(n, 2, "C", null);
        MemberDocument d0 = doc();

        assertThat(IntStream.of(0, 1, 2).mapToObj(i -> catchThrowable(() -> service.attach(me, n.getId(), i, d0.getId()))))
                .allMatch(EligibilityDocumentNotAllowedException.class::isInstance);
    }

    @Test
    @DisplayName("TC-QUAL-030 서류를 비우면 떼고 판정이 서류 부족으로 바뀐다")
    void 비움_서류붙이기_뗌() {
        Notice n = notice(gate("A"));
        EligibilityAnswer a = have(n, 0, "A", doc());

        Saved s = service.attach(me, n.getId(), 0, null);

        assertThat(tuple(a.getDocumentId(), s.verdict())).isEqualTo(tuple(null, EligibilityVerdict.DOCS_MISSING));
    }

    @Test
    @DisplayName("TC-QUAL-031 서류 붙은 있음을 없음으로 바꾸면 붙인 서류가 풀린다")
    void 있음에서없음_답저장_붙인서류풀림() {
        Notice n = notice(gate("A"));
        EligibilityAnswer a = have(n, 0, "A", doc());

        service.answer(me, n.getId(), 0, "LACK");

        assertThat(a.getDocumentId()).isNull();
    }

    @Test
    @DisplayName("TC-QUAL-032 서류 붙은 있음에 다시 있음으로 답하면 붙인 서류가 남는다")
    void 있음에서있음_답저장_붙인서류남음() {
        Notice n = notice(gate("A"));
        MemberDocument d0 = doc();
        EligibilityAnswer a = have(n, 0, "A", d0);

        service.answer(me, n.getId(), 0, "HAVE");

        assertThat(a.getDocumentId()).isEqualTo(d0.getId());
    }

    @Test
    @DisplayName("TC-QUAL-033 서류 붙은 옛 답에 지금 이름으로 있음이라 답하면 붙인 서류가 풀린다")
    void 옛답에다시답함_답저장_붙인서류풀림() {
        Notice n = notice(gate("Active SAM registration"));
        EligibilityAnswer a = have(n, 0, "SAM registration", doc());

        service.answer(me, n.getId(), 0, "HAVE");

        assertThat(a.getDocumentId()).isNull();
    }

    @Test
    @DisplayName("TC-QUAL-034 제안 서류마다 만료 여부를 싣는다. 만료일이 어제면 만료, 당일이면 아니다")
    void 맞은분류서류_공고별진단_만료여부() {
        Notice n = notice(item("Active SAM", true, "SAM.gov registration printout"));
        doc(me, DocumentCategory.SAM_REGISTRATION, LocalDate.of(2026, 10, 10));
        doc(me, DocumentCategory.SAM_REGISTRATION, LocalDate.of(2026, 10, 11));

        Item i = service.detail(me, n.getId()).items().get(0);

        assertThat(i.vaultDocuments()).extracting(v -> tuple(v.expiresOn(), v.expired()))
                .containsExactlyInAnyOrder(tuple(LocalDate.of(2026, 10, 10), true), tuple(LocalDate.of(2026, 10, 11), false));
    }
}
