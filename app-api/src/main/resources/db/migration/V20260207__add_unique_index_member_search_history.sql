-- Ensure unique per member/keyword while allowing soft delete.
-- PostgreSQL supports IF NOT EXISTS; adjust if DB engine differs.
CREATE UNIQUE INDEX IF NOT EXISTS idx_member_search_history_member_keyword
    ON member_search_history (member_id, keyword, deleted_at);
