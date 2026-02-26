-- Migration: create_ai_job_table
-- Task 3: AI Job 테이블 생성 (batch_execution FK)
-- job_type: VECTOR_UPLOAD, REVIEW_SENTIMENT, REVIEW_SUMMARY, RESTAURANT_COMPARISON
-- status: PENDING, RUNNING, COMPLETED, FAILED, STALE

CREATE TABLE IF NOT EXISTS ai_job (
    id BIGSERIAL PRIMARY KEY,
    batch_execution_id BIGINT NOT NULL REFERENCES batch_execution(id),
    job_type VARCHAR(50) NOT NULL,
    restaurant_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    base_epoch BIGINT NOT NULL DEFAULT 0,
    attempt_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE ai_job
    ADD CONSTRAINT chk_ai_job_job_type CHECK (job_type IN (
        'VECTOR_UPLOAD', 'REVIEW_SENTIMENT', 'REVIEW_SUMMARY', 'RESTAURANT_COMPARISON'
    )),
    ADD CONSTRAINT chk_ai_job_status CHECK (status IN (
        'PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'STALE'
    ));

CREATE INDEX IF NOT EXISTS idx_ai_job_batch_execution_id
    ON ai_job(batch_execution_id);

CREATE INDEX IF NOT EXISTS idx_ai_job_restaurant_type_status
    ON ai_job(restaurant_id, job_type, status);

CREATE INDEX IF NOT EXISTS idx_ai_job_status_created
    ON ai_job(status, created_at)
    WHERE status = 'PENDING';
