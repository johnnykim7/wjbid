package com.biddingagency.controller;

import com.biddingagency.domain.federal.dto.FederalContractResponses.AwardRangeResponse;
import com.biddingagency.domain.federal.dto.FederalContractResponses.NaicsResponse;
import com.biddingagency.domain.federal.service.FederalContractQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * 메뉴 속 화면 4묶음: 미 연방 계약 자료 (CR-142, T3-2 C-4). 로그인 회원만 연다.
 */
@RestController
@RequestMapping("/federal-contracts")
@RequiredArgsConstructor
@Tag(name = "FederalContract", description = "미 연방 계약 자료 (CR-142)")
public class FederalContractController {

    private final FederalContractQueryService queryService;

    @GetMapping("/award-range")
    @Operation(summary = "낙찰가 범위", description = "noticeNumber 또는 naics 하나는 필수, q 는 작업 설명 낱말")
    public ResponseEntity<AwardRangeResponse> awardRange(
            @RequestParam(required = false) String noticeNumber,
            @RequestParam(required = false) String naics,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(queryService.awardRange(blankToNull(noticeNumber), blankToNull(naics), blankToNull(q)));
    }

    @GetMapping("/naics")
    @Operation(summary = "업종 고르기 목록", description = "계약 수 많은 순 50건")
    public ResponseEntity<NaicsResponse> naics(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(queryService.naics(q));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
