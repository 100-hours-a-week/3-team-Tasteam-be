CREATE INDEX IF NOT EXISTS idx_restaurant_active_id
    ON restaurant (id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_restaurant_name_trgm_active
    ON restaurant USING gin (lower(name) gin_trgm_ops)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_restaurant_full_address_trgm_active
    ON restaurant USING gin (lower(full_address) gin_trgm_ops)
    WHERE deleted_at IS NULL;
