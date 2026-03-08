DO $$
BEGIN
    IF to_regclass('public.subgroup_favorite_restaurant') IS NOT NULL THEN
        ALTER TABLE subgroup_favorite_restaurant
            ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

        ALTER TABLE subgroup_favorite_restaurant
            DROP CONSTRAINT IF EXISTS uq_subgroup_favorite_subgroup_restaurant;

        ALTER TABLE subgroup_favorite_restaurant
            DROP CONSTRAINT IF EXISTS uq_subgroup_favorite_restaurant_member;

        DROP INDEX IF EXISTS uq_subgroup_like_active;

        WITH ranked AS (
            SELECT id,
                   ROW_NUMBER() OVER (
                       PARTITION BY member_id, restaurant_id, subgroup_id
                       ORDER BY created_at DESC, id DESC
                   ) AS rn
            FROM subgroup_favorite_restaurant
            WHERE deleted_at IS NULL
        )
        UPDATE subgroup_favorite_restaurant
        SET deleted_at = NOW()
        WHERE id IN (SELECT id FROM ranked WHERE rn > 1);

        CREATE UNIQUE INDEX IF NOT EXISTS uq_subgroup_like_active
            ON subgroup_favorite_restaurant (member_id, restaurant_id, subgroup_id)
            WHERE deleted_at IS NULL;
    END IF;
END $$;
