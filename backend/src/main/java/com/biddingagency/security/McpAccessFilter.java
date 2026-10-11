package com.biddingagency.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * /mcp 로 들어오는 요청은 Aimbase 열쇠가 있을 때만 넘긴다 (CR-144, BIZ-061).
 * 내려받기는 이 플랫폼이 만든 서명 주소로도 넘긴다. 등록은 McpAccessConfig 가 /mcp 경로에만 건다.
 */
@Slf4j
@RequiredArgsConstructor
public class McpAccessFilter extends OncePerRequestFilter {

    private final McpAccessGuard guard;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (guard.keyMatches(request.getHeader(McpAccessGuard.KEY_HEADER)) || signedDownload(request)) {
            chain.doFilter(request, response);
            return;
        }
        log.warn("MCP 열쇠 확인 실패: {} {} from {}",
                request.getMethod(), request.getRequestURI(), request.getRemoteAddr());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    private boolean signedDownload(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return HttpMethod.GET.matches(request.getMethod())
                && guard.isDownloadPath(path)
                && guard.signatureValid(path,
                        request.getParameter(McpAccessGuard.EXPIRES_PARAM),
                        request.getParameter(McpAccessGuard.SIGNATURE_PARAM));
    }
}
