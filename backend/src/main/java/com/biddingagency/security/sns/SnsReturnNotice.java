package com.biddingagency.security.sns;

/**
 * SNS 왕복이 로그인으로 이어지지 않을 때 로그인 화면 주소(/login?sns=)에 싣는 안내 (CR-146, T3-2)
 */
public enum SnsReturnNotice {
    /** SNS 이메일로 이미 이메일 가입한 회원이 있다 (5.1 결정) */
    EMAIL_IN_USE,
    /** 고객이 SNS 화면에서 동의를 취소했다 */
    CANCELLED,
    FAILED
}
