-- Migration: create_ai_analysis_result_tables
-- Task 4: 분석 결과 테이블 3종 (설계 5.1, 5.2, 5.3)
-- 기존 ai_restaurant_review_analysis / ai_restaurant_comparison 는 유지, 새 테이블만 추가

-- 5.1 감정 분석 결과: restaurant_id, vector_epoch 기준 최신 1건
CREATE TABLE IF NOT EXISTS restaurant_review_sentiment (
    id BIGSERIAL PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    vector_epoch BIGINT NOT NULL,
    model_version VARCHAR(50),
    positive_count INT NOT NULL DEFAULT 0,
    negative_count INT NOT NULL DEFAULT 0,
    neutral_count INT NOT NULL DEFAULT 0,
    positive_ratio NUMERIC(5, 4) NOT NULL DEFAULT 0,
    negative_ratio NUMERIC(5, 4) NOT NULL DEFAULT 0,
    neutral_ratio NUMERIC(5, 4) NOT NULL DEFAULT 0,
    analyzed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_restaurant_review_sentiment_restaurant_epoch
    ON restaurant_review_sentiment(restaurant_id, vector_epoch);

CREATE INDEX IF NOT EXISTS idx_restaurant_review_sentiment_restaurant_analyzed
    ON restaurant_review_sentiment(restaurant_id, analyzed_at DESC);

-- 5.2 요약 분석 결과: restaurant_id, vector_epoch 기준 최신 1건
CREATE TABLE IF NOT EXISTS restaurant_review_summary (
    id BIGSERIAL PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    vector_epoch BIGINT NOT NULL,
    model_version VARCHAR(50),
    summary_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    analyzed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_restaurant_review_summary_restaurant_epoch
    ON restaurant_review_summary(restaurant_id, vector_epoch);

CREATE INDEX IF NOT EXISTS idx_restaurant_review_summary_restaurant_analyzed
    ON restaurant_review_summary(restaurant_id, analyzed_at DESC);

-- 5.3 비교 분석 결과: restaurant_id당 최신 1건 (vector_epoch 없음)
CREATE TABLE IF NOT EXISTS restaurant_comparison_analysis (
    id BIGSERIAL PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    model_version VARCHAR(50),
    comparison_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    analyzed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_restaurant_comparison_analysis_restaurant
    ON restaurant_comparison_analysis(restaurant_id);

CREATE INDEX IF NOT EXISTS idx_restaurant_comparison_analysis_restaurant_analyzed
    ON restaurant_comparison_analysis(restaurant_id, analyzed_at DESC);
