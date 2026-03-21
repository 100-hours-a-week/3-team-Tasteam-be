CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_group_member_group_id_active
    ON group_member (group_id)
    WHERE deleted_at IS NULL;
