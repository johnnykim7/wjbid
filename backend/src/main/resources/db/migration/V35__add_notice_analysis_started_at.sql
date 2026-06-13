-- CR-039: 공고문 한글화/분석 시작 시각 — stuck 자동 정리 임계 판정 기준.
-- updatedAt(@LastModifiedDate)은 markAnalyzing 이후 무관한 update에 밀려 부정확하므로 전용 컬럼.
ALTER TABLE notices ADD COLUMN analysis_started_at DATETIME(6) NULL;

-- 이미 ANALYZING 상태로 멈춰있는 기존 건들은 시작 시각을 알 수 없으므로 updated_at으로 보정
-- (다음 정리 스케줄러 사이클에서 임계 초과 시 자동 FAILED 처리 대상이 되도록).
UPDATE notices SET analysis_started_at = updated_at WHERE generation_status = 'ANALYZING';
