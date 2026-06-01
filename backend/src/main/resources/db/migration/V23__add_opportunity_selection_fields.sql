-- V23: CR-022 2차 — 원본공고 선별용 정보 필드
-- 관리자가 "이 공고 우리 일감인가" 1분 판단할 수 있는 핵심값만 노출.
-- 모든 컬럼 NULL 허용 (빈값이면 화면 행 자체를 숨김)

ALTER TABLE opportunities
  ADD COLUMN description_summary_ko VARCHAR(500) NULL AFTER translated_at,
  ADD COLUMN award_amount VARCHAR(50) NULL AFTER description_summary_ko,
  ADD COLUMN place_of_performance_short VARCHAR(200) NULL AFTER award_amount,
  ADD COLUMN set_aside_ko VARCHAR(100) NULL AFTER place_of_performance_short,
  ADD COLUMN naics_label_ko VARCHAR(200) NULL AFTER set_aside_ko;
