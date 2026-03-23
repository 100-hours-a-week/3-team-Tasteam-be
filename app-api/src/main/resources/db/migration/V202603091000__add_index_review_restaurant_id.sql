CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_review_restaurant_id_active
    ON review (restaurant_id)
    WHERE deleted_at IS NULL;
