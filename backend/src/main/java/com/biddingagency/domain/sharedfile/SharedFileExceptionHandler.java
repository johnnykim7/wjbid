package com.biddingagency.domain.sharedfile;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class SharedFileExceptionHandler {

    @ExceptionHandler(SharedFileException.class)
    public ResponseEntity<Map<String, Object>> handle(SharedFileException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", ex.getError().name());
        body.put("message", ex.getMessage());
        body.putAll(ex.getDetails());
        return ResponseEntity.status(ex.getError().status()).body(body);
    }
}
