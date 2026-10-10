package com.biddingagency.dto;

/**
 * 요금 조회 응답 (CR-129)
 *
 * @param memberMonthlyPrice 회원 월 요금(원, 부가세 별도). 미정이면 null
 */
public record PricingResponse(Long memberMonthlyPrice) {
}
