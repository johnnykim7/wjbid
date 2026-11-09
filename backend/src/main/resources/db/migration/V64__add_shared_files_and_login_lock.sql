-- CR-196 자료실과 로그인 잠금 (BIZ-111 ~ 116, POL-042)
CREATE TABLE shared_files (
    id BINARY(16) NOT NULL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    size_bytes BIGINT NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    key_salt BINARY(16) NOT NULL,
    password_check BINARY(32) NOT NULL,
    uploaded_by_id BINARY(16) NOT NULL,
    uploaded_by_email VARCHAR(255) NOT NULL,
    failed_attempts INT NOT NULL DEFAULT 0,
    locked_until DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    INDEX idx_shared_files_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 파일을 지워도 기록이 남아야 하므로 shared_file_id 에 외래 키를 두지 않는다
CREATE TABLE shared_file_logs (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    shared_file_id BINARY(16) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    action VARCHAR(20) NOT NULL,
    member_id BINARY(16) NOT NULL,
    member_email VARCHAR(255) NOT NULL,
    client_ip VARCHAR(45) NULL,
    created_at DATETIME(6) NOT NULL,
    INDEX idx_shared_file_logs_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE members
    ADD COLUMN failed_login_count INT NOT NULL DEFAULT 0,
    ADD COLUMN login_locked_until DATETIME(6) NULL;

INSERT INTO system_settings (setting_key, setting_value, description) VALUES
('SHARED_FILE_MAX_UPLOAD_MB', '50', '자료실 한 파일 크기 상한(MB)'),
('SHARED_FILE_BLOCKED_EXTENSIONS', 'exe,bat,cmd,com,scr,msi,ps1,vbs,js,jar,sh,html,htm,xhtml,svg,php,jsp', '자료실에서 받지 않는 확장자(쉼표로 나눔)'),
('SHARED_FILE_MAX_ATTEMPTS', '5', '자료실 파일 비밀번호를 틀릴 수 있는 횟수'),
('SHARED_FILE_LOCK_MINUTES', '30', '자료실 파일 잠금 시간(분)'),
('LOGIN_MAX_ATTEMPTS', '5', '로그인을 틀릴 수 있는 횟수'),
('LOGIN_LOCK_MINUTES', '15', '로그인 잠금 시간(분)');
