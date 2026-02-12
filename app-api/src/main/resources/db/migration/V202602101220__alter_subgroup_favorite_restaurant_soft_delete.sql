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

        CREATE UNIQUE INDEX IF NOT EXISTS uq_subgroup_like_active
            ON subgroup_favorite_restaurant (member_id, restaurant_id, subgroup_id)
            WHERE deleted_at IS NULL;
    END IF;
END $$;
