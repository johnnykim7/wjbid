package com.biddingagency.domain.reading.entity;

/**
 * 고객 서류 읽기 상태 (CR-169).
 * 화면은 WAITING 과 READING 을 모두 읽는 중으로 보인다.
 */
public enum ReadingStatus {
    WAITING,
    READING,
    DONE,
    FAILED,
    UNSUPPORTED;

    public boolean isInFlight() {
        return this == WAITING || this == READING;
    }
}
