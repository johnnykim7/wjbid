package com.biddingagency.domain.proposal.entity;

/**
 * generation_log target 종류.
 *
 * 선반영 시점(CR-027)엔 트리 노드 단위(CHAPTER/SECTION/BLOCK)로 정의했으나,
 * CR-029 본격에서 비용 추적 단위는 CR-028 파이프라인 단계(design/write-section/assemble)와 1:1 이므로
 * 단계 단위 값을 추가한다. targetId 는 그 단계가 생성/대상한 엔티티(documentId 또는 sectionId).
 */
public enum GenerationTargetType {
    // 트리 노드 단위 (선반영 — 향후 block 단위 추적 시 사용)
    CHAPTER,
    SECTION,
    BLOCK,
    VERIFY,

    // CR-028 파이프라인 단계 단위 (CR-029 비용 추적)
    DESIGN,        // proposal-design — targetId = documentId
    WRITE_SECTION, // proposal-write-section — targetId = sectionId
    ASSEMBLE       // proposal-assemble — targetId = documentId
}
