package com.biddingagency.domain.compliance.entity;

/**
 * 제출 검사에서 관리자가 적는 처리 (CR-194, BIZ-106, BIZ-109).
 */
public enum SubmissionDisposition {
    NOT_ANSWERED,
    EQUIVALENCE_CHECK,
    EQUIVALENT,
    APPLICABILITY_UNCONFIRMED,
    RESOLVED
}
