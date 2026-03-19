-- ====================================
-- Fix opportunities table schema
-- Add missing created_at, updated_at columns required by BaseEntity JPA Auditing
-- ====================================

ALTER TABLE opportunities
    ADD COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) AFTER last_modified_at,
    ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) AFTER created_at;
