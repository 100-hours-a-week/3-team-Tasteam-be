CREATE TABLE IF NOT EXISTS restaurant_recommendation_import_checkpoint (
    id BIGSERIAL PRIMARY KEY,
    pipeline_version VARCHAR(100) NOT NULL,
    batch_dt DATE NOT NULL,
    imported_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_recommendation_import_checkpoint_version_dt
    ON restaurant_recommendation_import_checkpoint (pipeline_version, batch_dt);

