package com.biddingagency.domain.document.entity;

import com.biddingagency.common.BaseEntity;
import com.biddingagency.domain.bid.entity.BidRequest;
import jakarta.persistence.*;
import lombok.*;

/**
 * Bid Document entity
 *
 * Container for document versions. Each document can have multiple versions.
 */
@Entity
@Table(name = "bid_documents",
        indexes = {
                @Index(name = "idx_bid_documents_bid_request", columnList = "bid_request_id"),
                @Index(name = "idx_bid_documents_type", columnList = "document_type"),
                @Index(name = "idx_bid_documents_status", columnList = "status")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BidDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bid_request_id", nullable = false, columnDefinition = "BINARY(16)")
    private BidRequest bidRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private DocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.DRAFT;

    @Column(name = "current_version_no", nullable = false)
    @Builder.Default
    private Integer currentVersionNo = 1;

    @Column(name = "template_id", columnDefinition = "BINARY(16)")
    private java.util.UUID templateId;

    // Business methods

    /**
     * Lock document (make immutable)
     */
    public void lock() {
        if (this.status == DocumentStatus.LOCKED) {
            throw new IllegalStateException("Document is already locked");
        }
        this.status = DocumentStatus.LOCKED;
    }

    /**
     * Unlock document (LOCKED → DRAFT)
     */
    public void unlock() {
        if (this.status != DocumentStatus.LOCKED) {
            throw new IllegalStateException("Document is not locked");
        }
        this.status = DocumentStatus.DRAFT;
    }

    /**
     * Check if document can be edited
     */
    public boolean canEdit() {
        return this.status.canEdit();
    }

    /**
     * Check if document is locked
     */
    public boolean isLocked() {
        return this.status.isImmutable();
    }

    /**
     * Update status
     */
    public void updateStatus(DocumentStatus newStatus) {
        if (this.status == DocumentStatus.LOCKED) {
            throw new IllegalStateException("Cannot change status of LOCKED document");
        }
        this.status = newStatus;
    }

    /**
     * Increment version number
     */
    public void incrementVersion() {
        this.currentVersionNo++;
    }
}
