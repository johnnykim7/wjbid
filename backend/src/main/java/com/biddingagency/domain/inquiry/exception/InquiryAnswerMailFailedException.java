package com.biddingagency.domain.inquiry.exception;

public class InquiryAnswerMailFailedException extends RuntimeException {
    public InquiryAnswerMailFailedException() {
        super("고객에게 메일을 보내지 못했습니다. 답변은 저장되지 않았습니다.");
    }
}
