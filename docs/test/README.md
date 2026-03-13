# 테스트 코드 계약 허브

이 문서는 `3-team-Tasteam-be` 백엔드 모듈의 테스트 계약을 한 곳에서 찾기 위한 허브다.
기준은 **현재 코드가 실제로 보장하는 테스트 계약**이며, 아직 전면 적용되지 않은 이상 상태는 별도 갭으로 분리한다.

## 1. 테스트 레이어 책임 매트릭스

| 레이어 | 주 어노테이션/형태 | 핵심 책임 | 세부 문서 |
| --- | --- | --- | --- |
| 도메인 / 정책 | `@UnitTest` + 순수 객체 | 불변식, 상태 전이, 정책 판단 | `DOMAIN_TEST_CONVENTION.md` |
| 단위 테스트 공통 | `@UnitTest` | 행위 중심 검증, 빠른 피드백 | `TEST_COMMON_CONVENTION.md` |
| Repository | `@RepositoryJpaTest` | JPA 매핑, QueryDSL/쿼리 결과, 제약조건 | `REPOSITORY_TEST_CONVENTION.md` |
| Controller WebMvc | `@ControllerWebMvcTest` | 요청 바인딩, Validation, 예외 매핑, 응답 계약 | `CONTROLLER_WEBMVC_TEST_CONVENTION.md` |
| 서비스 통합 | `@ServiceIntegrationTest` | 유즈케이스 플로우, 실제 빈 연결, DB 상태 변화 | `INTEGRATION_TEST_CONVENTION.md` |
| HTTP 통합 | `@IntegrationTest` | 전체 컨텍스트 + `MockMvc` 기반 HTTP 플로우 | `INTEGRATION_TEST_CONVENTION.md` |
| MQ 플로우 | `@MessageQueueFlowTest` + 개별 `@SpringBootTest` | 발행/구독 wiring, topic/consumer-group 계약, 메시지 직렬화 흐름 | `INTEGRATION_TEST_CONVENTION.md` |
| 잡/배치 통합 | `@JobIntegrationTest` | 배치/리스너성 플로우를 서비스 통합 규약으로 실행 | `INTEGRATION_TEST_CONVENTION.md` |
| 성능/동시성 | `@PerformanceTest` | 동시성, 멱등성, 경합 시 일관성 | `INTEGRATION_TEST_CONVENTION.md`, `TEST_EXECUTION_STRATEGY.md` |
| 비동기 이벤트 | 단위 + 선택적 통합 | 리스너 결과, AFTER_COMMIT 연결, 에러 격리 | `ASYNC_TEST_GUIDE.md` |

## 2. 커스텀 테스트 어노테이션 계약

| 어노테이션 | 현재 구성 | 현재 사용 위치 |
| --- | --- | --- |
| `@UnitTest` | Mockito 확장 + `unit` 태그 | 순수 서비스, 정책, 유틸, 리스너 단위 테스트 |
| `@RepositoryJpaTest` | `@DataJpaTest`, `test` 프로필, QueryDSL/JPA Auditing/Testcontainers, `repository` 태그 | 리포지토리/JPA 슬라이스 테스트 |
| `@ControllerWebMvcTest` | `@WebMvcTest`, `MockMvc`, 필터 비활성화, `TestSecurityConfig` import | 컨트롤러 슬라이스 테스트 |
| `@IntegrationTest` | 전체 Spring Boot + `MockMvc`, `integration` 태그 | HTTP 전체 플로우용 어노테이션. 현재 `GroupSubgroupApiSmokeIntegrationTest`가 사용 |
| `@ServiceIntegrationTest` | 전체 Spring Boot(webEnvironment NONE), `service-integration` 태그, storage/testcontainers import | 핵심 유즈케이스 통합 테스트 |
| `@MessageQueueFlowTest` | `test` 프로필 + `integration` 태그 | MQ wiring 플로우 테스트. 별도 `TestConfig`와 함께 사용 |
| `@JobIntegrationTest` | `@ServiceIntegrationTest` + `job-integration` 태그 | 배치/잡 전용 어노테이션. 현재 구체 클래스는 없음 |
| `@PerformanceTest` | 전체 Spring Boot(webEnvironment NONE), `perf` 태그 | 동시성/성능 테스트 |

## 3. Test Double / Fixture 우선순위

1. 실제 객체
2. Fake / Stub
3. Mock
4. Spy 최후 수단

- 외부 시스템은 가능한 한 테스트 전용 Fake로 치환한다.
  - 예: `TestStorageConfiguration.FakeStorageClient`
  - 예: `com.tasteam.config.fake` 패키지의 Fake 구현체
- WebMvc 슬라이스에서는 `@MockitoBean`으로 서비스 계층을 교체한다.
- 통합 테스트에서는 불필요한 `@MockitoBean` 대신 공통 Fake/Stub Bean을 우선한다.
- 공용 Fixture는 `app-api/src/testFixtures/java/com/tasteam/fixture` 아래에 둔다.

관련 문서:

- `TEST_DOUBLE_CONVENTION.md`
- `TEST_FIXTURE_CONVENTION.md`
- `why/TEST_FIXTURE.md`

## 4. 실행 전략

실행 태그/태스크와 컨텍스트 재사용 규칙은 별도 문서로 관리한다.

- `TEST_EXECUTION_STRATEGY.md`

핵심 요약:

- `test` 태스크는 `perf`만 제외하고 대부분의 테스트를 실행한다.
- `unitTest`는 `unit` 태그만 실행한다.
- `integrationTest`는 현재 `integration` 태그만 실행하므로 `service-integration`, `repository`, `job-integration`은 포함하지 않는다.
- 테스트 전역 설정은 병렬 포크를 `1`로 제한해 Mockito Bean 공유 충돌과 Testcontainers 자원 경합을 줄인다.

## 5. 현재 인벤토리

- 서비스 통합 테스트, MQ 플로우 테스트, 성능/동시성 테스트 인벤토리는 `USECASE_TEST_TARGETS.md`에서 관리한다.
- 이 문서는 “현재 어떤 테스트가 어떤 시나리오를 보장하는가”를 추적하는 운영 문서다.

## 6. 현재 갭 / 미완성 계약

- `@JobIntegrationTest` 어노테이션은 존재하지만 현재 구체 테스트 클래스는 없다.
- `integrationTest` Gradle 태스크는 `integration` 태그만 포함하므로 `@ServiceIntegrationTest`, `@RepositoryJpaTest`, `@JobIntegrationTest`는 자동 포함되지 않는다.
- 일부 테스트 클래스는 클래스 `@DisplayName` 형식이 문서의 권장 패턴과 다르다.
  - 예: `NotificationServiceIntegrationTest`
  - 예: `SearchServiceConcurrencyIntegrationTest`
- `docs/test` 문서 일부는 문서화 시점 이전 기준 표현을 포함하고 있었으며, 현재 허브를 기준으로 지속 정리한다.
