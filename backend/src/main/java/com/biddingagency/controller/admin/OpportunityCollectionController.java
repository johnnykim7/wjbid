package com.biddingagency.controller.admin;

import com.biddingagency.integration.samgov.scheduler.OpportunityCollectionScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Opportunity Collection Controller (Admin)
 *
 * Manual control for SAM.gov opportunity collection
 */
@Slf4j
@RestController
@RequestMapping("/admin/collection")
@RequiredArgsConstructor
@Tag(name = "Admin - Collection", description = "SAM.gov opportunity collection management")
@PreAuthorize("hasRole('ADMIN')")
public class OpportunityCollectionController {

    private final OpportunityCollectionScheduler collectionScheduler;

    /**
     * Manually trigger opportunity collection
     */
    @PostMapping("/trigger")
    @Operation(summary = "Trigger collection", description = "Manually trigger SAM.gov opportunity collection")
    public ResponseEntity<Map<String, String>> triggerCollection(
            @RequestParam(defaultValue = "30") int daysBack) {
        log.info("Manual collection triggered by admin for last {} days", daysBack);

        // Execute collection asynchronously
        new Thread(() -> collectionScheduler.triggerManualCollection(daysBack)).start();

        return ResponseEntity.accepted().body(Map.of(
                "status", "ACCEPTED",
                "message", "Collection started in background for last " + daysBack + " days",
                "note", "Check logs for progress and results"
        ));
    }

    /**
     * Get collection status (placeholder for future implementation)
     */
    @GetMapping("/status")
    @Operation(summary = "Collection status", description = "Get current collection job status")
    public ResponseEntity<Map<String, String>> getCollectionStatus() {
        // TODO: Implement actual status tracking (e.g., with Redis or DB)
        return ResponseEntity.ok(Map.of(
                "status", "NOT_IMPLEMENTED",
                "message", "Status tracking will be implemented in Phase 2"
        ));
    }
}
