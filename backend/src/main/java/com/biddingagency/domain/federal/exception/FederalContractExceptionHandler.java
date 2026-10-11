package com.biddingagency.domain.federal.exception;

import com.biddingagency.domain.federal.service.SyncAlreadyRunningException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * CR-142: 미 연방 계약 예외를 T3-2 의 상태 코드와 {error, message} 본문으로 바꾼다.
 */
@RestControllerAdvice
public class FederalContractExceptionHandler {

    @ExceptionHandler(FederalContractException.class)
    public ResponseEntity<Map<String, Object>> handle(FederalContractException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of("error", ex.getError(), "message", ex.getMessage()));
    }

    @ExceptionHandler(SyncAlreadyRunningException.class)
    public ResponseEntity<Map<String, Object>> handle(SyncAlreadyRunningException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "FEDERAL_SYNC_RUNNING", "message", ex.getMessage()));
    }
}
