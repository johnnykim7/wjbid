package com.biddingagency.domain.analysis.service;

import com.biddingagency.domain.analysis.service.BidAnalysisBodyValidator.Part;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.biddingagency.domain.analysis.service.BidAnalysisFixtures.capturePlan;
import static com.biddingagency.domain.analysis.service.BidAnalysisFixtures.decision;
import static com.biddingagency.domain.analysis.service.BidAnalysisFixtures.report;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** TC-DOC-258, 265: save_bid_analysis 본문 꼴 검사 (CR-170, BIZ-091, BIZ-093) */
class BidAnalysisBodyValidatorTest {

    @Test
    @DisplayName("견본 본문 세 단계는 통과")
    void 견본_통과() {
        assertThatCode(() -> {
            BidAnalysisBodyValidator.validate(Part.REPORT, report());
            BidAnalysisBodyValidator.validate(Part.CAPTURE_PLAN, capturePlan());
            BidAnalysisBodyValidator.validate(Part.DECISION, decision(4));
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("TC-DOC-258 저장_근거값확신도없음_틀린곳응답")
    @SuppressWarnings("unchecked")
    void 근거값확신도없음_틀린곳응답() {
        Map<String, Object> body = report();
        Map<String, Object> first = (Map<String, Object>) ((List<Object>) body.get("eligibility")).get(0);
        ((Map<String, Object>) first.get("evidence")).remove("confidence");

        assertThatThrownBy(() -> BidAnalysisBodyValidator.validate(Part.REPORT, body))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("eligibility[0].evidence.confidence:");
    }

    @Test
    @DisplayName("TC-DOC-265 결정저장_자사없음_거부")
    @SuppressWarnings("unchecked")
    void 자사없음_거부() {
        Map<String, Object> body = decision(4);
        ((List<Object>) body.get("scorecard")).remove(0);

        assertThatThrownBy(() -> BidAnalysisBodyValidator.validate(Part.DECISION, body))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("scorecard:");
    }
}
