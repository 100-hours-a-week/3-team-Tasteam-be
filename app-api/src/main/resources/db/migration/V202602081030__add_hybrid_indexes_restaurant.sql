-- restaurant 검색/조회 성능 개선을 위한 인덱스 및 확장 설정
-- 포함: pg_trgm 확장, 부분/일반 trgm 인덱스, 위치 gist 인덱스
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_restaurant_active_id
    ON restaurant (id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_restaurant_name_trgm_active
    ON restaurant USING gin (lower(name) gin_trgm_ops)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_restaurant_full_address_trgm_active
    ON restaurant USING gin (lower(full_address) gin_trgm_ops)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_restaurant_location_gist
    ON restaurant USING gist (location);

CREATE INDEX IF NOT EXISTS idx_restaurant_geography_gist
    ON restaurant USING gist (geography(location));

CREATE INDEX IF NOT EXISTS idx_restaurant_name_trgm
    ON restaurant USING gin (lower(name) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_restaurant_full_address_trgm
    ON restaurant USING gin (lower(full_address) gin_trgm_ops);
