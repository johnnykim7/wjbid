package com.biddingagency.domain.member;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CR-131: 이미 가입된 이메일을 409 로 매핑.
 * CR-146: 인증번호와 SNS 가입 오류를 코드별 상태로 매핑.
 * CR-157: 회원 전용 막음과 회원 기간 오류를 코드별 상태로 매핑.
 * 이 예외들만 처리한다. 다른 예외의 기존 동작은 건드리지 않는다.
 */
@RestControllerAdvice
public class MemberExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handle(EmailAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "EMAIL_ALREADY_EXISTS",
                "message", ex.getMessage()));
    }

    @ExceptionHandler(AuthFlowException.class)
    public ResponseEntity<Map<String, Object>> handle(AuthFlowException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", ex.getError().name());
        body.put("message", ex.getMessage());
        body.putAll(ex.getDetails());
        return ResponseEntity.status(ex.getError().status()).body(body);
    }

    @ExceptionHandler(MembershipException.class)
    public ResponseEntity<Map<String, Object>> handle(MembershipException ex) {
        return ResponseEntity.status(ex.getError().status()).body(Map.of(
                "error", ex.getError().name(),
                "message", ex.getMessage()));
    }
}
