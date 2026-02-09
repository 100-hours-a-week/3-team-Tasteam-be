CREATE INDEX IF NOT EXISTS idx_restaurant_location_gist
    ON restaurant USING gist (location);

CREATE INDEX IF NOT EXISTS idx_restaurant_geography_gist
    ON restaurant USING gist (geography(location));
