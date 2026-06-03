-- V31: CR-034 재정의 — piee_available 컬럼 폐기
-- 본문(noticedesc) 텍스트에서 piee.eb.mil을 contains로 감지하던 방식을 폐기한다.
-- (PIEE 단서는 search 응답에 없고 본문 fetch에 의존해 신뢰도가 낮음.)
-- 대신 PIEE 안내 링크는 첨부 섹션에서 solicitation_number 기반으로 화면에서 항상 노출한다.
-- V30에서 추가한 컬럼을 되돌린다.

ALTER TABLE opportunities
  DROP COLUMN piee_available;
