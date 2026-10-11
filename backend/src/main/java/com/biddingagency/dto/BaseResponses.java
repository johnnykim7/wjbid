package com.biddingagency.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 기지 관리와 기지별 집계 응답 (CR-136)
 */
public final class BaseResponses {

    private BaseResponses() {
    }

    /** 관리자 기지 목록 줄. matchedCount 는 지금 걸리는 공고번호 수(낙찰 공지 제외) */
    public record AdminBase(UUID id, String nameKo, String nameEn, List<String> matchTerms, String region,
                            BigDecimal latitude, BigDecimal longitude, int sortOrder, int matchedCount) {
    }

    /** 어느 기지에도 걸리지 않은 공고. 공고번호마다 가장 최근 판 하나 */
    public record UnmatchedRow(UUID id, String noticeId, String solicitationNumber, String title, String titleKo,
                               LocalDate postedDate) {
    }

    public record Unmatched(int count, List<UnmatchedRow> rows) {
    }

    /** 기지 하나의 집계 */
    public record BaseStat(UUID id, String nameKo, String nameEn, String region, BigDecimal latitude,
                           BigDecimal longitude, int opportunityCount, int awardCount, int openCount) {
    }

    /** 기지별 집계. 기지별 합은 한 공고가 여러 기지에 걸리면 totalNumbers 보다 클 수 있다 */
    public record BaseStats(LocalDate dataFrom, LocalDate dataTo, int totalNumbers, int unmatchedNumbers,
                            List<BaseStat> bases) {
    }
}
