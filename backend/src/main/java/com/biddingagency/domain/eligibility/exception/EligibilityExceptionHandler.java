package com.biddingagency.domain.eligibility.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * CR-137: 적격성 진단 예외를 T3-2 의 상태 코드와 {error, message} 본문으로 바꾼다.
 */
@RestControllerAdvice
public class EligibilityExceptionHandler {

    @ExceptionHandler(EligibilityNoticeNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handle(EligibilityNoticeNotFoundException ex) {
        return body(HttpStatus.NOT_FOUND, "ELIGIBILITY_NOTICE_NOT_FOUND", ex);
    }

    @ExceptionHandler(EligibilityItemInvalidException.class)
    public ResponseEntity<Map<String, Object>> handle(EligibilityItemInvalidException ex) {
        return body(HttpStatus.BAD_REQUEST, "ELIGIBILITY_ITEM_INVALID", ex);
    }

    @ExceptionHandler(EligibilityAnswerInvalidException.class)
    public ResponseEntity<Map<String, Object>> handle(EligibilityAnswerInvalidException ex) {
        return body(HttpStatus.BAD_REQUEST, "ELIGIBILITY_ANSWER_INVALID", ex);
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String error, RuntimeException ex) {
        return ResponseEntity.status(status).body(Map.of("error", error, "message", ex.getMessage()));
    }
}
