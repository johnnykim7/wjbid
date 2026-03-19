-- V5: Role 단순화 (ADMIN + CUSTOMER) + 구독 필드 제거
-- AI-SDLC 역설계 기준 Sprint 1

-- 1. MEMBER → CUSTOMER 변환, 기타 역할 → ADMIN 변환
UPDATE members SET role = 'CUSTOMER' WHERE role = 'MEMBER';
UPDATE members SET role = 'ADMIN' WHERE role IN ('ANALYST', 'WRITER', 'REVIEWER', 'SUBMITTER');

-- 2. Role ENUM 재정의 (ADMIN, CUSTOMER만)
ALTER TABLE members MODIFY COLUMN role ENUM('ADMIN', 'CUSTOMER') NOT NULL DEFAULT 'CUSTOMER';

-- 3. 구독 관련 컬럼 제거 (MVP 범위 외)
ALTER TABLE members DROP COLUMN IF EXISTS subscription_status;
ALTER TABLE members DROP COLUMN IF EXISTS subscription_started_at;
ALTER TABLE members DROP COLUMN IF EXISTS subscription_expires_at;

-- 4. 구독 상태 인덱스 제거
DROP INDEX IF EXISTS idx_members_subscription_status ON members;

-- 5. BidRequest에 service_level 컬럼 추가
ALTER TABLE bid_requests ADD COLUMN IF NOT EXISTS service_level VARCHAR(20) DEFAULT 'FULL_PACKAGE';

-- 6. BidRequest 상태 ENUM 재정의 (9개 상태)
-- 기존 상태 매핑: WAITING_FOR_CLIENT_DOCS→DOCS_PENDING, CLIENT_DOCS_RECEIVED→DOCS_RECEIVED,
-- REQUIREMENT_ANALYSIS→ANALYZING, DOCUMENT_DRAFTING→GENERATING,
-- INTERNAL_REVIEW→REVIEW, CLIENT_CONFIRMATION→CONFIRMED,
-- READY_FOR_SUBMISSION/SUBMITTING→CONFIRMED, SUBMIT_FAILED→CLOSED
UPDATE bid_requests SET state = 'DOCS_PENDING' WHERE state = 'WAITING_FOR_CLIENT_DOCS';
UPDATE bid_requests SET state = 'DOCS_RECEIVED' WHERE state = 'CLIENT_DOCS_RECEIVED';
UPDATE bid_requests SET state = 'ANALYZING' WHERE state = 'REQUIREMENT_ANALYSIS';
UPDATE bid_requests SET state = 'GENERATING' WHERE state = 'DOCUMENT_DRAFTING';
UPDATE bid_requests SET state = 'REVIEW' WHERE state = 'INTERNAL_REVIEW';
UPDATE bid_requests SET state = 'CONFIRMED' WHERE state IN ('CLIENT_CONFIRMATION', 'READY_FOR_SUBMISSION', 'SUBMITTING');
UPDATE bid_requests SET state = 'CLOSED' WHERE state = 'SUBMIT_FAILED';

ALTER TABLE bid_requests MODIFY COLUMN state VARCHAR(30) NOT NULL DEFAULT 'CREATED';

-- 7. BidDocument 상태 정리 (DRAFT, LOCKED, ARCHIVED)
UPDATE bid_documents SET status = 'LOCKED' WHERE status = 'APPROVED';
UPDATE bid_documents SET status = 'DRAFT' WHERE status = 'READY';

-- 8. 공고 첨부문서 테이블 생성
CREATE TABLE IF NOT EXISTS opportunity_attachments (
    id BINARY(16) NOT NULL,
    opportunity_id BINARY(16) NOT NULL,
    file_name VARCHAR(500) NOT NULL,
    file_size BIGINT,
    content_type VARCHAR(100),
    source_url TEXT NOT NULL,
    storage_url TEXT,
    download_status VARCHAR(20) NOT NULL DEFAULT 'FAILED',
    downloaded_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_opp_attachments_opportunity FOREIGN KEY (opportunity_id) REFERENCES opportunities(id),
    INDEX idx_opp_attachments_opportunity (opportunity_id),
    INDEX idx_opp_attachments_status (download_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. 알림 이력 테이블 생성
CREATE TABLE IF NOT EXISTS notification_logs (
    id BINARY(16) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    recipient_id BINARY(16),
    subject VARCHAR(500) NOT NULL,
    reference_id BINARY(16),
    reference_type VARCHAR(50),
    sent_at DATETIME(6) NOT NULL,
    success BOOLEAN NOT NULL,
    error_message TEXT,
    idempotency_key VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uniq_notification_idempotency (idempotency_key),
    INDEX idx_notification_type (notification_type),
    INDEX idx_notification_reference (reference_type, reference_id),
    INDEX idx_notification_sent_at (sent_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. 고객 문서 테이블에 bid_request_id 추가 (기존 테이블 보완)
ALTER TABLE client_documents ADD COLUMN IF NOT EXISTS bid_request_id BINARY(16);
ALTER TABLE client_documents ADD COLUMN IF NOT EXISTS document_category VARCHAR(100);

-- 11. BidDocument에 template_id 추가
ALTER TABLE bid_documents ADD COLUMN IF NOT EXISTS template_id BINARY(16);
