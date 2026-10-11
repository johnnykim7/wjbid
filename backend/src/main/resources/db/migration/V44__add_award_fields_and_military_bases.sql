-- CR-136: 낙찰 공지의 업체 이름, 업체 UEI, 금액 숫자를 저장하고(BID-MKT-007), 공고 제목에서 찾을 기지 목록 표를 둔다(BID-MKT-010).
-- 기지 목록과 위치는 사람이 관리자 화면에서 채우므로 행을 넣지 않는다.

ALTER TABLE opportunities
    ADD COLUMN award_amount_usd DECIMAL(15,2) NULL,
    ADD COLUMN awardee_name VARCHAR(255) NULL,
    ADD COLUMN awardee_uei VARCHAR(20) NULL;

-- 이미 모은 낙찰 공지의 금액 숫자는 표시 문자열(예 $60,700.00)에서 읽는다. 숫자로 읽을 수 없는 값은 비워 둔다.
UPDATE opportunities
SET award_amount_usd = CAST(REPLACE(SUBSTRING(award_amount, 2), ',', '') AS DECIMAL(15,2))
WHERE award_amount REGEXP '^[$][0-9][0-9,]*([.][0-9]{1,2})?$';

CREATE TABLE military_bases (
    id BINARY(16) PRIMARY KEY,
    name_ko VARCHAR(100) NOT NULL,
    name_en VARCHAR(100) NOT NULL,
    match_terms VARCHAR(1000) NOT NULL,
    region VARCHAR(100) NULL,
    latitude DECIMAL(9,6) NULL,
    longitude DECIMAL(9,6) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_military_bases_name_en UNIQUE (name_en)
);
