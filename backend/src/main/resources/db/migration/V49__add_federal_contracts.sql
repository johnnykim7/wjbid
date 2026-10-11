-- CR-142: USAspending 에서 받은 한국 내 국방부 계약(BID-MKT-025), 받기 회차(BID-MKT-027),
-- 회원이 끝날짜 알림을 받으려고 등록한 계약(BID-MKT-031). 계약 구분은 조회할 때 내므로 저장하지 않는다(BIZ-050).

CREATE TABLE federal_contracts (
    id BINARY(16) PRIMARY KEY,
    award_key VARCHAR(200) NOT NULL,
    piid VARCHAR(100) NOT NULL,
    parent_piid VARCHAR(100) NULL,
    award_type VARCHAR(30) NOT NULL,
    recipient_name VARCHAR(300) NULL,
    recipient_uei VARCHAR(20) NULL,
    start_date DATE NULL,
    end_date DATE NULL,
    potential_end_date DATE NULL,
    solicitation_number VARCHAR(100) NULL,
    detail_fetched_at DATETIME(6) NULL,
    amount_usd DECIMAL(15, 2) NULL,
    naics_code VARCHAR(10) NULL,
    naics_name VARCHAR(300) NULL,
    psc_code VARCHAR(10) NULL,
    description TEXT NULL,
    awarding_sub_agency VARCHAR(200) NULL,
    source_modified_date DATE NULL,
    raw_json JSON NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_federal_contracts_award_key UNIQUE (award_key)
);

CREATE INDEX idx_federal_contracts_piid ON federal_contracts (piid);
CREATE INDEX idx_federal_contracts_end ON federal_contracts (award_type, potential_end_date);
CREATE INDEX idx_federal_contracts_naics ON federal_contracts (naics_code);

CREATE TABLE federal_contract_sync_runs (
    id BINARY(16) PRIMARY KEY,
    mode VARCHAR(10) NOT NULL,
    status VARCHAR(10) NOT NULL,
    modified_since DATE NULL,
    started_at DATETIME(6) NOT NULL,
    finished_at DATETIME(6) NULL,
    search_calls INT NOT NULL DEFAULT 0,
    detail_calls INT NOT NULL DEFAULT 0,
    inserted_count INT NOT NULL DEFAULT 0,
    updated_count INT NOT NULL DEFAULT 0,
    error_message TEXT NULL,
    triggered_by VARCHAR(20) NOT NULL
);

CREATE INDEX idx_federal_sync_runs_started ON federal_contract_sync_runs (started_at);

CREATE TABLE member_contracts (
    id BINARY(16) PRIMARY KEY,
    member_id BINARY(16) NOT NULL,
    piid VARCHAR(100) NOT NULL,
    federal_contract_id BINARY(16) NULL,
    source VARCHAR(10) NOT NULL,
    description VARCHAR(500) NULL,
    end_date DATE NOT NULL,
    potential_end_date DATE NULL,
    dates_changed_on DATE NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_member_contracts UNIQUE (member_id, piid),
    CONSTRAINT fk_member_contracts_member FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT fk_member_contracts_federal FOREIGN KEY (federal_contract_id) REFERENCES federal_contracts (id) ON DELETE SET NULL
);

CREATE INDEX idx_member_contracts_end ON member_contracts (end_date);
CREATE INDEX idx_member_contracts_potential_end ON member_contracts (potential_end_date);

INSERT INTO system_settings (setting_key, setting_value, description) VALUES
    ('AWARD_RANGE_MIN_COUNT', '5', '낙찰가 범위를 보이는 최소 계약 수. 이보다 적으면 범위 대신 건수만'),
    ('CONTRACT_END_ALERT_DAYS', '90,30', '계약 끝 메일을 보내는 날(끝날짜 며칠 전, 쉼표로 나눔). 비어 있으면 보내지 않음'),
    ('FEDERAL_DATA_LAG_DAYS', '90', '미 연방 계약 자료가 늦게 올라오는 날 수. 화면 안내 문구에만 쓴다'),
    ('RECOMPETE_ONE_TIME_NAICS_PREFIXES', '23', '한 번짜리 공사일 수 있음 꼬리표를 붙이는 업종 코드 앞자리(쉼표로 나눔). 비어 있으면 꼬리표 없음');
