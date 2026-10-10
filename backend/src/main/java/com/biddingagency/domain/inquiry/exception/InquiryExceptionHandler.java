package com.biddingagency.domain.inquiry.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * CR-132: 문의 예외를 T3-2 의 상태 코드와 {error, message} 본문으로 바꾼다.
 */
@RestControllerAdvice
public class InquiryExceptionHandler {

    @ExceptionHandler(InquiryTypeInvalidException.class)
    public ResponseEntity<Map<String, Object>> handle(InquiryTypeInvalidException ex) {
        return body(HttpStatus.BAD_REQUEST, "INQUIRY_TYPE_INVALID", ex);
    }

    @ExceptionHandler(InquiryNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handle(InquiryNotFoundException ex) {
        return body(HttpStatus.NOT_FOUND, "INQUIRY_NOT_FOUND", ex);
    }

    @ExceptionHandler(InquiryAlreadyAnsweredException.class)
    public ResponseEntity<Map<String, Object>> handle(InquiryAlreadyAnsweredException ex) {
        return body(HttpStatus.CONFLICT, "INQUIRY_ALREADY_ANSWERED", ex);
    }

    @ExceptionHandler(InquiryAnswerMailFailedException.class)
    public ResponseEntity<Map<String, Object>> handle(InquiryAnswerMailFailedException ex) {
        return body(HttpStatus.BAD_GATEWAY, "INQUIRY_ANSWER_MAIL_FAILED", ex);
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String error, RuntimeException ex) {
        return ResponseEntity.status(status).body(Map.of("error", error, "message", ex.getMessage()));
    }
}
