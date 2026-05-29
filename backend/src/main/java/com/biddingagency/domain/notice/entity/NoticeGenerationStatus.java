package com.biddingagency.domain.notice.entity;

/**
 * 공고문 한글화/요약 생성 진행 상태 (CR-016).
 * 구 AnalysisStatus를 Notice로 흡수.
 */
public enum NoticeGenerationStatus {
    PENDING,
    ANALYZING,
    COMPLETED,
    FAILED
}
