package com.biddingagency.domain.eligibility.exception;

public class EligibilityNoticeNotFoundException extends RuntimeException {
    public EligibilityNoticeNotFoundException() {
        super("진단할 수 없는 공고입니다.");
    }
}
