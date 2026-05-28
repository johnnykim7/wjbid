package com.biddingagency.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * bp-notification 연동용 RestTemplate 설정 (CR-005/006)
 *
 * 알림 발송 인프라를 Gmail SMTP에서 외부 bp-notification 서비스로 전환.
 * X-API-Key 인증 헤더를 모든 요청에 자동 추가한다.
 */
@Slf4j
@Configuration
public class BpNotificationConfig {

    @Value("${app.bp-notification.connect-timeout:5000}")
    private int connectTimeout;

    @Value("${app.bp-notification.read-timeout:10000}")
    private int readTimeout;

    @Value("${app.bp-notification.api-key:}")
    private String apiKey;

    @Bean
    public RestTemplate bpNotificationRestTemplate() {
        log.info("Initializing bp-notification RestTemplate (connectTimeout={}ms, readTimeout={}ms)",
                connectTimeout, readTimeout);

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);

        RestTemplate restTemplate = new RestTemplate(factory);

        restTemplate.getInterceptors().add((request, body, execution) -> {
            if (apiKey != null && !apiKey.isEmpty()) {
                request.getHeaders().set("X-API-Key", apiKey);
            }
            request.getHeaders().set("Content-Type", "application/json");
            return execution.execute(request, body);
        });

        log.info("bp-notification interceptor registered (key prefix={}...)",
                apiKey != null && apiKey.length() > 8 ? apiKey.substring(0, 8) : "none");

        return restTemplate;
    }
}
