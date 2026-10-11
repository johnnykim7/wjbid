package com.biddingagency.domain.opportunity.entity;

import com.biddingagency.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

/**
 * Opportunity Requirement Item entity
 *
 * Individual requirement extracted from opportunity
 */
@Entity
@Table(name = "opportunity_requirement_items",
        indexes = {
                @Index(name = "idx_requirement_items_opportunity", columnList = "opportunity_id"),
                @Index(name = "idx_requirement_items_category", columnList = "category"),
                @Index(name = "idx_requirement_items_blocker", columnList = "is_blocker")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OpportunityRequirementItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunity_id", nullable = false, columnDefinition = "BINARY(16)")
    private Opportunity opportunity;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private RequirementCategory category;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Flexible requirement data in JSON format
     * Can include: reference, page_number, source, etc.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "requirement_json", columnDefinition = "JSON", nullable = false)
    private Map<String, Object> requirementJson;

    /**
     * Is this a blocking requirement?
     * If true, must be fulfilled before submission
     */
    @Column(name = "is_blocker", nullable = false)
    @Builder.Default
    private Boolean isBlocker = false;

    /**
     * Has this requirement been verified by human?
     */
    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;

    /** 공고 분석의 제출 서류 순서 (CR-141) */
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    // Business methods

    /**
     * Mark as verified
     */
    public void markVerified() {
        this.isVerified = true;
    }

    /**
     * Mark as blocker
     */
    public void markAsBlocker() {
        this.isBlocker = true;
    }

    /**
     * 공고 분석으로 다시 맞출 때 같은 열쇠 항목을 고쳐 쓴다 (CR-141, BIZ-045)
     */
    public void syncFromAnalysis(String title, String description, Boolean isBlocker,
                                 Map<String, Object> requirementJson, int sortOrder) {
        this.title = title;
        this.description = description;
        this.isBlocker = isBlocker;
        this.requirementJson = requirementJson;
        this.sortOrder = sortOrder;
    }

    /**
     * Update requirement details
     */
    public void update(String title, String description, RequirementCategory category,
                       Boolean isBlocker, Map<String, Object> requirementJson) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.isBlocker = isBlocker;
        this.requirementJson = requirementJson;
    }
}
