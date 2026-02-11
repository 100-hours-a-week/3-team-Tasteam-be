ALTER TABLE push_notification_target
    ADD COLUMN IF NOT EXISTS device_id VARCHAR(64);

ALTER TABLE push_notification_target
    ADD CONSTRAINT uq_push_target_member_device UNIQUE (member_id, device_id);

CREATE INDEX IF NOT EXISTS idx_push_target_member_device
    ON push_notification_target (member_id, device_id);
