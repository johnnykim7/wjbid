package com.biddingagency.domain.compliance.entity;

import com.biddingagency.common.BaseEntity;
import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.entity.ClientDocument;
import com.biddingagency.domain.document.entity.BidDocument;
import com.biddingagency.domain.opportunity.entity.OpportunityRequirementItem;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Requirement Fulfillment Map entity
 *
 * Maps opportunity requirements to document sections or attachments
 */
@Entity
@Table(name = "requirement_fulfillment_maps",
        indexes = {
                @Index(name = "idx_fulfillment_bid_request", columnList = "bid_request_id"),
                @Index(name = "idx_fulfillment_requirement", columnList = "requirement_item_id"),
                @Index(name = "idx_fulfillment_status", columnList = "status")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RequirementFulfillmentMap extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bid_request_id", nullable = false, columnDefinition = "BINARY(16)")
    private BidRequest bidRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requirement_item_id", nullable = false, columnDefinition = "BINARY(16)")
    private OpportunityRequirementItem requirementItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "fulfillment_type", nullable = false, length = 30)
    private FulfillmentType fulfillmentType;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", columnDefinition = "BINARY(16)")
    private BidDocument document;

    /** 고객 업로드 서류로 충족 시 (CR-010) */
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_document_id", columnDefinition = "BINARY(16)")
    private ClientDocument clientDocument;

    @Column(name = "document_section_path", columnDefinition = "TEXT")
    private String documentSectionPath;

    @Column(name = "attachment_reference", columnDefinition = "TEXT")
    private String attachmentReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private FulfillmentStatus status = FulfillmentStatus.PENDING;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "mapped_by", columnDefinition = "BINARY(16)")
    private UUID mappedBy;

    @Column(name = "mapped_at")
    private LocalDateTime mappedAt;

    // Business methods

    /**
     * Mark as fulfilled
     */
    public void markFulfilled(UUID userId, String notes) {
        this.status = FulfillmentStatus.FULFILLED;
        this.mappedBy = userId;
        this.mappedAt = LocalDateTime.now();
        if (notes != null) {
            this.notes = notes;
        }
    }

    /**
     * Mark as missing
     */
    public void markMissing(String notes) {
        this.status = FulfillmentStatus.MISSING;
        this.notes = notes;
    }

    /**
     * Update fulfillment details
     */
    public void updateFulfillment(FulfillmentType type, UUID documentId,
                                   String sectionPath, String attachmentRef,
                                   String notes, UUID userId) {
        this.fulfillmentType = type;
        this.documentSectionPath = sectionPath;
        this.attachmentReference = attachmentRef;
        this.notes = notes;
        this.mappedBy = userId;
        this.mappedAt = LocalDateTime.now();

        // Auto-mark as fulfilled if type is valid
        if (type != FulfillmentType.MISSING) {
            this.status = FulfillmentStatus.FULFILLED;
        }
    }

    /**
     * 제안서 절 검증이 이 요구를 답했는지 남긴다 (CR-159, BIZ-077)
     */
    public void recordSectionAnswer(BidDocument document, String sectionId, boolean answered,
                                    String notes, LocalDateTime recordedAt) {
        this.fulfillmentType = FulfillmentType.DOCUMENT_SECTION;
        this.document = document;
        this.clientDocument = null;
        this.documentSectionPath = sectionId;
        this.status = answered ? FulfillmentStatus.FULFILLED : FulfillmentStatus.MISSING;
        this.notes = notes;
        this.mappedBy = null;
        this.mappedAt = recordedAt;
    }

    /** CR-159: 이 제안서 문서의 절로 충족되었나 */
    public boolean isFulfilledByDocument(UUID documentId) {
        return status == FulfillmentStatus.FULFILLED
                && fulfillmentType == FulfillmentType.DOCUMENT_SECTION
                && document != null && documentId.equals(document.getId());
    }

    /** CR-159: 그 절이 아닌 곳(다른 절, 다른 문서, 고객 서류)으로 이미 충족되었나 */
    public boolean isFulfilledElsewhere(UUID documentId, String sectionId) {
        return status == FulfillmentStatus.FULFILLED
                && !(isFulfilledByDocument(documentId) && sectionId.equals(documentSectionPath));
    }

    /**
     * Fulfill this slot with a client-uploaded document (CR-010)
     */
    public void fulfillWithClientDocument(ClientDocument clientDocument, UUID userId) {
        this.fulfillmentType = FulfillmentType.CLIENT_DOCUMENT;
        this.clientDocument = clientDocument;
        this.status = FulfillmentStatus.FULFILLED;
        this.mappedBy = userId;
        this.mappedAt = LocalDateTime.now();
    }
}
