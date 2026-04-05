package com.biddingagency.integration.samgov.client;

import com.biddingagency.common.EvidenceLogger;
import com.biddingagency.integration.samgov.dto.SAMOpportunityResponse;
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

    public SAMGovApiClient(
            @Value("${app.sam-gov.api-key}") String apiKey,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
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
                String responseBody = EntityUtils.toString(response.getEntity());

                if (statusCode == 200) {
                    SAMOpportunityResponse result = objectMapper.readValue(responseBody, SAMOpportunityResponse.class);
                    // SAM.gov sometimes returns 200 with a quota-exceeded JSON body
                    if (result.getOpportunitiesData() == null && responseBody.contains("throttled")) {
                        log.warn("SAM.gov quota exceeded (200 throttled): {}", responseBody);
                        EvidenceLogger.logFailure("sam-gov", "GET /opportunities/v2/search", reqId,
                                reqSummary, Map.of("statusCode", 200, "throttled", true), "THROTTLED");
                        throw new RuntimeException("SAM.gov API quota exceeded (throttled)");
                    }
                    int count = result.getOpportunitiesData() != null ? result.getOpportunitiesData().size() : 0;
                    EvidenceLogger.logSuccess("sam-gov", "GET /opportunities/v2/search", reqId,
                            reqSummary, Map.of("statusCode", 200, "resultCount", count));
                    return result;
                } else if (statusCode == 429) {
                    log.warn("SAM.gov API rate limited (429). Body: {}", responseBody);
                    EvidenceLogger.logFailure("sam-gov", "GET /opportunities/v2/search", reqId,
                            reqSummary, Map.of("statusCode", 429), "RATE_LIMITED");
                    throw new RuntimeException("SAM.gov API quota exceeded (429)");
                } else {
                    log.error("SAM.gov API error. Status: {}, Body: {}", statusCode, responseBody);
                    EvidenceLogger.logFailure("sam-gov", "GET /opportunities/v2/search", reqId,
                            reqSummary, Map.of("statusCode", statusCode), String.valueOf(statusCode));
                    throw new RuntimeException("SAM.gov API returned status: " + statusCode);
                }
            });

        } catch (IOException e) {
            log.error("Error calling SAM.gov API", e);
            EvidenceLogger.logTimeout("sam-gov", "GET /opportunities/v2/search",
                    UUID.randomUUID().toString(), Map.of("keyword", keyword != null ? keyword : ""));
            throw new RuntimeException("Failed to call SAM.gov API", e);
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
            url.append("&q=").append(encodeValue(keyword));
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
