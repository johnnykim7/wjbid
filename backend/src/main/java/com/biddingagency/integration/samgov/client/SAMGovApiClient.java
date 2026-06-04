package com.biddingagency.integration.samgov.client;

import com.biddingagency.common.EvidenceLogger;
import com.biddingagency.integration.samgov.dto.SAMOpportunityResponse;
import com.biddingagency.integration.samgov.quota.SamQuotaLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * SAM.gov API Client
 *
 * Interacts with SAM.gov Opportunities API v2
 */
@Slf4j
@Component
public class SAMGovApiClient {

    private static final String API_BASE_URL = "https://api.sam.gov/opportunities/v2/search";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    private final String apiKey;
    private final ObjectMapper objectMapper;
    private final CloseableHttpClient httpClient;
    private final SamQuotaLogger quotaLogger;

    public SAMGovApiClient(
            @Value("${app.sam-gov.api-key}") String apiKey,
            ObjectMapper objectMapper,
            SamQuotaLogger quotaLogger) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        this.quotaLogger = quotaLogger;
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(30))
                .setResponseTimeout(Timeout.ofSeconds(120))
                .build();
        this.httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    /**
     * Search opportunities by keyword
     */
    public SAMOpportunityResponse searchOpportunities(String keyword, LocalDate postedFrom,
                                                      LocalDate postedTo, int limit, int offset) {
        try {
            String url = buildSearchUrl(keyword, postedFrom, postedTo, limit, offset);
            log.info("Calling SAM.gov API: {}", url);

            HttpGet request = new HttpGet(url);
            request.addHeader("X-Api-Key", apiKey);
            request.addHeader("Accept", "application/json");

            String reqId = UUID.randomUUID().toString();
            Map<String, Object> reqSummary = Map.of("keyword", keyword != null ? keyword : "", "limit", limit);

            return httpClient.execute(request, response -> {
                int statusCode = response.getCode();
                org.apache.hc.core5.http.Header[] headers = response.getHeaders();
                String responseBody = EntityUtils.toString(response.getEntity());

                if (statusCode == 200) {
                    SAMOpportunityResponse result = objectMapper.readValue(responseBody, SAMOpportunityResponse.class);
                    // SAM.gov sometimes returns 200 with a quota-exceeded JSON body
                    if (result.getOpportunitiesData() == null && responseBody.contains("throttled")) {
                        log.warn("SAM.gov quota exceeded (200 throttled): {}", responseBody);
                        // CR-036: throttle은 200이어도 쿼터 소진 — 에러로 계측(응답 본문에 한도 단서)
                        quotaLogger.record(SamQuotaLogger.EP_SEARCH, 200, headers, false, responseBody);
                        EvidenceLogger.logFailure("sam-gov", "GET /opportunities/v2/search", reqId,
                                reqSummary, Map.of("statusCode", 200, "throttled", true), "THROTTLED");
                        throw new RuntimeException("SAM.gov API quota exceeded (throttled)");
                    }
                    int count = result.getOpportunitiesData() != null ? result.getOpportunitiesData().size() : 0;
                    quotaLogger.record(SamQuotaLogger.EP_SEARCH, 200, headers, true, null);
                    EvidenceLogger.logSuccess("sam-gov", "GET /opportunities/v2/search", reqId,
                            reqSummary, Map.of("statusCode", 200, "resultCount", count));
                    return result;
                } else if (statusCode == 429) {
                    log.warn("SAM.gov API rate limited (429). Body: {}", responseBody);
                    quotaLogger.record(SamQuotaLogger.EP_SEARCH, 429, headers, false, responseBody);
                    EvidenceLogger.logFailure("sam-gov", "GET /opportunities/v2/search", reqId,
                            reqSummary, Map.of("statusCode", 429), "RATE_LIMITED");
                    throw new RuntimeException("SAM.gov API quota exceeded (429)");
                } else {
                    log.error("SAM.gov API error. Status: {}, Body: {}", statusCode, responseBody);
                    quotaLogger.record(SamQuotaLogger.EP_SEARCH, statusCode, headers, false, responseBody);
                    EvidenceLogger.logFailure("sam-gov", "GET /opportunities/v2/search", reqId,
                            reqSummary, Map.of("statusCode", statusCode), String.valueOf(statusCode));
                    throw new RuntimeException("SAM.gov API returned status: " + statusCode);
                }
            });

        } catch (IOException e) {
            log.error("Error calling SAM.gov API", e);
            // CR-036: 응답을 못 받은 호출(timeout/IO 실패)도 쿼터를 소진했을 수 있다 — error로 계측
            quotaLogger.record(SamQuotaLogger.EP_SEARCH, null, null, false,
                    e.getClass().getSimpleName() + ": " + e.getMessage());
            EvidenceLogger.logTimeout("sam-gov", "GET /opportunities/v2/search",
                    UUID.randomUUID().toString(), Map.of("keyword", keyword != null ? keyword : ""));
            throw new RuntimeException("Failed to call SAM.gov API", e);
        }
    }

    /**
     * CR-022 2차: noticedesc URL을 호출해 본문(평문) 응답을 가져온다.
     * SAM 응답 형식: {"description": "<HTML or plain text>"} (빈 본문이면 " " 한 칸).
     * 호출 실패/빈 본문 시 null 반환. 예외 안 던짐(수집 흐름 방해 방지).
     */
    public String fetchNoticeDescription(String noticeDescUrl) {
        if (noticeDescUrl == null || noticeDescUrl.isBlank()) return null;
        try {
            String url = noticeDescUrl + (noticeDescUrl.contains("?") ? "&" : "?") + "api_key=" + apiKey;
            HttpGet request = new HttpGet(url);
            request.addHeader("Accept", "application/json");
            return httpClient.execute(request, response -> {
                int statusCode = response.getCode();
                org.apache.hc.core5.http.Header[] headers = response.getHeaders();
                String body = EntityUtils.toString(response.getEntity());
                if (statusCode != 200) {
                    log.warn("[CR-022-2] noticedesc {}: {}", statusCode, noticeDescUrl);
                    // CR-036: noticedesc도 SAM 쿼터를 쓴다 — non-200(throttle/429 포함) 계측
                    quotaLogger.record(SamQuotaLogger.EP_NOTICEDESC, statusCode, headers, false, body);
                    return null;
                }
                // CR-036: throttle은 200 + throttled 본문으로도 올 수 있음
                boolean throttled = body != null && body.contains("throttled");
                quotaLogger.record(SamQuotaLogger.EP_NOTICEDESC, 200, headers, !throttled, throttled ? body : null);
                @SuppressWarnings("unchecked")
                Map<String, Object> json = objectMapper.readValue(body, Map.class);
                Object desc = json.get("description");
                if (!(desc instanceof String s)) return null;
                String trimmed = s.trim();
                return trimmed.isEmpty() ? null : trimmed;
            });
        } catch (Exception e) {
            log.warn("[CR-022-2] noticedesc fetch 실패: {} ({})", noticeDescUrl, e.getMessage());
            // CR-036: 응답을 못 받은 호출(timeout/IO 실패)도 쿼터를 소진했을 수 있다 — error로 계측
            quotaLogger.record(SamQuotaLogger.EP_NOTICEDESC, null, null, false,
                    e.getClass().getSimpleName() + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Build search URL with parameters
     */
    private String buildSearchUrl(String keyword, LocalDate postedFrom, LocalDate postedTo,
                                   int limit, int offset) {
        StringBuilder url = new StringBuilder(API_BASE_URL);
        url.append("?api_key=").append(apiKey);

        if (keyword != null && !keyword.isEmpty()) {
            // CR-036: q는 SAM v2 미지원 파라미터 — 무시되어 날짜창 전체 공고가 반환됐다(4880건).
            // 우리가 원하는 건 발주처(organization)가 411TH CSB인 공고이므로 정식 파라미터
            // organizationName으로 서버측 필터한다. totalRecords가 실수치로 떨어져 1콜로 끝난다.
            // (client-side organizationMatches는 이중 안전망으로 유지)
            String orgName = keyword.replaceAll("\"", "").trim();
            url.append("&organizationName=").append(encodeValue(orgName));
        }

        if (postedFrom != null) {
            url.append("&postedFrom=").append(postedFrom.format(DATE_FORMATTER));
        }

        if (postedTo != null) {
            url.append("&postedTo=").append(postedTo.format(DATE_FORMATTER));
        }

        url.append("&limit=").append(Math.min(limit, 1000)); // Max 1000 per request
        url.append("&offset=").append(offset);

        return url.toString();
    }

    /**
     * URL encode value
     */
    private String encodeValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * Close HTTP client
     */
    public void close() {
        try {
            httpClient.close();
        } catch (IOException e) {
            log.error("Error closing HTTP client", e);
        }
    }
}
