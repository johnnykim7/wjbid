-- CR-137: 적격성 진단 답. 자격 항목은 공고문 분석 결과에 있고 여기에는 회원의 답만 둔다(BIZ-032, BIZ-033).
CREATE TABLE eligibility_answers (
    id BINARY(16) PRIMARY KEY,
    member_id BINARY(16) NOT NULL,
    notice_id BINARY(16) NOT NULL,
    item_no INT NOT NULL,
    item_title VARCHAR(500) NOT NULL,
    answer VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_eligibility_answers UNIQUE (member_id, notice_id, item_no),
    CONSTRAINT fk_eligibility_answers_member FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT fk_eligibility_answers_notice FOREIGN KEY (notice_id) REFERENCES notices (id) ON DELETE CASCADE
);

CREATE INDEX idx_eligibility_answers_notice ON eligibility_answers (notice_id);
