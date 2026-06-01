package com.biddingagency.domain.proposal.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * generation_log — LLM 호출 비용 추적 (CR-029 선반영, CR-027 시점에 테이블만 박음).
 *
 * BaseEntity 미상속 — updated_at 없는 append-only 로그.
 */
@Entity
@Table(name = "generation_log",
        indexes = {
                @Index(name = "idx_generation_log_target", columnList = "target_type, target_id"),
                @Index(name = "idx_generation_log_run", columnList = "run_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GenerationLog {

    @Id
    @UuidGenerator
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private GenerationTargetType targetType;

    @Column(name = "target_id", columnDefinition = "BINARY(16)")
    private UUID targetId;

    @Column(name = "workflow_id", length = 255)
    private String workflowId;

    @Column(name = "run_id", length = 255)
    private String runId;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "tokens_in")
    private Integer tokensIn;

    @Column(name = "tokens_out")
    private Integer tokensOut;

    @Column(name = "cost_usd", precision = 12, scale = 6)
    private BigDecimal costUsd;

    /** 재실행 캐시 검증 */
    @Column(name = "prompt_hash", length = 64)
    private String promptHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
