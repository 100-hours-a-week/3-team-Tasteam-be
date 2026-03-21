-- Migration: add_restaurant_review_vector_fields
-- Task 1: Restaurant / Review 벡터 필드 추가
-- 기존 행은 vector_epoch = 0, vector_synced_at = NULL 유지

ALTER TABLE restaurant
    ADD COLUMN IF NOT EXISTS vector_epoch BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS vector_synced_at TIMESTAMPTZ;

ALTER TABLE review
    ADD COLUMN IF NOT EXISTS vector_synced_at TIMESTAMPTZ;
