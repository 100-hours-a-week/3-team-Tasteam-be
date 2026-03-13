# 테스트 더블 컨벤션

이 문서는 현재 백엔드 테스트에서 Test Double을 어떤 순서와 기준으로 선택하는지 정리한다.

## 1. 우선순위

1. 실제 객체
2. Fake / Stub
3. Mock
4. Spy 최후 수단

핵심 원칙:

- 가능한 한 결과와 상태 변화로 검증한다.
- 호출 여부 자체가 비즈니스 규칙일 때만 `verify`를 적극 사용한다.

## 2. 레이어별 기본 전략

### 2.1 도메인 / 정책

- 실제 객체만 사용한다.
- Test Double을 도입하지 않는다.

### 2.2 Repository

- 실제 JPA + Testcontainers PostgreSQL 사용
- Fake/Mock Repository 사용 안 함

### 2.3 서비스 단위 테스트

- Repository, 외부 클라이언트, 복잡한 협력자: `@Mock`
- 단순 정책/값 객체: 실제 객체 허용

### 2.4 Controller WebMvc

- 서비스 의존성: `@MockitoBean`
- MVC 인프라와 Jackson/Validation은 실제 빈

### 2.5 서비스 통합 / HTTP 통합

- 내부 빈은 실제 객체
- 외부 시스템은 Fake/Stub 우선
- `@MockitoBean`은 필요한 경우에만 제한적으로 사용

### 2.6 MQ 플로우 테스트

- producer / consumer / downstream collaborator는 mock으로 두는 경우가 많다.
- 이유는 이 레이어의 목적이 비즈니스 계산이 아니라 **wiring 계약** 검증이기 때문이다.

## 3. 현재 코드 기준 Fake / Stub 예시

- `TestStorageConfiguration.FakeStorageClient`
- `com.tasteam.config.fake.FakeEmailSender`
- `com.tasteam.config.fake.FakeJwtTokenProvider`
- `com.tasteam.config.fake.FakeNaverGeocodingClient`
- `com.tasteam.config.fake.FakeOAuth2Provider`
- `com.tasteam.config.fake.FakeRestaurantEventPublisher`

운영 규칙:

- 이미 공통 Fake가 있으면 새 `@MockitoBean`을 추가하기 전에 그 Fake로 충분한지 먼저 확인한다.

## 4. `@MockitoBean` 사용 규칙

- 현재 Spring Boot 테스트 코드 기준 표준 표현은 `@MockitoBean`이다.
- 주 사용처는 `@ControllerWebMvcTest`다.
- 통합 테스트에서는 컨텍스트 캐시 분리를 유발하므로 남용하지 않는다.
- 필요한 경우에도 공통 베이스나 `@TestConfiguration`으로 묶을 수 있는지 먼저 본다.

## 5. `verify` 사용 기준

허용:

- 메시지 발행, 알림 생성, 카운터 증가처럼 **호출 자체가 계약**인 경우
- MQ consumer subscribe / producer publish 같은 wiring 검증

지양:

- Repository 호출 횟수, 내부 호출 순서 같은 구현 세부 검증
- 상태 변화만 봐도 충분한 서비스/도메인 코드

## 6. Spy / Dummy

- Spy는 마지막 수단이다.
- 실제 객체를 대부분 유지하면서 특정 메서드만 감시해야 할 때만 사용한다.
- Dummy는 사용되지 않는 인자를 채우기 위한 최소 객체로 제한한다.

## 7. 현재 갭

- 과거 문서에는 존재하지 않는 `SERVICE_TEST_CONVENTION.md`, `IntegrationSecurityTest` 참조가 있었으나 현재 문서 체계에서는 사용하지 않는다.
- 일부 예시 문서에는 `@MockBean` 표현이 남아 있었으나, 현재 기준 표현은 `@MockitoBean`이다.
