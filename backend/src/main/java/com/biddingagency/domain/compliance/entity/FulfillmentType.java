package com.biddingagency.domain.compliance.entity;

/**
 * Fulfillment Type Enum
 *
 * How a requirement is fulfilled
 */
public enum FulfillmentType {
    /**
     * Requirement is addressed in a document section
     */
    DOCUMENT_SECTION,

    /**
     * Requirement is fulfilled by an attachment
     */
    ATTACHMENT,

    /**
     * Requirement is fulfilled by a client-uploaded document (CR-010)
     */
    CLIENT_DOCUMENT,

    /**
     * Requirement is not applicable to this bid
     */
    NOT_APPLICABLE,

    /**
     * Requirement is not yet fulfilled
     */
    MISSING;

    /**
     * Check if fulfillment is complete
     */
    public boolean isFulfilled() {
        return this == DOCUMENT_SECTION || this == ATTACHMENT
                || this == CLIENT_DOCUMENT || this == NOT_APPLICABLE;
    }
}
