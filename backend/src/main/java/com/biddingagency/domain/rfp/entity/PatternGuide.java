package com.biddingagency.domain.rfp.entity;

import com.biddingagency.common.BaseEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 슬롯별 패턴 가이드 (CR-013).
 * 추출 단위 = 슬롯(BIZ-016). 출처 보호 = source가 HUMAN_*이면 자동추출 보호(BIZ-017).
 */
@Entity
@Table(name = "pattern_guide",
        indexes = {
                @Index(name = "idx_pattern_guide_status", columnList = "status")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PatternGuide extends BaseEntity {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_definition_id", nullable = false, columnDefinition = "BINARY(16)")
    private SlotDefinition slotDefinition;

    /** 사업유형 — 1차엔 NULL(공통) 허용 */
    @Enumerated(EnumType.STRING)
    @Column(name = "industry_type", length = 30)
    private IndustryType industryType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ExtractionStatus status = ExtractionStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    @Builder.Default
    private GuideSource source = GuideSource.AI_EXTRACTED;

    /** 구조화 가이드 (8블록 골격/체크리스트/금기 — LLM 입력용) */
    @Column(name = "guide_json", columnDefinition = "longtext")
    private String guideJsonRaw;

    /** 편집용 본문 (사람 편집 위주) */
    @Column(name = "guide_markdown", columnDefinition = "longtext")
    private String guideMarkdown;

    @Column(name = "sample_count")
    private Integer sampleCount;

    @Column(name = "extracted_at")
    private LocalDateTime extractedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "workflow_run_id")
    private String workflowRunId;

    // JSON accessor

    public Map<String, Object> getGuideJson() {
        if (guideJsonRaw == null || guideJsonRaw.isBlank()) return null;
        try {
            return OBJECT_MAPPER.readValue(guideJsonRaw, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse guide JSON", e);
        }
    }

    private static String toJsonString(Map<String, Object> map) {
        if (map == null) return null;
        try {
            return OBJECT_MAPPER.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize guide JSON", e);
        }
    }

    // Business methods

    /** BIZ-017: 사람 편집분(HUMAN_*)은 AI 자동추출이 덮어쓰지 않음 */
    public boolean canAutoUpdate() {
        return this.source == GuideSource.AI_EXTRACTED;
    }

    public void markExtracting(String workflowRunId) {
        this.status = ExtractionStatus.EXTRACTING;
        this.workflowRunId = workflowRunId;
        this.errorMessage = null;
    }

    public void markCompleted(Map<String, Object> guideJson, Integer sampleCount) {
        this.status = ExtractionStatus.COMPLETED;
        this.guideJsonRaw = toJsonString(guideJson);
        this.sampleCount = sampleCount;
        this.source = GuideSource.AI_EXTRACTED;
        this.extractedAt = LocalDateTime.now();
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.status = ExtractionStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    /** 관리자 수동 편집 → 출처를 HUMAN_EDITED로 전환(자동추출 보호) */
    public void applyManualEdit(Map<String, Object> guideJson, String guideMarkdown) {
        if (guideJson != null) {
            this.guideJsonRaw = toJsonString(guideJson);
        }
        if (guideMarkdown != null) {
            this.guideMarkdown = guideMarkdown;
        }
        this.source = GuideSource.HUMAN_EDITED;
        this.status = ExtractionStatus.COMPLETED;
    }
}
