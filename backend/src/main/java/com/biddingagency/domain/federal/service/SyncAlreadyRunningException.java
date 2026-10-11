package com.biddingagency.domain.federal.service;

/** 받기 회차가 이미 돌고 있다 (CR-142, BID-MKT-027). 한 번에 한 회차만 돈다 */
public class SyncAlreadyRunningException extends RuntimeException {

    public SyncAlreadyRunningException() {
        super("미 연방 계약 받기가 이미 돌고 있습니다");
    }
}
