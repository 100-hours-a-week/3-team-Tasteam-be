CREATE TABLE notification (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL,
    notification_type VARCHAR(20) NOT NULL,
    title VARCHAR(100) NOT NULL,
    body VARCHAR(500) NOT NULL,
    deep_link VARCHAR(500),
    read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_notification_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE INDEX idx_notification_member_id ON notification(member_id, id DESC);
CREATE INDEX idx_notification_member_unread ON notification(member_id, read_at, id);

CREATE TABLE member_notification_preference (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL,
    channel VARCHAR(10) NOT NULL,
    notification_type VARCHAR(20) NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_preference_member FOREIGN KEY (member_id) REFERENCES member(id),
    CONSTRAINT uq_preference UNIQUE (member_id, channel, notification_type)
);

CREATE TABLE push_notification_target (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL,
    fcm_token VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_push_target_member FOREIGN KEY (member_id) REFERENCES member(id),
    CONSTRAINT uq_fcm_token UNIQUE (fcm_token)
);

CREATE INDEX idx_push_target_member ON push_notification_target(member_id, created_at);
