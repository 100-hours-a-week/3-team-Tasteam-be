-- V1__init에서 chat_* 시퀀스를 먼저 만들고,
-- 이후 BIGSERIAL로 chat_* 테이블을 생성하면서 *_seq1 기본 시퀀스가 추가 생성되었다.
-- 애플리케이션은 기존 *_seq를 사용하므로 컬럼 기본값과 시퀀스를 동일하게 맞춘다.
DO $$
DECLARE
    max_chat_room_id BIGINT;
    max_chat_room_member_id BIGINT;
    max_chat_message_id BIGINT;
    max_chat_message_file_id BIGINT;
BEGIN
    SELECT COALESCE(MAX(id), 0) INTO max_chat_room_id FROM chat_room;
    IF max_chat_room_id = 0 THEN
        PERFORM setval('chat_room_id_seq', 1, false);
    ELSE
        PERFORM setval('chat_room_id_seq', max_chat_room_id, true);
    END IF;
    ALTER TABLE chat_room ALTER COLUMN id SET DEFAULT nextval('chat_room_id_seq'::regclass);
    ALTER SEQUENCE chat_room_id_seq OWNED BY chat_room.id;

    SELECT COALESCE(MAX(id), 0) INTO max_chat_room_member_id FROM chat_room_member;
    IF max_chat_room_member_id = 0 THEN
        PERFORM setval('chat_room_member_id_seq', 1, false);
    ELSE
        PERFORM setval('chat_room_member_id_seq', max_chat_room_member_id, true);
    END IF;
    ALTER TABLE chat_room_member ALTER COLUMN id SET DEFAULT nextval('chat_room_member_id_seq'::regclass);
    ALTER SEQUENCE chat_room_member_id_seq OWNED BY chat_room_member.id;

    SELECT COALESCE(MAX(id), 0) INTO max_chat_message_id FROM chat_message;
    IF max_chat_message_id = 0 THEN
        PERFORM setval('chat_message_id_seq', 1, false);
    ELSE
        PERFORM setval('chat_message_id_seq', max_chat_message_id, true);
    END IF;
    ALTER TABLE chat_message ALTER COLUMN id SET DEFAULT nextval('chat_message_id_seq'::regclass);
    ALTER SEQUENCE chat_message_id_seq OWNED BY chat_message.id;

    SELECT COALESCE(MAX(id), 0) INTO max_chat_message_file_id FROM chat_message_file;
    IF max_chat_message_file_id = 0 THEN
        PERFORM setval('chat_message_file_id_seq', 1, false);
    ELSE
        PERFORM setval('chat_message_file_id_seq', max_chat_message_file_id, true);
    END IF;
    ALTER TABLE chat_message_file ALTER COLUMN id SET DEFAULT nextval('chat_message_file_id_seq'::regclass);
    ALTER SEQUENCE chat_message_file_id_seq OWNED BY chat_message_file.id;
END $$;
