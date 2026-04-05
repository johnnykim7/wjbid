package com.biddingagency.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * FlowGuard 헬스체크 엔드포인트.
 * /health — 인증 없이 접근 가능 (내부 네트워크 전용).
 */
@RestController
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("timestamp", Instant.now().toString());
        try {
            dataSource.getConnection().close();
            result.put("status", "ok");
        } catch (Exception e) {
            result.put("status", "error");
            result.put("reason", "DB 연결 실패: " + e.getMessage());
        }
        return result;
    }
}
