package com.biddingagency.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 실행 설정
 * LLM 호출은 수 분이 소요되므로 별도 스레드 풀에서 실행합니다.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "llmTaskExecutor")
    public Executor llmTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("llm-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(300); // 최대 5분 대기
        executor.initialize();
        return executor;
    }
}
