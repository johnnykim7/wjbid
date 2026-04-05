package com.biddingagency.domain.collection.entity;

import com.biddingagency.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "collector_runs",
        indexes = {
                @Index(name = "idx_collector_runs_started_at", columnList = "started_at"),
                @Index(name = "idx_collector_runs_success", columnList = "success")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CollectorRun extends BaseEntity {

    @Column(name = "keyword", length = 255)
    private String keyword;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "total_fetched", nullable = false)
    @Builder.Default
    private Integer totalFetched = 0;

    @Column(name = "new_count", nullable = false)
    @Builder.Default
    private Integer newCount = 0;

    @Column(name = "updated_count", nullable = false)
    @Builder.Default
    private Integer updatedCount = 0;

    @Column(name = "success", nullable = false)
    private Boolean success;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "triggered_by", nullable = false, length = 50)
    private String triggeredBy;

    public void complete(int totalFetched, int newCount, int updatedCount) {
        this.completedAt = LocalDateTime.now();
        this.totalFetched = totalFetched;
        this.newCount = newCount;
        this.updatedCount = updatedCount;
        this.success = true;
    }

    public void fail(String errorMessage) {
        this.completedAt = LocalDateTime.now();
        this.success = false;
        this.errorMessage = errorMessage;
    }
}
