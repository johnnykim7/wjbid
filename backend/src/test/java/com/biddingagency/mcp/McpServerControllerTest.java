package com.biddingagency.mcp;

import com.biddingagency.mcp.tool.DocumentMcpTool;
import com.biddingagency.mcp.tool.DocumentTemplateMcpTool;
import com.biddingagency.mcp.tool.OpportunityMcpTool;
import com.biddingagency.mcp.tool.RequirementMcpTool;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * TC-MCP-001 ~ TC-MCP-005: McpServerController 테스트
 */
@ExtendWith(MockitoExtension.class)
class McpServerControllerTest {

    @Mock
    private OpportunityMcpTool opportunityMcpTool;
    @Mock
    private RequirementMcpTool requirementMcpTool;
    @Mock
    private DocumentMcpTool documentMcpTool;
    @Mock
    private DocumentTemplateMcpTool documentTemplateMcpTool;

    @InjectMocks
    private McpServerController mcpServerController;

    // TC-MCP-001: initialize 메서드
    @Test
    @DisplayName("initialize요청_서버정보와capabilities반환")
    @SuppressWarnings("unchecked")
    void initialize_서버정보반환() {
        // given
        Map<String, Object> request = Map.of(
                "jsonrpc", "2.0",
                "id", 1,
                "method", "initialize"
        );

        // when
        Map<String, Object> response = mcpServerController.handle(request);

        // then
        assertThat(response.get("jsonrpc")).isEqualTo("2.0");
        assertThat(response.get("id")).isEqualTo(1);
        Map<String, Object> result = (Map<String, Object>) response.get("result");
        assertThat(result).containsKey("protocolVersion");
        assertThat(result).containsKey("capabilities");
        assertThat(result).containsKey("serverInfo");
        Map<String, Object> serverInfo = (Map<String, Object>) result.get("serverInfo");
        assertThat(serverInfo.get("name")).isEqualTo("bidding-agency-mcp");
    }

    // TC-MCP-002: tools/list 메서드
    @Test
    @DisplayName("toolsList요청_등록된전체Tool목록반환")
    @SuppressWarnings("unchecked")
    void toolsList_전체도구목록반환() {
        // given
        Map<String, Object> request = Map.of(
                "jsonrpc", "2.0",
                "id", 2,
                "method", "tools/list"
        );

        // when
        Map<String, Object> response = mcpServerController.handle(request);

        // then
        Map<String, Object> result = (Map<String, Object>) response.get("result");
        assertThat(result).containsKey("tools");
        java.util.List<?> tools = (java.util.List<?>) result.get("tools");
        assertThat(tools).isNotEmpty();
        // 7개 도구: get_opportunity, search_opportunities, get_opportunity_requirements,
        //          save_requirements, get_bid_request, save_document_version, get_document_template
        assertThat(tools).hasSize(7);
    }

    // TC-MCP-003: tools/call 유효 도구 (get_opportunity)
    @Test
    @DisplayName("toolsCall_get_opportunity_공고정보반환")
    @SuppressWarnings("unchecked")
    void toolsCall_getOpportunity_결과반환() {
        // given
        String mockResult = "{\"id\":\"test-id\",\"title\":\"Test\"}";
        given(opportunityMcpTool.getOpportunity(any())).willReturn(mockResult);

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", "get_opportunity");
        params.put("arguments", Map.of("opportunityId", "550e8400-e29b-41d4-a716-446655440000"));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", 3);
        request.put("method", "tools/call");
        request.put("params", params);

        // when
        Map<String, Object> response = mcpServerController.handle(request);

        // then
        assertThat(response.get("jsonrpc")).isEqualTo("2.0");
        Map<String, Object> result = (Map<String, Object>) response.get("result");
        assertThat(result).containsKey("content");
        java.util.List<?> content = (java.util.List<?>) result.get("content");
        assertThat(content).isNotEmpty();
    }

    // TC-MCP-004: tools/call 무효 도구
    @Test
    @DisplayName("toolsCall_존재하지않는Tool_에러응답")
    @SuppressWarnings("unchecked")
    void toolsCall_무효도구_에러() {
        // given
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", "nonexistent_tool");
        params.put("arguments", Map.of());

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", 4);
        request.put("method", "tools/call");
        request.put("params", params);

        // when
        Map<String, Object> response = mcpServerController.handle(request);

        // then
        Map<String, Object> result = (Map<String, Object>) response.get("result");
        assertThat(result.get("isError")).isEqualTo(true);
    }

    // TC-MCP-005: 무상태 검증 (동일 요청 2회 → 동일 결과)
    @Test
    @DisplayName("동일요청2회_동일결과반환_무상태_BIZ013")
    void 동일요청_2회_동일결과() {
        // given
        String mockResult = "{\"id\":\"test\",\"title\":\"Same\"}";
        given(opportunityMcpTool.getOpportunity(any())).willReturn(mockResult);

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", "get_opportunity");
        params.put("arguments", Map.of("opportunityId", "550e8400-e29b-41d4-a716-446655440000"));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", 5);
        request.put("method", "tools/call");
        request.put("params", params);

        // when
        Map<String, Object> response1 = mcpServerController.handle(request);
        Map<String, Object> response2 = mcpServerController.handle(request);

        // then
        assertThat(response1.get("result")).isEqualTo(response2.get("result"));
    }

    // 존재하지 않는 메서드
    @Test
    @DisplayName("미지원메서드_호출_에러응답")
    @SuppressWarnings("unchecked")
    void 미지원메서드_에러() {
        // given
        Map<String, Object> request = Map.of(
                "jsonrpc", "2.0",
                "id", 6,
                "method", "unknown/method"
        );

        // when
        Map<String, Object> response = mcpServerController.handle(request);

        // then
        assertThat(response).containsKey("error");
        Map<String, Object> error = (Map<String, Object>) response.get("error");
        assertThat(error.get("code")).isEqualTo(-32601);
    }
}
