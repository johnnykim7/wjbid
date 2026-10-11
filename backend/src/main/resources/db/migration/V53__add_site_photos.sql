-- CR-153 고객 화면 사진 자동 교체(관리자 사진 묶음)

-- 사진 자리(BID-SITE-001). 고객 포털 코드는 칸마다 자리 키만 안다
CREATE TABLE site_photo_slots (
    slot_key VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    location VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL
);

INSERT INTO site_photo_slots (slot_key, name, location, sort_order) VALUES
('HOME_INTRO', '홈 첫걸음 상자', '홈 오른쪽 무료 회원 상자 머리(로그인 안 한 손님, 공고 0건일 때 포함)', 1),
('HOME_EMPTY', '홈 공고 0건 상자', '새 공고가 없을 때 왼쪽 큰 상자 오른쪽 칸', 2),
('HOME_PROMO_REGISTRATION', '홈 홍보 칸 등록 대행', '홈 요금 아래 왼쪽 칸', 3),
('HOME_PROMO_SITE_VISIT', '홈 홍보 칸 현장 지원', '홈 요금 아래 오른쪽 칸', 4),
('LOGIN', '로그인', '로그인 화면 오른쪽 상자 머리', 5),
('REGISTER', '회원가입', '회원가입, SNS 가입 화면 오른쪽 상자 머리', 6),
('SERVICE_DESK', '서비스 안내 상담', 'SAM 등록 대행, SAM 갱신, 서류 영문 번역, 카카오톡 알림 준비 안내의 머리 사진', 7),
('SERVICE_ONLINE', '서비스 안내 온라인 등록', 'PIEE 등록 대행, 대금 청구, 유효기간 알림, 제출 전 검사, 제안서 검토 준비 안내의 머리 사진', 8),
('SERVICE_MEETING', '서비스 안내 회의', '현장 설명회, 팀 구성 찾기, 낙찰 업체 준비 안내의 머리 사진', 9),
('SERVICE_SEMINAR', '서비스 안내 세미나', '기지별 지도 준비 안내의 머리 사진', 10);

-- 자리마다 올린 사진. 고객 화면과 함께 배포된 사진은 bundled_path, 관리자가 올린 사진은 storage_url 만 있다
CREATE TABLE site_photos (
    id BINARY(16) PRIMARY KEY,
    slot_key VARCHAR(50) NOT NULL,
    bundled_path VARCHAR(255) NULL,
    storage_url VARCHAR(500) NULL,
    file_name VARCHAR(255) NOT NULL,
    width INT NOT NULL,
    height INT NOT NULL,
    original_width INT NULL,
    original_height INT NULL,
    size_bytes BIGINT NOT NULL,
    visible BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_site_photos_slot FOREIGN KEY (slot_key) REFERENCES site_photo_slots (slot_key),
    CONSTRAINT ck_site_photos_source CHECK ((bundled_path IS NULL) <> (storage_url IS NULL))
);

CREATE INDEX idx_site_photos_slot ON site_photos (slot_key, created_at);

-- 첫 배포 뒤 고객 화면이 지금과 같도록 지금 사진 8장을 자리마다 넣는다. 같은 사진을 쓰던 자리는 행을 따로 둔다
INSERT INTO site_photos (id, slot_key, bundled_path, file_name, width, height, size_bytes, visible, created_at, updated_at) VALUES
(UNHEX(REPLACE(UUID(), '-', '')), 'HOME_INTRO', '/img/mil-airlift-dawn.jpg', 'mil-airlift-dawn.jpg', 1600, 1067, 160009, TRUE, NOW(6), NOW(6)),
(UNHEX(REPLACE(UUID(), '-', '')), 'HOME_EMPTY', '/img/mil-carrier-busan.jpg', 'mil-carrier-busan.jpg', 1600, 1067, 293788, TRUE, NOW(6), NOW(6)),
(UNHEX(REPLACE(UUID(), '-', '')), 'HOME_PROMO_REGISTRATION', '/img/mil-f16-taxi.jpg', 'mil-f16-taxi.jpg', 1600, 1067, 107102, TRUE, NOW(6), NOW(6)),
(UNHEX(REPLACE(UUID(), '-', '')), 'HOME_PROMO_SITE_VISIT', '/img/mil-destroyer-busan.jpg', 'mil-destroyer-busan.jpg', 1600, 1067, 267194, TRUE, NOW(6), NOW(6)),
(UNHEX(REPLACE(UUID(), '-', '')), 'LOGIN', '/img/biz-handshake.jpg', 'biz-handshake.jpg', 1600, 1067, 122375, TRUE, NOW(6), NOW(6)),
(UNHEX(REPLACE(UUID(), '-', '')), 'REGISTER', '/img/biz-laptop.jpg', 'biz-laptop.jpg', 1600, 1067, 117173, TRUE, NOW(6), NOW(6)),
(UNHEX(REPLACE(UUID(), '-', '')), 'SERVICE_DESK', '/img/biz-handshake.jpg', 'biz-handshake.jpg', 1600, 1067, 122375, TRUE, NOW(6), NOW(6)),
(UNHEX(REPLACE(UUID(), '-', '')), 'SERVICE_ONLINE', '/img/biz-laptop.jpg', 'biz-laptop.jpg', 1600, 1067, 117173, TRUE, NOW(6), NOW(6)),
(UNHEX(REPLACE(UUID(), '-', '')), 'SERVICE_MEETING', '/img/biz-meeting.jpg', 'biz-meeting.jpg', 1600, 1067, 120307, TRUE, NOW(6), NOW(6)),
(UNHEX(REPLACE(UUID(), '-', '')), 'SERVICE_SEMINAR', '/img/biz-seminar.jpg', 'biz-seminar.jpg', 1600, 1067, 209784, TRUE, NOW(6), NOW(6));

-- 올리기 규칙(POL-039). 비었거나 읽을 수 없으면 코드의 처음 값을 쓴다
INSERT INTO system_settings (setting_key, setting_value, description) VALUES
('SITE_PHOTO_ALLOWED_TYPES', 'image/jpeg,image/png', '고객 화면 사진으로 받는 형식(쉼표로 나눔)'),
('SITE_PHOTO_MAX_UPLOAD_MB', '20', '고객 화면 사진 한 장 올리는 무게 상한(MB)'),
('SITE_PHOTO_MAX_MEGAPIXELS', '50', '고객 화면 사진 원본 화소 수 상한(백만 화소)'),
('SITE_PHOTO_MIN_WIDTH', '1000', '고객 화면 사진 가로 최소. 이보다 좁으면 받지 않는다'),
('SITE_PHOTO_SAVE_WIDTH', '1600', '고객 화면 사진 저장 가로. 이보다 넓으면 줄여 저장'),
('SITE_PHOTO_SAVE_MAX_KB', '400', '고객 화면 사진 저장 무게 목표(KB)');
