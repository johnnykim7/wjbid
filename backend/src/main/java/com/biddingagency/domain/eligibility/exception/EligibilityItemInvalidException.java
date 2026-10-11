package com.biddingagency.domain.eligibility.exception;

public class EligibilityItemInvalidException extends RuntimeException {
    public EligibilityItemInvalidException() {
        super("자격 순번이 공고의 참가 자격 목록에 없습니다.");
    }
}
