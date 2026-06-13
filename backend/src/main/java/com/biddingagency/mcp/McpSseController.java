package com.biddingagency.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * MCP SSE 전송 컨트롤러 (CR-002)
 *
 * Aimbase가 SSE로 MCP 서버에 연결하여 도구를 호출하는 엔드포인트.
 *
 * 프로토콜:
 *   1. 클라이언트: GET /mcp/sse → SSE 스트림 수신
 *   2. 서버: 'endpoint' 이벤트로 메시지 URL 전달
 *   3. 클라이언트: POST /mcp/message?sessionId=xxx → JSON-RPC 메시지 전송
 *   4. 서버: SSE 'message' 이벤트로 응답 전송
 */
@Slf4j
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
public class McpSseController {

    private final McpDispatcher mcpDispatcher;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, SseEmitter> sessions = new ConcurrentHashMap<>();

    /** Heartbeat 스케줄러 — 30초 간격으로 ping 전송하여 연결 유지 */
    private final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "mcp-sse-heartbeat");
        t.setDaemon(true);
        return t;
    });

    /**
     * SSE 연결 엔드포인트.
     * Aimbase가 이 엔드포인트로 연결하면 sessionId를 발급하고 메시지 URL을 전달.
     */
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect() {
        String sessionId = UUID.randomUUID().toString();
        // 타임아웃 없음 (0L) — heartbeat로 연결 유지
        SseEmitter emitter = new SseEmitter(0L);

        sessions.put(sessionId, emitter);
        log.info("MCP SSE 연결: sessionId={}, 활성 세션={}", sessionId, sessions.size());

        emitter.onCompletion(() -> {
            sessions.remove(sessionId);
            log.info("MCP SSE 연결 종료: sessionId={}", sessionId);
        });
        emitter.onTimeout(() -> {
            sessions.remove(sessionId);
            log.warn("MCP SSE 타임아웃: sessionId={}", sessionId);
        });
        emitter.onError(e -> {
            sessions.remove(sessionId);
            log.warn("MCP SSE 오류: sessionId={}, error={}", sessionId, e.getMessage());
        });

        // endpoint 이벤트 전송 — 클라이언트에게 메시지 URL 알려줌
        try {
            String messageUrl = "/api/mcp/message?sessionId=" + sessionId;
            emitter.send(SseEmitter.event()
                .name("endpoint")
                .data(messageUrl));
        } catch (IOException e) {
            log.error("MCP SSE endpoint 이벤트 전송 실패: sessionId={}", sessionId, e);
            sessions.remove(sessionId);
        }

        // Heartbeat 시작
        heartbeatScheduler.scheduleAtFixedRate(() -> sendHeartbeat(sessionId), 30, 30, TimeUnit.SECONDS);

        return emitter;
    }

    /**
     * SSE 메시지 수신 엔드포인트.
     * 클라이언트가 JSON-RPC 메시지를 POST로 전송하면, 처리 후 SSE로 응답.
     */
    @PostMapping(value = "/message", consumes = "application/json")
    public ResponseEntity<Void> message(
            @RequestParam String sessionId,
            @RequestBody Map<String, Object> body) {

        SseEmitter emitter = sessions.get(sessionId);
        if (emitter == null) {
            log.warn("MCP SSE 세션 없음: sessionId={}", sessionId);
            return ResponseEntity.notFound().build();
        }

        log.debug("MCP SSE 메시지 수신: sessionId={}, method={}", sessionId, body.get("method"));

        try {
            Map<String, Object> response = mcpDispatcher.handle(body);

            // notification(notifications/initialized 등)은 응답이 없다(null) → SSE 전송 스킵, 202 반환.
            if (response == null) {
                return ResponseEntity.accepted().build();
            }

            String json = objectMapper.writeValueAsString(response);

            emitter.send(SseEmitter.event()
                .name("message")
                .data(json));

            return ResponseEntity.ok().build();
        } catch (IOException e) {
            log.error("MCP SSE 응답 전송 실패: sessionId={}", sessionId, e);
            sessions.remove(sessionId);
            return ResponseEntity.internalServerError().build();
        }
    }

    private void sendHeartbeat(String sessionId) {
        SseEmitter emitter = sessions.get(sessionId);
        if (emitter == null) return;

        try {
            emitter.send(SseEmitter.event().comment("ping"));
        } catch (IOException e) {
            sessions.remove(sessionId);
            log.debug("MCP SSE heartbeat 실패, 세션 제거: sessionId={}", sessionId);
        }
    }
}
