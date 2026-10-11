package com.biddingagency.domain.federal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 미 연방 계약 받기 회차 (CR-142, BID-MKT-027). 시각은 모두 Asia/Seoul.
 */
@Entity
@Table(name = "federal_contract_sync_runs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FederalContractSyncRun {

    public static final String BY_SCHEDULER = "SCHEDULER";
    public static final String BY_ADMIN = "ADMIN";

    @Id
    @UuidGenerator
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 10)
    private FederalSyncMode mode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private FederalSyncStatus status;

    /** DAILY 일 때 이 날 이후 바뀐 것만 받았다 */
    @Column(name = "modified_since")
    private LocalDate modifiedSince;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "search_calls", nullable = false)
    private int searchCalls;

    @Column(name = "detail_calls", nullable = false)
    private int detailCalls;

    @Column(name = "inserted_count", nullable = false)
    private int insertedCount;

    @Column(name = "updated_count", nullable = false)
    private int updatedCount;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "triggered_by", nullable = false, length = 20)
    private String triggeredBy;

    public static FederalContractSyncRun start(FederalSyncMode mode, LocalDate modifiedSince, LocalDateTime now,
                                               String triggeredBy) {
        FederalContractSyncRun run = new FederalContractSyncRun();
        run.mode = mode;
        run.status = FederalSyncStatus.RUNNING;
        run.modifiedSince = modifiedSince;
        run.startedAt = now;
        run.triggeredBy = triggeredBy;
        return run;
    }

    public void record(int searchCalls, int detailCalls, int inserted, int updated) {
        this.searchCalls = searchCalls;
        this.detailCalls = detailCalls;
        this.insertedCount = inserted;
        this.updatedCount = updated;
    }

    public void succeed(LocalDateTime now) {
        this.status = FederalSyncStatus.SUCCEEDED;
        this.finishedAt = now;
    }

    public void fail(String message, LocalDateTime now) {
        this.status = FederalSyncStatus.FAILED;
        this.errorMessage = message;
        this.finishedAt = now;
    }
}
