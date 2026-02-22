-- Migration: create_batch_execution_table
-- Task 2: BatchExecution 테이블 생성 (배치 런 메타데이터)
-- batch_type: VECTOR_DAILY, ANALYSIS_DAILY, COMPARISON_WEEKLY
-- status: RUNNING, COMPLETED, FAILED

CREATE TABLE IF NOT EXISTS batch_execution (
    id BIGSERIAL PRIMARY KEY,
    batch_type VARCHAR(50) NOT NULL,
    status VARCHAR(32) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ,
    total_jobs INT NOT NULL DEFAULT 0,
    success_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    stale_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_batch_execution_batch_type_status
    ON batch_execution(batch_type, status);

CREATE INDEX IF NOT EXISTS idx_batch_execution_started_at
    ON batch_execution(started_at DESC);
