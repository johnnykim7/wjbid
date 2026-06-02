-- V29: CR-032 원문 본문 저장 컬럼
-- SAM search 응답의 description은 noticedesc URL이라 본문이 화면에 안 보였음.
-- noticedesc로 fetch한 원문 본문(평문)을 여기에 저장해 관리자 상세에서 노출.

ALTER TABLE opportunities
  ADD COLUMN description_body MEDIUMTEXT NULL AFTER description_summary_ko;
