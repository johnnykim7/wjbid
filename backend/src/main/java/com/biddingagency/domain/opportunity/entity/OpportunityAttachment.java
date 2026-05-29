package com.biddingagency.domain.opportunity.entity;

import com.biddingagency.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "opportunity_attachments",
        indexes = {
                @Index(name = "idx_opp_attachments_opportunity", columnList = "opportunity_id"),
                @Index(name = "idx_opp_attachments_status", columnList = "download_status")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OpportunityAttachment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunity_id", nullable = false, columnDefinition = "BINARY(16)")
    private Opportunity opportunity;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "source_url", columnDefinition = "TEXT", nullable = false)
    private String sourceUrl;

    @Column(name = "storage_url", columnDefinition = "TEXT")
    private String storageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "download_status", nullable = false, length = 30)
    @Builder.Default
    private AttachmentDownloadStatus downloadStatus = AttachmentDownloadStatus.FAILED;

    @Column(name = "downloaded_at")
    private LocalDateTime downloadedAt;

    public void markDownloaded(String storageUrl) {
        this.storageUrl = storageUrl;
        this.downloadStatus = AttachmentDownloadStatus.SUCCESS;
        this.downloadedAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.downloadStatus = AttachmentDownloadStatus.FAILED;
    }

    public void markLinkOnly() {
        this.downloadStatus = AttachmentDownloadStatus.LINK_ONLY;
    }

    /** CR-019: 외부 사이트 첨부 — 관리자가 외부서 직접 가져와야 함 표식 */
    public void markManualFetchRequired() {
        this.downloadStatus = AttachmentDownloadStatus.MANUAL_FETCH_REQUIRED;
    }

    /** CR-019: 관리자 수동 업로드로 실제 파일 정보 확정 */
    public void applyUpload(String fileName, Long fileSize, String contentType, String storageUrl) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.contentType = contentType;
        markDownloaded(storageUrl);
    }
}
