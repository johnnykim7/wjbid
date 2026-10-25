package com.biddingagency.domain.analysis.service;

import com.biddingagency.domain.analysis.entity.BidVerdict;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bid/No-Bid 결정을 저장할 때 서버가 하는 계산 (CR-170, BIZ-091).
 *
 * 모델은 점수와 근거만 쓰고 Pwin 과 판정은 쓰지 않는다. 같은 점수에 같은 판정이 나와야 관리자가 회차끼리 견줄 수 있다.
 * 입력 꼴은 {@link BidAnalysisBodyValidator} 가 먼저 본다.
 */
public final class BidAnalysisScoring {

    public static final String OUR_COMPANY = "OUR_COMPANY";
    private static final BigDecimal MAX_SCORE = BigDecimal.valueOf(5);

    private BidAnalysisScoring() {
    }

    public record Result(
            BigDecimal pwinPercent,
            BidVerdict verdict,
            String verdictReason,
            List<Map<String, Object>> scores,
            List<Map<String, Object>> strengths,
            List<Map<String, Object>> weaknesses,
            List<Map<String, Object>> issueRanking
    ) {
    }

    public static Result compute(Map<String, Object> decision, Map<String, Object> report,
                                 Map<String, Object> capturePlan, PwinPolicy policy) {
        List<Map<String, Object>> scores = new ArrayList<>();
        BigDecimal pwin = null;
        for (Map<String, Object> row : list(decision.get("scorecard"))) {
            String company = String.valueOf(row.get("company"));
            BigDecimal percent = weightedPercent(map(row.get("scores")), policy);
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("company", company);
            s.put("percent", percent);
            scores.add(s);
            if (OUR_COMPANY.equals(company)) pwin = percent;
        }

        List<String> blockers = new ArrayList<>();
        for (Map<String, Object> q : list(decision.get("qualification"))) {
            if (Boolean.FALSE.equals(q.get("resolvable"))) blockers.add(String.valueOf(q.get("item")));
        }
        BidVerdict verdict;
        String reason;
        if (!blockers.isEmpty()) {
            verdict = BidVerdict.NO_GO;
            reason = "해결할 수 없는 자격 미달: " + String.join(", ", blockers);
        } else {
            verdict = verdictOf(pwin, policy.thresholds());
            reason = reasonOf(pwin, verdict, policy.thresholds());
        }

        Map<String, Object> bcc = map(map(report.get("competitors")).get("bcc"));
        List<Map<String, Object>> strengths = new ArrayList<>();
        List<Map<String, Object>> weaknesses = new ArrayList<>();
        compareThemes(bcc, strengths, weaknesses);

        return new Result(pwin, verdict, reason, scores, strengths, weaknesses,
                issueRanking(map(capturePlan.get("issues"))));
    }

    /** Σ(팩터 점수 x 가중치) / 5. 가중치 합이 100 이라 만점이 100 이다 */
    static BigDecimal weightedPercent(Map<String, Object> factorScores, PwinPolicy policy) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Map.Entry<String, Integer> w : policy.weights().entrySet()) {
            int score = ((Number) map(factorScores.get(w.getKey())).get("score")).intValue();
            sum = sum.add(BigDecimal.valueOf((long) score * w.getValue()));
        }
        return sum.divide(MAX_SCORE, 1, RoundingMode.HALF_UP);
    }

    static BidVerdict verdictOf(BigDecimal pwin, List<BigDecimal> thresholds) {
        if (pwin.compareTo(thresholds.get(0)) >= 0) return BidVerdict.STRONG_GO;
        if (pwin.compareTo(thresholds.get(1)) >= 0) return BidVerdict.CONDITIONAL_GO;
        if (pwin.compareTo(thresholds.get(2)) >= 0) return BidVerdict.WEAK;
        return BidVerdict.NO_GO;
    }

    private static String reasonOf(BigDecimal pwin, BidVerdict verdict, List<BigDecimal> t) {
        String p = pwin.toPlainString();
        return switch (verdict) {
            case STRONG_GO -> "Pwin " + p + "% 가 Strong Go 문턱 " + t.get(0).toPlainString() + " 이상";
            case CONDITIONAL_GO -> "Pwin " + p + "% 가 조건부 Go 문턱 " + t.get(1).toPlainString()
                    + " 이상, Strong Go 문턱 " + t.get(0).toPlainString() + " 미만";
            case WEAK -> "Pwin " + p + "% 가 Weak 문턱 " + t.get(2).toPlainString()
                    + " 이상, 조건부 Go 문턱 " + t.get(1).toPlainString() + " 미만";
            case NO_GO -> "Pwin " + p + "% 가 Weak 문턱 " + t.get(2).toPlainString() + " 미만";
        };
    }

    /** BCC 테마마다 자사 점수에서 경쟁사 최고점을 뺀 값이 +1 이상이면 강점, -1 이하면 약점. 경쟁사가 없으면 비운다 */
    private static void compareThemes(Map<String, Object> bcc, List<Map<String, Object>> strengths,
                                      List<Map<String, Object>> weaknesses) {
        Map<String, Object> byCompany = map(bcc.get("scores"));
        Map<String, Object> ours = map(byCompany.get(OUR_COMPANY));
        for (Object themeRaw : rawList(bcc.get("themes"))) {
            String theme = String.valueOf(themeRaw);
            if (!(ours.get(theme) instanceof Number our)) continue;
            Integer best = null;
            for (Map.Entry<String, Object> e : byCompany.entrySet()) {
                if (OUR_COMPANY.equals(e.getKey())) continue;
                if (map(e.getValue()).get(theme) instanceof Number n && (best == null || n.intValue() > best)) {
                    best = n.intValue();
                }
            }
            if (best == null) continue;
            int diff = our.intValue() - best;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("theme", theme);
            row.put("our", our.intValue());
            row.put("bestCompetitor", best);
            row.put("diff", diff);
            if (diff >= 1) strengths.add(row);
            else if (diff <= -1) weaknesses.add(row);
        }
    }

    /** 관련자 이슈 점수 = 영향력 x 중요도, 묶은 이슈 점수 = 속한 관련자 이슈 점수의 합, 높은 순 */
    private static List<Map<String, Object>> issueRanking(Map<String, Object> issues) {
        Map<String, Map<String, Object>> byId = new LinkedHashMap<>();
        for (Map<String, Object> s : list(issues.get("stakeholderIssues"))) {
            byId.put(String.valueOf(s.get("id")), s);
        }
        List<Map<String, Object>> ranking = new ArrayList<>();
        for (Map<String, Object> g : list(issues.get("groupedIssues"))) {
            int total = 0;
            List<Map<String, Object>> members = new ArrayList<>();
            for (Object idRaw : rawList(g.get("memberIssues"))) {
                Map<String, Object> s = byId.get(String.valueOf(idRaw));
                if (s == null) continue;
                int influence = ((Number) s.get("influence")).intValue();
                int importance = ((Number) s.get("importance")).intValue();
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", s.get("id"));
                m.put("stakeholder", s.get("stakeholder"));
                m.put("issue", s.get("issue"));
                m.put("influence", influence);
                m.put("importance", importance);
                m.put("score", influence * importance);
                members.add(m);
                total += influence * importance;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("issue", g.get("issue"));
            row.put("score", total);
            row.put("memberIssues", members);
            ranking.add(row);
        }
        ranking.sort(Comparator.comparingInt((Map<String, Object> r) -> (Integer) r.get("score")).reversed());
        return ranking;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object raw) {
        return raw instanceof Map ? (Map<String, Object>) raw : Map.of();
    }

    private static List<Object> rawList(Object raw) {
        return raw instanceof List<?> l ? new ArrayList<>(l) : List.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> list(Object raw) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object o : rawList(raw)) {
            if (o instanceof Map) out.add((Map<String, Object>) o);
        }
        return out;
    }
}
