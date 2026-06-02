package com.biddingagency.integration.samgov.scheduler;

import com.biddingagency.domain.event.OpportunitiesCollectedEvent;
import com.biddingagency.integration.samgov.client.OpportunityCollectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Opportunity Collection Scheduler
 *
 * Runs 4 times daily: 06:00, 12:00, 18:00, 23:00 KST
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpportunityCollectionScheduler {

    private final OpportunityCollectorService collectorService;
    private final ApplicationEventPublisher eventPublisher;

    /** 스케줄 수집 on/off. SAM 일일 쿼터 보호용으로 운영에서 끌 수 있다. 수동 트리거는 영향 없음. */
    @Value("${app.sam-gov.scheduler.enabled:true}")
    private boolean schedulerEnabled;

    // 타깃: 411TH CSB 단일 키워드만 수집 (CR-026).
    // SAM의 fullParentPathName은 '411TH CSB' 대문자가 정본. phrase 검색 + organization 후필터 양쪽에 동일 토큰 사용.
    private static final List<String> MAIN_KEYWORDS = List.of("411TH CSB");

    /**
     * Scheduled collection - runs at 06:00, 12:00, 18:00, 23:00 KST
     * Cron: "0 0 6,12,18,23 * * *"
     */
    @Scheduled(cron = "0 0 6,12,18,23 * * *", zone = "Asia/Seoul")
    public void collectOpportunities() {
        if (!schedulerEnabled) {
            log.info("====== Scheduled collection SKIPPED (app.sam-gov.scheduler.enabled=false) ======");
            return;
        }
        log.info("====== Starting scheduled opportunity collection at {} ======",
                LocalDateTime.now());

        int totalCollected = 0;
        int totalTargetNew = 0;
        int totalChanged = 0;
        int totalUnchanged = 0;
        int totalErrors = 0;

        // Collect last 30 days
        int daysBack = 30;

        // MAIN Keywords (411TH CSB - primary target)
        log.info("Collecting MAIN keywords...");
        for (String keyword : MAIN_KEYWORDS) {
            OpportunityCollectorService.CollectionResult result =
                    collectorService.collectByKeyword(keyword, daysBack);
            totalCollected += result.collected();
            totalTargetNew += result.targetNew();
            totalChanged += result.changed();
            totalUnchanged += result.unchanged();
            totalErrors += result.errors();
        }

        // CR-009: totalFetched = 신규 + 변경 + 무변경, updatedCount = 실제 변경(CHANGED)만
        int totalFetched = totalCollected + totalChanged + totalUnchanged;
        log.info("====== Collection completed: {} new ({} target), {} changed, {} unchanged, {} errors ======",
                totalCollected, totalTargetNew, totalChanged, totalUnchanged, totalErrors);

        eventPublisher.publishEvent(new OpportunitiesCollectedEvent(
                null, totalCollected, totalTargetNew, totalChanged, totalFetched));

        if (totalErrors > 0) {
            log.error("Collection completed with {} errors - manual review needed", totalErrors);
        }
    }

    /**
     * Manual trigger method (can be called by controller)
     */
    public void triggerManualCollection(int daysBack) {
        log.info("Manual collection triggered for last {} days", daysBack);

        int totalCollected = 0;
        int totalChanged = 0;
        int totalUnchanged = 0;
        int totalErrors = 0;

        for (String keyword : MAIN_KEYWORDS) {
            OpportunityCollectorService.CollectionResult result =
                    collectorService.collectByKeyword(keyword, daysBack);
            totalCollected += result.collected();
            totalChanged += result.changed();
            totalUnchanged += result.unchanged();
            totalErrors += result.errors();
        }

        log.info("Manual collection completed: {} new, {} changed, {} unchanged, {} errors",
                totalCollected, totalChanged, totalUnchanged, totalErrors);
    }
}
