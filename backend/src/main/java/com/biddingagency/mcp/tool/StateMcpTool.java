package com.biddingagency.mcp.tool;

import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.entity.BidRequestState;
import com.biddingagency.domain.bid.service.BidFSMService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class StateMcpTool {

    private final BidFSMService fsmService;
    private final ObjectMapper objectMapper;

    public static final List<Map<String, Object>> TOOL_DEFINITIONS = List.of(
        Map.of(
            "name", "transition_bid_state",
            "description", "입찰 요청의 상태를 전이합니다. FSM 화이트리스트 규칙에 따라 허용된 전이만 실행됩니다.",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "bidRequestId", Map.of("type", "string", "description", "입찰 요청 UUID"),
                    "targetState", Map.of("type", "string", "description",
                        "전이할 상태 (CREATED, DOCS_PENDING, DOCS_RECEIVED, ANALYZING, GENERATING, REVIEW, CONFIRMED, SUBMITTED, CLOSED)"),
                    "notes", Map.of("type", "string", "description", "전이 사유 (선택)")
                ),
                "required", List.of("bidRequestId", "targetState")
            )
        )
    );

    public String transitionBidState(Map<String, Object> args) {
        UUID bidRequestId = UUID.fromString((String) args.get("bidRequestId"));
        BidRequestState targetState = BidRequestState.valueOf((String) args.get("targetState"));
        String notes = args.containsKey("notes") ? (String) args.get("notes") : "Aimbase 자동 전이";

        log.info("MCP transition_bid_state: bidRequestId={}, targetState={}", bidRequestId, targetState);

        // System actor (UUID 0)
        UUID systemActor = new UUID(0L, 0L);
        BidRequest bidRequest = fsmService.transition(bidRequestId, targetState, systemActor, "Aimbase", notes);

        try {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("bidRequestId", bidRequest.getId().toString());
            result.put("previousState", bidRequest.getStateHistory().isEmpty() ? null :
                bidRequest.getStateHistory().get(bidRequest.getStateHistory().size() - 1).getFromState().name());
            result.put("currentState", bidRequest.getState().name());
            result.put("success", true);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
}
