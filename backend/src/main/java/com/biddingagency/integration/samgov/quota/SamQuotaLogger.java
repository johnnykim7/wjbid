package com.biddingagency.integration.samgov.quota;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.Header;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CR-036: SAM API 호출 계측 — 매 호출 결과를 로그로 찍고 sam_api_call_log에 일자(UTC)·엔드포인트별 집계.
 *
 * <p>기록하는 것:
 * <ul>
 *   <li>우리가 몇 번 불렀는지(success/error 카운트)</li>
 *   <li>SAM이 응답 헤더로 알려주는 실제 한도/잔량(X-RateLimit-Limit / X-RateLimit-Remaining 등)</li>
 *   <li>에러 시 SAM 응답 본문 전문(쿼터 초과 메시지에 실제 한도 숫자가 들어있을 수 있음)</li>
 * </ul>
 *
 * <p>계측은 본 호출 실패 시에도 남아야 하므로 Repository.record는 REQUIRES_NEW 트랜잭션.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SamQuotaLogger {

    public static final String EP_SEARCH = "search";
    public static final String EP_NOTICEDESC = "noticedesc";
    public static final String EP_ATTACHMENT = "attachment";

    private static final int ERROR_BODY_MAX = 3000;

    private final SamApiCallLogRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock = Clock.systemUTC();

    /**
     * 한 번의 SAM 호출 결과를 기록한다.
     * @param endpoint  EP_SEARCH / EP_NOTICEDESC / EP_ATTACHMENT
     * @param status    HTTP status (응답 못 받았으면 null)
     * @param headers   응답 헤더 배열 (null 가능)
     * @param success   비즈니스적으로 성공인가 (200 + 정상 본문). throttle/429/non-200/예외는 false
     * @param errorBody 에러 시 SAM 응답 본문 (성공이면 null)
     */
    public void record(String endpoint, Integer status, Header[] headers, boolean success, String errorBody) {
        Map<String, String> rateHeaders = extractRateHeaders(headers);
        String rateLimit = firstNonNull(rateHeaders, "x-ratelimit-limit", "x-rate-limit-limit", "ratelimit-limit");
        String rateRemaining = firstNonNull(rateHeaders, "x-ratelimit-remaining", "x-rate-limit-remaining", "ratelimit-remaining");
        String rateHeadersJson = rateHeaders.isEmpty() ? null : toJson(rateHeaders);
        String truncatedBody = truncate(errorBody);

        // 항상 로그로 남긴다 (DB와 별개로 즉시 grep 가능)
        if (success) {
            log.info("[SAM-QUOTA] endpoint={} status={} limit={} remaining={} headers={}",
                    endpoint, status, rateLimit, rateRemaining, rateHeadersJson);
        } else {
            log.warn("[SAM-QUOTA][ERROR] endpoint={} status={} limit={} remaining={} headers={} body={}",
                    endpoint, status, rateLimit, rateRemaining, rateHeadersJson, truncatedBody);
        }

        try {
            LocalDate utcDate = LocalDate.now(clock);
            repository.record(
                    utcDate, endpoint,
                    success ? 1L : 0L,
                    success ? 0L : 1L,
                    status, rateLimit, rateRemaining, rateHeadersJson,
                    truncatedBody,
                    LocalDateTime.now(ZoneOffset.UTC));
        } catch (Exception e) {
            // 계측 실패가 본 흐름을 막지 않도록
            log.warn("[SAM-QUOTA] 계측 적재 실패 endpoint={}: {}", endpoint, e.getMessage());
        }
    }

    /** rate/limit 관련 헤더만 추려 소문자 키 맵으로. */
    private Map<String, String> extractRateHeaders(Header[] headers) {
        Map<String, String> out = new LinkedHashMap<>();
        if (headers == null) return out;
        for (Header h : headers) {
            if (h == null || h.getName() == null) continue;
            String name = h.getName().toLowerCase();
            if (name.contains("ratelimit") || name.contains("rate-limit") || name.contains("retry-after")) {
                out.put(name, h.getValue());
            }
        }
        return out;
    }

    private String firstNonNull(Map<String, String> map, String... keys) {
        for (String k : keys) {
            String v = map.get(k);
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    private String toJson(Map<String, String> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return map.toString();
        }
    }

    private String truncate(String s) {
        if (s == null) return null;
        return s.length() > ERROR_BODY_MAX ? s.substring(0, ERROR_BODY_MAX) : s;
    }
}
