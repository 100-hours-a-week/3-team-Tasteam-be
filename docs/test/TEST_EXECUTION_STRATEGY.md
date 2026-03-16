# 테스트 실행 전략 계약

이 문서는 현재 `app-api/build.gradle`과 `src/test/java/com/tasteam/config` 구성 기준으로,
테스트를 어떻게 실행하고 어떤 컨텍스트를 재사용해야 하는지 정리한다.

## 1. Gradle 태스크와 태그 계약

| 태스크 | 현재 설정 | 실제 포함 범위 | 비고 |
| --- | --- | --- | --- |
| `./gradlew test` | `useJUnitPlatform { excludeTags 'perf' }` | `unit`, `repository`, `integration`, `service-integration`, `job-integration` 등 `perf`를 제외한 전체 | 기본 회귀 실행 경로 |
| `./gradlew unitTest` | `includeTags 'unit'` | `@UnitTest` 계열만 | 빠른 로컬 루프용 |
| `./gradlew integrationTest` | `includeTags 'integration'` | `@IntegrationTest`, `@MessageQueueFlowTest` | 현재 `@ServiceIntegrationTest`, `@RepositoryJpaTest`, `@JobIntegrationTest`는 포함되지 않음 |
| `./gradlew performanceTest` | `includeTags 'perf'` | `@PerformanceTest` 계열 | 기본 `test`에서는 제외 |

현재 태그 체계:

- `unit`
- `repository`
- `integration`
- `service-integration`
- `job-integration`
- `perf`

주의:

- `repository`, `service-integration`, `job-integration` 전용 Gradle 태스크는 아직 없다.
- 현재 `integrationTest` 태스크 이름만 보면 통합 테스트 전체처럼 보이지만, 실제로는 `integration` 태그만 실행한다.

## 2. 전역 테스트 런타임 계약

`tasks.withType(Test).configureEach` 기준 공통 동작:

- `spring.profiles.active=test`를 항상 주입한다.
- `maxParallelForks = 1`로 고정한다.
- 테스트 실행 전 `build/test-results/<task>`와 `build/tmp/<task>`를 정리한다.
- `test` 태스크는 실행 전 `build/jacoco/test.exec`를 삭제한다.

의미:

- 병렬 포크를 제한해 공유 Mockito Bean과 Testcontainers 자원 충돌을 줄인다.
- 중단된 이전 실행의 결과물 때문에 발생하는 flaky 리포트 실패를 방지한다.

## 3. 공통 테스트 컨텍스트 계약

### 3.1 `BaseControllerWebMvcTest`

공용 사용자/운영성 WebMvc 슬라이스 베이스 클래스다.

- `@ControllerWebMvcTest`로 일반 사용자 컨트롤러와 공통 운영성 컨트롤러를 묶어 로드한다.
- 공통 `@MockitoBean`과 테스트 보안 mock을 한 곳에 모아 컨텍스트 재사용률을 높인다.
- `MockMvc`, `ObjectMapper`, `JwtTokenProvider`, `JwtCookieProvider`를 기본 제공한다.

포함 범위:

- 그룹/서브그룹/리뷰/검색/메인/멤버/파일/알림/프로모션/공지
- 인증, 채팅, 분석 수집, 음식 카테고리, 지오코딩, 신고, 헬스체크, 테스트용 웹훅

### 3.2 `BaseAdminControllerWebMvcTest`

관리자 및 MQ 운영용 WebMvc 슬라이스 베이스 클래스다.

- 관리자 로그인/콘텐츠 관리/잡 제어/MQ 운영 컨트롤러를 하나의 `@ControllerWebMvcTest` 컨텍스트로 묶는다.
- 관리자 로그인과 MQ 운영 테스트에 필요한 프로퍼티를 베이스에 고정해 별도 `@TestPropertySource` 분기를 줄인다.
- `MockMvc`, `ObjectMapper`, `JwtTokenProvider`, `JwtCookieProvider`를 기본 제공한다.

포함 범위:

- `Admin*Controller`
- `AdminNotificationController`
- `MessageQueueTraceAdminController`
- `UserActivityOutboxAdminController`

운영 규칙:

- 같은 컨텍스트를 공유할 수 있는 컨트롤러 테스트는 두 베이스 클래스 중 하나를 우선 사용한다.
- 새 WebMvc 테스트에서 로컬 `@MockitoBean`을 추가하기 전에 해당 베이스에 이미 있는지 확인한다.
- 컨트롤러별로 전혀 다른 Mockito 조합이나 프로퍼티 조합을 만들면 컨텍스트 캐시가 분리된다.

### 3.2 `TestSecurityConfig`

테스트용 보안 빈 계약:

- `TestCurrentUserContext`
- 테스트용 `CurrentUserArgumentResolver`
- `JwtTokenProvider` / `JwtCookieProvider` mock
- `RefreshTokenArgumentResolver`
- `LogoutHandler` mock
- `SecurityResponseSender`

의미:

- WebMvc/통합 테스트에서 실제 인증 필터 전체를 올리지 않고도 현재 사용자 컨텍스트를 흉내낼 수 있다.

### 3.3 `TestStorageConfiguration`

테스트용 스토리지/비동기 실행 계약:

- `StorageClient`를 `FakeStorageClient`로 대체한다.
- `searchQueryExecutor`, `mainQueryExecutor`를 `Runnable::run`으로 고정한다.

의미:

- S3 같은 외부 스토리지를 실제로 호출하지 않는다.
- 일부 비동기성은 테스트에서 동기 실행으로 단순화한다.

운영 규칙:

- 스토리지 관련 통합 테스트에서 `StorageClient`를 별도 `@MockitoBean`으로 다시 교체하기 전에 Fake로 충분한지 먼저 확인한다.

### 3.4 `TestcontainersConfiguration`

테스트 DB/Redis 계약:

- PostGIS 기반 PostgreSQL 컨테이너 재사용(`withReuse(true)`)
- Redis 컨테이너 재사용(`withReuse(true)`)
- PostgreSQL `max_connections=200`
- `db/init-extensions.sql`로 `pg_trgm` 확장 초기화

의미:

- `@ServiceIntegrationTest`, `@RepositoryJpaTest`, `@PerformanceTest`, `@IntegrationTest` 계열은 실제 Testcontainers 환경을 공유한다.

## 4. 컨텍스트 재사용 운영 규칙

트러블슈팅 문서 기준으로 현재 지켜야 하는 규칙:

1. 공통 Fake가 이미 있으면 `@MockitoBean`보다 Fake를 우선 사용한다.
2. 테스트 클래스별 `@MockitoBean` 조합을 필요 이상으로 다르게 만들지 않는다.
3. WebMvc 테스트는 가능하면 `BaseControllerWebMvcTest` 또는 `BaseAdminControllerWebMvcTest`를 재사용한다.
4. 병렬 포크를 임의로 늘리지 않는다.
5. Testcontainers 재사용이 깨졌을 때만 컨테이너를 수동 정리한다.

관련 문서:

- [Spring 테스트 컨텍스트 분리 - @MockitoBean 과다 선언](https://github.com/100-hours-a-week/3-team-tasteam-wiki/wiki/%5BTroubleshooting%5D-Spring-테스트-컨텍스트-분리-MockitoBean-과다-선언)
- [Testcontainers 테스트 속도 개선](https://github.com/100-hours-a-week/3-team-tasteam-wiki/wiki/%5BTroubleshooting%5D-Testcontainers-테스트-속도-개선)
- [병렬 테스트에서 Mockito Bean 충돌](https://github.com/100-hours-a-week/3-team-tasteam-wiki/wiki/%5BTroubleshooting%5D-%EB%B3%91%EB%A0%AC-%ED%85%8C%EC%8A%A4%ED%8A%B8%EC%97%90%EC%84%9C-Mockito-Bean-%EC%B6%A9%EB%8F%8C)

## 5. 현재 실행 명령 권장안

- 빠른 단위 확인: `./gradlew unitTest`
- 기본 회귀: `./gradlew test`
- MQ wiring 확인: `./gradlew integrationTest --tests '*MessageQueueFlow*'`
- 성능/동시성 확인: `./gradlew performanceTest`

## 6. 현재 갭

- `repository`, `service-integration`, `job-integration` 전용 태스크가 없어 현재는 `test` 또는 개별 `--tests` 실행에 의존한다.
- `SearchServiceConcurrencyIntegrationTest`는 이름과 클래스 `@DisplayName`은 통합 테스트처럼 보이지만 실제 실행 태그는 `perf`다.
