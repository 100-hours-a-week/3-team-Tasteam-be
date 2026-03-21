ALTER TABLE notification ADD COLUMN IF NOT EXISTS event_id VARCHAR(64);
CREATE UNIQUE INDEX IF NOT EXISTS uq_notification_event_id ON notification (event_id) WHERE event_id IS NOT NULL;
