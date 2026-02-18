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

CREATE INDEX IF NOT EXISTS idx_user_activity_event_member_occurred
    ON user_activity_event(member_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_user_activity_event_name_occurred
    ON user_activity_event(event_name, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_user_activity_event_occurred
    ON user_activity_event(occurred_at DESC);

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

CREATE INDEX IF NOT EXISTS idx_user_activity_source_outbox_created
    ON user_activity_source_outbox(created_at, id);

CREATE TABLE IF NOT EXISTS user_activity_dispatch_outbox (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL,
    sink_type VARCHAR(30) NOT NULL,
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
    ON user_activity_dispatch_outbox(event_id, sink_type);

CREATE INDEX IF NOT EXISTS idx_user_activity_dispatch_outbox_status_retry
    ON user_activity_dispatch_outbox(status, next_retry_at, id);

CREATE INDEX IF NOT EXISTS idx_user_activity_dispatch_outbox_sink_status
    ON user_activity_dispatch_outbox(sink_type, status, id);

DO $$
BEGIN
    IF to_regclass('public.member') IS NOT NULL THEN
        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'fk_user_activity_event_member'
        ) THEN
            ALTER TABLE user_activity_event
                ADD CONSTRAINT fk_user_activity_event_member
                FOREIGN KEY (member_id) REFERENCES member(id);
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'fk_user_activity_source_outbox_member'
        ) THEN
            ALTER TABLE user_activity_source_outbox
                ADD CONSTRAINT fk_user_activity_source_outbox_member
                FOREIGN KEY (member_id) REFERENCES member(id);
        END IF;
    END IF;
END $$;

DO $$
BEGIN
    IF to_regclass('public.user_activity_event') IS NOT NULL THEN
        COMMENT ON TABLE user_activity_event IS '사용자 행동 이벤트 원본 저장소';
        COMMENT ON COLUMN user_activity_event.event_id IS '멱등 저장을 위한 이벤트 고유 식별자';
        COMMENT ON COLUMN user_activity_event.properties IS '이벤트 속성 JSON';
    END IF;

    IF to_regclass('public.user_activity_source_outbox') IS NOT NULL THEN
        COMMENT ON TABLE user_activity_source_outbox IS '서버 사실 이벤트 발행 보장을 위한 소스 아웃박스';
        COMMENT ON COLUMN user_activity_source_outbox.status IS 'PENDING/PUBLISHED/FAILED';
    END IF;

    IF to_regclass('public.user_activity_dispatch_outbox') IS NOT NULL THEN
        COMMENT ON TABLE user_activity_dispatch_outbox IS '외부 분석 도구 전송용 디스패치 아웃박스';
        COMMENT ON COLUMN user_activity_dispatch_outbox.status IS 'PENDING/SENT/FAILED';
    END IF;
END $$;
