package com.biddingagency.domain.compliance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Validation Result DTO
 *
 * Result of compliance validation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationResult {

    public enum Status {
        PASS,      // No issues
        WARNING,   // Has warnings but no blockers
        BLOCKER    // Has blocking issues
    }

    /**
     * Overall validation status
     */
    private Status status;

    /**
     * Blocking issues (must be resolved before submission)
     */
    private List<ComplianceIssue> blockers;

    /**
     * Warning issues (should be reviewed but not blocking)
     */
    private List<ComplianceIssue> warnings;

    /**
     * Fulfillment rate (percentage of requirements fulfilled)
     */
    private double fulfillmentRate;

    /**
     * Total requirements count
     */
    private int totalRequirements;

    /**
     * Fulfilled requirements count
     */
    private int fulfilledRequirements;

    /**
     * Validation timestamp
     */
    private LocalDateTime validatedAt;

    /**
     * Check if validation passed
     */
    public boolean hasPassed() {
        return status == Status.PASS;
    }

    /**
     * Check if has blockers
     */
    public boolean hasBlockers() {
        return status == Status.BLOCKER;
    }

    /**
     * Get blocker count
     */
    public int getBlockerCount() {
        return blockers != null ? blockers.size() : 0;
    }

    /**
     * Get warning count
     */
    public int getWarningCount() {
        return warnings != null ? warnings.size() : 0;
    }
}
