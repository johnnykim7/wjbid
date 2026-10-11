package com.biddingagency.config;

import com.biddingagency.security.McpAccessFilter;
import com.biddingagency.security.McpAccessGuard;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Aimbase 열쇠 확인을 /mcp 경로에만 건다 (CR-144).
 * 보안 설정은 /mcp 를 로그인 없이 열어 두고, 열쇠 확인은 이 거름망이 한다.
 */
@Configuration
public class McpAccessConfig {

    @Bean
    public FilterRegistrationBean<McpAccessFilter> mcpAccessFilter(McpAccessGuard guard) {
        FilterRegistrationBean<McpAccessFilter> registration = new FilterRegistrationBean<>(new McpAccessFilter(guard));
        registration.addUrlPatterns("/mcp", "/mcp/*");
        return registration;
    }
}
