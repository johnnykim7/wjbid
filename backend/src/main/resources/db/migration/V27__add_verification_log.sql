-- CR-031: 출력 충실성·분량 검증 파이프라인
-- LLM 산출물(공고문 정제 / 제안서 section)이 ① 원문·첨부에 근거하는지(환각 차단)
-- ② 분량이 적정한지를 검증한 결과를 append-only 로 적재.
--
-- 검증 두 경로:
--   BE 정형 룰(LLM 0콜) — 분량/첨부 0건/block 0개/min_words. method=RULE 로 기록.
--   별 LLM 워크플로우(proposal-verify-fidelity, Haiku) — 문장 단위 환각·누락. method=LLM 으로 기록.
--
-- target_type: NOTICE(공고문 정제) / PROPOSAL_SECTION(제안서 section)
-- verdict: PASS / FAIL
-- 관리자 콘솔(CR-030)이 target 별 최신 1건을 배지·패널로 노출.

CREATE TABLE verification_log (
    id BINARY(16) NOT NULL,
    target_type VARCHAR(20) NOT NULL COMMENT 'NOTICE / PROPOSAL_SECTION',
    target_id BINARY(16) NOT NULL COMMENT 'noticeId 또는 sectionId',
    method VARCHAR(10) NOT NULL DEFAULT 'LLM' COMMENT 'RULE(BE 정형 룰) / LLM(verify-fidelity)',
    verdict VARCHAR(10) NOT NULL COMMENT 'PASS / FAIL',
    workflow_run_id VARCHAR(255) COMMENT 'LLM 검증 시 Aimbase runId (RULE 이면 NULL)',
    hallucinations JSON COMMENT '근거 없는 문장 배열 [{sentence, reason}]',
    missing_from_source JSON COMMENT '원문에 있는데 누락된 항목 배열 [{source_quote, reason}]',
    rule_findings JSON COMMENT 'BE 정형 룰 위반 항목 배열 (분량/첨부/block)',
    word_count INT COMMENT '결과물 단어/글자 수 (분량 추적)',
    attempt INT NOT NULL DEFAULT 1 COMMENT '자동 재시도 회차 (1=최초, 2=1회 재시도 후)',
    verified_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_verification_log_target (target_type, target_id, verified_at),
    KEY idx_verification_log_run (workflow_run_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
