package com.biddingagency.domain.member;

import org.springframework.http.HttpStatus;

/**
 * 회원 구분과 회원 기간에서 화면이 알아보고 안내를 고르는 오류 (CR-157, T3-2)
 */
public enum MembershipError {
    // 토큰이 없거나 지난 요청. 401 이어야 고객 화면이 토큰을 갱신하고 다시 부른다
    LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다"),
    MEMBERSHIP_REQUIRED(HttpStatus.FORBIDDEN, "회원 전용입니다"),
    MEMBERSHIP_PERIOD_INVALID(HttpStatus.BAD_REQUEST, "끝일은 시작일과 같거나 뒤여야 합니다"),
    MEMBERSHIP_NOT_FOUND(HttpStatus.NOT_FOUND, "회원 기간이 없습니다"),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원이 없습니다");

    private final HttpStatus status;
    private final String message;

    MembershipError(HttpStatus status, String message) {
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
