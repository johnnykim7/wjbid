-- CR-125: 회사 공통 서류함 — member 종속 공통 서류 보관함
-- 사업자등록증·CAGE 증빙 등을 한 번 보관 후 매 공고 재사용 + 제안서 슬롯 자동채움.
-- ClientDocument(client_documents, BidRequest 종속)와 별개 — 이쪽은 회사 단위 공통 보관.

CREATE TABLE member_documents (
    id BINARY(16) PRIMARY KEY,
    member_id BINARY(16) NOT NULL,
    category VARCHAR(40) NOT NULL,        -- DocumentCategory enum (고정 6종 + OTHER)
    file_name VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    storage_url TEXT NOT NULL,
    display_name VARCHAR(255) NULL,       -- 문서제목(고정=카테고리 한글명, 기타=사용자 입력)
    description TEXT NULL,                 -- 설명 메모(기타 전용)
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_member_documents_member FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE INDEX idx_member_documents_member ON member_documents (member_id);
CREATE INDEX idx_member_documents_member_category ON member_documents (member_id, category);
