CREATE TABLE IF NOT EXISTS consumed_notification_event (
    consumer_group VARCHAR(64)  NOT NULL,
    event_id       VARCHAR(64)  NOT NULL,
    stream_key     VARCHAR(128) NOT NULL,
    processed_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (consumer_group, event_id)
);

CREATE INDEX IF NOT EXISTS idx_consumed_notification_processed_at
    ON consumed_notification_event (processed_at);
