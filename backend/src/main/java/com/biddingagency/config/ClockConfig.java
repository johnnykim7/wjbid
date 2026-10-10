package com.biddingagency.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * 서비스가 시각을 읽는 기준 시계 (CR-132).
 *
 * JPA 감사(created_at, updated_at)가 JVM 기본 시간대로 LocalDateTime 을 채우므로,
 * 같은 행에 들어가는 다른 시각(답변 시각 등)도 같은 기준이어야 앞뒤가 맞는다.
 * 그래서 고정 시간대가 아니라 JVM 기본 시간대를 쓴다. 시험에서는 고정 시계로 바꿔 넣는다.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
