-- CR-169 고객 서류 읽기와 절별 근거 서류(BID-DOC-004, BID-DOC-005, BIZ-085~089).
-- 읽은 결과는 파일 내용 해시 하나에 한 줄이라 같은 내용은 한 번만 읽는다.
-- 서류 두 표의 해시는 파일을 읽어야 채울 수 있어 여기서 채우지 않고, 읽기를 시작할 때 비어 있는 줄을 채운다
CREATE TABLE document_readings (
    id BINARY(16) NOT NULL,
    content_sha256 CHAR(64) NOT NULL,
    file_name VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NULL,
    read_as VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    status_at DATETIME(6) NOT NULL,
    facts_json LONGTEXT NULL,
    fact_count INT NOT NULL DEFAULT 0,
    vision_used BOOLEAN NOT NULL DEFAULT 0,
    failure_reason VARCHAR(500) NULL,
    workflow_run_id VARCHAR(100) NULL,
    read_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_document_readings_sha256 UNIQUE (content_sha256)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE client_documents ADD COLUMN content_sha256 CHAR(64) NULL;
CREATE INDEX idx_client_documents_sha256 ON client_documents (content_sha256);

ALTER TABLE member_documents ADD COLUMN content_sha256 CHAR(64) NULL;
CREATE INDEX idx_member_documents_sha256 ON member_documents (content_sha256);

ALTER TABLE proposal_section ADD COLUMN source_reading_ids JSON NULL;
