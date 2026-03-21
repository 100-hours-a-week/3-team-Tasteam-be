INSERT INTO chat_room (subgroup_id, created_at, deleted_at)
SELECT s.id, s.created_at, s.deleted_at
FROM subgroup s
LEFT JOIN chat_room cr ON cr.subgroup_id = s.id
WHERE cr.id IS NULL;

INSERT INTO chat_room_member (member_id, chat_room_id, last_read_message_id, created_at, updated_at, deleted_at)
SELECT sm.member_id, cr.id, NULL, sm.created_at, sm.created_at, sm.deleted_at
FROM subgroup_member sm
JOIN chat_room cr ON cr.subgroup_id = sm.subgroup_id
LEFT JOIN chat_room_member crm ON crm.chat_room_id = cr.id AND crm.member_id = sm.member_id
WHERE crm.id IS NULL;
