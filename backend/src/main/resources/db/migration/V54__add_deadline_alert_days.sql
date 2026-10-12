-- CR-156: 입찰 요청 마감 알림을 보내는 날(POL-009). 코드에 고정돼 있던 7, 3, 1 을 설정으로 옮긴다.

INSERT INTO system_settings (setting_key, setting_value, description)
VALUES ('DEADLINE_ALERT_DAYS', '7,3,1', '입찰 요청 마감 알림을 보내는 날(마감 며칠 전, 쉼표로 나눔). 비어 있으면 보내지 않음');
