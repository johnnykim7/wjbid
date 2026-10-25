package com.biddingagency.domain.analysis.entity;

/**
 * 입찰 분석 회차 상태 (CR-170, BID-DOC-006).
 * DECISION_ONLY 는 분석 없이 관리자 결정만 기록한 회차다(BID-DOC-007).
 */
public enum BidAnalysisStatus {
    RUNNING,
    DONE,
    FAILED,
    DECISION_ONLY
}
