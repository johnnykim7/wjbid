package com.biddingagency.domain.proposal.entity;

/**
 * 검증 판정 (CR-031). PASS = 근거 충실 + 분량 적정. FAIL = 환각 또는 분량 미달/초과.
 * UNVERIFIED = 검증이 돌지 못했거나 이번 시도 결과가 없음 (CR-194, BIZ-104). 통과도 실패도 아니다.
 */
public enum Verdict {
    PASS,
    FAIL,
    UNVERIFIED;

    public boolean isPass() {
        return this == PASS;
    }
}
