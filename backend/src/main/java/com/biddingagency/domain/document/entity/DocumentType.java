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
    OTHER("Other", "Additional supporting documents"),

    /**
     * 공고문 표시 양식 (CR-021).
     * 제안서 생성용이 아닌, 공고문(Notice) 상세 화면을 그릴 때 사용하는 TipTap JSON 골격.
     * Aimbase 한글화 워크플로우에 함께 입력되어 LLM이 이 골격을 채워 반환.
     */
    NOTICE_VIEW("Notice View", "공고문 표시 양식 (TipTap JSON 골격)");

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
