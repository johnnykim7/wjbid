-- CR-136: 공통 서류함 서류의 만료일(BID-MKT-012)과 만료 메일을 보내는 날(POL-016).

ALTER TABLE member_documents
    ADD COLUMN expires_on DATE NULL;

CREATE INDEX idx_member_documents_expires_on ON member_documents (expires_on);

INSERT INTO system_settings (setting_key, setting_value, description)
VALUES ('DOC_EXPIRY_ALERT_DAYS', '30,7', '서류 만료 메일을 보내는 날(만료 며칠 전, 쉼표로 나눔). 비어 있으면 보내지 않음');
