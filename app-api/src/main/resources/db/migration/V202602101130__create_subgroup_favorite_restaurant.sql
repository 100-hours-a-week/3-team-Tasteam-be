DO $$
BEGIN
    IF to_regclass('public.member') IS NOT NULL
       AND to_regclass('public.subgroup') IS NOT NULL
       AND to_regclass('public.restaurant') IS NOT NULL THEN
        CREATE TABLE IF NOT EXISTS subgroup_favorite_restaurant (
            id BIGSERIAL PRIMARY KEY,
            member_id BIGINT NOT NULL,
            subgroup_id BIGINT NOT NULL,
            restaurant_id BIGINT NOT NULL,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        );

        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'uq_subgroup_favorite_subgroup_restaurant'
        ) THEN
            ALTER TABLE subgroup_favorite_restaurant
                ADD CONSTRAINT uq_subgroup_favorite_subgroup_restaurant UNIQUE (subgroup_id, restaurant_id);
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'uq_subgroup_favorite_restaurant_member'
        ) THEN
            ALTER TABLE subgroup_favorite_restaurant
                ADD CONSTRAINT uq_subgroup_favorite_restaurant_member UNIQUE (restaurant_id, member_id);
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'fk_subgroup_favorite_member'
        ) THEN
            ALTER TABLE subgroup_favorite_restaurant
                ADD CONSTRAINT fk_subgroup_favorite_member FOREIGN KEY (member_id) REFERENCES member (id);
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'fk_subgroup_favorite_subgroup'
        ) THEN
            ALTER TABLE subgroup_favorite_restaurant
                ADD CONSTRAINT fk_subgroup_favorite_subgroup FOREIGN KEY (subgroup_id) REFERENCES subgroup (id);
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'fk_subgroup_favorite_restaurant'
        ) THEN
            ALTER TABLE subgroup_favorite_restaurant
                ADD CONSTRAINT fk_subgroup_favorite_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurant (id);
        END IF;

        CREATE INDEX IF NOT EXISTS idx_subgroup_favorite_subgroup_created_at
            ON subgroup_favorite_restaurant (subgroup_id, created_at DESC);
    END IF;
END $$;
