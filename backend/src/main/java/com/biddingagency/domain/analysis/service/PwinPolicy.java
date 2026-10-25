package com.biddingagency.domain.analysis.service;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pwin 계산 정책값 (POL-041, CR-170).
 *
 * 가중치는 일곱 팩터가 다 있고 합이 100 이어야 하고, 문턱은 Strong Go, 조건부 Go, Weak 순으로 내림차순이어야 한다.
 * 어긋나면 초기값으로 계산하고 경고를 남긴다. 잘못 넣은 값으로 판정이 뒤틀리는 것보다 스킬의 기준이 낫다.
 */
@Slf4j
public record PwinPolicy(Map<String, Integer> weights, List<BigDecimal> thresholds) {

    public static final List<String> FACTORS = List.of(
            "CUSTOMER_RELATION", "SOLUTION_FIT", "COMPETITION", "PRICE",
            "PAST_PERFORMANCE", "PERSONNEL", "ENVIRONMENT");

    static final String DEFAULT_WEIGHTS =
            "CUSTOMER_RELATION:15,SOLUTION_FIT:20,COMPETITION:15,PRICE:15,PAST_PERFORMANCE:15,PERSONNEL:10,ENVIRONMENT:10";
    static final String DEFAULT_THRESHOLDS = "70,50,30";

    private static final int WEIGHT_SUM = 100;
    private static final int THRESHOLD_COUNT = 3;

    public static PwinPolicy of(String weightsValue, String thresholdsValue) {
        Map<String, Integer> weights = parseWeights(weightsValue);
        if (weights == null) {
            log.warn("[CR-170] BID_PWIN_WEIGHTS 값이 틀려 초기값으로 계산합니다: {}", weightsValue);
            weights = parseWeights(DEFAULT_WEIGHTS);
        }
        List<BigDecimal> thresholds = parseThresholds(thresholdsValue);
        if (thresholds == null) {
            log.warn("[CR-170] BID_PWIN_THRESHOLDS 값이 틀려 초기값으로 계산합니다: {}", thresholdsValue);
            thresholds = parseThresholds(DEFAULT_THRESHOLDS);
        }
        return new PwinPolicy(weights, thresholds);
    }

    public static PwinPolicy defaults() {
        return new PwinPolicy(parseWeights(DEFAULT_WEIGHTS), parseThresholds(DEFAULT_THRESHOLDS));
    }

    private static Map<String, Integer> parseWeights(String value) {
        if (value == null || value.isBlank()) return null;
        Map<String, Integer> out = new LinkedHashMap<>();
        try {
            for (String pair : value.split(",")) {
                String[] kv = pair.split(":");
                if (kv.length != 2) return null;
                String factor = kv[0].trim();
                int weight = Integer.parseInt(kv[1].trim());
                if (!FACTORS.contains(factor) || weight < 0 || out.put(factor, weight) != null) return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
        if (out.size() != FACTORS.size()) return null;
        int sum = out.values().stream().mapToInt(Integer::intValue).sum();
        return sum == WEIGHT_SUM ? out : null;
    }

    private static List<BigDecimal> parseThresholds(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            List<BigDecimal> out = Arrays.stream(value.split(",")).map(String::trim).map(BigDecimal::new).toList();
            if (out.size() != THRESHOLD_COUNT) return null;
            for (int i = 0; i < out.size(); i++) {
                BigDecimal t = out.get(i);
                if (t.signum() < 0 || t.compareTo(BigDecimal.valueOf(WEIGHT_SUM)) > 0) return null;
                if (i > 0 && out.get(i - 1).compareTo(t) <= 0) return null;
            }
            return out;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
