-- Auto-generated cleanup SQL for Tasteam dummy data
-- run_token: 4564823f0055

BEGIN;
SET LOCAL client_min_messages TO WARNING;

CREATE TEMP TABLE tmp_cleanup_member_ids AS
SELECT id FROM member WHERE email LIKE 'perf-user-4564823f0055-%';
INSERT INTO tmp_cleanup_member_ids (id)
SELECT moa.member_id FROM member_oauth_account moa WHERE moa.provider = 'TEST'   AND moa.provider_user_id ~ '^test-user-[0-9]{3}$';

CREATE TEMP TABLE tmp_cleanup_group_ids AS
SELECT id FROM "group" WHERE name LIKE '성능그룹-4564823f0055-%';
INSERT INTO tmp_cleanup_group_ids (id)
SELECT 2002 WHERE EXISTS (SELECT 1 FROM "group" WHERE id = 2002);

CREATE TEMP TABLE tmp_cleanup_restaurant_ids AS
SELECT id FROM restaurant WHERE name LIKE '성능식당-4564823f0055-%';

CREATE TEMP TABLE tmp_cleanup_subgroup_ids AS
SELECT id FROM subgroup WHERE group_id IN (SELECT id FROM tmp_cleanup_group_ids);

CREATE TEMP TABLE tmp_cleanup_chat_room_ids AS
SELECT id FROM chat_room WHERE subgroup_id IN (SELECT id FROM tmp_cleanup_subgroup_ids);

DELETE FROM member_notification_preference WHERE member_id IN (SELECT id FROM tmp_cleanup_member_ids);
DELETE FROM member_search_history WHERE member_id IN (SELECT id FROM tmp_cleanup_member_ids);
DELETE FROM member_favorite_restaurant WHERE member_id IN (SELECT id FROM tmp_cleanup_member_ids);
DELETE FROM subgroup_favorite_restaurant WHERE subgroup_id IN (SELECT id FROM tmp_cleanup_subgroup_ids)    OR member_id IN (SELECT id FROM tmp_cleanup_member_ids);
DELETE FROM notification WHERE event_id LIKE 'perf-4564823f0055-%';
DELETE FROM notification WHERE member_id IN (SELECT id FROM tmp_cleanup_member_ids);

DELETE FROM review_keyword WHERE review_id IN (
  SELECT id FROM review
  WHERE member_id IN (SELECT id FROM tmp_cleanup_member_ids)
     OR restaurant_id IN (SELECT id FROM tmp_cleanup_restaurant_ids)
);
DELETE FROM review WHERE member_id IN (SELECT id FROM tmp_cleanup_member_ids)
   OR restaurant_id IN (SELECT id FROM tmp_cleanup_restaurant_ids);

DELETE FROM chat_message WHERE chat_room_id IN (SELECT id FROM tmp_cleanup_chat_room_ids);
DELETE FROM chat_room_member WHERE chat_room_id IN (SELECT id FROM tmp_cleanup_chat_room_ids)
   OR member_id IN (SELECT id FROM tmp_cleanup_member_ids);
DELETE FROM chat_room WHERE id IN (SELECT id FROM tmp_cleanup_chat_room_ids);

DELETE FROM subgroup_member WHERE subgroup_id IN (SELECT id FROM tmp_cleanup_subgroup_ids)
   OR member_id IN (SELECT id FROM tmp_cleanup_member_ids);
DELETE FROM member_oauth_account WHERE member_id IN (SELECT id FROM tmp_cleanup_member_ids);
DELETE FROM subgroup WHERE id IN (SELECT id FROM tmp_cleanup_subgroup_ids);
DELETE FROM group_member WHERE group_id IN (SELECT id FROM tmp_cleanup_group_ids)
   OR member_id IN (SELECT id FROM tmp_cleanup_member_ids);
DELETE FROM group_auth_code WHERE group_id IN (SELECT id FROM tmp_cleanup_group_ids);
DELETE FROM "group" WHERE id IN (SELECT id FROM tmp_cleanup_group_ids);

DELETE FROM restaurant_review_summary WHERE restaurant_id IN (SELECT id FROM tmp_cleanup_restaurant_ids);
DELETE FROM restaurant_review_sentiment WHERE restaurant_id IN (SELECT id FROM tmp_cleanup_restaurant_ids);
DELETE FROM restaurant_food_category WHERE restaurant_id IN (SELECT id FROM tmp_cleanup_restaurant_ids);
DELETE FROM restaurant_weekly_schedule WHERE restaurant_id IN (SELECT id FROM tmp_cleanup_restaurant_ids);
DELETE FROM restaurant_address WHERE restaurant_id IN (SELECT id FROM tmp_cleanup_restaurant_ids);
DELETE FROM menu WHERE category_id IN (
  SELECT id FROM menu_category WHERE restaurant_id IN (SELECT id FROM tmp_cleanup_restaurant_ids)
);
DELETE FROM menu_category WHERE restaurant_id IN (SELECT id FROM tmp_cleanup_restaurant_ids);
DELETE FROM restaurant WHERE id IN (SELECT id FROM tmp_cleanup_restaurant_ids);

DELETE FROM member WHERE id IN (SELECT id FROM tmp_cleanup_member_ids);

COMMIT;
