package com.biddingagency.domain.event;

import java.util.UUID;

public class OpportunitiesCollectedEvent extends DomainEvent {
    private final int newCount;
    private final int updatedCount;
    private final int totalFetched;

    public OpportunitiesCollectedEvent(UUID collectorRunId, int newCount, int updatedCount, int totalFetched) {
        super("OpportunitiesCollected", collectorRunId, "CollectorRun", null);
        this.newCount = newCount;
        this.updatedCount = updatedCount;
        this.totalFetched = totalFetched;
    }

    public int getNewCount() { return newCount; }
    public int getUpdatedCount() { return updatedCount; }
    public int getTotalFetched() { return totalFetched; }
}
