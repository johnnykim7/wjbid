package com.biddingagency.domain.event;

import java.util.UUID;

public class OpportunityApprovedEvent extends DomainEvent {

    public OpportunityApprovedEvent(UUID opportunityId, UUID actorId) {
        super("OpportunityApproved", opportunityId, "Opportunity", actorId);
    }
}
