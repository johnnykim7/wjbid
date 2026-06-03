-- V32: CR-036 SAM API 호출 계측 — 일자별·엔드포인트별 집계
-- SAM 쿼터는 추측("1,000?")이 아니라 실측해야 한다. 우리가 몇 번 부르는지(success/error),
-- SAM이 응답 헤더로 알려주는 실제 한도(X-RateLimit-Limit/Remaining), 에러 시 응답 본문을 기록한다.
-- SAM 쿼터 리셋이 UTC 자정이므로 call_date도 UTC 기준으로 적재한다.

CREATE TABLE sam_api_call_log (
    id                   BINARY(16)   NOT NULL,
    call_date            DATE         NOT NULL COMMENT 'UTC 기준 호출 일자 (SAM 쿼터 리셋 UTC 자정)',
    endpoint             VARCHAR(30)  NOT NULL COMMENT 'search | noticedesc | attachment',
    success_count        BIGINT       NOT NULL DEFAULT 0,
    error_count          BIGINT       NOT NULL DEFAULT 0,
    last_status          INT          NULL COMMENT '마지막 HTTP status',
    last_rate_limit      VARCHAR(50)  NULL COMMENT 'SAM X-RateLimit-Limit 헤더 (실제 일일 한도)',
    last_rate_remaining  VARCHAR(50)  NULL COMMENT 'SAM X-RateLimit-Remaining 헤더 (남은 횟수)',
    last_rate_headers    TEXT         NULL COMMENT 'SAM이 준 rate 관련 헤더 전체(JSON)',
    last_error_body      TEXT         NULL COMMENT '마지막 에러 응답 본문(전문, 길면 truncate)',
    last_called_at       DATETIME     NULL COMMENT '마지막 호출 시각(서버 로컬)',
    created_at           DATETIME     NOT NULL,
    updated_at           DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sam_call_date_endpoint (call_date, endpoint)
);
