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
    COVER_LETTER("Cover Letter", "Introduction and summary", false),

    /**
     * Technical proposal detailing approach and methodology
     */
    TECHNICAL_PROPOSAL("Technical Proposal", "Approach, methodology, and technical details", true),

    /**
     * Past performance examples and references
     */
    PAST_PERFORMANCE("Past Performance", "Previous contract successes and references", true),

    /**
     * Company profile and qualifications
     */
    COMPANY_PROFILE("Company Profile", "Company overview, certifications, and qualifications", false),

    /**
     * Compliance matrix mapping requirements to responses
     */
    COMPLIANCE_MATRIX("Compliance Matrix", "Requirement fulfillment mapping", false),

    /**
     * Pricing summary and cost breakdown
     */
    PRICING_SUMMARY("Pricing Summary", "Cost proposal and pricing details", true),

    /**
     * Other supporting documents
     */
    OTHER("Other", "Additional supporting documents", false),

    /**
     * 공고문 표시 양식 (CR-021).
     * 제안서 생성용이 아닌, 공고문(Notice) 상세 화면을 그릴 때 사용하는 TipTap JSON 골격.
     * Aimbase 한글화 워크플로우에 함께 입력되어 LLM이 이 골격을 채워 반환.
     */
    NOTICE_VIEW("Notice View", "공고문 표시 양식 (TipTap JSON 골격)", false);

    private final String displayName;
    private final String description;
    private final boolean evaluated;

    DocumentType(String displayName, String description, boolean evaluated) {
        this.displayName = displayName;
        this.description = description;
        this.evaluated = evaluated;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * BIZ-095: 평가받는 문서만 목차에 평가 항목과 대응 REQ ID 를 건다.
     * 커버레터가 매트릭스를 받아 요구사항 충족을 먼저 가져가면 기술 제안서의 절 검증 결과가 남지 않는다(CR-178)
     */
    public boolean isEvaluated() {
        return evaluated;
    }
}
