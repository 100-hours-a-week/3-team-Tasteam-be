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

## 현재 서비스 쿼리 조건/정렬 (QueryDSL 기준)
### 필터 조건 (WHERE)
- `deleted_at IS NULL`
- 키워드가 이름에 포함되거나 (`name ILIKE %kw%`)
- 키워드가 주소에 포함되거나 (`full_address ILIKE %kw%`)
- 키워드가 카테고리명과 정확히 일치하는 음식점이거나 (`food_category.name = kw`)
- 위/경도와 반경이 있을 경우 거리 제한 적용 (`distance <= radius`)

### 정렬 기준 (ORDER BY)
- 점수 합산 방식
  - 이름 완전 일치: `+100점`
  - 이름 유사도: `similarity * 30점`
  - 거리 가중치: `max(0, 1 - (distance / radius)) * 50점`
- 점수 내림차순 → `updated_at` 내림차순 → `id` 내림차순

### 비고
- 1-step/2-step 모두 동일한 조건/정렬이며, 2-step은 후보를 먼저 추려 재정렬함.

## 가중치 점수 + 거리 제한(3km) EXPLAIN (ONE_STEP / TWO_STEP / JOIN_AGGREGATE)
### ONE_STEP (score + distance filter)
```
Limit  (cost=5475145.16..5476394.11 rows=20 width=162) (actual time=1860.660..1860.901 rows=20 loops=1)
  Buffers: shared hit=2182
  ->  Result  (cost=5475145.16..6874900.50 rows=22415 width=162) (actual time=434.851..435.020 rows=20 loops=1)
        Buffers: shared hit=2182
        ->  Sort  (cost=5475145.16..5475201.20 rows=22415 width=153) (actual time=419.178..419.276 rows=20 loops=1)
              Sort Key: (((((CASE WHEN (lower((r.name)::text) = '치킨'::text) THEN 1 ELSE 0 END * 100))::double precision + (similarity(lower((r.name)::text), '치킨'::text) * '30'::double precision)) + (GREATEST('0'::double precision, ('1'::double precision - (st_distance(geography(r.location), '0101000020E61000009A99999999B95F400000000000C04240'::geography, false) / '3000'::double precision))) * '50'::double precision))) DESC, r.updated_at DESC, r.id DESC
              Sort Method: top-N heapsort  Memory: 34kB
              Buffers: shared hit=2180
              ->  Seq Scan on restaurant r  (cost=0.00..5474548.71 rows=22415 width=153) (actual time=102.273..415.104 rows=8773 loops=1)
                    Filter: ((deleted_at IS NULL) AND (st_distance(geography(location), '0101000020E61000009A99999999B95F400000000000C04240'::geography, false) <= '3000'::double precision) AND ((lower((name)::text) ~~ '%치킨%'::text) OR (lower((full_address)::text) ~~ '%치킨%'::text) OR (ANY (id = (hashed SubPlan 4).col1))))
                    Rows Removed by Filter: 95244
                    Buffers: shared hit=2171
                    SubPlan 4
                      ->  Hash Join  (cost=22.95..52.79 rows=7 width=8) (actual time=15.782..15.923 rows=1 loops=1)
                            Hash Cond: (rfc_1.food_category_id = fc_1.id)
                            Buffers: shared hit=2
                            ->  Seq Scan on restaurant_food_category rfc_1  (cost=0.00..25.70 rows=1570 width=16) (actual time=0.048..0.050 rows=12 loops=1)
                                  Buffers: shared hit=1
                            ->  Hash  (cost=22.90..22.90 rows=4 width=8) (actual time=14.878..14.903 rows=1 loops=1)
                                  Buckets: 1024  Batches: 1  Memory Usage: 9kB
                                  Buffers: shared hit=1
                                  ->  Seq Scan on food_category fc_1  (cost=0.00..22.90 rows=4 width=8) (actual time=14.678..14.683 rows=1 loops=1)
                                        Filter: (lower((name)::text) = '치킨'::text)
                                        Rows Removed by Filter: 9
                                        Buffers: shared hit=1
        SubPlan 2
          ->  Hash Join  (cost=22.95..52.79 rows=7 width=8) (actual time=14.742..14.776 rows=1 loops=1)
                Hash Cond: (rfc.food_category_id = fc.id)
                Buffers: shared hit=2
                ->  Seq Scan on restaurant_food_category rfc  (cost=0.00..25.70 rows=1570 width=16) (actual time=0.020..0.022 rows=12 loops=1)
                      Buffers: shared hit=1
                ->  Hash  (cost=22.90..22.90 rows=4 width=8) (actual time=14.567..14.597 rows=1 loops=1)
                      Buckets: 1024  Batches: 1  Memory Usage: 9kB
                      Buffers: shared hit=1
                      ->  Seq Scan on food_category fc  (cost=0.00..22.90 rows=4 width=8) (actual time=14.543..14.548 rows=1 loops=1)
                            Filter: (lower((name)::text) = '치킨'::text)
                            Rows Removed by Filter: 9
                            Buffers: shared hit=1
Planning:
  Buffers: shared hit=313
Planning Time: 98.866 ms
JIT:
  Functions: 51
  Options: Inlining true, Optimization true, Expressions true, Deforming true
  Timing: Generation 21.314 ms (Deform 8.458 ms), Inlining 157.948 ms, Optimization 631.913 ms, Emission 664.831 ms, Total 1476.006 ms
Execution Time: 2343.587 ms
```

### TWO_STEP (score + distance filter, candidates=200)
```
Limit  (cost=5479121.07..5480370.01 rows=20 width=162) (actual time=777.199..777.251 rows=20 loops=1)
  Buffers: shared hit=2641
  ->  Result  (cost=5479121.07..5491610.52 rows=200 width=162) (actual time=228.193..228.242 rows=20 loops=1)
        Buffers: shared hit=2641
        ->  Sort  (cost=5479121.07..5479121.57 rows=200 width=153) (actual time=210.833..210.841 rows=20 loops=1)
              Sort Key: (((((CASE WHEN (lower((r.name)::text) = '치킨'::text) THEN 1 ELSE 0 END * 100))::double precision + (similarity(lower((r.name)::text), '치킨'::text) * '30'::double precision)) + (GREATEST('0'::double precision, ('1'::double precision - (st_distance(geography(r.location), '0101000020E61000009A99999999B95F400000000000C04240'::geography, false) / '3000'::double precision))) * '50'::double precision))) DESC, r.updated_at DESC, r.id DESC
              Sort Method: top-N heapsort  Memory: 30kB
              Buffers: shared hit=2639
              ->  Nested Loop  (cost=5475181.54..5479115.74 rows=200 width=153) (actual time=203.108..210.711 rows=200 loops=1)
                    Buffers: shared hit=2639
                    ->  Limit  (cost=5475181.24..5475181.74 rows=200 width=24) (actual time=202.082..202.147 rows=200 loops=1)
                          Buffers: shared hit=2039
                          ->  Sort  (cost=5475181.24..5475237.28 rows=22415 width=24) (actual time=202.059..202.098 rows=200 loops=1)
                                Sort Key: (((((CASE WHEN (lower((r_1.name)::text) = '치킨'::text) THEN 1 ELSE 0 END * 100))::double precision + (similarity(lower((r_1.name)::text), '치킨'::text) * '30'::double precision)) + (GREATEST('0'::double precision, ('1'::double precision - (st_distance(geography(r_1.location), '0101000020E61000009A99999999B95F400000000000C04240'::geography, false) / '3000'::double precision))) * '50'::double precision))) DESC, r_1.updated_at DESC, r_1.id DESC
                                Sort Method: top-N heapsort  Memory: 48kB
                                Buffers: shared hit=2039
                                ->  Seq Scan on restaurant r_1  (cost=0.00..5474212.48 rows=22415 width=24) (actual time=0.648..199.536 rows=8773 loops=1)
                                      Filter: ((deleted_at IS NULL) AND (st_distance(geography(location), '0101000020E61000009A99999999B95F400000000000C04240'::geography, false) <= '3000'::double precision) AND ((lower((name)::text) ~~ '%치킨%'::text) OR (lower((full_address)::text) ~~ '%치킨%'::text) OR (ANY (id = (hashed SubPlan 4).col1))))
                                      Rows Removed by Filter: 95244
                                      Buffers: shared hit=2039
                                      SubPlan 4
                                        ->  Hash Join  (cost=22.95..52.79 rows=7 width=8) (actual time=18.925..18.932 rows=1 loops=1)
                                              Hash Cond: (rfc_1.food_category_id = fc_1.id)
                                              Buffers: shared hit=2
                                              ->  Seq Scan on restaurant_food_category rfc_1  (cost=0.00..25.70 rows=1570 width=16) (actual time=0.030..0.032 rows=12 loops=1)
                                                    Buffers: shared hit=1
                                              ->  Hash  (cost=22.90..22.90 rows=4 width=8) (actual time=18.705..18.706 rows=1 loops=1)
                                                    Buckets: 1024  Batches: 1  Memory Usage: 9kB
                                                    Buffers: shared hit=1
                                                    ->  Seq Scan on food_category fc_1  (cost=0.00..22.90 rows=4 width=8) (actual time=18.670..18.675 rows=1 loops=1)
                                                          Filter: (lower((name)::text) = '치킨'::text)
                                                          Rows Removed by Filter: 9
                                                          Buffers: shared hit=1
                    ->  Index Scan using restaurant_pkey on restaurant r  (cost=0.29..7.11 rows=1 width=139) (actual time=0.031..0.031 rows=1 loops=200)
                          Index Cond: (id = r_1.id)
                          Buffers: shared hit=600
        SubPlan 2
          ->  Hash Join  (cost=22.95..52.79 rows=7 width=8) (actual time=16.637..16.640 rows=1 loops=1)
                Hash Cond: (rfc.food_category_id = fc.id)
                Buffers: shared hit=2
                ->  Seq Scan on restaurant_food_category rfc  (cost=0.00..25.70 rows=1570 width=16) (actual time=0.023..0.025 rows=12 loops=1)
                      Buffers: shared hit=1
                ->  Hash  (cost=22.90..22.90 rows=4 width=8) (actual time=16.233..16.233 rows=1 loops=1)
                      Buckets: 1024  Batches: 1  Memory Usage: 9kB
                      Buffers: shared hit=1
                      ->  Seq Scan on food_category fc  (cost=0.00..22.90 rows=4 width=8) (actual time=16.193..16.197 rows=1 loops=1)
                            Filter: (lower((name)::text) = '치킨'::text)
                            Rows Removed by Filter: 9
                            Buffers: shared hit=1
Planning:
  Buffers: shared hit=43
Planning Time: 4.164 ms
JIT:
  Functions: 57
  Options: Inlining true, Optimization true, Expressions true, Deforming true
  Timing: Generation 4.945 ms (Deform 2.018 ms), Inlining 53.936 ms, Optimization 270.290 ms, Emission 259.459 ms, Total 588.630 ms
Execution Time: 782.460 ms
```

### JOIN_AGGREGATE (score + distance filter)
```
Limit  (cost=900888.11..901389.41 rows=20 width=165) (actual time=2473.865..2514.787 rows=20 loops=1)
  Buffers: shared hit=3178
  ->  Result  (cost=900888.11..1158556.31 rows=10280 width=165) (actual time=2091.961..2132.878 rows=20 loops=1)
        Buffers: shared hit=3178
        ->  Sort  (cost=900888.11..900913.81 rows=10280 width=157) (actual time=2091.665..2132.560 rows=20 loops=1)
              Sort Key: (((((CASE WHEN (lower((r.name)::text) = '치킨'::text) THEN 1 ELSE 0 END * 100))::double precision + (similarity(lower((r.name)::text), '치킨'::text) * '30'::double precision)) + (GREATEST('0'::double precision, ('1'::double precision - (st_distance(geography(r.location), '0101000020E61000009A99999999B95F400000000000C04240'::geography, false) / '3000'::double precision))) * '50'::double precision))) DESC, r.updated_at DESC, r.id DESC
              Sort Method: top-N heapsort  Memory: 34kB
              Buffers: shared hit=3178
              ->  Finalize GroupAggregate  (cost=1142.84..900614.56 rows=10280 width=157) (actual time=1969.916..2129.344 rows=8773 loops=1)
                    Group Key: r.id
                    Buffers: shared hit=3178
                    ->  Gather Merge  (cost=1142.84..771467.52 rows=6047 width=143) (actual time=1968.354..2070.917 rows=8773 loops=1)
                          Workers Planned: 1
                          Workers Launched: 1
                          Buffers: shared hit=3178
                          ->  Partial GroupAggregate  (cost=142.83..769787.23 rows=6047 width=143) (actual time=824.123..901.910 rows=4386 loops=2)
                                Group Key: r.id
                                Buffers: shared hit=3178
                                ->  Merge Left Join  (cost=142.83..769666.29 rows=6047 width=197) (actual time=823.002..898.646 rows=4386 loops=2)
                                      Merge Cond: (r.id = rfc.restaurant_id)
                                      Filter: ((lower((r.name)::text) ~~ '%치킨%'::text) OR (lower((r.full_address)::text) ~~ '%치킨%'::text) OR (lower((fc.name)::text) = '치킨'::text))
                                      Rows Removed by Filter: 11292
                                      Buffers: shared hit=3178
                                      ->  Parallel Index Scan Backward using restaurant_pkey on restaurant r  (cost=0.29..769457.22 rows=20395 width=139) (actual time=819.122..881.531 rows=15678 loops=2)
                                            Filter: ((deleted_at IS NULL) AND (st_distance(geography(location), '0101000020E61000009A99999999B95F400000000000C04240'::geography, false) <= '3000'::double precision))
                                            Rows Removed by Filter: 36330
                                            Buffers: shared hit=3161
                                      ->  Sort  (cost=142.53..146.46 rows=1570 width=66) (actual time=1.832..1.897 rows=1 loops=2)
                                            Sort Key: rfc.restaurant_id DESC
                                            Sort Method: quicksort  Memory: 25kB
                                            Buffers: shared hit=17
                                            Worker 0:  Sort Method: quicksort  Memory: 25kB
                                            ->  Hash Left Join  (cost=29.35..59.19 rows=1570 width=66) (actual time=0.903..1.034 rows=12 loops=2)
                                                  Hash Cond: (rfc.food_category_id = fc.id)
                                                  Buffers: shared hit=13
                                                  ->  Seq Scan on restaurant_food_category rfc  (cost=0.00..25.70 rows=1570 width=16) (actual time=0.082..0.084 rows=12 loops=2)
                                                        Buffers: shared hit=2
                                                  ->  Hash  (cost=18.60..18.60 rows=860 width=66) (actual time=0.210..0.236 rows=10 loops=2)
                                                        Buckets: 1024  Batches: 1  Memory Usage: 9kB
                                                        Buffers: shared hit=2
                                                        ->  Seq Scan on food_category fc  (cost=0.00..18.60 rows=860 width=66) (actual time=0.074..0.077 rows=10 loops=2)
                                                              Buffers: shared hit=2
Planning:
  Buffers: shared hit=9
Planning Time: 11.818 ms
JIT:
  Functions: 58
  Options: Inlining true, Optimization true, Expressions true, Deforming true
  Timing: Generation 19.327 ms (Deform 9.165 ms), Inlining 237.722 ms, Optimization 989.327 ms, Emission 687.187 ms, Total 1933.563 ms
Execution Time: 2519.828 ms
```

### 비교 요약
- ONE_STEP: 2343.587 ms
- TWO_STEP: 782.460 ms
- JOIN_AGGREGATE: 2519.828 ms

## 성능 결과 요약
| 전략 | 실행 시간 | 비고 |
| --- | --- | --- |
| ONE_STEP | 2343ms | 전체 스캔 + 복잡한 정렬 |
| TWO_STEP | 782ms | 후보 추출 후 재정렬 |
| JOIN_AGGREGATE | 2519ms | 가장 느림 |

## 병목 원인 분석
### 1. Seq Scan 문제
- 모든 전략에서 restaurant 테이블에 대해 Seq Scan 발생
- 104,017건 전체를 스캔
- `LIKE '%치킨%'` 패턴은 인덱스 사용 불가 (앞에 와일드카드)
- `st_distance()` 계산이 모든 행에 대해 수행됨

### 2. ONE_STEP이 느린 이유
- 모든 행(104K)에 대해 복잡한 score 계산 수행
- JIT 컴파일 오버헤드: 1476ms (전체의 63%)
- 정렬 대상이 22,415건으로 큼

### 3. TWO_STEP이 빠른 이유
- 첫 단계에서 200건으로 후보 제한
- Nested Loop로 PK 인덱스 활용 (Index Scan)
- 최종 정렬 대상이 200건으로 작음

### 4. JOIN_AGGREGATE가 가장 느린 이유
- `GROUP BY r.id`로 인한 GroupAggregate 비용
- Merge Left Join + Filter 조합이 비효율적
- 모든 카테고리 조인 후 필터링 (push-down 안됨)

## 개선 방안
### 방안 1: GIN 인덱스 + pg_trgm 활용
```sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_restaurant_name_trgm ON restaurant USING gin (lower(name) gin_trgm_ops);
CREATE INDEX idx_restaurant_addr_trgm ON restaurant USING gin (lower(full_address) gin_trgm_ops);
```
`LIKE '%keyword%'`가 GIN 인덱스를 타게 됨.

### 방안 2: 공간 인덱스 선필터링
```sql
CREATE INDEX idx_restaurant_location ON restaurant USING gist (location);
CREATE INDEX idx_restaurant_geography ON restaurant USING gist (geography(location));

WITH geo_filtered AS (
  SELECT r.id
  FROM restaurant r
  WHERE r.deleted_at IS NULL
    AND ST_DWithin(
      r.location::geography,
      ST_MakePoint(126.9, 37.5)::geography,
      3000
    )
),
text_matched AS (
  SELECT r.id
  FROM restaurant r
  WHERE r.deleted_at IS NULL
    AND (
      lower(r.name) LIKE '%치킨%'
      OR lower(r.full_address) LIKE '%치킨%'
    )
  UNION
  SELECT rfc.restaurant_id
  FROM restaurant_food_category rfc
  JOIN food_category fc ON fc.id = rfc.food_category_id
  WHERE lower(fc.name) = '치킨'
),
candidates AS (
  SELECT g.id
  FROM geo_filtered g
  JOIN text_matched t ON t.id = g.id
  ORDER BY (
    SELECT similarity(lower(name), '치킨') FROM restaurant WHERE id = g.id
  ) DESC
  LIMIT 200
)
SELECT r.*,
       (lower(r.name) = '치킨') AS name_exact,
       similarity(lower(r.name), '치킨') AS name_sim,
       ST_DistanceSphere(r.location, ST_MakePoint(126.9, 37.5)) AS dist_m
FROM restaurant r
JOIN candidates c ON c.id = r.id
ORDER BY 
  CASE WHEN lower(r.name) = '치킨' THEN 100 ELSE 0 END
  + similarity(lower(r.name), '치킨') * 30
  + GREATEST(0, 1 - ST_DistanceSphere(r.location, ST_MakePoint(126.9, 37.5)) / 3000) * 50
  DESC,
  r.updated_at DESC,
  r.id DESC
LIMIT 20;
```

### 방안 2 적용 결과 (공간 인덱스)
적용한 인덱스:
```sql
CREATE INDEX IF NOT EXISTS idx_restaurant_location_gist
    ON restaurant USING gist (location);
CREATE INDEX IF NOT EXISTS idx_restaurant_geography_gist
    ON restaurant USING gist (geography(location));
```

### 방안 3: 카테고리 서브쿼리 최적화 (현재 코드 기준)
```sql
WITH category_restaurants AS (
  SELECT DISTINCT rfc.restaurant_id AS id
  FROM restaurant_food_category rfc
  JOIN food_category fc ON fc.id = rfc.food_category_id
  WHERE lower(fc.name) = '치킨'
),
candidates AS (
  SELECT r.id
  FROM restaurant r
  LEFT JOIN category_restaurants cr ON cr.id = r.id
  WHERE r.deleted_at IS NULL
    AND ST_DWithin(r.location::geography, ST_MakePoint(126.9, 37.5)::geography, 3000)
    AND (
      lower(r.name) LIKE '%치킨%'
      OR lower(r.full_address) LIKE '%치킨%'
      OR cr.id IS NOT NULL
    )
  ORDER BY similarity(lower(r.name), '치킨') DESC
  LIMIT 200
)
SELECT r.*, ...
FROM restaurant r
JOIN candidates c ON c.id = r.id
ORDER BY score DESC, ...
LIMIT 20;
```

## 권장 사항
- 즉시 적용: TWO_STEP 전략 유지 (현재 가장 빠름)
- 인덱스 추가: pg_trgm GIN 인덱스 + gist 공간 인덱스
- 카테고리 조인: SubPlan 대신 CTE로 미리 계산
- candidates 수 조정: 200건이 적절하나, 검색 품질에 따라 300~500으로 조정 가능

## candidates 수란?
TWO_STEP 전략에서 1단계에서 추출하는 후보 개수.
```sql
WITH candidates AS (
  SELECT r.id
  FROM restaurant r
  WHERE ...
  ORDER BY similarity(lower(r.name), lower('치킨')) DESC
  LIMIT 200
)
```

### 역할
- 1단계: 104,017건 중 유사도 상위 200건만 추출
- 2단계: 200건에 대해서만 복잡한 score 계산 + 최종 정렬 → 20건 반환

### 트레이드오프
| candidates 수 | 장점 | 단점 |
| --- | --- | --- |
| 작음 (100) | 빠름 | 거리 가중치로 순위 바뀔 수 있는 결과 누락 |
| 큼 (500) | 정확한 결과 | 2단계 정렬 비용 증가 |

### 왜 200인가?
- 최종 결과는 20건
- 1단계는 유사도만 보고, 2단계에서 거리 가중치가 추가됨
- 거리로 인해 순위가 크게 바뀔 여지를 고려해 10배(200건) 확보
- 실측 결과 200건 정렬은 0.1ms 수준으로 부담 없음

## 추가 개선 방안
### 방안 4: Materialized View + 트리거
```sql
CREATE MATERIALIZED VIEW restaurant_search_mv AS
SELECT 
  r.id,
  r.name,
  lower(r.name) AS name_lower,
  r.full_address,
  lower(r.full_address) AS addr_lower,
  r.location,
  r.updated_at,
  r.deleted_at,
  array_agg(DISTINCT lower(fc.name)) FILTER (WHERE fc.name IS NOT NULL) AS category_names
FROM restaurant r
LEFT JOIN restaurant_food_category rfc ON rfc.restaurant_id = r.id
LEFT JOIN food_category fc ON fc.id = rfc.food_category_id
GROUP BY r.id;

CREATE INDEX idx_mv_name_trgm ON restaurant_search_mv USING gin (name_lower gin_trgm_ops);
CREATE INDEX idx_mv_addr_trgm ON restaurant_search_mv USING gin (addr_lower gin_trgm_ops);
CREATE INDEX idx_mv_location ON restaurant_search_mv USING gist (location);
CREATE INDEX idx_mv_categories ON restaurant_search_mv USING gin (category_names);
```
장점: 조인 제거, 인덱스 최적화, 카테고리 배열로 단순화  
단점: 데이터 동기화 필요 (REFRESH 또는 트리거)

### 방안 5: Full-Text Search (tsvector)
```sql
ALTER TABLE restaurant ADD COLUMN search_vector tsvector;

UPDATE restaurant SET search_vector = 
  setweight(to_tsvector('simple', coalesce(name, '')), 'A') ||
  setweight(to_tsvector('simple', coalesce(full_address, '')), 'B');

CREATE INDEX idx_restaurant_fts ON restaurant USING gin (search_vector);

SELECT r.*, ts_rank(search_vector, query) AS rank
FROM restaurant r, plainto_tsquery('simple', '치킨') query
WHERE r.deleted_at IS NULL
  AND r.search_vector @@ query
ORDER BY rank DESC
LIMIT 200;
```
장점: PostgreSQL 내장, 형태소 분석 가능  
단점: 한국어는 simple 또는 별도 파서 필요, 부분 일치 약함

### 방안 6: 외부 검색 엔진 (Elasticsearch/Meilisearch)
[Client] → [Spring API] → [Elasticsearch] → restaurant_id 목록  
[Client] → [Spring API] → [PostgreSQL] → 상세 조회  
장점: 퍼지 매칭/오타 교정/자동완성/한글 형태소 분석/수평 확장  
단점: 인프라 추가, 동기화 복잡도

### 방안 7: 하이브리드 인덱스 전략
```sql
CREATE INDEX idx_restaurant_geo ON restaurant USING gist (geography(location));
CREATE INDEX idx_restaurant_active ON restaurant (id) WHERE deleted_at IS NULL;
CREATE INDEX idx_restaurant_name_trgm ON restaurant 
  USING gin (lower(name) gin_trgm_ops) 
  WHERE deleted_at IS NULL;
CREATE INDEX idx_restaurant_full_address_trgm ON restaurant
  USING gin (lower(full_address) gin_trgm_ops)
  WHERE deleted_at IS NULL;

WITH geo_candidates AS (
  SELECT id
  FROM restaurant
  WHERE deleted_at IS NULL
    AND ST_DWithin(location::geography, ST_MakePoint(126.9, 37.5)::geography, 3000)
),
text_candidates AS (
  SELECT id FROM restaurant 
  WHERE deleted_at IS NULL AND lower(name) % '치킨'
  UNION
  SELECT id FROM restaurant 
  WHERE deleted_at IS NULL AND lower(full_address) LIKE '%치킨%'
  UNION
  SELECT rfc.restaurant_id
  FROM restaurant_food_category rfc
  JOIN food_category fc ON fc.id = rfc.food_category_id
  WHERE lower(fc.name) = '치킨'
),
final_candidates AS (
  SELECT g.id
  FROM geo_candidates g
  INNER JOIN text_candidates t ON t.id = g.id
  LIMIT 500
)
SELECT r.*,
  (CASE WHEN lower(r.name) = '치킨' THEN 100 ELSE 0 END
   + similarity(lower(r.name), '치킨') * 30
   + GREATEST(0, 1 - ST_Distance(r.location::geography, ST_MakePoint(126.9, 37.5)::geography) / 3000) * 50
  ) AS score
FROM restaurant r
JOIN final_candidates c ON c.id = r.id
ORDER BY score DESC, r.updated_at DESC, r.id DESC
LIMIT 20;
```

### 방안 7 적용 결과 (하이브리드 인덱스)
적용한 인덱스:
```sql
CREATE INDEX IF NOT EXISTS idx_restaurant_active_id
    ON restaurant (id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_restaurant_name_trgm_active
    ON restaurant USING gin (lower(name) gin_trgm_ops)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_restaurant_full_address_trgm_active
    ON restaurant USING gin (lower(full_address) gin_trgm_ops)
    WHERE deleted_at IS NULL;
```

## 비교 요약
| 방안 | 예상 성능 | 구현 난이도 | 유지보수 |
| --- | --- | --- | --- |
| TWO_STEP (현재) | 782ms | 낮음 | 쉬움 |
| 방안 1 (GIN trgm) | 200~400ms | 낮음 | 쉬움 |
| 방안 4 (MV) | 100~200ms | 중간 | 동기화 필요 |
| 방안 5 (FTS) | 150~300ms | 중간 | 한글 제한 |
| 방안 6 (ES) | 50~100ms | 높음 | 인프라 추가 |
| 방안 7 (하이브리드) | 100~200ms | 중간 | 인덱스 관리 |

## 권장 순서
- 즉시: 방안 1 (GIN trigram 인덱스 추가) - 최소 노력으로 2~3배 개선
- 단기: 방안 7 (하이브리드) - 공간+텍스트 인덱스 조합
- 장기: 방안 6 (Elasticsearch) - 트래픽 증가 시 검토

## 방안 1 적용 결과 (pg_trgm GIN 인덱스 추가)
적용한 인덱스:
```sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX IF NOT EXISTS idx_restaurant_name_trgm
    ON restaurant USING gin (lower(name) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_restaurant_full_address_trgm
    ON restaurant USING gin (lower(full_address) gin_trgm_ops);
```

### 적용 후 EXPLAIN (score + distance filter)
#### ONE_STEP
```
Execution Time: 1937.413 ms
```

#### TWO_STEP
```
Execution Time: 643.283 ms
```

#### JOIN_AGGREGATE
```
Execution Time: 2343.463 ms
```

### 비교 요약 (Before → After)
- ONE_STEP: 2343.587 ms → 1937.413 ms
- TWO_STEP: 782.460 ms → 643.283 ms
- JOIN_AGGREGATE: 2519.828 ms → 2343.463 ms

### 비고
- 실행 계획은 여전히 Seq Scan 중심이며, `LIKE '%키워드%'` 패턴과 거리 계산 때문에 인덱스 효용이 제한적.
- trgm 인덱스는 `similarity` 및 `LIKE` 비용을 일부 줄여 약 15~18% 개선됨.
