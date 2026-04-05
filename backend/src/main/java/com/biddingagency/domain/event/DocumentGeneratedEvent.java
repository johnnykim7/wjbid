package com.biddingagency.domain.event;

import java.util.UUID;

public class DocumentGeneratedEvent extends DomainEvent {
    private final UUID bidRequestId;
    private final String documentType;

    public DocumentGeneratedEvent(UUID documentId, UUID bidRequestId, String documentType) {
        super("DocumentGenerated", documentId, "BidDocument", null);
        this.bidRequestId = bidRequestId;
        this.documentType = documentType;
    }

    public UUID getBidRequestId() { return bidRequestId; }
    public String getDocumentType() { return documentType; }
}
