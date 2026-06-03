package com.biddingagency.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * CR-033: Aimbase Chat Widget BFF 토큰 프록시.
 *
 * 브라우저(admin-console)에 Aimbase API Key 를 절대 내려보내지 않는다(3-Tier 보안).
 * 관리자 인증을 통과한 요청만, 우리 BE 가 보관한 API Key 로 Aimbase 에서
 * 30분 TTL 단기 위젯 토큰을 대리 발급받아 브라우저에 전달한다.
 *
 * 가이드: ~/Documents/GitHub/bp-platform/aimbase/docs/guides/embed-chat-widget.md §2-1
 * llmPlatformRestTemplate 빈이 X-API-Key / X-Tenant-Id 를 자동 첨부한다(LLMPlatformConfig).
 */
@Slf4j
@RestController
@RequestMapping("/admin/aimbase")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Aimbase Widget", description = "공고문 교정 채팅 위젯 토큰 프록시")
public class AimbaseWidgetTokenController {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestTemplate llmPlatformRestTemplate;

    @Value("${app.aimbase.base-url}")
    private String aimbaseBaseUrl;

    /** 브라우저 위젯의 origin. Aimbase widget.allowed-origins 화이트리스트와 정확히 일치해야 한다. */
    @Value("${app.aimbase.widget.allowed-origin:http://woojinusbid.com}")
    private String allowedOrigin;

    @Value("${app.aimbase.widget.token-ttl-seconds:1800}")
    private int tokenTtlSeconds;

    @PostMapping("/widget-token")
    @Operation(summary = "위젯 단기 토큰 발급 (BFF 프록시)",
            description = "관리자 인증 통과 시 Aimbase 위젯 토큰(30분 TTL)을 대리 발급한다.")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> issueWidgetToken() {
        Map<String, Object> body = Map.of(
                "origin", allowedOrigin,
                "scopes", List.of("chat:stream", "workflow:subscribe"),
                "ttl_seconds", tokenTtlSeconds
        );
        try {
            ResponseEntity<Map<String, Object>> resp = llmPlatformRestTemplate.exchange(
                    aimbaseBaseUrl + "/api/v1/sessions/issue-widget-token",
                    HttpMethod.POST, new HttpEntity<>(body), MAP_TYPE);

            Map<String, Object> wrapper = resp.getBody();
            Object data = wrapper != null ? wrapper.get("data") : null;
            if (data instanceof Map<?, ?> tokenData) {
                return ResponseEntity.ok((Map<String, Object>) tokenData); // {token, expires_at, refresh_after, scopes}
            }
            log.error("Aimbase widget token: unexpected body={}", wrapper);
            return ResponseEntity.status(502).body(Map.of("error", "토큰 발급 응답이 올바르지 않습니다."));
        } catch (RestClientException e) {
            log.error("Aimbase widget token 발급 실패", e);
            return ResponseEntity.status(502).body(Map.of("error", "Aimbase 토큰 발급에 실패했습니다."));
        }
    }
}
