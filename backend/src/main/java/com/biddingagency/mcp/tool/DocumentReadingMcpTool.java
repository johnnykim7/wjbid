package com.biddingagency.mcp.tool;

import com.biddingagency.domain.reading.service.DocumentReadingService;
import com.biddingagency.domain.reading.service.DocumentReadingService.SaveResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 고객 서류 읽기 결과 저장 (CR-169, BID-DOC-004) */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentReadingMcpTool {

    private final DocumentReadingService readingService;
    private final ObjectMapper objectMapper;

    public static final List<Map<String, Object>> TOOL_DEFINITIONS = List.of(
        Map.of(
            "name", "save_client_document_facts",
            "description", "고객 서류 읽기 결과를 저장합니다. results 원소의 index 로 targets 의 같은 순번 서류를 찾고, "
                + "사실에 sourceFile 이 있으면 그 경로(targetPath)의 서류로 보냅니다. 결과가 온 서류는 사실이 0개여도 읽음입니다. "
                + "targets 에 없는 경로와 이 입찰 건의 고객 서류가 아닌 readingId 는 버리고 ignored 로 셉니다.",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "bidRequestId", Map.of("type", "string", "description", "입찰 요청 UUID"),
                    "targets", Map.of("type", "array", "description",
                        "워크플로우 입력의 targets 그대로 [{readingId, targetPath}]"),
                    "results", Map.of("type", "array", "description",
                        "extract_facts 의 structured_data 배열. 원소마다 index 와 structured_data.facts")
                ),
                "required", List.of("bidRequestId", "targets", "results")
            )
        )
    );

    public String saveClientDocumentFacts(Map<String, Object> args) {
        UUID bidRequestId = UUID.fromString(String.valueOf(args.get("bidRequestId")));
        List<Map<String, Object>> targets = new ArrayList<>();
        for (Object o : list(args.get("targets"), "targets")) {
            Map<String, Object> m = map(o);
            if (m != null) targets.add(m);
        }
        List<Object> results = list(args.get("results"), "results");

        log.info("MCP save_client_document_facts: bidRequestId={}, targets={}, results={}",
                bidRequestId, targets.size(), results.size());
        SaveResult saved = readingService.saveFacts(bidRequestId, targets, results);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("saved", saved.saved());
        out.put("ignored", saved.ignored());
        return toJson(out);
    }

    /** Aimbase 변수 치환이 배열을 JSON 문자열로 보내는 경우가 있어 양쪽 받는다 */
    @SuppressWarnings("unchecked")
    private List<Object> list(Object value, String name) {
        if (value instanceof String s) {
            try {
                value = objectMapper.readValue(s, Object.class);
            } catch (Exception e) {
                throw new IllegalArgumentException(name + " 는 배열이어야 합니다");
            }
        }
        if (!(value instanceof List<?>)) {
            throw new IllegalArgumentException(name + " 는 배열이어야 합니다");
        }
        return (List<Object>) value;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        if (value instanceof String s) {
            try {
                value = objectMapper.readValue(s, Object.class);
            } catch (Exception e) {
                return null;
            }
        }
        return value instanceof Map ? (Map<String, Object>) value : null;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }
}
