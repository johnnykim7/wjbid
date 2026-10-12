package com.biddingagency.controller;

import com.biddingagency.domain.opportunity.MarketGroup;
import com.biddingagency.domain.opportunity.dto.MarketResponses.AmendmentsResponse;
import com.biddingagency.domain.opportunity.dto.MarketResponses.AwardsResponse;
import com.biddingagency.domain.opportunity.dto.MarketResponses.CollectedResponse;
import com.biddingagency.domain.opportunity.dto.MarketResponses.MonthlyResponse;
import com.biddingagency.domain.opportunity.dto.MarketResponses.NaicsResponse;
import com.biddingagency.domain.opportunity.dto.MarketResponses.SampleResponse;
import com.biddingagency.domain.opportunity.service.MarketDataService;
import com.biddingagency.domain.opportunity.service.MarketDataService.AwardSort;
import com.biddingagency.domain.opportunity.service.MarketDataService.CollectedFilter;
import com.biddingagency.domain.opportunity.service.MarketDataService.CollectedSort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/**
 * 메뉴 속 화면 2묶음: 모은 공고 목록과 집계 (CR-135, T3-2 C-2). 로그인 없이 연다(GET /opportunities/** 공개).
 */
@RestController
@RequestMapping("/opportunities")
@RequiredArgsConstructor
@Tag(name = "Market", description = "모은 공고 목록과 집계 (CR-135)")
public class MarketDataController {

    private static final int MAX_PAGE_SIZE = 100;

    private final MarketDataService marketDataService;

    @GetMapping("/collected")
    @Operation(summary = "모은 공고 목록", description = "게시일 최신순 또는 마감순. 묶음, 유형, 게시 달, 업종 대분류, "
            + "기한 남은 것만, 공고번호별 마지막 판만, 검색어, 마감까지 남은 날")
    public ResponseEntity<CollectedResponse> collected(
            @RequestParam(required = false) MarketGroup group,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String sector,
            @RequestParam(defaultValue = "false") boolean openOnly,
            @RequestParam(defaultValue = "false") boolean latestOnly,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "LATEST") CollectedSort sort,
            @RequestParam(required = false) Integer dueWithinDays,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (dueWithinDays != null && dueWithinDays < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dueWithinDays 는 1 이상이어야 합니다");
        }
        CollectedFilter filter = new CollectedFilter(group, blankToNull(type), parseMonth(month), blankToNull(sector),
                openOnly, latestOnly, blankToNull(q), sort, dueWithinDays);
        return ResponseEntity.ok(marketDataService.collected(filter, Math.max(page, 0), clampSize(size)));
    }

    @GetMapping("/amendments")
    @Operation(summary = "변경 공고", description = "같은 공고번호로 두 번 이상 올라온 공고 묶음(낙찰 공지 제외), 마지막 게시일 최신순. 공고번호 하나로 거를 수 있다")
    public ResponseEntity<AmendmentsResponse> amendments(
            @RequestParam(required = false) String solicitationNumber,
            @RequestParam(defaultValue = "false") boolean openOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(marketDataService.amendments(blankToNull(solicitationNumber), openOnly,
                Math.max(page, 0), clampSize(size)));
    }

    @GetMapping("/sample")
    @Operation(summary = "공고문 예시", description = "설정 NOTICE_SAMPLE_ID 의 공개 공고문과 원문 제목, SAM.gov 공개 주소, 첫 게시일. 없으면 204")
    public ResponseEntity<SampleResponse> sample() {
        return marketDataService.sample()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/awards")
    @Operation(summary = "최근 낙찰", description = "낙찰 공지 목록과 요약. 공지 달 거르기, 최신순 또는 금액순")
    public ResponseEntity<AwardsResponse> awards(
            @RequestParam(required = false) String month,
            @RequestParam(defaultValue = "LATEST") AwardSort sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(marketDataService.awards(parseMonth(month), sort, Math.max(page, 0), clampSize(size)));
    }

    @GetMapping("/stats/monthly")
    @Operation(summary = "월별 동향", description = "첫 게시 달부터 이번 달까지 달마다 묶음 유형별 건수")
    public ResponseEntity<MonthlyResponse> monthly() {
        return ResponseEntity.ok(marketDataService.monthly());
    }

    @GetMapping("/stats/naics")
    @Operation(summary = "업종별 발주", description = "업종 대분류별 건수와 세부 코드별 건수")
    public ResponseEntity<NaicsResponse> naics() {
        return ResponseEntity.ok(marketDataService.naics());
    }

    private static YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) return null;
        try {
            return YearMonth.parse(month.trim());
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "month 는 YYYY-MM 형식이어야 합니다");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static int clampSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }
}
