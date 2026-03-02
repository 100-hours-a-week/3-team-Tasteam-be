# Presigned GET URL 캐싱 불가 문제 분석 및 서버 측 캐싱 개선

> 작성일: 2026-02-26
> 작성자: devon
> 브랜치: `perf/presigned-url-server-cache`
> 관련 파일: `FileService`, `FileController`, `LocalCacheConfig`, `PresignedUrlCacheService`

---

## 요약

`access-mode=presigned` 운영 환경에서 `/api/v1/files/{fileUuid}/url`을 호출할 때마다
새로운 서명 URL이 생성되어 CDN·Service Worker·브라우저 캐시가 모두 무력화되는 문제를 확인.
서버 측 Caffeine 캐시로 Presigned URL을 TTL 내 재사용하고, API 응답에 `Cache-Control` 헤더를 추가하여
클라이언트(Service Worker)의 API 응답 캐싱을 가능하게 함.

---

## Situation — 상황

### 운영 환경 설정

```yaml
# application.prod.yml
tasteam:
  storage:
    access-mode: ${STORAGE_ACCESS_MODE:presigned}
    presigned-expiration-seconds: ${STORAGE_PRESIGNED_EXPIRATION_SECONDS:300}
```

`access-mode=presigned`이므로 `GET /api/v1/files/{fileUuid}/url` 호출 시
매번 `S3StorageClient.createPresignedGetUrl()`이 실행되어 새 서명 URL이 반환된다.

### Presigned URL 구조

```
https://bucket.s3.ap-northeast-2.amazonaws.com/uploads/review/image/{uuid}.jpg
  ?X-Amz-Algorithm=AWS4-HMAC-SHA256
  &X-Amz-Date=20260226T123456Z        ← 호출 시점마다 달라짐
  &X-Amz-Expires=300
  &X-Amz-SignedHeaders=host
  &X-Amz-Signature=abc123...           ← 호출 시점마다 달라짐
```

### 문제점

| 캐시 레이어 | 동작 | 원인 |
|---|---|---|
| CDN (Caddy / CloudFront) | cache miss 반복 | URL이 캐시 키 → 매번 다른 URL |
| Service Worker (`cache.match`) | cache miss 반복 | 동일. Request URL 기반 매칭 |
| 브라우저 HTTP 캐시 | cache miss 반복 | 동일 |

### 현재 코드 (문제 지점)

```java
// FileService.java (before)
private String buildPublicUrl(String storageKey) {
    if (storageProperties.isPresignedAccess()) {
        return storageClient.createPresignedGetUrl(storageKey); // 매 호출마다 새 서명
    }
    return buildStaticUrl(storageKey);
}
```

`createPresignedGetUrl()` 내부에서 `Instant.now()`로 서명 시각을 계산하므로
동일한 `storageKey`라도 URL이 항상 달라진다.

---

## Task — 목표

1. **동일 storageKey에 대해 presigned URL을 TTL 내 재사용** (S3 서명 호출 횟수 감소)
2. **API 응답에 `Cache-Control` 헤더 추가** → Service Worker가 API 응답 자체를 캐시할 수 있도록
3. 코드 변경 최소화, 기존 `public` 모드 동작 유지

### 검토한 대안

| 옵션 | 설명 | 결정 |
|---|---|---|
| A. `access-mode=public` 전환 | URL 완전 고정, 코드 변경 없음. 버킷 공개 필요 | 인프라 결정 필요 — 보류 |
| B. 서버 측 URL 캐싱 (채택) | Caffeine 캐시로 TTL 내 URL 재사용, 코드 변경 최소 | **채택** |
| C. Caddy S3 프록시 | Caddy에서 직접 S3 서명 | 공식 SigV4 모듈 없어 구현 복잡 — 기각 |
| D. CloudFront + OAC | AWS CDN으로 URL 고정 | 장기 권장, 당장 인프라 변경 필요 — 보류 |

옵션 B를 채택한 이유: Redis는 이미 prod에 구성되어 있으나 단순 로컬 캐시(Caffeine)로
충분히 해결 가능하고, 추가 인프라 의존성 없이 즉시 적용 가능.

---

## Action — 구현

### 1. `PresignedUrlCacheService` 신설

`FileService` 내 `buildPublicUrl()`에서 직접 `storageClient`를 호출하던 구조를
`@Cacheable` 프록시가 가능한 별도 빈으로 분리.

```java
// infra/storage/PresignedUrlCacheService.java
@Service
@RequiredArgsConstructor
public class PresignedUrlCacheService {

    private final StorageClient storageClient;

    @Cacheable(cacheNames = "presigned-url", key = "#storageKey")
    public String getPresignedUrl(String storageKey) {
        return storageClient.createPresignedGetUrl(storageKey);
    }
}
```

> **왜 별도 빈인가?**
> `@Cacheable`은 Spring AOP 프록시 기반이므로 같은 클래스 내 private 메서드에는 동작하지 않는다.
> `buildPublicUrl()`이 private이므로 캐시 적용 메서드를 별도 빈으로 추출했다.

### 2. `LocalCacheConfig` — 캐시별 개별 TTL 지원

기존에 선언만 되고 실제로 연결되지 않았던 `LocalCacheProperties.ttl` 맵을 활성화.

```java
// global/config/LocalCacheConfig.java (추가된 부분)
localCacheProperties.getTtl().forEach((cacheName, cacheTtl) -> {
    if (cacheTtl.getTtl() != null) {
        cacheManager.registerCustomCache(cacheName,
            Caffeine.newBuilder()
                .maximumSize(caffeineConfig.getMaximumSize())
                .expireAfterWrite(cacheTtl.getTtl())
                .recordStats()
                .build());
    }
});
```

이제 `tasteam.local-cache.ttl.<name>.ttl: <duration>` 형식으로
캐시별 TTL을 설정 파일에서 독립적으로 관리할 수 있다.

### 3. `application.yml` — presigned-url 캐시 TTL 설정

```yaml
tasteam:
  local-cache:
    caffeine:
      expire-after-write: ${LOCAL_CACHE_CAFFEINE_EXPIRE_AFTER_WRITE:10m}
    ttl:
      presigned-url:
        ttl: ${LOCAL_CACHE_PRESIGNED_URL_TTL:240s}   # ← 신규
```

TTL = 240s = presigned 만료(300s) − 60s 안전 마진.
만료 직전에 캐시에서 꺼낸 URL이 이미 무효화된 상태로 클라이언트에 전달되는 것을 방지.

### 4. `FileService` — 캐시 서비스 사용

```java
// FileService.java (after)
private String buildPublicUrl(String storageKey) {
    if (storageProperties.isPresignedAccess()) {
        return presignedUrlCacheService.getPresignedUrl(storageKey); // 캐시 경유
    }
    return buildStaticUrl(storageKey);
}
```

`public` 모드(`buildStaticUrl`)는 변경 없음.

### 5. `FileController` — `Cache-Control` 응답 헤더

```java
// FileController.java
@GetMapping("/{fileUuid}/url")
public SuccessResponse<ImageUrlResponse> getImageUrl(
    @PathVariable String fileUuid,
    HttpServletResponse response) {
    if (storageProperties.isPresignedAccess()) {
        long maxAge = Math.max(storageProperties.getPresignedExpirationSeconds() - 60, 0);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "private, max-age=" + maxAge);
    }
    return SuccessResponse.success(fileService.getImageUrl(fileUuid));
}
```

- `private`: 공유 캐시(CDN)에는 저장하지 않음. URL에 서명이 포함되어 있어 사용자별 다를 수 있음.
- `max-age`: presigned 만료 − 60s. 서버 캐시 TTL과 동일하게 맞춰 일관성 유지.
- `public` 모드에서는 헤더를 추가하지 않음 (Spring의 기본 동작 유지).

---

## Result — 결과 및 한계

### 개선된 점

| 항목 | before | after |
|---|---|---|
| 동일 파일 URL 재사용 | 매 API 호출마다 새 URL | 240s 동안 동일 URL 반환 |
| S3 서명 API 호출 | 모든 요청 | 캐시 미스 시만 (최대 1회/240s per key) |
| Service Worker API 캐싱 | 불가 (URL 가변) | 가능 (`Cache-Control: max-age=240`) |
| 캐시 TTL 관리 | 전역 단일 TTL만 지원 | 캐시별 독립 TTL 지원 |

### 한계 (근본 해결 아님)

- **이미지 바이너리 자체의 CDN 캐싱은 여전히 불가**
  - URL에 `X-Amz-Signature`가 포함되어 있어 CDN이 캐시 키로 사용 불가
  - 완전한 해결은 `access-mode=public` 전환 또는 CloudFront + OAC 구성 필요
- **수평 확장 시 캐시 불일치 가능**
  - 서버 인스턴스마다 독립 Caffeine 캐시 → 동일 key라도 인스턴스에 따라 다른 URL 반환 가능
  - 허용 가능: 두 URL 모두 유효한 서명 URL이므로 클라이언트 동작에 문제 없음

### 후속 권장 작업

1. **CloudFront + OAC 적용** (장기) — S3 버킷 비공개 유지하면서 URL 완전 고정
2. 혹은 **`access-mode=public` + Caddy `Cache-Control` 헤더** (단기) — 이미지가 공개 콘텐츠인 경우

---

## 변경 파일 목록

| 파일 | 변경 유형 |
|---|---|
| `infra/storage/PresignedUrlCacheService.java` | 신규 |
| `global/config/LocalCacheConfig.java` | 수정 (per-cache TTL 활성화) |
| `domain/file/service/FileService.java` | 수정 (캐시 서비스 위임) |
| `domain/file/controller/FileController.java` | 수정 (Cache-Control 헤더) |
| `resources/application.yml` | 수정 (presigned-url TTL 설정 추가) |
