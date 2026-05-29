package com.biddingagency.domain.notification.entity;

/**
 * 알림 유형. 각 유형은 bp-notification에 등록된 BIDDING 솔루션 템플릿 코드와 매핑된다 (CR-005/006).
 */
public enum NotificationType {

    COLLECTION_COMPLETE("BID_COLLECTION_DONE"),
    DOCUMENT_GENERATED("BID_DOC_GENERATED"),
    DEADLINE_D7("BID_DEADLINE_ALERT"),
    DEADLINE_D3("BID_DEADLINE_ALERT"),
    DEADLINE_D1("BID_DEADLINE_ALERT"),
    BID_REQUEST_CREATED("BID_REQUEST_ADMIN"),
    AI_WORKFLOW_FAILED("BID_ANALYSIS_DONE"),
    OPPORTUNITY_ANALYSIS_COMPLETED("BID_ANALYSIS_DONE"),
    // CR-006: 고객 대상 신규 공고 등록 알림
    OPPORTUNITY_APPROVED("BID_OPPORTUNITY_APPROVED"),
    // CR-018: 고객 대상 진행 알림 3종 (기존 bp-notification 템플릿 재사용 — 전용 템플릿은 후속 운영작업)
    PROPOSAL_READY_CUSTOMER("BID_DOC_GENERATED"),
    BID_SUBMITTED_CUSTOMER("BID_REQUEST_ADMIN"),
    BID_AWARDED_CUSTOMER("BID_OPPORTUNITY_APPROVED");

    private final String templateCode;

    NotificationType(String templateCode) {
        this.templateCode = templateCode;
    }

    /**
     * bp-notification 발송 시 사용할 템플릿 코드.
     */
    public String getTemplateCode() {
        return templateCode;
    }
}
