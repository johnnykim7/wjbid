-- CR-016: 원본(opportunities) / 공고문(notices) 분리
-- 원본은 SAM 수집물(선별 풀), 공고문은 관리자가 선별해 한글화+요약한 산출물(별개 엔티티).
-- 기존 opportunity_analysis(한글화+요약 결과)를 notices로 흡수. opportunities.visibility는 notices로 이동.

-- 1. notices 테이블 신설 (opportunity 1:N notice)
CREATE TABLE notices (
    id BINARY(16) NOT NULL,
    opportunity_id BINARY(16) NOT NULL,
    -- 한글화 생성 진행 상태 (구 opportunity_analysis.status)
    generation_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    -- 고객 노출 상태 (구 opportunities.visibility)
    visibility VARCHAR(20) NOT NULL DEFAULT 'HIDDEN',
    korean_title VARCHAR(500),
    summary_json LONGTEXT,
    document_formats_json LONGTEXT,
    required_documents_json LONGTEXT,
    llm_prompt_preset_json LONGTEXT,
    analyzed_at DATETIME(6),
    error_message TEXT,
    workflow_run_id VARCHAR(255),
    created_by BINARY(16),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_notices_opportunity FOREIGN KEY (opportunity_id) REFERENCES opportunities(id) ON DELETE CASCADE,
    KEY idx_notices_opportunity (opportunity_id),
    KEY idx_notices_generation_status (generation_status),
    KEY idx_notices_visibility (visibility)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 기존 opportunity_analysis → notices 이관
--    visibility는 해당 opportunity의 기존 visibility를 승계 (분석 완료분이 노출 중이었으면 유지).
INSERT INTO notices (
    id, opportunity_id, generation_status, visibility,
    summary_json, document_formats_json, required_documents_json, llm_prompt_preset_json,
    analyzed_at, error_message, workflow_run_id, created_at, updated_at
)
SELECT
    a.id, a.opportunity_id, a.status, o.visibility,
    a.summary_json, a.document_formats_json, a.required_documents_json, a.llm_prompt_preset_json,
    a.analyzed_at, a.error_message, a.workflow_run_id, a.created_at, a.updated_at
FROM opportunity_analysis a
JOIN opportunities o ON o.id = a.opportunity_id;

-- 3. 기존 opportunity_analysis 테이블 폐지 (notices로 흡수됨)
DROP TABLE opportunity_analysis;

-- 4. opportunities.visibility 제거 (노출 개념은 notices로 이동, 원본은 선별 풀)
DROP INDEX idx_opportunities_visibility ON opportunities;
ALTER TABLE opportunities DROP COLUMN visibility;
