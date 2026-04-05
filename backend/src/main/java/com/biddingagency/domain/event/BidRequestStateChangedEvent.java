package com.biddingagency.domain.event;

import com.biddingagency.domain.bid.entity.BidRequestState;

import java.util.UUID;

public class BidRequestStateChangedEvent extends DomainEvent {
    private final BidRequestState fromState;
    private final BidRequestState toState;

    public BidRequestStateChangedEvent(UUID bidRequestId, BidRequestState fromState,
                                        BidRequestState toState, UUID actorId) {
        super("BidRequestStateChanged", bidRequestId, "BidRequest", actorId);
        this.fromState = fromState;
        this.toState = toState;
    }

    public BidRequestState getFromState() { return fromState; }
    public BidRequestState getToState() { return toState; }
}
