-- CR-129: 운영 중 바뀌는 값을 코드 대신 DB 에 둔다. 첫 항목은 회원 월 요금(POL-010).
-- 회원 금액은 발주사와 정한 뒤 채우므로 값을 비워 둔다.

CREATE TABLE system_settings (
    setting_key VARCHAR(100) PRIMARY KEY,
    setting_value VARCHAR(500) NULL,
    description VARCHAR(255) NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

INSERT INTO system_settings (setting_key, setting_value, description)
VALUES ('PRICING_MEMBER_MONTHLY_KRW', NULL, '회원 월 요금(원, 부가세 별도). 비어 있으면 요금 화면에 금액 대신 안내 문구');
