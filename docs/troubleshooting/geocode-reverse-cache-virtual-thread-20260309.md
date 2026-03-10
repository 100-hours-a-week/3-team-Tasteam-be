# Reverse Geocode 엔드포인트 성능 개선 — Caffeine 캐시 + Virtual Thread

> 작성일: 2026-03-09
> 브랜치: `perf/#542/geocode-reverse-cache-and-virtual-thread`
> 관련 파일: `GeocodeService`, `application.yml`, `LocalCacheConfig`

---

## 요약

`GET /api/v1/geocode/reverse`는 Nominatim 외부 API를 동기(`RestClient`)로 호출한다.
동일 좌표 반복 요청에도 매번 외부 호출이 발생하고, 응답 대기 동안 Tomcat 스레드가 blocking I/O로 묶이는
두 가지 병목을 확인. Caffeine 캐시와 Virtual Thread 활성화로 각각 해소했다.

---

## Situation — 상황

### 엔드포인트 흐름

```
클라이언트 → GeocodeController → GeocodeService → NominatimClient(RestClient) → Nominatim API
```

- `NominatimClient`는 `RestClient`(동기 HTTP)로 구현되어 있다.
- Nominatim 응답 지연(평균 200 ~ 500 ms)이 있는 동안 Tomcat 스레드는 반납되지 않는다.
- 클라이언트는 소수점 3자리로 truncate한 좌표를 전송하므로, **동일 셀(~110 m 반경) 요청이 자주 반복된다**.

### 확인된 두 가지 병목

| 병목 | 설명 |
|------|------|
| **중복 외부 호출** | 동일 좌표 요청이 반복되어도 매번 Nominatim 호출 |
| **스레드 점유** | Nominatim 응답 대기 중 Tomcat 스레드가 blocking I/O로 점유 |

---

## Task — 목표

1. 동일 좌표에 대한 Nominatim 외부 호출 횟수를 최소화
2. Nominatim 응답 대기 시간 동안 스레드가 다른 요청을 처리할 수 있도록 개선
3. 코드 변경 최소화, 기존 인프라 재사용

### 검토한 대안

| 옵션 | 설명 | 결정 |
|------|------|------|
| A. Caffeine 캐시 (채택) | 기존 `LocalCacheConfig` 재사용, 설정만 추가 | **채택** |
| B. Redis 분산 캐시 | 다중 인스턴스 환경에서 일관성 보장 | 현재 단일 인스턴스 운영 → 오버엔지니어링. 보류 |
| C. WebClient(비동기) 전환 | Nominatim 호출을 non-blocking으로 변경 | Virtual Thread로 동일 효과. 기존 코드 변경 큼 → 기각 |
| D. Virtual Thread (채택) | Spring Boot 3.2+ 설정 한 줄로 전체 적용 | **채택** |

---

## Action — 구현

### 1. `GeocodeService` — `@Cacheable` 적용

```java
// domain/location/service/GeocodeService.java
@Cacheable(cacheNames = "reverse-geocode", key = "T(String).format('%.3f_%.3f', #lat, #lon)")
public ReverseGeocodeResponse reverseGeocode(double lat, double lon) { ... }
```

**캐시 키 설계: `String.format("%.3f_%.3f", lat, lon)`**

- `double` 타입은 부동소수점 표현 차이로 `37.123` ≠ `37.1230000000001` 같은 false miss가 발생할 수 있다.
- `%.3f` 포맷으로 소수점 3자리를 고정하여 floating-point 편차를 제거한다.
- 클라이언트가 이미 소수점 3자리로 truncate하여 전송하므로 키 공간이 일치한다.

> **왜 별도 빈으로 분리하지 않았나?**
> `GeocodeService.reverseGeocode()`는 public 메서드이므로 같은 클래스 내에서도 Spring AOP 프록시가 정상 동작한다.
> (컨트롤러 → 서비스 호출이므로 self-invocation 문제 없음)
> `PresignedUrlCacheService`처럼 별도 빈을 만든 케이스와 달리 이미 적절한 구조를 갖추고 있어 추가 분리가 불필요하다.

### 2. `application.yml` — reverse-geocode 캐시 TTL 등록

```yaml
tasteam:
  local-cache:
    ttl:
      presigned-url:
        ttl: ${LOCAL_CACHE_PRESIGNED_URL_TTL:240s}
      reverse-geocode:                                    # ← 추가
        ttl: ${LOCAL_CACHE_REVERSE_GEOCODE_TTL:24h}
```

- TTL = **24시간**: 도로명 주소·행정구역 정보는 자주 바뀌지 않는다.
- `LocalCacheConfig`가 `ttl` 맵을 순회하여 custom Caffeine cache를 자동 등록하는 구조를 그대로 재사용.
- 환경 변수 `LOCAL_CACHE_REVERSE_GEOCODE_TTL`로 운영 환경별 조정 가능.

### 3. `application.yml` — Virtual Thread 활성화

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

- Spring Boot 3.2+에서 이 설정 하나로 Tomcat executor가 virtual thread pool로 전환된다.
- `AsyncConfig.java`의 `webhookExecutor`, `notificationExecutor`, `searchQueryExecutor`가 이미 virtual thread를 사용하고 있어 패턴이 일치한다.
- Nominatim 응답 대기 중 carrier thread를 반납하므로 동시에 처리할 수 있는 요청 수가 증가한다.
- 코드 변경 없이 설정만으로 적용된다.

---

## Result — 결과 및 한계

### 개선된 점

| 항목 | before | after |
|------|--------|-------|
| 동일 좌표 Nominatim 호출 | 매 요청마다 1회 | 최초 1회 (이후 24h 캐시) |
| Nominatim 대기 중 스레드 | Tomcat 스레드 blocking | Virtual thread — carrier thread 반납 |
| 처리량 | Tomcat thread pool 크기에 의존 | Virtual thread 수 제한 없음 |
| 인프라 변경 | — | 없음 (기존 Caffeine 재사용) |

### 한계

- **단일 인스턴스 캐시**: Caffeine은 프로세스 로컬 캐시이므로, 인스턴스가 늘어나면 각자 독립적으로 워밍업된다.
  - 허용 가능: 캐시 히트가 없어도 정상 동작하며, 동일 좌표가 반복되면 자연스럽게 워밍업된다.
- **캐시 무효화 없음**: 주소 변경(재개발 등) 시 TTL 만료 전까지 이전 데이터를 반환할 수 있다.
  - 주소가 실시간으로 바뀌는 데이터가 아니므로 24h TTL은 허용 범위.
- **Virtual Thread 전체 적용**: `spring.threads.virtual.enabled`는 서버 전체에 적용된다. Virtual thread에서 ThreadLocal을 사용하는 라이브러리(일부 구버전 라이브러리)와의 호환성을 주의해야 한다.
  - 현재 사용 중인 Spring Security, JPA, Caffeine 모두 호환 확인.

### 후속 권장 작업

1. **캐시 히트율 모니터링**: `LocalCacheConfig`의 `recordStats = true` 설정으로 Micrometer `cache.gets` 메트릭(`tag: result=hit/miss`)이 자동 수집된다. Grafana에서 `reverse-geocode` 캐시의 hit/miss 비율을 확인하여 TTL 적정성을 검토할 것.
2. **다중 인스턴스 전환 시**: 인스턴스 수가 N개 이상으로 늘어날 경우 Redis 분산 캐시 도입 검토.

---

## 변경 파일 목록

| 파일 | 변경 유형 |
|------|-----------|
| `domain/location/service/GeocodeService.java` | 수정 (`@Cacheable` 추가) |
| `resources/application.yml` | 수정 (Virtual Thread 활성화, reverse-geocode TTL 추가) |
