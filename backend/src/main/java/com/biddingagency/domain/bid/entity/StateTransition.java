package com.biddingagency.domain.bid.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * State Transition Record
 *
 * Represents a single state change in bid request lifecycle
 * Stored in state_history JSON column
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StateTransition {

    /**
     * Previous state
     */
    private BidRequestState fromState;

    /**
     * New state
     */
    private BidRequestState toState;

    /**
     * User who triggered the transition
     */
    private UUID userId;

    /**
     * Username for display
     */
    private String username;

    /**
     * Timestamp of transition
     */
    private LocalDateTime timestamp;

    /**
     * Optional notes/reason for transition
     */
    private String notes;

    /**
     * Was this an automatic transition?
     */
    @Builder.Default
    private Boolean automatic = false;
}
