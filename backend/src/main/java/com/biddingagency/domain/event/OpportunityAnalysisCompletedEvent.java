package com.biddingagency.domain.event;

import java.util.UUID;

public class OpportunityAnalysisCompletedEvent extends DomainEvent {

    private final UUID opportunityId;
    private final boolean success;
    private final String errorMessage;

    public OpportunityAnalysisCompletedEvent(UUID analysisId, UUID opportunityId, boolean success, String errorMessage) {
        super("OpportunityAnalysisCompleted", analysisId, "OpportunityAnalysis", null);
        this.opportunityId = opportunityId;
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public UUID getOpportunityId() { return opportunityId; }
    public boolean isSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }
}
