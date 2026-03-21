-- V1: Fix member_search_history duplicate records and add unique constraint
--
-- Purpose:
-- 1. Remove duplicate records (keep the most recent one)
-- 2. Create partial unique index to prevent future duplicates
--
-- Author: Claude Code
-- Date: 2026-02-01

DO $$
BEGIN
    IF to_regclass('public.member_search_history') IS NOT NULL THEN
        -- Step 1: Delete duplicate records (keeping only the most recent one per member_id + keyword)
        DELETE FROM member_search_history
        WHERE id IN (
            SELECT id
            FROM (
                SELECT id,
                       ROW_NUMBER() OVER (
                           PARTITION BY member_id, keyword
                           ORDER BY updated_at DESC, id DESC
                       ) AS rn
                FROM member_search_history
                WHERE deleted_at IS NULL
            ) ranked
            WHERE rn > 1
        );

        -- Step 2: Create partial unique index (only for non-deleted records)
        CREATE UNIQUE INDEX IF NOT EXISTS idx_member_search_history_unique
        ON member_search_history(member_id, keyword)
        WHERE deleted_at IS NULL;

        -- Step 3: Add comment for documentation
        COMMENT ON INDEX idx_member_search_history_unique IS
        'Partial unique index to prevent duplicate search history records. Only enforced when deleted_at IS NULL to support soft delete.';
    END IF;
END $$;
