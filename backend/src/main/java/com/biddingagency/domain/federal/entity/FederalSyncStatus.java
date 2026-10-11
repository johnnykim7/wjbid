package com.biddingagency.domain.federal.entity;

/**
 * 받기 회차 상태 (CR-142). RUNNING 회차가 있으면 새 실행을 거부한다.
 */
public enum FederalSyncStatus {
    RUNNING,
    SUCCEEDED,
    FAILED
}
