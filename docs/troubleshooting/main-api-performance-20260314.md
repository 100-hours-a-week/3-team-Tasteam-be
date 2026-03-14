# /api/v1/main 성능 최적화 트러블슈팅 로그 (Round 2)

> 작성일: 2026-03-14
> 대상 엔드포인트: `GET /api/v1/main/home`, `GET /api/v1/main/ai-recommend`
> 전편: `docs/troubleshooting/main-api-performance-20260309.md`
> 관련 PR: `perf/#main-api-optimization/home-and-ai-recommend`

---

## 1. 배경 및 문제 인식

1차 최적화(2026-03-09)에서 다음 항목을 개선했음에도 운영 측정 지연이 지속됐다:

| 엔드포인트 | 응답 시간 |
|---|---|
| `GET /api/v1/main/home` | **14.897 s** |
| `GET /api/v1/main/ai-recommend` | **10.760 s** |

1차 최적화에서 처리한 항목:
- `review.restaurant_id` 부분 인덱스 추가 (1,512ms → 119ms)
- `resolveLocation()` N+1 제거 → 단일 native 쿼리
- 섹션 및 보조 데이터 병렬화 (`CompletableFuture.allOf`)
- 위치 없는 요청 `@Cacheable` 캐시 적용

1차 후 잔여 개선 여지로 문서화된 항목:
1. `group_member.member_id` 단독 인덱스 미존재
2. 반경 확장 전략 (최악 섹션당 5 DB 쿼리)

추가로 코드 분석에서 **1차 때 발견하지 못한 구조적 문제** 2개를 발견했다.

---

## 2. 병목 분석

### 2.1 [CRITICAL] `@Transactional` + `CompletableFuture` — 커넥션 낭비

**발견 경위**: `spring.threads.virtual.enabled: true` + HikariCP `maximum-pool-size: 50` 조합에서
`MainService.getHome()`에 `@Transactional(readOnly=true)`가 붙어 있으면 아래 문제가 발생한다.

**동작 구조**:

```
HTTP 요청 스레드
  └─ Spring AOP (@Transactional 인터셉터)
       └─ Hibernate Session 열림 + HikariCP 커넥션 1개 획득 (conn-A)
            └─ CompletableFuture.supplyAsync(() -> fetchHotSection(), mainQueryExecutor)
                 → Virtual Thread #1 실행
                 → TransactionSynchronizationManager는 thread-local
                 → Virtual Thread에 트랜잭션 컨텍스트 없음
                 → MainDataService.fetchHotSectionByLocation() @Transactional 진입
                 → 새 트랜잭션 시작 + HikariCP 커넥션 획득 (conn-B)
            └─ CompletableFuture.supplyAsync(() -> fetchNewSection(), ...)
                 → Virtual Thread #2 → conn-C 획득
            └─ CompletableFuture.supplyAsync(() -> fetchCategories(), ...)
                 → Virtual Thread #3 → conn-D 획득
            ... 총 5개 future
       └─ conn-A: 모든 .join() 완료까지 아무 쿼리도 실행하지 않고 점유만
```

**getHome() 기준 동시 커넥션 소비**:
- HTTP 스레드 idle: 1개
- resolveLocation (groupMemberRepository 직접 호출): 1개
- hot + new 섹션 parallel: 2개
- cat + thumb + summary 메타데이터 parallel: 3개
- **합계: 요청 1건당 7개 동시 커넥션**

pool `maximum-pool-size: 50`에서 동시 7명 요청 시 49개 커넥션 소비.
8번째 요청은 `connection-timeout: 20000ms` 동안 대기 → **최대 20초 지연**.

**핵심**: HTTP 스레드의 커넥션(conn-A)은 `getHome()` 전체 수행 시간 동안 아무 것도 하지 않고 점유됨.
`@Transactional`이 실제로 트랜잭션을 사용하지 않음에도 커넥션 비용을 치름.

**수정**:

```java
// Before
@Transactional(readOnly = true)
public HomePageResponse getHome(Long memberId, MainPageRequest request) { ... }

// After
public HomePageResponse getHome(Long memberId, MainPageRequest request) { ... }
```

`MainDataService`의 `fetchXxxByLocation()`, `fetchXxxAll()` 메서드들은 이미 각자
`@Transactional(readOnly = true)`를 보유하고 있어 기능 변화 없음.
`resolveLocation()` 내부의 `groupMemberRepository` 호출도 Spring Data가 자체 트랜잭션 처리.

---

### 2.2 [HIGH] `getAiRecommend()` — AI 섹션 동기 실행

**코드 (`MainService.java:165-189`)**:

```java
// Before
public AiRecommendResponse getAiRecommend(Long memberId, MainPageRequest request) {
    LocationContext location = resolveLocation(memberId, request);

    List<MainRestaurantDistanceProjection> aiRestaurants = fetchAiSection(location); // ← 동기!

    List<Long> allIds = aiRestaurants.stream()...toList();

    CompletableFuture<...> catFuture = CompletableFuture.supplyAsync(...);  // 이후 병렬
    ...
}
```

`fetchAiSection()` → `MainDataService.fetchAiSectionByLocation()` → `fetchWithRadiusExpansion()`
→ 최악 5개 쿼리 순차 실행. 이 블록이 HTTP 스레드에서 동기적으로 수행되므로
`allIds`가 확정될 때까지 메타데이터 fetch가 시작될 수 없다.

`getHome()`은 hot/new를 `CompletableFuture.supplyAsync`로 즉시 dispatch하는 반면,
`getAiRecommend()`만 이 패턴을 누락했다.

**수정**:

```java
// After
public AiRecommendResponse getAiRecommend(Long memberId, MainPageRequest request) {
    LocationContext location = resolveLocation(memberId, request);

    List<MainRestaurantDistanceProjection> aiRestaurants = CompletableFuture
        .supplyAsync(() -> fetchAiSection(location), mainQueryExecutor)
        .join();
    ...
}
```

이제 AI 섹션 fetch가 `mainQueryExecutor` virtual thread에서 실행되어
`@Transactional` 제거(2.1)와 함께 HTTP 스레드 커넥션 낭비가 제거된다.

---

### 2.3 [HIGH] `group_member.member_id` 단독 인덱스 미존재

1차 최적화 잔여 항목. 운영 DB에 `group_member` 46K 행 기준 EXPLAIN:

```text
-- findFirstGroupLocationByMemberId
Limit  (actual time=1.761..1.815 rows=1 loops=1)
  -> Nested Loop
       -> Index Scan Backward using group_member_pkey on group_member gm
            Filter: ((deleted_at IS NULL) AND (member_id = 1370551))
            Rows Removed by Filter: 3,313   ← member_id 단독 인덱스 없어서 PK 역방향 전체 탐색
       -> Index Scan using group_pkey on "group" g
```

- 기존 unique index: `(group_id, member_id)` — `group_id`가 선행 컬럼이라 `member_id` 단독 조건 사용 불가
- 현재 PK 역방향 스캔으로 `member_id` 조건에 맞는 행을 찾을 때까지 3,313행 필터링
- 매 로그인 요청 (`getHome`, `getAiRecommend` 공통)에서 발생

**인덱스 설계**:

```sql
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_group_member_member_id_active
    ON group_member (member_id, id DESC)
    WHERE deleted_at IS NULL;
```

- `member_id` 선행: 해당 회원의 행만 직접 seek
- `id DESC` 후행: `ORDER BY gm.id DESC LIMIT 1` 처리를 index-only scan으로 처리 가능
- `WHERE deleted_at IS NULL`: 소프트삭제 행 제외, 인덱스 크기 최소화
- `CONCURRENTLY`: 운영 중 무중단 생성

**기대 효과**: 3,313행 filter walk → 1-2 page read (index seek)

**마이그레이션**: `V202603141000__add_index_group_member_member_id.sql`

---

### 2.4 [MEDIUM] 반경 확장 전략 — 섹션당 최악 5 쿼리

1차 최적화 잔여 항목.

**기존 동작 (`MainDataService.fetchWithRadiusExpansion()`)**:

```java
for (int radius : {3_000, 5_000, 10_000, 20_000}) {
    results = query.execute(lat, lon, radius, SECTION_SIZE=20);
    // 20개 이상이면 조기 반환
    if (collected.size() >= SECTION_SIZE) return;
}
fillWithRandom(...);  // 여전히 부족하면 5번째 쿼리
```

데이터 희소 지역(지방 소도시, 테스트 환경): 4개 반경 모두 소진 + random fill = 섹션당 5 쿼리.
`getHome()` 최악: hot(5) + new(5) = **10 DB 쿼리** (future 병렬이지만 각 future 내부는 순차).

**개선**:

```java
// 단일 최대 반경(20km) 쿼리 + 부족 시만 random fill
private List<...> fetchWithRadiusExpansion(double lat, double lon, LocationQuery query) {
    int maxRadius = EXPANDED_RADII[EXPANDED_RADII.length - 1];  // 20,000m
    List<...> results = query.execute(lat, lon, maxRadius, SECTION_SIZE);

    if (results.size() >= SECTION_SIZE) {
        return results;
    }

    LinkedHashMap<Long, ...> collected = new LinkedHashMap<>();
    results.forEach(r -> collected.put(r.getId(), r));
    fillWithRandom(collected, SECTION_SIZE - collected.size());
    return new ArrayList<>(collected.values());
}
```

| | 기존 | 개선 후 |
|---|---|---|
| 최선 (밀집 지역) | 1 쿼리 (3km에서 조기 종료) | 1 쿼리 (20km 내 20개 이상) |
| 최악 (희소 지역) | 5 쿼리 (4 반경 + random) | **2 쿼리** (1 geo + 1 random) |

**트레이드오프**: 밀집 지역에서 3km 대신 20km 스캔이 발생하나,
GiST geography 인덱스가 DWithin 공간 필터링을 효율적으로 처리하므로 실용적 차이 미미.
오히려 1차 최적화로 `idx_review_restaurant_id_active`가 추가된 이후 join 비용이 줄어
20km 단일 쿼리가 이전 3km 쿼리보다 빠를 수 있다.

---

### 2.5 [LOW] ST_Distance 과잉 계산

**기존 쿼리 패턴** (findHotRestaurants 예시):

```sql
SELECT r.id, r.name,
  ST_Distance(r.location::geography, ST_SetSRID(...)) as distanceMeter  -- 모든 DWithin 통과 행
FROM restaurant r
LEFT JOIN review rv ON ...
WHERE ST_DWithin(...)   -- 20km 내 수백 행 통과
GROUP BY r.id
ORDER BY count(rv.id) DESC
LIMIT 20
```

`ST_Distance`를 DWithin 통과 전체 행(최대 수백 건)에 계산한 후 `LIMIT 20` 적용.

**CTE 패턴 적용**:

```sql
WITH ranked AS (
  SELECT r.id, r.name, r.location       -- location만 SELECT
  FROM restaurant r
  LEFT JOIN review rv ON ...
  WHERE ST_DWithin(...)
  GROUP BY r.id, r.name, r.location
  ORDER BY count(rv.id) DESC, r.id ASC
  LIMIT 20                               -- 먼저 20개 선별
)
SELECT id, name,
  ST_Distance(location::geography, ...) as distanceMeter  -- 20개에만 계산
FROM ranked
```

`ST_Distance` 계산 대상: 전체 DWithin 통과 행 → **상위 20개**로 감소.
geography 캐스트(`location::geography`) 중복 연산도 함께 감소.

대상: `findHotRestaurants`, `findNewRestaurants`, `findAiRecommendRestaurants`

---

## 3. 변경 내용 요약

### 3.1 변경 파일

| 파일 | 변경 내용 | 커밋 |
|------|-----------|------|
| `V202603141000__add_index_group_member_member_id.sql` (신규) | group_member(member_id, id DESC) 부분 인덱스 | 1st |
| `MainService.java` | @Transactional 3개 제거, getAiRecommend 비동기화 | 2nd |
| `MainDataService.java` | 반경 확장 loop → 단일 최대 반경 쿼리 | 3rd |
| `RestaurantRepository.java` | findHot/New/AiRecommend 3개 쿼리 CTE 패턴 | 4th |

### 3.2 아키텍처 변화

```text
[기존 getAiRecommend()]
HTTP 스레드
  ├─ resolveLocation()         (1 쿼리)
  ├─ fetchAiSection()          (최악 5 쿼리 순차, HTTP 스레드에서)
  └─ [병렬] cat/thumb/summary  (3 쿼리)
  총 커넥션: HTTP idle 1 + 5 + 3 = 9개 동시 (최악)

[개선 후 getAiRecommend()]
HTTP 스레드
  ├─ resolveLocation()                    (1 쿼리)
  └─ supplyAsync(fetchAiSection).join()   (최악 2 쿼리, virtual thread에서)
     → [병렬] cat/thumb/summary            (3 쿼리)
  총 커넥션: 2 + 3 = 5개 동시 (최악)
```

```text
[기존 getHome()]
HTTP 스레드 커넥션 1개 idle 점유 (@Transactional)
  + resolveLocation 1
  + hot/new 2
  + cat/thumb/summary 3
  = 7개 동시

[개선 후 getHome()]
@Transactional 제거 → HTTP 스레드 idle 커넥션 없음
  resolveLocation 1
  + hot/new 2
  + cat/thumb/summary 3
  = 6개 동시
```

---

## 4. 예상 효과

| 문제 | 현상 | 개선 후 기대 |
|------|------|------------|
| @Transactional idle 커넥션 | 요청당 1개 불필요 점유, pool 포화 임계값 낮춤 | 제거 |
| getAiRecommend AI 섹션 동기 | HTTP 스레드에서 최악 5 순차 쿼리 후 메타데이터 시작 | virtual thread에서 비동기 실행 |
| group_member 스캔 | 3,313행 필터 walk (~1.8ms) | 인덱스 seek (< 0.5ms 예상) |
| 반경 확장 최악 케이스 | 5 쿼리/섹션 | 2 쿼리/섹션 |
| ST_Distance 과잉 계산 | DWithin 통과 전체 행 계산 | 상위 20행에만 계산 |

---

## 5. 검증 방법

```bash
# 변경된 코드 관련 테스트
./gradlew test --tests "com.tasteam.domain.main.*"

# 인덱스 적용 확인 (운영/dev DB)
EXPLAIN (ANALYZE, BUFFERS)
SELECT ST_Y(g.location), ST_X(g.location)
FROM group_member gm
JOIN "group" g ON g.id = gm.group_id
WHERE gm.member_id = :memberId
  AND gm.deleted_at IS NULL
  AND g.deleted_at IS NULL
  AND g.status = 'ACTIVE'
  AND g.location IS NOT NULL
ORDER BY gm.id DESC
LIMIT 1;
-- 기대: Index Scan using idx_group_member_member_id_active
```
