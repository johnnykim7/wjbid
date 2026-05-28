-- CR-006: 고객 인앱 알림 — 읽음 상태 관리

-- 1. read_at 컬럼 추가 (NULL = 미읽음). 기존 발송 이력은 미읽음으로 시작.
ALTER TABLE notification_logs ADD COLUMN read_at DATETIME(6) NULL;

-- 2. 고객별 알림 목록 조회용 인덱스 (recipient_id 기준 최신순)
CREATE INDEX idx_notification_recipient ON notification_logs (recipient_id, sent_at);
