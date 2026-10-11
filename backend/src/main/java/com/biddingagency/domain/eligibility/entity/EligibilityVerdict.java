package com.biddingagency.domain.eligibility.entity;

/**
 * 공고별 판정 (CR-140, BIZ-040).
 * 회원이 남긴 답과 붙인 서류로만 낸다. 저장하지 않고 조회할 때 낸다.
 */
public enum EligibilityVerdict {
    ELIGIBLE,
    DOCS_MISSING,
    DIFFICULT,
    UNDECIDED
}
