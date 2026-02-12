CREATE TABLE image_optimization_job (
    id BIGSERIAL PRIMARY KEY,
    image_id BIGINT NOT NULL REFERENCES image(id),
    status VARCHAR(32) NOT NULL,
    original_size BIGINT,
    optimized_size BIGINT,
    original_width INT,
    original_height INT,
    optimized_width INT,
    optimized_height INT,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_image_optimization_job_image UNIQUE (image_id)
);

CREATE INDEX idx_image_optimization_job_status ON image_optimization_job(status);
