package com.biddingagency.domain.reading;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class DocumentReadingExceptionHandler {

    @ExceptionHandler(DocumentReadingException.class)
    public ResponseEntity<Map<String, Object>> handle(DocumentReadingException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of(
                "error", ex.getCode(),
                "message", ex.getMessage()));
    }
}
