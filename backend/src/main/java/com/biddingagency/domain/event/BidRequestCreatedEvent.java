package com.biddingagency.domain.event;

import java.util.UUID;

public class BidRequestCreatedEvent extends DomainEvent {
    private final UUID memberId;
    private final UUID opportunityId;

    public BidRequestCreatedEvent(UUID bidRequestId, UUID memberId, UUID opportunityId, UUID actorId) {
        super("BidRequestCreated", bidRequestId, "BidRequest", actorId);
        this.memberId = memberId;
        this.opportunityId = opportunityId;
    }

    public UUID getMemberId() { return memberId; }
    public UUID getOpportunityId() { return opportunityId; }
}
