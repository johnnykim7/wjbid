-- CR-170 입찰 분석과 Bid/No-Bid 결정(BID-DOC-006, BID-DOC-007, BIZ-090~094, POL-041).
-- 입찰 건 하나에 회차 여러 줄이고 지금 분석은 started_at 이 가장 늦은 회차다.
-- 배포 전부터 분석 단계에 있던 건의 회차는 만들지 않는다. 관리자가 다시 분석하거나 결정만 기록한다
CREATE TABLE bid_analyses (
    id BINARY(16) NOT NULL,
    bid_request_id BINARY(16) NOT NULL,
    status VARCHAR(20) NOT NULL,
    workflow_run_id VARCHAR(100) NULL,
    started_at DATETIME(6) NOT NULL,
    finished_at DATETIME(6) NULL,
    failure_reason VARCHAR(500) NULL,
    report_json LONGTEXT NULL,
    capture_plan_json LONGTEXT NULL,
    decision_json LONGTEXT NULL,
    questions_json LONGTEXT NULL,
    pwin_percent DECIMAL(4,1) NULL,
    ai_verdict VARCHAR(20) NULL,
    verdict_reason VARCHAR(1000) NULL,
    scores_json TEXT NULL,
    strengths_json TEXT NULL,
    weaknesses_json TEXT NULL,
    issue_ranking_json TEXT NULL,
    admin_decision VARCHAR(20) NULL,
    admin_note VARCHAR(2000) NULL,
    decided_by BINARY(16) NULL,
    decided_at DATETIME(6) NULL,
    started_by BINARY(16) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_bid_analyses_bid_request FOREIGN KEY (bid_request_id) REFERENCES bid_requests (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_bid_analyses_bid_request ON bid_analyses (bid_request_id, started_at);

INSERT INTO system_settings (setting_key, setting_value, description)
VALUES ('BID_PWIN_WEIGHTS',
        'CUSTOMER_RELATION:15,SOLUTION_FIT:20,COMPETITION:15,PRICE:15,PAST_PERFORMANCE:15,PERSONNEL:10,ENVIRONMENT:10',
        'Pwin 팩터 가중치(팩터:가중치, 쉼표로 나눔). 일곱 팩터가 다 있지 않거나 합이 100 이 아니면 초기값으로 계산 (POL-041)'),
       ('BID_PWIN_THRESHOLDS', '70,50,30',
        'Pwin 판정 문턱(Strong Go, 조건부 Go, Weak 순). 내림차순이 아니면 초기값으로 계산 (POL-041)');
