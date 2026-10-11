-- CR-141 제출 전 검사
-- 1) 입찰 건 서류 쪽수 칸(BIZ-048). PDF 를 올릴 때 센다. PDF 가 아니거나 세지 못하면 비운다.
-- 2) 입찰 건 서류와 요구 항목 연결 표를 T3-1 과 엔티티에 맞춘다. 마이그레이션에는 두 표의 created_at, updated_at 과
--    연결 표의 attachment_reference 가 없어 마이그레이션만으로 만든 DB 에서는 저장이 칸 없음으로 실패한다.
--    운영에는 created_at, updated_at 이 마이그레이션 밖에서 이미 들어가 있어 IF NOT EXISTS 로 양쪽을 같게 맞춘다.
--    운영 값을 건드리지 않으려고 기존 행은 채우지 않는다. 칸이 없던 DB 의 기존 행은 이 마이그레이션 시각이 된다.
-- 3) 쪽수 조건을 숫자로 읽을 때 쓰는 쪽 단위 낱말 설정(POL-028).

ALTER TABLE client_documents
  ADD COLUMN IF NOT EXISTS page_count INT NULL,
  ADD COLUMN IF NOT EXISTS created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  ADD COLUMN IF NOT EXISTS updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);

ALTER TABLE requirement_fulfillment_maps
  ADD COLUMN IF NOT EXISTS attachment_reference TEXT NULL,
  ADD COLUMN IF NOT EXISTS created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  ADD COLUMN IF NOT EXISTS updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);

INSERT INTO system_settings (setting_key, setting_value, description)
VALUES ('PRE_CHECK_PAGE_UNITS', 'page,pages,p,pp,페이지,쪽,매',
        '쪽수 조건을 숫자로 읽을 때 쓰는 쪽 단위 낱말(쉼표로 나눔, 대소문자 무시). 비어 있으면 쪽수 조건은 모두 사람 확인 (POL-028)');
