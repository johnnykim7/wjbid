package com.biddingagency.domain.opportunity.entity;

import com.biddingagency.common.BaseEntity;
import com.biddingagency.domain.rfp.entity.IndustryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Opportunity entity (공고)
 *
 * Represents a contract opportunity from SAM.gov
 */
@Entity
@Table(name = "opportunities",
        indexes = {
                @Index(name = "idx_opportunities_notice_id", columnList = "notice_id"),
                @Index(name = "idx_opportunities_posted_date", columnList = "posted_date"),
                @Index(name = "idx_opportunities_deadline", columnList = "response_deadline"),
                @Index(name = "idx_opportunities_active", columnList = "active"),
                @Index(name = "idx_opportunities_industry_type", columnList = "industry_type")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Opportunity extends BaseEntity {

    @Column(name = "notice_id", nullable = false, unique = true)
    private String noticeId;

    @Column(name = "solicitation_number")
    private String solicitationNumber;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "type", length = 100)
    private String type;

    @Column(name = "organization_name")
    private String organizationName;

    /** 사업유형 자동분류 (CR-014, BIZ-018). null = 미분류. 성공 자산 매칭 키 */
    @Enumerated(EnumType.STRING)
    @Column(name = "industry_type", length = 30)
    private IndustryType industryType;

    @Column(name = "posted_date")
    private LocalDateTime postedDate;

    @Column(name = "response_deadline")
    private LocalDateTime responseDeadline;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "ui_link", columnDefinition = "TEXT")
    private String uiLink;

    @Column(name = "description_link", columnDefinition = "TEXT")
    private String descriptionLink;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "first_seen_at", nullable = false)
    private LocalDateTime firstSeenAt;

    @Column(name = "last_modified_at", nullable = false)
    private LocalDateTime lastModifiedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_json", columnDefinition = "JSON")
    private Map<String, Object> rawJson;

    // Business methods

    public void updateContent(String title, String type, String organizationName,
                              LocalDateTime postedDate, LocalDateTime responseDeadline,
                              String uiLink, String descriptionLink,
                              Map<String, Object> rawJson, String contentHash) {
        this.title = title;
        this.type = type;
        this.organizationName = organizationName;
        this.postedDate = postedDate;
        this.responseDeadline = responseDeadline;
        this.uiLink = uiLink;
        this.descriptionLink = descriptionLink;
        this.rawJson = rawJson;
        this.contentHash = contentHash;
        this.lastModifiedAt = LocalDateTime.now();
    }

    public void markAsInactive() {
        this.active = false;
        this.lastModifiedAt = LocalDateTime.now();
    }

    /** 자동분류 결과 반영 (CR-014). null이면 미분류로 둔다 */
    public void assignIndustryType(IndustryType industryType) {
        this.industryType = industryType;
    }

    public boolean isDeadlinePassed() {
        return responseDeadline != null && responseDeadline.isBefore(LocalDateTime.now());
    }

    public boolean isDeadlineNear(int days) {
        return responseDeadline != null
                && responseDeadline.isAfter(LocalDateTime.now())
                && responseDeadline.isBefore(LocalDateTime.now().plusDays(days));
    }
}
