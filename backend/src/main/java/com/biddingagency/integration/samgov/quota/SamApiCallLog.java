package com.biddingagency.integration.samgov.quota;

import com.biddingagency.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * CR-036: SAM API 호출 계측 — 일자(UTC)·엔드포인트별 집계.
 * UPSERT는 SamApiCallLogRepository.record(...) native 쿼리로 수행한다(동시성 안전).
 * 이 엔티티는 주로 조회용.
 */
@Entity
@Table(name = "sam_api_call_log",
        uniqueConstraints = @UniqueConstraint(name = "uk_sam_call_date_endpoint",
                columnNames = {"call_date", "endpoint"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SamApiCallLog extends BaseEntity {

    @Column(name = "call_date", nullable = false)
    private LocalDate callDate;

    /** search | noticedesc | attachment */
    @Column(name = "endpoint", nullable = false, length = 30)
    private String endpoint;

    @Column(name = "success_count", nullable = false)
    @Builder.Default
    private long successCount = 0;

    @Column(name = "error_count", nullable = false)
    @Builder.Default
    private long errorCount = 0;

    @Column(name = "last_status")
    private Integer lastStatus;

    @Column(name = "last_rate_limit", length = 50)
    private String lastRateLimit;

    @Column(name = "last_rate_remaining", length = 50)
    private String lastRateRemaining;

    @Column(name = "last_rate_headers", columnDefinition = "TEXT")
    private String lastRateHeaders;

    @Column(name = "last_error_body", columnDefinition = "TEXT")
    private String lastErrorBody;

    @Column(name = "last_called_at")
    private LocalDateTime lastCalledAt;
}
