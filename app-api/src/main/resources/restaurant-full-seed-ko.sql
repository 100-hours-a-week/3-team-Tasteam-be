-- Single-restaurant full seed (Korean text)

-- Restaurant (master)
INSERT INTO restaurant (
  id, name, full_address, location, deleted_at, created_at, updated_at
) VALUES
  (12001, '테이스팀 비스트로', '서울특별시 마포구 합정동 123-45',
   ST_GeomFromText('POINT(126.9147 37.5485)', 4326),
   NULL, now(), now());

-- Address (1:1)
INSERT INTO restaurant_address (
  id, restaurant_id, sido, sigungu, eupmyeondong, postal_code,
  created_at, updated_at
) VALUES
  (12101, 12001, '서울특별시', '마포구', '합정동', '04000', now(), now());

-- Food categories (master)
INSERT INTO food_category (id, name) VALUES
  (12201, '한식'),
  (12202, '카페');

-- Restaurant ↔ FoodCategory (N:M)
INSERT INTO restaurant_food_category (id, restaurant_id, food_category_id) VALUES
  (12301, 12001, 12201),
  (12302, 12001, 12202);

-- Weekly schedule (1:N, Mon=1 ... Sun=7)
INSERT INTO restaurant_weekly_schedule (
  id, restaurant_id, day_of_week, open_time, close_time, is_closed,
  effective_from, effective_to, created_at, updated_at
) VALUES
  (12401, 12001, 1, '10:00', '21:00', false, '2026-01-30', NULL, now(), now()),
  (12402, 12001, 2, '10:00', '21:00', false, '2026-01-30', NULL, now(), now()),
  (12403, 12001, 3, '10:00', '21:00', false, '2026-01-30', NULL, now(), now()),
  (12404, 12001, 4, '10:00', '21:00', false, '2026-01-30', NULL, now(), now()),
  (12405, 12001, 5, '10:00', '22:00', false, '2026-01-30', NULL, now(), now()),
  (12406, 12001, 6, '11:00', '22:00', false, '2026-01-30', NULL, now(), now()),
  (12407, 12001, 7, NULL, NULL, true,  '2026-01-30', NULL, now(), now());

-- Schedule override (1:N, 특정일 예외)
INSERT INTO restaurant_schedule_override (
  id, restaurant_id, date, open_time, close_time, is_closed, reason,
  created_at, updated_at
) VALUES
  (12501, 12001, '2026-02-10', '12:00', '18:00', false, '단축 영업', now(), now()),
  (12502, 12001, '2026-02-11', NULL, NULL, true,  '공휴일 휴무', now(), now());

-- Restaurant images (legacy domain)
INSERT INTO restaurant_image (
  id, restaurant_id, image_url, sort_order, deleted_at, created_at
) VALUES
  (12601, 12001, 'https://picsum.photos/seed/tasteam-12001/800/600', 1, NULL, now()),
  (12602, 12001, 'https://picsum.photos/seed/tasteam-12001-2/800/600', 2, NULL, now());

-- Menu categories (1:N)
INSERT INTO menu_category (
  id, restaurant_id, name, display_order, created_at, updated_at
) VALUES
  (12701, 12001, '메인', 0, now(), now()),
  (12702, 12001, '음료', 1, now(), now());

-- Menus (1:N per category)
INSERT INTO menu (
  id, category_id, name, description, price, image_url,
  is_recommended, display_order, created_at, updated_at
) VALUES
  (12801, 12701, '트러플 파스타', '크리미한 트러플 파스타', 15000,
   'https://picsum.photos/seed/menu-12001-1/600/400', true, 0, now(), now()),
  (12802, 12701, '등심 스테이크', '250g 그릴 스테이크', 28000,
   NULL, false, 1, now(), now()),
  (12803, 12702, '아메리카노', '핫/아이스', 4500,
   NULL, false, 0, now(), now()),
  (12804, 12702, '레몬에이드', '상큼한 레몬 소다', 5500,
   NULL, false, 1, now(), now());

-- (선택) File domain 이미지 사용 시
INSERT INTO image (
  id, file_name, file_size, file_type, storage_key, file_uuid,
  status, purpose, deleted_at, created_at, updated_at
) VALUES
  (12901, 'restaurant-12001-1.jpg', 234567, 'image/jpeg', 'uploads/restaurant/restaurant-12001-1.jpg',
   'aaaaaaaa-0000-0000-0000-000000000001', 'ACTIVE', 'RESTAURANT_IMAGE', NULL, now(), now()),
  (12902, 'restaurant-12001-2.jpg', 245678, 'image/jpeg', 'uploads/restaurant/restaurant-12001-2.jpg',
   'aaaaaaaa-0000-0000-0000-000000000002', 'ACTIVE', 'RESTAURANT_IMAGE', NULL, now(), now());

INSERT INTO domain_image (
  id, domain_type, domain_id, image_id, sort_order, created_at
) VALUES
  (13001, 'RESTAURANT', 12001, 12901, 0, now()),
  (13002, 'RESTAURANT', 12001, 12902, 1, now());
