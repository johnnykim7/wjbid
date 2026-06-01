package com.biddingagency.domain.proposal.entity;

/**
 * Proposal Section 상태 (CR-027 / CR-030 FSM).
 *
 * PENDING → DRAFTING → DRAFTED → VERIFIED
 *                            ↓ (실패)
 *                      NEEDS_REGEN (1회 자동 재시도 후도 실패)
 *                            ↓ (사람 수정 + 잠금)
 *                      LOCKED  ← 자동 트리거 모두 차단
 */
public enum SectionStatus {
    PENDING,
    DRAFTING,
    DRAFTED,
    VERIFIED,
    NEEDS_REGEN,
    LOCKED;

    /** LOCKED 는 어떤 자동 재생성 트리거(②③④)도 차단 (CR-030). */
    public boolean isLocked() {
        return this == LOCKED;
    }

    /** 작성 결과가 존재하는 상태 (재생성 시 이전 block 교체 대상). */
    public boolean hasContent() {
        return this == DRAFTED || this == VERIFIED || this == NEEDS_REGEN || this == LOCKED;
    }
}
