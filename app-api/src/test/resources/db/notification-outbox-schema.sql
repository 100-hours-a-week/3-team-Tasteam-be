-- ── user_activity 아웃박스 테이블 (Flyway-only, JPA 엔티티 없음) ─────────────────

CREATE TABLE IF NOT EXISTS user_activity_event (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL,
    event_name VARCHAR(100) NOT NULL,
    event_version VARCHAR(20) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    member_id BIGINT,
    anonymous_id VARCHAR(100),
    session_id VARCHAR(100),
    source VARCHAR(20) NOT NULL,
    request_path VARCHAR(255),
    request_method VARCHAR(10),
    device_id VARCHAR(100),
    platform VARCHAR(30),
    app_version VARCHAR(30),
    locale VARCHAR(20),
    properties JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_user_activity_event_event_id
    ON user_activity_event(event_id);

CREATE TABLE IF NOT EXISTS user_activity_source_outbox (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL,
    event_name VARCHAR(100) NOT NULL,
    event_version VARCHAR(20) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    member_id BIGINT,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INTEGER NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMPTZ,
    last_error VARCHAR(1000),
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_user_activity_source_outbox_event_id
    ON user_activity_source_outbox(event_id);

CREATE INDEX IF NOT EXISTS idx_user_activity_source_outbox_status_retry
    ON user_activity_source_outbox(status, next_retry_at, id);

CREATE TABLE IF NOT EXISTS user_activity_dispatch_outbox (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL,
    dispatch_target VARCHAR(30) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INTEGER NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMPTZ,
    last_error VARCHAR(1000),
    dispatched_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_user_activity_dispatch_outbox_event_sink
    ON user_activity_dispatch_outbox(event_id, dispatch_target);

CREATE INDEX IF NOT EXISTS idx_user_activity_dispatch_outbox_status_retry
    ON user_activity_dispatch_outbox(status, next_retry_at, id);

-- ── notification 아웃박스 테이블 ─────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS notification_outbox (
    id            BIGSERIAL    PRIMARY KEY,
    event_id      VARCHAR(64)  NOT NULL,
    event_type    VARCHAR(64)  NOT NULL,
    recipient_id  BIGINT       NOT NULL,
    payload       JSONB        NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                  CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    retry_count   INTEGER      NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMPTZ,
    last_error    VARCHAR(1000),
    published_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_notification_outbox_event_id UNIQUE (event_id)
);

CREATE INDEX IF NOT EXISTS idx_notification_outbox_poll
    ON notification_outbox (status, next_retry_at, id);

CREATE TABLE IF NOT EXISTS consumed_notification_event (
    consumer_group VARCHAR(64)  NOT NULL,
    event_id       VARCHAR(64)  NOT NULL,
    stream_key     VARCHAR(128) NOT NULL,
    processed_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (consumer_group, event_id)
);

CREATE INDEX IF NOT EXISTS idx_consumed_notification_processed_at
    ON consumed_notification_event (processed_at);
