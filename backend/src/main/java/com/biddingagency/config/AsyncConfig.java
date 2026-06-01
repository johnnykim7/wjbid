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

    /**
     * CR-025: SAM 첨부 자동 다운로드 전용 풀.
     * IO bound 작업이며 LLM 풀과 분리. SAM rate limit 보호를 위해 동시성 제한(core=3, max=5).
     */
    @Bean(name = "attachmentDownloadExecutor")
    public Executor attachmentDownloadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("attach-dl-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        executor.initialize();
        return executor;
    }
}
