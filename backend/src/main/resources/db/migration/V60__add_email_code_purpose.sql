-- CR-186 인증번호 쓰임(가입, 비밀번호 재설정). 확인과 쓰기는 같은 쓰임의 번호만 본다 (BIZ-097)
-- 이 CR 전에 보낸 번호는 전부 가입 번호다
ALTER TABLE email_verification_codes ADD COLUMN purpose VARCHAR(20) NOT NULL DEFAULT 'SIGNUP' AFTER email;

CREATE INDEX idx_email_codes_email_purpose_created ON email_verification_codes (email, purpose, created_at);
