-- V7: Fix opportunity_requirement_items to use direct opportunity_id FK
-- Remove intermediate opportunity_requirement_sets table

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Drop the old table and recreate with direct FK
DROP TABLE IF EXISTS requirement_fulfillment_map;
DROP TABLE IF EXISTS opportunity_requirement_items;
DROP TABLE IF EXISTS opportunity_requirement_sets;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE opportunity_requirement_items (
    id BINARY(16) PRIMARY KEY,
    opportunity_id BINARY(16) NOT NULL,
    category VARCHAR(30) NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    requirement_json JSON NOT NULL,
    is_blocker BOOLEAN NOT NULL DEFAULT FALSE,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    confidence DECIMAL(3,2),
    manual_override BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    FOREIGN KEY (opportunity_id) REFERENCES opportunities(id) ON DELETE CASCADE,
    INDEX idx_requirement_items_opportunity (opportunity_id),
    INDEX idx_requirement_items_category (category),
    INDEX idx_requirement_items_blocker (is_blocker)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
