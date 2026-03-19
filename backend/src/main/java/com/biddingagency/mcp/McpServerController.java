package com.biddingagency.mcp;

import com.biddingagency.mcp.tool.DocumentMcpTool;
import com.biddingagency.mcp.tool.DocumentTemplateMcpTool;
import com.biddingagency.mcp.tool.OpportunityMcpTool;
import com.biddingagency.mcp.tool.RequirementMcpTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Stream;

/**
 * MCP (Model Context Protocol) 서버 컨트롤러
 *
 * LLM Platform이 워크플로우 실행 중 이 엔드포인트를 통해 비즈니스 데이터에 접근합니다.
 * JSON-RPC 2.0 over HTTP 를 구현합니다.
 *
 * 등록 방법:
 *   POST http://llm-platform:9000/api/v1/mcp-servers
 *   { "id": "bidding-agency-mcp", "url": "http://bidding-agency:8088/mcp", "type": "HTTP" }
 */
@Slf4j
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
public class McpServerController {

    private final OpportunityMcpTool opportunityMcpTool;
    private final RequirementMcpTool requirementMcpTool;
    private final DocumentMcpTool documentMcpTool;
    private final DocumentTemplateMcpTool documentTemplateMcpTool;

    /** 전체 도구 정의 목록 (initialize + tools/list에서 사용) */
    private static final List<Map<String, Object>> ALL_TOOLS = Stream.of(
        OpportunityMcpTool.TOOL_DEFINITIONS,
        RequirementMcpTool.TOOL_DEFINITIONS,
        DocumentMcpTool.TOOL_DEFINITIONS,
        DocumentTemplateMcpTool.TOOL_DEFINITIONS
    ).flatMap(Collection::stream).toList();

    // ─── JSON-RPC 2.0 핸들러 ─────────────────────────────────────────────

    @PostMapping(consumes = "application/json", produces = "application/json")
    public Map<String, Object> handle(@RequestBody Map<String, Object> body) {
        String method = (String) body.get("method");
        Object id = body.get("id");

        log.debug("MCP request: method={}, id={}", method, id);

        return switch (method) {
            case "initialize"  -> handleInitialize(id);
            case "tools/list"  -> success(id, Map.of("tools", ALL_TOOLS));
            case "tools/call"  -> handleToolCall(id, body);
            default            -> error(id, -32601, "Method not found: " + method);
        };
    }

    // ─── 메서드 구현 ──────────────────────────────────────────────────────

    private Map<String, Object> handleInitialize(Object id) {
        return success(id, Map.of(
            "protocolVersion", "2024-11-05",
            "capabilities", Map.of("tools", Map.of()),
            "serverInfo", Map.of(
                "name", "bidding-agency-mcp",
                "version", "1.0.0",
                "description", "Bidding Agency Platform — 공고/요구사항/문서 데이터 접근"
            )
        ));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> handleToolCall(Object id, Map<String, Object> body) {
        Map<String, Object> params = (Map<String, Object>) body.get("params");
        String toolName = (String) params.get("name");
        Map<String, Object> args = params.containsKey("arguments")
            ? (Map<String, Object>) params.get("arguments")
            : Map.of();

        log.info("MCP tools/call: tool={}, args_keys={}", toolName, args.keySet());

        try {
            String result = dispatch(toolName, args);
            return success(id, Map.of(
                "content", List.of(Map.of("type", "text", "text", result))
            ));
        } catch (IllegalArgumentException e) {
            log.warn("MCP tool 파라미터 오류: tool={}, error={}", toolName, e.getMessage());
            return success(id, Map.of(
                "content", List.of(Map.of("type", "text", "text", "오류: " + e.getMessage())),
                "isError", true
            ));
        } catch (Exception e) {
            log.error("MCP tool 실행 오류: tool={}", toolName, e);
            return success(id, Map.of(
                "content", List.of(Map.of("type", "text", "text", "서버 오류: " + e.getMessage())),
                "isError", true
            ));
        }
    }

    private String dispatch(String toolName, Map<String, Object> args) {
        return switch (toolName) {
            // Opportunity tools
            case "get_opportunity"             -> opportunityMcpTool.getOpportunity(args);
            case "search_opportunities"        -> opportunityMcpTool.searchOpportunities(args);
            case "get_opportunity_requirements"-> opportunityMcpTool.getOpportunityRequirements(args);
            // Requirement tools
            case "save_requirements"           -> requirementMcpTool.saveRequirements(args);
            // Document tools
            case "get_bid_request"             -> documentMcpTool.getBidRequest(args);
            case "save_document_version"       -> documentMcpTool.saveDocumentVersion(args);
            // Template tools
            case "get_document_template"       -> documentTemplateMcpTool.getDocumentTemplate(args);
            default -> throw new IllegalArgumentException("알 수 없는 도구: " + toolName
                + ". 사용 가능: get_opportunity, search_opportunities, get_opportunity_requirements, "
                + "save_requirements, get_bid_request, save_document_version, get_document_template");
        };
    }

    // ─── JSON-RPC 응답 헬퍼 ──────────────────────────────────────────────

    private Map<String, Object> success(Object id, Object result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("result", result);
        return response;
    }

    private Map<String, Object> error(Object id, int code, String message) {
        return Map.of(
            "jsonrpc", "2.0",
            "id", id,
            "error", Map.of("code", code, "message", message)
        );
    }
}
