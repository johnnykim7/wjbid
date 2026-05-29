package com.biddingagency.integration.samgov.scheduler;

import com.biddingagency.domain.event.OpportunitiesCollectedEvent;
import com.biddingagency.integration.samgov.client.OpportunityCollectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    // Keyword groups as defined in the plan
    private static final List<String> MAIN_KEYWORDS = List.of("411th csb");
    private static final List<String> KOREA_KEYWORDS = List.of("korea");

    /**
     * Scheduled collection - runs at 06:00, 12:00, 18:00, 23:00 KST
     * Cron: "0 0 6,12,18,23 * * *"
     */
    @Scheduled(cron = "0 0 6,12,18,23 * * *", zone = "Asia/Seoul")
    public void collectOpportunities() {
        log.info("====== Starting scheduled opportunity collection at {} ======",
                LocalDateTime.now());

        int totalCollected = 0;
        int totalTargetNew = 0;
        int totalChanged = 0;
        int totalUnchanged = 0;
        int totalErrors = 0;

        // Collect last 30 days
        int daysBack = 30;

        // MAIN Keywords (411th csb - primary target)
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

        // KOREA Keywords
        log.info("Collecting KOREA keywords...");
        for (String keyword : KOREA_KEYWORDS) {
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

        for (String keyword : KOREA_KEYWORDS) {
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
