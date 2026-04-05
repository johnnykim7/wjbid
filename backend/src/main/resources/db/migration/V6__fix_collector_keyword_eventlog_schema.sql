-- V6: Fix collector_runs, keyword_groups, event_logs schema to match entity classes

-- 1. collector_runs: Recreate with correct columns
DROP TABLE IF EXISTS collector_runs;
CREATE TABLE collector_runs (
    id BINARY(16) PRIMARY KEY,
    keyword VARCHAR(255),
    started_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6),
    total_fetched INT NOT NULL DEFAULT 0,
    new_count INT NOT NULL DEFAULT 0,
    updated_count INT NOT NULL DEFAULT 0,
    success BOOLEAN NOT NULL DEFAULT FALSE,
    error_message TEXT,
    triggered_by VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_collector_runs_started_at (started_at DESC),
    INDEX idx_collector_runs_success (success)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. keyword_groups: Recreate with single keyword field
DROP TABLE IF EXISTS keyword_groups;
CREATE TABLE keyword_groups (
    id BINARY(16) PRIMARY KEY,
    keyword VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_keyword_groups_active (active),
    UNIQUE INDEX uk_keyword_groups_keyword (keyword)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. event_logs: Recreate for domain event logging
DROP TABLE IF EXISTS event_logs;
CREATE TABLE event_logs (
    id BINARY(16) PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BINARY(16) NOT NULL,
    actor_id BINARY(16),
    payload JSON,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_event_logs_type (event_type),
    INDEX idx_event_logs_entity (entity_type, entity_id),
    INDEX idx_event_logs_created_at (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
