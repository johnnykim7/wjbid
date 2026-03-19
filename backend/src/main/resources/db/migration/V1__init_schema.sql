-- ====================================
-- SAM.gov Bidding Agency Platform
-- Initial Schema Migration
-- ====================================

-- Members (회원)
CREATE TABLE members (
    id BINARY(16) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    business_registration_number VARCHAR(50),
    contact_person VARCHAR(100),
    phone VARCHAR(50),
    address TEXT,
    role VARCHAR(50) NOT NULL DEFAULT 'MEMBER',
    subscription_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    subscription_started_at DATETIME(6),
    subscription_expires_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    last_login_at DATETIME(6),
    INDEX idx_members_email (email),
    INDEX idx_members_subscription_status (subscription_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Client Documents (고객 서류 - 재사용 가능)
CREATE TABLE client_documents (
    id BINARY(16) PRIMARY KEY,
    member_id BINARY(16) NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100),
    uploaded_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    expires_at DATETIME(6),
    FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE,
    INDEX idx_client_docs_member (member_id),
    INDEX idx_client_docs_type (document_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Opportunities (공고)
CREATE TABLE opportunities (
    id BINARY(16) PRIMARY KEY,
    notice_id VARCHAR(255) NOT NULL UNIQUE,
    solicitation_number VARCHAR(255),
    title VARCHAR(500) NOT NULL,
    type VARCHAR(100),
    organization_name VARCHAR(255),
    posted_date DATETIME(6),
    response_deadline DATETIME(6),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    ui_link TEXT,
    description_link TEXT,
    content_hash VARCHAR(64),
    first_seen_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_modified_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    raw_json JSON,
    INDEX idx_opportunities_notice_id (notice_id),
    INDEX idx_opportunities_posted_date (posted_date),
    INDEX idx_opportunities_deadline (response_deadline),
    INDEX idx_opportunities_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Opportunity Source Raw (원문 보관)
CREATE TABLE opportunity_source_raw (
    id BINARY(16) PRIMARY KEY,
    opportunity_id BINARY(16) NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    raw_content MEDIUMTEXT NOT NULL,
    fetched_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    FOREIGN KEY (opportunity_id) REFERENCES opportunities(id) ON DELETE CASCADE,
    INDEX idx_source_raw_opportunity (opportunity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Opportunity Match (키워드 매칭 로그)
CREATE TABLE opportunity_matches (
    id BINARY(16) PRIMARY KEY,
    opportunity_id BINARY(16) NOT NULL,
    keyword VARCHAR(255) NOT NULL,
    keyword_group VARCHAR(50) NOT NULL,
    matched_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    FOREIGN KEY (opportunity_id) REFERENCES opportunities(id) ON DELETE CASCADE,
    INDEX idx_matches_opportunity (opportunity_id),
    INDEX idx_matches_keyword_group (keyword_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Opportunity Requirement Sets (요구사항 세트)
CREATE TABLE opportunity_requirement_sets (
    id BINARY(16) PRIMARY KEY,
    opportunity_id BINARY(16) NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    source_ref VARCHAR(500),
    extracted_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    extraction_version VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'EXTRACTED',
    FOREIGN KEY (opportunity_id) REFERENCES opportunities(id) ON DELETE CASCADE,
    INDEX idx_req_sets_opportunity (opportunity_id),
    INDEX idx_req_sets_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Opportunity Requirement Items (요구사항 항목)
CREATE TABLE opportunity_requirement_items (
    id BINARY(16) PRIMARY KEY,
    requirement_set_id BINARY(16) NOT NULL,
    category VARCHAR(50) NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    requirement_json JSON NOT NULL,
    is_blocker BOOLEAN NOT NULL DEFAULT FALSE,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    confidence DECIMAL(3,2),
    manual_override BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    FOREIGN KEY (requirement_set_id) REFERENCES opportunity_requirement_sets(id) ON DELETE CASCADE,
    INDEX idx_req_items_set (requirement_set_id),
    INDEX idx_req_items_category (category),
    INDEX idx_req_items_blocker (is_blocker)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bid Requests (입찰 신청)
CREATE TABLE bid_requests (
    id BINARY(16) PRIMARY KEY,
    member_id BINARY(16) NOT NULL,
    opportunity_id BINARY(16) NOT NULL,
    state VARCHAR(50) NOT NULL,
    state_history JSON NOT NULL,
    assigned_to BINARY(16),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    submitted_at DATETIME(6),
    metadata JSON,
    FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE,
    FOREIGN KEY (opportunity_id) REFERENCES opportunities(id),
    FOREIGN KEY (assigned_to) REFERENCES members(id),
    INDEX idx_bid_requests_member (member_id),
    INDEX idx_bid_requests_opportunity (opportunity_id),
    INDEX idx_bid_requests_state (state),
    INDEX idx_bid_requests_assigned (assigned_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bid Authorization (전자 동의)
CREATE TABLE bid_authorizations (
    id BINARY(16) PRIMARY KEY,
    bid_request_id BINARY(16) NOT NULL,
    agreed_terms_version VARCHAR(50) NOT NULL,
    signed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    signer_name VARCHAR(100) NOT NULL,
    signer_ip VARCHAR(45),
    document_snapshot_path VARCHAR(500),
    FOREIGN KEY (bid_request_id) REFERENCES bid_requests(id) ON DELETE CASCADE,
    INDEX idx_authorizations_bid_request (bid_request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bid Documents (문서 컨테이너)
CREATE TABLE bid_documents (
    id BINARY(16) PRIMARY KEY,
    bid_request_id BINARY(16) NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    title VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    current_version_no INT NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    FOREIGN KEY (bid_request_id) REFERENCES bid_requests(id) ON DELETE CASCADE,
    INDEX idx_bid_docs_bid_request (bid_request_id),
    INDEX idx_bid_docs_status (status),
    INDEX idx_bid_docs_type (document_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bid Document Versions (불변 버전)
CREATE TABLE bid_document_versions (
    id BINARY(16) PRIMARY KEY,
    document_id BINARY(16) NOT NULL,
    version_no INT NOT NULL,
    content_json JSON NOT NULL,
    edited_by BINARY(16) NOT NULL,
    edited_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    change_summary TEXT,
    word_count INT,
    FOREIGN KEY (document_id) REFERENCES bid_documents(id) ON DELETE CASCADE,
    FOREIGN KEY (edited_by) REFERENCES members(id),
    UNIQUE KEY uk_doc_version (document_id, version_no),
    INDEX idx_doc_versions_document (document_id),
    INDEX idx_doc_versions_edited_by (edited_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bid Document Assets (첨부 파일)
CREATE TABLE bid_document_assets (
    id BINARY(16) PRIMARY KEY,
    version_id BINARY(16) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_type VARCHAR(100),
    file_size BIGINT NOT NULL,
    sha256_hash VARCHAR(64),
    uploaded_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    FOREIGN KEY (version_id) REFERENCES bid_document_versions(id) ON DELETE CASCADE,
    INDEX idx_assets_version (version_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Requirement Fulfillment Maps (Compliance 매핑)
CREATE TABLE requirement_fulfillment_maps (
    id BINARY(16) PRIMARY KEY,
    bid_request_id BINARY(16) NOT NULL,
    requirement_item_id BINARY(16) NOT NULL,
    fulfillment_type VARCHAR(30) NOT NULL,
    document_id BINARY(16),
    document_section_path TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    notes TEXT,
    mapped_by BINARY(16),
    mapped_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    FOREIGN KEY (bid_request_id) REFERENCES bid_requests(id) ON DELETE CASCADE,
    FOREIGN KEY (requirement_item_id) REFERENCES opportunity_requirement_items(id),
    FOREIGN KEY (document_id) REFERENCES bid_documents(id),
    FOREIGN KEY (mapped_by) REFERENCES members(id),
    INDEX idx_fulfillment_bid_request (bid_request_id),
    INDEX idx_fulfillment_requirement (requirement_item_id),
    INDEX idx_fulfillment_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Validation Runs (검증 실행 이력)
CREATE TABLE validation_runs (
    id BINARY(16) PRIMARY KEY,
    bid_request_id BINARY(16) NOT NULL,
    run_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    status VARCHAR(20) NOT NULL,
    blocker_count INT NOT NULL DEFAULT 0,
    warning_count INT NOT NULL DEFAULT 0,
    fulfillment_rate DECIMAL(5,2),
    result_json JSON,
    FOREIGN KEY (bid_request_id) REFERENCES bid_requests(id) ON DELETE CASCADE,
    INDEX idx_validation_bid_request (bid_request_id),
    INDEX idx_validation_run_at (run_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bid Packages (제출 패키지)
CREATE TABLE bid_packages (
    id BINARY(16) PRIMARY KEY,
    bid_request_id BINARY(16) NOT NULL,
    package_version INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by BINARY(16) NOT NULL,
    file_manifest JSON NOT NULL,
    FOREIGN KEY (bid_request_id) REFERENCES bid_requests(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES members(id),
    INDEX idx_packages_bid_request (bid_request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Submission Attempts (제출 시도)
CREATE TABLE submission_attempts (
    id BINARY(16) PRIMARY KEY,
    bid_request_id BINARY(16) NOT NULL,
    package_id BINARY(16),
    channel VARCHAR(30) NOT NULL,
    attempted_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    result VARCHAR(20) NOT NULL,
    submitted_documents JSON,
    proof_archive_path VARCHAR(500),
    logs TEXT,
    FOREIGN KEY (bid_request_id) REFERENCES bid_requests(id) ON DELETE CASCADE,
    FOREIGN KEY (package_id) REFERENCES bid_packages(id),
    INDEX idx_submissions_bid_request (bid_request_id),
    INDEX idx_submissions_result (result)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Event Logs (감사 로그)
CREATE TABLE event_logs (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16),
    user_role VARCHAR(30),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BINARY(16) NOT NULL,
    changes_json JSON,
    timestamp DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ip_address VARCHAR(45),
    user_agent TEXT,
    result VARCHAR(20),
    error_message TEXT,
    FOREIGN KEY (user_id) REFERENCES members(id),
    INDEX idx_event_logs_entity (entity_type, entity_id),
    INDEX idx_event_logs_user (user_id),
    INDEX idx_event_logs_timestamp (timestamp DESC),
    INDEX idx_event_logs_action (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Keyword Groups (키워드 관리)
CREATE TABLE keyword_groups (
    id BINARY(16) PRIMARY KEY,
    group_name VARCHAR(50) NOT NULL UNIQUE,
    group_type VARCHAR(30) NOT NULL,
    keywords JSON NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    priority INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_keyword_groups_active (active),
    INDEX idx_keyword_groups_type (group_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Collector Runs (수집 실행 로그)
CREATE TABLE collector_runs (
    id BINARY(16) PRIMARY KEY,
    keyword_group VARCHAR(50) NOT NULL,
    started_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6),
    new_count INT NOT NULL DEFAULT 0,
    duplicate_count INT NOT NULL DEFAULT 0,
    error_count INT NOT NULL DEFAULT 0,
    errors TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'RUNNING',
    INDEX idx_collector_runs_group (keyword_group),
    INDEX idx_collector_runs_started (started_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Document Templates (문서 템플릿)
CREATE TABLE document_templates (
    id BINARY(16) PRIMARY KEY,
    template_name VARCHAR(255) NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    template_version INT NOT NULL DEFAULT 1,
    content_json JSON NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_templates_type (document_type),
    INDEX idx_templates_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
