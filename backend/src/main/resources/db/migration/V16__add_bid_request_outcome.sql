-- CR-018: 입찰 결과 상태(AWARDED/NOT_AWARDED) 신설.
-- 결과 상태는 BidRequest.state(VARCHAR enum)로 표현되므로 별도 상태 컬럼은 불필요.
-- 결과 확정 시각만 기록한다.

ALTER TABLE bid_requests
    ADD COLUMN outcome_decided_at DATETIME NULL AFTER submitted_at;
