package com.biddingagency.domain.compliance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 제출 검사 처리 (CR-194, BIZ-106, BIZ-109). 줄이나 결정 조건마다 한 건이고, 다시 처리하면 그 건을 고쳐 쓴다.
 * 처리할 때의 공고 분석 시각과 지금 값이 다르면 그 처리는 무효다.
 */
@Entity
@Table(name = "submission_check_notes",
        uniqueConstraints = @UniqueConstraint(name = "uk_submission_check_notes_target",
                columnNames = {"bid_request_id", "target_type", "target_key"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SubmissionCheckNote {

    @Id
    @UuidGenerator
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "bid_request_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID bidRequestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private SubmissionNoteTarget targetType;

    @Column(name = "target_key", nullable = false, length = 100)
    private String targetKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "disposition", nullable = false, length = 30)
    private SubmissionDisposition disposition;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "source_ref", length = 500)
    private String sourceRef;

    @Column(name = "notice_analyzed_at", nullable = false)
    private LocalDateTime noticeAnalyzedAt;

    @Column(name = "decided_by", nullable = false)
    private String decidedBy;

    @Column(name = "decided_at", nullable = false)
    private LocalDateTime decidedAt;

    public void rewrite(SubmissionDisposition disposition, String reason, String sourceRef,
                        LocalDateTime noticeAnalyzedAt, String decidedBy, LocalDateTime decidedAt) {
        this.disposition = disposition;
        this.reason = reason;
        this.sourceRef = sourceRef;
        this.noticeAnalyzedAt = noticeAnalyzedAt;
        this.decidedBy = decidedBy;
        this.decidedAt = decidedAt;
    }
}
