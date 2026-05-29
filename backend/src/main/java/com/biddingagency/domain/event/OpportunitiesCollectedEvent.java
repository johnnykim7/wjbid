package com.biddingagency.domain.event;

import java.util.UUID;

public class OpportunitiesCollectedEvent extends DomainEvent {
    private final int newCount;
    private final int targetNewCount; // CR-015: 시설관리 타깃(IndustryClassifier 매칭) 신규
    private final int updatedCount;
    private final int totalFetched;

    public OpportunitiesCollectedEvent(UUID collectorRunId, int newCount, int targetNewCount,
                                       int updatedCount, int totalFetched) {
        super("OpportunitiesCollected", collectorRunId, "CollectorRun", null);
        this.newCount = newCount;
        this.targetNewCount = targetNewCount;
        this.updatedCount = updatedCount;
        this.totalFetched = totalFetched;
    }

    public int getNewCount() { return newCount; }
    public int getTargetNewCount() { return targetNewCount; }
    public int getUpdatedCount() { return updatedCount; }
    public int getTotalFetched() { return totalFetched; }
}
