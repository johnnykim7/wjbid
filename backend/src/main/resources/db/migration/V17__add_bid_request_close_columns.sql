-- Entity와 DB 스키마 불일치 해소 (마이그레이션 누락 컬럼 일괄 추가)

-- BidRequest.closed_at, close_reason
ALTER TABLE bid_requests
    ADD COLUMN closed_at DATETIME(6) NULL AFTER outcome_decided_at,
    ADD COLUMN close_reason VARCHAR(500) NULL AFTER closed_at;

-- NotificationLog.updated_at (BaseEntity 상속 시 자동 추가되는데 마이그레이션에 빠짐)
ALTER TABLE notification_logs
    ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);
