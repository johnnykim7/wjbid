package com.biddingagency.domain.bid.entity;

/**
 * Bid Request State (FSM States)
 *
 * 11-state lifecycle: CREATED → DOCS_PENDING → DOCS_RECEIVED → ANALYZING → GENERATING → REVIEW → CONFIRMED → SUBMITTED → AWARDED / NOT_AWARDED, (각 단계) → CLOSED
 * CR-018: SUBMITTED 이후 입찰 결과(AWARDED 합격 / NOT_AWARDED 불합격) 상태 추가.
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
    /** 제출 완료 (입찰 결과 대기) */
    SUBMITTED,
    /** 입찰 합격 (CR-018) */
    AWARDED,
    /** 입찰 불합격 (CR-018) */
    NOT_AWARDED,
    /** 종료/취소 */
    CLOSED;

    public boolean isTerminal() {
        return this == AWARDED || this == NOT_AWARDED || this == CLOSED;
    }

    public boolean canEditDocuments() {
        return this == GENERATING || this == REVIEW;
    }

    public boolean requiresClientAction() {
        return this == DOCS_PENDING;
    }
}
