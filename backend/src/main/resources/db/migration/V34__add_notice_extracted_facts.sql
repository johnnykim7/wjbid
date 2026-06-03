-- CR-038: 공고문 분석 WF 재설계 — 추출 사실의 근거(sourceQuote/sourceFile/page/confidence) 영속화.
--
-- extracted_facts_json : WF v2 의 extract_facts STEP 이 작업장(grep/read)에서 추출한 각 사실에
--                        근거를 부착한 JSON 배열. 원소 = {key,value,sourceFile,sourceQuote,page,confidence}.
--                        별도 테이블이 아니라 Notice 컬럼 — 교정 채팅(CR-033)이 Notice 를 전체치환
--                        저장하므로 facts 가 Notice 에 속해야 치환 시 함께 보존됨(별도 테이블이면 동기화 누락).
--
-- NULL 허용. 구버전 WF(v1)나 facts 미생성 시 NULL.

ALTER TABLE notices
    ADD COLUMN extracted_facts_json LONGTEXT NULL
        COMMENT '공고문 분석 추출 사실 + 근거(sourceQuote/sourceFile/page/confidence) JSON 배열' AFTER price_items_json;
