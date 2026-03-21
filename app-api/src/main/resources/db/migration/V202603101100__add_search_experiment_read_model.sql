CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_food_category_name_lower
    ON food_category (lower(name));

CREATE INDEX IF NOT EXISTS idx_restaurant_food_category_food_category_restaurant
    ON restaurant_food_category (food_category_id, restaurant_id);

DO $$
BEGIN
    IF to_regclass('public.restaurant_search_mv') IS NULL THEN
        EXECUTE $view$
            CREATE MATERIALIZED VIEW public.restaurant_search_mv AS
            SELECT
                r.id AS restaurant_id,
                lower(r.name) AS name_lower,
                lower(r.full_address) AS addr_lower,
                r.location,
                r.updated_at,
                r.deleted_at,
                COALESCE(
                    array_agg(DISTINCT lower(fc.name)) FILTER (WHERE fc.name IS NOT NULL),
                    ARRAY[]::text[]
                ) AS category_names
            FROM restaurant r
            LEFT JOIN restaurant_food_category rfc ON rfc.restaurant_id = r.id
            LEFT JOIN food_category fc ON fc.id = rfc.food_category_id
            GROUP BY r.id, r.name, r.full_address, r.location, r.updated_at, r.deleted_at
        $view$;
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS idx_restaurant_search_mv_restaurant_id
    ON restaurant_search_mv (restaurant_id);

CREATE INDEX IF NOT EXISTS idx_restaurant_search_mv_name_trgm_active
    ON restaurant_search_mv USING gin (name_lower gin_trgm_ops)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_restaurant_search_mv_addr_trgm_active
    ON restaurant_search_mv USING gin (addr_lower gin_trgm_ops)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_restaurant_search_mv_category_names_active
    ON restaurant_search_mv USING gin (category_names)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_restaurant_search_mv_geography_active
    ON restaurant_search_mv USING gist (geography(location))
    WHERE deleted_at IS NULL;
