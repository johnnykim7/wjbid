package com.biddingagency.domain.bid.entity;

/**
 * Bid Request State (FSM States)
 *
 * 9-state lifecycle: CREATED → DOCS_PENDING → DOCS_RECEIVED → ANALYZING → GENERATING → REVIEW → CONFIRMED → SUBMITTED / CLOSED
 */
public enum BidRequestState {
    /** 신청 접수 */
    CREATED,
    /** 문서 대기 */
    DOCS_PENDING,
    /** 문서 접수 완료 */
    DOCS_RECEIVED,
    /** AI 요구사항 분석 중 */
    ANALYZING,
    /** AI 문서 생성 중 */
    GENERATING,
    /** 관리자 검토 */
    REVIEW,
    /** 확정 (문서 잠금) */
    CONFIRMED,
    /** 제출 완료 */
    SUBMITTED,
    /** 종료/취소 */
    CLOSED;

    public boolean isTerminal() {
        return this == SUBMITTED || this == CLOSED;
    }

    public boolean canEditDocuments() {
        return this == GENERATING || this == REVIEW;
    }

    public boolean requiresClientAction() {
        return this == DOCS_PENDING;
    }
}
