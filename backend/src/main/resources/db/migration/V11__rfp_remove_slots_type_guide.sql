-- CR-013 재설계: 슬롯(7축 사전 분류) 폐기 — 원본 통째 보관 + 공고유형별 가이드
-- 슬롯으로 원본을 쪼개는 구조(slot_definition/slot_assignment)를 제거하고,
-- pattern_guide를 슬롯 단위 → 공고유형(industry_type) 단위로 전환한다.

-- 1. slot_definition 을 FK로 참조하는 테이블을 먼저 제거해야 한다.
--    참조자: slot_assignment, pattern_guide(slot_definition_id). 이 둘을 먼저 드롭.
DROP TABLE IF EXISTS slot_assignment;
-- pattern_guide(V10, 슬롯FK 보유)는 어차피 유형단위로 재구축 → 먼저 드롭
DROP TABLE IF EXISTS pattern_guide;
-- 이제 참조자가 없으므로 slot_definition 드롭 가능
DROP TABLE IF EXISTS slot_definition;

CREATE TABLE pattern_guide (
    id BINARY(16) NOT NULL,
    industry_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    source VARCHAR(20) NOT NULL DEFAULT 'AI_EXTRACTED',
    guide_json LONGTEXT,
    guide_markdown LONGTEXT,
    sample_count INT,
    extracted_at DATETIME(6),
    error_message TEXT,
    workflow_run_id VARCHAR(255),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uniq_pattern_guide_industry (industry_type),
    KEY idx_pattern_guide_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
