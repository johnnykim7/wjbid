package com.biddingagency.domain.federal.entity;

/**
 * 미 연방 계약 받기 방식 (CR-142)
 */
public enum FederalSyncMode {
    /** 받는 시작일(POL-031)부터 전체 */
    FULL,
    /** 지난 성공 회차 이후 바뀐 계약만 */
    DAILY
}
