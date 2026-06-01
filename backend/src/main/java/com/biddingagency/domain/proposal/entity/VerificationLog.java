package com.biddingagency.domain.proposal.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * verification_log — 충실성·분량 검증 결과 (CR-031).
 *
 * <p>두 산출물(공고문 정제 / 제안서 section)에 공통. method=RULE(BE 정형 룰) 또는 LLM(verify-fidelity).
 * BaseEntity 미상속 — append-only 로그 ({@link GenerationLog} 와 동일 패턴).
 *
 * <p>관리자 콘솔(CR-030)이 target 별 최신 1건을 배지·패널로 노출한다.
 */
@Entity
@Table(name = "verification_log",
        indexes = {
                @Index(name = "idx_verification_log_target", columnList = "target_type, target_id, verified_at"),
                @Index(name = "idx_verification_log_run", columnList = "workflow_run_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class VerificationLog {

    @Id
    @UuidGenerator
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private VerificationTargetType targetType;

    @Column(name = "target_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 10)
    @Builder.Default
    private VerificationMethod method = VerificationMethod.LLM;

    @Enumerated(EnumType.STRING)
    @Column(name = "verdict", nullable = false, length = 10)
    private Verdict verdict;

    /** LLM 검증 시 Aimbase runId (RULE 이면 NULL). */
    @Column(name = "workflow_run_id", length = 255)
    private String workflowRunId;

    /** 근거 없는 문장 배열 [{sentence, reason}]. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "hallucinations", columnDefinition = "JSON")
    private List<Map<String, Object>> hallucinations;

    /** 원문에 있는데 누락된 항목 배열 [{source_quote, reason}]. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "missing_from_source", columnDefinition = "JSON")
    private List<Map<String, Object>> missingFromSource;

    /** BE 정형 룰 위반 항목 배열 (분량/첨부/block). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rule_findings", columnDefinition = "JSON")
    private List<String> ruleFindings;

    /** 결과물 단어/글자 수 (분량 추적). */
    @Column(name = "word_count")
    private Integer wordCount;

    /** 자동 재시도 회차 (1=최초, 2=1회 재시도 후). */
    @Column(name = "attempt", nullable = false)
    @Builder.Default
    private Integer attempt = 1;

    @Column(name = "verified_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime verifiedAt = LocalDateTime.now();

    public int hallucinationCount() {
        return hallucinations != null ? hallucinations.size() : 0;
    }
}
