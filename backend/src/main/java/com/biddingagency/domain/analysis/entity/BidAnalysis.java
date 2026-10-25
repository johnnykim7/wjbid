package com.biddingagency.domain.analysis.entity;

import com.biddingagency.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 입찰 건의 입찰 분석 회차 (CR-170, BID-DOC-006, BID-DOC-007).
 *
 * 입찰 건 하나에 회차 여러 줄이고 지금 분석은 started_at 이 가장 늦은 회차다.
 * 관리자 결정은 회차에 붙으므로 다시 분석한 신규 회차는 결정이 비어 있다(BIZ-092).
 * 실패해도 그때까지 저장된 산출물은 지우지 않는다.
 */
@Entity
@Table(name = "bid_analyses",
        indexes = @Index(name = "idx_bid_analyses_bid_request", columnList = "bid_request_id, started_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BidAnalysis extends BaseEntity {

    @Column(name = "bid_request_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID bidRequestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BidAnalysisStatus status;

    @Column(name = "workflow_run_id", length = 100)
    private String workflowRunId;

    /** 지금 분석 판정과 대기 한도 판정에 쓴다 */
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "report_json", columnDefinition = "LONGTEXT")
    private String reportJson;

    @Column(name = "capture_plan_json", columnDefinition = "LONGTEXT")
    private String capturePlanJson;

    @Column(name = "decision_json", columnDefinition = "LONGTEXT")
    private String decisionJson;

    @Column(name = "questions_json", columnDefinition = "LONGTEXT")
    private String questionsJson;

    @Column(name = "pwin_percent", precision = 4, scale = 1)
    private BigDecimal pwinPercent;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_verdict", length = 20)
    private BidVerdict aiVerdict;

    @Column(name = "verdict_reason", length = 1000)
    private String verdictReason;

    @Column(name = "scores_json", columnDefinition = "TEXT")
    private String scoresJson;

    @Column(name = "strengths_json", columnDefinition = "TEXT")
    private String strengthsJson;

    @Column(name = "weaknesses_json", columnDefinition = "TEXT")
    private String weaknessesJson;

    @Column(name = "issue_ranking_json", columnDefinition = "TEXT")
    private String issueRankingJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "admin_decision", length = 20)
    private BidDecision adminDecision;

    @Column(name = "admin_note", length = 2000)
    private String adminNote;

    @Column(name = "decided_by", columnDefinition = "BINARY(16)")
    private UUID decidedBy;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    /** 자동 시작이면 분석 단계로 전환한 관리자 */
    @Column(name = "started_by", columnDefinition = "BINARY(16)")
    private UUID startedBy;

    public static BidAnalysis running(UUID bidRequestId, UUID startedBy, LocalDateTime now) {
        return BidAnalysis.builder()
                .bidRequestId(bidRequestId)
                .status(BidAnalysisStatus.RUNNING)
                .startedAt(now)
                .startedBy(startedBy)
                .build();
    }

    public static BidAnalysis decisionOnly(UUID bidRequestId, LocalDateTime now) {
        return BidAnalysis.builder()
                .bidRequestId(bidRequestId)
                .status(BidAnalysisStatus.DECISION_ONLY)
                .startedAt(now)
                .build();
    }

    public boolean isRunning() {
        return status == BidAnalysisStatus.RUNNING;
    }

    /** 분석 중이 워크플로우 대기 한도를 넘었는지. 서버가 재기동해 결과를 받지 못한 회차를 가린다 */
    public boolean isStale(LocalDateTime now, Duration limit) {
        return isRunning() && startedAt.plus(limit).isBefore(now);
    }

    public void recordRun(String workflowRunId) {
        this.workflowRunId = workflowRunId;
    }

    public void saveReport(String json) {
        this.reportJson = json;
    }

    public void saveCapturePlan(String json) {
        this.capturePlanJson = json;
    }

    public void saveQuestions(String json) {
        this.questionsJson = json;
    }

    public void markDone(String decisionJson, BigDecimal pwinPercent, BidVerdict verdict, String verdictReason,
                         String scoresJson, String strengthsJson, String weaknessesJson, String issueRankingJson,
                         LocalDateTime now) {
        this.decisionJson = decisionJson;
        this.pwinPercent = pwinPercent;
        this.aiVerdict = verdict;
        this.verdictReason = verdictReason;
        this.scoresJson = scoresJson;
        this.strengthsJson = strengthsJson;
        this.weaknessesJson = weaknessesJson;
        this.issueRankingJson = issueRankingJson;
        this.status = BidAnalysisStatus.DONE;
        this.finishedAt = now;
        this.failureReason = null;
    }

    public void markFailed(String reason, LocalDateTime now) {
        this.status = BidAnalysisStatus.FAILED;
        this.failureReason = reason;
        this.finishedAt = now;
    }

    public void decide(BidDecision decision, String note, UUID decidedBy, LocalDateTime now) {
        this.adminDecision = decision;
        this.adminNote = note;
        this.decidedBy = decidedBy;
        this.decidedAt = now;
    }
}
