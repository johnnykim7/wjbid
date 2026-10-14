-- CR-160 PIEE 입찰서류 자동 받기(BIZ-079, BIZ-080, POL-040).
-- 받기 결과는 공고에, 같은 파일을 가리는 내용 해시는 첨부에 둔다. 옛 첨부의 해시는 비워 두고 그 공고를 PIEE 로 받을 때 채운다
ALTER TABLE opportunities ADD COLUMN piee_fetch_status VARCHAR(20) NULL;
ALTER TABLE opportunities ADD COLUMN piee_fetch_status_at DATETIME(6) NULL;
ALTER TABLE opportunities ADD COLUMN piee_file_count INT NULL;
ALTER TABLE opportunities ADD COLUMN piee_fetch_message VARCHAR(500) NULL;

ALTER TABLE opportunity_attachments ADD COLUMN content_sha256 CHAR(64) NULL;
CREATE INDEX idx_opp_attachments_sha256 ON opportunity_attachments (content_sha256);

INSERT INTO system_settings (setting_key, setting_value, description) VALUES
('PIEE_FETCH_ENABLED', 'true', '수집 때 PIEE 입찰서류 받기를 건다(true 또는 false)'),
('PIEE_NOTICE_URL', 'https://piee.eb.mil/sol/xhtml/unauth/search/oppMgmtLink.xhtml?solNo={solNo}', 'PIEE 공개 공고 주소. {solNo} 자리에 공고번호'),
('PIEE_MAX_ZIP_MB', '1024', 'PIEE 입찰서류 압축 무게 상한(MB). 넘으면 받기 실패'),
('PIEE_TIMEOUT_SECONDS', '900', 'PIEE 입찰서류 한 번 받기 시간 상한(초)'),
('PIEE_SAM_WAIT_MINUTES', '10', 'PIEE 받기 전에 그 공고의 SAM 첨부를 기다리는 상한(분)');
