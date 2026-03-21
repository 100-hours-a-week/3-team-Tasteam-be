ALTER TABLE chat_message_file
    ADD COLUMN IF NOT EXISTS file_uuid VARCHAR(36);

ALTER TABLE chat_message_file
    ALTER COLUMN file_url DROP NOT NULL;
