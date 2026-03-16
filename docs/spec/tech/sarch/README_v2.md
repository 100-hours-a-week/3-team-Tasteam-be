| 항목 | 내용 |
|---|---|
| 문서 제목 | 검색(Search) 테크 스펙 v2 — FTS + MV + 캐시 성능 최적화 |
| 문서 목적 | FTS(tsvector), 스코어링 개선, MV 자동 갱신, 첫 페이지 캐시 적용 내역 및 운영 기준 명문화 |
| 작성 및 관리 | Backend Team |
| 최초 작성일 | 2026.03.13 |
| 최종 수정일 | 2026.03.16 |
| 문서 버전 | v2.2 |
| 기반 브랜치 | `feat/#search/fts-mv-ranked-optimization` |

<br>

# 검색(Search) 성능 최적화 테크스펙 v2.2

<br>

---

# **[1] 배경 및 목표 (Background & Objective)**

## **[1-1] 이전 버전(v1) 요약**

v1(2026-01-18)은 MVP 통합 검색 API 설계를 다룬다. 기술 스택은 PostgreSQL + PostGIS + pg_trgm 이었으며, 검색 전략 프레임워크(`SearchQueryStrategy` enum)를 도입해 7개 전략을 A/B 비교 가능하게 유지했다.

v1 시점의 벤치마크 결과(10K 데이터, 2026-03-10 EXPLAIN 분석):

| 전략 | Execution Time | JIT Functions | 비고 |
|---|---|---|---|
| HYBRID_SPLIT_CANDIDATES | 2,384ms | 81 | 4 CTE UNION + correlated subquery |
| READ_MODEL_TWO_STEP | 2,302ms | 60 | MV 사용, 4 CTE UNION |
| GEO_FIRST_HYBRID | 1,807ms | 46 | EXISTS correlated subquery 반복 |
| **MV_SINGLE_PASS** | **1,402ms** | **12** | 단일 CTE, scoring 선계산 — v1 최적 |
| TWO_STEP (기본값) | ~325ms | N/A | QueryDSL, JIT 없음, 소규모 유리 |

MV_SINGLE_PASS는 JIT Functions를 기존 대비 80~85% 감소시켰다. 그러나 다음 한계가 남았다:

1. **FTS 미사용**: `restaurant_search_mv`에 `tsvector` 컬럼 없음 — 카테고리명 복합 토큰, 주소 토큰 recall 낮음
2. **스코어링 불완전**: `category_match`, `address_match`가 SELECT에는 있지만 `total_score`에 미반영
3. **MV 자동 갱신 없음**: `restaurant_search_mv` static → restaurant 변경 후 검색 결과 stale
4. **검색 캐시 없음**: 인기 검색어 첫 페이지가 매 요청마다 DB 조회

## **[1-2] v2 목표**

| 목표 | 지표 |
|---|---|
| FTS(tsvector) 도입으로 recall 향상 | category/address 매칭 recall ↑ |
| 스코어링 완전성 — category/address 가중치 반영 | `total_score`에 `category_match*15 + address_match*5` 포함 |
| MV 자동 갱신 | `REFRESH MATERIALIZED VIEW CONCURRENTLY` 주기적 실행 |
| 첫 페이지 캐시 | Caffeine, TTL 5분, cursor==null 조건 |
| 기존 전략 A/B 비교 유지 | `FTS_MV_RANKED` 신규 전략 추가, 기존 7개 전략 그대로 유지 |

<br>

---

# **[2] 변경 사항 요약 (What Changed)**

## **[2-1] 변경 파일 목록**

| 파일 | 유형 | 주요 변경 |
|---|---|---|
| `db/migration/V202603131200__add_fts_tsvector_to_restaurant_search_mv.sql` | 신규 | MV 재정의 + `search_vector tsvector` 컬럼 + FTS GIN 인덱스 |
| `domain/search/repository/SearchQueryStrategy.java` | 수정 | `FTS_MV_RANKED` enum 값 추가 |
| `domain/search/dto/SearchCursor.java` | 수정 | `ftsRank` 필드 추가 |
| `domain/search/dto/SearchRestaurantCursorRow.java` | 수정 | `ftsRank` 필드 추가 |
| `domain/search/repository/SearchQueryRepository.java` | 수정 | `searchRestaurantsByKeywordWithFallback()` 메서드 추가 |
| `domain/search/repository/impl/SearchQueryRepositoryImpl.java` + 실행기들 | 수정 | `SearchQueryRepositoryImpl` 라우터 + 전략별 `SearchQueryExecutor` 등록 + `FtsMvRankedExecutor`, `NativeSearchExecutorSupport` 정비 + `searchRestaurantsByKeywordWithFallback()` 구현 |
| `domain/search/repository/SearchQueryProperties.java` | 수정 | `fallbackStrategy` 필드 추가 |
| `domain/search/service/SearchDataService.java` | 수정 | `ftsRank` 커서 설정 + `@Cacheable` 첫 페이지 캐시 + `fetchRestaurantsWithFallback()` 추가 |
| `domain/search/service/SearchService.java` | 수정 | CompletableFuture 병렬 실행 + TimeoutException 시 fallback 전환 |
| `domain/search/scheduler/SearchMvRefreshScheduler.java` | 신규 | MV CONCURRENTLY refresh 스케줄러 |
| `resources/application.yml` | 수정 | 기본 전략 `FTS_MV_RANKED`, fallback 전략 `ONE_STEP`, MV refresh 설정, 캐시 TTL 항목 추가 |
| `test/.../SearchStrategyBenchmarkTest.java` | 신규 | 전략별 벤치마크 (1000건 bulk, avg/p95/p99 측정) |

<br>

---

# **[3] 설계 상세 (Architecture & Design)**

## **[3-1] DB Migration — MV tsvector 추가**

**파일**: `app-api/src/main/resources/db/migration/V202603131200__add_fts_tsvector_to_restaurant_search_mv.sql`

PostgreSQL Materialized View는 `ALTER ... ADD COLUMN`을 지원하지 않으므로 DROP/RECREATE 패턴을 사용한다.

**MV 신규 컬럼 `search_vector`**:

```sql
setweight(to_tsvector('simple', coalesce(lower(r.name), '')), 'A')
|| setweight(to_tsvector('simple', coalesce(
    array_to_string(category_names, ' '), '')), 'B')
|| setweight(to_tsvector('simple', coalesce(lower(r.full_address), '')), 'C')
AS search_vector
```

**가중치 배분**:

| 필드 | FTS Weight | 의미 |
|---|---|---|
| `name` | A (최고) | 음식점 이름이 가장 중요한 매칭 신호 |
| `category_names` | B (중간) | 카테고리 키워드 복합 토큰 매칭 |
| `full_address` | C (낮음) | 주소 토큰 매칭 (보조 신호) |

**`simple` dictionary 선택 이유**:
- `english` dictionary: 영어 어간 추출 → 한국어에 부적합
- `simple` dictionary: lowercase 변환만 수행 → 한국어 토큰 그대로 통과
- 한글 형태소 분리는 여전히 pg_trgm trigram이 담당; FTS는 단어 단위 exact/prefix 매칭 보완

**인덱스 목록** (재생성 + 신규):

| 인덱스 명 | 타입 | 컬럼 | 조건 | 용도 |
|---|---|---|---|---|
| `idx_restaurant_search_mv_restaurant_id` | UNIQUE | `restaurant_id` | - | CONCURRENTLY 갱신 필수 |
| `idx_restaurant_search_mv_name_trgm_active` | GIN (trgm) | `name_lower` | `deleted_at IS NULL` | trigram 유사도 검색 |
| `idx_restaurant_search_mv_addr_trgm_active` | GIN (trgm) | `addr_lower` | `deleted_at IS NULL` | 주소 trigram |
| `idx_restaurant_search_mv_category_names_active` | GIN | `category_names` | `deleted_at IS NULL` | 카테고리 배열 포함 검사 |
| `idx_restaurant_search_mv_geography_active` | GiST | `geography(location)` | `deleted_at IS NULL` | 공간 범위 검색 |
| `idx_restaurant_search_mv_fts_active` | GIN | `search_vector` | `deleted_at IS NULL` | **신규** FTS 토큰 검색 |

**배포 주의사항**: Migration 실행 중 MV 기반 전략(`MV_SINGLE_PASS`, `READ_MODEL_TWO_STEP`) 사용 불가. 배포 시 전략을 `TWO_STEP`으로 임시 전환 후 Migration 완료 후 복구 권장.

<br>

## **[3-2] 신규 전략 — FTS_MV_RANKED**

### 전략 등록

```java
public enum SearchQueryStrategy {
    ONE_STEP, TWO_STEP, JOIN_AGGREGATE,
    HYBRID_SPLIT_CANDIDATES, GEO_FIRST_HYBRID,
    READ_MODEL_TWO_STEP, MV_SINGLE_PASS,
    FTS_MV_RANKED  // 신규
}
```

### 스코어링 공식 비교

**기존 MV_SINGLE_PASS**:
```
total_score = name_exact * 100 + similarity * 30 + distance_weight * 50
```

**신규 FTS_MV_RANKED**:
```
total_score =
    name_exact      * 100.0   -- 이름 정확 매칭 보너스
  + similarity      *  30.0   -- pg_trgm trigram 유사도 (0~1)
  + fts_rank        *  25.0   -- ts_rank_cd FTS 관련도 (0~1 근사)
  + category_match  *  15.0   -- 카테고리 정확 매칭 (기존 미반영 → 신규 반영)
  + address_match   *   5.0   -- 주소 포함 매칭 (기존 미반영 → 신규 반영)
  + distance_weight *  50.0   -- 거리 가중치 (반경 내 거리 기반)
-- 이론적 최대값: 225 (위치 있음) / 175 (위치 없음)
```

**변경 포인트**: `category_match(15)`, `address_match(5)`는 v1에서 SELECT에는 반환했으나 `total_score`에 반영하지 않았다. v2에서 이를 스코어에 포함해 카테고리 키워드 검색("치킨" 카테고리 검색) 시 올바르게 상위 노출된다.

### SQL 구조

```sql
WITH candidates AS (
    SELECT
        mv.restaurant_id,
        mv.updated_at,
        CASE WHEN mv.name_lower = :kw THEN 1 ELSE 0 END                  AS name_exact,
        similarity(mv.name_lower, :kw)::double precision                  AS name_similarity,
        ts_rank_cd(mv.search_vector, plainto_tsquery('simple', :kw))
            ::double precision                                             AS fts_rank,
        <distance_expr>                                                    AS distance_meters,
        CASE WHEN mv.category_names @> ARRAY[:kw]::text[] THEN 1 ELSE 0 END AS category_match,
        CASE WHEN mv.addr_lower LIKE '%' || :kw || '%' THEN 1 ELSE 0 END    AS address_match,
        (
            CASE WHEN mv.name_lower = :kw THEN 1 ELSE 0 END * 100.0
            + similarity(mv.name_lower, :kw)::double precision * 30.0
            + ts_rank_cd(mv.search_vector, plainto_tsquery('simple', :kw))
                ::double precision * 25.0
            + CASE WHEN mv.category_names @> ARRAY[:kw]::text[] THEN 1 ELSE 0 END * 15.0
            + CASE WHEN mv.addr_lower LIKE '%' || :kw || '%' THEN 1 ELSE 0 END * 5.0
            + <distance_score>
        ) AS total_score
    FROM restaurant_search_mv mv
    WHERE mv.deleted_at IS NULL
      <geo_filter>
      AND (
            mv.name_lower LIKE '%' || :kw || '%'
            OR mv.name_lower % :kw
            OR mv.search_vector @@ plainto_tsquery('simple', :kw)  -- FTS 조건 추가
            OR mv.category_names @> ARRAY[:kw]::text[]
          )
    ORDER BY total_score DESC, mv.updated_at DESC, mv.restaurant_id DESC
    LIMIT :text_candidate_limit
)
SELECT restaurant_id, name_exact, name_similarity, fts_rank,
       distance_meters, category_match, address_match
FROM candidates
WHERE (
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

> **주의**: cursor 파라미터에 `CAST`가 필수다. PostgreSQL은 `null IS NULL` 패턴에서 첫 번째 파라미터 참조 시 타입을 추론하는데, `IS NULL` 단독으로는 타입 정보가 없어 `42P18: could not determine data type of parameter $N` 에러가 발생한다. 자세한 내용은 [트러블슈팅 문서](../../../../troubleshooting/search-null-cursor-param-type-inference-20260313.md) 참고.

**`plainto_tsquery` 선택 이유**:
- `to_tsquery`는 `&`, `|`, `!` 연산자를 직접 써야 하며, 사용자 입력의 특수문자에 취약
- `plainto_tsquery`는 입력을 AND로 결합하고 특수문자를 안전하게 처리 → 사용자 입력에 적합

<br>

## **[3-3] DTO 변경 — ftsRank 필드 추가**

### SearchCursor.java

```java
public record SearchCursor(
    Integer nameExact,
    Double nameSimilarity,
    Double ftsRank,          // 신규 — legacy 전략은 null
    Double distanceMeters,
    Integer categoryMatch,
    Integer addressMatch,
    Instant updatedAt,
    Long id) {}
```

### SearchRestaurantCursorRow.java

```java
public record SearchRestaurantCursorRow(
    Restaurant restaurant,
    Integer nameExact,
    Double nameSimilarity,
    Double ftsRank,          // 신규
    Double distanceMeters,
    Integer categoryMatch,
    Integer addressMatch) {}
```

**하위 호환성**: 기존 전략(ONE_STEP~MV_SINGLE_PASS)은 `ftsRank=null`로 반환. QueryDSL 전략에서는 `Expressions.nullExpression(Double.class)` 사용. FTS 포함/미포함 모두 `NativeSearchExecutorSupport`의 `cursorScore()`/`cursorScoreFts()` 경로로 나뉘어 처리하며, 기존 `cursorScore()` 호출은 유지한다.

<br>

## **[3-4] 첫 페이지 캐시**

**파일**: `SearchDataService.java`

```java
@Cacheable(
    cacheNames = "search-restaurant-first-page",
    key = "T(String).format('%s_%.2f_%.2f_%.0f',
           #keyword,
           #latitude  != null ? #latitude  : 0.0,
           #longitude != null ? #longitude : 0.0,
           #radiusMeters != null ? #radiusMeters : 0.0)",
    condition = "#cursorToken == null"
)
@Transactional(readOnly = true)
public RestaurantPageData fetchRestaurants(
        String keyword, String cursorToken, int pageSize,
        Double latitude, Double longitude, Double radiusMeters) { ... }
```

**설정** (`application.yml`):

```yaml
tasteam:
  local-cache:
    ttl:
      search-restaurant-first-page:
        ttl: 5m
```

**적용 범위**: `cursorToken == null`인 첫 페이지 요청만 캐싱. 페이지네이션(2페이지 이상)은 캐시 무효 → 동적 커서 계산 유지.

**캐시 키 구성**: `{keyword}_{lat:.2f}_{lon:.2f}_{radius:.0f}` — 위치가 없을 때 `0.00_0.00_0`으로 처리.

<br>

## **[3-5] MV 자동 갱신 스케줄러**

**파일**: `SearchMvRefreshScheduler.java`

```java
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "tasteam.search.mv-refresh.enabled", havingValue = "true")
public class SearchMvRefreshScheduler {

    private final JdbcTemplate jdbcTemplate;

    @Scheduled(fixedDelayString = "${tasteam.search.mv-refresh.fixed-delay:PT15M}")
    public void refreshSearchMv() {
        long start = System.currentTimeMillis();
        jdbcTemplate.execute(
            "REFRESH MATERIALIZED VIEW CONCURRENTLY public.restaurant_search_mv");
        log.info("restaurant_search_mv refreshed in {}ms", System.currentTimeMillis() - start);
    }
}
```

**설정** (`application.yml`):

```yaml
tasteam:
  search:
    mv-refresh:
      enabled: ${TASTEAM_SEARCH_MV_REFRESH_ENABLED:false}
      fixed-delay: ${TASTEAM_SEARCH_MV_REFRESH_FIXED_DELAY:PT15M}
```

**`CONCURRENTLY` 동작 조건**: `idx_restaurant_search_mv_restaurant_id` UNIQUE 인덱스 필요 (Migration에서 생성). 읽기 잠금 없이 MV 갱신 가능.

**기본값 `enabled=false`**: 환경 변수 `TASTEAM_SEARCH_MV_REFRESH_ENABLED=true`로 활성화. 개발/테스트 환경에서는 off.

<br>

## **[3-6] 전략 선택 구성**

```yaml
tasteam:
  search:
    query:
      strategy: ${TASTEAM_SEARCH_QUERY_STRATEGY:FTS_MV_RANKED}
      fallback-strategy: ${TASTEAM_SEARCH_QUERY_FALLBACK_STRATEGY:ONE_STEP}
      candidate-limit: ${TASTEAM_SEARCH_QUERY_CANDIDATE_LIMIT:200}
```

**기본 전략 변경**: v1 `TWO_STEP` → v2 `FTS_MV_RANKED`

| 환경 변수 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `TASTEAM_SEARCH_QUERY_STRATEGY` | `SearchQueryStrategy` enum | `FTS_MV_RANKED` | 검색 전략 |
| `TASTEAM_SEARCH_QUERY_FALLBACK_STRATEGY` | `SearchQueryStrategy` enum | `ONE_STEP` | 타임아웃 시 폴백 전략 |
| `TASTEAM_SEARCH_QUERY_CANDIDATE_LIMIT` | int | `200` | candidates CTE LIMIT 기준 |
| `TASTEAM_SEARCH_MV_REFRESH_ENABLED` | boolean | `false` | MV 갱신 스케줄러 활성화 |
| `TASTEAM_SEARCH_MV_REFRESH_FIXED_DELAY` | Duration | `PT15M` | MV 갱신 간격 |

<br>

## **[3-7] Fallback 전략**

주 전략(`FTS_MV_RANKED`)이 타임아웃될 경우 안정적인 폴백 전략으로 자동 전환한다.

### 동작 흐름

```
SearchService.search()
  ├─ CompletableFuture 병렬 실행 (timeout: 3초)
  │    ├─ fetchGroups()
  │    └─ fetchRestaurants() → FTS_MV_RANKED 전략
  │
  └─ TimeoutException 발생 시
       ├─ Future 취소
       ├─ fetchGroups() 동기 재실행
       └─ fetchRestaurantsWithFallback() → ONE_STEP 전략
```

### 관련 클래스

- `SearchQueryProperties`: `fallbackStrategy` 필드 (`tasteam.search.query.fallback-strategy`)
- `SearchQueryRepository`: `searchRestaurantsByKeywordWithFallback()` 메서드
- `SearchQueryRepositoryImpl`: `getFallbackStrategy()`로 executor 선택
- `SearchDataService`: `fetchRestaurantsWithFallback()` → `fetchRestaurantsInternal(..., useFallback=true)`
- `SearchService`: `TimeoutException` catch 블록에서 fallback 호출

<br>

---

# **[4] 전략 전체 비교 (Strategy Reference)**

v2 기준 8개 전략 전체 비교표:

| 전략 | 데이터 소스 | 구조 | FTS | category/address in score | JIT Functions | 권장 상황 |
|---|---|---|---|---|---|---|
| `ONE_STEP` | restaurant 테이블 | QueryDSL 단일 | ❌ | ❌ | N/A | 소규모, 단순 |
| `TWO_STEP` | restaurant 테이블 | QueryDSL 2단계 | ❌ | ❌ | N/A | JIT off 환경 |
| `JOIN_AGGREGATE` | restaurant 테이블 | QueryDSL LEFT JOIN | ❌ | ❌ | N/A | 카테고리 우선 |
| `HYBRID_SPLIT_CANDIDATES` | restaurant 테이블 | 4 CTE UNION + INTERSECT | ❌ | ❌ | ~81 | 미권장 |
| `GEO_FIRST_HYBRID` | restaurant 테이블 | GIS 우선 + text filter | ❌ | ❌ | ~46 | 협소 반경 |
| `READ_MODEL_TWO_STEP` | MV | 4 CTE UNION | ❌ | ❌ | ~60 | 중간 규모 |
| `MV_SINGLE_PASS` | MV | 단일 CTE, scoring 선계산 | ❌ | ❌ | ~12 | 위치 검색 (v1 최적) |
| **`FTS_MV_RANKED`** | **MV** | **단일 CTE + FTS** | **✅** | **✅** | **~12** | **기본값 (v2 권장)** |

**FTS_MV_RANKED vs MV_SINGLE_PASS 차이점**:

| 항목 | MV_SINGLE_PASS | FTS_MV_RANKED |
|---|---|---|
| FTS WHERE 조건 | 없음 | `OR mv.search_vector @@ plainto_tsquery('simple', :kw)` 추가 |
| fts_rank 컬럼 | 없음 | `ts_rank_cd(search_vector, query)` |
| category 스코어 | 0 (반환만) | `* 15.0` 반영 |
| address 스코어 | 0 (반환만) | `* 5.0` 반영 |
| total_score 최대 | 175 | 225 |

<br>

---

# **[5] 테스트 (Testing)**

## **[5-1] 단위 테스트 — SearchQueryRepositoryTest**

**파일**: `test/.../SearchQueryRepositoryTest.java`
**어노테이션**: `@RepositoryJpaTest`

검증 항목:
- 이름 조건 + 거리 조건 + 스코어 정렬이 계약 SQL 결과와 일치
- 삭제된 식당(`deleted_at IS NOT NULL`) 결과 제외
- 반경 외 식당 결과 제외

## **[5-2] 성능 벤치마크 — SearchStrategyBenchmarkTest**

**파일**: `test/.../SearchStrategyBenchmarkTest.java`
**어노테이션**: `@PerformanceTest` (SpringBootTest + Testcontainers), `@Tag("perf")`

**데이터 구성** (bulk insert 1000건):

| 유형 | 건수 | 이름 패턴 | 위치 | 목적 |
|---|---|---|---|---|
| 정확 매칭 | 10 | `치킨` | 강남구 반경 내 | name_exact=1 |
| 유사 매칭 | 50 | `치킨N` | 강남구 반경 내 | name_similarity 검증 |
| 카테고리 후보 | 30 | `bench_pizzaN` | 강남구 반경 내 | 카테고리 무관 노이즈 |
| 무관 | 910 | `bench_otherN` | 부산 해운대 | 반경 외 노이즈 |

**측정 항목**:
- warmup 5회 → 측정 50회
- avg / p95 / p99 레이턴시 (ms)
- 결과 비어있지 않음 검증
- 상위 5개 ID 로깅 (전략 간 일관성 육안 비교)

**실행 대상 전략**: `TWO_STEP`, `MV_SINGLE_PASS`, `FTS_MV_RANKED`

**실행 명령**:

```bash
./gradlew test --tests SearchStrategyBenchmarkTest
```

또는 태그 기반:

```bash
./gradlew test -Pgroups=perf
```

<br>

---

# **[6] 검증 체크리스트 (Verification Checklist)**

| 항목 | 검증 방법 | 기대 결과 |
|---|---|---|
| Migration 적용 | `SELECT search_vector FROM restaurant_search_mv LIMIT 1` | `search_vector` 컬럼 존재 |
| FTS 인덱스 동작 | `EXPLAIN ANALYZE SELECT ... WHERE search_vector @@ plainto_tsquery('simple', '치킨')` | `Bitmap Index Scan on idx_restaurant_search_mv_fts_active` |
| 전략 전환 | `application.yml strategy: FTS_MV_RANKED` → 검색 API 호출 | 결과 정상 반환 |
| **첫 페이지 500 없음** | cursor 없이 `GET /api/v1/search?keyword=test&latitude=37.5&longitude=126.9&radiusKm=1` | **200 응답**, 500 에러 없음 |
| 스코어링 | 정확 매칭 식당(`name='치킨'`)이 유사 매칭보다 상위 노출 | `name_exact=1` 결과가 1위 |
| 첫 페이지 캐시 | 동일 keyword 2회 요청 → `LOCAL_CACHE_CAFFEINE_RECORD_STATS=true` 후 캐시 hit 확인 | `hitCount` 증가 |
| MV 갱신 | `TASTEAM_SEARCH_MV_REFRESH_ENABLED=true` 설정 → 15분 주기 갱신 | 로그 `restaurant_search_mv refreshed in Nms` |
| 하위 호환 | `strategy: TWO_STEP`으로 롤백 → 검색 정상 동작 | ftsRank=null 커서 처리 정상 |

<br>

---

# **[7] 운영 고려사항 (Operations)**

## **[7-1] MV 갱신 정책**

- 기본 비활성화 (`enabled=false`) — 프로덕션 활성화 시 `TASTEAM_SEARCH_MV_REFRESH_ENABLED=true`
- `CONCURRENTLY` 갱신은 읽기 잠금 없이 동작하나 추가 I/O 발생
- 갱신 주기(`PT15M`)는 restaurant 수정 빈도와 검색 결과 신선도 요구사항에 따라 조정

## **[7-2] 배포 시 Migration 주의**

MV DROP/RECREATE가 포함된 Migration 적용 중에는 MV 기반 전략이 동작하지 않는다.
배포 절차:
1. 환경 변수 `TASTEAM_SEARCH_QUERY_STRATEGY=TWO_STEP`으로 변경 (롤백 대비)
2. Flyway Migration 실행 (`V202603131200__...`)
3. MV 재생성 완료 확인
4. 환경 변수 `TASTEAM_SEARCH_QUERY_STRATEGY=FTS_MV_RANKED`로 복구

## **[7-3] 캐시 무효화**

Caffeine 캐시는 TTL 기반(5분)으로 자동 만료된다. 수동 무효화가 필요한 경우:
- 서버 재시작
- 또는 Actuator 캐시 evict 엔드포인트 활용 (설정 필요)

## **[7-4] native query null 파라미터 주의사항**

native SQL에서 `cursor == null` 시 파라미터가 null로 바인딩된다. PostgreSQL은 `IS NULL` 단독 절에서 파라미터 타입을 추론하지 못해 `42P18` 에러가 발생한다.

**규칙**: null이 될 수 있는 파라미터는 항상 `CAST` 명시:

```sql
-- ❌ 금지 — null 바인딩 시 42P18 에러
:cursor_score IS NULL OR total_score < :cursor_score

-- ✅ 필수 — CAST로 타입 명시
CAST(:cursor_score AS double precision) IS NULL
OR total_score < CAST(:cursor_score AS double precision)
```

참고: [트러블슈팅 문서](../../../../troubleshooting/search-null-cursor-param-type-inference-20260313.md)

## **[7-5] 타임아웃/폴백**

- 주 검색 쿼리(그룹 + 음식점 병렬) **3초** 초과 시 `ONE_STEP` 폴백 자동 전환
- `TASTEAM_SEARCH_QUERY_FALLBACK_STRATEGY`로 폴백 전략 변경 가능
- 폴백 발생 시 별도 지표 수집 고려 (`search.fallback.triggered` 카운터 등)

## **[7-6] 모니터링**

| 지표 | 확인 방법 | 임계값 |
|---|---|---|
| 검색 p95 레이턴시 | Grafana / AOP `service-performance` 로그 | < 150ms |
| 캐시 hit rate | `LOCAL_CACHE_CAFFEINE_RECORD_STATS=true` → `/actuator/caches` | > 50% (인기 검색어) |
| MV 갱신 시간 | `restaurant_search_mv refreshed in Nms` 로그 | < 1000ms |
| FTS 인덱스 사용 여부 | slow query 모니터링 + EXPLAIN | Seq Scan 없이 GIN 사용 |

<br>

---

# **[8] 남은 과제 (Open Items)**

1. **100K 데이터 FTS_MV_RANKED EXPLAIN 재검증** — 10K 기준 벤치마크는 완료. 대규모 데이터에서 `plainto_tsquery` + GIN 인덱스 효율 검증 필요
2. **location OFF FTS OR 조건 최적화** — FTS 조건 추가 후에도 `OR` 구조로 인해 GIN BitmapOr 미결합 가능. `UNION ALL` 분리 전략 탐색
3. **캐시 조건 확장** — 현재 첫 페이지만 캐시. pageSize별 캐시 키 확장 검토
4. **MV 갱신 이벤트 드리븐 전환** — 현재 스케줄 기반. restaurant 변경 이벤트 수신 시 즉시 갱신으로 전환 검토

<br>

---

# **[9] 변경이력**

| 버전 | 일자 | 작성자 | 변경 내역 |
|---|---|---|---|
| `v2.2` | 2026.03.16 | Backend Team | fallback 전략 아키텍처 추가 (SearchQueryProperties fallbackStrategy, SearchDataService fetchRestaurantsWithFallback, SearchService TimeoutException 처리) |
| `v2.1` | 2026.03.13 | Backend Team | native query NULL 커서 파라미터 CAST 수정 (42P18 첫 페이지 500 수정) — 5개 SQL 빌더 전체 적용 |
| `v2.0` | 2026.03.13 | Backend Team | FTS tsvector 도입, FTS_MV_RANKED 전략 추가, 스코어링 완전성 개선, MV 자동 갱신 스케줄러, 첫 페이지 Caffeine 캐시 |
| `v1.2` | 2026.01.18 | Backend Team | 템플릿 정합성 반영 (ERD/API/에러/테스트 섹션 정리) |
| `v1.1` | 2026.01.15 | Backend Team | 검색 중심 스펙 정리 |
