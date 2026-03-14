# 메인 섹션 캐시 정교화 트러블슈팅 로그 (Round 3)

> 작성일: 2026-03-14
> 대상 엔드포인트: `GET /api/v1/main/home`, `GET /api/v1/main/ai-recommend`
> 전편: `docs/troubleshooting/main-api-performance-20260314.md` (Round 2)
> 관련 PR: `perf/#586/main-cache-refinement`

---

## 1. 배경 및 문제 인식

Round 2(PR #583)에서 `@Transactional` 커넥션 낭비, 반경 확장 쿼리 최악 5회, group_member 인덱스 등
구조적 문제를 해결했다. 이어서 Hot/New 섹션에 **Geo Grid 기반 캐싱**(PR #581)을 도입했으나,
아래 세 가지 문제가 여전히 남아 있었다.

| # | 문제 | 증상 |
|---|------|------|
| 1 | Geo 캐시 셀 크기 과대 | 0.1° ≈ 11km 셀 → 같은 셀 안에서도 실제로 다른 사용자가 드물어 hit율 낮음 |
| 2 | 거리 계산 DB 부하 지속 | geo 캐시 hit 후에도 매 요청마다 `findDistancesByIds` SQL 실행 |
| 3 | 짧은 TTL과 동시 만료 | 모든 main 섹션 캐시 TTL 5m + jitter 없음 → 대량 동시 만료 시 DB 부하 스파이크 |

---

## 2. 문제 분석

### 2.1 Geo 캐시 키 셀이 너무 커서 hit율 낮음

**기존 캐시 키**:

```java
key = "T(String).format('%d_%d',
    T(java.lang.Math).round(#lat / 0.1),
    T(java.lang.Math).round(#lon / 0.1))"
```

`Math.round(lat / 0.1)`은 0.1° 단위로 반올림하여 정수로 만든다.
- 0.1° 위도 ≈ 11.1km, 0.1° 경도(서울 기준) ≈ 9.0km
- 한 셀 안에 11km × 9km 넓이의 모든 사용자가 동일 키를 공유해야 hit 가능
- 실사용 패턴에서 같은 셀 안에 동시 접속하는 사용자가 적으면 hit율 거의 0%

**측정**: dev 환경에서 동일 지역 10회 연속 호출 시 geo 캐시 hit = 3회 (같은 장소에서도 GPS 드리프트로 셀 경계를 넘는 경우 발생).

---

### 2.2 geo 캐시 hit 후에도 DB 거리 계산 쿼리 실행

**기존 흐름**:

```text
fetchHotSectionByLocation(lat, lon)
  → self.fetchHotSectionIdsByLocation(lat, lon)  ← 캐시 hit (ID 목록만 저장)
  → restaurantRepository.findDistancesByIds(ids, lat, lon)  ← 매번 DB 실행
```

`findDistancesByIds` 쿼리:

```sql
SELECT r.id, r.name,
    ST_Distance(
        r.location::geography,
        ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
    ) AS distanceMeter
FROM restaurant r
WHERE r.id IN (:ids)  -- 20개 ID
```

geo 캐시가 ID 목록을 캐싱하더라도 거리 계산 쿼리는 매 요청마다 발생했다.
20개 음식점 × `ST_Distance` geography 연산 = 비교적 가벼운 쿼리이지만,
home 페이지 로드 시 hot + new 두 섹션에서 2번씩 실행되므로 총 4회.

---

### 2.3 짧은 TTL과 동시 만료 (thundering herd)

**기존 설정**:

```yaml
main-section-hot-all:
  ttl: 5m
main-section-new-all:
  ttl: 5m
# ... 모두 동일한 5m
```

애플리케이션 기동 후 5분이 지나면 모든 main 섹션 캐시가 동시에 만료된다.
이 순간 다수 요청이 동시에 캐시 miss → DB 쿼리 폭발(cache stampede).

또한 TTL 5분은 지나치게 짧다. 음식점 Hot/New 랭킹은 리뷰 누적 수나 최신 등록 기준이므로
1시간 이내에 급격히 변하지 않는다.

---

## 3. 해결 방안

### 3.1 Geo 캐시 키 — 소수점 3자리 truncation

**핵심 아이디어**: 위경도를 소수점 3자리에서 truncation하여 ≈111m 셀 단위로 양자화.

```java
// 변경 후
key = "T(String).format('%d_%d', (long)(#lat * 1000), (long)(#lon * 1000))"
```

| 항목 | 기존 (0.1° 반올림) | 변경 후 (3자리 truncation) |
|------|-------------------|--------------------------|
| 셀 크기 | ≈11km × 9km | ≈111m × 90m |
| 키 예시 (37.5012, 127.0234) | `375_1270` | `37501_127023` |
| 셀 내 공유 가능 사용자 | 11km 이내 전체 | 같은 블록·건물 수준 |
| 예상 hit율 향상 | — | 동일 장소 재방문 시 90% 이상 hit 가능 |

**중요**: 캐시 키는 truncated 좌표를 사용하지만, 실제 DB 쿼리(`findHotRestaurants` 등)에는
원본 소수점 좌표를 그대로 전달한다. 검색 반경과 정렬은 원본 좌표 기준으로 정확히 동작.

**Java `(long)` 캐스트 특성**:
- `(long)(37.5012345 * 1000)` = `(long)(37501.2345)` = `37501` (0 방향 truncation)
- 한국 좌표계는 양수이므로 `Math.floor`와 동일하게 동작

---

### 3.2 음식점 위치 좌표 캐싱 + 앱 레이어 Haversine 거리 계산

**핵심 아이디어**: 음식점 위치(PostGIS Point)를 `restaurant-location` 캐시에 저장.
거리 계산 시 DB ST_Distance 대신 앱 레이어 `GeoUtils.distanceMeter()`(Haversine)를 사용.

**새 흐름**:

```text
fetchHotSectionByLocation(lat, lon)
  → self.fetchHotSectionIdsByLocation(lat, lon)  ← geo 캐시 hit (ID 목록)
  → fetchDistancesWithCoordCache(ids, lat, lon)
       ↓
       restaurant-location 캐시 조회 per id
         hit → CachedLocation(name, lat, lon) 획득
         miss → missIds 수집
       ↓ (missIds가 있으면)
       findLocationsByIds(missIds)  ← DB 조회 (좌표만, ST_Distance 없음)
       → 결과를 restaurant-location 캐시에 저장
       ↓
       GeoUtils.distanceMeter(userLat, userLon, restLat, restLon) × 20개  ← 앱에서 계산
```

**신규 쿼리 `findLocationsByIds`**:

```sql
SELECT r.id AS id, r.name AS name,
    ST_Y(r.location::geometry) AS latitude,
    ST_X(r.location::geometry) AS longitude
FROM restaurant r
WHERE r.id IN (:ids)
```

`ST_Y` / `ST_X`는 단순 좌표 추출로 `ST_Distance`(geography 연산) 대비 비용이 극히 낮다.

**이후 요청 동작** (restaurant-location 캐시가 워밍된 이후):

```text
[캐시 full hit 시]
  findLocationsByIds: 0회  (모두 앱 캐시에서 처리)
  GeoUtils.distanceMeter: 20회  (CPU ≈ 0.01ms, 무시 가능)
  DB 쿼리: 0회
```

**`CachedLocation` / `CachedDistance` inner record** (MainDataService):

```java
private record CachedLocation(String name, double lat, double lon) {}

private record CachedDistance(Long id, String name, Double distanceMeter)
    implements MainRestaurantDistanceProjection {}
```

**GeoUtils.distanceMeter** (기존 Haversine 구현체, `GeoUtils.java`):

```java
public static double distanceMeter(double lat1, double lon1, double lat2, double lon2) {
    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);
    double a = Math.sin(dLat/2) * Math.sin(dLat/2)
        + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
          * Math.sin(dLon/2) * Math.sin(dLon/2);
    return 6371000 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}
```

**PostGIS ST_Distance(geography) vs Haversine 정확도 차이**:

| 방식 | 지구 모델 | 오차 |
|------|-----------|------|
| ST_Distance geography | WGS84 타원체 | 기준 |
| Haversine (구형 지구) | 구형 (R=6371km) | 최대 0.3% (한국 위도 기준 ≈ 10m/3km) |

거리 표시 UI에서 10m 오차는 실사용에서 무의미.

---

### 3.3 TTL 상향 및 Jitter 적용

#### TTL 1h 상향

음식점 Hot/New 랭킹 변화 빈도를 분석:
- Hot(리뷰 수 기준): 하루 몇 건 수준의 리뷰가 1시간 안에 랭킹을 바꾸기 어려움
- New(등록일 기준): 신규 등록은 배치/어드민 작업으로 빈번하지 않음

5분 TTL은 DB 보호 효과가 거의 없는 반면 hit율을 극히 낮게 만드는 설정.
1시간으로 변경해도 데이터 신선도에 실용적 영향이 없다고 판단.

```yaml
# 기존
main-section-hot-all:
  ttl: 5m

# 변경
main-section-hot-all:
  ttl: ${LOCAL_CACHE_MAIN_SECTION_TTL:1h}
  jitter: ${LOCAL_CACHE_MAIN_SECTION_JITTER:30s}
```

#### Caffeine TTL Jitter 구현

Redis는 `SET key value EX N`을 항목별로 다르게 설정 가능하지만,
Caffeine은 `expireAfterWrite(Duration)` 고정값만 지원한다.

해결책: `Caffeine.expireAfter(Expiry<K,V>)`를 사용하여 항목 생성 시점에 랜덤 TTL 주입.

```java
class JitterExpiry implements Expiry<Object, Object> {

    private final long baseNanos;
    private final long jitterNanos;

    @Override
    public long expireAfterCreate(Object key, Object value, long currentTime) {
        if (jitterNanos <= 0) return baseNanos;
        return baseNanos + ThreadLocalRandom.current().nextLong(jitterNanos + 1);
    }

    @Override
    public long expireAfterUpdate(Object key, Object value, long currentTime, long currentDuration) {
        return expireAfterCreate(key, value, currentTime);  // 갱신 시도 재롤
    }

    @Override
    public long expireAfterRead(Object key, Object value, long currentTime, long currentDuration) {
        return currentDuration;  // 조회는 TTL 유지 (연장 없음)
    }
}
```

**효과**: 기동 후 1시간 뒤 만료가 `[1h, 1h 30s]` 범위에 분산.
단일 순간에 전체 캐시가 동시에 만료되는 stampede 방지.

**`LocalCacheConfig` 변경**:

```java
// 기존
Caffeine.newBuilder()
    .maximumSize(caffeineConfig.getMaximumSize())
    .expireAfterWrite(cacheTtl.getTtl())
    .recordStats()
    .build()

// 변경
Caffeine<Object, Object> builder = Caffeine.newBuilder()
    .maximumSize(size)
    .recordStats();

if (cacheTtl.getJitter() != null && !cacheTtl.getJitter().isZero()) {
    builder.expireAfter(new JitterExpiry(
        cacheTtl.getTtl().toNanos(), cacheTtl.getJitter().toNanos()));
} else {
    builder.expireAfterWrite(cacheTtl.getTtl());
}
```

---

## 4. 변경 내용 요약

### 4.1 변경 파일

| 파일 | 변경 내용 | 커밋 |
|------|-----------|------|
| `LocalCacheProperties.java` | `CacheTtl`에 `jitter`, `maximumSize` 필드 추가 | 1st |
| `JitterExpiry.java` (신규) | Caffeine `Expiry` 구현, base+jitter TTL 랜덤화 | 2nd |
| `LocalCacheConfig.java` | jitter 있으면 `JitterExpiry` 사용, per-cache `maximumSize` 지원 | 2nd |
| `application.yml` | main 섹션 TTL 1h + jitter 30s; `restaurant-location` 캐시 신규 | 3rd |
| `RestaurantLocationProjection.java` (신규) | `(id, name, latitude, longitude)` projection | 4th |
| `RestaurantRepository.java` | `findLocationsByIds()` 쿼리 추가 (ST_Y/ST_X) | 4th |
| `MainDataService.java` | geo 키 변경, `fetchDistancesWithCoordCache()`, inner record | 5th |
| `LocalCacheMetricsBinder.java` | hot-geo, new-geo, restaurant-location case + `resolveCapacity()` | 5th |

### 4.2 캐시 구성 변경 후

| 캐시 이름 | TTL | Jitter | maxSize | 저장 내용 |
|-----------|-----|--------|---------|----------|
| `main-section-hot-geo` | 1h | 30s | 1000 | `List<Long>` ID 목록 |
| `main-section-new-geo` | 1h | 30s | 1000 | `List<Long>` ID 목록 |
| `main-section-hot-all` | 1h | 30s | 1000 | `List<MainRestaurantDistanceProjection>` |
| `main-section-new-all` | 1h | 30s | 1000 | 동일 |
| `main-section-ai-all` | 1h | 30s | 1000 | 동일 |
| `restaurant-location` | 2h | 30s | 10000 | `CachedLocation(name, lat, lon)` |

`restaurant-location` TTL을 2h로 설정한 이유: geo ID 캐시(1h) 만료 후 새 위치로
재조회가 발생해도 음식점 좌표 캐시는 살아있어 `findLocationsByIds` 재실행을 최소화.

### 4.3 메모리 예산

```
restaurant-location 캐시:
  maximumSize = 10000 항목
  항목당 크기 ≈ name(≈20B) + lat(8B) + lon(8B) + key overhead ≈ 60~80B
  총 ≈ 800KB — Caffeine 로컬 캐시로 충분
```

---

## 5. 예상 효과

| 문제 | 기존 | 개선 후 기대 |
|------|------|------------|
| Geo 캐시 hit율 | 낮음 (11km 셀, GPS 드리프트로 경계 이탈) | 높음 (111m 셀, 같은 장소 재방문 시 안정적 hit) |
| 거리 계산 DB 쿼리 | 매 요청마다 `findDistancesByIds` (ST_Distance 20회) | 캐시 hit 시 DB 미접촉, Haversine 앱 계산 |
| main 섹션 캐시 TTL | 5m (잦은 DB 조회) | 1h (DB 조회 대폭 감소) |
| 동시 TTL 만료 | 전체 동시 만료 시 DB 부하 스파이크 | [1h, 1h+30s] 분산 만료 |
| restaurant-location miss 시 | N/A (신규) | `findLocationsByIds` 1회 (ST_Distance 없는 경량 쿼리) |

---

## 6. 검증 방법

### 6.1 기동 로그 확인

```
INFO Registered custom cache 'main-section-hot-geo' TTL=PT1H jitter=PT30S maxSize=1000
INFO Registered custom cache 'restaurant-location' TTL=PT2H jitter=PT30S maxSize=10000
```

### 6.2 캐시 hit율 확인

```bash
# Actuator 캐시 메트릭
curl http://localhost:8080/actuator/metrics/tasteam.cache.requests \
  --get --data-urlencode 'tag=cache:main-section-hot-geo' \
  --data-urlencode 'tag=result:hit'

curl http://localhost:8080/actuator/metrics/tasteam.cache.requests \
  --get --data-urlencode 'tag=cache:restaurant-location' \
  --data-urlencode 'tag=result:miss'
```

### 6.3 DB 쿼리 감소 확인 (두 번 연속 동일 위치 호출)

```bash
# 첫 번째 호출 — findHotRestaurants + findLocationsByIds 발생
# 두 번째 호출 (같은 3자리 truncated 좌표) — geo 캐시 hit, restaurant-location hit
# → Hibernate 쿼리 로그에서 두 번째 호출 시 SQL 미발생 확인
```

로그 설정 (로컬 확인용):

```yaml
logging:
  level:
    org.hibernate.SQL: DEBUG
```

### 6.4 Jitter 동작 확인

```bash
# 캐시 등록 직후 stats 확인 — expiry 시간이 항목마다 약간씩 다름
curl http://localhost:8080/actuator/caches
```
