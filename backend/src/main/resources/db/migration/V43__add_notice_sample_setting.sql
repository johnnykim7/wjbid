-- CR-135: 공고문 예시 화면에 보일 공개 공고문(POL-015). 사용자가 10/11 에 고른 부산지역 쓰레기 수거 공고문(W90VN926QA046).
-- 바꿀 때는 이 행의 값만 고친다.

INSERT INTO system_settings (setting_key, setting_value, description)
VALUES ('NOTICE_SAMPLE_ID', '12bede2f-10a4-443d-aab0-b80f7160e48c',
        '공고문 예시 화면에 보일 공고문 id(UUID). 비거나 그 공고문이 숨김이면 화면에 준비 안내');
