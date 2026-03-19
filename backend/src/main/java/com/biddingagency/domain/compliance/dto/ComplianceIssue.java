package com.biddingagency.domain.compliance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Compliance Issue DTO
 *
 * Represents a validation issue (warning or blocker)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplianceIssue {

    /**
     * Issue severity
     */
    public enum Severity {
        WARNING,
        BLOCKER
    }

    /**
     * Severity level
     */
    private Severity severity;

    /**
     * Issue category
     */
    private String category;

    /**
     * Related requirement ID (if applicable)
     */
    private UUID requirementItemId;

    /**
     * Requirement title
     */
    private String requirementTitle;

    /**
     * Issue description
     */
    private String message;

    /**
     * Suggested action
     */
    private String suggestedAction;

    /**
     * Check if this is a blocker
     */
    public boolean isBlocker() {
        return severity == Severity.BLOCKER;
    }
}
