CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_restaurant_name_trgm
    ON restaurant USING gin (lower(name) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_restaurant_full_address_trgm
    ON restaurant USING gin (lower(full_address) gin_trgm_ops);
