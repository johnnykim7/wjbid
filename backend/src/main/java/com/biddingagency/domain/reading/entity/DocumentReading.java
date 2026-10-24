package com.biddingagency.domain.reading.entity;

import com.biddingagency.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 고객 서류를 읽은 결과 (CR-169, BID-DOC-004).
 *
 * 파일 내용의 SHA-256 하나에 한 줄이라 같은 내용은 공고별 서류든 서류함 서류든 한 번만 읽는다(BIZ-085).
 * 어느 서류가 어느 입찰 건의 후보인지는 저장하지 않고 읽을 때마다 모은다.
 */
@Entity
@Table(name = "document_readings",
        uniqueConstraints = @UniqueConstraint(name = "uk_document_readings_sha256", columnNames = "content_sha256"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DocumentReading extends BaseEntity {

    @Column(name = "content_sha256", nullable = false, length = 64)
    private String contentSha256;

    /** 처음 읽을 때의 파일 이름. 확장자로 읽는 꼴을 정한다 */
    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "read_as", nullable = false, length = 10)
    private ReadAs readAs;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReadingStatus status;

    /** 상태가 마지막으로 바뀐 시각. 읽는 중 30분 판정에 쓴다(BIZ-087) */
    @Column(name = "status_at", nullable = false)
    private LocalDateTime statusAt;

    @Column(name = "facts_json", columnDefinition = "LONGTEXT")
    private String factsJson;

    @Column(name = "fact_count", nullable = false)
    @Builder.Default
    private int factCount = 0;

    @Column(name = "vision_used", nullable = false)
    @Builder.Default
    private boolean visionUsed = false;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "workflow_run_id", length = 100)
    private String workflowRunId;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public void markWaiting(LocalDateTime now) {
        changeStatus(ReadingStatus.WAITING, now);
        this.failureReason = null;
    }

    public void markReading(LocalDateTime now) {
        changeStatus(ReadingStatus.READING, now);
    }

    public void recordRun(String workflowRunId) {
        this.workflowRunId = workflowRunId;
    }

    public void markDone(String factsJson, int factCount, boolean visionUsed, LocalDateTime now) {
        changeStatus(ReadingStatus.DONE, now);
        this.factsJson = factsJson;
        this.factCount = factCount;
        this.visionUsed = visionUsed;
        this.failureReason = null;
        this.readAt = now;
    }

    public void markFailed(String reason, LocalDateTime now) {
        changeStatus(ReadingStatus.FAILED, now);
        this.failureReason = reason;
    }

    /** 읽는 중이 기한을 넘겼는지. 서버가 재기동해 결과를 받지 못한 줄을 가린다(BIZ-087) */
    public boolean isStale(LocalDateTime now, java.time.Duration limit) {
        return status.isInFlight() && statusAt.plus(limit).isBefore(now);
    }

    private void changeStatus(ReadingStatus status, LocalDateTime now) {
        this.status = status;
        this.statusAt = now;
    }
}
