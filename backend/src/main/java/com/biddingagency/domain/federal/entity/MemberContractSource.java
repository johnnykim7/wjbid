package com.biddingagency.domain.federal.entity;

/**
 * 회원 계약 날짜 출처 (CR-142, BIZ-056)
 */
public enum MemberContractSource {
    /** 미 연방 계약 자료에서 찾은 계약. 매일 받기로 날짜가 바뀌면 따라 바뀐다 */
    PUBLIC,
    /** 회원이 날짜를 직접 넣은 계약. 회원만 고친다 */
    MANUAL
}
