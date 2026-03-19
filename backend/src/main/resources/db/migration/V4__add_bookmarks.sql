-- ====================================
-- Add bookmarks table
-- ====================================

CREATE TABLE bookmarks (
    id BINARY(16) PRIMARY KEY,
    member_id BINARY(16) NOT NULL,
    opportunity_id BINARY(16) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE,
    FOREIGN KEY (opportunity_id) REFERENCES opportunities(id) ON DELETE CASCADE,
    UNIQUE KEY uk_bookmark (member_id, opportunity_id),
    INDEX idx_bookmarks_member (member_id),
    INDEX idx_bookmarks_opportunity (opportunity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
