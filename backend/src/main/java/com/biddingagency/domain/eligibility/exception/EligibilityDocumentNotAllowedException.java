package com.biddingagency.domain.eligibility.exception;

public class EligibilityDocumentNotAllowedException extends RuntimeException {
    public EligibilityDocumentNotAllowedException() {
        super("있음으로 답한 자격에만 증빙 서류를 붙일 수 있습니다.");
    }
}
