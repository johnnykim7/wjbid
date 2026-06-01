package com.biddingagency.domain.proposal.entity;

/**
 * 검증 수행 방식 (CR-031).
 *
 * RULE — BE 정형 룰 (LLM 0콜). 분량/첨부 0건/block 0개/min_words.
 * LLM  — proposal-verify-fidelity 워크플로우 (Haiku). 문장 단위 환각·누락.
 */
public enum VerificationMethod {
    RULE,
    LLM
}
