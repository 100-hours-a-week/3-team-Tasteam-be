ALTER TABLE chat_message_file
    ADD COLUMN IF NOT EXISTS domain_image_id BIGINT;

ALTER TABLE chat_message_file
    ADD CONSTRAINT fk_chat_message_file_domain_image
    FOREIGN KEY (domain_image_id) REFERENCES domain_image(id);
