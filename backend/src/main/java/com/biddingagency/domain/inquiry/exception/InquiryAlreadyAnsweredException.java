package com.biddingagency.domain.inquiry.exception;

public class InquiryAlreadyAnsweredException extends RuntimeException {
    public InquiryAlreadyAnsweredException() {
        super("이미 답변된 문의입니다.");
    }
}
