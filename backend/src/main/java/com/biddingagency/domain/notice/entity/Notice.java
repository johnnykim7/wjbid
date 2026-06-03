package com.biddingagency.domain.notice.entity;

import com.biddingagency.common.BaseEntity;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.OpportunityVisibility;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Notice (공고문) — CR-016.
 *
 * 원본(Opportunity, SAM 수집물)에서 관리자가 선별해 한글화+요약한 산출물.
 * 원본 1:N 공고문. 원본은 손대지 않고 보존, 공고문은 별개 엔티티로 노출/비노출 관리.
 * 구 OpportunityAnalysis(한글화+요약 결과)를 흡수.
 *
 * - generationStatus: 한글화 생성 진행 상태 (PENDING→ANALYZING→COMPLETED/FAILED)
 * - visibility: 고객 노출 상태 (HIDDEN/VISIBLE) — 생성 상태와 별개 축
 */
@Entity
@Table(name = "notices",
        indexes = {
                @Index(name = "idx_notices_opportunity", columnList = "opportunity_id"),
                @Index(name = "idx_notices_generation_status", columnList = "generation_status"),
                @Index(name = "idx_notices_visibility", columnList = "visibility")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Notice extends BaseEntity {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunity_id", nullable = false, columnDefinition = "BINARY(16)")
    private Opportunity opportunity;

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_status", nullable = false, length = 20)
    @Builder.Default
    private NoticeGenerationStatus generationStatus = NoticeGenerationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    @Builder.Default
    private OpportunityVisibility visibility = OpportunityVisibility.HIDDEN;

    @Column(name = "korean_title", length = 500)
    private String koreanTitle;

    @Column(name = "summary_json", columnDefinition = "longtext")
    private String summaryJsonRaw;

    @Column(name = "document_formats_json", columnDefinition = "longtext")
    private String documentFormatsJsonRaw;

    @Column(name = "required_documents_json", columnDefinition = "longtext")
    private String requiredDocumentsJsonRaw;

    @Column(name = "llm_prompt_preset_json", columnDefinition = "longtext")
    private String llmPromptPresetJsonRaw;

    /** CR-021: TipTap JSON 본문 — PDF 양식 풍부도 재현용 (사람이 읽는 본문) */
    @Column(name = "content_json", columnDefinition = "longtext")
    private String contentJsonRaw;

    /** CR-028: Section L/M 에서 도출한 FACTOR/Subfactor 트리 (proposal-design 입력). NULL 가능 */
    @Column(name = "factor_tree_json", columnDefinition = "longtext")
    private String factorTreeJsonRaw;

    /** CR-028: Price 항목·수량·단위 추천 (단가 제외). NULL 가능 */
    @Column(name = "price_items_json", columnDefinition = "longtext")
    private String priceItemsJsonRaw;

    /**
     * CR-038: 공고문 분석 추출 사실의 근거(JSON 배열). 각 원소 = {key,value,sourceFile,sourceQuote,page,confidence}.
     * markCompleted 에 포함시켜 전체치환 — 교정 채팅(CR-033)이 Notice 통째 덮어쓸 때 facts 도 함께 보존되도록.
     */
    @Column(name = "extracted_facts_json", columnDefinition = "longtext")
    private String extractedFactsJsonRaw;

    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "workflow_run_id")
    private String workflowRunId;

    /** 공고문을 만든 관리자 */
    @Column(name = "created_by", columnDefinition = "BINARY(16)")
    private UUID createdBy;

    // JSON accessor methods

    public Map<String, Object> getSummaryJson() {
        return parseJson(summaryJsonRaw);
    }

    public Map<String, Object> getDocumentFormatsJson() {
        return parseJson(documentFormatsJsonRaw);
    }

    public Map<String, Object> getRequiredDocumentsJson() {
        return parseJson(requiredDocumentsJsonRaw);
    }

    public Map<String, Object> getLlmPromptPresetJson() {
        return parseJson(llmPromptPresetJsonRaw);
    }

    /** CR-021: TipTap JSON 본문 (PDF 양식 풍부도) */
    public Map<String, Object> getContentJson() {
        return parseJson(contentJsonRaw);
    }

    /** CR-028: FACTOR/Subfactor 트리 (proposal-design 입력) */
    public Map<String, Object> getFactorTreeJson() {
        return parseJson(factorTreeJsonRaw);
    }

    /** CR-028: Price 항목·수량·단위 추천 (단가 제외) */
    public Map<String, Object> getPriceItemsJson() {
        return parseJson(priceItemsJsonRaw);
    }

    /** CR-038: 추출 사실 목록(JSON 배열). 근거 추적·교정 채팅용. NULL 가능 */
    public java.util.List<Map<String, Object>> getExtractedFactsJson() {
        return parseJsonList(extractedFactsJsonRaw);
    }

    private Map<String, Object> parseJson(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return OBJECT_MAPPER.readValue(raw, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }

    private java.util.List<Map<String, Object>> parseJsonList(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return OBJECT_MAPPER.readValue(raw, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse JSON list", e);
        }
    }

    private String toJsonString(Map<String, Object> map) {
        if (map == null) return null;
        try {
            return OBJECT_MAPPER.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize JSON", e);
        }
    }

    private String toJsonString(java.util.List<Map<String, Object>> list) {
        if (list == null) return null;
        try {
            return OBJECT_MAPPER.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize JSON list", e);
        }
    }

    // Business methods — 생성(한글화) 축

    public void markAnalyzing(String workflowRunId) {
        this.generationStatus = NoticeGenerationStatus.ANALYZING;
        this.workflowRunId = workflowRunId;
        this.errorMessage = null;
    }

    public void markCompleted(String koreanTitle,
                              Map<String, Object> summaryJson,
                              Map<String, Object> documentFormatsJson,
                              Map<String, Object> requiredDocumentsJson,
                              Map<String, Object> llmPromptPresetJson,
                              Map<String, Object> contentJson,
                              java.util.List<Map<String, Object>> extractedFactsJson) {
        this.generationStatus = NoticeGenerationStatus.COMPLETED;
        this.koreanTitle = koreanTitle;
        this.summaryJsonRaw = toJsonString(summaryJson);
        this.documentFormatsJsonRaw = toJsonString(documentFormatsJson);
        this.requiredDocumentsJsonRaw = toJsonString(requiredDocumentsJson);
        this.llmPromptPresetJsonRaw = toJsonString(llmPromptPresetJson);
        this.contentJsonRaw = toJsonString(contentJson);
        // CR-038: facts 전체치환. null 이면 기존값 유지(교정 채팅이 facts 안 보낸 경우 보존 — get 이 반환하므로 보통은 옴).
        if (extractedFactsJson != null) {
            this.extractedFactsJsonRaw = toJsonString(extractedFactsJson);
        }
        this.analyzedAt = LocalDateTime.now();
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.generationStatus = NoticeGenerationStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public void resetForRegeneration() {
        this.generationStatus = NoticeGenerationStatus.PENDING;
        this.errorMessage = null;
        this.workflowRunId = null;
    }

    public void updateResult(String koreanTitle,
                             Map<String, Object> summaryJson,
                             Map<String, Object> documentFormatsJson,
                             Map<String, Object> requiredDocumentsJson,
                             Map<String, Object> llmPromptPresetJson,
                             Map<String, Object> contentJson) {
        if (koreanTitle != null) this.koreanTitle = koreanTitle;
        this.summaryJsonRaw = toJsonString(summaryJson);
        this.documentFormatsJsonRaw = toJsonString(documentFormatsJson);
        this.requiredDocumentsJsonRaw = toJsonString(requiredDocumentsJson);
        this.llmPromptPresetJsonRaw = toJsonString(llmPromptPresetJson);
        if (contentJson != null) this.contentJsonRaw = toJsonString(contentJson);
    }

    /**
     * CR-028: 정제 시 도출한 FACTOR 트리 / Price 항목을 저장.
     * 정제(NoticeService) 가 Section L/M 에서 도출하지 못하면 호출하지 않음 → NULL 유지,
     * 그 경우 proposal-design WF 가 공고 본문에서 직접 발췌한다.
     */
    public void updateFactorTree(Map<String, Object> factorTreeJson, Map<String, Object> priceItemsJson) {
        if (factorTreeJson != null) this.factorTreeJsonRaw = toJsonString(factorTreeJson);
        if (priceItemsJson != null) this.priceItemsJsonRaw = toJsonString(priceItemsJson);
    }

    // Business methods — 노출 축 (게이트②)

    /** 검수 완료 → 고객 노출. 한글화가 완료된 공고문만 노출 가능 (BIZ). */
    public void publish() {
        this.visibility = OpportunityVisibility.VISIBLE;
    }

    public void hide() {
        this.visibility = OpportunityVisibility.HIDDEN;
    }

    public boolean isVisible() {
        return this.visibility == OpportunityVisibility.VISIBLE;
    }

    public boolean isGenerationCompleted() {
        return this.generationStatus == NoticeGenerationStatus.COMPLETED;
    }
}
