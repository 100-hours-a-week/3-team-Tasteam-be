-- Add composite index on (member_id, updated_at DESC) to speed up recent-search queries.
-- The query pattern is: WHERE member_id = ? AND deleted_at IS NULL ORDER BY updated_at DESC LIMIT N
CREATE INDEX IF NOT EXISTS idx_member_search_history_member_updated_at
    ON member_search_history (member_id, updated_at DESC)
    WHERE deleted_at IS NULL;
