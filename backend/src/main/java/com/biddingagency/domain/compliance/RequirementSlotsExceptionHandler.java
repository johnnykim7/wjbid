package com.biddingagency.domain.compliance;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * CR-010 슬롯 미충족 전이 차단을 409로 매핑.
 * 슬롯 예외만 처리 — 다른 예외의 기존 동작은 건드리지 않는다.
 */
@RestControllerAdvice
public class RequirementSlotsExceptionHandler {

    @ExceptionHandler(RequirementSlotsNotFulfilledException.class)
    public ResponseEntity<Map<String, Object>> handle(RequirementSlotsNotFulfilledException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", ex.getErrorCode(),
                "unfulfilledSlots", ex.getUnfulfilledSlots()));
    }
}
