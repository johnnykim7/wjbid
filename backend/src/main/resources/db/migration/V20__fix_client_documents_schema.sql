-- V20: client_documents 스키마를 ClientDocument 엔티티에 맞춤 (CR-024)
-- 엔티티 컬럼: content_type, storage_url. 기존 테이블은 mime_type, file_path.
-- document_type은 엔티티에 없는데 NOT NULL이라 INSERT 실패 → NULL 허용으로 완화.

ALTER TABLE client_documents
  CHANGE COLUMN mime_type content_type VARCHAR(100) NOT NULL DEFAULT 'application/octet-stream';

ALTER TABLE client_documents
  CHANGE COLUMN file_path storage_url TEXT NOT NULL;

ALTER TABLE client_documents
  MODIFY COLUMN document_type VARCHAR(50) NULL;
