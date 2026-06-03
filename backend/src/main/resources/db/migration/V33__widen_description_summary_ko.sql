-- V33: CR-035 본문 한글 번역 전체 보존 — description_summary_ko를 varchar(500) → TEXT로 확대.
-- 이 컬럼은 원래 "요약"용 varchar(500)이었으나 현재 본문 전체 번역을 담는다.
-- 500자 컷으로 긴 본문 번역이 잘리던 문제 해소(코드의 500자 substring도 함께 제거).

ALTER TABLE opportunities
  MODIFY COLUMN description_summary_ko TEXT NULL;
