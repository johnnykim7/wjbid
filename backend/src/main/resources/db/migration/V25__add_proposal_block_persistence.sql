-- CR-027: 제안서 Chapter/Section/Block DB 영속화
-- 기존 bid_document_versions.content_json 단일 LongText → 공고 Section L/M 트리 기반 영속화로 전환.
-- 부분 재생성·요구사항 추적·샘플 활용의 1차 단계.
--
-- 양식 갈래: 자체 양식 없음. AI는 공고 Section L/M에서 발췌·구조화 (FACTOR/Subfactor 라벨·순서 모두 공고 그대로).
-- 산출물 모델: chapter(FACTOR) → section(Subfactor) → block(단락/표/근거).
--
-- 1차 범위 (사용자 결정 2026-06-01): 핵심 4테이블 + generation_log.
--   Notice 컬럼(factor_tree/price_items) · member_profile · content_json DROP 은 후속 CR.

-- ─────────────────────────────────────────────────────────────
-- 1. proposal_chapter — FACTOR 단위 (공고가 박은 라벨 그대로)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE proposal_chapter (
    id BINARY(16) NOT NULL,
    document_id BINARY(16) NOT NULL,
    factor_label VARCHAR(50) COMMENT '공고가 박은 라벨 그대로 ("I", "A", "1")',
    factor_title VARCHAR(500),
    source_section VARCHAR(20) NOT NULL DEFAULT 'NOTICE_M' COMMENT 'NOTICE_L / NOTICE_M / SECTION_K',
    order_no INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_proposal_chapter_document FOREIGN KEY (document_id) REFERENCES bid_documents(id) ON DELETE CASCADE,
    KEY idx_proposal_chapter_document (document_id),
    KEY idx_proposal_chapter_order (document_id, order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─────────────────────────────────────────────────────────────
-- 2. proposal_section — Subfactor 단위. CR-028 write-section 과 1:1.
--    Subfactor 강제 안 한 공고는 section 1개로 통합 (subfactor_label NULL).
-- ─────────────────────────────────────────────────────────────
CREATE TABLE proposal_section (
    id BINARY(16) NOT NULL,
    chapter_id BINARY(16) NOT NULL,
    subfactor_label VARCHAR(50) COMMENT '공고 그대로, NULL 가능 (통합 section)',
    title VARCHAR(500),
    scope TEXT COMMENT '작성 지침',
    requirement_refs JSON COMMENT 'Section L/M 발췌 ID 배열',
    min_words INT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/DRAFTING/DRAFTED/VERIFIED/NEEDS_REGEN/LOCKED',
    order_no INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_proposal_section_chapter FOREIGN KEY (chapter_id) REFERENCES proposal_chapter(id) ON DELETE CASCADE,
    KEY idx_proposal_section_chapter (chapter_id),
    KEY idx_proposal_section_status (status),
    KEY idx_proposal_section_order (chapter_id, order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─────────────────────────────────────────────────────────────
-- 3. proposal_block — 단락/표/근거. TipTap node 단위.
-- ─────────────────────────────────────────────────────────────
CREATE TABLE proposal_block (
    id BINARY(16) NOT NULL,
    section_id BINARY(16) NOT NULL,
    block_type VARCHAR(20) NOT NULL DEFAULT 'PARAGRAPH' COMMENT 'PARAGRAPH/BULLET_LIST/TABLE/IMAGE/EVIDENCE',
    content_json JSON COMMENT 'TipTap node',
    source_evidence JSON COMMENT '어떤 client doc/sample에서 가져왔는지',
    order_no INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_proposal_block_section FOREIGN KEY (section_id) REFERENCES proposal_section(id) ON DELETE CASCADE,
    KEY idx_proposal_block_section (section_id),
    KEY idx_proposal_block_order (section_id, order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─────────────────────────────────────────────────────────────
-- 4. proposal_attachment — Price XLSX·기타 첨부.
--    실측: 이 코드베이스는 attachment_storage 테이블 없이 StorageService(MinIO/Local) 경로 직접 보관.
--    → file_id FK 대신 storage_key VARCHAR 로 보관 (사용자 결정 2026-06-01).
-- ─────────────────────────────────────────────────────────────
CREATE TABLE proposal_attachment (
    id BINARY(16) NOT NULL,
    document_id BINARY(16) NOT NULL,
    file_type VARCHAR(30) NOT NULL DEFAULT 'OTHER_ATTACHMENT' COMMENT 'PRICE_XLSX / OTHER_ATTACHMENT',
    storage_key VARCHAR(1024) NOT NULL COMMENT 'StorageService key/path (MinIO 또는 Local)',
    original_filename VARCHAR(500),
    content_type VARCHAR(255),
    file_size BIGINT,
    uploaded_by BINARY(16),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_proposal_attachment_document FOREIGN KEY (document_id) REFERENCES bid_documents(id) ON DELETE CASCADE,
    KEY idx_proposal_attachment_document (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─────────────────────────────────────────────────────────────
-- 5. generation_log — CR-029 비용 추적 선반영.
--    테이블만 처음부터 박아 비용 데이터 누적 시작 (라우팅 본격은 CR-029).
-- ─────────────────────────────────────────────────────────────
CREATE TABLE generation_log (
    id BINARY(16) NOT NULL,
    target_type VARCHAR(20) NOT NULL COMMENT 'chapter / section / block / verify',
    target_id BINARY(16),
    workflow_id VARCHAR(255),
    run_id VARCHAR(255),
    model_name VARCHAR(100),
    tokens_in INT,
    tokens_out INT,
    cost_usd DECIMAL(12, 6),
    prompt_hash VARCHAR(64) COMMENT '재실행 캐시 검증',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_generation_log_target (target_type, target_id),
    KEY idx_generation_log_run (run_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
