-- restaurant_search_mv에 name, full_address 컬럼 추가
-- fetchRestaurantMap 후속 쿼리 제거를 위해 원본 값을 MV에 포함
DROP MATERIALIZED VIEW IF EXISTS restaurant_search_mv;

CREATE MATERIALIZED VIEW public.restaurant_search_mv AS
SELECT
    r.id             AS restaurant_id,
    r.name           AS name,
    r.full_address   AS full_address,
    lower(r.name)    AS name_lower,
    lower(r.full_address) AS addr_lower,
    r.location,
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

CREATE INDEX idx_restaurant_search_mv_geography_active
    ON restaurant_search_mv USING gist (geography(location))
    WHERE deleted_at IS NULL;

CREATE INDEX idx_restaurant_search_mv_fts_active
    ON restaurant_search_mv USING gin (search_vector)
    WHERE deleted_at IS NULL;
