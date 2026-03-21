CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_group_member_member_id_active
    ON group_member (member_id, id DESC)
    WHERE deleted_at IS NULL;
