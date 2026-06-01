package com.biddingagency.domain.proposal.entity;

/**
 * Proposal Block 종류 (CR-027). TipTap node 단위로 영속화.
 */
public enum BlockType {
    PARAGRAPH,
    BULLET_LIST,
    TABLE,
    IMAGE,
    EVIDENCE
}
