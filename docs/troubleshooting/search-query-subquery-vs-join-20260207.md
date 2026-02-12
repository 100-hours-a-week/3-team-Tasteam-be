# 검색 쿼리 실행계획 비교 (서브쿼리 vs 조인) - 2026-02-07

## 환경
- DB: PostgreSQL 17 (docker-compose.local `tasteam-db`)
- 데이터: restaurant 104,017건 (더미 시드 후)
- 키워드: `피자`
- 위치: 위도 37.5 / 경도 126.9 (거리 정렬 포함)

## 1) 서브쿼리 사용 버전
쿼리:
```sql
SELECT r.*,
       (lower(r.name) = lower('피자')) AS name_exact,
       similarity(lower(r.name), lower('피자')) AS name_sim,
       CASE WHEN 37.5 IS NULL OR 126.9 IS NULL THEN NULL
            ELSE ST_DistanceSphere(r.location, ST_MakePoint(126.9, 37.5)) END AS dist_m,
       EXISTS (
         SELECT 1
         FROM restaurant_food_category rfc
         JOIN food_category fc ON fc.id = rfc.food_category_id
         WHERE rfc.restaurant_id = r.id AND lower(fc.name) = lower('피자')
       ) AS category_match,
       (lower(r.full_address) LIKE '%'||lower('피자')||'%') AS addr_match
FROM restaurant r
WHERE r.deleted_at IS NULL
  AND (
    lower(r.name) LIKE '%'||lower('피자')||'%'
    OR lower(r.full_address) LIKE '%'||lower('피자')||'%'
    OR EXISTS (
      SELECT 1
      FROM restaurant_food_category rfc
      JOIN food_category fc ON fc.id = rfc.food_category_id
      WHERE rfc.restaurant_id = r.id AND lower(fc.name) = lower('피자')
    )
  )
ORDER BY name_exact DESC, name_sim DESC, dist_m ASC NULLS LAST,
         category_match DESC, addr_match DESC, r.updated_at DESC, r.id DESC
LIMIT 20;
```

실행 계획(원문):
```
Limit  (cost=6489627.69..6489627.74 rows=20 width=154) (actual time=1266.288..1266.438 rows=1 loops=1)
  Buffers: shared hit=2190
  ->  Sort  (cost=6489627.69..6489757.74 rows=52019 width=154) (actual time=193.212..193.321 rows=1 loops=1)
        Sort Key: ((lower((r.name)::text) = '피자'::text)) DESC, (similarity(lower((r.name)::text), '피자'::text)) DESC, (st_distance(geography(r.location), '0101000020E61000009A99999999B95F400000000000C04240'::geography, false)), ((ANY (r.id = (hashed SubPlan 2).col1))) DESC, ((lower((r.full_address)::text) ~~ '%피자%'::text)) DESC, r.updated_at DESC, r.id DESC
        Sort Method: quicksort  Memory: 25kB
        Buffers: shared hit=2190
        ->  Seq Scan on restaurant r  (cost=0.00..6488243.49 rows=52019 width=154) (actual time=103.274..192.227 rows=1 loops=1)
              Filter: ((deleted_at IS NULL) AND ((lower((name)::text) ~~ '%피자%'::text) OR (lower((full_address)::text) ~~ '%피자%'::text) OR (ANY (id = (hashed SubPlan 4).col1))))
              Rows Removed by Filter: 104016
              Buffers: shared hit=2173
              SubPlan 2
                ->  Hash Join  (cost=22.95..52.79 rows=7 width=8) (actual time=14.017..14.112 rows=1 loops=1)
                      Hash Cond: (rfc.food_category_id = fc.id)
                      Buffers: shared hit=2
                      ->  Seq Scan on restaurant_food_category rfc  (cost=0.00..25.70 rows=1570 width=16) (actual time=0.037..0.041 rows=12 loops=1)
                            Buffers: shared hit=1
                      ->  Hash  (cost=22.90..22.90 rows=4 width=8) (actual time=13.828..13.851 rows=1 loops=1)
                            Buckets: 1024  Batches: 1  Memory Usage: 9kB
                            Buffers: shared hit=1
                            ->  Seq Scan on food_category fc  (cost=0.00..22.90 rows=4 width=8) (actual time=13.807..13.812 rows=1 loops=1)
                                  Filter: (lower((name)::text) = '피자'::text)
                                  Rows Removed by Filter: 9
                                  Buffers: shared hit=1
              SubPlan 4
                ->  Hash Join  (cost=22.95..52.79 rows=7 width=8) (actual time=16.404..16.456 rows=1 loops=1)
                      Hash Cond: (rfc_1.food_category_id = fc_1.id)
                      Buffers: shared hit=2
                      ->  Seq Scan on restaurant_food_category rfc_1  (cost=0.00..25.70 rows=1570 width=16) (actual time=0.107..0.110 rows=12 loops=1)
                            Buffers: shared hit=1
                      ->  Hash  (cost=22.90..22.90 rows=4 width=8) (actual time=15.348..15.349 rows=1 loops=1)
                            Buckets: 1024  Batches: 1  Memory Usage: 9kB
                            Buffers: shared hit=1
                            ->  Seq Scan on food_category fc_1  (cost=0.00..22.90 rows=4 width=8) (actual time=15.120..15.128 rows=1 loops=1)
                                  Filter: (lower((name)::text) = '피자'::text)
                                  Rows Removed by Filter: 9
                                  Buffers: shared hit=1
Planning:
  Buffers: shared hit=335
Planning Time: 60.962 ms
JIT:
  Functions: 49
  Options: Inlining true, Optimization true, Expressions true, Deforming true
  Timing: Generation 13.300 ms (Deform 5.282 ms), Inlining 115.929 ms, Optimization 516.368 ms, Emission 468.982 ms, Total 1114.579 ms
Execution Time: 1475.892 ms
```

## 2) 서브쿼리 제거 버전 (JOIN + 집계)
쿼리:
```sql
SELECT r.*,
       (lower(r.name) = lower('피자')) AS name_exact,
       similarity(lower(r.name), lower('피자')) AS name_sim,
       CASE WHEN 37.5 IS NULL OR 126.9 IS NULL THEN NULL
            ELSE ST_DistanceSphere(r.location, ST_MakePoint(126.9, 37.5)) END AS dist_m,
       MAX(CASE WHEN lower(fc.name) = lower('피자') THEN 1 ELSE 0 END) AS category_match,
       (lower(r.full_address) LIKE '%'||lower('피자')||'%') AS addr_match
FROM restaurant r
LEFT JOIN restaurant_food_category rfc ON rfc.restaurant_id = r.id
LEFT JOIN food_category fc ON fc.id = rfc.food_category_id
WHERE r.deleted_at IS NULL
  AND (
    lower(r.name) LIKE '%'||lower('피자')||'%'
    OR lower(r.full_address) LIKE '%'||lower('피자')||'%'
    OR lower(fc.name) = lower('피자')
  )
GROUP BY r.id
ORDER BY name_exact DESC, name_sim DESC, dist_m ASC NULLS LAST,
         category_match DESC, addr_match DESC, r.updated_at DESC, r.id DESC
LIMIT 20;
```

실행 계획(원문):
```
Limit  (cost=11995.57..11995.62 rows=20 width=157) (actual time=224.212..224.348 rows=1 loops=1)
  Buffers: shared hit=2761
  ->  Sort  (cost=11995.57..11996.92 rows=541 width=157) (actual time=224.107..224.201 rows=1 loops=1)
        Sort Key: ((lower((r.name)::text) = '피자'::text)) DESC, (similarity(lower((r.name)::text), '피자'::text)) DESC, (st_distance(geography(r.location), '0101000020E61000009A99999999B95F400000000000C04240'::geography, false)), (max(CASE WHEN (lower((fc.name)::text) = '피자'::text) THEN 1 ELSE 0 END)) DESC, ((lower((r.full_address)::text) ~~ '%피자%'::text)) DESC, r.updated_at DESC, r.id DESC
        Sort Method: quicksort  Memory: 25kB
        Buffers: shared hit=2761
        ->  GroupAggregate  (cost=142.83..11981.17 rows=541 width=157) (actual time=223.571..223.664 rows=1 loops=1)
              Group Key: r.id
              Buffers: shared hit=2744
              ->  Merge Left Join  (cost=142.83..5198.38 rows=541 width=197) (actual time=141.685..141.804 rows=1 loops=1)
                    Merge Cond: (r.id = rfc.restaurant_id)
                    Filter: ((lower((r.name)::text) ~~ '%피자%'::text) OR (lower((r.full_address)::text) ~~ '%피자%'::text) OR (lower((fc.name)::text) = '피자'::text))
                    Rows Removed by Filter: 104016
                    Buffers: shared hit=2620
                    ->  Index Scan Backward using restaurant_pkey on restaurant r  (cost=0.29..4748.71 rows=104017 width=139) (actual time=0.468..40.139 rows=104017 loops=1)
                          Filter: (deleted_at IS NULL)
                          Buffers: shared hit=2618
                    ->  Sort  (cost=142.53..146.46 rows=1570 width=66) (actual time=2.239..2.340 rows=12 loops=1)
                          Sort Key: rfc.restaurant_id DESC
                          Sort Method: quicksort  Memory: 25kB
                          Buffers: shared hit=2
                          ->  Hash Left Join  (cost=29.35..59.19 rows=1570 width=66) (actual time=1.374..1.609 rows=12 loops=1)
                                Hash Cond: (rfc.food_category_id = fc.id)
                                Buffers: shared hit=2
                                ->  Seq Scan on restaurant_food_category rfc  (cost=0.00..25.70 rows=1570 width=16) (actual time=0.065..0.066 rows=12 loops=1)
                                      Buffers: shared hit=1
                                ->  Hash  (cost=18.60..18.60 rows=860 width=66) (actual time=0.403..0.429 rows=10 loops=1)
                                      Buckets: 1024  Batches: 1  Memory Usage: 9kB
                                      Buffers: shared hit=1
                                      ->  Seq Scan on food_category fc  (cost=0.00..18.60 rows=860 width=66) (actual time=0.105..0.108 rows=10 loops=1)
                                            Buffers: shared hit=1
Planning:
  Buffers: shared hit=324
Planning Time: 70.129 ms
Execution Time: 228.066 ms
```

## 비교 요약
- 서브쿼리 버전: `Seq Scan` + subplan, 약 1.62s
- 조인 버전: `Index Scan Backward` + `GroupAggregate`, 약 0.21s
- 두 버전 모두 정렬 비용이 크고, 문자열 유사도/거리 계산 비용이 비쌈

## 실행 순서 반대 측정 (JOIN -> SUBQUERY)
- 1차: JOIN 버전 먼저 실행
- 2차: SUBQUERY 버전 실행

### JOIN 먼저 실행 결과
```
Execution Time: 368.457 ms
```

### SUBQUERY 이후 실행 결과
```
Execution Time: 1624.466 ms
```

## JIT 비활성화 측정 (SUBQUERY, 동일 조건)
실행:
```sql
SET jit = off;
EXPLAIN (ANALYZE, BUFFERS)
SELECT r.*, (lower(r.name)=lower('피자')) AS name_exact,
       similarity(lower(r.name), lower('피자')) AS name_sim,
       CASE WHEN 37.5 IS NULL OR 126.9 IS NULL THEN NULL
            ELSE ST_DistanceSphere(r.location, ST_MakePoint(126.9, 37.5)) END AS dist_m,
       EXISTS (SELECT 1 FROM restaurant_food_category rfc JOIN food_category fc ON fc.id = rfc.food_category_id
               WHERE rfc.restaurant_id = r.id AND lower(fc.name) = lower('피자')) AS category_match,
       (lower(r.full_address) LIKE '%'||lower('피자')||'%') AS addr_match
FROM restaurant r
WHERE r.deleted_at IS NULL
  AND (
    lower(r.name) LIKE '%'||lower('피자')||'%'
    OR lower(r.full_address) LIKE '%'||lower('피자')||'%'
    OR EXISTS (
      SELECT 1
      FROM restaurant_food_category rfc JOIN food_category fc ON fc.id = rfc.food_category_id
      WHERE rfc.restaurant_id = r.id AND lower(fc.name) = lower('피자')
    )
  )
ORDER BY name_exact DESC, name_sim DESC, dist_m ASC NULLS LAST,
         category_match DESC, addr_match DESC, r.updated_at DESC, r.id DESC
LIMIT 20;
```

실행 계획(원문):
```
Limit  (cost=6489627.69..6489627.74 rows=20 width=154) (actual time=220.015..220.212 rows=1 loops=1)
  Buffers: shared hit=2190
  ->  Sort  (cost=6489627.69..6489757.74 rows=52019 width=154) (actual time=219.910..220.026 rows=1 loops=1)
        Sort Key: ((lower((r.name)::text) = '피자'::text)) DESC, (similarity(lower((r.name)::text), '피자'::text)) DESC, (st_distance(geography(r.location), '0101000020E61000009A99999999B95F400000000000C04240'::geography, false)), ((ANY (r.id = (hashed SubPlan 2).col1))) DESC, ((lower((r.full_address)::text) ~~ '%피자%'::text)) DESC, r.updated_at DESC, r.id DESC
        Sort Method: quicksort  Memory: 25kB
        Buffers: shared hit=2190
        ->  Seq Scan on restaurant r  (cost=0.00..6488243.49 rows=52019 width=154) (actual time=86.827..219.100 rows=1 loops=1)
              Filter: ((deleted_at IS NULL) AND ((lower((name)::text) ~~ '%피자%'::text) OR (lower((full_address)::text) ~~ '%피자%'::text) OR (ANY (id = (hashed SubPlan 4).col1))))
              Rows Removed by Filter: 104016
              Buffers: shared hit=2173
              SubPlan 2
                ->  Hash Join  (cost=22.95..52.79 rows=7 width=8) (actual time=0.100..0.198 rows=1 loops=1)
                      Hash Cond: (rfc.food_category_id = fc.id)
                      Buffers: shared hit=2
                      ->  Seq Scan on restaurant_food_category rfc  (cost=0.00..25.70 rows=1570 width=16) (actual time=0.029..0.030 rows=12 loops=1)
                            Buffers: shared hit=1
                      ->  Hash  (cost=22.90..22.90 rows=4 width=8) (actual time=0.042..0.068 rows=1 loops=1)
                            Buckets: 1024  Batches: 1  Memory Usage: 9kB
                            Buffers: shared hit=1
                            ->  Seq Scan on food_category fc  (cost=0.00..22.90 rows=4 width=8) (actual time=0.036..0.037 rows=1 loops=1)
                                  Filter: (lower((name)::text) = '피자'::text)
                                  Rows Removed by Filter: 9
                                  Buffers: shared hit=1
              SubPlan 4
                ->  Hash Join  (cost=22.95..52.79 rows=7 width=8) (actual time=1.511..1.599 rows=1 loops=1)
                      Hash Cond: (rfc_1.food_category_id = fc_1.id)
                      Buffers: shared hit=2
                      ->  Seq Scan on restaurant_food_category rfc_1  (cost=0.00..25.70 rows=1570 width=16) (actual time=0.103..0.105 rows=12 loops=1)
                            Buffers: shared hit=1
                      ->  Hash  (cost=22.90..22.90 rows=4 width=8) (actual time=0.424..0.425 rows=1 loops=1)
                            Buckets: 1024  Batches: 1  Memory Usage: 9kB
                            Buffers: shared hit=1
                            ->  Seq Scan on food_category fc_1  (cost=0.00..22.90 rows=4 width=8) (actual time=0.206..0.210 rows=1 loops=1)
                                  Filter: (lower((name)::text) = '피자'::text)
                                  Rows Removed by Filter: 9
                                  Buffers: shared hit=1
Planning:
  Buffers: shared hit=335
Planning Time: 68.625 ms
Execution Time: 224.222 ms
```

## 대안 최적화 방법 (요구 조건 기준)
조건:
- 대상 규모: 약 50만 건
- 검색 정확도: 부분 일치 + 유사도 필요

### 적용 난이도 순 (쉬움 → 어려움)
1. **TRGM(GIN) 인덱스 + 유사도 정렬**
   - 핵심: `pg_trgm` 확장으로 `name`, `address`에 트리그램 인덱스 적용 후 `similarity()`로 정렬.
   - 장점: 스키마 변경 최소, 부분일치(`%keyword%`)에도 인덱스 활용 가능.
   - 단점: 유사도 계산 자체는 비용이 큼, 다중 컬럼 비교 시 계산량 증가.
   - 필요 작업:
     - `CREATE EXTENSION IF NOT EXISTS pg_trgm;`
     - `CREATE INDEX ... gin (lower(name) gin_trgm_ops)` 등
     - 쿼리에서 `similarity(lower(name), lower(:kw))` 사용
   - 난이도: 낮음

2. **2단계 검색 (후보 ID 먼저 추출 → 정렬)**
   - 핵심: 1차에서 인덱스 기반으로 후보 ID를 추출하고, 2차에서 거리/유사도/정렬 적용.
   - 장점: 비싼 계산(거리/유사도)을 후보 집합에만 적용해 성능 개선 폭 큼.
   - 단점: 쿼리 2회 실행, 후보 크기(예: 200~500) 튜닝 필요.
   - 필요 작업:
     - 후보 추출용 인덱스(TRGM)
     - 서비스 레벨에서 2쿼리 파이프라인 구성
   - 난이도: 중간

3. **Full‑Text Search (tsvector + GIN)**
   - 핵심: 이름/주소/카테고리를 합친 `tsvector` 컬럼을 만들고 `ts_rank`로 정렬.
   - 장점: 대규모 데이터에서도 안정적인 성능/정확도, 순위 정렬 내장.
   - 단점: 스키마 변경 필요, 형태소/동의어 설정 고려 필요.
   - 필요 작업:
     - `tsvector` 컬럼 생성 + GIN 인덱스
     - `plainto_tsquery` 또는 `websearch_to_tsquery` 적용
   - 난이도: 중간~상

4. **검색 전용 테이블 (denormalized)**
   - 핵심: `restaurant_search` 같은 전용 테이블에 검색 필드와 정렬 키를 미리 계산해 저장.
   - 장점: 쿼리 단순화, 조인 제거, 가장 큰 성능 개선 여지.
   - 단점: 동기화(트리거/배치/이벤트) 비용과 정합성 관리 필요.
   - 필요 작업:
     - 전용 테이블 설계
     - 동기화 파이프라인 구축
     - 검색 쿼리 전환
   - 난이도: 높음

5. **거리 최적화(GIST + 반경 제한)**
   - 핵심: 위치가 있을 때만 `ST_DWithin`으로 반경 후보를 축소 후 거리 정렬.
   - 장점: 위치 검색 비중이 높을수록 효과 큼, 거리 계산 대상 축소.
   - 단점: 위치 값 없으면 적용 불가, 반경 정책 설계 필요.
   - 필요 작업:
     - `GIST` 인덱스 on `location`
     - `WHERE ST_DWithin(location, point, radius)` 적용
   - 난이도: 중간

## 메모
- 데이터가 작거나 캐시 상태에 따라 편차가 큼.
- 정확한 비교를 위해서는 같은 캐시 상태에서 여러 번 반복 측정 필요.
