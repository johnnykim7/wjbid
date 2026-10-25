package com.biddingagency.domain.analysis.entity;

/** 관리자의 Bid/No-Bid 결정 (CR-170, BIZ-092). Go 와 조건부 Go 여야 문서 작성으로 넘어간다 */
public enum BidDecision {
    GO,
    CONDITIONAL_GO,
    NO_GO;

    public boolean allowsGeneration() {
        return this == GO || this == CONDITIONAL_GO;
    }

    public boolean requiresNote() {
        return this == CONDITIONAL_GO || this == NO_GO;
    }
}
