-- CR-194 제출 검사와 가격 표 (BIZ-105 ~ 108)
ALTER TABLE verification_log ADD COLUMN input_bundle_json TEXT NULL;

CREATE TABLE submission_check_notes (
    id BINARY(16) NOT NULL PRIMARY KEY,
    bid_request_id BINARY(16) NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_key VARCHAR(100) NOT NULL,
    disposition VARCHAR(30) NOT NULL,
    reason TEXT NULL,
    source_ref VARCHAR(500) NULL,
    notice_analyzed_at DATETIME(6) NOT NULL,
    decided_by VARCHAR(255) NOT NULL,
    decided_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_submission_check_notes_target (bid_request_id, target_type, target_key),
    CONSTRAINT fk_submission_check_notes_bid_request FOREIGN KEY (bid_request_id) REFERENCES bid_requests (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE bid_price_inputs (
    id BINARY(16) NOT NULL PRIMARY KEY,
    bid_request_id BINARY(16) NOT NULL,
    clin VARCHAR(50) NOT NULL,
    period VARCHAR(200) NOT NULL,
    unit_price DECIMAL(20,6) NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_bid_price_inputs_line (bid_request_id, clin, period),
    CONSTRAINT fk_bid_price_inputs_bid_request FOREIGN KEY (bid_request_id) REFERENCES bid_requests (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
