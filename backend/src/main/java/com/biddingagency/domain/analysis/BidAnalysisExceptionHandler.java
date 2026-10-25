package com.biddingagency.domain.analysis;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class BidAnalysisExceptionHandler {

    @ExceptionHandler(BidAnalysisException.class)
    public ResponseEntity<Map<String, Object>> handle(BidAnalysisException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", ex.getCode());
        body.put("message", ex.getMessage());
        if (ex.getReason() != null) {
            body.put("reason", ex.getReason().name());
        }
        return ResponseEntity.status(ex.getStatus()).body(body);
    }
}
