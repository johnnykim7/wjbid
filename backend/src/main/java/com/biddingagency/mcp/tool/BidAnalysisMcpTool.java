package com.biddingagency.mcp.tool;

import com.biddingagency.domain.analysis.service.AwardHistoryService;
import com.biddingagency.domain.analysis.service.AwardHistoryService.Criteria;
import com.biddingagency.domain.analysis.service.BidAnalysisBodyValidator.Part;
import com.biddingagency.domain.analysis.service.BidAnalysisInputService;
import com.biddingagency.domain.analysis.service.BidAnalysisInputService.Step;
import com.biddingagency.domain.analysis.service.BidAnalysisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * MCP 도구: 입찰 분석 (CR-170, BID-DOC-006).
 *
 * - get_bid_analysis_input : 단계마다 필요한 자료 조회
 * - search_award_history   : 미 연방 계약과 낙찰 공고 찾기(읽기만)
 * - save_bid_analysis      : 단계 산출물 저장. DECISION 이면 서버가 Pwin 과 판정을 계산한다
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BidAnalysisMcpTool {

    private final BidAnalysisInputService inputService;
    private final BidAnalysisService analysisService;
    private final AwardHistoryService awardHistoryService;
    private final ObjectMapper objectMapper;

    public static final List<Map<String, Object>> TOOL_DEFINITIONS = List.of(
        Map.of(
            "name", "get_bid_analysis_input",
            "description", "입찰 분석 단계의 입력 자료를 조회합니다. step REPORT: 공고 메타, 공고 분석, 같은 공고번호의 다른 공고, "
                + "같은 기관 최근 공고 20건, 고객 회사 칸, 고객 서류 사실, 참가 자격 답. step CAPTURE_PLAN: 공고 메타, 공고 분석 요약과 "
                + "평가 항목 구조, 고객 회사 칸, 고객 서류 사실, 이 회차의 종합 분석 리포트. step DECISION: 공고 메타, 종합 분석 리포트의 "
                + "자격, 경쟁사, 공정성, 발주 이력과 캡처플랜, 고객 회사 칸, 매트릭스 줄 수와 고객 제출 줄, Pwin 정책 가중치.",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "bidRequestId", Map.of("type", "string", "description", "입찰 요청 UUID"),
                    "analysisId", Map.of("type", "string", "description", "입찰 분석 회차 UUID(워크플로우 입력)"),
                    "step", Map.of("type", "string", "enum", List.of("REPORT", "CAPTURE_PLAN", "DECISION"))
                ),
                "required", List.of("bidRequestId", "analysisId", "step")
            )
        ),
        Map.of(
            "name", "search_award_history",
            "description", "미 연방 계약(FEDERAL_CONTRACT)과 모은 낙찰 공고(SAM_AWARD_NOTICE)를 같이 찾습니다. 읽기만 합니다. "
                + "줄마다 source, 계약번호나 공고번호(number), 제목이나 설명, 수주사 이름과 UEI, 금액, 시작일과 끝날, NAICS, PSC, "
                + "계약 부서, 계약 구분을 줍니다. 날짜 늦은 순이고 limit 은 50 까지(초기값 20). 조건이 하나도 없으면 거부합니다. "
                + "발주 이력은 NAICS, PSC 로 먼저 찾고 keyword 와 solicitationNumber 로 넓힙니다. 경쟁사 실적은 recipientName 으로 찾습니다.",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "naicsCodes", Map.of("type", "array", "items", Map.of("type", "string")),
                    "pscCodes", Map.of("type", "array", "items", Map.of("type", "string")),
                    "recipientName", Map.of("type", "string", "description", "수주사 이름 일부"),
                    "keyword", Map.of("type", "string", "description", "계약 설명이나 낙찰 공고 제목에 든 말"),
                    "solicitationNumber", Map.of("type", "string"),
                    "fromDate", Map.of("type", "string", "description", "yyyy-MM-dd"),
                    "toDate", Map.of("type", "string", "description", "yyyy-MM-dd"),
                    "limit", Map.of("type", "integer", "description", "1~50, 초기값 20")
                )
            )
        ),
        Map.of(
            "name", "save_bid_analysis",
            "description", "입찰 분석 단계 산출물을 저장합니다. 회차가 분석 중일 때만 저장하고, part 는 REPORT, CAPTURE_PLAN, DECISION "
                + "순서이며 앞 단계가 없으면 거부합니다. 본문 꼴이 틀리면 틀린 곳의 경로(예: eligibility[0].evidence.confidence)를 "
                + "응답하고 거부하니 고쳐 다시 저장하세요. questions 는 앞 단계 것 뒤에 이어 붙입니다. DECISION 을 저장하면 서버가 "
                + "Pwin, 판정, 강점과 약점, 핵심이슈 순위를 계산하고 분석 완료로 바꿉니다. Pwin 과 판정은 본문에 쓰지 않습니다.",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "bidRequestId", Map.of("type", "string", "description", "입찰 요청 UUID"),
                    "analysisId", Map.of("type", "string", "description", "입찰 분석 회차 UUID"),
                    "part", Map.of("type", "string", "enum", List.of("REPORT", "CAPTURE_PLAN", "DECISION")),
                    "body", Map.of("type", "object", "description", "그 단계 산출물(워크플로우 지시문의 꼴)"),
                    "questions", Map.of("type", "array", "description",
                        "확인 필요 [{target CUSTOMER|ADMIN, question, reason, field}]")
                ),
                "required", List.of("bidRequestId", "analysisId", "part", "body")
            )
        )
    );

    public String getBidAnalysisInput(Map<String, Object> args) {
        UUID bidRequestId = uuid(args.get("bidRequestId"), "bidRequestId");
        UUID analysisId = uuid(args.get("analysisId"), "analysisId");
        Step step = enumOf(Step.class, args.get("step"), "step");
        log.info("MCP get_bid_analysis_input: bidRequestId={}, analysisId={}, step={}", bidRequestId, analysisId, step);
        return toJson(inputService.input(bidRequestId, analysisId, step));
    }

    public String searchAwardHistory(Map<String, Object> args) {
        Criteria criteria = new Criteria(
                strings(args.get("naicsCodes"), "naicsCodes"),
                strings(args.get("pscCodes"), "pscCodes"),
                text(args.get("recipientName")),
                text(args.get("keyword")),
                text(args.get("solicitationNumber")),
                date(args.get("fromDate"), "fromDate"),
                date(args.get("toDate"), "toDate"),
                integer(args.get("limit")));
        List<Map<String, Object>> rows = awardHistoryService.search(criteria);
        log.info("MCP search_award_history: {}줄", rows.size());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("count", rows.size());
        out.put("rows", rows);
        return toJson(out);
    }

    public String saveBidAnalysis(Map<String, Object> args) {
        UUID bidRequestId = uuid(args.get("bidRequestId"), "bidRequestId");
        UUID analysisId = uuid(args.get("analysisId"), "analysisId");
        Part part = enumOf(Part.class, args.get("part"), "part");
        Map<String, Object> body = map(args.get("body"));
        if (body == null) {
            throw new IllegalArgumentException("body: 객체여야 합니다");
        }
        List<Object> questions = args.get("questions") == null ? List.of() : list(args.get("questions"), "questions");
        log.info("MCP save_bid_analysis: bidRequestId={}, analysisId={}, part={}, questions={}",
                bidRequestId, analysisId, part, questions.size());
        return toJson(analysisService.save(bidRequestId, analysisId, part, body, questions));
    }

    // ─── 인자 읽기 ───────────────────────────────────────────────────────

    private static UUID uuid(Object raw, String name) {
        if (raw == null) throw new IllegalArgumentException(name + " 가 필요합니다");
        try {
            return UUID.fromString(raw.toString().trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(name + " 는 UUID 여야 합니다");
        }
    }

    private static <E extends Enum<E>> E enumOf(Class<E> type, Object raw, String name) {
        if (raw == null) throw new IllegalArgumentException(name + " 가 필요합니다");
        try {
            return Enum.valueOf(type, raw.toString().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(name + " 값이 틀렸습니다: " + raw);
        }
    }

    private static String text(Object raw) {
        if (raw == null) return null;
        String s = raw.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private static Integer integer(Object raw) {
        if (raw == null) return null;
        if (raw instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(raw.toString().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("limit 는 정수여야 합니다");
        }
    }

    private static LocalDate date(Object raw, String name) {
        String s = text(raw);
        if (s == null) return null;
        try {
            return LocalDate.parse(s);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(name + " 는 yyyy-MM-dd 여야 합니다");
        }
    }

    private List<String> strings(Object raw, String name) {
        if (raw == null) return List.of();
        List<String> out = new ArrayList<>();
        for (Object o : list(raw, name)) {
            String s = text(o);
            if (s != null) out.add(s);
        }
        return out;
    }

    /** Aimbase 변수 치환이 배열과 객체를 JSON 문자열로 보내는 경우가 있어 양쪽 받는다 */
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

    /** 입력 자료가 앱 도구 응답 한도(CR-169 2.11)에 들게 들여쓰기 없이 쓴다 */
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }
}
