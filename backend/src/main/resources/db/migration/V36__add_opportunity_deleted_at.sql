-- CR-042: 원본 공고(Opportunity) 소프트 삭제.
-- 관리자가 목록에서 불필요/중복 공고를 숨길 수 있도록 deleted_at 컬럼 추가(hard delete 아님 — 원본 보존 BIZ-004).
-- NULL = 정상, 값 있음 = 삭제됨(목록·검색·고객 노출에서 제외). 단 existsByNoticeId 재수집 중복판정에는 삭제분도 포함.
ALTER TABLE opportunities ADD COLUMN deleted_at DATETIME(6) NULL;

-- 소프트 삭제 필터(deleted_at IS NULL)가 active 인덱스와 함께 자주 쓰이므로 보조 인덱스.
CREATE INDEX idx_opportunities_deleted_at ON opportunities (deleted_at);
