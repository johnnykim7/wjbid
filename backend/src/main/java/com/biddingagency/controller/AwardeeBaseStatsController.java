package com.biddingagency.controller;

import com.biddingagency.domain.base.service.MilitaryBaseService;
import com.biddingagency.domain.opportunity.dto.AwardeeResponses.AwardeesResponse;
import com.biddingagency.domain.opportunity.dto.MarketRowDto;
import com.biddingagency.domain.opportunity.service.AwardeeService;
import com.biddingagency.domain.opportunity.service.AwardeeService.AwardeeSort;
import com.biddingagency.dto.BaseResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 메뉴 속 화면 3묶음: 낙찰 업체와 기지별 집계 (CR-136, T3-2 C-3). 로그인 없이 연다(GET /opportunities/** 공개).
 */
@RestController
@RequestMapping("/opportunities")
@RequiredArgsConstructor
@Tag(name = "Market", description = "낙찰 업체와 기지별 집계 (CR-136)")
public class AwardeeBaseStatsController {

    private static final int MAX_BASE_ROWS = 50;

    private final AwardeeService awardeeService;
    private final MilitaryBaseService militaryBaseService;

    @GetMapping("/awardees")
    @Operation(summary = "낙찰 업체", description = "업체 이름이 있는 낙찰 공지를 업체별로 묶음. 건수순 또는 금액순")
    public ResponseEntity<AwardeesResponse> awardees(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "COUNT") AwardeeSort sort) {
        return ResponseEntity.ok(awardeeService.awardees(q, sort));
    }

    @GetMapping("/stats/bases")
    @Operation(summary = "기지별 집계", description = "기지마다 공고번호 수, 낙찰 공지 수, 마감 전 공고번호 수")
    public ResponseEntity<BaseResponses.BaseStats> baseStats() {
        return ResponseEntity.ok(militaryBaseService.stats());
    }

    @GetMapping("/stats/bases/{baseId}/opportunities")
    @Operation(summary = "기지 공고", description = "그 기지 공고 최근 게시순(낙찰 공지 제외)")
    public ResponseEntity<List<MarketRowDto>> baseOpportunities(
            @PathVariable UUID baseId,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(militaryBaseService.recentOpportunities(baseId,
                Math.min(Math.max(size, 1), MAX_BASE_ROWS)));
    }
}
