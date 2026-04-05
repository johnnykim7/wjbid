package com.biddingagency.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * FlowGuard Evidence 로그 출력 유틸리티.
 * 외부 시스템 호출 시 구조화된 JSON 로그를 남겨 Log Probe가 수집할 수 있도록 한다.
 */
@Slf4j
public final class EvidenceLogger {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private EvidenceLogger() {}

    public static void logSuccess(String partner, String endpoint, String requestId,
                                  Map<String, Object> requestSummary, Map<String, Object> responseSummary) {
        writeLog(partner, endpoint, requestId, requestSummary, responseSummary, "SUCCESS", null);
    }

    public static void logFailure(String partner, String endpoint, String requestId,
                                  Map<String, Object> requestSummary, Map<String, Object> responseSummary,
                                  String errorCode) {
        writeLog(partner, endpoint, requestId, requestSummary, responseSummary, "FAILURE", errorCode);
    }

    public static void logTimeout(String partner, String endpoint, String requestId,
                                  Map<String, Object> requestSummary) {
        writeLog(partner, endpoint, requestId, requestSummary, Map.of(), "TIMEOUT", null);
    }

    private static void writeLog(String partner, String endpoint, String requestId,
                                 Map<String, Object> requestSummary, Map<String, Object> responseSummary,
                                 String result, String errorCode) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("logType", "EXTERNAL_INTEGRATION");
        evidence.put("flowId", FlowIdContext.get());
        evidence.put("partner", partner);
        evidence.put("endpoint", endpoint);
        evidence.put("requestId", requestId);
        evidence.put("requestSummary", requestSummary);
        evidence.put("responseSummary", responseSummary);
        evidence.put("result", result);
        evidence.put("errorCode", errorCode);
        evidence.put("timestamp", Instant.now().toString());

        try {
            log.info(MAPPER.writeValueAsString(evidence));
        } catch (Exception e) {
            log.warn("Failed to serialize evidence log", e);
        }
    }
}
