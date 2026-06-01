package com.biddingagency.domain.proposal.entity;

import com.biddingagency.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Proposal Attachment — Price XLSX·기타 첨부 (CR-027).
 *
 * 실측: 이 코드베이스는 attachment_storage 테이블 없이 StorageService(MinIO/Local) 경로 직접 보관.
 * → file_id FK 대신 storageKey 로 보관 (사용자 결정 2026-06-01).
 */
@Entity
@Table(name = "proposal_attachment",
        indexes = {
                @Index(name = "idx_proposal_attachment_document", columnList = "document_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ProposalAttachment extends BaseEntity {

    @Column(name = "document_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID documentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 30)
    @Builder.Default
    private AttachmentFileType fileType = AttachmentFileType.OTHER_ATTACHMENT;

    /** StorageService key/path (MinIO 또는 Local) */
    @Column(name = "storage_key", nullable = false, length = 1024)
    private String storageKey;

    @Column(name = "original_filename", length = 500)
    private String originalFilename;

    @Column(name = "content_type", length = 255)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "uploaded_by", columnDefinition = "BINARY(16)")
    private UUID uploadedBy;
}
