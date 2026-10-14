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

    /** 끝난 시각이 비어 있는 동안은 진행 중이다. 성공 여부 칸이 필수라 시작할 때는 false 로 둔다 */
    public static CollectorRun start(String keyword, String triggeredBy, LocalDateTime startedAt) {
        return CollectorRun.builder()
                .keyword(keyword)
                .triggeredBy(triggeredBy)
                .startedAt(startedAt)
                .success(false)
                .build();
    }

    public void complete(LocalDateTime completedAt, int totalFetched, int newCount, int updatedCount) {
        this.completedAt = completedAt;
        this.totalFetched = totalFetched;
        this.newCount = newCount;
        this.updatedCount = updatedCount;
        this.success = true;
    }

    public void fail(LocalDateTime completedAt, int totalFetched, int newCount, int updatedCount, String errorMessage) {
        this.completedAt = completedAt;
        this.totalFetched = totalFetched;
        this.newCount = newCount;
        this.updatedCount = updatedCount;
        this.success = false;
        this.errorMessage = errorMessage;
    }
}
