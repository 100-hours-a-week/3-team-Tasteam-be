-- Ensure unique per member/keyword while allowing soft delete.
DO $$
BEGIN
    IF to_regclass('public.member_search_history') IS NOT NULL THEN
        CREATE UNIQUE INDEX IF NOT EXISTS idx_member_search_history_member_keyword
            ON member_search_history (member_id, keyword, deleted_at);
    END IF;
END $$;
