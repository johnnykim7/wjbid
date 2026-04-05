package com.biddingagency.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base domain event
 */
public abstract class DomainEvent {
    private final String eventType;
    private final UUID entityId;
    private final String entityType;
    private final UUID actorId;
    private final LocalDateTime occurredAt;

    protected DomainEvent(String eventType, UUID entityId, String entityType, UUID actorId) {
        this.eventType = eventType;
        this.entityId = entityId;
        this.entityType = entityType;
        this.actorId = actorId;
        this.occurredAt = LocalDateTime.now();
    }

    public String getEventType() { return eventType; }
    public UUID getEntityId() { return entityId; }
    public String getEntityType() { return entityType; }
    public UUID getActorId() { return actorId; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
