package com.biddingagency.domain.opportunity.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 메뉴 속 화면 2묶음 응답 (CR-135, T3-2 C-2).
 *
 * <p>모든 응답의 dataFrom, dataTo 는 대상 공고의 첫·마지막 게시일, total 은 거르기 전 대상 건수다.
 */
public final class MarketResponses {

    private MarketResponses() {
    }

    public record TypeCount(String type, String typeKo, long count) {
    }

    public record CollectedResponse(List<MarketRowDto> content, int page, int size, long totalElements,
                                    int totalPages, long total, LocalDate dataFrom, LocalDate dataTo,
                                    List<TypeCount> typeCounts) {
    }

    public record AmendmentGroup(String solicitationNumber, String title, String titleKo, int count,
                                 LocalDate firstPostedDate, LocalDate lastPostedDate,
                                 OffsetDateTime lastResponseDeadline, boolean deadlineChanged,
                                 boolean hasAward, boolean open, List<MarketRowDto> versions) {
    }

    public record AmendmentsResponse(List<AmendmentGroup> content, int page, int size, long totalElements,
                                     int totalPages, long total, long openCount, long versionCount,
                                     LocalDate dataFrom, LocalDate dataTo) {
    }

    /** 공고문 예시. notice 는 공고 상세와 같은 공고문, 나머지는 원문 제목과 SAM.gov 공개 주소, 같은 공고번호의 첫 게시일 */
    public record SampleResponse(OpportunityDto notice, String originalTitle, String samUrl,
                                 LocalDate firstPostedDate) {
    }

    public record AwardSummary(long total, long amountKnownCount, BigDecimal amountSum, BigDecimal amountMax,
                               long awardeeKnownCount) {
    }

    public record MonthCountItem(String month, long count) {
    }

    public record AwardsResponse(List<MarketRowDto> content, int page, int size, long totalElements,
                                 int totalPages, AwardSummary summary, List<MonthCountItem> monthCounts,
                                 long total, LocalDate dataFrom, LocalDate dataTo) {
    }

    public record MonthStat(String month, long total, long bid, long pre, long award, long other) {
    }

    public record MonthlyResponse(List<MonthStat> months, List<String> emptyMonths, String currentMonth,
                                  long total, LocalDate dataFrom, LocalDate dataTo) {
    }

    public record SubCodeCount(String code, long count) {
    }

    public record SectorStat(String code, String nameKo, long count, List<SubCodeCount> subCodes) {
    }

    public record NaicsResponse(List<SectorStat> sectors, long noneCount, long codedCount, long total,
                                LocalDate dataFrom, LocalDate dataTo) {
    }

    /**
     * 사이트 자료 규모 (CR-162, T3-2 C-3a).
     *
     * @param federalContracts 미 연방 계약 자료 행 수
     * @param notices          모은 공고 수. 같은 공고번호는 한 건
     * @param openNotices      마감 전 공고 수. 첫 화면 건수와 같은 기준(BIZ-069)
     */
    public record SiteStats(long federalContracts, long notices, long openNotices) {
    }
}
