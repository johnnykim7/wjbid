package com.biddingagency.domain.document.entity;

/**
 * Document Status Enum
 *
 * Status lifecycle for bid documents
 */
public enum DocumentStatus {
    /**
     * Document is being drafted/edited
     */
    DRAFT,

    /**
     * Document is ready for review
     */
    READY,

    /**
     * Document is approved by reviewer
     */
    APPROVED,

    /**
     * Document is locked (immutable after submission)
     * Cannot be edited - must create amendment instead
     */
    LOCKED;

    /**
     * Check if document can be edited
     */
    public boolean canEdit() {
        return this == DRAFT || this == READY || this == APPROVED;
    }

    /**
     * Check if document is final/immutable
     */
    public boolean isImmutable() {
        return this == LOCKED;
    }
}
