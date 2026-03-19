package com.biddingagency.domain.compliance.entity;

/**
 * Fulfillment Status Enum
 *
 * Status of requirement fulfillment
 */
public enum FulfillmentStatus {
    /**
     * Fulfillment is pending review/mapping
     */
    PENDING,

    /**
     * Requirement is fulfilled
     */
    FULFILLED,

    /**
     * Requirement is not fulfilled (BLOCKER if isBlocker = true)
     */
    MISSING;

    /**
     * Check if status is complete
     */
    public boolean isComplete() {
        return this == FULFILLED;
    }
}
