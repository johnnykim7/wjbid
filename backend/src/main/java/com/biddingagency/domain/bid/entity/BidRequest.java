package com.biddingagency.domain.bid.entity;

import com.biddingagency.common.BaseEntity;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Bid Request entity
 *
 * Represents a client's request to bid on an opportunity
 * Managed by FSM (Finite State Machine)
 */
@Entity
@Table(name = "bid_requests",
        indexes = {
                @Index(name = "idx_bid_requests_state", columnList = "state"),
                @Index(name = "idx_bid_requests_member", columnList = "member_id"),
                @Index(name = "idx_bid_requests_opportunity", columnList = "opportunity_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uniq_bid_requests_member_opportunity",
                        columnNames = {"member_id", "opportunity_id"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BidRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, columnDefinition = "BINARY(16)")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunity_id", nullable = false, columnDefinition = "BINARY(16)")
    private Opportunity opportunity;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 50)
    @Builder.Default
    private BidRequestState state = BidRequestState.CREATED;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "state_history", columnDefinition = "JSON", nullable = false)
    @Builder.Default
    private List<StateTransition> stateHistory = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "service_level", length = 30)
    private ServiceLevel serviceLevel;

    @Column(name = "assigned_to", columnDefinition = "BINARY(16)")
    private UUID assignedTo;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    /** CR-018: 입찰 결과(합격/불합격)가 확정된 시각 */
    @Column(name = "outcome_decided_at")
    private LocalDateTime outcomeDecidedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "close_reason", length = 500)
    private String closeReason;

    // Business methods

    /**
     * Add state transition to history
     */
    public void addStateTransition(StateTransition transition) {
        if (this.stateHistory == null) {
            this.stateHistory = new ArrayList<>();
        }
        this.stateHistory.add(transition);
        this.state = transition.getToState();
    }

    /**
     * Transition to new state
     */
    public void transitionTo(BidRequestState newState, UUID userId, String username, String notes) {
        StateTransition transition = StateTransition.builder()
                .fromState(this.state)
                .toState(newState)
                .userId(userId)
                .username(username)
                .timestamp(LocalDateTime.now())
                .notes(notes)
                .automatic(false)
                .build();

        addStateTransition(transition);

        // Set submitted_at if transitioning to SUBMITTED
        if (newState == BidRequestState.SUBMITTED) {
            this.submittedAt = LocalDateTime.now();
        }
        // CR-018: Set outcome_decided_at if transitioning to a result state
        if (newState == BidRequestState.AWARDED || newState == BidRequestState.NOT_AWARDED) {
            this.outcomeDecidedAt = LocalDateTime.now();
        }
        // Set closed_at if transitioning to CLOSED
        if (newState == BidRequestState.CLOSED) {
            this.closedAt = LocalDateTime.now();
        }
    }

    /**
     * Assign to user
     */
    public void assignTo(UUID userId) {
        this.assignedTo = userId;
    }

    /**
     * Check if in terminal state
     */
    public boolean isTerminal() {
        return this.state.isTerminal();
    }

    /**
     * Check if documents can be edited
     */
    public boolean canEditDocuments() {
        return this.state.canEditDocuments();
    }

    /**
     * Check if client action is required
     */
    public boolean requiresClientAction() {
        return this.state.requiresClientAction();
    }

    /**
     * Get current state name for display
     */
    public String getStateDisplay() {
        return switch (this.state) {
            case CREATED -> "신청 접수";
            case DOCS_PENDING -> "문서 대기";
            case DOCS_RECEIVED -> "문서 접수 완료";
            case ANALYZING -> "분석 중";
            case GENERATING -> "문서 생성 중";
            case REVIEW -> "관리자 검토";
            case CONFIRMED -> "확정";
            case SUBMITTED -> "제출 완료";
            case AWARDED -> "합격";
            case NOT_AWARDED -> "불합격";
            case CLOSED -> "종료";
        };
    }
}
