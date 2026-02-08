# 검색 쿼리 1단계/2단계 비교 (TRGM + 유사도) - 2026-02-07

## 환경
- DB: PostgreSQL 17 (docker-compose.local `tasteam-db`)
- 데이터: restaurant 104,017건 (더미 시드 후)
- 키워드: `치킨`
- 위치: 위도 37.5 / 경도 126.9 (거리 정렬 포함)

## 1단계 적용 (단일 쿼리)
쿼리:
```sql
SELECT r.*,
       (lower(r.name)=lower('치킨')) AS name_exact,
       similarity(lower(r.name), lower('치킨')) AS name_sim,
       CASE WHEN 37.5 IS NULL OR 126.9 IS NULL THEN NULL
            ELSE ST_DistanceSphere(r.location, ST_MakePoint(126.9, 37.5)) END AS dist_m,
       (lower(r.full_address) LIKE '%'||lower('치킨')||'%') AS addr_match
FROM restaurant r
WHERE r.deleted_at IS NULL
  AND (
    lower(r.name) LIKE '%'||lower('치킨')||'%'
    OR lower(r.full_address) LIKE '%'||lower('치킨')||'%'
  )
ORDER BY name_exact DESC, name_sim DESC, dist_m ASC NULLS LAST,
         addr_match DESC, r.updated_at DESC, r.id DESC
LIMIT 20;
```

실행 계획(원문):
```
Limit  (cost=229113.90..229116.20 rows=20 width=153) (actual time=727.886..750.078 rows=20 loops=1)
  Buffers: shared hit=2702
  ->  Gather Merge  (cost=229113.90..231175.28 rows=17925 width=153) (actual time=522.339..544.483 rows=20 loops=1)
        Workers Planned: 1
        Workers Launched: 1
        Buffers: shared hit=2702
        ->  Sort  (cost=228113.89..228158.71 rows=17925 width=153) (actual time=305.745..305.750 rows=20 loops=2)
              Sort Key: ((lower((name)::text) = '치킨'::text)) DESC, (similarity(lower((name)::text), '치킨'::text)) DESC, (st_distance(geography(location), '0101000020E61000009A99999999B95F400000000000C04240'::geography, false)), ((lower((full_address)::text) ~~ '%치킨%'::text)) DESC, updated_at DESC, id DESC
              Sort Method: top-N heapsort  Memory: 34kB
              Buffers: shared hit=2702
              Worker 0:  Sort Method: quicksort  Memory: 32kB
              ->  Parallel Seq Scan on restaurant r  (cost=0.00..227636.92 rows=17925 width=153) (actual time=183.136..297.979 rows=15280 loops=2)
                    Filter: ((deleted_at IS NULL) AND ((lower((name)::text) ~~ '%치킨%'::text) OR (lower((full_address)::text) ~~ '%치킨%'::text)))
                    Rows Removed by Filter: 36728
                    Buffers: shared hit=2613
Planning:
  Buffers: shared hit=232
Planning Time: 56.379 ms
JIT:
  Functions: 9
  Options: Inlining false, Optimization false, Expressions true, Deforming true
  Timing: Generation 24.593 ms (Deform 9.643 ms), Inlining 0.000 ms, Optimization 39.039 ms, Emission 362.867 ms, Total 426.499 ms
Execution Time: 955.651 ms
```

## 2단계 적용 (후보 추출 → 정렬)
쿼리:
```sql
WITH candidates AS (
  SELECT r.id
  FROM restaurant r
  WHERE r.deleted_at IS NULL
    AND (
      lower(r.name) LIKE '%'||lower('치킨')||'%'
      OR lower(r.full_address) LIKE '%'||lower('치킨')||'%'
    )
  ORDER BY similarity(lower(r.name), lower('치킨')) DESC
  LIMIT 200
)
SELECT r.*,
       (lower(r.name)=lower('치킨')) AS name_exact,
       similarity(lower(r.name), lower('치킨')) AS name_sim,
       CASE WHEN 37.5 IS NULL OR 126.9 IS NULL THEN NULL
            ELSE ST_DistanceSphere(r.location, ST_MakePoint(126.9, 37.5)) END AS dist_m,
       (lower(r.full_address) LIKE '%'||lower('치킨')||'%') AS addr_match
FROM restaurant r
JOIN candidates c ON c.id = r.id
ORDER BY name_exact DESC, name_sim DESC, dist_m ASC NULLS LAST,
         addr_match DESC, r.updated_at DESC, r.id DESC
LIMIT 20;
```

실행 계획(원문):
```
Limit  (cost=9080.89..9080.94 rows=20 width=153) (actual time=206.150..206.773 rows=20 loops=1)
  Buffers: shared hit=2841
  ->  Sort  (cost=9080.89..9081.39 rows=200 width=153) (actual time=206.056..206.676 rows=20 loops=1)
        Sort Key: ((lower((r.name)::text) = '치킨'::text)) DESC, (similarity(lower((r.name)::text), '치킨'::text)) DESC, (st_distance(geography(r.location), '0101000020E61000009A99999999B95F400000000000C04240'::geography, false)), ((lower((r.full_address)::text) ~~ '%치킨%'::text)) DESC, r.updated_at DESC, r.id DESC
        Sort Method: top-N heapsort  Memory: 33kB
        Buffers: shared hit=2841
        ->  Nested Loop  (cost=5125.36..9075.57 rows=200 width=153) (actual time=204.292..206.264 rows=200 loops=1)
              Buffers: shared hit=2827
              ->  Limit  (cost=5125.07..5148.07 rows=200 width=12) (actual time=124.623..125.356 rows=200 loops=1)
                    Buffers: shared hit=2095
                    ->  Gather Merge  (cost=5125.07..7186.45 rows=17925 width=12) (actual time=124.592..125.265 rows=200 loops=1)
                          Workers Planned: 1
                          Workers Launched: 1
                          Buffers: shared hit=2095
                          ->  Sort  (cost=4125.06..4169.87 rows=17925 width=12) (actual time=91.645..91.667 rows=200 loops=2)
                                Sort Key: (similarity(lower((r_1.name)::text), '치킨'::text)) DESC
                                Sort Method: top-N heapsort  Memory: 34kB
                                Buffers: shared hit=2095
                                Worker 0:  Sort Method: top-N heapsort  Memory: 32kB
                                ->  Parallel Seq Scan on restaurant r_1  (cost=0.00..3350.35 rows=17925 width=12) (actual time=1.854..87.540 rows=15280 loops=2)
                                      Filter: ((deleted_at IS NULL) AND ((lower((name)::text) ~~ '%치킨%'::text) OR (lower((full_address)::text) ~~ '%치킨%'::text)))
                                      Rows Removed by Filter: 36728
                                      Buffers: shared hit=2037
              ->  Index Scan using restaurant_pkey on restaurant r  (cost=0.29..7.11 rows=1 width=139) (actual time=0.003..0.003 rows=1 loops=200)
                    Index Cond: (id = r_1.id)
                    Buffers: shared hit=600
Planning:
  Buffers: shared hit=300
Planning Time: 63.619 ms
Execution Time: 208.747 ms
```
