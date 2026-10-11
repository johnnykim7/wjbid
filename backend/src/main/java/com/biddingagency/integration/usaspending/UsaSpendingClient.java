package com.biddingagency.integration.usaspending;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * USAspending.gov 계약 검색과 상세 (CR-142, BID-MKT-025, BID-MKT-026). 키와 요금이 없다.
 *
 * 호출 한도가 공식 숫자로 밝혀져 있지 않아(2026-10-11 확인 못 함) 호출 사이에 정한 간격을 둔다(POL-032).
 * 연결 실패, 429, 5xx 는 정한 시간 기다려 다시 부르고, 정한 횟수만큼 실패하면 예외를 낸다(BIZ-053).
 */
@Slf4j
@Component
public class UsaSpendingClient {

    public static final int PAGE_SIZE = 100;

    private static final List<String> SEARCH_FIELDS = List.of("Award ID", "Recipient Name", "Start Date",
            "End Date", "Award Amount", "Description", "NAICS", "PSC", "Contract Award Type",
            "generated_internal_id", "Last Modified Date", "Recipient UEI", "Awarding Sub Agency",
            "Parent Award ID");

    private final String baseUrl;
    private final List<String> awardTypeCodes;
    private final String agencyName;
    private final String countryCode;
    private final long requestIntervalMs;
    private final long retryWaitMs;
    private final int maxAttempts;
    private final ObjectMapper objectMapper;
    private final CloseableHttpClient httpClient;
    private long lastCallAt;

    public UsaSpendingClient(
            @Value("${app.federal-contract.base-url:https://api.usaspending.gov}") String baseUrl,
            @Value("${app.federal-contract.award-type-codes:A,B,C,D}") String awardTypeCodes,
            @Value("${app.federal-contract.agency-name:Department of Defense}") String agencyName,
            @Value("${app.federal-contract.country-code:KOR}") String countryCode,
            @Value("${app.federal-contract.request-interval-ms:1000}") long requestIntervalMs,
            @Value("${app.federal-contract.retry-wait-ms:5000}") long retryWaitMs,
            @Value("${app.federal-contract.max-attempts:3}") int maxAttempts,
            ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.awardTypeCodes = java.util.Arrays.stream(awardTypeCodes.split(","))
                .map(String::trim).filter(c -> !c.isEmpty()).toList();
        this.agencyName = agencyName;
        this.countryCode = countryCode;
        this.requestIntervalMs = requestIntervalMs;
        this.retryWaitMs = retryWaitMs;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.objectMapper = objectMapper;
        RequestConfig config = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(30))
                .setResponseTimeout(Timeout.ofSeconds(120))
                .build();
        this.httpClient = HttpClients.custom().setDefaultRequestConfig(config).build();
    }

    /**
     * 받는 범위(POL-031) 안의 계약 한 쪽. 거래일 또는 마지막 바뀐 날이 from 부터 to 사이인 것.
     * 계약번호 순으로 받아 쪽이 넘어가는 사이에 줄이 섞이지 않게 한다.
     *
     * @param byModifiedDate 참이면 마지막 바뀐 날로 거른다(매일 받기)
     * @param awardIds       계약번호로 좁힐 때(옵션 연장 알림 찾기). 없으면 null
     * @return 검색 결과 줄. PAGE_SIZE 보다 적으면 마지막 쪽이다
     */
    public List<Map<String, Object>> search(LocalDate from, LocalDate to, boolean byModifiedDate,
                                            List<String> awardIds, int page) {
        Map<String, Object> period = new LinkedHashMap<>();
        period.put("start_date", from.toString());
        period.put("end_date", to.toString());
        if (byModifiedDate) period.put("date_type", "last_modified_date");

        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("award_type_codes", awardTypeCodes);
        filters.put("agencies", List.of(Map.of("type", "awarding", "tier", "toptier", "name", agencyName)));
        filters.put("place_of_performance_locations", List.of(Map.of("country", countryCode)));
        filters.put("recipient_locations", List.of(Map.of("country", countryCode)));
        filters.put("time_period", List.of(period));
        if (awardIds != null && !awardIds.isEmpty()) filters.put("award_ids", awardIds);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("filters", filters);
        body.put("fields", SEARCH_FIELDS);
        body.put("limit", PAGE_SIZE);
        body.put("page", page);
        body.put("sort", "Award ID");
        body.put("order", "asc");

        Map<String, Object> response = call("search", () -> {
            HttpPost post = new HttpPost(baseUrl + "/api/v2/search/spending_by_award/");
            post.setEntity(new StringEntity(objectMapper.writeValueAsString(body), ContentType.APPLICATION_JSON));
            return post;
        });
        Object results = response.get("results");
        if (!(results instanceof List<?> list)) return List.of();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object o : list) {
            if (o instanceof Map<?, ?> m) {
                @SuppressWarnings("unchecked") Map<String, Object> row = (Map<String, Object>) m;
                rows.add(row);
            }
        }
        return rows;
    }

    /** 계약 상세. 옵션 끝날짜와 원래 공고번호는 여기에만 있다 */
    public Map<String, Object> detail(String generatedInternalId) {
        String id = URLEncoder.encode(generatedInternalId, StandardCharsets.UTF_8);
        return call("detail", () -> new HttpGet(baseUrl + "/api/v2/awards/" + id + "/"));
    }

    private interface RequestFactory {
        HttpUriRequestBase create() throws IOException;
    }

    private synchronized Map<String, Object> call(String kind, RequestFactory factory) {
        UsaSpendingException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            if (attempt > 1) sleep(retryWaitMs);
            waitInterval();
            try {
                HttpUriRequestBase request = factory.create();
                request.addHeader("Accept", "application/json");
                return httpClient.execute(request, response -> {
                    int status = response.getCode();
                    String text = response.getEntity() == null ? "" : EntityUtils.toString(response.getEntity());
                    if (status == 200) {
                        return objectMapper.readValue(text, new TypeReference<Map<String, Object>>() { });
                    }
                    boolean clientError = status >= 400 && status < 500 && status != 429;
                    throw new UsaSpendingException("USAspending " + kind + " " + status + ": " + abbreviate(text),
                            status, clientError, null);
                });
            } catch (UsaSpendingException e) {
                if (e.isClientError()) throw e;
                last = e;
            } catch (IOException e) {
                last = new UsaSpendingException("USAspending " + kind + " 연결 실패: " + e.getMessage(), 0, false, e);
            } finally {
                lastCallAt = System.currentTimeMillis();
            }
            log.warn("USAspending {} 실패 {}/{}: {}", kind, attempt, maxAttempts, last.getMessage());
        }
        throw last;
    }

    private void waitInterval() {
        long wait = lastCallAt + requestIntervalMs - System.currentTimeMillis();
        if (wait > 0) sleep(wait);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UsaSpendingException("USAspending 호출이 멈춤", 0, false, e);
        }
    }

    private static String abbreviate(String s) {
        return s.length() > 300 ? s.substring(0, 300) : s;
    }
}
