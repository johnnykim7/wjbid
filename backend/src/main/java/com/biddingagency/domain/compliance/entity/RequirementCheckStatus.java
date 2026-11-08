package com.biddingagency.domain.compliance.entity;

/**
 * 제출 검사의 요구 줄 상태 (CR-194, BIZ-105).
 */
public enum RequirementCheckStatus {
    MET,
    NOT_MET,
    UNVERIFIED,
    NOT_CHECKED,
    NOT_ANSWERED,
    EQUIVALENCE_CHECK,
    EQUIVALENT,
    APPLICABILITY_UNCONFIRMED;

    /** 제출을 막지 않는 상태 */
    public boolean isClear() {
        return this == MET || this == NOT_ANSWERED || this == EQUIVALENT;
    }

    public static RequirementCheckStatus of(SubmissionDisposition disposition) {
        return valueOf(disposition.name());
    }
}
