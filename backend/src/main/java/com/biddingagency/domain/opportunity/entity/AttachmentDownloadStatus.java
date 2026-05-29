package com.biddingagency.domain.opportunity.entity;

public enum AttachmentDownloadStatus {
    SUCCESS,
    FAILED,
    LINK_ONLY,
    /** CR-019: 외부 사이트 첨부 — 관리자가 외부서 직접 가져와 업로드해야 함 */
    MANUAL_FETCH_REQUIRED
}
