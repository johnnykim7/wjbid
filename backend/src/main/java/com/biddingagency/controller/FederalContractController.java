package com.biddingagency.controller;

import com.biddingagency.domain.federal.dto.FederalContractResponses.AwardRangeResponse;
import com.biddingagency.domain.federal.dto.FederalContractResponses.CalendarResponse;
import com.biddingagency.domain.federal.dto.FederalContractResponses.NaicsResponse;
import com.biddingagency.domain.federal.dto.FederalContractResponses.RecompeteResponse;
import com.biddingagency.domain.federal.entity.RecompeteCategory;
import com.biddingagency.domain.federal.exception.FederalContractException;
import com.biddingagency.domain.federal.service.FederalContractQueryService;
import com.biddingagency.domain.federal.service.FederalContractQueryService.Sort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/**
 * 메뉴 속 화면 4묶음: 미 연방 계약 자료 (CR-142, T3-2 C-4). 로그인 회원만 연다.
 */
@RestController
@RequestMapping("/federal-contracts")
@RequiredArgsConstructor
@Tag(name = "FederalContract", description = "미 연방 계약 자료 (CR-142)")
public class FederalContractController {

    private static final int MAX_PAGE_SIZE = 100;

    private final FederalContractQueryService queryService;

    @GetMapping("/recompete")
    @Operation(summary = "고른 달 계약", description = "구분(예측)과 근거. 기본 달은 다음 달(한국 날짜)")
    public ResponseEntity<RecompeteResponse> recompete(
            @RequestParam(required = false) String month,
            @RequestParam(required = false) RecompeteCategory category,
            @RequestParam(required = false) String naics,
            @RequestParam(defaultValue = "END") Sort sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        YearMonth ym = month == null || month.isBlank() ? queryService.calendarMonths().get(1) : parseMonth(month);
        return ResponseEntity.ok(queryService.recompete(ym, category, blankToNull(naics), sort, Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE)));
    }

    @GetMapping("/recompete/calendar")
    @Operation(summary = "재발주 달력", description = "이번 달부터 정한 달 수만큼 달마다 구분별 수")
    public ResponseEntity<CalendarResponse> calendar(@RequestParam(required = false) String naics) {
        return ResponseEntity.ok(queryService.calendar(blankToNull(naics)));
    }

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

    private static YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month.trim());
        } catch (DateTimeParseException e) {
            throw FederalContractException.badRequest("FEDERAL_MONTH_INVALID", "month 는 YYYY-MM 형식이어야 합니다");
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
