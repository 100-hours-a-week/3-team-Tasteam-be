CREATE TABLE IF NOT EXISTS message_queue_trace_log (
    id BIGSERIAL PRIMARY KEY,
    message_id VARCHAR(100) NOT NULL,
    topic VARCHAR(100) NOT NULL,
    provider VARCHAR(30) NOT NULL,
    message_key VARCHAR(200),
    consumer_group VARCHAR(100),
    stage VARCHAR(30) NOT NULL,
    processing_millis BIGINT,
    error_message VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_mq_trace_message_id
    ON message_queue_trace_log(message_id, id DESC);

CREATE INDEX IF NOT EXISTS idx_mq_trace_topic_stage
    ON message_queue_trace_log(topic, stage, id DESC);
