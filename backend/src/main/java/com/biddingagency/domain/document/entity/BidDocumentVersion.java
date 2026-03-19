package com.biddingagency.domain.document.entity;

import com.biddingagency.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Bid Document Version entity
 *
 * Immutable document version. Once created, cannot be modified.
 */
@Entity
@Table(name = "bid_document_versions",
        indexes = {
                @Index(name = "idx_bid_document_versions_document", columnList = "document_id"),
                @Index(name = "idx_bid_document_versions_version", columnList = "version_no")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_document_version", columnNames = {"document_id", "version_no"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BidDocumentVersion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false, columnDefinition = "BINARY(16)")
    private BidDocument document;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    /**
     * Content in JSON format (TipTap editor format)
     * Structure: {type: "doc", content: [...]}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_json", columnDefinition = "JSON", nullable = false)
    private Map<String, Object> contentJson;

    @Column(name = "edited_by", nullable = false, columnDefinition = "BINARY(16)")
    private UUID editedBy;

    @Column(name = "edited_at", nullable = false)
    private LocalDateTime editedAt;

    @Column(name = "change_summary", columnDefinition = "TEXT")
    private String changeSummary;

    @Column(name = "word_count")
    private Integer wordCount;

    // No setters - immutable once created

    /**
     * Calculate word count from content JSON
     */
    public static int calculateWordCount(Map<String, Object> contentJson) {
        // TODO: Implement actual word counting from TipTap JSON
        // For now, rough estimate
        String jsonString = contentJson.toString();
        return jsonString.split("\\s+").length;
    }

    /**
     * Get version label for display
     */
    public String getVersionLabel() {
        return "v" + versionNo;
    }
}
