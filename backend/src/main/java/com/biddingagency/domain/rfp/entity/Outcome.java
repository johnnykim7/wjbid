package com.biddingagency.domain.rfp.entity;

/** 성공 제안서 결과 (CR-013) */
public enum Outcome {
    /** 낙찰 확정 (B경로 최고 가치) */
    WON,
    /** 제출완료 (낙찰 여부 미상, 실무 통과) */
    SUBMITTED,
    /** 타업체 제출본 등 기타 (패턴 참고 신중) */
    OTHER
}
