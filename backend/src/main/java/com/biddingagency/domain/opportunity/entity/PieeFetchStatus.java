package com.biddingagency.domain.opportunity.entity;

/** CR-160: PIEE 입찰서류 받기 결과(BIZ-079). 화면 이름은 받는 중, 받음, PIEE 에 없음, 받기 실패 */
public enum PieeFetchStatus {
    WAITING,
    FETCHING,
    DONE,
    NOT_FOUND,
    FAILED
}
