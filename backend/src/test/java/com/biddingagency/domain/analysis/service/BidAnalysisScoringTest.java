package com.biddingagency.domain.analysis.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.biddingagency.domain.analysis.entity.BidVerdict;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.biddingagency.domain.analysis.service.BidAnalysisFixtures.capturePlan;
import static com.biddingagency.domain.analysis.service.BidAnalysisFixtures.decision;
import static com.biddingagency.domain.analysis.service.BidAnalysisFixtures.l;
import static com.biddingagency.domain.analysis.service.BidAnalysisFixtures.m;
import static com.biddingagency.domain.analysis.service.BidAnalysisFixtures.report;
import static org.assertj.core.api.Assertions.assertThat;

/** TC-DOC-260~264, 266: Bid/No-Bid 결정 저장 때 서버 계산 (CR-170, BIZ-091, POL-041) */
class BidAnalysisScoringTest {

    private static final PwinPolicy DEFAULTS = PwinPolicy.defaults();

    @Test
    @DisplayName("TC-DOC-260 결정저장_가중점수계산")
    void 가중점수계산() {
        BidAnalysisScoring.Result r = BidAnalysisScoring.compute(decision(4), report(), capturePlan(), DEFAULTS);

        assertThat(r.pwinPercent()).isEqualByComparingTo("80.0");
    }

    @Test
    @DisplayName("TC-DOC-261 결정저장_문턱별판정")
    void 문턱별판정() {
        List<BidVerdict> verdicts = Stream.of("70.0", "69.9", "50.0", "30.0", "29.9")
                .map(p -> BidAnalysisScoring.verdictOf(new BigDecimal(p), DEFAULTS.thresholds()))
                .toList();

        assertThat(verdicts).containsExactly(BidVerdict.STRONG_GO, BidVerdict.CONDITIONAL_GO,
                BidVerdict.CONDITIONAL_GO, BidVerdict.WEAK, BidVerdict.NO_GO);
    }

    @Test
    @DisplayName("TC-DOC-262 결정저장_해결할수없는자격_NoGo")
    void 해결할수없는자격_NoGo() {
        Map<String, Object> decision = decision(4);
        decision.put("qualification", l(m("item", "시설 보안 허가", "resolvable", false)));

        BidAnalysisScoring.Result r = BidAnalysisScoring.compute(decision, report(), capturePlan(), DEFAULTS);

        assertThat(r.verdict()).isEqualTo(BidVerdict.NO_GO);
        assertThat(r.verdictReason()).contains("시설 보안 허가");
    }

    @Test
    @DisplayName("TC-DOC-263 결정저장_강점약점")
    void 강점약점() {
        BidAnalysisScoring.Result r = BidAnalysisScoring.compute(decision(4), report(), capturePlan(), DEFAULTS);

        assertThat(r.strengths()).extracting(s -> s.get("theme")).containsExactly("품질");
        assertThat(r.weaknesses()).extracting(s -> s.get("theme")).containsExactly("경험");
    }

    @Test
    @DisplayName("TC-DOC-264 결정저장_핵심이슈순위")
    void 핵심이슈순위() {
        BidAnalysisScoring.Result r = BidAnalysisScoring.compute(decision(4), report(), capturePlan(), DEFAULTS);

        assertThat(r.issueRanking()).extracting(i -> i.get("issue"), i -> i.get("score"))
                .containsExactly(org.assertj.core.groups.Tuple.tuple("A", 24), org.assertj.core.groups.Tuple.tuple("B", 9));
    }

    @Test
    @DisplayName("TC-DOC-266 정책값합틀림_초기값으로계산")
    void 정책값합틀림_초기값() {
        Logger logger = (Logger) LoggerFactory.getLogger(PwinPolicy.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            PwinPolicy policy = PwinPolicy.of(
                    "CUSTOMER_RELATION:15,SOLUTION_FIT:10,COMPETITION:15,PRICE:15,PAST_PERFORMANCE:15,PERSONNEL:10,ENVIRONMENT:10",
                    "70,50,30");

            assertThat(policy.weights()).isEqualTo(DEFAULTS.weights());
            assertThat(appender.list).filteredOn(e -> e.getFormattedMessage().contains("BID_PWIN_WEIGHTS")).hasSize(1);
        } finally {
            logger.detachAppender(appender);
        }
    }
}
