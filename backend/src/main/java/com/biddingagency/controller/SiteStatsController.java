package com.biddingagency.controller;

import com.biddingagency.domain.opportunity.dto.MarketResponses.SiteStats;
import com.biddingagency.domain.opportunity.service.SiteStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사이트 자료 규모 (CR-162, T3-2 C-3a). 로그인 없이 연다(GET /opportunities/** 공개).
 */
@RestController
@RequestMapping("/opportunities")
@RequiredArgsConstructor
@Tag(name = "Market", description = "사이트 자료 규모 (CR-162)")
public class SiteStatsController {

    private final SiteStatsService siteStatsService;

    @GetMapping("/stats/site")
    @Operation(summary = "사이트 자료 규모", description = "미 연방 계약 자료 건수, 모은 공고 수, 마감 전 공고 수")
    public ResponseEntity<SiteStats> siteStats() {
        return ResponseEntity.ok(siteStatsService.stats());
    }
}
