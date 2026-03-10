# 검색 전략 EXPLAIN 분석 및 MV_SINGLE_PASS 설계 (2026-03-10)

## 배경

검색 API에서 CPU 사용량 과다 + 쿼리 지연 문제가 관측됨.
- 기본값 `TWO_STEP`이 10K 데이터에서 평균 325ms, 104K에서 782ms
- 동시 요청 시 `Seq Scan` 경쟁으로 CPU 사용량 폭증 가능
- 기존 미검증 전략(`HYBRID_SPLIT_CANDIDATES`, `GEO_FIRST_HYBRID`, `READ_MODEL_TWO_STEP`)의 EXPLAIN 미수행
- `restaurant_search_mv` MV 활용 신규 전략(`MV_SINGLE_PASS`) 설계·검증 필요

---

## 현재 방식의 문제점

| 문제 | 원인 | 영향 |
|---|---|---|
| Seq Scan 유지 | `LIKE '%kw%'` + `similarity >= 0.3` OR 조건 → 플래너 GIN 인덱스 회피 | CPU 집약, 동시 부하 취약 |
| category correlated subquery | `EXISTS (SELECT 1 FROM restaurant_food_category JOIN food_category WHERE ...)` — 행마다 실행 | CPU 낭비, JIT 함수 수 증가 |
| UNION dedup 비용 | HYBRID 전략의 4개 text CTE UNION → 임시 결과 생성 | 메모리 + CPU 오버헤드 |
| similarity() 이중 계산 | candidate 추출 + scored_base 두 단계에서 반복 계산 | 불필요한 연산 |
| JIT 오버헤드 | 복잡한 score 표현식 → JIT 함수 수 증가, 컴파일 시간 급증 | 첫 요청 레이턴시 급증 |

---

## 테스트 환경

- DB: tasteam-db-loadtest (PostgreSQL + PostGIS)
- 데이터: 10,000건 (`SELECT count(*) FROM restaurant_search_mv WHERE deleted_at IS NULL` → 10000)
- GUC 설정:
  ```sql
  SET work_mem = '64MB'; SET jit = on; SET jit_above_cost = 0;
  SET jit_inline_above_cost = 0; SET jit_optimize_above_cost = 0;
  SET track_io_timing = on; SET effective_cache_size = '4GB';
  SET random_page_cost = 1.0; SET max_parallel_workers_per_gather = 4;
  ```
- 키워드: `강남` (위치 포함), `치킨` (위치 없음)
- 위치: lat=33.153, lng=126.037, radius=3,000m

---

## 전략별 EXPLAIN 결과 요약

| 전략 | 키워드 | location | Execution Time | JIT Functions | JIT Total | Seq Scan | Index 유형 | 특이사항 |
|---|---|---|---|---|---|---|---|---|
| S4 HYBRID_SPLIT_CANDIDATES | 강남 | ON | **2,384ms** | 81 | 1,499ms | ✅ (name_like) | Bitmap Index (trgm) + Seq Scan | EXISTS correlated subquery 1회 |
| S5 READ_MODEL_TWO_STEP | 강남 | ON | **2,302ms** | 60 | 1,478ms | ✅ (name_like, addr) | Bitmap Index (trgm) + GIS Index | MV 사용, category GIN 활용 |
| S3 GEO_FIRST_HYBRID | 치킨 | ON | **1,807ms** | 46 | 1,510ms | ❌ | GIS Index + row-by-row filter | EXISTS correlated subquery 6회 (geo 6건) |
| S6 MV_SINGLE_PASS | 강남 | ON | **1,402ms** | 12 | 1,066ms | ❌ | GIS Index (`idx_restaurant_search_mv_geography_active`) | 단일 CTE, scoring 선계산 |
| S6 MV_SINGLE_PASS | 치킨 | OFF | **1,379ms** | 8 | 1,183ms | ✅ | Seq Scan (OR 조건으로 GIN 회피) | seqscan=off 강제 시 BitmapScan 전환되나 실행시간 동일 |

---

## 주요 EXPLAIN 분석

### S4 HYBRID_SPLIT_CANDIDATES (강남, WITH location)

```
Execution Time: 2,384ms
JIT Functions: 81, Total: 1,499ms
```

- 4개 text CTE UNION → `HashAggregate` dedup (1,164건)
- `name_like_candidates`: Seq Scan (8,750 rows removed)
- `name_similarity_candidates`: Bitmap Index Scan `idx_restaurant_name_trgm` → Heap Scan (1,250 rows removed by recheck)
- `geo_candidates`: GIS Index Scan (6건)
- INTERSECT → 1건
- `scored_base`에서 EXISTS correlated subquery 1회 실행
- **JIT 함수 81개**: 복잡한 UNION + correlated subquery 구조에서 증가

### S5 READ_MODEL_TWO_STEP (강남, WITH location)

```
Execution Time: 2,302ms
JIT Functions: 60, Total: 1,478ms
```

- S4와 동일한 UNION 구조이나 `restaurant_search_mv` 활용
- `category_candidates`: GIN Bitmap Index Scan `idx_restaurant_search_mv_category_names_active` (0건 — '강남' 카테고리 없음)
- `geo_candidates`: GIS Index Scan `idx_restaurant_search_mv_geography_active` (6건)
- correlated subquery 없음 → JIT 함수 81→60 감소
- 여전히 4개 CTE UNION dedup 비용 + Seq Scan (name_like, addr) 잔존

### S3 GEO_FIRST_HYBRID (치킨, WITH location)

```
Execution Time: 1,807ms
JIT Functions: 46, Total: 1,510ms
```

- GIS Index Scan으로 geo 6건 추출 → 각 행에 OR 조건 text filter 적용
- EXISTS correlated subquery 6번 실행 (geo 결과 6건)
- 결과 1건, 최종 Nested Loop
- geo 결과가 적으면 Seq Scan 없이 빠를 수 있으나, geo 결과 많을 때 EXISTS 반복이 문제

### S6 MV_SINGLE_PASS (강남, WITH location) ← 신규

```
Execution Time: 1,402ms
JIT Functions: 12, Total: 1,066ms
```

```
Index Scan using idx_restaurant_search_mv_geography_active on restaurant_search_mv mv
  Index Cond: (geography(location) && _st_expand(..., 3000))
  Filter: ((name_lower ~~ '%강남%') OR (name_lower % '강남') OR (addr_lower ~~ '%강남%')
           OR (category_names @> '{강남}'))
  AND st_dwithin(geography(location), ..., 3000)
  Rows Removed by Filter: 6
```

- **단일 CTE**, GIS Index로 반경 내 7건 필터 → OR 조건으로 1건 최종 선택
- correlated subquery 없음, UNION 없음
- scoring을 candidates CTE 내 선계산 → 2단계 재계산 제거
- **JIT Functions 12개** (S4 대비 85% 감소, S5 대비 80% 감소)
- GIS 반경이 좁을수록 후보 집합이 작아 OR 조건의 Seq-scan 영향 최소화

### S6 MV_SINGLE_PASS (치킨, WITHOUT location)

```
Execution Time: 1,379ms (seqscan=on)
Execution Time: 1,383ms (seqscan=off → BitmapScan)
JIT Functions: 8 / 10
```

- 위치 없을 때 OR 조건으로 인해 GIN 인덱스 대신 Seq Scan 선택
- `enable_seqscan=off` 강제 시 `Bitmap Index Scan on idx_restaurant_search_mv_category_names_active`로 전환되나 전체 10K 행 Bitmap Heap Scan 후 OR 재검증 → 실행시간 동일
- **결론**: location 없는 경우 OR 조건에서 GIN BitmapOr 결합 최적화 미발생 — Seq Scan과 차이 없음

---

## MV_SINGLE_PASS 설계

### 기존 6개 전략과의 차이점

| 항목 | 기존 전략 (S4/S5) | MV_SINGLE_PASS (S6) |
|---|---|---|
| CTE 구조 | 4개 text CTE + UNION dedup | 단일 candidates CTE |
| category 조건 | correlated subquery EXISTS | MV `category_names @> ARRAY[...]::text[]` |
| scoring | scored_base CTE에서 재계산 | candidates CTE 내 선계산 |
| geo 처리 | geo_candidates CTE INTERSECT | candidates WHERE절 내 ST_DWithin |
| JIT Functions | 60~81개 | 12개 (with location) |

### SQL (WITH location)

```sql
WITH candidates AS (
    SELECT
        mv.restaurant_id,
        mv.updated_at,
        CASE WHEN mv.name_lower = :kw THEN 1 ELSE 0 END AS name_exact,
        similarity(mv.name_lower, :kw)::double precision AS name_similarity,
        ST_DistanceSphere(mv.location, ST_MakePoint(:lng, :lat)) AS distance_meters,
        CASE WHEN mv.category_names @> ARRAY[:kw]::text[] THEN 1 ELSE 0 END AS category_match,
        CASE WHEN mv.addr_lower LIKE '%' || :kw || '%' THEN 1 ELSE 0 END AS address_match,
        (
            CASE WHEN mv.name_lower = :kw THEN 1 ELSE 0 END * 100.0
            + similarity(mv.name_lower, :kw)::double precision * 30.0
            + GREATEST(0.0, 1.0 - (ST_DistanceSphere(mv.location, ST_MakePoint(:lng, :lat)) / :radius_m)) * 50.0
        ) AS total_score
    FROM restaurant_search_mv mv
    WHERE mv.deleted_at IS NULL
      AND ST_DWithin(geography(mv.location), geography(ST_MakePoint(:lng, :lat)), :radius_m)
      AND (
            mv.name_lower LIKE '%' || :kw || '%'
            OR mv.name_lower % :kw
            OR mv.addr_lower LIKE '%' || :kw || '%'
            OR mv.category_names @> ARRAY[:kw]::text[]
          )
    ORDER BY total_score DESC, mv.updated_at DESC, mv.restaurant_id DESC
    LIMIT :text_candidate_limit
)
SELECT restaurant_id, name_exact, name_similarity, distance_meters, category_match, address_match
FROM candidates
WHERE (
    :cursor_score IS NULL
    OR total_score < :cursor_score
    OR (total_score = :cursor_score AND updated_at < :cursor_updated_at)
    OR (total_score = :cursor_score AND updated_at = :cursor_updated_at AND restaurant_id < :cursor_id)
)
ORDER BY total_score DESC, updated_at DESC, restaurant_id DESC
LIMIT :size
```

### 트레이드오프

| 상황 | 특성 |
|---|---|
| location ON + 반경 좁음 | GIS Index → 소수 후보 → OR filter 비용 최소 → **최적** |
| location ON + 반경 넓음 | 후보 많아질수록 OR Seq Scan 영향 증가 |
| location OFF | OR 조건 전체 범위 Seq Scan 불가피 → S5와 유사 |
| 데이터 규모 100K+ | GIS Index 효율성 높아질수록 S6 유리 |

---

## 전략 비교 테이블

| 전략 | 설명 | 10K Execution Time (est.) | JIT Functions | Seq Scan | MV 사용 | 권장 상황 |
|---|---|---|---|---|---|---|
| ONE_STEP | QueryDSL 단일 쿼리 | ~300ms | N/A | ✅ | ❌ | 소규모 |
| TWO_STEP | QueryDSL 2단계 | ~325ms | N/A | ✅ | ❌ | 기본값 |
| JOIN_AGGREGATE | QueryDSL JOIN+GROUP BY | ~350ms | N/A | ✅ | ❌ | 카테고리 중요 시 |
| HYBRID_SPLIT_CANDIDATES | 4 text CTE UNION + INTERSECT | 2,384ms | 81 | ✅ | ❌ | 미권장 |
| GEO_FIRST_HYBRID | GIS 먼저 + text filter | 1,807ms | 46 | ❌ | ❌ | 협소 반경, 적은 geo 결과 |
| READ_MODEL_TWO_STEP | MV + 4 text CTE UNION | 2,302ms | 60 | ✅ | ✅ | 중간 규모 |
| **MV_SINGLE_PASS** | MV + 단일 CTE, scoring 선계산 | **1,402ms** | **12** | ❌ (with loc) | ✅ | **위치 검색 권장** |

> 주의: Execution Time에는 JIT 컴파일 시간 포함. 워밍업 후(JIT 캐시) 실제 쿼리 실행시간은 수십ms 수준으로 낮아질 수 있음.

---

## 권장 전략 결론

1. **위치 기반 검색 (WITH location)**: `MV_SINGLE_PASS` — JIT Functions 12개, GIS Index 단일 패스, 가장 단순한 실행 계획
2. **위치 없는 전체 검색 (WITHOUT location)**: `READ_MODEL_TWO_STEP` 또는 `TWO_STEP` — OR 조건 Seq Scan은 동일하나 MV 활용이 JOIN 비용 절감
3. **현재 기본값 `TWO_STEP`**: QueryDSL 기반으로 JIT 없이 동작 — 소규모(<1K) 또는 JIT off 환경에서 오히려 유리

### 후속 작업

- [ ] `MV_SINGLE_PASS`를 위치 검색 기본 전략으로 설정 실험
- [ ] MV 자동 갱신 (`REFRESH MATERIALIZED VIEW CONCURRENTLY`) 스케줄러 추가
- [ ] 100K 데이터 규모에서 S6 재검증
- [ ] location OFF 시 GIN BitmapOr 결합 조건 개선 방안 탐색 (split OR → UNION ALL)
