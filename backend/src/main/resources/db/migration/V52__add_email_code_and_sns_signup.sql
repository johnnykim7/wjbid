-- CR-146 회원가입 이메일 인증번호와 SNS 가입

-- SNS 로만 가입한 회원은 비밀번호가 없다 (BIZ-064)
ALTER TABLE members MODIFY COLUMN password_hash VARCHAR(255) NULL;

-- 인증번호나 SNS 가 이메일을 확인한 시각. 이 CR 전에 가입한 회원은 비어 있다 (BIZ-062)
ALTER TABLE members ADD COLUMN email_verified_at DATETIME(6) NULL AFTER email;

-- SNS 계정 하나는 회원 하나에만, 회원 하나는 SNS 마다 계정 하나만 (BIZ-063)
CREATE TABLE member_social_accounts (
    id BINARY(16) PRIMARY KEY,
    member_id BINARY(16) NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(100) NOT NULL,
    provider_email VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL,
    last_login_at DATETIME(6) NULL,
    CONSTRAINT uk_member_social_provider_user UNIQUE (provider, provider_user_id),
    CONSTRAINT uk_member_social_member_provider UNIQUE (member_id, provider),
    CONSTRAINT fk_member_social_member FOREIGN KEY (member_id) REFERENCES members (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 이메일 인증번호. 번호는 해시로만 둔다
CREATE TABLE email_verification_codes (
    id BINARY(16) PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    code_hash VARCHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    verified_at DATETIME(6) NULL,
    consumed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    INDEX idx_email_codes_email_created (email, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- SNS 에서 돌아온 뒤 화면에 넘기는 한 번 쓰는 표. 표 값은 해시로만 둔다
CREATE TABLE sns_tickets (
    id BINARY(16) PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL,
    kind VARCHAR(10) NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(100) NOT NULL,
    provider_email VARCHAR(255) NULL,
    email_verified_by_provider BOOLEAN NOT NULL DEFAULT FALSE,
    member_id BINARY(16) NULL,
    expires_at DATETIME(6) NOT NULL,
    used_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_sns_tickets_token UNIQUE (token_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO system_settings (setting_key, setting_value, description)
VALUES ('EMAIL_CODE_TTL_MINUTES', '10', '이메일 인증번호 유효 시간(분). 비었거나 숫자가 아니면 10 (POL-037)'),
       ('EMAIL_CODE_RESEND_SECONDS', '60', '인증번호 다시 받기 간격(초). 비었거나 숫자가 아니면 60 (POL-037)'),
       ('EMAIL_CODE_MAX_ATTEMPTS', '5', '인증번호를 틀릴 수 있는 횟수. 비었거나 숫자가 아니면 5 (POL-037)'),
       ('EMAIL_CODE_DAILY_LIMIT', '10', '이메일 하나에 하루 보낼 수 있는 인증번호 수. 비었거나 숫자가 아니면 10 (POL-037)'),
       ('EMAIL_CODE_VERIFIED_MINUTES', '30', '인증번호 확인 뒤 가입을 마쳐야 하는 시간(분). 비었거나 숫자가 아니면 30 (POL-037)'),
       ('SNS_SIGNUP_TICKET_MINUTES', '30', 'SNS 에서 돌아와 가입 화면을 쓸 수 있는 시간(분). 비었거나 숫자가 아니면 30 (POL-038)'),
       ('SNS_LOGIN_TICKET_SECONDS', '60', 'SNS 에서 돌아와 로그인을 마칠 시간(초). 비었거나 숫자가 아니면 60 (POL-038)');
