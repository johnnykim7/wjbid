package com.biddingagency.domain.rfp.entity;

/** 패턴 가이드 출처 (CR-013, BIZ-017) */
public enum GuideSource {
    /** AI 자동추출 — 재추출 시 자동 갱신 가능 */
    AI_EXTRACTED,
    /** 사람이 AI 초안을 편집 — 자동추출 보호 */
    HUMAN_EDITED,
    /** 사람이 직접 추가 — 자동추출 보호 */
    HUMAN_ADDED
}
