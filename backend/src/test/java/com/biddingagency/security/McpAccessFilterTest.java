package com.biddingagency.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TC-MCPKEY-001 ~ 010: Aimbase 통로 열쇠와 내려받기 서명 (CR-144, BIZ-061)
 */
class McpAccessFilterTest {

    private static final String KEY = "test-mcp-key";
    private static final String BASE = "http://test:8183/api";
    private static final Instant NOW = Instant.parse("2026-10-11T03:30:00Z");
    private static final Duration TTL = Duration.ofHours(1);

    private static McpAccessGuard guardAt(String key, Instant now) {
        return new McpAccessGuard(key, TTL, BASE, Clock.fixed(now, ZoneId.of("Asia/Seoul")));
    }

    private static MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/api" + path);
        request.setContextPath("/api");
        return request;
    }

    /** 서명 주소를 받은 그대로 요청으로 옮긴다(Aimbase 가 주소를 그대로 부르는 것과 같다). */
    private static MockHttpServletRequest requestFor(String signedUrl) {
        URI uri = URI.create(signedUrl);
        MockHttpServletRequest request = request("GET", uri.getPath().substring("/api".length()));
        for (String pair : uri.getQuery().split("&")) {
            String[] kv = pair.split("=", 2);
            request.addParameter(kv[0], kv[1]);
        }
        return request;
    }

    private static MockFilterChain run(McpAccessGuard guard, MockHttpServletRequest request,
                                       MockHttpServletResponse response) throws Exception {
        MockFilterChain chain = new MockFilterChain();
        new McpAccessFilter(guard).doFilter(request, response, chain);
        return chain;
    }

    @Test
    @DisplayName("TC-MCPKEY-001 열쇠없음_막힘")
    void 열쇠없음_막힘() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = run(guardAt(KEY, NOW), request("POST", "/mcp"), response);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("TC-MCPKEY-002 틀린열쇠_막힘")
    void 틀린열쇠_막힘() throws Exception {
        MockHttpServletRequest request = request("GET", "/mcp/sse");
        request.addHeader(McpAccessGuard.KEY_HEADER, "wrong-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = run(guardAt(KEY, NOW), request, response);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("TC-MCPKEY-003 맞는열쇠_통과")
    void 맞는열쇠_통과() throws Exception {
        MockHttpServletRequest request = request("POST", "/mcp/message");
        request.addHeader(McpAccessGuard.KEY_HEADER, KEY);
        MockFilterChain chain = run(guardAt(KEY, NOW), request, new MockHttpServletResponse());

        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    @DisplayName("TC-MCPKEY-004 서버열쇠빈값_막힘")
    void 서버열쇠빈값_막힘() throws Exception {
        MockHttpServletRequest request = request("POST", "/mcp");
        request.addHeader(McpAccessGuard.KEY_HEADER, "");
        MockHttpServletResponse response = new MockHttpServletResponse();
        run(guardAt("", NOW), request, response);

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("TC-MCPKEY-005 서명주소_기한안_통과")
    void 서명주소_통과() throws Exception {
        String url = guardAt(KEY, NOW).opportunityAttachmentUrl(UUID.randomUUID());
        MockHttpServletRequest request = requestFor(url);
        MockFilterChain chain = run(guardAt(KEY, NOW.plus(TTL)), request, new MockHttpServletResponse());

        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    @DisplayName("TC-MCPKEY-006 서명없음_막힘")
    void 서명없음_막힘() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        run(guardAt(KEY, NOW),
                request("GET", "/mcp/opportunity-attachments/" + UUID.randomUUID() + "/download"), response);

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("TC-MCPKEY-007 기한지난서명_막힘")
    void 기한지난서명_막힘() throws Exception {
        String url = guardAt(KEY, NOW).opportunityAttachmentUrl(UUID.randomUUID());
        MockHttpServletResponse response = new MockHttpServletResponse();
        run(guardAt(KEY, NOW.plus(TTL).plusSeconds(1)), requestFor(url), response);

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("TC-MCPKEY-008 다른파일에서명재사용_막힘")
    void 다른파일에서명재사용_막힘() throws Exception {
        UUID other = UUID.randomUUID();
        MockHttpServletRequest signed = requestFor(guardAt(KEY, NOW).rfpFileUrl(UUID.randomUUID()));
        MockHttpServletRequest request = request("GET", "/mcp/rfp-files/" + other + "/download");
        request.addParameter(McpAccessGuard.EXPIRES_PARAM, signed.getParameter(McpAccessGuard.EXPIRES_PARAM));
        request.addParameter(McpAccessGuard.SIGNATURE_PARAM, signed.getParameter(McpAccessGuard.SIGNATURE_PARAM));
        MockHttpServletResponse response = new MockHttpServletResponse();
        run(guardAt(KEY, NOW), request, response);

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("TC-MCPKEY-009 머리글열쇠로_내려받기_통과")
    void 머리글열쇠로_내려받기() throws Exception {
        MockHttpServletRequest request = request("GET", "/mcp/rfp-files/" + UUID.randomUUID() + "/download");
        request.addHeader(McpAccessGuard.KEY_HEADER, KEY);
        MockFilterChain chain = run(guardAt(KEY, NOW), request, new MockHttpServletResponse());

        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    @DisplayName("TC-MCPKEY-010 서명주소꼴_만료는지금더하기기한")
    void 서명주소꼴() {
        UUID id = UUID.randomUUID();
        String url = guardAt(KEY, NOW).opportunityAttachmentUrl(id);

        long expires = NOW.plus(TTL).getEpochSecond();
        assertThat(url).matches("\\Q" + BASE + "/mcp/opportunity-attachments/" + id + "/download?exp="
                + expires + "&sig=\\E[A-Za-z0-9_-]{43}");
    }
}
