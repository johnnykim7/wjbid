package com.biddingagency.domain.analysis.entity;

/** 서버가 Pwin 과 정책 문턱으로 정한 판정 (CR-170, BIZ-091, POL-041) */
public enum BidVerdict {
    STRONG_GO,
    CONDITIONAL_GO,
    WEAK,
    NO_GO
}
