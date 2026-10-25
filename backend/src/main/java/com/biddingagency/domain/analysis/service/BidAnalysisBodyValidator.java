package com.biddingagency.domain.analysis.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * save_bid_analysis 본문의 꼴 검사 (CR-170, CR-170 문서 2.4).
 *
 * 틀린 곳을 경로(예: eligibility[0].evidence.confidence)와 함께 IllegalArgumentException 으로 알린다.
 * 워크플로우가 그 경로를 보고 고쳐 다시 저장한다. 서버 계산이 읽는 칸(점수, 자격 해결, 이슈 점수)은 값 범위까지 본다.
 */
public final class BidAnalysisBodyValidator {

    public enum Part {
        REPORT,
        CAPTURE_PLAN,
        DECISION
    }

    private static final Set<String> CONFIDENCES = Set.of("CONFIRMED", "ESTIMATED", "UNKNOWN");
    private static final Set<String> AWARD_SOURCES = Set.of("FEDERAL_CONTRACT", "SAM_AWARD_NOTICE");
    private static final Set<String> ORDER_VERDICTS = Set.of("NEW", "RECURRING", "FOLLOW_ON", "UNKNOWN");
    private static final Set<String> ELIGIBILITY_STATUSES = Set.of("MET", "NOT_MET", "CONDITIONAL", "UNKNOWN");
    private static final Set<String> LIKELIHOODS = Set.of("HIGH", "MEDIUM", "LOW");
    private static final Set<String> INDICATOR_VALUES = Set.of("YES", "NO", "UNKNOWN");
    private static final Set<String> FAIRNESS_JUDGEMENTS = Set.of("GOOD", "CAUTION", "RISK");
    private static final Set<String> COMPETITIVENESS_LEVELS = Set.of("ADVANTAGE", "EQUAL", "DISADVANTAGE");
    private static final List<String> COMPETITIVENESS_AXES = List.of("overall", "sales", "technical", "price");
    private static final Set<String> RISK_AREAS = Set.of("FINANCIAL", "TECHNICAL", "CONTRACT");
    private static final Set<String> LEVELS_HML = Set.of("H", "M", "L");
    private static final Set<String> QUESTION_TARGETS = Set.of("CUSTOMER", "ADMIN");
    private static final List<String> OVERVIEW_EVIDENCE = List.of("projectName", "agency", "budget", "period", "contractType");
    /** 스킬 01 STEP 7 의 진입 공정성 징후 수 */
    private static final int FAIRNESS_INDICATORS = 10;
    private static final int SCORE_MIN = 1;
    private static final int SCORE_MAX = 5;

    private BidAnalysisBodyValidator() {
    }

    public static void validate(Part part, Map<String, Object> body) {
        switch (part) {
            case REPORT -> report(body);
            case CAPTURE_PLAN -> capturePlan(body);
            case DECISION -> decision(body);
        }
    }

    public static void validateQuestions(List<Object> questions) {
        for (int i = 0; i < questions.size(); i++) {
            String p = "questions[" + i + "]";
            Map<String, Object> q = object(questions.get(i), p);
            oneOf(q.get("target"), QUESTION_TARGETS, p + ".target");
            text(q.get("question"), p + ".question");
        }
    }

    // ─── 종합 분석 리포트 ─────────────────────────────────────────────────

    private static void report(Map<String, Object> body) {
        Map<String, Object> overview = object(body.get("overview"), "overview");
        for (String key : OVERVIEW_EVIDENCE) {
            evidence(overview.get(key), "overview." + key);
        }
        object(body.get("agency"), "agency");

        Map<String, Object> history = object(body.get("orderHistory"), "orderHistory");
        List<Object> rows = array(history.get("rows"), "orderHistory.rows");
        for (int i = 0; i < rows.size(); i++) {
            String p = "orderHistory.rows[" + i + "]";
            oneOf(object(rows.get(i), p).get("source"), AWARD_SOURCES, p + ".source");
        }
        oneOf(history.get("verdict"), ORDER_VERDICTS, "orderHistory.verdict");
        evidence(history.get("incumbent"), "orderHistory.incumbent");
        evidence(history.get("incumbentAdvantage"), "orderHistory.incumbentAdvantage");

        Map<String, Object> requirements = object(body.get("requirements"), "requirements");
        array(requirements.get("themes"), "requirements.themes");
        array(requirements.get("evaluation"), "requirements.evaluation");
        array(requirements.get("questionCandidates"), "requirements.questionCandidates");

        List<Object> eligibility = array(body.get("eligibility"), "eligibility");
        for (int i = 0; i < eligibility.size(); i++) {
            String p = "eligibility[" + i + "]";
            Map<String, Object> e = object(eligibility.get(i), p);
            text(e.get("item"), p + ".item");
            oneOf(e.get("status"), ELIGIBILITY_STATUSES, p + ".status");
            evidence(e.get("evidence"), p + ".evidence");
        }

        Map<String, Object> competitors = object(body.get("competitors"), "competitors");
        List<Object> companies = array(competitors.get("companies"), "competitors.companies");
        for (int i = 0; i < companies.size(); i++) {
            String p = "competitors.companies[" + i + "]";
            Map<String, Object> c = object(companies.get(i), p);
            text(c.get("name"), p + ".name");
            oneOf(c.get("likelihood"), LIKELIHOODS, p + ".likelihood");
        }
        bcc(object(competitors.get("bcc"), "competitors.bcc"));

        Map<String, Object> fairness = object(body.get("fairness"), "fairness");
        List<Object> indicators = array(fairness.get("indicators"), "fairness.indicators");
        if (indicators.size() != FAIRNESS_INDICATORS) {
            throw invalid("fairness.indicators", "징후 " + FAIRNESS_INDICATORS + "개가 순번 1~10 으로 있어야 합니다");
        }
        Set<Integer> numbers = new HashSet<>();
        for (int i = 0; i < indicators.size(); i++) {
            String p = "fairness.indicators[" + i + "]";
            Map<String, Object> ind = object(indicators.get(i), p);
            numbers.add(integer(ind.get("no"), 1, FAIRNESS_INDICATORS, p + ".no"));
            oneOf(ind.get("value"), INDICATOR_VALUES, p + ".value");
        }
        if (numbers.size() != FAIRNESS_INDICATORS) {
            throw invalid("fairness.indicators", "순번 1~10 이 한 번씩 있어야 합니다");
        }
        oneOf(fairness.get("judgement"), FAIRNESS_JUDGEMENTS, "fairness.judgement");

        array(body.get("hiddenIssues"), "hiddenIssues");
        array(body.get("insights"), "insights");
    }

    /** BCC 점수는 강점과 약점 계산이 읽는다. 자사가 있어야 하고 테마 점수는 1~5 */
    private static void bcc(Map<String, Object> bcc) {
        List<Object> themes = array(bcc.get("themes"), "competitors.bcc.themes");
        for (int i = 0; i < themes.size(); i++) {
            text(themes.get(i), "competitors.bcc.themes[" + i + "]");
        }
        Map<String, Object> scores = object(bcc.get("scores"), "competitors.bcc.scores");
        if (!scores.containsKey(BidAnalysisScoring.OUR_COMPANY)) {
            throw invalid("competitors.bcc.scores." + BidAnalysisScoring.OUR_COMPANY, "자사 점수가 있어야 합니다");
        }
        for (Map.Entry<String, Object> company : scores.entrySet()) {
            String p = "competitors.bcc.scores." + company.getKey();
            for (Map.Entry<String, Object> theme : object(company.getValue(), p).entrySet()) {
                integer(theme.getValue(), SCORE_MIN, SCORE_MAX, p + "." + theme.getKey());
            }
        }
    }

    // ─── 캡처플랜 ─────────────────────────────────────────────────────────

    private static void capturePlan(Map<String, Object> body) {
        object(body.get("business"), "business");

        Map<String, Object> issues = object(body.get("issues"), "issues");
        List<Object> stakeholderIssues = array(issues.get("stakeholderIssues"), "issues.stakeholderIssues");
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < stakeholderIssues.size(); i++) {
            String p = "issues.stakeholderIssues[" + i + "]";
            Map<String, Object> s = object(stakeholderIssues.get(i), p);
            if (!ids.add(text(s.get("id"), p + ".id"))) {
                throw invalid(p + ".id", "id 가 겹칩니다");
            }
            text(s.get("issue"), p + ".issue");
            integer(s.get("influence"), SCORE_MIN, SCORE_MAX, p + ".influence");
            integer(s.get("importance"), SCORE_MIN, SCORE_MAX, p + ".importance");
        }
        List<Object> grouped = array(issues.get("groupedIssues"), "issues.groupedIssues");
        for (int i = 0; i < grouped.size(); i++) {
            String p = "issues.groupedIssues[" + i + "]";
            Map<String, Object> g = object(grouped.get(i), p);
            text(g.get("issue"), p + ".issue");
            List<Object> members = array(g.get("memberIssues"), p + ".memberIssues");
            for (int j = 0; j < members.size(); j++) {
                String mp = p + ".memberIssues[" + j + "]";
                if (!ids.contains(text(members.get(j), mp))) {
                    throw invalid(mp, "stakeholderIssues 의 id 여야 합니다");
                }
            }
        }

        List<Object> competitors = array(body.get("competitors"), "competitors");
        for (int i = 0; i < competitors.size(); i++) {
            String p = "competitors[" + i + "]";
            Map<String, Object> c = object(competitors.get(i), p);
            text(c.get("name"), p + ".name");
            competitiveness(c.get("competitiveness"), p + ".competitiveness");
        }
        competitiveness(object(body.get("company"), "company").get("competitiveness"), "company.competitiveness");
        object(body.get("other"), "other");
    }

    private static void competitiveness(Object raw, String path) {
        Map<String, Object> c = object(raw, path);
        for (String axis : COMPETITIVENESS_AXES) {
            oneOf(object(c.get(axis), path + "." + axis).get("level"), COMPETITIVENESS_LEVELS, path + "." + axis + ".level");
        }
    }

    // ─── Bid/No-Bid 결정 ──────────────────────────────────────────────────

    private static void decision(Map<String, Object> body) {
        List<Object> scorecard = array(body.get("scorecard"), "scorecard");
        int ours = 0;
        for (int i = 0; i < scorecard.size(); i++) {
            String p = "scorecard[" + i + "]";
            Map<String, Object> row = object(scorecard.get(i), p);
            if (BidAnalysisScoring.OUR_COMPANY.equals(text(row.get("company"), p + ".company"))) ours++;
            Map<String, Object> scores = object(row.get("scores"), p + ".scores");
            for (String factor : PwinPolicy.FACTORS) {
                String fp = p + ".scores." + factor;
                Map<String, Object> f = object(scores.get(factor), fp);
                integer(f.get("score"), SCORE_MIN, SCORE_MAX, fp + ".score");
                text(f.get("basis"), fp + ".basis");
            }
        }
        if (ours != 1) {
            throw invalid("scorecard", "company " + BidAnalysisScoring.OUR_COMPANY + " 인 줄이 하나 있어야 합니다");
        }
        if (body.get("sensitivity") == null) {
            throw invalid("sensitivity", "값이 있어야 합니다");
        }
        List<Object> qualification = array(body.get("qualification"), "qualification");
        for (int i = 0; i < qualification.size(); i++) {
            String p = "qualification[" + i + "]";
            Map<String, Object> q = object(qualification.get(i), p);
            text(q.get("item"), p + ".item");
            if (!(q.get("resolvable") instanceof Boolean)) {
                throw invalid(p + ".resolvable", "true 나 false 여야 합니다");
            }
        }
        object(body.get("profitability"), "profitability");
        List<Object> risks = array(body.get("risks"), "risks");
        for (int i = 0; i < risks.size(); i++) {
            String p = "risks[" + i + "]";
            Map<String, Object> r = object(risks.get(i), p);
            oneOf(r.get("area"), RISK_AREAS, p + ".area");
            oneOf(r.get("probability"), LEVELS_HML, p + ".probability");
            oneOf(r.get("impact"), LEVELS_HML, p + ".impact");
        }
        array(body.get("conditions"), "conditions");
        text(body.get("advice"), "advice");
    }

    // ─── 공통 ─────────────────────────────────────────────────────────────

    /** 근거 값 {value, confidence, source}. 미상이면 value 는 비어도 된다 */
    private static void evidence(Object raw, String path) {
        Map<String, Object> e = object(raw, path);
        oneOf(e.get("confidence"), CONFIDENCES, path + ".confidence");
        text(e.get("source"), path + ".source");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object raw, String path) {
        if (!(raw instanceof Map)) throw invalid(path, "객체여야 합니다");
        return (Map<String, Object>) raw;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> array(Object raw, String path) {
        if (!(raw instanceof List)) throw invalid(path, "배열이어야 합니다");
        return (List<Object>) raw;
    }

    private static String text(Object raw, String path) {
        if (!(raw instanceof String s) || s.isBlank()) throw invalid(path, "글이 있어야 합니다");
        return s;
    }

    private static void oneOf(Object raw, Set<String> allowed, String path) {
        if (!(raw instanceof String s) || !allowed.contains(s)) {
            throw invalid(path, String.join(", ", allowed.stream().sorted().toList()) + " 가운데 하나여야 합니다");
        }
    }

    private static int integer(Object raw, int min, int max, String path) {
        if (!(raw instanceof Number n) || n.doubleValue() != Math.rint(n.doubleValue())
                || n.intValue() < min || n.intValue() > max) {
            throw invalid(path, min + "~" + max + " 의 정수여야 합니다");
        }
        return n.intValue();
    }

    private static IllegalArgumentException invalid(String path, String problem) {
        return new IllegalArgumentException(path + ": " + problem);
    }
}
