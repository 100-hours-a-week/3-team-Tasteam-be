-- Migration: create_restaurant_recommendation_tables
-- 개인화 추천 모델 버전 메타데이터 + 사용자별 추천 결과 저장 테이블 생성

CREATE TABLE IF NOT EXISTS restaurant_recommendation_model (
    id BIGSERIAL PRIMARY KEY,
    version VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    activated_at TIMESTAMPTZ,
    deactivated_at TIMESTAMPTZ,
    CONSTRAINT chk_restaurant_recommendation_model_status CHECK (status IN (
        'LOADING', 'READY', 'ACTIVE', 'INACTIVE', 'FAILED'
    ))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_restaurant_recommendation_model_single_active
    ON restaurant_recommendation_model ((status))
    WHERE status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_restaurant_recommendation_model_status_created
    ON restaurant_recommendation_model (status, created_at DESC);

CREATE TABLE IF NOT EXISTS restaurant_recommendation (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT,
    anonymous_id VARCHAR(100),
    restaurant_id BIGINT NOT NULL,
    score DOUBLE PRECISION NOT NULL,
    rank INT NOT NULL,
    model_id BIGINT NOT NULL,
    context_snapshot JSONB NOT NULL DEFAULT '{}'::jsonb,
    pipeline_version VARCHAR(30) NOT NULL,
    generated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_restaurant_recommendation_rank_positive CHECK (rank > 0),
    CONSTRAINT chk_restaurant_recommendation_expiry CHECK (expires_at > generated_at),
    CONSTRAINT chk_restaurant_recommendation_subject CHECK (
        member_id IS NOT NULL OR anonymous_id IS NOT NULL
    ),

    CONSTRAINT fk_recommendation_member
        FOREIGN KEY (member_id) REFERENCES member(id),

    CONSTRAINT fk_recommendation_restaurant
        FOREIGN KEY (restaurant_id) REFERENCES restaurant(id),

    CONSTRAINT fk_recommendation_model
        FOREIGN KEY (model_id) REFERENCES restaurant_recommendation_model(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_restaurant_recommendation_model_member_rank
    ON restaurant_recommendation (model_id, member_id, rank)
    WHERE member_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_restaurant_recommendation_model_anonymous_rank
    ON restaurant_recommendation (model_id, anonymous_id, rank)
    WHERE anonymous_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_restaurant_recommendation_member_model_rank
    ON restaurant_recommendation (member_id, model_id, rank ASC);

CREATE INDEX IF NOT EXISTS idx_restaurant_recommendation_anonymous_model_rank
    ON restaurant_recommendation (anonymous_id, model_id, rank ASC);

CREATE INDEX IF NOT EXISTS idx_restaurant_recommendation_model_id
    ON restaurant_recommendation (model_id);
