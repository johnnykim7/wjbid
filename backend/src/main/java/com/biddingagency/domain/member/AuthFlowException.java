package com.biddingagency.domain.member;

import java.util.Map;

/**
 * 인증번호와 SNS 가입 흐름의 오류 (CR-146). 응답 본문에 오류 코드와 덧붙일 값(남은 횟수 등)을 싣는다.
 */
public class AuthFlowException extends RuntimeException {

    private final AuthError error;
    private final Map<String, Object> details;

    public AuthFlowException(AuthError error) {
        this(error, Map.of());
    }

    public AuthFlowException(AuthError error, Map<String, Object> details) {
        super(error.message());
        this.error = error;
        this.details = details;
    }

    public AuthError getError() {
        return error;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
