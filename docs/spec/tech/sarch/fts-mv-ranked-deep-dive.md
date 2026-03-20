| 항목 | 내용 |
|---|---|
| 문서 제목 | FTS_MV_RANKED 전략 Deep Dive |
| 문서 목적 | 현재 기본 검색 전략(FTS_MV_RANKED)의 설계 근거, SQL 구조, 점수식, 인덱스 매핑 상세 설명 |
| 작성 및 관리 | Backend Team |
| 최초 작성일 | 2026.03.16 |
| 기반 문서 | [검색 테크스펙 v2.2](./README_v2.md) |

<br>

# FTS_MV_RANKED 전략 Deep Dive

<br>

---

# **[1] 배경 — MV_SINGLE_PASS의 한계**

v1 최적 전략이었던 `MV_SINGLE_PASS`는 다음 점수식을 사용했다:

```
total_score = name_exact × 100 + name_similarity × 30 + distance_weight × 50
            (최대값 175)
```

이 방식에는 세 가지 한계가 있었다.

### 한계 1: 카테고리/주소 매칭이 스코어에 미반영

"치킨"을 검색하면 카테고리가 `치킨`인 음식점과 이름에 "치킨"이 포함된 음식점을 동일하게 취급했다.
카테고리나 주소가 WHERE 절 필터 조건에는 포함되지만 `total_score` 계산에서 제외되어 **관련성 높은 결과가 낮은 순위**에 배치될 수 있었다.

### 한계 2: FTS 없음 — recall 낮음

`search_vector @@ plainto_tsquery(...)` 조건이 없어, **FTS 인덱스(`idx_restaurant_search_mv_fts_active`)가 전혀 사용되지 않았다**.
LIKE와 trigram만으로는 단어 경계 기반 매칭이 부족하고, `ts_rank_cd`를 통한 관련도 점수도 없었다.

### 한계 3: 스코어 최대값 175 → 관련성 표현력 부족

거리 가중치가 50점으로 점수의 28%를 차지해, 카테고리/주소 매칭을 전혀 반영하지 못했다.

---

# **[2] FTS_MV_RANKED 아키텍처 — 4개 컴포넌트**

```
[검색 요청: keyword + (lat, lng, radius)]
         │
         ▼
restaurant_search_mv  ← Materialized View (역정규화 읽기 모델)
  ├── name_lower          ← pg_trgm (similarity + % 연산자, GIN idx)
  ├── addr_lower          ← LIKE 필터 + LIKE 스코어 (GIN trgm idx)
  ├── category_names[]    ← @> 배열 포함 검사 (GIN idx)
  ├── location            ← ST_DWithin 반경 필터 (GiST idx)
  └── search_vector       ← @@ FTS 토큰 매칭 + ts_rank_cd 점수 (GIN idx)
         │
         ▼
  candidates CTE  ← 단일 패스로 5개 필드 동시 계산
         │
         ▼
  total_score 정렬 + LIMIT
```

### 각 컴포넌트 역할

| 컴포넌트 | 담당 | 인덱스 |
|---|---|---|
| **MV** | 역정규화 — JOIN 없이 name/category/address/location/tsvector를 한 테이블에 보유 | UNIQUE idx |
| **pg_trgm** | 오타 허용 유사도 (`similarity()`), 부분 문자열 (`%`) | GIN trgm |
| **FTS** | 단어 단위 토큰 매칭 (`@@`), 관련도 점수 (`ts_rank_cd`) | GIN tsvector |
| **GIS** | 공간 반경 필터 (`ST_DWithin`), 거리 점수 (`ST_DistanceSphere`) | GiST geography |

---

# **[3] search_vector 설계**

## 3-1. 가중치 배분 (A / B / C)

```sql
setweight(to_tsvector('simple', coalesce(lower(r.name), '')), 'A')       -- 음식점 이름 (최고)
|| setweight(to_tsvector('simple', coalesce(
    array_to_string(category_names, ' '), '')), 'B')                       -- 카테고리 (중간)
|| setweight(to_tsvector('simple', coalesce(lower(r.full_address), '')), 'C')  -- 주소 (낮음)
AS search_vector
```

`ts_rank_cd`는 가중치별 hit 빈도를 다르게 취급한다. A(이름) > B(카테고리) > C(주소) 순으로 관련도 점수에 반영된다.

## 3-2. `simple` dictionary 선택 이유

| dictionary | 동작 | 한국어 처리 |
|---|---|---|
| `english` | 어간 추출 (running → run) | ❌ 한국어 어간 분리 불가 |
| `simple` | lowercase 변환만 수행 | ✅ 한글 토큰 그대로 통과 |
| `korean` (pg_hunspell) | 한국어 형태소 분리 | 별도 extension 필요, 미설치 |

한국어 형태소 분리는 `pg_trgm`의 3글자 trigram이 담당한다. FTS의 역할은 **단어 단위 exact/prefix 매칭과 ts_rank_cd 관련도 점수**로 한정된다.

## 3-3. `plainto_tsquery` vs `to_tsquery`

| 함수 | 특징 | 사용자 입력 안전성 |
|---|---|---|
| `to_tsquery('치킨 & 강남')` | AND/OR/NOT 연산자 직접 | ❌ 특수문자 취약 |
| `plainto_tsquery('치킨 강남')` | 단어를 AND로 자동 결합 | ✅ 특수문자 안전 처리 |
| `phraseto_tsquery('치킨 강남')` | 단어 순서 유지 phrase 검색 | 검색 범위 너무 좁음 |

`plainto_tsquery` 선택: 사용자 입력의 특수문자를 안전하게 처리하면서 다단어 입력도 AND로 결합한다.

---

# **[4] FTS_MV_RANKED SQL 전체**

## 4-1. WITH location (위치 기반 검색)

```sql
WITH candidates AS (
    SELECT
        mv.restaurant_id,
        mv.updated_at,

        -- 정확 이름 일치 (1 or 0)
        CASE WHEN mv.name_lower = :kw THEN 1 ELSE 0 END AS name_exact,

        -- pg_trgm 유사도 (0.0 ~ 1.0)
        similarity(mv.name_lower, :kw)::double precision AS name_similarity,

        -- FTS 관련도 점수 (0.0 ~ 1.0 근사) — search_vector GIN 인덱스 활용
        ts_rank_cd(mv.search_vector, plainto_tsquery('simple', :kw))
            ::double precision AS fts_rank,

        -- 거리 (미터)
        ST_DistanceSphere(mv.location, ST_MakePoint(:lng, :lat)) AS distance_meters,

        -- 카테고리 배열 포함 여부 (1 or 0)
        CASE WHEN mv.category_names @> ARRAY[:kw]::text[] THEN 1 ELSE 0 END AS category_match,

        -- 주소 포함 여부 (1 or 0)
        CASE WHEN mv.addr_lower LIKE '%' || :kw || '%' THEN 1 ELSE 0 END AS address_match,

        -- 복합 점수 (max 225)
        (
            CASE WHEN mv.name_lower = :kw THEN 1 ELSE 0 END * 100.0
            + similarity(mv.name_lower, :kw)::double precision * 30.0
            + ts_rank_cd(mv.search_vector, plainto_tsquery('simple', :kw))
                ::double precision * 25.0
            + CASE WHEN mv.category_names @> ARRAY[:kw]::text[] THEN 1 ELSE 0 END * 15.0
            + CASE WHEN mv.addr_lower LIKE '%' || :kw || '%' THEN 1 ELSE 0 END * 5.0
            + GREATEST(0.0, 1.0 - (ST_DistanceSphere(mv.location,
                ST_MakePoint(:lng, :lat)) / :radius_m)) * 50.0
        ) AS total_score

    FROM restaurant_search_mv mv
    WHERE mv.deleted_at IS NULL
      -- GiST 인덱스로 반경 내 행 선필터 → OR 조건 행 수 최소화
      AND ST_DWithin(geography(mv.location),
                     geography(ST_MakePoint(:lng, :lat)), :radius_m)
      -- OR 조건: 4가지 매칭 방식 중 하나라도 해당하면 후보
      AND (
            mv.name_lower LIKE '%' || :kw || '%'    -- 부분 문자열 (LIKE)
            OR mv.name_lower % :kw                  -- trigram 유사도 (pg_trgm)
            OR mv.search_vector @@ plainto_tsquery('simple', :kw)  -- FTS 토큰 매칭
            OR mv.category_names @> ARRAY[:kw]::text[]             -- 카테고리 포함
          )
    ORDER BY total_score DESC, mv.updated_at DESC, mv.restaurant_id DESC
    LIMIT :text_candidate_limit  -- 기본 200, TASTEAM_SEARCH_QUERY_CANDIDATE_LIMIT
)
SELECT restaurant_id, name_exact, name_similarity, fts_rank,
       distance_meters, category_match, address_match
FROM candidates
WHERE (
    -- 커서 기반 페이지네이션 (CAST 필수 — 42P18 방지)
    CAST(:cursor_score AS double precision) IS NULL
    OR total_score < CAST(:cursor_score AS double precision)
    OR (total_score = CAST(:cursor_score AS double precision)
        AND updated_at < CAST(:cursor_updated_at AS timestamptz))
    OR (total_score = CAST(:cursor_score AS double precision)
        AND updated_at = CAST(:cursor_updated_at AS timestamptz)
        AND restaurant_id < CAST(:cursor_id AS bigint))
)
ORDER BY total_score DESC, updated_at DESC, restaurant_id DESC
LIMIT :size
```

## 4-2. WITHOUT location (전국 검색)

위치 조건이 없으므로:
- `ST_DWithin` 조건 제거
- `ST_DistanceSphere` 및 거리 점수(`× 50.0`) 제거
- 결과: GiST 인덱스 미사용, Seq Scan (OR 조건으로 GIN BitmapOr 미활성)
- 최대 점수: 175 (거리 항목 없음)

---

# **[5] 점수식 상세**

```
total_score = name_exact      × 100.0   ← 정확 이름 매칭
            + name_similarity × 30.0    ← trigram 유사도 (0~1)
            + fts_rank        × 25.0    ← FTS 관련도 (0~1 근사)
            + category_match  × 15.0    ← 카테고리 정확 포함
            + address_match   × 5.0     ← 주소 부분 문자열
            + distance_weight × 50.0    ← 거리 감소 가중치 (0~1)
```

| 항목 | 최대 | 의미 | 가중치 근거 |
|---|---|---|---|
| `name_exact` | 100 | 이름이 키워드와 완전 일치 | 최우선 — 정확 이름 검색이 가장 명확한 의도 |
| `name_similarity` | 30 | 이름 trigram 유사도 | 오타 허용 + 부분 이름 매칭 |
| `fts_rank` | 25 | FTS 관련도 (단어 빈도·위치·밀도) | 이름 외 카테고리/주소 토큰 매칭 보완 |
| `category_match` | 15 | 카테고리 배열에 키워드 포함 | "치킨" → 치킨집 카테고리 우선 |
| `address_match` | 5 | 주소에 키워드 포함 | 지역명 검색 보조 (주 목적 아님) |
| `distance_weight` | 50 | `GREATEST(0, 1 - dist/radius)` | 가까울수록 높은 점수, 반경 밖 0 |

**최대값**: 225 (위치 있음) / 175 (위치 없음)

**`distance_weight` 계산:**
```
distance_weight = GREATEST(0.0, 1.0 - (ST_DistanceSphere(location, target) / radius))
```
- 반경 내 거리 0m → 1.0 (× 50 = 50점)
- 반경 경계 → 0.0 (0점)
- 반경 밖 → GREATEST로 음수 방지 (0점)

---

# **[6] 인덱스 구성 — 쿼리 조건 매핑**

`restaurant_search_mv`에 생성된 6개 인덱스:

| 인덱스명 | 타입 | 컬럼 | 조건 | 활성화 쿼리 |
|---|---|---|---|---|
| `idx_restaurant_search_mv_restaurant_id` | UNIQUE | `restaurant_id` | 없음 | `REFRESH CONCURRENTLY` 필수 |
| `idx_restaurant_search_mv_geography_active` | GiST | `geography(location)` | `deleted_at IS NULL` | `ST_DWithin` (WITH location) |
| `idx_restaurant_search_mv_name_trgm_active` | GIN (trgm) | `name_lower` | `deleted_at IS NULL` | `name_lower % :kw` (trigram %) |
| `idx_restaurant_search_mv_addr_trgm_active` | GIN (trgm) | `addr_lower` | `deleted_at IS NULL` | `addr_lower LIKE` (단독 시) |
| `idx_restaurant_search_mv_category_names_active` | GIN | `category_names` | `deleted_at IS NULL` | `category_names @>` (단독 시) |
| `idx_restaurant_search_mv_fts_active` | GIN | `search_vector` | `deleted_at IS NULL` | `search_vector @@ tsquery` (단독 시) |

**OR 조건에서의 인덱스 활용:**

OR 조건(`LIKE OR % OR @@ OR @>`)은 PostgreSQL이 각 브랜치를 BitmapScan으로 처리한 뒤 BitmapOr로 결합해야 인덱스를 활용할 수 있다.

- **WITH location**: GiST 인덱스가 먼저 반경 내 행을 추출(최대 수백 건) → 그 위에 OR 필터 적용 → **실질적으로 인덱스 활용 효과**
- **WITHOUT location**: 전체 테이블 OR 조건 → 플래너가 Seq Scan 선택 (GIN BitmapOr보다 비용 유리)

---

# **[7] vs MV_SINGLE_PASS 비교**

| 항목 | MV_SINGLE_PASS | FTS_MV_RANKED |
|---|---|---|
| **점수 최대값** | 175 | 225 |
| **FTS** | ❌ | ✅ (`search_vector @@`, `ts_rank_cd`) |
| **category 스코어** | ❌ (WHERE만) | ✅ (`× 15`) |
| **address 스코어** | ❌ (WHERE만) | ✅ (`× 5`) |
| **WHERE 조건 수** | 4개 OR | 4개 OR (FTS로 교체) |
| **JIT Functions** | 12 (with loc) | 12 (with loc) |
| **인덱스 (with loc)** | GiST | GiST |
| **추가 인덱스** | 없음 | `idx_restaurant_search_mv_fts_active` |
| **Seq Scan (without loc)** | ✅ | ✅ |

FTS_MV_RANKED는 MV_SINGLE_PASS 대비 **JIT Functions 수는 동일하나, 점수 표현력이 더 높다.** `ts_rank_cd` 추가로 인해 동일 후보 집합에서 순위 품질이 개선된다.

---

# **[8] 한계 및 트레이드오프**

| 상황 | 특성 |
|---|---|
| **위치 ON + 반경 좁음** | GiST → 소수 후보 → OR filter 비용 최소 → **최적** |
| **위치 ON + 반경 넓음** | 후보 증가 → OR 조건 비용 증가, `ts_rank_cd` 재계산 반복 |
| **위치 OFF** | 전체 Seq Scan (OR 조건으로 GIN BitmapOr 미활성) — MV_SINGLE_PASS와 동일 |
| **`simple` dictionary** | 한국어 어간 분리 없음 — "치킨집" 검색 시 "치킨" 토큰과 불일치 가능 |
| **MV staleness** | 15분 refresh 주기 → 신규 등록 음식점 즉시 반영 안 됨 |
| **JIT 첫 요청** | JIT 컴파일 ~1초 오버헤드 (캐시 후 수십ms로 감소) |
