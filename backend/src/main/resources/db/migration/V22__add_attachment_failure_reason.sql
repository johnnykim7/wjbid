-- V22: CR-025 첨부 자동 다운로드 — PENDING 상태 + failure_reason 컬럼
-- PENDING: 자동 다운로드 큐 대기/진행 상태. FAILED에 사유 텍스트 보관.

ALTER TABLE opportunity_attachments
  ADD COLUMN failure_reason VARCHAR(500) NULL AFTER downloaded_at;
