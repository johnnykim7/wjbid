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
     * Document is locked (immutable after submission)
     * Cannot be edited - must create amendment instead
     */
    LOCKED,

    /**
     * Document is archived (after submission)
     */
    ARCHIVED;

    /**
     * Check if document can be edited
     */
    public boolean canEdit() {
        return this == DRAFT;
    }

    /**
     * Check if document is final/immutable
     */
    public boolean isImmutable() {
        return this == LOCKED || this == ARCHIVED;
    }
}
