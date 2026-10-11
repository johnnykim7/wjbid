package com.biddingagency.domain.federal.dto;

import java.time.LocalDate;

/** 회원 계약 등록과 고치기 본문 (CR-142, T3-2 C-5). 고치기는 piid 를 쓰지 않는다 */
public record MemberContractRequest(String piid, LocalDate endDate, LocalDate potentialEndDate, String description) {
}
