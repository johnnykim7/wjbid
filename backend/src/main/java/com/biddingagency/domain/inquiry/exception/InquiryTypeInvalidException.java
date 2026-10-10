package com.biddingagency.domain.inquiry.exception;

public class InquiryTypeInvalidException extends RuntimeException {
    public InquiryTypeInvalidException() {
        super("없거나 쓰지 않는 문의 유형입니다.");
    }
}
