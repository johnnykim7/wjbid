package com.biddingagency.domain.proposal.entity;

/**
 * 충실성·분량 검증 대상 종류 (CR-031).
 *
 * NOTICE            — 공고문 정제 결과 (NoticeService.saveResult). targetId = noticeId.
 * PROPOSAL_SECTION  — 제안서 section 본문 (write-section 후속). targetId = sectionId.
 */
public enum VerificationTargetType {
    NOTICE,
    PROPOSAL_SECTION
}
