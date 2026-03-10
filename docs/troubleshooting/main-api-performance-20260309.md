# /api/v1/main 성능 최적화 트러블슈팅 로그

> 작성일: 2026-03-09
> 대상 엔드포인트: `GET /api/v1/main/home`, `GET /api/v1/main/ai-recommend`
> 측정 환경: loadtest DB Docker (PostgreSQL 15 + PostGIS 3.3, restaurant 10K / review 300K / group_member 46K)

---

## 1. 배경 및 문제 인식

메인 페이지 API는 앱 최초 진입 시 항상 호출되는 핵심 엔드포인트다.
코드 리뷰 과정에서 다음 패턴들이 순차 실행 중임을 발견했다:

- hot → new → ai 섹션 쿼리 **순차** 실행 (각 섹션당 최대 5 DB 쿼리, 합계 최대 15)
- categories / thumbnails / summaries 보조 데이터 **순차** 조회
- `resolveLocation()` 내부 **N+1** 패턴
- `review.restaurant_id` 인덱스 **미존재**

---

## 2. 측정 환경 및 방법론

```
DB: tasteam-db-loadtest (Docker, PostgreSQL 15 + PostGIS 3.3, port 55432)
데이터 규모:
  restaurant   10,000행 (active)
  review      300,000행 (active)
  group_member 45,999행 (active)

측정 방법: EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT)
인덱스 비활성화: SET enable_indexscan=off; SET enable_bitmapscan=off;
```

### 최적화 전 인덱스 목록 (`review` 테이블)

```sql
-- 기존 인덱스 (review 테이블)
review_pkey  -- PRIMARY KEY (id) 만 존재
-- restaurant_id 컬럼에 인덱스 없음
```

---

## 3. 병목 분석 및 증거

### 3.1 `review.restaurant_id` 인덱스 미존재 (가장 큰 병목)

**문제 쿼리: `findHotRestaurants` (위치 기반)**

```sql
SELECT r.id as id, r.name as name,
  ST_Distance(r.location::geography, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography) as distanceMeter
FROM restaurant r
LEFT JOIN review rv ON rv.restaurant_id = r.id AND rv.deleted_at IS NULL
WHERE r.deleted_at IS NULL
  AND ST_DWithin(r.location::geography, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography, :radiusMeter)
GROUP BY r.id, r.name, r.location
ORDER BY count(rv.id) DESC, r.id ASC
LIMIT 20;
```

**EXPLAIN ANALYZE - 인덱스 없음 (`enable_bitmapscan=off`, `enable_indexscan=off`)**

```
Limit  (cost=262497.83..262522.84) (actual time=1005.923..1006.118 rows=6 loops=1)
  ...
  -> Hash Right Join  (cost=250375.01..262496.85)
       -> Seq Scan on review rv  (actual time=0.694..465.527 rows=300000 loops=1)
            Filter: (deleted_at IS NULL)
       -> Hash  (actual time=190.095..190.130 rows=6 loops=1)
            -> Seq Scan on restaurant r  (actual time=163.608..189.760 rows=6 loops=1)

Execution Time: 1512.440 ms
```

**병목 원인:**
- `review.restaurant_id`에 인덱스가 없어 반경 내 6개 식당에 대한 리뷰 집계 시 300,000행 전체 스캔
- ST_DWithin으로 6개 식당만 추려냈음에도 Hash Right Join을 위해 review 전체를 Hash에 올림
- `Seq Scan on review`: 300,000행 스캔, 465ms 소요

**EXPLAIN ANALYZE - 인덱스 적용 후**

```
Limit  (cost=308.80..333.82) (actual time=115.928..115.975 rows=6 loops=1)
  ...
  -> Nested Loop Left Join
       -> Index Scan using idx_restaurant_geography_gist on restaurant r
            (actual time=84.103..86.587 rows=6 loops=1)
       -> Bitmap Heap Scan on review rv
            Recheck Cond: ((restaurant_id = r.id) AND (deleted_at IS NULL))
            Heap Blocks: exact=100
            -> Bitmap Index Scan on idx_review_restaurant_id_active
                 Index Cond: (restaurant_id = r.id)
                 (actual time=0.475..0.475 rows=17 loops=6)

Execution Time: 119.026 ms
```

**개선 결과:**

| 측정항목 | 인덱스 없음 | 인덱스 적용 | 개선 |
|---------|-----------|-----------|------|
| Execution Time | 1,512 ms | 119 ms | **-92%, 12.7× 빠름** |
| review 스캔 rows | 300,000 | 17 × 6 = 102 | **-99.97%** |
| Buffers read | 8,142 pages | 104 pages | **-98.7%** |
| Join 전략 | Hash Right Join (전체 스캔) | Nested Loop + Bitmap Index Scan | - |

**인덱스 설계:**
```sql
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_review_restaurant_id_active
    ON review (restaurant_id)
    WHERE deleted_at IS NULL;
```
- Partial Index (`WHERE deleted_at IS NULL`): 논리 삭제된 리뷰 제외 → 인덱스 크기 최소화
- `CONCURRENTLY`: 운영 중 무중단 생성

> **Note**: `findHotRestaurantsAll` (위치 없음)의 경우 전체 restaurant에 대한 GROUP BY COUNT(*) 집계가 필요하므로 플래너가 여전히 Seq Scan을 선택 (~170ms). 이 경우는 Caffeine 캐시(5분 TTL)로 커버.

---

### 3.2 `resolveLocation()` N+1 패턴

**기존 코드 구조 (`MainService.java:163-175`)**

```java
// 기존: findMemberGroupSummaries 1회 + 루프에서 findByIdAndDeletedAtIsNull N회
List<MemberGroupSummaryRow> groups = groupMemberRepository.findMemberGroupSummaries(memberId, GroupStatus.ACTIVE);
for (MemberGroupSummaryRow groupSummary : groups) {
    Group group = groupRepository.findByIdAndDeletedAtIsNull(groupSummary.groupId()).orElse(null);
    if (group != null && group.getLocation() != null) { ... }
}
```

**원인 분석:**
- `findMemberGroupSummaries`의 JPQL은 `g.location`을 SELECT에 포함하지 않음
  ```sql
  select new MemberGroupSummaryRow(g.id, g.name)
  from GroupMember gm join Group g on g.id = gm.groupId
  where gm.member.id = :memberId ...
  ```
- location 확인을 위해 루프 내 별도 쿼리 N번 발생
- `group_member`에 `member_id` 단독 인덱스 없음 → 매 쿼리마다 Seq Scan

**실제 측정 - 6개 그룹을 가진 사용자 (member_id=1370551)**

```
[Query 1] findMemberGroupSummaries
  -> Seq Scan on group_member gm
       Filter: ((deleted_at IS NULL) AND (member_id = 1370551))
       Rows Removed by Filter: 45,993
  Execution Time: 6.558 ms

[Query 2~7] findByIdAndDeletedAtIsNull × 6회
  -> Index Scan using group_pkey on "group" (각 group_id)
  실행 시간: 1.664 + 1.502 + 1.886 + 1.241 + 2.252 + 1.277 = 9.822 ms
```

| | 기존 (N+1) | 개선 후 (단일 쿼리) |
|---|---|---|
| DB 쿼리 수 | 7회 (1 + N) | **1회** |
| 총 소요 시간 | ~16.4 ms | **~1.7 ms** (warm) |
| 그룹 수 증가 시 | 선형 증가 (N+1) | **항상 1회** |

**개선 쿼리 (native SQL):**
```sql
SELECT ST_Y(g.location) as latitude, ST_X(g.location) as longitude
FROM group_member gm
JOIN "group" g ON g.id = gm.group_id
WHERE gm.member_id = :memberId
  AND gm.deleted_at IS NULL
  AND g.deleted_at IS NULL
  AND g.status = 'ACTIVE'
  AND g.location IS NOT NULL
ORDER BY gm.id DESC
LIMIT 1;
```

```
Limit  (actual time=1.761..1.815 rows=1 loops=1)
  Buffers: shared hit=49
  -> Nested Loop
       -> Index Scan Backward using group_member_pkey on group_member gm
            Filter: ((deleted_at IS NULL) AND (member_id = 1370551))
            Rows Removed by Filter: 3,313
       -> Index Scan using group_pkey on "group" g
            Index Cond: (id = gm.group_id)

Execution Time: 1.815 ms (warm, member found at 3,314th position)
```

---

### 3.3 섹션 순차 실행 (15 DB 쿼리 순차)

**기존 코드 (`MainService.java:60-62`)**

```java
List<...> hotRestaurants  = fetchHotSection(location);   // 최대 5 쿼리
List<...> newRestaurants  = fetchNewSection(location);   // 최대 5 쿼리
List<...> aiRestaurants   = fetchAiRecommendSection(location); // 최대 5 쿼리
```

**`fetchWithRadiusExpansion()` 동작 방식:**
- `EXPANDED_RADII = {3_000, 5_000, 10_000, 20_000}` (미터 단위)
- `SECTION_SIZE = 20`
- 각 반경에서 20개 미만이면 다음 반경으로 확장
- 최종 20개 미달 시 `findRandomRestaurants`로 채움
- **최악 케이스: 4 반경 쿼리 + 1 랜덤 채움 = 섹션당 5 쿼리 × 3 섹션 = 15 쿼리 순차**

**위치 기반 섹션 쿼리 시간 (5km 반경, 6개 식당):**
- `findHotRestaurants`: ~119ms (인덱스 적용 후)
- 순차 최악: ~119ms × 3 = **~357ms** (섹션 조회만)

**개선: CompletableFuture.allOf 병렬화**

```java
CompletableFuture<List<...>> hotFuture = CompletableFuture.supplyAsync(
    () -> fetchHotSection(location), mainQueryExecutor);
CompletableFuture<List<...>> newFuture = CompletableFuture.supplyAsync(
    () -> fetchNewSection(location), mainQueryExecutor);
CompletableFuture<List<...>> aiFuture  = CompletableFuture.supplyAsync(
    () -> fetchAiSection(location), mainQueryExecutor);
CompletableFuture.allOf(hotFuture, newFuture, aiFuture).join();
```

- 3 섹션 순차 합산 → **max(hot, new, ai)** 단일 섹션 시간으로 단축
- `mainQueryExecutor`: Virtual Thread 기반 (`Executors.newVirtualThreadPerTaskExecutor()`)
  - 경량 스레드로 블로킹 I/O(DB 쿼리) 대기 비용 최소화

---

### 3.4 보조 데이터 3개 순차 실행

**기존 코드 (`MainService.java:70-72`)**

```java
Map<Long, List<String>> categoriesByRestaurant = fetchCategories(allIds);
Map<Long, String>       thumbnailByRestaurant  = fetchThumbnails(allIds);
Map<Long, String>       summaryByRestaurant    = fetchSummaries(allIds);
```

- 동일한 `allIds` 기준의 독립 쿼리 3개가 순차 실행
- 각 쿼리의 결과는 상호 의존 없음

**개선: 섹션 병렬화와 동일한 패턴 적용**

```java
CompletableFuture<Map<Long, List<String>>> catFuture     = supplyAsync(() -> fetchCategories(allIds), mainQueryExecutor);
CompletableFuture<Map<Long, String>>       thumbFuture   = supplyAsync(() -> fetchThumbnails(allIds), mainQueryExecutor);
CompletableFuture<Map<Long, String>>       summaryFuture = supplyAsync(() -> fetchSummaries(allIds), mainQueryExecutor);
CompletableFuture.allOf(catFuture, thumbFuture, summaryFuture).join();
```

---

### 3.5 위치 미제공 시 동일 결과 반복 조회

위치 정보가 없는 경우 hot/new/ai 섹션 결과는 모든 사용자에게 동일하다.
그러나 매 요청마다 DB를 조회했다.

**개선: `@Cacheable` + Caffeine 로컬 캐시 (TTL 5분)**

```java
@Cacheable(cacheNames = "main-section-hot-all", key = "'all'")
@Transactional(readOnly = true)
public List<MainRestaurantDistanceProjection> fetchHotSectionAll() { ... }
```

```yaml
tasteam:
  local-cache:
    ttl:
      main-section-hot-all:
        ttl: 5m
      main-section-new-all:
        ttl: 5m
      main-section-ai-all:
        ttl: 5m
      main-banners:
        ttl: 5m
```

---

## 4. 개선 내용 요약

### 4.1 아키텍처 변경

```
[기존]
MainService.getMain()
  ├─ resolveLocation()           ← N+1 (1 + N 쿼리)
  ├─ fetchHotSection()           ← 순차 실행
  ├─ fetchNewSection()           ← 순차 실행
  ├─ fetchAiRecommendSection()   ← 순차 실행
  ├─ fetchCategories()           ← 순차 실행
  ├─ fetchThumbnails()           ← 순차 실행
  └─ fetchSummaries()            ← 순차 실행
  총: 최대 18 쿼리 순차

[개선 후]
MainService.getMain()
  ├─ resolveLocation()              ← 단일 쿼리 (1회)
  ├─ [병렬 1그룹] allOf(hot, new, ai)  ← 3개 병렬
  └─ [병렬 2그룹] allOf(cat, thumb, summary) ← 3개 병렬
  총: max(hot,new,ai) + max(cat,thumb,summary) + 1
  캐시 히트 시: 메모리 조회만 (no DB)
```

### 4.2 변경 파일

| 파일 | 변경 내용 |
|------|-----------|
| `MainService.java` | 병렬 섹션/보조데이터 조회, resolveLocation 단순화 |
| `MainDataService.java` (신규) | 섹션 fetch 로직 분리, @Cacheable 적용 |
| `LocationContext.java` (신규) | 패키지 레벨 record 추출 |
| `GroupMemberRepository.java` | `findFirstGroupLocationByMemberId` 추가 |
| `GroupLocationProjection.java` (신규) | native 쿼리 projection |
| `AsyncConfig.java` | `mainQueryExecutor` 빈 추가 |
| `TestStorageConfiguration.java` | `mainQueryExecutor = Runnable::run` 추가 |
| `application.yml` | 캐시 TTL 4개 추가 |
| `V202603091000__add_index_review_restaurant_id.sql` (신규) | partial index |

---

## 5. 성능 개선 결과 요약

### DB 인덱스 효과 (실측)

| 쿼리 | 개선 전 | 개선 후 | 개선폭 |
|------|---------|---------|--------|
| `findHotRestaurants` (위치 기반) | **1,512 ms** | **119 ms** | **-92%, 12.7×** |
| `findHotRestaurantsAll` (위치 없음) | ~268 ms (cold) | ~170 ms (warm) | 캐시로 커버 |

### N+1 제거 효과 (실측, 6그룹 사용자 기준)

| | 기존 | 개선 후 |
|---|---|---|
| DB 쿼리 수 | 7회 | **1회** |
| 총 시간 | ~16.4 ms | ~1.7 ms |
| 스케일 특성 | O(N) — 그룹 수에 비례 | **O(1)** |

### 이론적 최악 케이스 단축 (위치 있음, 반경 확장 최대)

| 단계 | 기존 (순차) | 개선 후 (병렬) |
|------|-----------|-------------|
| 섹션 조회 | hot + new + ai ≈ 3× 단일 | max(hot, new, ai) ≈ 1× 단일 |
| 보조 데이터 | cat + thumb + summary | max(cat, thumb, summary) |
| resolveLocation | 1 + N 쿼리 | 1 쿼리 |

---

## 6. 잔여 개선 여지

1. **`group_member.member_id` 인덱스 미존재**
   - `findFirstGroupLocationByMemberId`의 group_member Seq Scan (45,999행)
   - `(group_id, member_id)` 복합 인덱스는 있지만 `member_id` 단독 조건에는 사용 불가
   - 추가 인덱스 후보: `CREATE INDEX ON group_member(member_id) WHERE deleted_at IS NULL;`

2. **섹션 반경 확장 전략 개선**
   - 현재 3/5/10/20km를 순차 탐색; 데이터가 희소한 경우 최대 5 쿼리 실행
   - 첫 쿼리에서 충분한 데이터가 있으면 조기 종료 (현재 동작) — 데이터 밀집 지역에서는 문제 없음
   - 데이터가 매우 희소한 지역에서는 최대 5쿼리 발생 (random fill 포함)
