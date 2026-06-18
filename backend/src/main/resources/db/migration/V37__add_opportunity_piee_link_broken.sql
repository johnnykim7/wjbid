-- CR-043: PIEE 입찰서류 링크 오류 표식.
-- 일부 공고는 PIEE에 solNo 직링크로 안 열리고 메인(index.xhtml)으로 302 리다이렉트된다.
-- 우리 코드/사용자 PC 문제가 아니라 해당 공고의 PIEE 게시 상태 문제 → 관리자가 수동으로 "오류" 표시.
-- true = 이 공고의 PIEE 링크는 오류로 표시됨(목록·상세에서 경고 노출, 헛클릭 방지).
ALTER TABLE opportunities
    ADD COLUMN piee_link_broken BOOLEAN NOT NULL DEFAULT FALSE;
