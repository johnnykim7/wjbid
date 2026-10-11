package com.biddingagency.domain.eligibility.exception;

public class EligibilityAnswerInvalidException extends RuntimeException {
    public EligibilityAnswerInvalidException() {
        super("답은 HAVE, LACK, UNSURE 중 하나여야 합니다.");
    }
}
