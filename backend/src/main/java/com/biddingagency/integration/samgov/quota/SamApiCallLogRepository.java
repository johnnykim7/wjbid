package com.biddingagency.integration.samgov.quota;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * CR-036: SAM API 호출 계측 UPSERT.
 * (call_date, endpoint) UNIQUE 위에서 ON DUPLICATE KEY로 카운터를 누적한다.
 * 계측은 본 호출 트랜잭션과 독립이어야 하므로 REQUIRES_NEW로 분리한다(본 작업 롤백돼도 카운트는 남김).
 */
public interface SamApiCallLogRepository extends JpaRepository<SamApiCallLog, UUID> {

    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query(value = """
            INSERT INTO sam_api_call_log
                (id, call_date, endpoint, success_count, error_count,
                 last_status, last_rate_limit, last_rate_remaining, last_rate_headers,
                 last_error_body, last_called_at, created_at, updated_at)
            VALUES
                (UNHEX(REPLACE(UUID(),'-','')), :callDate, :endpoint, :successDelta, :errorDelta,
                 :status, :rateLimit, :rateRemaining, :rateHeaders,
                 :errorBody, :calledAt, NOW(), NOW())
            ON DUPLICATE KEY UPDATE
                success_count       = success_count + :successDelta,
                error_count         = error_count + :errorDelta,
                last_status         = :status,
                last_rate_limit     = COALESCE(:rateLimit, last_rate_limit),
                last_rate_remaining = COALESCE(:rateRemaining, last_rate_remaining),
                last_rate_headers   = COALESCE(:rateHeaders, last_rate_headers),
                last_error_body     = COALESCE(:errorBody, last_error_body),
                last_called_at      = :calledAt,
                updated_at          = NOW()
            """, nativeQuery = true)
    void record(@Param("callDate") LocalDate callDate,
                @Param("endpoint") String endpoint,
                @Param("successDelta") long successDelta,
                @Param("errorDelta") long errorDelta,
                @Param("status") Integer status,
                @Param("rateLimit") String rateLimit,
                @Param("rateRemaining") String rateRemaining,
                @Param("rateHeaders") String rateHeaders,
                @Param("errorBody") String errorBody,
                @Param("calledAt") LocalDateTime calledAt);

    List<SamApiCallLog> findByCallDateOrderByEndpoint(LocalDate callDate);
}
