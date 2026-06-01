-- V21: 원본 공고(opportunities)에 한글화 결과 컬럼 추가 (CR-022)
-- title_ko: LLM 번역 결과 (실패 시 NULL → 영문 fallback)
-- type_ko: SAM 표준 type 한글 라벨 (코드 매핑)
-- translated_at: 마지막 번역 성공 시각 (재번역 시 갱신)

ALTER TABLE opportunities
  ADD COLUMN title_ko VARCHAR(1000) NULL AFTER title,
  ADD COLUMN type_ko VARCHAR(100) NULL AFTER type,
  ADD COLUMN translated_at DATETIME NULL AFTER last_modified_at;
