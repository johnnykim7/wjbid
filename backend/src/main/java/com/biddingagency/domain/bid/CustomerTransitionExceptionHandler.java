package com.biddingagency.domain.bid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * CR-017 ③: 고객의 관리자 전용 전이 시도를 403으로 매핑.
 * 이 예외만 처리 — 다른 예외의 기존 동작은 건드리지 않는다.
 */
@RestControllerAdvice
public class CustomerTransitionExceptionHandler {

    @ExceptionHandler(CustomerTransitionNotAllowedException.class)
    public ResponseEntity<Map<String, Object>> handle(CustomerTransitionNotAllowedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "error", "CUSTOMER_TRANSITION_NOT_ALLOWED",
                "attemptedState", ex.getAttemptedState().name(),
                "message", ex.getMessage()));
    }
}
