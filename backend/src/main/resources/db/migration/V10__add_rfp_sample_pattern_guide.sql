-- CR-013: 성공 제안서 패턴 가이드 — 관리자 등록 + 슬롯 패턴 추출
-- 5테이블: slot_definition(시드) / rfp_sample / rfp_sample_file / slot_assignment / pattern_guide

-- 1. 표준 슬롯 정의 (시드 데이터로 정의 — 코드 하드코딩 금지)
CREATE TABLE slot_definition (
    id BINARY(16) NOT NULL,
    slot_code VARCHAR(40) NOT NULL,
    label_ko VARCHAR(100) NOT NULL,
    display_order INT NOT NULL,
    is_other BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uniq_slot_definition_code (slot_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 시드 8행 (표준 7슬롯 + 기타)
INSERT INTO slot_definition (id, slot_code, label_ko, display_order, is_other) VALUES
    (UNHEX(REPLACE(UUID(), '-', '')), 'BIZ_QUALIFICATION', '사업자 자격/등록', 1, FALSE),
    (UNHEX(REPLACE(UUID(), '-', '')), 'PRIOR_EXPERIENCE',  '과거 수행 경험',   2, FALSE),
    (UNHEX(REPLACE(UUID(), '-', '')), 'KEY_PERSONNEL',     '핵심 인력 (CM/QCM)', 3, FALSE),
    (UNHEX(REPLACE(UUID(), '-', '')), 'STAFFING_PLAN',     '인력 배치 계획',   4, FALSE),
    (UNHEX(REPLACE(UUID(), '-', '')), 'EQUIPMENT_PLAN',    '장비 계획',        5, FALSE),
    (UNHEX(REPLACE(UUID(), '-', '')), 'PAST_PERFORMANCE',  '과거 실적',        6, FALSE),
    (UNHEX(REPLACE(UUID(), '-', '')), 'PRICE',             '가격',            7, FALSE),
    (UNHEX(REPLACE(UUID(), '-', '')), 'OTHER',             '기타',            99, TRUE);

-- 2. 성공 제안서 (등록 단위)
CREATE TABLE rfp_sample (
    id BINARY(16) NOT NULL,
    opportunity_no VARCHAR(50) NOT NULL,
    industry_type VARCHAR(30) NOT NULL,
    outcome VARCHAR(20) NOT NULL,
    company VARCHAR(200),
    agency VARCHAR(200),
    award_amount BIGINT,
    fiscal_year INT,
    use_for_pattern BOOLEAN NOT NULL DEFAULT TRUE,
    factors_json JSON,
    note TEXT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_rfp_sample_industry (industry_type),
    KEY idx_rfp_sample_outcome (outcome)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 성공 제안서 원본 파일 (원본만 보관 — 텍스트 추출/파싱은 Aimbase가 담당)
CREATE TABLE rfp_sample_file (
    id BINARY(16) NOT NULL,
    rfp_sample_id BINARY(16) NOT NULL,
    file_name VARCHAR(500) NOT NULL,
    file_size BIGINT,
    content_type VARCHAR(100),
    storage_url TEXT NOT NULL,
    is_pws BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_rfp_file_sample FOREIGN KEY (rfp_sample_id) REFERENCES rfp_sample(id) ON DELETE CASCADE,
    KEY idx_rfp_file_sample (rfp_sample_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. 슬롯 배치 (파일/섹션 → 표준 7슬롯)
CREATE TABLE slot_assignment (
    id BINARY(16) NOT NULL,
    rfp_sample_id BINARY(16) NOT NULL,
    slot_definition_id BINARY(16) NOT NULL,
    sample_file_id BINARY(16),
    other_label VARCHAR(100),
    section_text LONGTEXT,
    confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    auto_estimated BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_slot_asg_sample FOREIGN KEY (rfp_sample_id) REFERENCES rfp_sample(id) ON DELETE CASCADE,
    CONSTRAINT fk_slot_asg_slot FOREIGN KEY (slot_definition_id) REFERENCES slot_definition(id),
    CONSTRAINT fk_slot_asg_file FOREIGN KEY (sample_file_id) REFERENCES rfp_sample_file(id) ON DELETE CASCADE,
    KEY idx_slot_asg_sample (rfp_sample_id),
    KEY idx_slot_asg_slot (slot_definition_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. 슬롯별 패턴 가이드 (추출 결과)
CREATE TABLE pattern_guide (
    id BINARY(16) NOT NULL,
    slot_definition_id BINARY(16) NOT NULL,
    industry_type VARCHAR(30),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    source VARCHAR(20) NOT NULL DEFAULT 'AI_EXTRACTED',
    guide_json JSON,
    guide_markdown LONGTEXT,
    sample_count INT,
    extracted_at DATETIME(6),
    error_message TEXT,
    workflow_run_id VARCHAR(255),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_pattern_guide_slot FOREIGN KEY (slot_definition_id) REFERENCES slot_definition(id),
    UNIQUE KEY uniq_guide_slot_industry (slot_definition_id, industry_type),
    KEY idx_pattern_guide_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
