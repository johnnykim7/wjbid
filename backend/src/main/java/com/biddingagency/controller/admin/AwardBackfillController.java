package com.biddingagency.controller.admin;

import com.biddingagency.integration.samgov.client.AwardBackfillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 지난 낙찰 공지 다시 받기 (CR-136, BID-MKT-008)
 */
@RestController
@RequestMapping("/admin/opportunities")
@RequiredArgsConstructor
@Tag(name = "Admin - Award backfill", description = "Re-fetch past award notices to fill awardee and amount")
@PreAuthorize("hasRole('ADMIN')")
public class AwardBackfillController {

    private final AwardBackfillService backfillService;

    public record BackfillRequest(LocalDate postedFrom, LocalDate postedTo) {
    }

    @PostMapping("/award-backfill")
    @Operation(summary = "Re-fetch award notices posted in the period")
    public ResponseEntity<AwardBackfillService.BackfillResult> backfill(@RequestBody BackfillRequest request) {
        return ResponseEntity.ok(backfillService.backfill(request.postedFrom(), request.postedTo()));
    }
}
