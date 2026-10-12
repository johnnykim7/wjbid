-- CR-157 회원 기간(BIZ-072). 결제 연동 전까지 관리자가 넣고 지운다. 넣은 줄은 고치지 않는다
CREATE TABLE member_memberships (
    id BINARY(16) PRIMARY KEY,
    member_id BINARY(16) NOT NULL,
    starts_on DATE NOT NULL,
    ends_on DATE NOT NULL,
    memo VARCHAR(500) NULL,
    created_by BINARY(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_member_memberships_member FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE CASCADE,
    CONSTRAINT chk_member_memberships_period CHECK (ends_on >= starts_on)
);

CREATE INDEX idx_member_memberships_member ON member_memberships (member_id, ends_on);
