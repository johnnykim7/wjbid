-- CR-124: 회원 개편 — SAM.gov 식별자(UEI/CAGE) + 가입 동의이력
-- UI 전면 개편(Design System 도입)에 동반. 미군·연방 조달 대행이라 법인 회원 + SAM.gov 식별자 필요.

-- 1) SAM.gov 식별자 (가입엔 미입력, 가입 후 프로필에서 입력)
--    UEI = Unique Entity ID(12자), CAGE = Commercial and Government Entity 코드(5자). 여유 두고 설정.
ALTER TABLE members ADD COLUMN uei VARCHAR(20) NULL;
ALTER TABLE members ADD COLUMN cage_code VARCHAR(10) NULL;

-- 2) 가입 동의 이력 (법적 증거력 — 시점·항목·약관버전 기록)
--    이용약관(필수)·개인정보처리방침(필수)·마케팅 수신(선택). 재동의 시 행 추가(이력 보존).
CREATE TABLE member_consents (
    id BINARY(16) PRIMARY KEY,
    member_id BINARY(16) NOT NULL,
    terms_agreed BOOLEAN NOT NULL DEFAULT FALSE,
    privacy_agreed BOOLEAN NOT NULL DEFAULT FALSE,
    marketing_agreed BOOLEAN NOT NULL DEFAULT FALSE,
    terms_version VARCHAR(20) NULL,
    agreed_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_member_consents_member FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE INDEX idx_member_consents_member ON member_consents (member_id);
