DO $$
BEGIN
    IF to_regclass('public."group"') IS NOT NULL THEN
        CREATE INDEX IF NOT EXISTS idx_group_name_trgm_active
            ON "group" USING gin (lower(name) gin_trgm_ops)
            WHERE deleted_at IS NULL;

        CREATE INDEX IF NOT EXISTS idx_group_address_trgm_active
            ON "group" USING gin (lower(address) gin_trgm_ops)
            WHERE deleted_at IS NULL;
    END IF;
END $$;
