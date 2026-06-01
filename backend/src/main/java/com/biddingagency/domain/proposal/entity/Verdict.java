package com.biddingagency.domain.proposal.entity;

/**
 * 검증 판정 (CR-031). PASS = 근거 충실 + 분량 적정. FAIL = 환각 또는 분량 미달/초과.
 */
public enum Verdict {
    PASS,
    FAIL;

    public boolean isPass() {
        return this == PASS;
    }
}
