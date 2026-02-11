CREATE TABLE chat_room (
    id BIGSERIAL PRIMARY KEY,
    subgroup_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP,
    CONSTRAINT fk_chat_room_subgroup FOREIGN KEY (subgroup_id) REFERENCES subgroup(id)
);

CREATE TABLE chat_room_member (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL,
    chat_room_id BIGINT NOT NULL,
    last_read_message_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP,
    CONSTRAINT fk_chat_room_member_member FOREIGN KEY (member_id) REFERENCES member(id),
    CONSTRAINT fk_chat_room_member_room FOREIGN KEY (chat_room_id) REFERENCES chat_room(id),
    CONSTRAINT uq_chat_room_member UNIQUE (chat_room_id, member_id)
);

CREATE TABLE chat_message (
    id BIGSERIAL PRIMARY KEY,
    chat_room_id BIGINT NOT NULL,
    member_id BIGINT,
    type VARCHAR(20) NOT NULL,
    content VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP,
    CONSTRAINT fk_chat_message_room FOREIGN KEY (chat_room_id) REFERENCES chat_room(id),
    CONSTRAINT fk_chat_message_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE chat_message_file (
    id BIGSERIAL PRIMARY KEY,
    chat_message_id BIGINT NOT NULL,
    file_type VARCHAR(20) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP,
    CONSTRAINT fk_chat_message_file_message FOREIGN KEY (chat_message_id) REFERENCES chat_message(id)
);

CREATE INDEX idx_chat_message_room_id ON chat_message(chat_room_id, id DESC);
CREATE INDEX idx_chat_room_member_room_member ON chat_room_member(chat_room_id, member_id);
