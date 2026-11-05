-- CR-188 테스트 공고 표시. 고객 목록과 신규 공고 알림에서 뺀다 (BIZ-099)
-- CR-120 으로 만든 공고는 원본 JSON 의 source 가 MANUAL_TEST 다
ALTER TABLE opportunities ADD COLUMN test_notice BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE opportunities SET test_notice = TRUE WHERE JSON_VALUE(raw_json, '$.source') = 'MANUAL_TEST';
