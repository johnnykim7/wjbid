-- CR-003: 공고 사전 분석 파이프라인 + 노출 관리

-- 1. Add visibility column to opportunities (default HIDDEN for new, VISIBLE for existing)
ALTER TABLE opportunities ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'HIDDEN';
CREATE INDEX idx_opportunities_visibility ON opportunities (visibility);

-- 2. Set existing active opportunities to VISIBLE (서비스 중단 방지)
UPDATE opportunities SET visibility = 'VISIBLE' WHERE active = true;

-- 3. Create opportunity_analysis table
CREATE TABLE opportunity_analysis (
    id BINARY(16) NOT NULL,
    opportunity_id BINARY(16) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    summary_json JSON,
    document_formats_json JSON,
    required_documents_json JSON,
    llm_prompt_preset_json JSON,
    analyzed_at DATETIME(6),
    error_message TEXT,
    workflow_run_id VARCHAR(255),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uniq_opp_analysis_opportunity (opportunity_id),
    CONSTRAINT fk_opp_analysis_opportunity FOREIGN KEY (opportunity_id) REFERENCES opportunities(id) ON DELETE CASCADE,
    KEY idx_opp_analysis_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
