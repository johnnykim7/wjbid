package com.biddingagency.domain.bid;

import com.biddingagency.domain.bid.entity.BidRequestState;

/**
 * CR-017 ③: 고객이 허용되지 않은(관리자 전용) 상태 전이를 시도한 경우.
 * 403 Forbidden으로 매핑된다.
 */
public class CustomerTransitionNotAllowedException extends RuntimeException {

    private final BidRequestState attemptedState;

    public CustomerTransitionNotAllowedException(BidRequestState attemptedState) {
        super("고객은 해당 상태로 전이할 수 없습니다: " + attemptedState + " (관리자 전용)");
        this.attemptedState = attemptedState;
    }

    public BidRequestState getAttemptedState() {
        return attemptedState;
    }
}
