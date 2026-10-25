package com.biddingagency.domain.analysis.service;

import com.biddingagency.TestHelper;
import com.biddingagency.domain.analysis.BidAnalysisException;
import com.biddingagency.domain.analysis.BidAnalysisException.Reason;
import com.biddingagency.domain.analysis.dto.BidAnalysisDto.Overview;
import com.biddingagency.domain.analysis.entity.BidAnalysis;
import com.biddingagency.domain.analysis.entity.BidAnalysisStatus;
import com.biddingagency.domain.analysis.entity.BidDecision;
import com.biddingagency.domain.analysis.entity.BidVerdict;
import com.biddingagency.domain.analysis.repository.BidAnalysisRepository;
import com.biddingagency.domain.analysis.service.BidAnalysisBodyValidator.Part;
import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.entity.BidRequestState;
import com.biddingagency.domain.bid.entity.StateTransition;
import com.biddingagency.domain.bid.repository.BidRequestRepository;
import com.biddingagency.domain.bid.service.AIWorkflowService;
import com.biddingagency.domain.member.repository.MemberRepository;
import com.biddingagency.domain.reading.service.DocumentReadingService;
import com.biddingagency.domain.setting.entity.SettingKey;
import com.biddingagency.domain.setting.service.SystemSettingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.biddingagency.domain.analysis.service.BidAnalysisFixtures.capturePlan;
import static com.biddingagency.domain.analysis.service.BidAnalysisFixtures.decision;
import static com.biddingagency.domain.analysis.service.BidAnalysisFixtures.question;
import static com.biddingagency.domain.analysis.service.BidAnalysisFixtures.report;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

/** TC-DOC-245, 246, 248~257, 259, 267, 273~281, 287: 입찰 분석 회차와 Bid/No-Bid 결정 (CR-170) */
@ExtendWith(MockitoExtension.class)
class BidAnalysisServiceTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    /** 폴링 5000ms x 360회 = 30분 */
    private static final Duration LIMIT = Duration.ofMinutes(30);

    @Mock private BidAnalysisRepository analysisRepository;
    @Mock private BidRequestRepository bidRequestRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private DocumentReadingService documentReadingService;
    @Mock private AIWorkflowService aiWorkflowService;
    @Mock private SystemSettingService settingService;

    private final MovableClock clock = new MovableClock(Instant.parse("2026-10-25T03:00:00Z"));
    private final Map<UUID, BidAnalysis> store = new LinkedHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UUID adminId = UUID.randomUUID();

    private BidAnalysisService service;
    private BidRequest bid;

    @BeforeEach
    void setUp() {
        service = new BidAnalysisService(analysisRepository, bidRequestRepository, memberRepository,
                documentReadingService, aiWorkflowService, settingService, objectMapper, clock, 5000, 360);
        bid = bidIn(BidRequestState.ANALYZING);

        lenient().when(analysisRepository.save(any(BidAnalysis.class))).thenAnswer(inv -> {
            BidAnalysis a = inv.getArgument(0);
            if (a.getId() == null) TestHelper.withId(a);
            store.put(a.getId(), a);
            return a;
        });
        lenient().when(analysisRepository.findById(any()))
                .thenAnswer(inv -> Optional.ofNullable(store.get(inv.<UUID>getArgument(0))));
        lenient().when(analysisRepository.existsByBidRequestId(any()))
                .thenAnswer(inv -> store.values().stream().anyMatch(a -> a.getBidRequestId().equals(inv.getArgument(0))));
        lenient().when(analysisRepository.findFirstByBidRequestIdOrderByStartedAtDesc(any()))
                .thenAnswer(inv -> store.values().stream()
                        .filter(a -> a.getBidRequestId().equals(inv.getArgument(0)))
                        .max(Comparator.comparing(BidAnalysis::getStartedAt)));
        lenient().when(settingService.findValue(any())).thenReturn(Optional.empty());
    }

    // ─── 준비 ───────────────────────────────────────────────────────────

    private BidRequest bidIn(BidRequestState state) {
        BidRequest b = TestHelper.withId(BidRequest.builder().state(state).stateHistory(new ArrayList<>()).build());
        lenient().when(bidRequestRepository.findByIdForUpdate(b.getId())).thenReturn(Optional.of(b));
        lenient().when(bidRequestRepository.findById(b.getId())).thenReturn(Optional.of(b));
        return b;
    }

    private BidAnalysis stored(BidAnalysis a) {
        TestHelper.withId(a);
        store.put(a.getId(), a);
        return a;
    }

    private BidAnalysis running(LocalDateTime startedAt) {
        return stored(BidAnalysis.running(bid.getId(), adminId, startedAt));
    }

    private BidAnalysis done(LocalDateTime startedAt, BidDecision decision) {
        BidAnalysis a = running(startedAt);
        a.markDone("{}", null, BidVerdict.STRONG_GO, "견본", "[]", "[]", "[]", "[]", startedAt.plusMinutes(10));
        if (decision != null) a.decide(decision, "조건", adminId, startedAt.plusMinutes(20));
        return a;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private BidAnalysis current() {
        return store.values().stream().max(Comparator.comparing(BidAnalysis::getStartedAt)).orElseThrow();
    }

    // ─── 시작 (BIZ-090) ──────────────────────────────────────────────────

    @Test
    @DisplayName("TC-DOC-245 분석진입_읽을서류없음_입찰분석바로시작")
    void 읽을서류없음_바로시작() {
        bid.getStateHistory().add(StateTransition.builder()
                .fromState(BidRequestState.DOCS_RECEIVED).toState(BidRequestState.ANALYZING).userId(adminId).build());

        boolean started = service.startIfReady(bid.getId());

        assertThat(started).isTrue();
        assertThat(store.values()).singleElement()
                .extracting(BidAnalysis::getStatus, BidAnalysis::getStartedBy)
                .containsExactly(BidAnalysisStatus.RUNNING, adminId);
        then(aiWorkflowService).should().runBidAnalysisAsync(bid.getId(), current().getId());
    }

    @Test
    @DisplayName("TC-DOC-246 분석진입_읽는중있음_입찰분석미시작")
    void 읽는중있음_미시작() {
        given(documentReadingService.hasReadingInProgress(bid.getId())).willReturn(true);

        assertThat(service.startIfReady(bid.getId())).isFalse();
        assertThat(store).isEmpty();
        then(aiWorkflowService).should(never()).runBidAnalysisAsync(any(), any());
    }

    @Test
    @DisplayName("TC-DOC-248 읽기회차끝_다른건이같은해시읽음_그건도시작")
    void 회차없는분석단계건_모두대상() {
        BidRequest other = bidIn(BidRequestState.ANALYZING);
        BidRequest analysed = bidIn(BidRequestState.ANALYZING);
        stored(BidAnalysis.running(analysed.getId(), adminId, now()));
        given(bidRequestRepository.findByStateIn(List.of(BidRequestState.ANALYZING)))
                .willReturn(List.of(bid, other, analysed));

        assertThat(service.pendingBidRequestIds()).containsExactly(bid.getId(), other.getId());
    }

    @Test
    @DisplayName("TC-DOC-249 읽기회차끝_이미회차있음_다시시작안함")
    void 이미회차있음_다시시작안함() {
        done(now().minusHours(1), null);

        assertThat(service.startIfReady(bid.getId())).isFalse();
        assertThat(store).hasSize(1);
    }

    // ─── 다시 분석 ───────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-DOC-250 다시분석_분석단계아님_409")
    void 다시분석_분석단계아님() {
        BidRequest generating = bidIn(BidRequestState.GENERATING);

        assertThatThrownBy(() -> service.rerun(generating.getId(), adminId))
                .isInstanceOfSatisfying(BidAnalysisException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getCode()).isEqualTo("BID_ANALYSIS_NOT_ALLOWED");
                    assertThat(e.getReason()).isEqualTo(Reason.NOT_ANALYZING);
                });
    }

    @Test
    @DisplayName("TC-DOC-251 다시분석_분석중_409")
    void 다시분석_분석중() {
        running(now().minusMinutes(5));

        assertThatThrownBy(() -> service.rerun(bid.getId(), adminId))
                .isInstanceOfSatisfying(BidAnalysisException.class,
                        e -> assertThat(e.getReason()).isEqualTo(Reason.ANALYSIS_RUNNING));
    }

    @Test
    @DisplayName("TC-DOC-252 다시분석_서류읽는중_409")
    void 다시분석_서류읽는중() {
        given(documentReadingService.hasReadingInProgress(bid.getId())).willReturn(true);

        assertThatThrownBy(() -> service.rerun(bid.getId(), adminId))
                .isInstanceOfSatisfying(BidAnalysisException.class,
                        e -> assertThat(e.getReason()).isEqualTo(Reason.READING_IN_PROGRESS));
    }

    @Test
    @DisplayName("TC-DOC-253 다시분석_신규회차_앞결정안이어짐")
    void 다시분석_앞결정안이어짐() {
        BidAnalysis previous = done(now().minusHours(1), BidDecision.GO);

        Overview overview = service.rerun(bid.getId(), adminId);

        assertThat(overview.analysis().analysisId()).isNotEqualTo(previous.getId());
        assertThat(overview.analysis().status()).isEqualTo(BidAnalysisStatus.RUNNING);
        assertThat(overview.analysis().adminDecision()).isNull();
    }

    @Test
    @DisplayName("TC-DOC-254 분석중_대기한도넘음_분석실패")
    void 대기한도넘음_분석실패() {
        BidAnalysis stale = running(now().minus(LIMIT).minusSeconds(1));

        Overview overview = service.overview(bid.getId());

        assertThat(overview.analysis().status()).isEqualTo(BidAnalysisStatus.FAILED);
        assertThat(stale.getFailureReason()).isEqualTo("분석 시간 초과");
    }

    @Test
    @DisplayName("TC-DOC-255 워크플로우끝_결정없음_분석실패")
    void 워크플로우끝_결정없음_분석실패() {
        BidAnalysis a = running(now());
        service.save(bid.getId(), a.getId(), Part.REPORT, report(), List.of());
        service.save(bid.getId(), a.getId(), Part.CAPTURE_PLAN, capturePlan(), List.of());

        service.closeUnfinished(a.getId());

        assertThat(a.getStatus()).isEqualTo(BidAnalysisStatus.FAILED);
        assertThat(a.getFailureReason()).isEqualTo("Bid/No-Bid 결정이 저장되지 않음");
        assertThat(a.getReportJson()).isNotNull();
        assertThat(a.getCapturePlanJson()).isNotNull();
    }

    // ─── 저장 ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-DOC-256 저장_분석중아님_거부")
    void 저장_분석중아님_거부() {
        BidAnalysis a = done(now().minusHours(1), null);

        assertThatThrownBy(() -> service.save(bid.getId(), a.getId(), Part.REPORT, report(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(a.getReportJson()).isNull();
    }

    @Test
    @DisplayName("TC-DOC-257 저장_앞단계없음_거부")
    void 저장_앞단계없음_거부() {
        BidAnalysis a = running(now());

        assertThatThrownBy(() -> service.save(bid.getId(), a.getId(), Part.CAPTURE_PLAN, capturePlan(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("REPORT");
        assertThat(a.getCapturePlanJson()).isNull();
    }

    @Test
    @DisplayName("TC-DOC-259 저장_확인필요_앞단계뒤에이어붙임")
    void 확인필요_이어붙임() {
        BidAnalysis a = running(now());
        service.save(bid.getId(), a.getId(), Part.REPORT, report(),
                List.of(question("CUSTOMER", "첫째"), question("ADMIN", "둘째")));

        Map<String, Object> out = service.save(bid.getId(), a.getId(), Part.CAPTURE_PLAN, capturePlan(),
                List.of(question("CUSTOMER", "셋째")));

        assertThat(out.get("questions")).isEqualTo(3);
        assertThat(service.overview(bid.getId()).analysis().questions())
                .extracting(q -> q.get("question")).containsExactly("첫째", "둘째", "셋째");
    }

    @Test
    @DisplayName("TC-DOC-267 결정저장_분석완료전환")
    void 결정저장_분석완료() {
        BidAnalysis a = running(now());
        service.save(bid.getId(), a.getId(), Part.REPORT, report(), List.of());
        service.save(bid.getId(), a.getId(), Part.CAPTURE_PLAN, capturePlan(), List.of());

        service.save(bid.getId(), a.getId(), Part.DECISION, decision(4), List.of());

        assertThat(a.getStatus()).isEqualTo(BidAnalysisStatus.DONE);
        assertThat(a.getFinishedAt()).isEqualTo(now());
    }

    @Test
    @DisplayName("TC-DOC-287 정책값변경_다음계산부터_앞회차그대로")
    void 정책값변경_앞회차그대로() {
        Map<String, Object> ours = BidAnalysisFixtures.factors(3);
        ours.put("CUSTOMER_RELATION", BidAnalysisFixtures.m("score", 5, "basis", "근거"));
        BidAnalysis first = analysedWith(ours);
        given(settingService.findValue(SettingKey.BID_PWIN_WEIGHTS)).willReturn(Optional.of(
                "CUSTOMER_RELATION:40,SOLUTION_FIT:10,COMPETITION:10,PRICE:10,PAST_PERFORMANCE:10,PERSONNEL:10,ENVIRONMENT:10"));
        clock.advance(Duration.ofHours(1));

        BidAnalysis second = analysedWith(ours);

        assertThat(first.getPwinPercent()).isEqualByComparingTo("66.0");
        assertThat(second.getPwinPercent()).isEqualByComparingTo("76.0");
    }

    private BidAnalysis analysedWith(Map<String, Object> ourFactors) {
        BidAnalysis a = running(now());
        service.save(bid.getId(), a.getId(), Part.REPORT, report(), List.of());
        service.save(bid.getId(), a.getId(), Part.CAPTURE_PLAN, capturePlan(), List.of());
        service.save(bid.getId(), a.getId(), Part.DECISION, decision(ourFactors), List.of());
        return a;
    }

    // ─── 관리자 결정 (BIZ-092) ───────────────────────────────────────────

    @Test
    @DisplayName("TC-DOC-273 결정_조건부Go조건없음_400")
    void 조건부Go_조건없음() {
        done(now().minusHours(1), null);

        assertThatThrownBy(() -> service.decide(bid.getId(), BidDecision.CONDITIONAL_GO, " ", adminId))
                .isInstanceOfSatisfying(BidAnalysisException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getCode()).isEqualTo("DECISION_NOTE_REQUIRED");
                });
    }

    @Test
    @DisplayName("TC-DOC-274 결정_NoGo사유없음_400")
    void NoGo_사유없음() {
        done(now().minusHours(1), null);

        assertThatThrownBy(() -> service.decide(bid.getId(), BidDecision.NO_GO, null, adminId))
                .isInstanceOfSatisfying(BidAnalysisException.class,
                        e -> assertThat(e.getCode()).isEqualTo("DECISION_NOTE_REQUIRED"));
    }

    @Test
    @DisplayName("TC-DOC-275 결정_회차없음_결정만회차생성")
    void 회차없음_결정만회차() {
        service.decide(bid.getId(), BidDecision.GO, null, adminId);

        assertThat(store.values()).singleElement()
                .extracting(BidAnalysis::getStatus, BidAnalysis::getAdminDecision)
                .containsExactly(BidAnalysisStatus.DECISION_ONLY, BidDecision.GO);
    }

    @Test
    @DisplayName("TC-DOC-276 결정_분석중_409")
    void 결정_분석중() {
        running(now().minusMinutes(5));

        assertThatThrownBy(() -> service.decide(bid.getId(), BidDecision.GO, null, adminId))
                .isInstanceOfSatisfying(BidAnalysisException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getReason()).isEqualTo(Reason.ANALYSIS_RUNNING);
                });
    }

    @Test
    @DisplayName("TC-DOC-277 결정_다시정함_덮어씀")
    void 결정_다시정함_덮어씀() {
        BidAnalysis a = done(now().minusHours(1), null);
        service.decide(bid.getId(), BidDecision.GO, null, adminId);
        UUID otherAdmin = UUID.randomUUID();
        clock.advance(Duration.ofMinutes(30));

        service.decide(bid.getId(), BidDecision.NO_GO, "경쟁사 우위", otherAdmin);

        assertThat(a).extracting(BidAnalysis::getAdminDecision, BidAnalysis::getAdminNote,
                        BidAnalysis::getDecidedBy, BidAnalysis::getDecidedAt)
                .containsExactly(BidDecision.NO_GO, "경쟁사 우위", otherAdmin, now());
    }

    @Test
    @DisplayName("TC-DOC-278 생성전환_결정없음_409")
    void 생성전환_결정없음() {
        done(now().minusHours(1), null);

        assertThatThrownBy(() -> service.requireGoDecision(bid.getId()))
                .isInstanceOfSatisfying(BidAnalysisException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getCode()).isEqualTo("BID_DECISION_REQUIRED");
                });
    }

    @Test
    @DisplayName("TC-DOC-279 생성전환_NoGo_409")
    void 생성전환_NoGo() {
        done(now().minusHours(1), BidDecision.NO_GO);

        assertThatThrownBy(() -> service.requireGoDecision(bid.getId()))
                .isInstanceOfSatisfying(BidAnalysisException.class,
                        e -> assertThat(e.getCode()).isEqualTo("BID_DECISION_REQUIRED"));
    }

    @Test
    @DisplayName("TC-DOC-280 생성전환_조건부Go_허용")
    void 생성전환_조건부Go_허용() {
        done(now().minusHours(1), BidDecision.CONDITIONAL_GO);

        assertThatCode(() -> service.requireGoDecision(bid.getId())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("TC-DOC-281 생성전환_앞회차Go지금회차결정없음_409")
    void 앞회차Go_지금회차결정없음() {
        done(now().minusHours(2), BidDecision.GO);
        done(now().minusHours(1), null);

        assertThatThrownBy(() -> service.requireGoDecision(bid.getId()))
                .isInstanceOfSatisfying(BidAnalysisException.class,
                        e -> assertThat(e.getCode()).isEqualTo("BID_DECISION_REQUIRED"));
    }

    /** 결정을 다시 정한 시각을 앞 결정과 가르려고 쓴다 */
    private static final class MovableClock extends Clock {
        private Instant instant;

        MovableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration d) {
            instant = instant.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return SEOUL;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
