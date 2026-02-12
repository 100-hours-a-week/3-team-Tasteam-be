# 검색 쿼리 전략 보고서 - 2026-02-08

## 1. 검색 목적
사용자가 입력한 키워드를 기준으로 음식점 이름을 검색하고, 현재 위치(위도/경도)가 주어지면 일정 거리 반경 내에서 **가까운 + 관련도 높은** 결과를 상단에 노출한다.

## 2. 요구 조건 (데이터/필터/정렬)
### 입력 값
- 키워드: `keyword`
- 현재 위치: `latitude`, `longitude` (없을 수 있음)
- 반경: `radiusKm` (기본 3km)

### 필터 조건 (WHERE)
- `deleted_at IS NULL`
- 이름 조건: `name ILIKE %keyword%` **또는** `name % keyword` (pg_trgm 유사도)
- 거리 조건: 위치가 있는 경우 `distance <= radius`

### 정렬 조건 (ORDER BY)
점수 합산 + 타이브레이커
- 이름 완전 일치: `+100`
- 이름 유사도: `similarity * 30`
- 거리 가중치: `max(0, 1 - distance / radius) * 50`
정렬 우선순위:
1. `score DESC`
2. `updated_at DESC`
3. `id DESC`

## 3. 노멀(무식한) 쿼리
최적화/전략 없이 단일 쿼리로 필터 + 정렬 수행.
```sql
SELECT r.*,
  (CASE WHEN lower(r.name) = '치킨' THEN 1 ELSE 0 END) AS name_exact,
  similarity(lower(r.name), '치킨') AS name_sim,
  ST_DistanceSphere(r.location, ST_MakePoint(126.9, 37.5)) AS dist_m
FROM restaurant r
WHERE r.deleted_at IS NULL
  AND (lower(r.name) LIKE '%치킨%' OR lower(r.name) % '치킨')
  AND ST_DistanceSphere(r.location, ST_MakePoint(126.9, 37.5)) <= 3000
ORDER BY (
  (CASE WHEN lower(r.name) = '치킨' THEN 1 ELSE 0 END) * 100
  + similarity(lower(r.name), '치킨') * 30
  + GREATEST(0, 1 - ST_DistanceSphere(r.location, ST_MakePoint(126.9, 37.5)) / 3000.0) * 50
) DESC,
 r.updated_at DESC,
 r.id DESC
LIMIT 20;
```

## 4. 전략별 쿼리
### 전략 개요 (어떤 전략으로 쿼리를 구성했는가)
- 노멀: 단일 SELECT에 필터/정렬을 모두 넣어 한 번에 계산한다. 별도 후보 제한이나 조인 전략 없이 가장 직관적인 형태.
- JOIN 방식: 카테고리 정보를 `LEFT JOIN + GROUP BY`로 합쳐서 한 번에 집계한다. 카테고리는 부가 정보로 남기되, 그룹화 비용을 감수한다.
- 서브쿼리 방식: 카테고리 매칭을 `EXISTS`로 분리해 필요 시점에만 검사한다. 메인 쿼리 흐름은 단순하게 유지한다.
- 후보군 방식: 1단계에서 상위 200건을 먼저 뽑고, 2단계에서 최종 정렬한다. 정렬 대상 축소가 핵심 목표다.

### 전략 1: JOIN 방식
```sql
SELECT r.*,
  (CASE WHEN lower(r.name) = '치킨' THEN 1 ELSE 0 END) AS name_exact,
  similarity(lower(r.name), '치킨') AS name_sim,
  ST_DistanceSphere(r.location, ST_MakePoint(126.9, 37.5)) AS dist_m,
  MAX(CASE WHEN lower(fc.name) = '치킨' THEN 1 ELSE 0 END) AS category_match
FROM restaurant r
LEFT JOIN restaurant_food_category rfc ON rfc.restaurant_id = r.id
LEFT JOIN food_category fc ON fc.id = rfc.food_category_id
WHERE r.deleted_at IS NULL
  AND (lower(r.name) LIKE '%치킨%' OR lower(r.name) % '치킨')
  AND ST_DistanceSphere(r.location, ST_MakePoint(126.9, 37.5)) <= 3000
GROUP BY r.id
ORDER BY (
  (CASE WHEN lower(r.name) = '치킨' THEN 1 ELSE 0 END) * 100
  + similarity(lower(r.name), '치킨') * 30
  + GREATEST(0, 1 - ST_DistanceSphere(r.location, ST_MakePoint(126.9, 37.5)) / 3000.0) * 50
) DESC,
 r.updated_at DESC,
 r.id DESC
LIMIT 20;
```

### 전략 2: 서브쿼리 방식
```sql
SELECT r.*,
  (CASE WHEN lower(r.name) = '치킨' THEN 1 ELSE 0 END) AS name_exact,
  similarity(lower(r.name), '치킨') AS name_sim,
  ST_DistanceSphere(r.location, ST_MakePoint(126.9, 37.5)) AS dist_m,
  EXISTS (
    SELECT 1
    FROM restaurant_food_category rfc
    JOIN food_category fc ON fc.id = rfc.food_category_id
    WHERE rfc.restaurant_id = r.id AND lower(fc.name) = '치킨'
  ) AS category_match
FROM restaurant r
WHERE r.deleted_at IS NULL
  AND (lower(r.name) LIKE '%치킨%' OR lower(r.name) % '치킨')
  AND ST_DistanceSphere(r.location, ST_MakePoint(126.9, 37.5)) <= 3000
ORDER BY (
  (CASE WHEN lower(r.name) = '치킨' THEN 1 ELSE 0 END) * 100
  + similarity(lower(r.name), '치킨') * 30
  + GREATEST(0, 1 - ST_DistanceSphere(r.location, ST_MakePoint(126.9, 37.5)) / 3000.0) * 50
) DESC,
 r.updated_at DESC,
 r.id DESC
LIMIT 20;
```

### 전략 3: 후보군 방식 (Candidate Pool)
```sql
WITH candidates AS (
  SELECT r.id
  FROM restaurant r
  WHERE r.deleted_at IS NULL
    AND (lower(r.name) LIKE '%치킨%' OR lower(r.name) % '치킨')
    AND ST_DistanceSphere(r.location, ST_MakePoint(126.9, 37.5)) <= 3000
  ORDER BY (
    (CASE WHEN lower(r.name) = '치킨' THEN 1 ELSE 0 END) * 100
    + similarity(lower(r.name), '치킨') * 30
    + GREATEST(0, 1 - ST_DistanceSphere(r.location, ST_MakePoint(126.9, 37.5)) / 3000.0) * 50
  ) DESC,
   r.updated_at DESC,
   r.id DESC
  LIMIT 200
)
SELECT r.*,
  (CASE WHEN lower(r.name) = '치킨' THEN 1 ELSE 0 END) AS name_exact,
  similarity(lower(r.name), '치킨') AS name_sim,
  ST_DistanceSphere(r.location, ST_MakePoint(126.9, 37.5)) AS dist_m
FROM restaurant r
JOIN candidates c ON c.id = r.id
ORDER BY (
  (CASE WHEN lower(r.name) = '치킨' THEN 1 ELSE 0 END) * 100
  + similarity(lower(r.name), '치킨') * 30
  + GREATEST(0, 1 - ST_DistanceSphere(r.location, ST_MakePoint(126.9, 37.5)) / 3000.0) * 50
) DESC,
 r.updated_at DESC,
 r.id DESC
LIMIT 20;
```

## 5. 실행 계획 및 분석 (인덱스 적용 후)
### 노멀 쿼리 EXPLAIN (요약)
- 실행 시간: **1009.833 ms**
- 핵심 노드: `Parallel Bitmap Heap Scan` → `Sort (top-N heapsort)`
- 정렬은 최종 단계에서 수행, 거리/유사도 계산이 필터링 이후에도 다수 수행됨

### JOIN 방식 EXPLAIN (요약)
- 실행 시간: **975.326 ms**
- 핵심 노드: `Parallel Bitmap Heap Scan` + `GroupAggregate`
- `GROUP BY r.id` 비용이 추가됨

### 서브쿼리 방식 EXPLAIN (요약)
- 실행 시간: **484.969 ms**
- 핵심 노드: `Parallel Bitmap Heap Scan`
- 카테고리 서브플랜은 조회 결과 수가 적어 상대적으로 부담 낮음

### 후보군 방식 EXPLAIN (요약)
- 실행 시간: **500.287 ms**
- 핵심 노드: `Parallel Bitmap Heap Scan` → 후보 200건 제한 → `Nested Loop`
- 정렬 대상이 200건으로 축소됨

## 6. 추가 개선 작업 (인덱스) 및 비교
### 적용한 인덱스
```sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX IF NOT EXISTS idx_restaurant_name_trgm_active
    ON restaurant USING gin (lower(name) gin_trgm_ops)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_restaurant_location_gist
    ON restaurant USING gist (location);
CREATE INDEX IF NOT EXISTS idx_restaurant_geography_gist
    ON restaurant USING gist (geography(location));
```

### 인덱스 효과 비교
아래 “미적용”은 `enable_indexscan=off`, `enable_bitmapscan=off`로 **인덱스 사용을 강제 차단**한 측정값.

| 전략 | 인덱스 미적용 | 인덱스 적용 | 개선폭 |
| --- | --- | --- | --- |
| 노멀 | 2810.458 ms | 1009.833 ms | **-64%** |
| JOIN | 1679.508 ms | 975.326 ms | **-42%** |
| 서브쿼리 | 1750.810 ms | 484.969 ms | **-72%** |
| 후보군 방식 | 1875.216 ms | 500.287 ms | **-73%** |

## 7. 최종 결론
- **서브쿼리 / 후보군 방식이 가장 안정적으로 빠름**  
  (현재 데이터셋에서 약 0.48~0.50초)
- **JOIN + GroupAggregate는 비용이 크며 추천하지 않음**
- **인덱스 적용 효과가 매우 큼**  
  특히 이름 유사도/부분 일치 조건에 trgm 인덱스가 유의미하게 동작함

다음 단계로는 후보군 수 조정(200→300/500)과 거리 반경 변화에 따른 품질/성능 균형을 측정하면 좋다.

## EXPLAIN 원문 (인덱스 적용)
### 노멀 쿼리
```
Limit  (cost=314158.46..314410.81 rows=20 width=171) (actual time=747.237..766.456 rows=20 loops=1)
  Buffers: shared hit=2904
  ->  Gather Merge  (cost=314158.46..389548.02 rows=5975 width=171) (actual time=517.271..536.446 rows=20 loops=1)
        Workers Planned: 1
        Workers Launched: 1
        Buffers: shared hit=2904
        ->  Result  (cost=313158.45..462921.82 rows=5975 width=171) (actual time=388.617..388.641 rows=13 loops=2)
              Buffers: shared hit=2904
              ->  Sort  (cost=313158.45..313173.39 rows=5975 width=163) (actual time=388.322..388.331 rows=13 loops=2)
                    Sort Key: (((((CASE WHEN (lower((name)::text) = '치킨'::text) THEN 1 ELSE 0 END * 100))::double precision + (similarity(lower((name)::text), '치킨'::text) * '30'::double precision)) + (GREATEST('0'::double precision, ('1'::double precision - (st_distance(geography(location), '0101000020E61000009A99999999B95F400000000000C04240'::geography, false) / '3000'::double precision))) * '50'::double precision))) DESC, updated_at DESC, id DESC
                    Sort Method: top-N heapsort  Memory: 34kB
                    Buffers: shared hit=2904
                    Worker 0:  Sort Method: quicksort  Memory: 26kB
                    ->  Parallel Bitmap Heap Scan on restaurant r  (cost=11447.16..312999.46 rows=5975 width=163) (actual time=216.361..385.832 rows=4386 loops=2)
                          Recheck Cond: (((lower((name)::text) ~~ '%치킨%'::text) AND (deleted_at IS NULL)) OR ((lower((name)::text) % '치킨'::text) AND (deleted_at IS NULL)))
                          Rows Removed by Index Recheck: 36728
                          Filter: (((lower((name)::text) ~~ '%치킨%'::text) OR (lower((name)::text) % '치킨'::text)) AND (st_distance(geography(location), '0101000020E61000009A99999999B95F400000000000C04240'::geography, false) <= '3000'::double precision))
                          Rows Removed by Filter: 10894
                          Heap Blocks: exact=2036
                          Buffers: shared hit=2882
                          ->  BitmapOr  (cost=11447.16..11447.16 rows=30475 width=0) (actual time=36.579..36.581 rows=0 loops=1)
                                Buffers: shared hit=296
                                ->  Bitmap Index Scan on idx_restaurant_name_trgm_active  (cost=0.00..11411.86 rows=30465 width=0) (actual time=33.424..33.424 rows=104017 loops=1)
                                      Index Cond: (lower((name)::text) ~~ '%치킨%'::text)
                                      Buffers: shared hit=277
                                ->  Bitmap Index Scan on idx_restaurant_name_trgm_active  (cost=0.00..30.22 rows=10 width=0) (actual time=3.101..3.101 rows=30560 loops=1)
                                      Index Cond: (lower((name)::text) % '치킨'::text)
                                      Buffers: shared hit=19
Planning:
  Buffers: shared hit=305
Planning Time: 103.971 ms
JIT:
  Functions: 18
  Options: Inlining false, Optimization false, Expressions true, Deforming true
  Timing: Generation 23.970 ms (Deform 10.095 ms), Inlining 0.000 ms, Optimization 40.318 ms, Emission 376.800 ms, Total 441.089 ms
Execution Time: 1009.833 ms
```
**해석**
- **인덱스 사용:** `idx_restaurant_name_trgm_active`가 `Bitmap Index Scan`으로 사용됨.
- **풀스캔 여부:** 풀스캔이 아니라 `Bitmap Heap Scan`으로 후보만 읽음.
- **정렬 지점:** `Sort (top-N heapsort)`에서 최종 정렬 수행.
- **조인:** 없음.
- **메모리:** 정렬 메모리 34kB 수준.
- **시간 병목:** 후보 집합 생성 + 거리 계산 + 정렬.

### 전략 1: JOIN
```
Limit  (cost=370582.55..371083.75 rows=20 width=167) (actual time=773.929..774.702 rows=20 loops=1)
  Buffers: shared hit=2900
  ->  Result  (cost=370582.55..625116.97 rows=10157 width=167) (actual time=562.720..563.452 rows=20 loops=1)
        Buffers: shared hit=2900
        ->  Sort  (cost=370582.55..370607.94 rows=10157 width=159) (actual time=562.429..563.140 rows=20 loops=1)
              Sort Key: (((((CASE WHEN (lower((r.name)::text) = '치킨'::text) THEN 1 ELSE 0 END * 100))::double precision + (similarity(lower((r.name)::text), '치킨'::text) * '30'::double precision)) + (GREATEST('0'::double precision, ('1'::double precision - (st_distance(geography(r.location), '0101000020E61000009A99999999B95F400000000000C04240'::geography, false) / '3000'::double precision))) * '50'::double precision))) DESC, r.updated_at DESC, r.id DESC
              Sort Method: top-N heapsort  Memory: 34kB
              Buffers: shared hit=2900
              ->  GroupAggregate  (cost=239497.18..370312.28 rows=10157 width=159) (actual time=501.701..560.210 rows=8773 loops=1)
                    Group Key: r.id
                    Buffers: shared hit=2894
                    ->  Nested Loop Left Join  (cost=239497.18..242689.57 rows=10157 width=197) (actual time=500.316..507.507 rows=8773 loops=1)
                          Buffers: shared hit=2894
                          ->  Gather Merge  (cost=239497.03..240678.32 rows=10157 width=147) (actual time=499.981..504.097 rows=8773 loops=1)
                                Workers Planned: 1
                                Workers Launched: 1
                                Buffers: shared hit=2894
                                ->  Merge Left Join  (cost=238497.02..238535.65 rows=5975 width=147) (actual time=314.475..315.692 rows=4386 loops=2)
                                      Merge Cond: (r.id = rfc.restaurant_id)
                                      Buffers: shared hit=2894
                                      ->  Sort  (cost=238387.98..238402.92 rows=5975 width=139) (actual time=313.791..314.075 rows=4386 loops=2)
                                            Sort Key: r.id DESC
                                            Sort Method: quicksort  Memory: 1686kB
                                            Buffers: shared hit=2892
                                            Worker 0:  Sort Method: quicksort  Memory: 25kB
                                            ->  Parallel Bitmap Heap Scan on restaurant r  (cost=11447.16..238013.21 rows=5975 width=139) (actual time=200.994..311.257 rows=4386 loops=2)
                                                  Recheck Cond: (((lower((name)::text) ~~ '%치킨%'::text) AND (deleted_at IS NULL)) OR ((lower((name)::text) % '치킨'::text) AND (deleted_at IS NULL)))
                                                  Rows Removed by Index Recheck: 36728
                                                  Filter: (((lower((name)::text) ~~ '%치킨%'::text) OR (lower((name)::text) % '치킨'::text)) AND (st_distance(geography(location), '0101000020E61000009A99999999B95F400000000000C04240'::geography, false) <= '3000'::double precision))
                                                  Rows Removed by Filter: 10894
                                                  Heap Blocks: exact=2036
                                                  Buffers: shared hit=2888
                                                  ->  BitmapOr  (cost=11447.16..11447.16 rows=30475 width=0) (actual time=25.729..25.731 rows=0 loops=1)
                                                        Buffers: shared hit=296
                                                        ->  Bitmap Index Scan on idx_restaurant_name_trgm_active  (cost=0.00..11411.86 rows=30465 width=0) (actual time=22.700..22.700 rows=104017 loops=1)
                                                              Index Cond: (lower((name)::text) ~~ '%치킨%'::text)
                                                              Buffers: shared hit=277
                                                        ->  Bitmap Index Scan on idx_restaurant_name_trgm_active  (cost=0.00..30.22 rows=10 width=0) (actual time=2.975..2.975 rows=30560 loops=1)
                                                              Index Cond: (lower((name)::text) % '치킨'::text)
                                                              Buffers: shared hit=19
                                      ->  Sort  (cost=109.04..112.96 rows=1570 width=16) (actual time=0.193..0.193 rows=1 loops=2)
                                            Sort Key: rfc.restaurant_id DESC
                                            Sort Method: quicksort  Memory: 25kB
                                            Buffers: shared hit=2
                                            Worker 0:  Sort Method: quicksort  Memory: 25kB
                                            ->  Seq Scan on restaurant_food_category rfc  (cost=0.00..25.70 rows=1570 width=16) (actual time=0.097..0.100 rows=12 loops=2)
                                                  Buffers: shared hit=2
                          ->  Index Scan using food_category_pkey on food_category fc  (cost=0.15..0.20 rows=1 width=66) (actual time=0.000..0.000 rows=0 loops=8773)
                                Index Cond: (id = rfc.food_category_id)
Planning:
  Buffers: shared hit=384
Planning Time: 65.585 ms
JIT:
  Functions: 37
  Options: Inlining false, Optimization false, Expressions true, Deforming true
  Timing: Generation 22.899 ms (Deform 9.520 ms), Inlining 0.000 ms, Optimization 36.204 ms, Emission 375.497 ms, Total 434.600 ms
Execution Time: 975.326 ms
```
**해석**
- **인덱스 사용:** `idx_restaurant_name_trgm_active`로 `Bitmap Heap Scan`.
- **조인:** `Merge Left Join` + `Nested Loop Left Join`로 `restaurant_food_category`, `food_category` 결합.
- **집계:** `GroupAggregate`가 병목 후보.
- **정렬 지점:** 최종 `Sort (top-N heapsort)`.
- **메모리:** 정렬 34kB, 중간 Sort 1.6MB 수준.
- **시간 병목:** 조인 + 그룹화 비용이 큼.

### 전략 2: 서브쿼리
```
Limit  (cost=314344.37..315344.34 rows=20 width=164) (actual time=469.156..482.910 rows=20 loops=1)
  Buffers: shared hit=2750
  ->  Gather Merge  (cost=314344.37..822181.47 rows=10157 width=164) (actual time=458.198..471.945 rows=20 loops=1)
        Workers Planned: 1
        Workers Launched: 1
        Buffers: shared hit=2750
        ->  Sort  (cost=313344.36..313359.29 rows=5975 width=155) (actual time=272.042..272.050 rows=11 loops=2)
              Sort Key: (((((CASE WHEN (lower((r.name)::text) = '치킨'::text) THEN 1 ELSE 0 END * 100))::double precision + (similarity(lower((r.name)::text), '치킨'::text) * '30'::double precision)) + (GREATEST('0'::double precision, ('1'::double precision - (st_distance(geography(r.location), '0101000020E61000009A99999999B95F400000000000C04240'::geography, false) / '3000'::double precision))) * '50'::double precision))) DESC, r.updated_at DESC, r.id DESC
              Sort Method: top-N heapsort  Memory: 34kB
              Buffers: shared hit=2748
              Worker 0:  Sort Method: quicksort  Memory: 25kB
              ->  Parallel Bitmap Heap Scan on restaurant r  (cost=11447.16..312969.58 rows=5975 width=155) (actual time=147.107..270.210 rows=4386 loops=2)
                    Recheck Cond: (((lower((name)::text) ~~ '%치킨%'::text) AND (deleted_at IS NULL)) OR ((lower((name)::text) % '치킨'::text) AND (deleted_at IS NULL)))
                    Rows Removed by Index Recheck: 36728
                    Filter: (((lower((name)::text) ~~ '%치킨%'::text) OR (lower((name)::text) % '치킨'::text)) AND (st_distance(geography(location), '0101000020E61000009A99999999B95F400000000000C04240'::geography, false) <= '3000'::double precision))
                    Rows Removed by Filter: 10894
                    Heap Blocks: exact=2036
                    Buffers: shared hit=2726
                    ->  BitmapOr  (cost=11447.16..11447.16 rows=30475 width=0) (actual time=19.178..19.180 rows=0 loops=1)
                          Buffers: shared hit=296
                          ->  Bitmap Index Scan on idx_restaurant_name_trgm_active  (cost=0.00..11411.86 rows=30465 width=0) (actual time=16.430..16.431 rows=104017 loops=1)
                                Index Cond: (lower((name)::text) ~~ '%치킨%'::text)
                                Buffers: shared hit=277
                          ->  Bitmap Index Scan on idx_restaurant_name_trgm_active  (cost=0.00..30.22 rows=10 width=0) (actual time=2.746..2.746 rows=30560 loops=1)
                                Index Cond: (lower((name)::text) % '치킨'::text)
                                Buffers: shared hit=19
        SubPlan 2
          ->  Hash Join  (cost=22.95..52.79 rows=7 width=8) (actual time=2.574..2.707 rows=1 loops=1)
                Hash Cond: (rfc.food_category_id = fc.id)
                Buffers: shared hit=2
                ->  Seq Scan on restaurant_food_category rfc  (cost=0.00..25.70 rows=1570 width=16) (actual time=0.020..0.022 rows=12 loops=1)
                      Buffers: shared hit=1
                ->  Hash  (cost=22.90..22.90 rows=4 width=8) (actual time=0.567..0.588 rows=1 loops=1)
                      Buckets: 1024  Batches: 1  Memory Usage: 9kB
                      Buffers: shared hit=1
                      ->  Seq Scan on food_category fc  (cost=0.00..22.90 rows=4 width=8) (actual time=0.398..0.401 rows=1 loops=1)
                            Filter: (lower((name)::text) = '치킨'::text)
                            Rows Removed by Filter: 9
                            Buffers: shared hit=1
Planning:
  Buffers: shared hit=15
Planning Time: 3.260 ms
JIT:
  Functions: 46
  Options: Inlining false, Optimization false, Expressions true, Deforming true
  Timing: Generation 12.772 ms (Deform 4.647 ms), Inlining 0.000 ms, Optimization 18.064 ms, Emission 182.184 ms, Total 213.020 ms
Execution Time: 484.969 ms
```
**해석**
- **인덱스 사용:** `idx_restaurant_name_trgm_active`로 `Bitmap Heap Scan`.
- **조인:** 메인 쿼리에는 없음. 카테고리 매칭은 `SubPlan(Hash Join)`로 분리 실행.
- **정렬 지점:** 최종 `Sort (top-N heapsort)`.
- **메모리:** 정렬 34kB 수준.
- **시간 병목:** 메인 스캔 + 정렬, 서브플랜 비용은 작음.

### 전략 3: 후보군 방식
```
Limit  (cost=318129.40..318630.60 rows=20 width=163) (actual time=484.285..498.852 rows=20 loops=1)
  Buffers: shared hit=3354
  ->  Result  (cost=318129.40..323141.40 rows=200 width=163) (actual time=473.338..487.903 rows=20 loops=1)
        Buffers: shared hit=3354
        ->  Sort  (cost=318129.40..318129.90 rows=200 width=155) (actual time=473.150..487.694 rows=20 loops=1)
              Sort Key: (((((CASE WHEN (lower((r.name)::text) = '치킨'::text) THEN 1 ELSE 0 END * 100))::double precision + (similarity(lower((r.name)::text), '치킨'::text) * '30'::double precision)) + (GREATEST('0'::double precision, ('1'::double precision - (st_distance(geography(r.location), '0101000020E61000009A99999999B95F400000000000C04240'::geography, false) / '3000'::double precision))) * '50'::double precision))) DESC, r.updated_at DESC, r.id DESC
              Sort Method: top-N heapsort  Memory: 30kB
              Buffers: shared hit=3354
              ->  Nested Loop  (cost=314168.37..318124.08 rows=200 width=155) (actual time=470.230..487.611 rows=200 loops=1)
                    Buffers: shared hit=3354
                    ->  Limit  (cost=314168.08..314191.08 rows=200 width=24) (actual time=469.786..484.398 rows=200 loops=1)
                          Buffers: shared hit=2754
                          ->  Gather Merge  (cost=314168.08..314855.20 rows=5975 width=24) (actual time=469.773..484.363 rows=200 loops=1)
                                Workers Planned: 1
                                Workers Launched: 1
                                Buffers: shared hit=2754
                                ->  Sort  (cost=313168.07..313183.01 rows=5975 width=24) (actual time=268.148..268.167 rows=103 loops=2)
                                      Sort Key: (((((CASE WHEN (lower((r_1.name)::text) = '치킨'::text) THEN 1 ELSE 0 END * 100))::double precision + (similarity(lower((r_1.name)::text), '치킨'::text) * '30'::double precision)) + (GREATEST('0'::double precision, ('1'::double precision - (st_distance(geography(r_1.location), '0101000020E61000009A99999999B95F400000000000C04240'::geography, false) / '3000'::double precision))) * '50'::double precision))) DESC, r_1.updated_at DESC, r_1.id DESC
                                      Sort Method: top-N heapsort  Memory: 48kB
                                      Buffers: shared hit=2754
                                      Worker 0:  Sort Method: quicksort  Memory: 25kB
                                      ->  Parallel Bitmap Heap Scan on restaurant r_1  (cost=11447.16..312909.83 rows=5975 width=24) (actual time=151.244..266.739 rows=4386 loops=2)
                                            Recheck Cond: (((lower((name)::text) ~~ '%치킨%'::text) AND (deleted_at IS NULL)) OR ((lower((name)::text) % '치킨'::text) AND (deleted_at IS NULL)))
                                            Rows Removed by Index Recheck: 36728
                                            Filter: (((lower((name)::text) ~~ '%치킨%'::text) OR (lower((name)::text) % '치킨'::text)) AND (st_distance(geography(location), '0101000020E61000009A99999999B95F400000000000C04240'::geography, false) <= '3000'::double precision))
                                            Rows Removed by Filter: 10894
                                            Heap Blocks: exact=2036
                                            Buffers: shared hit=2732
                                            ->  BitmapOr  (cost=11447.16..11447.16 rows=30475 width=0) (actual time=18.564..18.565 rows=0 loops=1)
                                                  Buffers: shared hit=296
                                                  ->  Bitmap Index Scan on idx_restaurant_name_trgm_active  (cost=0.00..11411.86 rows=30465 width=0) (actual time=15.641..15.641 rows=104017 loops=1)
                                                        Index Cond: (lower((name)::text) ~~ '%치킨%'::text)
                                                        Buffers: shared hit=277
                                                  ->  Bitmap Index Scan on idx_restaurant_name_trgm_active  (cost=0.00..30.22 rows=10 width=0) (actual time=2.921..2.921 rows=30560 loops=1)
                                                        Index Cond: (lower((name)::text) % '치킨'::text)
                                                        Buffers: shared hit=19
                    ->  Index Scan using restaurant_pkey on restaurant r  (cost=0.29..7.11 rows=1 width=139) (actual time=0.008..0.008 rows=1 loops=200)
                          Index Cond: (id = r_1.id)
                          Buffers: shared hit=600
Planning:
  Buffers: shared hit=41
Planning Time: 2.373 ms
JIT:
  Functions: 21
  Options: Inlining false, Optimization false, Expressions true, Deforming true
  Timing: Generation 11.333 ms (Deform 4.791 ms), Inlining 0.000 ms, Optimization 17.064 ms, Emission 190.424 ms, Total 218.821 ms
Execution Time: 500.287 ms
```
**해석**
- **인덱스 사용:** 후보군 추출 단계에서 `idx_restaurant_name_trgm_active` 사용.
- **조인:** 후보 id와 `restaurant`를 `Nested Loop`로 결합.
- **정렬 지점:** 후보군 추출 단계와 최종 단계에서 정렬 발생.
- **메모리:** top-N 정렬 메모리 30~48kB.
- **시간 병목:** 후보군 추출 정렬이 핵심 비용, 최종 정렬은 200건이라 부담 적음.

## EXPLAIN 원문 (인덱스 미적용)
### 노멀 쿼리 (indexscan/bitmapscan off)
```
Limit  (cost=844512.92..844765.27 rows=20 width=167) (actual time=2572.824..2595.219 rows=20 loops=1)
  Buffers: shared hit=3275 read=230
  ->  Gather Merge  (cost=844512.92..919902.48 rows=5975 width=167) (actual time=1359.659..1382.007 rows=20 loops=1)
        Workers Planned: 1
        Workers Launched: 1
        Buffers: shared hit=3275 read=230
        ->  Result  (cost=843512.91..993246.41 rows=5975 width=167) (actual time=756.870..756.914 rows=15 loops=2)
              Buffers: shared hit=3275 read=230
              ->  Sort  (cost=843512.91..843527.85 rows=5975 width=159) (actual time=756.663..756.672 rows=15 loops=2)
                    Sort Key: (((((CASE WHEN (lower((name)::text) = '치킨'::text) THEN 1 ELSE 0 END * 100))::double precision + (similarity(lower((name)::text), '치킨'::text) * '30'::double precision)) + (GREATEST('0'::double precision, ('1'::double precision - (st_distance(geography(location), '0101000020E61000009A99999999B95F400000000000C04240'::geography, false) / '3000'::double precision))) * '50'::double precision))) DESC, updated_at DESC, id DESC
                    Sort Method: top-N heapsort  Memory: 34kB
                    Buffers: shared hit=3275 read=230
                    Worker 0:  Sort Method: quicksort  Memory: 27kB
                    ->  Parallel Seq Scan on restaurant r  (cost=0.00..843353.92 rows=5975 width=159) (actual time=632.366..754.314 rows=4386 loops=2)
                          Filter: ((deleted_at IS NULL) AND ((lower((name)::text) ~~ '%치킨%'::text) OR (lower((name)::text) % '치킨'::text)) AND (st_distance(geography(location), '0101000020E61000009A99999999B95F400000000000C04240'::geography, false) <= '3000'::double precision))
                          Rows Removed by Filter: 47622
                          Buffers: shared hit=3223 read=230
Planning:
  Buffers: shared hit=305
Planning Time: 51.025 ms
JIT:
  Functions: 14
  Options: Inlining true, Optimization true, Expressions true, Deforming true
  Timing: Generation 26.715 ms (Deform 11.259 ms), Inlining 342.054 ms, Optimization 949.779 ms, Emission 969.604 ms, Total 2288.151 ms
Execution Time: 2810.458 ms
```
**해석**
- **인덱스 사용:** 없음 (`enable_indexscan/bitmapscan=off`).
- **풀스캔 여부:** `Parallel Seq Scan`으로 전량 스캔.
- **정렬 지점:** 최종 `Sort (top-N heapsort)`.
- **메모리:** 정렬 34kB 수준.
- **시간 병목:** 전체 스캔 + 거리 계산 + 정렬.

### 전략 1: JOIN (indexscan/bitmapscan off)
```
Limit  (cost=898566.46..899067.66 rows=20 width=167) (actual time=1669.841..1670.062 rows=20 loops=1)
  Buffers: shared hit=2880
  ->  Result  (cost=898566.46..1153100.88 rows=10157 width=167) (actual time=1331.451..1331.668 rows=20 loops=1)
        Buffers: shared hit=2880
        ->  Sort  (cost=898566.46..898591.85 rows=10157 width=159) (actual time=1331.258..1331.456 rows=20 loops=1)
              Sort Key: (((((CASE WHEN (lower((r.name)::text) = '치킨'::text) THEN 1 ELSE 0 END * 100))::double precision + (similarity(lower((r.name)::text), '치킨'::text) * '30'::double precision)) + (GREATEST('0'::double precision, ('1'::double precision - (st_distance(geography(r.location), '0101000020E61000009A99999999B95F400000000000C04240'::geography, false) / '3000'::double precision))) * '50'::double precision))) DESC, r.updated_at DESC, r.id DESC
              Sort Method: top-N heapsort  Memory: 34kB
              Buffers: shared hit=2880
              ->  Finalize GroupAggregate  (cost=769914.86..898296.18 rows=10157 width=159) (actual time=1272.770..1328.965 rows=8773 loops=1)
                    Group Key: r.id
                    Buffers: shared hit=2880
                    ->  Gather Merge  (cost=769914.86..770745.17 rows=5975 width=143) (actual time=1271.640..1278.315 rows=8773 loops=1)
                          Workers Planned: 1
                          Workers Launched: 1
                          Buffers: shared hit=2880
                          ->  Partial GroupAggregate  (cost=768914.85..769072.98 rows=5975 width=143) (actual time=640.345..643.169 rows=4386 loops=2)
                                Group Key: r.id
                                Buffers: shared hit=2880
                                ->  Merge Left Join  (cost=768914.85..768953.48 rows=5975 width=197) (actual time=639.513..640.622 rows=4386 loops=2)
                                      Merge Cond: (r.id = rfc.restaurant_id)
                                      Buffers: shared hit=2880
                                      ->  Sort  (cost=768772.32..768787.26 rows=5975 width=139) (actual time=637.706..637.983 rows=4386 loops=2)
                                            Sort Key: r.id DESC
                                            Sort Method: quicksort  Memory: 1685kB
                                            Buffers: shared hit=2867
                                            Worker 0:  Sort Method: quicksort  Memory: 26kB
                                            ->  Parallel Seq Scan on restaurant r  (cost=0.00..768397.54 rows=5975 width=139) (actual time=542.322..633.138 rows=4386 loops=2)
                                                  Filter: ((deleted_at IS NULL) AND ((lower((name)::text) ~~ '%치킨%'::text) OR (lower((name)::text) % '치킨'::text)) AND (st_distance(geography(location), '0101000020E61000009A99999999B95F400000000000C04240'::geography, false) <= '3000'::double precision))
                                                  Rows Removed by Filter: 47622
                                                  Buffers: shared hit=2863
                                      ->  Sort  (cost=142.53..146.46 rows=1570 width=66) (actual time=1.285..1.373 rows=1 loops=2)
                                            Sort Key: rfc.restaurant_id DESC
                                            Sort Method: quicksort  Memory: 25kB
                                            Buffers: shared hit=13
                                            Worker 0:  Sort Method: quicksort  Memory: 25kB
                                            ->  Hash Left Join  (cost=29.35..59.19 rows=1570 width=66) (actual time=1.151..1.359 rows=12 loops=2)
                                                  Hash Cond: (rfc.food_category_id = fc.id)
                                                  Buffers: shared hit=13
                                                  ->  Seq Scan on restaurant_food_category rfc  (cost=0.00..25.70 rows=1570 width=16) (actual time=0.028..0.029 rows=12 loops=2)
                                                        Buffers: shared hit=2
                                                  ->  Hash  (cost=18.60..18.60 rows=860 width=66) (actual time=0.232..0.255 rows=10 loops=2)
                                                        Buckets: 1024  Batches: 1  Memory Usage: 9kB
                                                        Buffers: shared hit=2
                                                        ->  Seq Scan on food_category fc  (cost=0.00..18.60 rows=860 width=66) (actual time=0.052..0.054 rows=10 loops=2)
                                                              Buffers: shared hit=2
Planning:
  Buffers: shared hit=85
Planning Time: 12.813 ms
JIT:
  Functions: 52
  Options: Inlining true, Optimization true, Expressions true, Deforming true
  Timing: Generation 17.551 ms (Deform 6.669 ms), Inlining 123.695 ms, Optimization 616.610 ms, Emission 596.174 ms, Total 1354.030 ms
Execution Time: 1679.508 ms
```
**해석**
- **인덱스 사용:** 없음.
- **풀스캔 여부:** `Parallel Seq Scan`.
- **조인:** `Merge Left Join` + `Hash Left Join`.
- **집계:** `GroupAggregate` 비용 큼.
- **정렬 지점:** 최종 `Sort (top-N heapsort)`.
- **메모리:** 정렬 34kB, 중간 Sort 1.6MB 수준.
- **시간 병목:** 풀스캔 + 조인/집계.

### 전략 2: 서브쿼리 (indexscan/bitmapscan off)
```
Limit  (cost=844728.70..846041.33 rows=20 width=164) (actual time=1747.980..1748.507 rows=20 loops=1)
  Buffers: shared hit=2910
  ->  Gather Merge  (cost=844728.70..1511347.55 rows=10157 width=164) (actual time=1547.329..1547.852 rows=20 loops=1)
        Workers Planned: 1
        Workers Launched: 1
        Buffers: shared hit=2910
        ->  Sort  (cost=843728.69..843743.63 rows=5975 width=155) (actual time=789.068..789.077 rows=16 loops=2)
              Sort Key: (((((CASE WHEN (lower((r.name)::text) = '치킨'::text) THEN 1 ELSE 0 END * 100))::double precision + (similarity(lower((r.name)::text), '치킨'::text) * '30'::double precision)) + (GREATEST('0'::double precision, ('1'::double precision - (st_distance(geography(r.location), '0101000020E61000009A99999999B95F400000000000C04240'::geography, false) / '3000'::double precision))) * '50'::double precision))) DESC, r.updated_at DESC, r.id DESC
              Sort Method: top-N heapsort  Memory: 34kB
              Buffers: shared hit=2908
              Worker 0:  Sort Method: quicksort  Memory: 27kB
              ->  Parallel Seq Scan on restaurant r  (cost=0.00..843353.92 rows=5975 width=155) (actual time=672.322..787.141 rows=4386 loops=2)
                    Filter: ((deleted_at IS NULL) AND ((lower((name)::text) ~~ '%치킨%'::text) OR (lower((name)::text) % '치킨'::text)) AND (st_distance(geography(location), '0101000020E61000009A99999999B95F400000000000C04240'::geography, false) <= '3000'::double precision))
                    Rows Removed by Filter: 47622
                    Buffers: shared hit=2856
        SubPlan 2
          ->  Hash Join  (cost=22.95..52.79 rows=7 width=8) (actual time=20.426..20.432 rows=1 loops=1)
                Hash Cond: (rfc.food_category_id = fc.id)
                Buffers: shared hit=2
                ->  Seq Scan on restaurant_food_category rfc  (cost=0.00..25.70 rows=1570 width=16) (actual time=0.054..0.056 rows=12 loops=1)
                      Buffers: shared hit=1
                ->  Hash  (cost=22.90..22.90 rows=4 width=8) (actual time=20.176..20.177 rows=1 loops=1)
                      Buckets: 1024  Batches: 1  Memory Usage: 9kB
                      Buffers: shared hit=1
                      ->  Seq Scan on food_category fc  (cost=0.00..22.90 rows=4 width=8) (actual time=20.134..20.138 rows=1 loops=1)
                            Filter: (lower((name)::text) = '치킨'::text)
                            Rows Removed by Filter: 9
                            Buffers: shared hit=1
Planning:
  Buffers: shared hit=15
Planning Time: 3.084 ms
JIT:
  Functions: 42
  Options: Inlining true, Optimization true, Expressions true, Deforming true
  Timing: Generation 13.129 ms (Deform 4.781 ms), Inlining 106.951 ms, Optimization 690.392 ms, Emission 610.035 ms, Total 1420.506 ms
Execution Time: 1750.810 ms
```
**해석**
- **인덱스 사용:** 없음.
- **풀스캔 여부:** `Parallel Seq Scan`.
- **조인:** 메인 없음, 서브플랜만 `Hash Join`.
- **정렬 지점:** 최종 `Sort (top-N heapsort)`.
- **메모리:** 정렬 34kB 수준.
- **시간 병목:** 풀스캔 + 정렬.

### 전략 3: 후보군 방식 (indexscan/bitmapscan off)
```
Limit  (cost=850563.47..851064.67 rows=20 width=163) (actual time=1873.152..1873.301 rows=20 loops=1)
  Buffers: shared hit=4951
  ->  Result  (cost=850563.47..855575.47 rows=200 width=163) (actual time=1521.683..1521.828 rows=20 loops=1)
        Buffers: shared hit=4951
        ->  Sort  (cost=850563.47..850563.97 rows=200 width=155) (actual time=1521.370..1521.496 rows=20 loops=1)
              Sort Key: (((((CASE WHEN (lower((r.name)::text) = '치킨'::text) THEN 1 ELSE 0 END * 100))::double precision + (similarity(lower((r.name)::text), '치킨'::text) * '30'::double precision)) + (GREATEST('0'::double precision, ('1'::double precision - (st_distance(geography(r.location), '0101000020E61000009A99999999B95F400000000000C04240'::geography, false) / '3000'::double precision))) * '50'::double precision))) DESC, r.updated_at DESC, r.id DESC
              Sort Method: top-N heapsort  Memory: 34kB
              Buffers: shared hit=4951
              ->  Hash Join  (cost=844579.91..850558.15 rows=200 width=155) (actual time=1498.437..1521.333 rows=200 loops=1)
                    Hash Cond: (r.id = c.id)
                    Buffers: shared hit=4951
                    ->  Seq Scan on restaurant r  (cost=0.00..3077.17 rows=104017 width=139) (actual time=0.044..8.234 rows=104017 loops=1)
                          Buffers: shared hit=2037
                    ->  Hash  (cost=844577.41..844577.41 rows=200 width=8) (actual time=1497.349..1497.467 rows=200 loops=1)
                          Buckets: 1024  Batches: 1  Memory Usage: 16kB
                          Buffers: shared hit=2914
                          ->  Subquery Scan on c  (cost=844552.41..844577.41 rows=200 width=8) (actual time=1497.204..1497.413 rows=200 loops=1)
                                Buffers: shared hit=2914
                                ->  Limit  (cost=844552.41..844575.41 rows=200 width=24) (actual time=1497.012..1497.178 rows=200 loops=1)
                                      Buffers: shared hit=2914
                                      ->  Gather Merge  (cost=844552.41..845239.54 rows=5975 width=24) (actual time=1496.988..1497.133 rows=200 loops=1)
                                            Workers Planned: 1
                                            Workers Launched: 1
                                            Buffers: shared hit=2914
                                            ->  Sort  (cost=843552.40..843567.34 rows=5975 width=24) (actual time=757.165..757.185 rows=103 loops=2)
                                                  Sort Key: (((((CASE WHEN (lower((r_1.name)::text) = '치킨'::text) THEN 1 ELSE 0 END * 100))::double precision + (similarity(lower((r_1.name)::text), '치킨'::text) * '30'::double precision)) + (GREATEST('0'::double precision, ('1'::double precision - (st_distance(geography(r_1.location), '0101000020E61000009A99999999B95F400000000000C04240'::geography, false) / '3000'::double precision))) * '50'::double precision))) DESC, r_1.updated_at DESC, r_1.id DESC
                                                  Sort Method: top-N heapsort  Memory: 48kB
                                                  Buffers: shared hit=2914
                                                  Worker 0:  Sort Method: quicksort  Memory: 25kB
                                                  ->  Parallel Seq Scan on restaurant r_1  (cost=0.00..843294.17 rows=5975 width=24) (actual time=639.815..755.262 rows=4386 loops=2)
                                                        Filter: ((deleted_at IS NULL) AND ((lower((name)::text) ~~ '%치킨%'::text) OR (lower((name)::text) % '치킨'::text)) AND (st_distance(geography(location), '0101000020E61000009A99999999B95F400000000000C04240'::geography, false) <= '3000'::double precision))
                                                        Rows Removed by Filter: 47622
                                                        Buffers: shared hit=2862
Planning:
  Buffers: shared hit=41
Planning Time: 3.857 ms
JIT:
  Functions: 21
  Options: Inlining true, Optimization true, Expressions true, Deforming true
  Timing: Generation 12.860 ms (Deform 5.356 ms), Inlining 153.553 ms, Optimization 663.779 ms, Emission 687.460 ms, Total 1517.651 ms
Execution Time: 1875.216 ms
```
**해석**
- **인덱스 사용:** 없음.
- **풀스캔 여부:** 후보군 추출에서 `Parallel Seq Scan`.
- **조인:** 후보군과 본 테이블 `Hash Join`.
- **정렬 지점:** 후보군 추출 단계에서 정렬, 최종 정렬도 있음.
- **메모리:** 정렬 34~48kB, 해시 16kB.
- **시간 병목:** 후보군 추출(풀스캔 + 정렬) 단계.
