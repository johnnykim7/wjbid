package com.biddingagency.common;

import jakarta.persistence.PersistenceException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * DB 접근 오류의 문구에는 쿼리와 테이블 이름이 담긴다. server.error.include-message=always 라
 * 그대로 두면 /error 응답에 실려 누구에게나 보이므로, 서버 로그에만 남기고 응답은 일반 안내로 바꾼다(CR-130).
 * 응답 모양은 Spring 기본 오류 응답과 같게 둔다. 다른 예외의 기존 동작은 건드리지 않는다.
 */
@Slf4j
@RestControllerAdvice
public class DataAccessExceptionHandler {

    private static final String GENERIC_MESSAGE = "일시적인 서버 오류입니다. 잠시 후 다시 시도해 주세요.";

    @ExceptionHandler({DataAccessException.class, PersistenceException.class})
    public ResponseEntity<Map<String, Object>> handle(RuntimeException ex, HttpServletRequest request) {
        log.error("DB access failed: {} {}", request.getMethod(), request.getRequestURI(), ex);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status).body(Map.of(
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", GENERIC_MESSAGE,
                "path", request.getRequestURI()));
    }
}
