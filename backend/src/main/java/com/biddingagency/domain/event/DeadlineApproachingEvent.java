package com.biddingagency.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

public class DeadlineApproachingEvent extends DomainEvent {
    private final int daysRemaining;
    private final LocalDateTime deadline;

    public DeadlineApproachingEvent(UUID bidRequestId, int daysRemaining, LocalDateTime deadline) {
        super("DeadlineApproaching", bidRequestId, "BidRequest", null);
        this.daysRemaining = daysRemaining;
        this.deadline = deadline;
    }

    public int getDaysRemaining() { return daysRemaining; }
    public LocalDateTime getDeadline() { return deadline; }
}
