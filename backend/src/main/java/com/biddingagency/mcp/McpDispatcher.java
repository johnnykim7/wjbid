package com.biddingagency.mcp;

import com.biddingagency.mcp.tool.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Stream;

/**
 * MCP 도구 디스패치 서비스 (CR-002)
 * McpServerController(HTTP)와 McpSseController(SSE)에서 공유.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpDispatcher {

    private final OpportunityMcpTool opportunityMcpTool;
    private final RequirementMcpTool requirementMcpTool;
    private final DocumentMcpTool documentMcpTool;
    private final DocumentTemplateMcpTool documentTemplateMcpTool;
    private final StateMcpTool stateMcpTool;
    private final OpportunityAnalysisMcpTool opportunityAnalysisMcpTool;
    private final PatternGuideMcpTool patternGuideMcpTool;
    private final ProposalMcpTool proposalMcpTool;

    /** 전체 도구 정의 목록 */
    private static final List<Map<String, Object>> ALL_TOOLS = Stream.of(
        OpportunityMcpTool.TOOL_DEFINITIONS,
        RequirementMcpTool.TOOL_DEFINITIONS,
        DocumentMcpTool.TOOL_DEFINITIONS,
        DocumentTemplateMcpTool.TOOL_DEFINITIONS,
        StateMcpTool.TOOL_DEFINITIONS,
        OpportunityAnalysisMcpTool.TOOL_DEFINITIONS,
        PatternGuideMcpTool.TOOL_DEFINITIONS,
        ProposalMcpTool.TOOL_DEFINITIONS
    ).flatMap(Collection::stream).toList();

    /**
     * JSON-RPC 2.0 요청 처리
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> handle(Map<String, Object> body) {
        String method = (String) body.get("method");
        Object id = body.get("id");

        log.debug("MCP request: method={}, id={}", method, id);

        return switch (method) {
            case "initialize"  -> handleInitialize(id);
            case "tools/list"  -> success(id, Map.of("tools", ALL_TOOLS));
            case "tools/call"  -> handleToolCall(id, body);
            default            -> {
                // JSON-RPC notification (notifications/initialized 등)은 id 가 없고 응답해서도 안 됨.
                // null 을 반환하면 호출측(McpSseController.message)이 SSE 전송을 스킵한다.
                if (method != null && method.startsWith("notifications/")) {
                    yield null;
                }
                yield error(id, -32601, "Method not found: " + method);
            }
        };
    }

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
            case "get_opportunity"              -> opportunityMcpTool.getOpportunity(args);
            case "search_opportunities"         -> opportunityMcpTool.searchOpportunities(args);
            case "get_opportunity_requirements" -> opportunityMcpTool.getOpportunityRequirements(args);
            case "save_requirements"            -> requirementMcpTool.saveRequirements(args);
            case "get_bid_request"              -> documentMcpTool.getBidRequest(args);
            case "save_document_version"        -> documentMcpTool.saveDocumentVersion(args);
            case "get_document_template"        -> documentTemplateMcpTool.getDocumentTemplate(args);
            case "transition_bid_state"         -> stateMcpTool.transitionBidState(args);
            case "get_opportunity_analysis"    -> opportunityAnalysisMcpTool.getOpportunityAnalysis(args);
            case "save_opportunity_analysis"   -> opportunityAnalysisMcpTool.saveOpportunityAnalysis(args);
            case "get_past_submissions"        -> opportunityAnalysisMcpTool.getPastSubmissions(args);
            case "get_reference_samples"       -> patternGuideMcpTool.getReferenceSamples(args);
            case "save_pattern_guide"          -> patternGuideMcpTool.savePatternGuide(args);
            // CR-028: 제안서 파이프라인 3단계
            case "get_proposal_design_input"   -> proposalMcpTool.getProposalDesignInput(args);
            case "save_proposal_structure"     -> proposalMcpTool.saveProposalStructure(args);
            case "get_section_context"         -> proposalMcpTool.getSectionContext(args);
            case "save_section_blocks"         -> proposalMcpTool.saveSectionBlocks(args);
            // CR-031: 충실성 검증
            case "get_section_verify_input"    -> proposalMcpTool.getSectionVerifyInput(args);
            case "get_notice_verify_input"     -> proposalMcpTool.getNoticeVerifyInput(args);
            case "save_verification_result"    -> proposalMcpTool.saveVerificationResult(args);
            default -> throw new IllegalArgumentException("알 수 없는 도구: " + toolName);
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
        // Map.of 는 null value 를 허용하지 않아 id=null(notification)일 때 NPE → LinkedHashMap 사용.
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("error", Map.of("code", code, "message", message));
        return response;
    }
}
