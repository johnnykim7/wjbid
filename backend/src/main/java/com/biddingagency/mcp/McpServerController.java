package com.biddingagency.mcp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * MCP (Model Context Protocol) 서버 컨트롤러 — HTTP POST 전송
 *
 * JSON-RPC 2.0 over HTTP. 실제 로직은 McpDispatcher에 위임 (CR-002).
 */
@Slf4j
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
public class McpServerController {

    private final McpDispatcher mcpDispatcher;

    @PostMapping(consumes = "application/json", produces = "application/json")
    public Map<String, Object> handle(@RequestBody Map<String, Object> body) {
        return mcpDispatcher.handle(body);
    }
}
