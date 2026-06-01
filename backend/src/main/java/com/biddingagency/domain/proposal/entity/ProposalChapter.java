package com.biddingagency.domain.proposal.entity;

import com.biddingagency.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Proposal Chapter — FACTOR 단위 (CR-027).
 *
 * 공고가 박은 라벨·순서를 그대로 보존 (자체 양식 없음).
 * bid_documents 1:N proposal_chapter, chapter 1:N section.
 */
@Entity
@Table(name = "proposal_chapter",
        indexes = {
                @Index(name = "idx_proposal_chapter_document", columnList = "document_id"),
                @Index(name = "idx_proposal_chapter_order", columnList = "document_id, order_no")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ProposalChapter extends BaseEntity {

    @Column(name = "document_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID documentId;

    /** 공고가 박은 라벨 그대로 ("I", "A", "1") */
    @Column(name = "factor_label", length = 50)
    private String factorLabel;

    @Column(name = "factor_title", length = 500)
    private String factorTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_section", nullable = false, length = 20)
    @Builder.Default
    private SourceSection sourceSection = SourceSection.NOTICE_M;

    @Column(name = "order_no", nullable = false)
    @Builder.Default
    private Integer orderNo = 0;

    // Business methods

    public void updateOrder(int orderNo) {
        this.orderNo = orderNo;
    }

    public void updateMeta(String factorLabel, String factorTitle, SourceSection sourceSection) {
        if (factorLabel != null) this.factorLabel = factorLabel;
        if (factorTitle != null) this.factorTitle = factorTitle;
        if (sourceSection != null) this.sourceSection = sourceSection;
    }
}
