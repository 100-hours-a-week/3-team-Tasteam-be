-- 성능 테스트용 음식점 더미 데이터 생성 스크립트
-- 필요 시 아래 generate_series(1, 100000) 숫자를 변경하세요.

-- 시퀀스를 현재 최대 id보다 앞으로 이동
SELECT setval(pg_get_serial_sequence('restaurant','id'),
              COALESCE((SELECT MAX(id)+1 FROM restaurant), 1),
              false);

-- generate_series로 데이터 생성, 약 30%는 이름에 '치킨' 포함
WITH seed AS (
  SELECT g AS gid,
         CASE WHEN random() < 0.3 THEN '치킨맛집-'||g ELSE '맛집-'||g END AS name,
         '서울시 테스트구 테스트로 '||g||'번지' AS addr,
         POINT(126.9 + (random()-0.5)*0.1, 37.5 + (random()-0.5)*0.1) AS pt,
         LPAD((g % 10000)::text, 4, '0') AS phone_suffix
  FROM generate_series(1, 100000) g
)
INSERT INTO restaurant (name, full_address, location, phone_number, created_at, updated_at, deleted_at)
SELECT name,
       addr,
       ST_SetSRID(pt::geometry, 4326),
       '010-1234-'||phone_suffix,
       NOW() - (seed.gid % 30) * INTERVAL '1 day',
       NOW() - (seed.gid % 5) * INTERVAL '1 hour',
       NULL
FROM seed;

ANALYZE restaurant;
