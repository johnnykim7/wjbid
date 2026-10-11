package com.biddingagency.domain.member.entity;

/**
 * SNS 에서 돌아온 뒤 화면에 넘기는 표의 쓰임 (CR-146)
 */
public enum SnsTicketKind {
    /** 이미 이어진 회원. 화면이 토큰으로 바꾼다 */
    LOGIN,
    /** 처음 온 SNS 계정. SNS 가입 화면이 쓴다 */
    SIGNUP
}
