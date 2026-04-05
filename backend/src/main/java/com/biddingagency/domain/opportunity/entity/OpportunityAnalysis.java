package com.biddingagency.domain.opportunity.entity;

import com.biddingagency.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "opportunity_analysis",
        indexes = {
                @Index(name = "idx_opp_analysis_status", columnList = "status")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OpportunityAnalysis extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunity_id", nullable = false, unique = true, columnDefinition = "BINARY(16)")
    private Opportunity opportunity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AnalysisStatus status = AnalysisStatus.PENDING;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "summary_json", columnDefinition = "JSON")
    private Map<String, Object> summaryJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "document_formats_json", columnDefinition = "JSON")
    private Map<String, Object> documentFormatsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "required_documents_json", columnDefinition = "JSON")
    private Map<String, Object> requiredDocumentsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "llm_prompt_preset_json", columnDefinition = "JSON")
    private Map<String, Object> llmPromptPresetJson;

    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "workflow_run_id")
    private String workflowRunId;

    // Business methods

    public void markAnalyzing(String workflowRunId) {
        this.status = AnalysisStatus.ANALYZING;
        this.workflowRunId = workflowRunId;
        this.errorMessage = null;
    }

    public void markCompleted(Map<String, Object> summaryJson,
                              Map<String, Object> documentFormatsJson,
                              Map<String, Object> requiredDocumentsJson,
                              Map<String, Object> llmPromptPresetJson) {
        this.status = AnalysisStatus.COMPLETED;
        this.summaryJson = summaryJson;
        this.documentFormatsJson = documentFormatsJson;
        this.requiredDocumentsJson = requiredDocumentsJson;
        this.llmPromptPresetJson = llmPromptPresetJson;
        this.analyzedAt = LocalDateTime.now();
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.status = AnalysisStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public void resetForReanalysis() {
        this.status = AnalysisStatus.PENDING;
        this.errorMessage = null;
        this.workflowRunId = null;
    }

    public void updateAnalysisResult(Map<String, Object> summaryJson,
                                     Map<String, Object> documentFormatsJson,
                                     Map<String, Object> requiredDocumentsJson,
                                     Map<String, Object> llmPromptPresetJson) {
        this.summaryJson = summaryJson;
        this.documentFormatsJson = documentFormatsJson;
        this.requiredDocumentsJson = requiredDocumentsJson;
        this.llmPromptPresetJson = llmPromptPresetJson;
    }
}
