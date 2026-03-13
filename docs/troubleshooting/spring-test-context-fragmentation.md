## Spring 테스트 컨텍스트 분리 — @MockitoBean 과다 선언

### 상황

`@ServiceIntegrationTest`(`@SpringBootTest`) 기반 테스트들이 예상보다 느리고,
일부 테스트에서 컨텍스트 초기화 비용이 중복 발생하는 징후가 관찰됐다.
`AdminRestaurantServiceIntegrationTest`가 `@MockitoBean StorageClient`를 선언하면서
다른 `@ServiceIntegrationTest` 클래스들과 컨텍스트를 공유하지 못하고 있었다.

### 발생 원인

Spring Boot 테스트 컨텍스트 캐시 키는 다음 요소의 조합으로 결정된다.

- `@SpringBootTest` 설정 (webEnvironment 등)
- `@ActiveProfiles`
- `@Import` 클래스 목록
- **`@MockitoBean` 선언 목록 (타입 + 이름)**

`@MockitoBean` 구성이 하나라도 다르면 **별도의 컨텍스트**가 생성된다.

`AdminRestaurantService`가 `StorageClient`를 직접 사용하던 시절에는 mock이 필요했다.
그러나 URL 생성 로직을 `FileService.getPublicUrl()`로 위임한 이후에도
`@MockitoBean StorageClient`가 테스트에 남아 있었고,
결과적으로 이 테스트만 단독 컨텍스트를 유지하게 됐다.

```
// 변경 전 — 불필요한 mock이 컨텍스트를 분리시킴
@MockitoBean StorageClient storageClient;           // AdminRestaurantService가 직접 사용
@MockitoBean NaverGeocodingClient naverGeocodingClient;
@MockitoBean RestaurantEventPublisher restaurantEventPublisher;

// 변경 후 — FakeStorageClient(TestStorageConfiguration)로 충분하므로 제거
@MockitoBean NaverGeocodingClient naverGeocodingClient;
@MockitoBean RestaurantEventPublisher restaurantEventPublisher;
```

`TestStorageConfiguration`이 profile `test`에서 `FakeStorageClient`를 이미 등록하므로
`@MockitoBean StorageClient` 없이도 `FileService` 내부의 `StorageClient` 호출이 정상 처리된다.

### 해결

`AdminRestaurantServiceIntegrationTest`에서 `@MockitoBean StorageClient` 제거.
이로써 `RestaurantServiceIntegrationTest`(동일한 mock 구성)와 컨텍스트를 공유하게 됐다.

### 현재 컨텍스트 현황 (2026-03-13 기준)

총 **8개** 고유 컨텍스트 생성.

| # | 어노테이션 | @MockitoBean 구성 | 공유 테스트 수 |
|---|-----------|-----------------|-------------|
| 1 | `@RepositoryJpaTest` | 공통 import만 사용 | 18 |
| 2 | `@ServiceIntegrationTest` | 없음 | 다수 서비스 통합 테스트 공유 |
| 3 | `@ServiceIntegrationTest` + `@TestPropertySource` | 없음 | 1 (`GroupEmailRateLimitIntegrationTest`) |
| 4 | `BaseAdminControllerWebMvcTest` | 관리자/MQ 운영 공통 mock | 관리자 WebMvc 테스트 다수 공유 |
| 5 | `BaseControllerWebMvcTest` | 사용자/운영성 공통 mock | 일반 WebMvc 테스트 다수 공유 |
| 6 | `@IntegrationTest` | 없음 | 1 (`GroupSubgroupApiSmokeIntegrationTest`) |
| 7 | 커스텀 `@SpringBootTest` (`@MessageQueueFlowTest`) | 테스트 전용 `TestConfig` | 1 |
| 8 | 커스텀 `@SpringBootTest` (`@MessageQueueFlowTest`) | 테스트 전용 `TestConfig` | 1 |

**개선 결과:** 컨텍스트 36개 → 8개 (-28).

핵심 변경:

- `BaseControllerWebMvcTest`에 인증/채팅/지오코딩/헬스체크/웹훅 등 공통 WebMvc 테스트를 편입.
- `BaseAdminControllerWebMvcTest`를 추가해 관리자/MQ 운영 컨트롤러를 하나의 슬라이스 컨텍스트로 통합.
- `RepositoryJpaTest` 메타 어노테이션으로 `DummyDataJdbcRepository`를 공통 import하고, 개별 repository 테스트의 `@Import`를 제거.
- HTTP 풀플로우 스모크 테스트를 `@IntegrationTest`로 정리.

### 컨텍스트 생성 횟수 확인 방법

`application-test.yml`에 아래 로깅 설정이 활성화되어 있다.

```yaml
logging:
  level:
    org.springframework.test.context.support: DEBUG
```

테스트 실행 로그에서 아래 패턴으로 확인한다.

```
# 새 컨텍스트 생성
Loading ApplicationContext for [MergedContextConfiguration ...]

# 캐시 히트 (재사용)
Retrieved ApplicationContext [...] from cache
```

CLI에서 바로 세는 법:

```bash
./gradlew test 2>&1 | grep -c "Loading ApplicationContext"
```

### 예방 가이드

새로운 `@ServiceIntegrationTest` 테스트를 작성할 때:

1. `@MockitoBean`을 추가하기 전에 `TestStorageConfiguration` 등 기존 fake/stub이 이미 처리하는지 확인한다.
2. 같은 mock 조합을 가진 기존 테스트 클래스가 있다면 그 클래스와 컨텍스트를 공유하도록 맞춘다.
3. 서비스 간 의존 관계가 변경됐을 때 더 이상 필요 없는 `@MockitoBean`을 함께 제거한다.
