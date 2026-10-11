package com.biddingagency.domain.member.entity;

import com.biddingagency.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 회사 공통 서류함 문서 (CR-125)
 *
 * member 종속 — 사업자등록증·지명원 등 회사 공통 서류를 한 번 보관 후 매 공고 재사용.
 * ClientDocument(BidRequest 종속, "이 공고에 제출할 서류")와 별개.
 */
@Entity
@Table(name = "member_documents",
        indexes = {
                @Index(name = "idx_member_documents_member", columnList = "member_id"),
                @Index(name = "idx_member_documents_member_category", columnList = "member_id, category")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MemberDocument extends BaseEntity {

    @Column(name = "member_id", nullable = false, columnDefinition = "BINARY(16)")
    private java.util.UUID memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 40)
    private DocumentCategory category;

    /** 국문·영문 칸만 값이 있다(CR-147, BIZ-065) */
    @Enumerated(EnumType.STRING)
    @Column(name = "language", length = 2)
    private DocumentLanguage language;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "storage_url", columnDefinition = "TEXT", nullable = false)
    private String storageUrl;

    /** 문서제목 — 고정=카테고리 한글명, 기타=사용자 입력 */
    @Column(name = "display_name", length = 255)
    private String displayName;

    /** 설명 메모 — 기타(OTHER) 전용 */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** 만료일(한국 날짜). null = 만료일 없음 (CR-136) */
    @Column(name = "expires_on")
    private java.time.LocalDate expiresOn;

    public void changeExpiry(java.time.LocalDate expiresOn) {
        this.expiresOn = expiresOn;
    }

    /** 파일 교체 (storage_url 갱신 시 기존 파일은 서비스에서 삭제) */
    public void replaceFile(String fileName, Long fileSize, String contentType, String storageUrl) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.storageUrl = storageUrl;
    }

    public void updateMeta(String displayName, String description) {
        if (displayName != null) {
            this.displayName = displayName;
        }
        this.description = description;
    }
}
