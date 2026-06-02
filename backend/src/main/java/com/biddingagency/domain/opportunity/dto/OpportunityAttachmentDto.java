package com.biddingagency.domain.opportunity.dto;

import com.biddingagency.domain.opportunity.entity.AttachmentDownloadStatus;
import com.biddingagency.domain.opportunity.entity.OpportunityAttachment;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 관리자 원본 공고 첨부 DTO — CR-019.
 * downloadStatus(특히 MANUAL_FETCH_REQUIRED = "가져와야 함")와 외부 원본 링크(sourceUrl)를 노출.
 */
@Data
@Builder
public class OpportunityAttachmentDto {
    private UUID id;
    private String fileName;
    private Long fileSize;
    private String contentType;
    /** 외부 원본 다운로드 링크 (관리자가 여기서 받아 업로드). admin-upload면 수동 업로드분 */
    private String sourceUrl;
    private AttachmentDownloadStatus downloadStatus;
    /** 외부서 직접 가져와야 하는 첨부 여부 (표식) */
    private boolean manualFetchRequired;
    /** CR-025: 자동 다운로드 가능 URL인지 (sam.gov 자체호스팅) → FE 재시도 버튼 노출 판단 */
    private boolean autoFetchable;
    /** CR-025: 자동 다운로드 실패 사유 (FAILED 상태일 때) */
    private String failureReason;
    private LocalDateTime downloadedAt;

    public static OpportunityAttachmentDto from(OpportunityAttachment a) {
        return from(a, false);
    }

    public static OpportunityAttachmentDto from(OpportunityAttachment a, boolean autoFetchable) {
        return OpportunityAttachmentDto.builder()
                .id(a.getId())
                .fileName(a.getFileName())
                .fileSize(a.getFileSize())
                .contentType(a.getContentType())
                .sourceUrl(a.getSourceUrl())
                .downloadStatus(a.getDownloadStatus())
                .manualFetchRequired(a.getDownloadStatus() == AttachmentDownloadStatus.MANUAL_FETCH_REQUIRED)
                .autoFetchable(autoFetchable)
                .failureReason(a.getFailureReason())
                .downloadedAt(a.getDownloadedAt())
                .build();
    }
}
