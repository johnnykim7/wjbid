package com.biddingagency.domain.analysis.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** save_bid_analysis 본문 견본. 시험마다 고칠 수 있게 바꿀 수 있는 Map 과 List 로 만든다 (CR-170) */
final class BidAnalysisFixtures {

    static final String COMPETITOR = "Alpha Corp";

    private BidAnalysisFixtures() {
    }

    static Map<String, Object> m(Object... kv) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            out.put((String) kv[i], kv[i + 1]);
        }
        return out;
    }

    static List<Object> l(Object... items) {
        return new ArrayList<>(List.of(items));
    }

    static Map<String, Object> evidence(Object value) {
        return m("value", value, "confidence", "CONFIRMED", "source", "공고 본문");
    }

    /** BCC 테마 셋. 자사 5, 3, 2, 경쟁사 3, 3, 4 */
    static Map<String, Object> report() {
        List<Object> indicators = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            indicators.add(m("no", i, "value", "NO"));
        }
        return m(
                "overview", m(
                        "projectName", evidence("Camp Humphreys 시설 유지보수"),
                        "agency", evidence("411th CSB"),
                        "budget", evidence("USD 1,200,000"),
                        "period", evidence("12개월"),
                        "contractType", evidence("FFP")),
                "agency", m("name", "411th CSB"),
                "orderHistory", m(
                        "rows", l(m("source", "FEDERAL_CONTRACT", "number", "W91QVN-21-C-0001")),
                        "verdict", "RECURRING",
                        "incumbent", evidence("Beta Inc"),
                        "incumbentAdvantage", evidence("중간")),
                "requirements", m("themes", l("품질"), "evaluation", l(), "questionCandidates", l()),
                "eligibility", l(m("item", "SAM 등록", "status", "MET", "evidence", evidence("등록됨"))),
                "competitors", m(
                        "companies", l(m("name", COMPETITOR, "likelihood", "HIGH")),
                        "bcc", m(
                                "themes", l("품질", "가격", "경험"),
                                "scores", m(
                                        BidAnalysisScoring.OUR_COMPANY, m("품질", 5, "가격", 3, "경험", 2),
                                        COMPETITOR, m("품질", 3, "가격", 3, "경험", 4)))),
                "fairness", m("indicators", indicators, "judgement", "GOOD"),
                "hiddenIssues", l(),
                "insights", l());
    }

    /** 묶은 이슈 A 에 관련자 이슈 (5x4), (2x2), B 에 (3x3) */
    static Map<String, Object> capturePlan() {
        return m(
                "business", m("summary", "시설 유지보수"),
                "issues", m(
                        "stakeholderIssues", l(
                                m("id", "S1", "stakeholder", "계약관", "issue", "일정", "influence", 5, "importance", 4),
                                m("id", "S2", "stakeholder", "현장", "issue", "보안", "influence", 2, "importance", 2),
                                m("id", "S3", "stakeholder", "사용자", "issue", "품질", "influence", 3, "importance", 3)),
                        "groupedIssues", l(
                                m("issue", "B", "memberIssues", l("S3")),
                                m("issue", "A", "memberIssues", l("S1", "S2")))),
                "competitors", l(m("name", COMPETITOR, "competitiveness", competitiveness())),
                "company", m("competitiveness", competitiveness()),
                "other", m());
    }

    static Map<String, Object> competitiveness() {
        return m("overall", m("level", "EQUAL"), "sales", m("level", "ADVANTAGE"),
                "technical", m("level", "EQUAL"), "price", m("level", "DISADVANTAGE"));
    }

    /** 자사 일곱 팩터가 모두 ourScore, 경쟁사는 모두 3 */
    static Map<String, Object> decision(int ourScore) {
        return decision(factors(ourScore));
    }

    static Map<String, Object> decision(Map<String, Object> ourFactors) {
        return m(
                "scorecard", l(
                        m("company", BidAnalysisScoring.OUR_COMPANY, "scores", ourFactors),
                        m("company", COMPETITOR, "scores", factors(3))),
                "sensitivity", m("note", "가격이 가장 민감"),
                "qualification", l(m("item", "SAM 등록", "resolvable", true)),
                "profitability", m("margin", "10%"),
                "risks", l(m("area", "FINANCIAL", "probability", "M", "impact", "H")),
                "conditions", l(),
                "advice", "참여 권장");
    }

    static Map<String, Object> factors(int score) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (String f : PwinPolicy.FACTORS) {
            out.put(f, m("score", score, "basis", "근거"));
        }
        return out;
    }

    static Map<String, Object> question(String target, String text) {
        return m("target", target, "question", text, "reason", "자료 없음", "field", "overview.budget");
    }
}
