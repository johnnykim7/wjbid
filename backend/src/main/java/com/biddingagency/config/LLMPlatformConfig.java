package com.biddingagency.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for LLM Platform REST client
 */
@Slf4j
@Configuration
public class LLMPlatformConfig {

    @Value("${app.llm-platform.connect-timeout:5000}")
    private int connectTimeout;

    @Value("${app.llm-platform.read-timeout:300000}")
    private int readTimeout;

    @Bean
    public RestTemplate llmPlatformRestTemplate() {
        log.info("Initializing LLM Platform RestTemplate with connectTimeout={}ms, readTimeout={}ms",
            connectTimeout, readTimeout);

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);

        return new RestTemplate(factory);
    }
}
