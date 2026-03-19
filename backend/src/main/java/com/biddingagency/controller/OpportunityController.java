package com.biddingagency.controller;

import com.biddingagency.domain.opportunity.dto.OpportunityDto;
import com.biddingagency.domain.opportunity.service.OpportunityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Opportunity Controller
 *
 * Handles contract opportunity queries
 */
@Slf4j
@RestController
@RequestMapping("/opportunities")
@RequiredArgsConstructor
@Tag(name = "Opportunities", description = "Contract opportunity management")
public class OpportunityController {

    private final OpportunityService opportunityService;

    /**
     * Get all active opportunities
     */
    @GetMapping
    @Operation(summary = "List opportunities", description = "Get all active opportunities with pagination")
    public ResponseEntity<Page<OpportunityDto>> listOpportunities(
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("Fetching opportunities, page: {}", pageable.getPageNumber());
        Page<OpportunityDto> opportunities = opportunityService.findAllActive(pageable)
                .map(OpportunityDto::from);
        return ResponseEntity.ok(opportunities);
    }

    /**
     * Get opportunity by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get opportunity", description = "Get opportunity details by ID")
    public ResponseEntity<OpportunityDto> getOpportunity(@PathVariable UUID id) {
        log.debug("Fetching opportunity: {}", id);
        OpportunityDto dto = OpportunityDto.from(opportunityService.findById(id));
        return ResponseEntity.ok(dto);
    }

    /**
     * Search opportunities by keyword
     */
    @GetMapping("/search")
    @Operation(summary = "Search opportunities", description = "Search opportunities by keyword in title")
    public ResponseEntity<Page<OpportunityDto>> searchOpportunities(
            @RequestParam String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("Searching opportunities with keyword: {}", keyword);
        Page<OpportunityDto> opportunities = opportunityService.searchByKeyword(keyword, pageable)
                .map(OpportunityDto::from);
        return ResponseEntity.ok(opportunities);
    }

    /**
     * Search by organization
     */
    @GetMapping("/search/organization")
    @Operation(summary = "Search by organization", description = "Search opportunities by organization name")
    public ResponseEntity<Page<OpportunityDto>> searchByOrganization(
            @RequestParam String organization,
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("Searching opportunities for organization: {}", organization);
        Page<OpportunityDto> opportunities = opportunityService.searchByOrganization(organization, pageable)
                .map(OpportunityDto::from);
        return ResponseEntity.ok(opportunities);
    }

    /**
     * Get opportunities near deadline
     */
    @GetMapping("/near-deadline")
    @Operation(summary = "Near deadline", description = "Get opportunities approaching deadline")
    public ResponseEntity<List<OpportunityDto>> nearDeadline(
            @RequestParam(defaultValue = "7") int days) {
        log.debug("Fetching opportunities near deadline (within {} days)", days);
        List<OpportunityDto> opportunities = opportunityService.findNearDeadline(days)
                .stream().map(OpportunityDto::from).toList();
        return ResponseEntity.ok(opportunities);
    }

    /**
     * Get recently posted opportunities
     */
    @GetMapping("/recent")
    @Operation(summary = "Recently posted", description = "Get recently posted opportunities")
    public ResponseEntity<List<OpportunityDto>> recentlyPosted(
            @RequestParam(defaultValue = "30") int days) {
        log.debug("Fetching opportunities posted in last {} days", days);
        List<OpportunityDto> opportunities = opportunityService.findRecentlyPosted(days)
                .stream().map(OpportunityDto::from).toList();
        return ResponseEntity.ok(opportunities);
    }
}
