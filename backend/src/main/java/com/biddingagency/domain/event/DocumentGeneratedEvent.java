package com.biddingagency.domain.event;

import java.util.UUID;

public class DocumentGeneratedEvent extends DomainEvent {
    private final UUID bidRequestId;
    private final String documentType;
    private final int versionNo;

    public DocumentGeneratedEvent(UUID documentId, UUID bidRequestId, String documentType, int versionNo) {
        super("DocumentGenerated", documentId, "BidDocument", null);
        this.bidRequestId = bidRequestId;
        this.documentType = documentType;
        this.versionNo = versionNo;
    }

    public UUID getBidRequestId() { return bidRequestId; }
    public String getDocumentType() { return documentType; }
    public int getVersionNo() { return versionNo; }
}
