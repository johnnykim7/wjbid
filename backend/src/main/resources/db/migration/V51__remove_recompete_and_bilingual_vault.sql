-- CR-147 우진비드 긴급 점검 화면 정리

-- 재발주 예측(BID-MKT-028~033)을 걷어내 회원 계약 표와 그 설정 두 줄을 지운다. 운영 member_contracts 는 0행이었다
DROP TABLE member_contracts;

DELETE FROM system_settings
WHERE setting_key IN ('CONTRACT_END_ALERT_DAYS', 'RECOMPETE_ONE_TIME_NAICS_PREFIXES');

-- 서류함 국문·영문 칸(BIZ-065). 국문·영문 칸만 값이 있다
ALTER TABLE member_documents ADD COLUMN language VARCHAR(2) NULL AFTER category;

-- 칸에서 뺀 분류(CAGE 증빙, SAM 등록증, 과거 수행실적)로 올린 서류는 기타로 옮긴다. 제목은 원래 칸 이름이 남는다
UPDATE member_documents
SET category = 'OTHER'
WHERE category IN ('CAGE', 'SAM_REGISTRATION', 'PAST_PERFORMANCE');

-- 회사소개서 칸이 국문·영문 칸(지명원·회사소개 PPT)이 되어 이미 올린 파일은 국문으로 둔다
UPDATE member_documents
SET language = 'KO'
WHERE category = 'COMPANY_PROFILE';
