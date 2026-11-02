package com.biddingagency.domain.member.entity;

/**
 * 인증번호의 쓰임 (CR-186, BIZ-097). 쓰임마다 알림 서버 메일 틀이 다르다
 */
public enum EmailCodePurpose {
    /** 회원가입. 가입되지 않은 메일에만 보낸다 */
    SIGNUP("BID_EMAIL_CODE"),
    /** 비밀번호 재설정. 가입된 메일에만 보낸다 */
    PASSWORD_RESET("BID_PASSWORD_RESET_CODE");

    private final String templateCode;

    EmailCodePurpose(String templateCode) {
        this.templateCode = templateCode;
    }

    public String templateCode() {
        return templateCode;
    }
}
