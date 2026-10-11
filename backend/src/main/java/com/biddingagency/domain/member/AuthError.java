package com.biddingagency.domain.member;

import org.springframework.http.HttpStatus;

/**
 * 인증번호와 SNS 가입에서 고객 화면이 알아보고 안내를 고르는 오류 (CR-146, T3-2)
 */
public enum AuthError {
    EMAIL_CODE_TOO_SOON(HttpStatus.TOO_MANY_REQUESTS, "잠시 뒤 다시 받을 수 있습니다"),
    EMAIL_CODE_DAILY_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "오늘 받을 수 있는 횟수를 넘었습니다"),
    EMAIL_SEND_FAILED(HttpStatus.BAD_GATEWAY, "인증번호 메일을 보내지 못했습니다"),
    EMAIL_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "인증번호가 맞지 않습니다"),
    EMAIL_CODE_EXPIRED(HttpStatus.GONE, "인증 시간이 지났습니다"),
    EMAIL_CODE_LOCKED(HttpStatus.TOO_MANY_REQUESTS, "인증번호를 너무 많이 틀렸습니다"),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "이메일 인증을 마치지 않았습니다"),
    SNS_TICKET_EXPIRED(HttpStatus.GONE, "SNS 확인 시간이 지났습니다"),
    SNS_ACCOUNT_IN_USE(HttpStatus.CONFLICT, "이미 가입에 쓴 SNS 계정입니다");

    private final HttpStatus status;
    private final String message;

    AuthError(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }
}
