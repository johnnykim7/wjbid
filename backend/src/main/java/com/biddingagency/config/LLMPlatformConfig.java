package com.biddingagency.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for Aimbase REST client (CR-002)
 * LLM Platform → Aimbase 전환. X-API-Key 인증 헤더 자동 추가.
 */
@Slf4j
@Configuration
public class LLMPlatformConfig {

    @Value("${app.aimbase.connect-timeout:5000}")
    private int connectTimeout;

    @Value("${app.aimbase.read-timeout:300000}")
    private int readTimeout;

    @Value("${app.aimbase.api-key:}")
    private String apiKey;

    @Value("${app.aimbase.tenant-id:bidding_system}")
    private String tenantId;

    @Bean
    public RestTemplate llmPlatformRestTemplate() {
        log.info("Initializing Aimbase RestTemplate with connectTimeout={}ms, readTimeout={}ms",
            connectTimeout, readTimeout);

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);

        RestTemplate restTemplate = new RestTemplate(factory);

        // Aimbase 인증 인터셉터: X-API-Key + X-Tenant-Id
        restTemplate.getInterceptors().add((request, body, execution) -> {
            if (apiKey != null && !apiKey.isEmpty()) {
                request.getHeaders().set("X-API-Key", apiKey);
            }
            if (tenantId != null && !tenantId.isEmpty()) {
                request.getHeaders().set("X-Tenant-Id", tenantId);
            }
            request.getHeaders().set("Content-Type", "application/json");
            return execution.execute(request, body);
        });
        log.info("Aimbase interceptor registered (tenant={}, key prefix={}...)",
            tenantId, apiKey != null && apiKey.length() > 10 ? apiKey.substring(0, 10) : "none");

        return restTemplate;
    }
}
