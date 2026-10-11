package com.biddingagency.domain.eligibility.entity;

/**
 * 판정을 정한 통과 조건 자격이 걸린 까닭 (CR-140, BIZ-044).
 */
public enum EligibilityBlockerProblem {
    LACK,
    UNSURE,
    UNANSWERED,
    STALE,
    NO_DOCUMENT,
    EXPIRED
}
