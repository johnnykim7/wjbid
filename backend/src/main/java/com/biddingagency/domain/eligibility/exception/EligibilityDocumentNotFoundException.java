package com.biddingagency.domain.eligibility.exception;

public class EligibilityDocumentNotFoundException extends RuntimeException {
    public EligibilityDocumentNotFoundException() {
        super("서류함에서 서류를 찾을 수 없습니다.");
    }
}
