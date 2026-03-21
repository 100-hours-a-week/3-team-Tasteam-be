-- restaurant_search_mv에 location_geo(geography) 컬럼 추가
-- 기존에 ST_DWithin/ST_Distance 호출 시마다 발생하던 geography() 런타임 캐스팅 비용을 제거한다.
DROP MATERIALIZED VIEW IF EXISTS restaurant_search_mv;

CREATE MATERIALIZED VIEW public.restaurant_search_mv AS
SELECT
    r.id             AS restaurant_id,
    r.name           AS name,
    r.full_address   AS full_address,
    lower(r.name)    AS name_lower,
    lower(r.full_address) AS addr_lower,
    r.location,
    r.location::geography AS location_geo,
    r.updated_at,
    r.deleted_at,
    COALESCE(
        array_agg(DISTINCT lower(fc.name)) FILTER (WHERE fc.name IS NOT NULL),
        ARRAY[]::text[]
    ) AS category_names,
    setweight(to_tsvector('simple', coalesce(lower(r.name), '')), 'A')
    || setweight(to_tsvector('simple', coalesce(array_to_string(
        COALESCE(
            array_agg(DISTINCT lower(fc.name)) FILTER (WHERE fc.name IS NOT NULL),
            ARRAY[]::text[]
        ), ' '), '')), 'B')
    || setweight(to_tsvector('simple', coalesce(lower(r.full_address), '')), 'C')
    AS search_vector
FROM restaurant r
LEFT JOIN restaurant_food_category rfc ON rfc.restaurant_id = r.id
LEFT JOIN food_category fc ON fc.id = rfc.food_category_id
GROUP BY r.id, r.name, r.full_address, r.location, r.updated_at, r.deleted_at;

CREATE UNIQUE INDEX idx_restaurant_search_mv_restaurant_id
    ON restaurant_search_mv (restaurant_id);

CREATE INDEX idx_restaurant_search_mv_name_trgm_active
    ON restaurant_search_mv USING gin (name_lower gin_trgm_ops)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_restaurant_search_mv_addr_trgm_active
    ON restaurant_search_mv USING gin (addr_lower gin_trgm_ops)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_restaurant_search_mv_category_names_active
    ON restaurant_search_mv USING gin (category_names)
    WHERE deleted_at IS NULL;

-- geography 타입으로 직접 저장된 location_geo에 GIST 인덱스 (캐스팅 불필요)
CREATE INDEX idx_restaurant_search_mv_location_geo_active
    ON restaurant_search_mv USING gist (location_geo)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_restaurant_search_mv_fts_active
    ON restaurant_search_mv USING gin (search_vector)
    WHERE deleted_at IS NULL;

ANALYZE restaurant_search_mv;
