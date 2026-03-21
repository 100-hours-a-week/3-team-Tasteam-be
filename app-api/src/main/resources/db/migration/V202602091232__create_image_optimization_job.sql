DO $$
BEGIN
    IF to_regclass('public.image') IS NOT NULL THEN
        CREATE TABLE IF NOT EXISTS image_optimization_job (
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
            processed_at TIMESTAMP WITH TIME ZONE
        );

        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'uq_image_optimization_job_image'
        ) THEN
            ALTER TABLE image_optimization_job
                ADD CONSTRAINT uq_image_optimization_job_image UNIQUE (image_id);
        END IF;

        CREATE INDEX IF NOT EXISTS idx_image_optimization_job_status
            ON image_optimization_job(status);
    END IF;
END $$;
