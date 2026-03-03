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

CREATE INDEX IF NOT EXISTS idx_notification_outbox_recipient
    ON notification_outbox (recipient_id, created_at DESC);
