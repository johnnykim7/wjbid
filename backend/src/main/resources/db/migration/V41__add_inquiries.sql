-- CR-132: 고객센터 1:1 문의를 서버에 접수한다. 이전에는 화면이 접수 안내만 띄우고 아무 데도 저장하지 않았다.

-- 문의 유형은 보여 주기만 하므로 코드 목록으로 둔다(POL-014). 코드는 다른 화면이 주소로 넘기므로 바꾸지 않는다.
CREATE TABLE inquiry_types (
    code VARCHAR(40) PRIMARY KEY,
    label VARCHAR(100) NOT NULL,
    sort_order INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO inquiry_types (code, label, sort_order) VALUES
    ('SERVICE', '서비스 이용 문의', 1),
    ('CONSULTING', '서비스 상담', 2),
    ('SAM_REGISTRATION', 'SAM.gov 등록 문의', 3),
    ('PROPOSAL', '제안서 자문·검토 문의', 4),
    ('PAYMENT', '결제·환불 문의', 5),
    ('ETC', '기타', 6);

CREATE TABLE inquiries (
    id BINARY(16) PRIMARY KEY,
    type_code VARCHAR(40) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content VARCHAR(5000) NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    member_id BINARY(16) NULL,
    status VARCHAR(20) NOT NULL,
    answer VARCHAR(5000) NULL,
    answered_at DATETIME(6) NULL,
    answered_by BINARY(16) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_inquiries_type FOREIGN KEY (type_code) REFERENCES inquiry_types (code),
    CONSTRAINT fk_inquiries_member FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT fk_inquiries_answered_by FOREIGN KEY (answered_by) REFERENCES members (id)
);

CREATE INDEX idx_inquiries_status_created ON inquiries (status, created_at);
CREATE INDEX idx_inquiries_type ON inquiries (type_code);
CREATE INDEX idx_inquiries_member ON inquiries (member_id);

INSERT INTO system_settings (setting_key, setting_value, description)
VALUES ('INQUIRY_NOTIFY_EMAILS', NULL, '1:1 문의 접수 알림 받는 주소(쉼표로 나눔). 비어 있으면 관리자 계정 전원');
