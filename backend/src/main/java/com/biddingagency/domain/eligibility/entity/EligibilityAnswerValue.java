package com.biddingagency.domain.eligibility.entity;

/**
 * 적격성 진단에서 회원이 자격 하나에 남기는 답 (CR-137).
 * 판정이 아니라 회원 스스로의 답이다(BIZ-032).
 */
public enum EligibilityAnswerValue {
    HAVE,
    LACK,
    UNSURE
}
