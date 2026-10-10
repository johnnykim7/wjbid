package com.biddingagency.domain.member;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * CR-131: 이미 가입된 이메일을 409 로 매핑.
 * 이 예외만 처리한다. 다른 예외의 기존 동작은 건드리지 않는다.
 */
@RestControllerAdvice
public class MemberExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handle(EmailAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "EMAIL_ALREADY_EXISTS",
                "message", ex.getMessage()));
    }
}
