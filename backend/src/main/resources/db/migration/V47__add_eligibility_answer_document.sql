-- CR-140: "있음" 으로 답한 자격에 붙인 증빙 서류(BIZ-042, BIZ-043). 서류함에서 서류를 지우면 붙인 것만 풀린다.
ALTER TABLE eligibility_answers
    ADD COLUMN document_id BINARY(16) NULL;

ALTER TABLE eligibility_answers
    ADD CONSTRAINT fk_eligibility_answers_document FOREIGN KEY (document_id) REFERENCES member_documents (id) ON DELETE SET NULL;
