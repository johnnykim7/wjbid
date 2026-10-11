package com.biddingagency.domain.compliance.entity;

/**
 * 제출 전 검사의 형식·쪽수 판정 (CR-141, BIZ-047, BIZ-048).
 * MISMATCH 는 형식 어긋남, OVER 는 쪽수 넘음이다.
 */
public enum PreCheckVerdict {
    MATCH,
    MISMATCH,
    OVER,
    NEEDS_HUMAN,
    NO_CONDITION,
    NOT_UPLOADED;

    public boolean isProblem() {
        return this == MISMATCH || this == OVER;
    }
}
