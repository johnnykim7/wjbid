-- V30: CR-034 PIEE 제출 지정 공고 표식
-- 본문(noticedesc)에 piee.eb.mil이 있는 공고는 입찰서류 정본이 PIEE에 있어
-- 화면에 PIEE 링크(oppMgmtLink?solNo=)를 노출한다. SAM 자체완결 공고는 false.

ALTER TABLE opportunities
  ADD COLUMN piee_available TINYINT(1) NOT NULL DEFAULT 0 AFTER description_body;
