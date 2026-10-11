package com.biddingagency.domain.opportunity.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 낙찰 업체 응답 (CR-136, T3-2 C-3, BID-MKT-009)
 */
public final class AwardeeResponses {

    private AwardeeResponses() {
    }

    /** 목록 줄에 낙찰 금액 숫자를 붙인 것. 금액이 없으면 null */
    public record AwardRow(@JsonUnwrapped MarketRowDto row, BigDecimal awardAmountUsd) {
    }

    /**
     * @param key         UEI:값 또는 NAME:정규화한 이름 (BIZ-029)
     * @param rank        고른 정렬로 전체 업체 중 몇 번째인지. 이름으로 찾아도 바뀌지 않는다
     * @param amountSum   금액이 있는 공지만 더한 합계. 더한 공지가 없으면 null
     * @param amountCount 더한 공지 수
     */
    public record Awardee(String key, int rank, String name, String uei, int awardCount, BigDecimal amountSum,
                          int amountCount, LocalDate lastPostedDate, List<AwardRow> awards) {

        public Awardee ranked(int rank) {
            return new Awardee(key, rank, name, uei, awardCount, amountSum, amountCount, lastPostedDate, awards);
        }
    }

    public record AwardeesResponse(LocalDate dataFrom, LocalDate dataTo, long totalAwards, long awardsWithAwardee,
                                   List<Awardee> awardees) {
    }
}
