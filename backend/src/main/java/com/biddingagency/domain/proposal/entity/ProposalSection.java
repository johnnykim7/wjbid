package com.biddingagency.domain.proposal.entity;

import com.biddingagency.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

/**
 * Proposal Section — Subfactor 단위 (CR-027). CR-028 write-section 과 1:1.
 *
 * Subfactor 강제 안 한 공고는 section 1개로 통합 (subfactorLabel NULL).
 * status FSM 은 CR-030 부분 재생성·LOCKED 안전장치의 기준 (BIZ HUMAN_EDITED 보호).
 */
@Entity
@Table(name = "proposal_section",
        indexes = {
                @Index(name = "idx_proposal_section_chapter", columnList = "chapter_id"),
                @Index(name = "idx_proposal_section_status", columnList = "status"),
                @Index(name = "idx_proposal_section_order", columnList = "chapter_id, order_no")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ProposalSection extends BaseEntity {

    @Column(name = "chapter_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID chapterId;

    /** 공고 그대로, NULL 가능 (통합 section) */
    @Column(name = "subfactor_label", length = 50)
    private String subfactorLabel;

    @Column(name = "title", length = 500)
    private String title;

    /** 작성 지침 */
    @Column(name = "scope", columnDefinition = "TEXT")
    private String scope;

    /** Section L/M 발췌 ID 배열 */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "requirement_refs", columnDefinition = "JSON")
    private List<String> requirementRefs;

    @Column(name = "min_words")
    private Integer minWords;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SectionStatus status = SectionStatus.PENDING;

    @Column(name = "order_no", nullable = false)
    @Builder.Default
    private Integer orderNo = 0;

    // Business methods — status FSM (화이트리스트는 Service에서 검증; 여기선 단순 전이)

    public void markDrafting() {
        guardNotLocked();
        this.status = SectionStatus.DRAFTING;
    }

    public void markDrafted() {
        guardNotLocked();
        this.status = SectionStatus.DRAFTED;
    }

    public void markVerified() {
        guardNotLocked();
        this.status = SectionStatus.VERIFIED;
    }

    public void markNeedsRegen() {
        guardNotLocked();
        this.status = SectionStatus.NEEDS_REGEN;
    }

    /** 사람이 다듬은 후 잠금 — 이후 어떤 자동 트리거도 차단 (CR-030). */
    public void lock() {
        this.status = SectionStatus.LOCKED;
    }

    /** 잠금 해제는 명시 액션만. 재작업 가능하도록 DRAFTED 로 복귀. */
    public void unlock() {
        if (this.status != SectionStatus.LOCKED) {
            throw new IllegalStateException("Section is not locked");
        }
        this.status = SectionStatus.DRAFTED;
    }

    public boolean isLocked() {
        return this.status.isLocked();
    }

    public void updateOrder(int orderNo) {
        this.orderNo = orderNo;
    }

    public void updateMeta(String subfactorLabel, String title, String scope,
                           List<String> requirementRefs, Integer minWords) {
        if (subfactorLabel != null) this.subfactorLabel = subfactorLabel;
        if (title != null) this.title = title;
        if (scope != null) this.scope = scope;
        if (requirementRefs != null) this.requirementRefs = requirementRefs;
        if (minWords != null) this.minWords = minWords;
    }

    private void guardNotLocked() {
        if (this.status.isLocked()) {
            throw new IllegalStateException("Cannot transition LOCKED section — unlock first");
        }
    }
}
