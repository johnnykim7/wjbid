package com.biddingagency.config;

import com.biddingagency.common.FlowIdContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * FlowGuard X-Flow-Id 헤더를 수신하여 MDC + ThreadLocal에 전파하는 필터.
 * X-Flow-Id가 없으면 무시하고 정상 처리한다 (거부 금지).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class FlowIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Flow-Id";
    private static final String MDC_KEY = "flowId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String flowId = request.getHeader(HEADER_NAME);
        try {
            if (flowId != null && !flowId.isBlank()) {
                FlowIdContext.set(flowId);
                MDC.put(MDC_KEY, flowId);
            }
            filterChain.doFilter(request, response);
        } finally {
            FlowIdContext.clear();
            MDC.remove(MDC_KEY);
        }
    }
}
