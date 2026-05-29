-- CR-019: 원본 공고 첨부 보강
-- AttachmentDownloadStatus에 MANUAL_FETCH_REQUIRED(21자) 추가 → 기존 VARCHAR(20) 초과.
-- download_status 컬럼을 VARCHAR(30)으로 확장. 기존 데이터(SUCCESS/FAILED/LINK_ONLY)는 영향 없음.

ALTER TABLE opportunity_attachments
    MODIFY COLUMN download_status VARCHAR(30) NOT NULL DEFAULT 'FAILED';
