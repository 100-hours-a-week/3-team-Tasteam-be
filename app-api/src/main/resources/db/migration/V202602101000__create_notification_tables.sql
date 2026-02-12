DO $$
BEGIN
    IF to_regclass('public.member') IS NOT NULL THEN
        CREATE TABLE IF NOT EXISTS notification (
            id BIGSERIAL PRIMARY KEY,
            member_id BIGINT NOT NULL,
            notification_type VARCHAR(20) NOT NULL,
            title VARCHAR(100) NOT NULL,
            body VARCHAR(500) NOT NULL,
            deep_link VARCHAR(500),
            read_at TIMESTAMP,
            created_at TIMESTAMP NOT NULL DEFAULT NOW()
        );

        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'fk_notification_member'
        ) THEN
            ALTER TABLE notification
                ADD CONSTRAINT fk_notification_member FOREIGN KEY (member_id) REFERENCES member(id);
        END IF;

        CREATE INDEX IF NOT EXISTS idx_notification_member_id
            ON notification(member_id, id DESC);
        CREATE INDEX IF NOT EXISTS idx_notification_member_unread
            ON notification(member_id, read_at, id);

        CREATE TABLE IF NOT EXISTS member_notification_preference (
            id BIGSERIAL PRIMARY KEY,
            member_id BIGINT NOT NULL,
            channel VARCHAR(10) NOT NULL,
            notification_type VARCHAR(20) NOT NULL,
            is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
            created_at TIMESTAMP NOT NULL DEFAULT NOW(),
            updated_at TIMESTAMP NOT NULL DEFAULT NOW()
        );

        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'fk_preference_member'
        ) THEN
            ALTER TABLE member_notification_preference
                ADD CONSTRAINT fk_preference_member FOREIGN KEY (member_id) REFERENCES member(id);
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'uq_preference'
        ) THEN
            ALTER TABLE member_notification_preference
                ADD CONSTRAINT uq_preference UNIQUE (member_id, channel, notification_type);
        END IF;

        CREATE TABLE IF NOT EXISTS push_notification_target (
            id BIGSERIAL PRIMARY KEY,
            member_id BIGINT NOT NULL,
            fcm_token VARCHAR(255) NOT NULL,
            created_at TIMESTAMP NOT NULL DEFAULT NOW()
        );

        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'fk_push_target_member'
        ) THEN
            ALTER TABLE push_notification_target
                ADD CONSTRAINT fk_push_target_member FOREIGN KEY (member_id) REFERENCES member(id);
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'uq_fcm_token'
        ) THEN
            ALTER TABLE push_notification_target
                ADD CONSTRAINT uq_fcm_token UNIQUE (fcm_token);
        END IF;

        CREATE INDEX IF NOT EXISTS idx_push_target_member
            ON push_notification_target(member_id, created_at);
    END IF;
END $$;
