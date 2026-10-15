package com.biddingagency.config;

import org.springframework.beans.factory.annotation.Value;
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

    /**
     * CR-160: PIEE 입찰서류 받기 전용. 받기 일꾼이 한 번에 한 공고만 넘기므로 한 줄이면 된다(BIZ-079).
     */
    @Bean(name = "pieeFetchExecutor")
    public Executor pieeFetchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("piee-fetch-");
        executor.initialize();
        return executor;
    }

    /**
     * CR-164: 수집 때 새 공고 번역 전용. 번역기(Aimbase 실행기)는 동시에 여러 건을 받으면 멈추거나
     * 메모리가 넘쳐 죽으므로 한 줄로 돌린다(BIZ-070). 한 번에 새 공고가 많이 들어와도 버리지 않도록
     * 대기 칸은 설정값으로 넉넉히 둔다. 재기동 때 남은 번역은 기다리지 않는다(관리자 번역 단추로 채운다).
     */
    @Bean(name = "translationExecutor")
    public Executor translationExecutor(@Value("${app.async.translation.queue-capacity}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("translate-");
        executor.initialize();
        return executor;
    }
}
