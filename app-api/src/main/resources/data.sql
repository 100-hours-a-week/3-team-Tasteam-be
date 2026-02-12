-- Local seed data for development (loaded when spring.sql.init.mode=always)

-- Members (SEQUENCE ids must be explicit for SQL inserts)
INSERT INTO member (
  id, email, nickname, status, role, profile_image_url,
  last_login_at, agreed_terms_at, agreed_privacy_at, deleted_at,
  created_at, updated_at
) VALUES
  (1001, 'local.user1@tasteam.dev', '로컬유저1', 'ACTIVE', 'USER', NULL,
   NULL, NULL, NULL, NULL, now(), now()),
  (1002, 'local.user2@tasteam.dev', '로컬유저2', 'ACTIVE', 'USER', NULL,
   NULL, NULL, NULL, NULL, now(), now());

-- Group (reserved word)
INSERT INTO "group" (
  id, name, type, logo_image_url, address, detail_address, location,
  join_type, email_domain, status, deleted_at, created_at, updated_at
) VALUES
  (2001, '테이스팀 공식 그룹', 'OFFICIAL', NULL, '서울특별시 마포구', '합정동 123-45',
   ST_GeomFromText('POINT(126.9147 37.5485)', 4326),
   'EMAIL', 'tasteam.dev', 'ACTIVE', NULL, now(), now());

INSERT INTO group_member (id, group_id, member_id, deleted_at, created_at) VALUES
  (3001, 2001, 1001, NULL, now()),
  (3002, 2001, 1002, NULL, now());

-- Subgroup (reserved word)
INSERT INTO "subgroup" (
  id, group_id, name, description, profile_image_url,
  join_type, join_password, status, member_count, deleted_at,
  created_at, updated_at
) VALUES
  (4001, 2001, '테이스팀 백엔드', '백엔드 스터디 서브그룹', NULL,
   'OPEN', NULL, 'ACTIVE', 2, NULL, now(), now());

INSERT INTO subgroup_member (id, subgroup_id, member_id, deleted_at, created_at) VALUES
  (5001, 4001, 1001, NULL, now()),
  (5002, 4001, 1002, NULL, now());

-- Restaurants
INSERT INTO restaurant (
  id, name, full_address, location, deleted_at, created_at, updated_at
) VALUES
  (6001, '로컬 맛집', '서울특별시 마포구 합정동 123-45',
   ST_GeomFromText('POINT(126.9147 37.5485)', 4326),
   NULL, now(), now());

INSERT INTO restaurant_address (
  id, restaurant_id, sido, sigungu, eupmyeondong, postal_code,
  created_at, updated_at
) VALUES
  (6101, 6001, '서울특별시', '마포구', '합정동', '04000', now(), now());

INSERT INTO food_category (id, name) VALUES
  (6201, '한식');

INSERT INTO restaurant_food_category (id, restaurant_id, food_category_id) VALUES
  (6301, 6001, 6201);

-- Images (DomainImage 기반 샘플)
INSERT INTO image (
  id, file_name, file_size, file_type, storage_key, file_uuid, status, purpose, deleted_at, created_at, updated_at
) VALUES
  (8001, 'restaurant-6001.jpg', 102400, 'image/jpeg', 'seed/restaurants/6001.jpg',
   '11111111-1111-1111-1111-111111111111', 'ACTIVE', 'RESTAURANT_IMAGE', NULL, now(), now()),
  (8002, 'review-7001.jpg', 204800, 'image/jpeg', 'seed/reviews/7001.jpg',
   '22222222-2222-2222-2222-222222222222', 'ACTIVE', 'REVIEW_IMAGE', NULL, now(), now()),
  (8003, 'restaurant-6002.jpg', 102400, 'image/jpeg', 'seed/restaurants/6002.jpg',
   '33333333-3333-3333-3333-333333333333', 'ACTIVE', 'RESTAURANT_IMAGE', NULL, now(), now());

INSERT INTO domain_image (
  id, domain_type, domain_id, image_id, sort_order, created_at
) VALUES
  (8101, 'RESTAURANT', 6001, 8001, 0, now()),
  (8102, 'REVIEW', 7001, 8002, 0, now()),
  (8103, 'RESTAURANT', 6002, 8003, 0, now());

-- Reviews
INSERT INTO review (
  id, restaurant_id, member_id, group_id, subgroup_id,
  content, is_recommended, deleted_at, created_at, updated_at
) VALUES
  (7001, 6001, 1001, 2001, 4001,
   '로컬 환경 테스트용 리뷰입니다.', true, NULL, now(), now());

INSERT INTO keyword (id, type, name) VALUES
  (7101, 'VISIT_PURPOSE', '점심'),
  (7102, 'POSITIVE_ASPECT', '가성비');

INSERT INTO review_keyword (id, review_id, keyword_id) VALUES
  (7201, 7001, 7101),
  (7202, 7001, 7102);

-- Member search history
INSERT INTO member_search_history (
  id, member_id, keyword, count, deleted_at, created_at, updated_at
) VALUES
  (7401, 1001, '합정 맛집', 1, NULL, now(), now());

-- ===== Extended local seed data (additional domains) =====

-- Additional members
INSERT INTO member (
  id, email, nickname, status, role, profile_image_url,
  last_login_at, agreed_terms_at, agreed_privacy_at, deleted_at,
  created_at, updated_at
) VALUES
  (1003, 'local.admin@tasteam.dev', '로컬관리자', 'ACTIVE', 'ADMIN', NULL,
   NULL, NULL, NULL, NULL, now(), now());

-- Additional group and subgroup
INSERT INTO "group" (
  id, name, type, logo_image_url, address, detail_address, location,
  join_type, email_domain, status, deleted_at, created_at, updated_at
) VALUES
  (2002, '테이스팀 스터디 그룹', 'UNOFFICIAL', NULL, '서울특별시 강남구', '역삼동 222-11',
   ST_GeomFromText('POINT(127.0295 37.4981)', 4326),
   'PASSWORD', NULL, 'ACTIVE', NULL, now(), now());

INSERT INTO group_member (id, group_id, member_id, deleted_at, created_at) VALUES
  (3003, 2002, 1001, NULL, now()),
  (3004, 2002, 1003, NULL, now());

INSERT INTO "subgroup" (
  id, group_id, name, description, profile_image_url,
  join_type, join_password, status, member_count, deleted_at,
  created_at, updated_at
) VALUES
  (4002, 2002, '테이스팀 프론트엔드', '프론트엔드 스터디', NULL,
   'PASSWORD', 'local-pass-1234', 'ACTIVE', 2, NULL, now(), now());

INSERT INTO subgroup_member (id, subgroup_id, member_id, deleted_at, created_at) VALUES
  (5003, 4002, 1001, NULL, now()),
  (5004, 4002, 1003, NULL, now());

-- Additional restaurants and categories
INSERT INTO restaurant (
  id, name, full_address, location, deleted_at, created_at, updated_at
) VALUES
  (6002, '로컬 카페', '서울특별시 강남구 역삼동 222-11',
   ST_GeomFromText('POINT(127.0295 37.4981)', 4326),
   NULL, now(), now());

INSERT INTO restaurant_address (
  id, restaurant_id, sido, sigungu, eupmyeondong, postal_code,
  created_at, updated_at
) VALUES
  (6102, 6002, '서울특별시', '강남구', '역삼동', '06200', now(), now());

INSERT INTO food_category (id, name) VALUES
  (6202, '일식'),
  (6203, '카페'),
  (6204, '중식');

INSERT INTO restaurant_food_category (id, restaurant_id, food_category_id) VALUES
  (6302, 6002, 6203);

-- Additional reviews
INSERT INTO review (
  id, restaurant_id, member_id, group_id, subgroup_id,
  content, is_recommended, deleted_at, created_at, updated_at
) VALUES
  (7002, 6002, 1002, 2002, 4002,
   '커피가 깔끔하고 좌석이 넉넉합니다.', true, NULL, now(), now());

INSERT INTO keyword (id, type, name) VALUES
  (7103, 'COMPANION_TYPE', '혼밥'),
  (7104, 'WAITING_EXPERIENCE', '대기없음');

INSERT INTO review_keyword (id, review_id, keyword_id) VALUES
  (7203, 7002, 7103),
  (7204, 7002, 7104);

-- Member OAuth account
INSERT INTO member_oauth_account (
  id, member_id, provider, provider_user_id, provider_user_email, created_at
) VALUES
  (8001, 1001, 'google', 'local-google-1001', 'local.user1@tasteam.dev', now());

-- Refresh token
INSERT INTO refresh_token (
  id, member_id, token_hash, token_family_id, expires_at, rotated_at, revoked_at, created_at
) VALUES
  (8101, 1001, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
   'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb',
   now() + interval '30 days', NULL, NULL, now());

-- Image + Domain image (new file domain)
INSERT INTO image (
  id, file_name, file_size, file_type, storage_key, file_uuid,
  status, purpose, deleted_at, created_at, updated_at
) VALUES
  (9001, 'review-7002.jpg', 123456, 'image/jpeg', 'uploads/review/review-7002.jpg',
   '44444444-4444-4444-4444-444444444444', 'ACTIVE', 'REVIEW_IMAGE', NULL, now(), now()),
  (9002, 'restaurant-6002.jpg', 234567, 'image/jpeg', 'uploads/restaurant/restaurant-6002.jpg',
   '55555555-5555-5555-5555-555555555555', 'ACTIVE', 'RESTAURANT_IMAGE', NULL, now(), now());

INSERT INTO domain_image (
  id, domain_type, domain_id, image_id, sort_order, created_at
) VALUES
  (9101, 'REVIEW', 7002, 9001, 0, now()),
  (9102, 'RESTAURANT', 6002, 9002, 0, now());

-- AI restaurant analysis data
INSERT INTO ai_restaurant_comparison (
  id, restaurant_id, category_lift, comparison_display,
  total_candidates, validated_count, analyzed_at, status, created_at, updated_at
) VALUES
  (9501, 6001, '{"service": 0.1200, "price": 0.0800, "food": 0.1000}'::jsonb,
   '["점심에 방문하기 좋은 합정 로컬 맛집입니다."]'::jsonb, 20, 15, now(), 'COMPLETED', now(), now()),
  (9502, 6002, '{"service": 0.1000, "price": 0.0500, "food": 0.1100}'::jsonb,
   '["조용한 분위기의 카페로 작업하기에 좋습니다."]'::jsonb, 18, 12, now(), 'COMPLETED', now(), now());

INSERT INTO ai_restaurant_review_analysis (
  id, restaurant_id, overall_summary, category_summaries,
  positive_ratio, negative_ratio, analyzed_at, status, created_at, updated_at
) VALUES
  (9601, 6001, '가성비와 위치가 좋은 편입니다.',
   '{"service": "응대가 빠른 편입니다.", "price": "가격 만족도가 높습니다.", "food": "식사 품질이 안정적입니다."}'::jsonb,
   0.8500, 0.1500, now(), 'COMPLETED', now(), now()),
  (9602, 6002, '커피 품질과 좌석이 좋은 편입니다.',
   '{"service": "직원 응대가 친절합니다.", "price": "가격이 합리적이라는 평가가 많습니다.", "food": "커피 맛 만족도가 높습니다."}'::jsonb,
   0.9000, 0.1000, now(), 'COMPLETED', now(), now());

INSERT INTO ai_restaurant_recommendation (
  id, restaurant_id, reason, cache_key, expires_at, created_at
) VALUES
  (9701, 6001, '점심 추천도가 높아 그룹 모임에 적합합니다.', 'seed:member:1001:query:lunch', now() + interval '1 day', now()),
  (9702, 6002, '카페 이용 고객 만족도가 높습니다.', 'seed:member:1002:query:cafe', now() + interval '1 day', now());

-- Group auth code
INSERT INTO group_auth_code (
  id, group_id, code, email, verified_at, expires_at, created_at
) VALUES
  (9801, 2002, 'LOCAL-1234', 'local.user2@tasteam.dev', NULL, now() + interval '15 minutes', now());

-- Additional search history
INSERT INTO member_search_history (
  id, member_id, keyword, count, deleted_at, created_at, updated_at
) VALUES
  (7402, 1002, '강남 카페', 1, NULL, now(), now());

-- ===== Vertical expansion: core datasets ~10 each =====

-- Members (10)
INSERT INTO member (
  id, email, nickname, status, role, profile_image_url,
  last_login_at, agreed_terms_at, agreed_privacy_at, deleted_at,
  created_at, updated_at
) VALUES
  (1101, 'local.user3@tasteam.dev', '로컬유저3', 'ACTIVE', 'USER', NULL, NULL, NULL, NULL, NULL, now(), now()),
  (1102, 'local.user4@tasteam.dev', '로컬유저4', 'ACTIVE', 'USER', NULL, NULL, NULL, NULL, NULL, now(), now()),
  (1103, 'local.user5@tasteam.dev', '로컬유저5', 'ACTIVE', 'USER', NULL, NULL, NULL, NULL, NULL, now(), now()),
  (1104, 'local.user6@tasteam.dev', '로컬유저6', 'ACTIVE', 'USER', NULL, NULL, NULL, NULL, NULL, now(), now()),
  (1105, 'local.user7@tasteam.dev', '로컬유저7', 'ACTIVE', 'USER', NULL, NULL, NULL, NULL, NULL, now(), now()),
  (1106, 'local.user8@tasteam.dev', '로컬유저8', 'ACTIVE', 'USER', NULL, NULL, NULL, NULL, NULL, now(), now()),
  (1107, 'local.user9@tasteam.dev', '로컬유저9', 'ACTIVE', 'USER', NULL, NULL, NULL, NULL, NULL, now(), now()),
  (1108, 'local.user10@tasteam.dev', '로컬유저10', 'ACTIVE', 'USER', NULL, NULL, NULL, NULL, NULL, now(), now()),
  (1109, 'local.user11@tasteam.dev', '로컬유저11', 'ACTIVE', 'USER', NULL, NULL, NULL, NULL, NULL, now(), now()),
  (1110, 'local.user12@tasteam.dev', '로컬유저12', 'ACTIVE', 'USER', NULL, NULL, NULL, NULL, NULL, now(), now());

-- Groups (10)
INSERT INTO "group" (
  id, name, type, logo_image_url, address, detail_address, location,
  join_type, email_domain, status, deleted_at, created_at, updated_at
) VALUES
  (2101, '테이스팀 그룹 1', 'OFFICIAL', NULL, '서울특별시 종로구', '세종대로 1',
   ST_GeomFromText('POINT(126.9780 37.5665)', 4326),
   'EMAIL', 'group1.tasteam.dev', 'ACTIVE', NULL, now(), now()),
  (2102, '테이스팀 그룹 2', 'UNOFFICIAL', NULL, '서울특별시 중구', '명동 2',
   ST_GeomFromText('POINT(126.9860 37.5635)', 4326),
   'PASSWORD', NULL, 'ACTIVE', NULL, now(), now()),
  (2103, '테이스팀 그룹 3', 'OFFICIAL', NULL, '서울특별시 용산구', '이태원 3',
   ST_GeomFromText('POINT(126.9940 37.5340)', 4326),
   'EMAIL', 'group3.tasteam.dev', 'ACTIVE', NULL, now(), now()),
  (2104, '테이스팀 그룹 4', 'UNOFFICIAL', NULL, '서울특별시 성동구', '왕십리 4',
   ST_GeomFromText('POINT(127.0370 37.5610)', 4326),
   'PASSWORD', NULL, 'ACTIVE', NULL, now(), now()),
  (2105, '테이스팀 그룹 5', 'OFFICIAL', NULL, '서울특별시 광진구', '건대입구 5',
   ST_GeomFromText('POINT(127.0700 37.5400)', 4326),
   'EMAIL', 'group5.tasteam.dev', 'ACTIVE', NULL, now(), now()),
  (2106, '테이스팀 그룹 6', 'UNOFFICIAL', NULL, '서울특별시 마포구', '홍대입구 6',
   ST_GeomFromText('POINT(126.9240 37.5570)', 4326),
   'PASSWORD', NULL, 'ACTIVE', NULL, now(), now()),
  (2107, '테이스팀 그룹 7', 'OFFICIAL', NULL, '서울특별시 영등포구', '여의도 7',
   ST_GeomFromText('POINT(126.9237 37.5219)', 4326),
   'EMAIL', 'group7.tasteam.dev', 'ACTIVE', NULL, now(), now()),
  (2108, '테이스팀 그룹 8', 'UNOFFICIAL', NULL, '서울특별시 서초구', '서초동 8',
   ST_GeomFromText('POINT(127.0145 37.4836)', 4326),
   'PASSWORD', NULL, 'ACTIVE', NULL, now(), now()),
  (2109, '테이스팀 그룹 9', 'OFFICIAL', NULL, '서울특별시 강서구', '마곡 9',
   ST_GeomFromText('POINT(126.8260 37.5660)', 4326),
   'EMAIL', 'group9.tasteam.dev', 'ACTIVE', NULL, now(), now()),
  (2110, '테이스팀 그룹 10', 'UNOFFICIAL', NULL, '서울특별시 송파구', '잠실 10',
   ST_GeomFromText('POINT(127.1000 37.5130)', 4326),
   'PASSWORD', NULL, 'ACTIVE', NULL, now(), now());

INSERT INTO group_member (id, group_id, member_id, deleted_at, created_at) VALUES
  (3101, 2101, 1101, NULL, now()), (3102, 2101, 1102, NULL, now()),
  (3103, 2102, 1102, NULL, now()), (3104, 2102, 1103, NULL, now()),
  (3105, 2103, 1103, NULL, now()), (3106, 2103, 1104, NULL, now()),
  (3107, 2104, 1104, NULL, now()), (3108, 2104, 1105, NULL, now()),
  (3109, 2105, 1105, NULL, now()), (3110, 2105, 1106, NULL, now()),
  (3111, 2106, 1106, NULL, now()), (3112, 2106, 1107, NULL, now()),
  (3113, 2107, 1107, NULL, now()), (3114, 2107, 1108, NULL, now()),
  (3115, 2108, 1108, NULL, now()), (3116, 2108, 1109, NULL, now()),
  (3117, 2109, 1109, NULL, now()), (3118, 2109, 1110, NULL, now()),
  (3119, 2110, 1110, NULL, now()), (3120, 2110, 1101, NULL, now());

-- Subgroups (10)
INSERT INTO "subgroup" (
  id, group_id, name, description, profile_image_url,
  join_type, join_password, status, member_count, deleted_at,
  created_at, updated_at
) VALUES
  (4101, 2101, '그룹1 서브', '그룹1 서브 설명', NULL, 'OPEN', NULL, 'ACTIVE', 2, NULL, now(), now()),
  (4102, 2102, '그룹2 서브', '그룹2 서브 설명', NULL, 'PASSWORD', 'pass-2102', 'ACTIVE', 2, NULL, now(), now()),
  (4103, 2103, '그룹3 서브', '그룹3 서브 설명', NULL, 'OPEN', NULL, 'ACTIVE', 2, NULL, now(), now()),
  (4104, 2104, '그룹4 서브', '그룹4 서브 설명', NULL, 'PASSWORD', 'pass-2104', 'ACTIVE', 2, NULL, now(), now()),
  (4105, 2105, '그룹5 서브', '그룹5 서브 설명', NULL, 'OPEN', NULL, 'ACTIVE', 2, NULL, now(), now()),
  (4106, 2106, '그룹6 서브', '그룹6 서브 설명', NULL, 'PASSWORD', 'pass-2106', 'ACTIVE', 2, NULL, now(), now()),
  (4107, 2107, '그룹7 서브', '그룹7 서브 설명', NULL, 'OPEN', NULL, 'ACTIVE', 2, NULL, now(), now()),
  (4108, 2108, '그룹8 서브', '그룹8 서브 설명', NULL, 'PASSWORD', 'pass-2108', 'ACTIVE', 2, NULL, now(), now()),
  (4109, 2109, '그룹9 서브', '그룹9 서브 설명', NULL, 'OPEN', NULL, 'ACTIVE', 2, NULL, now(), now()),
  (4110, 2110, '그룹10 서브', '그룹10 서브 설명', NULL, 'PASSWORD', 'pass-2110', 'ACTIVE', 2, NULL, now(), now());

INSERT INTO subgroup_member (id, subgroup_id, member_id, deleted_at, created_at) VALUES
  (5101, 4101, 1101, NULL, now()), (5102, 4101, 1102, NULL, now()),
  (5103, 4102, 1102, NULL, now()), (5104, 4102, 1103, NULL, now()),
  (5105, 4103, 1103, NULL, now()), (5106, 4103, 1104, NULL, now()),
  (5107, 4104, 1104, NULL, now()), (5108, 4104, 1105, NULL, now()),
  (5109, 4105, 1105, NULL, now()), (5110, 4105, 1106, NULL, now()),
  (5111, 4106, 1106, NULL, now()), (5112, 4106, 1107, NULL, now()),
  (5113, 4107, 1107, NULL, now()), (5114, 4107, 1108, NULL, now()),
  (5115, 4108, 1108, NULL, now()), (5116, 4108, 1109, NULL, now()),
  (5117, 4109, 1109, NULL, now()), (5118, 4109, 1110, NULL, now()),
  (5119, 4110, 1110, NULL, now()), (5120, 4110, 1101, NULL, now());

-- Restaurants (10)
INSERT INTO restaurant (
  id, name, full_address, location, deleted_at, created_at, updated_at
) VALUES
  (8001, '로컬 양식당', '서울특별시 종로구 세종대로 1', ST_GeomFromText('POINT(126.9780 37.5665)', 4326), NULL, now(), now()),
  (8002, '로컬 분식집', '서울특별시 중구 명동 2', ST_GeomFromText('POINT(126.9860 37.5635)', 4326), NULL, now(), now()),
  (8003, '로컬 베이커리', '서울특별시 용산구 이태원 3', ST_GeomFromText('POINT(126.9940 37.5340)', 4326), NULL, now(), now()),
  (8004, '로컬 치킨', '서울특별시 성동구 왕십리 4', ST_GeomFromText('POINT(127.0370 37.5610)', 4326), NULL, now(), now()),
  (8005, '로컬 피자', '서울특별시 광진구 건대입구 5', ST_GeomFromText('POINT(127.0700 37.5400)', 4326), NULL, now(), now()),
  (8006, '로컬 아시아', '서울특별시 마포구 홍대입구 6', ST_GeomFromText('POINT(126.9240 37.5570)', 4326), NULL, now(), now()),
  (8007, '로컬 한식', '서울특별시 영등포구 여의도 7', ST_GeomFromText('POINT(126.9237 37.5219)', 4326), NULL, now(), now()),
  (8008, '로컬 일식', '서울특별시 서초구 서초동 8', ST_GeomFromText('POINT(127.0145 37.4836)', 4326), NULL, now(), now()),
  (8009, '로컬 중식', '서울특별시 강서구 마곡 9', ST_GeomFromText('POINT(126.8260 37.5660)', 4326), NULL, now(), now()),
  (8010, '로컬 카페 2', '서울특별시 송파구 잠실 10', ST_GeomFromText('POINT(127.1000 37.5130)', 4326), NULL, now(), now());

INSERT INTO restaurant_address (
  id, restaurant_id, sido, sigungu, eupmyeondong, postal_code,
  created_at, updated_at
) VALUES
  (8101, 8001, '서울특별시', '종로구', '세종대로', '03000', now(), now()),
  (8102, 8002, '서울특별시', '중구', '명동', '04500', now(), now()),
  (8103, 8003, '서울특별시', '용산구', '이태원', '04300', now(), now()),
  (8104, 8004, '서울특별시', '성동구', '왕십리', '04700', now(), now()),
  (8105, 8005, '서울특별시', '광진구', '건대입구', '05000', now(), now()),
  (8106, 8006, '서울특별시', '마포구', '홍대입구', '04000', now(), now()),
  (8107, 8007, '서울특별시', '영등포구', '여의도', '07200', now(), now()),
  (8108, 8008, '서울특별시', '서초구', '서초동', '06600', now(), now()),
  (8109, 8009, '서울특별시', '강서구', '마곡', '07700', now(), now()),
  (8110, 8010, '서울특별시', '송파구', '잠실', '05500', now(), now());

INSERT INTO food_category (id, name) VALUES
  (6205, '양식'), (6206, '분식'), (6207, '베이커리'), (6208, '치킨'), (6209, '피자'), (6210, '아시아');

INSERT INTO restaurant_food_category (id, restaurant_id, food_category_id) VALUES
  (8301, 8001, 6205), (8302, 8002, 6206), (8303, 8003, 6207), (8304, 8004, 6208),
  (8305, 8005, 6209), (8306, 8006, 6210), (8307, 8007, 6201), (8308, 8008, 6202),
  (8309, 8009, 6204), (8310, 8010, 6203);

-- Reviews (10)
INSERT INTO review (
  id, restaurant_id, member_id, group_id, subgroup_id,
  content, is_recommended, deleted_at, created_at, updated_at
) VALUES
  (9001, 8001, 1101, 2101, 4101, '분위기가 좋아요.', true, NULL, now(), now()),
  (9002, 8002, 1102, 2102, 4102, '양이 넉넉합니다.', true, NULL, now(), now()),
  (9003, 8003, 1103, 2103, 4103, '디저트가 맛있어요.', true, NULL, now(), now()),
  (9004, 8004, 1104, 2104, 4104, '치킨이 바삭해요.', true, NULL, now(), now()),
  (9005, 8005, 1105, 2105, 4105, '피자가 담백해요.', true, NULL, now(), now()),
  (9006, 8006, 1106, 2106, 4106, '향신료가 적절해요.', false, NULL, now(), now()),
  (9007, 8007, 1107, 2107, 4107, '국물이 진해요.', true, NULL, now(), now()),
  (9008, 8008, 1108, 2108, 4108, '스시가 신선해요.', true, NULL, now(), now()),
  (9009, 8009, 1109, 2109, 4109, '볶음밥이 맛있어요.', false, NULL, now(), now()),
  (9010, 8010, 1110, 2110, 4110, '커피가 좋아요.', true, NULL, now(), now());

INSERT INTO keyword (id, type, name) VALUES
  (7110, 'VISIT_PURPOSE', '저녁'),
  (7111, 'VISIT_PURPOSE', '모임'),
  (7112, 'COMPANION_TYPE', '친구'),
  (7113, 'COMPANION_TYPE', '가족'),
  (7114, 'WAITING_EXPERIENCE', '10분대기'),
  (7115, 'WAITING_EXPERIENCE', '예약'),
  (7116, 'POSITIVE_ASPECT', '친절'),
  (7117, 'POSITIVE_ASPECT', '청결'),
  (7118, 'POSITIVE_ASPECT', '분위기'),
  (7119, 'POSITIVE_ASPECT', '접근성');

INSERT INTO review_keyword (id, review_id, keyword_id) VALUES
  (9201, 9001, 7118), (9202, 9002, 7111), (9203, 9003, 7117), (9204, 9004, 7116),
  (9205, 9005, 7110), (9206, 9006, 7114), (9207, 9007, 7119), (9208, 9008, 7112),
  (9209, 9009, 7113), (9210, 9010, 7115);

-- File domain images (10 restaurants + 10 reviews)
INSERT INTO image (
  id, file_name, file_size, file_type, storage_key, file_uuid,
  status, purpose, deleted_at, created_at, updated_at
) VALUES
  (9301, 'review-9001.jpg', 120001, 'image/jpeg', 'uploads/review/review-9001.jpg', 'aaaaaaaa-0000-0000-0000-000000000001', 'ACTIVE', 'REVIEW_IMAGE', NULL, now(), now()),
  (9302, 'review-9002.jpg', 120002, 'image/jpeg', 'uploads/review/review-9002.jpg', 'aaaaaaaa-0000-0000-0000-000000000002', 'ACTIVE', 'REVIEW_IMAGE', NULL, now(), now()),
  (9303, 'review-9003.jpg', 120003, 'image/jpeg', 'uploads/review/review-9003.jpg', 'aaaaaaaa-0000-0000-0000-000000000003', 'ACTIVE', 'REVIEW_IMAGE', NULL, now(), now()),
  (9304, 'review-9004.jpg', 120004, 'image/jpeg', 'uploads/review/review-9004.jpg', 'aaaaaaaa-0000-0000-0000-000000000004', 'ACTIVE', 'REVIEW_IMAGE', NULL, now(), now()),
  (9305, 'review-9005.jpg', 120005, 'image/jpeg', 'uploads/review/review-9005.jpg', 'aaaaaaaa-0000-0000-0000-000000000005', 'ACTIVE', 'REVIEW_IMAGE', NULL, now(), now()),
  (9306, 'review-9006.jpg', 120006, 'image/jpeg', 'uploads/review/review-9006.jpg', 'aaaaaaaa-0000-0000-0000-000000000006', 'ACTIVE', 'REVIEW_IMAGE', NULL, now(), now()),
  (9307, 'review-9007.jpg', 120007, 'image/jpeg', 'uploads/review/review-9007.jpg', 'aaaaaaaa-0000-0000-0000-000000000007', 'ACTIVE', 'REVIEW_IMAGE', NULL, now(), now()),
  (9308, 'review-9008.jpg', 120008, 'image/jpeg', 'uploads/review/review-9008.jpg', 'aaaaaaaa-0000-0000-0000-000000000008', 'ACTIVE', 'REVIEW_IMAGE', NULL, now(), now()),
  (9309, 'review-9009.jpg', 120009, 'image/jpeg', 'uploads/review/review-9009.jpg', 'aaaaaaaa-0000-0000-0000-000000000009', 'ACTIVE', 'REVIEW_IMAGE', NULL, now(), now()),
  (9310, 'review-9010.jpg', 120010, 'image/jpeg', 'uploads/review/review-9010.jpg', 'aaaaaaaa-0000-0000-0000-000000000010', 'ACTIVE', 'REVIEW_IMAGE', NULL, now(), now()),
  (9311, 'restaurant-8001.jpg', 220001, 'image/jpeg', 'uploads/restaurant/restaurant-8001.jpg', 'bbbbbbbb-0000-0000-0000-000000000001', 'ACTIVE', 'RESTAURANT_IMAGE', NULL, now(), now()),
  (9312, 'restaurant-8002.jpg', 220002, 'image/jpeg', 'uploads/restaurant/restaurant-8002.jpg', 'bbbbbbbb-0000-0000-0000-000000000002', 'ACTIVE', 'RESTAURANT_IMAGE', NULL, now(), now()),
  (9313, 'restaurant-8003.jpg', 220003, 'image/jpeg', 'uploads/restaurant/restaurant-8003.jpg', 'bbbbbbbb-0000-0000-0000-000000000003', 'ACTIVE', 'RESTAURANT_IMAGE', NULL, now(), now()),
  (9314, 'restaurant-8004.jpg', 220004, 'image/jpeg', 'uploads/restaurant/restaurant-8004.jpg', 'bbbbbbbb-0000-0000-0000-000000000004', 'ACTIVE', 'RESTAURANT_IMAGE', NULL, now(), now()),
  (9315, 'restaurant-8005.jpg', 220005, 'image/jpeg', 'uploads/restaurant/restaurant-8005.jpg', 'bbbbbbbb-0000-0000-0000-000000000005', 'ACTIVE', 'RESTAURANT_IMAGE', NULL, now(), now()),
  (9316, 'restaurant-8006.jpg', 220006, 'image/jpeg', 'uploads/restaurant/restaurant-8006.jpg', 'bbbbbbbb-0000-0000-0000-000000000006', 'ACTIVE', 'RESTAURANT_IMAGE', NULL, now(), now()),
  (9317, 'restaurant-8007.jpg', 220007, 'image/jpeg', 'uploads/restaurant/restaurant-8007.jpg', 'bbbbbbbb-0000-0000-0000-000000000007', 'ACTIVE', 'RESTAURANT_IMAGE', NULL, now(), now()),
  (9318, 'restaurant-8008.jpg', 220008, 'image/jpeg', 'uploads/restaurant/restaurant-8008.jpg', 'bbbbbbbb-0000-0000-0000-000000000008', 'ACTIVE', 'RESTAURANT_IMAGE', NULL, now(), now()),
  (9319, 'restaurant-8009.jpg', 220009, 'image/jpeg', 'uploads/restaurant/restaurant-8009.jpg', 'bbbbbbbb-0000-0000-0000-000000000009', 'ACTIVE', 'RESTAURANT_IMAGE', NULL, now(), now()),
  (9320, 'restaurant-8010.jpg', 220010, 'image/jpeg', 'uploads/restaurant/restaurant-8010.jpg', 'bbbbbbbb-0000-0000-0000-000000000010', 'ACTIVE', 'RESTAURANT_IMAGE', NULL, now(), now());

INSERT INTO domain_image (
  id, domain_type, domain_id, image_id, sort_order, created_at
) VALUES
  (9401, 'REVIEW', 9001, 9301, 0, now()), (9402, 'REVIEW', 9002, 9302, 0, now()),
  (9403, 'REVIEW', 9003, 9303, 0, now()), (9404, 'REVIEW', 9004, 9304, 0, now()),
  (9405, 'REVIEW', 9005, 9305, 0, now()), (9406, 'REVIEW', 9006, 9306, 0, now()),
  (9407, 'REVIEW', 9007, 9307, 0, now()), (9408, 'REVIEW', 9008, 9308, 0, now()),
  (9409, 'REVIEW', 9009, 9309, 0, now()), (9410, 'REVIEW', 9010, 9310, 0, now()),
  (9411, 'RESTAURANT', 8001, 9311, 0, now()), (9412, 'RESTAURANT', 8002, 9312, 0, now()),
  (9413, 'RESTAURANT', 8003, 9313, 0, now()), (9414, 'RESTAURANT', 8004, 9314, 0, now()),
  (9415, 'RESTAURANT', 8005, 9315, 0, now()), (9416, 'RESTAURANT', 8006, 9316, 0, now()),
  (9417, 'RESTAURANT', 8007, 9317, 0, now()), (9418, 'RESTAURANT', 8008, 9318, 0, now()),
  (9419, 'RESTAURANT', 8009, 9319, 0, now()), (9420, 'RESTAURANT', 8010, 9320, 0, now());

-- OAuth accounts + refresh tokens (sample)
INSERT INTO member_oauth_account (
  id, member_id, provider, provider_user_id, provider_user_email, created_at
) VALUES
  (8201, 1101, 'google', 'local-google-1101', 'local.user3@tasteam.dev', now()),
  (8202, 1102, 'kakao', 'local-kakao-1102', 'local.user4@tasteam.dev', now()),
  (8203, 1103, 'google', 'local-google-1103', 'local.user5@tasteam.dev', now()),
  (8204, 1104, 'kakao', 'local-kakao-1104', 'local.user6@tasteam.dev', now()),
  (8205, 1105, 'google', 'local-google-1105', 'local.user7@tasteam.dev', now());

INSERT INTO refresh_token (
  id, member_id, token_hash, token_family_id, expires_at, rotated_at, revoked_at, created_at
) VALUES
  (8206, 1101, 'cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc', 'dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd', now() + interval '30 days', NULL, NULL, now()),
  (8207, 1102, 'eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee', 'ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff', now() + interval '30 days', NULL, NULL, now()),
  (8208, 1103, '1111111111111111111111111111111111111111111111111111111111111111', '2222222222222222222222222222222222222222222222222222222222222222', now() + interval '30 days', NULL, NULL, now()),
  (8209, 1104, '3333333333333333333333333333333333333333333333333333333333333333', '4444444444444444444444444444444444444444444444444444444444444444', now() + interval '30 days', NULL, NULL, now()),
  (8210, 1105, '5555555555555555555555555555555555555555555555555555555555555555', '6666666666666666666666666666666666666666666666666666666666666666', now() + interval '30 days', NULL, NULL, now());

-- Announcements (sample)
INSERT INTO announcement (
  id, title, content, deleted_at, created_at, updated_at
) VALUES
  (20001, '서비스 점검 안내', '2월 셋째 주 새벽 2시~3시 사이에 점검이 예정되어 있습니다. 이용에 참고해주세요.', NULL, now(), now()),
  (20002, '신규 프로모션 런칭', '테이스팀 앱에서 진행되는 신규 프로모션을 확인해보세요.', NULL, now(), now());

-- Promotions (sample)
INSERT INTO promotion (
  id, title, content, landing_url, promotion_start_at, promotion_end_at, publish_status, deleted_at, created_at, updated_at
) VALUES
  (12001, '봄 한정 스페셜 쿠폰', '메뉴 2개 이상 주문 시 15% 할인 쿠폰을 드립니다.', 'https://tasteam.kr/spring-special', now() - interval '1 day', now() + interval '30 days', 'PUBLISHED', NULL, now(), now()),
  (12002, '신규 가입 웰컴 이벤트', '신규 가입 후 첫 리뷰 작성 시 아메리카노 쿠폰을 드립니다.', 'https://tasteam.kr/welcome', now() + interval '1 day', now() + interval '45 days', 'PUBLISHED', NULL, now(), now());

INSERT INTO promotion_display (
  id, promotion_id, display_enabled, display_start_at, display_end_at, display_channel, display_priority, deleted_at, created_at, updated_at
) VALUES
  (13001, 12001, true, now() - interval '1 day', now() + interval '30 days', 'BOTH', 1, NULL, now(), now()),
  (13002, 12002, true, now(), now() + interval '45 days', 'PROMOTION_LIST', 2, NULL, now(), now());

INSERT INTO promotion_asset (
  id, promotion_id, asset_type, image_url, alt_text, sort_order, is_primary, deleted_at, created_at, updated_at
) VALUES
  (14001, 12001, 'BANNER', 'https://picsum.photos/seed/promo12001-banner/1600/800', '봄 한정 스페셜 배너', 0, true, NULL, now(), now()),
  (14002, 12001, 'DETAIL', 'https://picsum.photos/seed/promo12001-detail1/1200/800', '봄 한정 메뉴 이미지 1', 1, false, NULL, now(), now()),
  (14003, 12001, 'DETAIL', 'https://picsum.photos/seed/promo12001-detail2/1200/800', '봄 한정 메뉴 이미지 2', 2, false, NULL, now(), now()),
  (14004, 12002, 'BANNER', 'https://picsum.photos/seed/promo12002-banner/1600/800', '웰컴 이벤트 배너', 0, true, NULL, now(), now()),
  (14005, 12002, 'DETAIL', 'https://picsum.photos/seed/promo12002-detail1/1200/800', '웰컴 이벤트 안내 1', 1, false, NULL, now(), now());

-- AI analysis (sample for 10 restaurants)
INSERT INTO ai_restaurant_comparison (
  id, restaurant_id, category_lift, comparison_display,
  total_candidates, validated_count, analyzed_at, status, created_at, updated_at
) VALUES
  (9503, 8001, '{"service": 0.0900, "price": 0.0400, "food": 0.1200}'::jsonb, '["분위기가 안정적인 양식당입니다."]'::jsonb, 20, 14, now(), 'COMPLETED', now(), now()),
  (9504, 8002, '{"service": 0.1100, "price": 0.1000, "food": 0.0600}'::jsonb, '["빠른 회전율의 분식집입니다."]'::jsonb, 20, 13, now(), 'COMPLETED', now(), now()),
  (9505, 8003, '{"service": 0.0800, "price": 0.0500, "food": 0.1300}'::jsonb, '["디저트 종류가 다양한 베이커리입니다."]'::jsonb, 20, 16, now(), 'COMPLETED', now(), now()),
  (9506, 8004, '{"service": 0.0700, "price": 0.0600, "food": 0.1400}'::jsonb, '["치킨의 풍미가 좋은 곳입니다."]'::jsonb, 20, 15, now(), 'COMPLETED', now(), now()),
  (9507, 8005, '{"service": 0.0600, "price": 0.0700, "food": 0.1500}'::jsonb, '["피자 토핑이 풍부한 곳입니다."]'::jsonb, 20, 12, now(), 'COMPLETED', now(), now()),
  (9508, 8006, '{"service": 0.0500, "price": 0.0300, "food": 0.0900}'::jsonb, '["아시아 음식 특유의 향이 있습니다."]'::jsonb, 20, 11, now(), 'COMPLETED', now(), now()),
  (9509, 8007, '{"service": 0.1000, "price": 0.0800, "food": 0.1200}'::jsonb, '["국물 요리가 강점입니다."]'::jsonb, 20, 15, now(), 'COMPLETED', now(), now()),
  (9510, 8008, '{"service": 0.0900, "price": 0.0600, "food": 0.1600}'::jsonb, '["신선한 해산물을 제공합니다."]'::jsonb, 20, 17, now(), 'COMPLETED', now(), now()),
  (9511, 8009, '{"service": 0.0700, "price": 0.0900, "food": 0.1100}'::jsonb, '["중식 볶음 요리가 인기입니다."]'::jsonb, 20, 14, now(), 'COMPLETED', now(), now()),
  (9512, 8010, '{"service": 0.1200, "price": 0.0500, "food": 0.1000}'::jsonb, '["커피 향이 좋은 카페입니다."]'::jsonb, 20, 18, now(), 'COMPLETED', now(), now());

INSERT INTO ai_restaurant_review_analysis (
  id, restaurant_id, overall_summary, category_summaries,
  positive_ratio, negative_ratio, analyzed_at, status, created_at, updated_at
) VALUES
  (9603, 8001, '전체적으로 만족도가 높습니다.', '{"service":"서비스 만족도가 높습니다.","price":"가격 대비 만족도가 높습니다.","food":"음식 품질 평가가 좋습니다."}'::jsonb, 0.8800, 0.1200, now(), 'COMPLETED', now(), now()),
  (9604, 8002, '가성비가 좋다는 평가가 많습니다.', '{"service":"빠른 제공 속도가 장점입니다.","price":"가성비가 좋다는 의견이 많습니다.","food":"메뉴 만족도가 안정적입니다."}'::jsonb, 0.8200, 0.1800, now(), 'COMPLETED', now(), now()),
  (9605, 8003, '디저트 만족도가 높습니다.', '{"service":"응대가 친절하다는 평가가 있습니다.","price":"가격이 합리적이라는 의견이 많습니다.","food":"디저트 품질이 좋습니다."}'::jsonb, 0.9000, 0.1000, now(), 'COMPLETED', now(), now()),
  (9606, 8004, '치킨 식감이 좋습니다.', '{"service":"포장 및 응대가 무난합니다.","price":"가격이 적절하다는 의견이 있습니다.","food":"치킨 식감 만족도가 높습니다."}'::jsonb, 0.8600, 0.1400, now(), 'COMPLETED', now(), now()),
  (9607, 8005, '토핑에 대한 평가가 좋습니다.', '{"service":"주문 처리 속도가 빠른 편입니다.","price":"가격 대비 토핑 구성이 좋습니다.","food":"토핑 만족도가 높습니다."}'::jsonb, 0.8400, 0.1600, now(), 'COMPLETED', now(), now()),
  (9608, 8006, '향신료 호불호가 있습니다.', '{"service":"서비스는 안정적입니다.","price":"가격 평가는 보통 수준입니다.","food":"향신료 취향 차이가 있습니다."}'::jsonb, 0.7200, 0.2800, now(), 'COMPLETED', now(), now()),
  (9609, 8007, '국물 맛이 강점입니다.', '{"service":"응대가 빠른 편입니다.","price":"가격 대비 만족도가 높습니다.","food":"국물 맛 평가가 좋습니다."}'::jsonb, 0.8900, 0.1100, now(), 'COMPLETED', now(), now()),
  (9610, 8008, '신선도에 대한 만족도가 높습니다.', '{"service":"친절도에 대한 평가가 좋습니다.","price":"가격은 다소 높지만 수용 가능하다는 의견입니다.","food":"신선도 만족도가 높습니다."}'::jsonb, 0.9100, 0.0900, now(), 'COMPLETED', now(), now()),
  (9611, 8009, '볶음 요리가 인기입니다.', '{"service":"서비스는 무난합니다.","price":"가격 대비 적절하다는 의견이 있습니다.","food":"볶음 요리 선호도가 높습니다."}'::jsonb, 0.8000, 0.2000, now(), 'COMPLETED', now(), now()),
  (9612, 8010, '카페 분위기가 좋습니다.', '{"service":"직원 응대가 친절합니다.","price":"가격이 합리적이라는 평가가 많습니다.","food":"커피 맛과 분위기 만족도가 높습니다."}'::jsonb, 0.9300, 0.0700, now(), 'COMPLETED', now(), now());

INSERT INTO ai_restaurant_recommendation (
  id, restaurant_id, reason, cache_key, expires_at, created_at
) VALUES
  (9703, 8001, '모임 장소로 추천됩니다.', 'seed:member:1101:query:restaurant-8001', now() + interval '1 day', now()),
  (9704, 8002, '빠르게 식사할 곳으로 추천됩니다.', 'seed:member:1102:query:restaurant-8002', now() + interval '1 day', now()),
  (9705, 8003, '디저트 모임에 적합합니다.', 'seed:member:1103:query:restaurant-8003', now() + interval '1 day', now()),
  (9706, 8004, '치킨 선호자에게 추천됩니다.', 'seed:member:1104:query:restaurant-8004', now() + interval '1 day', now()),
  (9707, 8005, '피자 모임에 적합합니다.', 'seed:member:1105:query:restaurant-8005', now() + interval '1 day', now()),
  (9708, 8006, '이색 음식 체험에 좋습니다.', 'seed:member:1106:query:restaurant-8006', now() + interval '1 day', now()),
  (9709, 8007, '따뜻한 국물 메뉴가 인기입니다.', 'seed:member:1107:query:restaurant-8007', now() + interval '1 day', now()),
  (9710, 8008, '신선한 해산물로 추천됩니다.', 'seed:member:1108:query:restaurant-8008', now() + interval '1 day', now()),
  (9711, 8009, '중식 볶음 메뉴가 강점입니다.', 'seed:member:1109:query:restaurant-8009', now() + interval '1 day', now()),
  (9712, 8010, '카페 작업 장소로 좋습니다.', 'seed:member:1110:query:restaurant-8010', now() + interval '1 day', now());

-- Search history for new members
INSERT INTO member_search_history (
  id, member_id, keyword, count, deleted_at, created_at, updated_at
) VALUES
  (7403, 1101, '종로 양식', 1, NULL, now(), now()),
  (7404, 1102, '명동 분식', 1, NULL, now(), now()),
  (7405, 1103, '이태원 베이커리', 1, NULL, now(), now()),
  (7406, 1104, '왕십리 치킨', 1, NULL, now(), now()),
  (7407, 1105, '건대 피자', 1, NULL, now(), now()),
  (7408, 1106, '홍대 아시아', 1, NULL, now(), now()),
  (7409, 1107, '여의도 한식', 1, NULL, now(), now()),
  (7410, 1108, '서초 일식', 1, NULL, now(), now()),
  (7411, 1109, '마곡 중식', 1, NULL, now(), now()),
  (7412, 1110, '잠실 카페', 1, NULL, now(), now());

-- Keep identity sequence aligned after explicit id inserts.
SELECT setval(
  pg_get_serial_sequence('member_search_history', 'id'),
  COALESCE((SELECT MAX(id) FROM member_search_history), 1),
  true
);

SELECT setval(
  pg_get_serial_sequence('ai_restaurant_comparison', 'id'),
  COALESCE((SELECT MAX(id) FROM ai_restaurant_comparison), 1),
  true
);

SELECT setval(
  pg_get_serial_sequence('ai_restaurant_review_analysis', 'id'),
  COALESCE((SELECT MAX(id) FROM ai_restaurant_review_analysis), 1),
  true
);

SELECT setval(
  pg_get_serial_sequence('ai_restaurant_recommendation', 'id'),
  COALESCE((SELECT MAX(id) FROM ai_restaurant_recommendation), 1),
  true
);
