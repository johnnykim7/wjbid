package com.biddingagency.integration.samgov.scheduler;

import com.biddingagency.integration.samgov.client.OpportunityCollectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    // Keyword groups as defined in the plan
    private static final List<String> MAIN_KEYWORDS = List.of("411th csb");
    private static final List<String> KOREA_KEYWORDS = List.of("korea");
    private static final List<String> USFK_LOCATION_KEYWORDS = List.of(
            "Camp Casey", "Osan Air Base", "Kunsan Air Base",
            "Camp Humphreys", "Camp Henry", "USAG Daegu"
    );

    /**
     * Scheduled collection - runs at 06:00, 12:00, 18:00, 23:00 KST
     * Cron: "0 0 6,12,18,23 * * *"
     */
    @Scheduled(cron = "0 0 6,12,18,23 * * *", zone = "Asia/Seoul")
    public void collectOpportunities() {
        log.info("====== Starting scheduled opportunity collection at {} ======",
                LocalDateTime.now());

        int totalCollected = 0;
        int totalDuplicates = 0;
        int totalErrors = 0;

        // Collect last 30 days
        int daysBack = 30;

        // MAIN Keywords (411th csb - primary target)
        log.info("Collecting MAIN keywords...");
        for (String keyword : MAIN_KEYWORDS) {
            OpportunityCollectorService.CollectionResult result =
                    collectorService.collectByKeyword(keyword, daysBack);
            totalCollected += result.collected();
            totalDuplicates += result.duplicates();
            totalErrors += result.errors();
        }

        // KOREA Keywords
        log.info("Collecting KOREA keywords...");
        for (String keyword : KOREA_KEYWORDS) {
            OpportunityCollectorService.CollectionResult result =
                    collectorService.collectByKeyword(keyword, daysBack);
            totalCollected += result.collected();
            totalDuplicates += result.duplicates();
            totalErrors += result.errors();
        }

        // USFK Location Keywords
        log.info("Collecting USFK_LOCATION keywords...");
        for (String keyword : USFK_LOCATION_KEYWORDS) {
            OpportunityCollectorService.CollectionResult result =
                    collectorService.collectByKeyword(keyword, daysBack);
            totalCollected += result.collected();
            totalDuplicates += result.duplicates();
            totalErrors += result.errors();
        }

        log.info("====== Collection completed: {} new, {} duplicates, {} errors ======",
                totalCollected, totalDuplicates, totalErrors);

        // TODO: Send Slack notification if errors > 0
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
        int totalDuplicates = 0;
        int totalErrors = 0;

        for (String keyword : MAIN_KEYWORDS) {
            OpportunityCollectorService.CollectionResult result =
                    collectorService.collectByKeyword(keyword, daysBack);
            totalCollected += result.collected();
            totalDuplicates += result.duplicates();
            totalErrors += result.errors();
        }

        for (String keyword : KOREA_KEYWORDS) {
            OpportunityCollectorService.CollectionResult result =
                    collectorService.collectByKeyword(keyword, daysBack);
            totalCollected += result.collected();
            totalDuplicates += result.duplicates();
            totalErrors += result.errors();
        }

        for (String keyword : USFK_LOCATION_KEYWORDS) {
            OpportunityCollectorService.CollectionResult result =
                    collectorService.collectByKeyword(keyword, daysBack);
            totalCollected += result.collected();
            totalDuplicates += result.duplicates();
            totalErrors += result.errors();
        }

        log.info("Manual collection completed: {} new, {} duplicates, {} errors",
                totalCollected, totalDuplicates, totalErrors);
    }
}
