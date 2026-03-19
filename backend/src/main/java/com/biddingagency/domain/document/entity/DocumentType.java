package com.biddingagency.domain.document.entity;

/**
 * Document Type Enum
 *
 * Types of documents generated for bid submissions
 */
public enum DocumentType {
    /**
     * Cover letter introducing the proposal
     */
    COVER_LETTER("Cover Letter", "Introduction and summary"),

    /**
     * Technical proposal detailing approach and methodology
     */
    TECHNICAL_PROPOSAL("Technical Proposal", "Approach, methodology, and technical details"),

    /**
     * Past performance examples and references
     */
    PAST_PERFORMANCE("Past Performance", "Previous contract successes and references"),

    /**
     * Company profile and qualifications
     */
    COMPANY_PROFILE("Company Profile", "Company overview, certifications, and qualifications"),

    /**
     * Compliance matrix mapping requirements to responses
     */
    COMPLIANCE_MATRIX("Compliance Matrix", "Requirement fulfillment mapping"),

    /**
     * Pricing summary and cost breakdown
     */
    PRICING_SUMMARY("Pricing Summary", "Cost proposal and pricing details"),

    /**
     * Other supporting documents
     */
    OTHER("Other", "Additional supporting documents");

    private final String displayName;
    private final String description;

    DocumentType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
