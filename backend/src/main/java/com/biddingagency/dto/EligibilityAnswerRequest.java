package com.biddingagency.dto;

/**
 * CR-137 적격성 진단 답 저장 본문. 값 검사는 서비스가 해 T3-2 의 400 본문으로 돌려준다.
 */
public record EligibilityAnswerRequest(String answer) {
}
