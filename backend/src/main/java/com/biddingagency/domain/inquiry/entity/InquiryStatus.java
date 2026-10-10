package com.biddingagency.domain.inquiry.entity;

/**
 * 1:1 문의 상태 (CR-132, T1-5).
 * RECEIVED 에서 ANSWERED 로만 간다. 답변 메일이 성공해야 ANSWERED 가 된다(BIZ-024).
 */
public enum InquiryStatus {
    RECEIVED,
    ANSWERED
}
