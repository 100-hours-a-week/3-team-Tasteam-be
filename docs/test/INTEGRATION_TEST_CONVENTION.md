# 통합 계열 테스트 컨벤션

> 대상: `@IntegrationTest`, `@ServiceIntegrationTest`, `@JobIntegrationTest`, `@MessageQueueFlowTest`, `@PerformanceTest`

이 문서는 백엔드의 통합 계열 테스트를 현재 코드 기준으로 구분한다.
공통 목적은 **실제 빈 연결과 상태 변화가 운영과 같은 조건에서 유지되는지 확인하는 것**이다.

## 1. 현재 테스트 종류

### 1.1 `@IntegrationTest`

- 전체 Spring Boot 컨텍스트 + `MockMvc`
- `integration` 태그
- `TestSecurityConfig`, `TestStorageConfiguration`, `TestcontainersConfiguration` import

용도:

- HTTP 전체 플로우를 끝까지 검증할 때

현재 상태:

- `GroupSubgroupApiSmokeIntegrationTest`가 현재 이 어노테이션을 사용한다.

### 1.2 `@ServiceIntegrationTest`

- 전체 Spring Boot 컨텍스트(webEnvironment NONE)
- `service-integration` 태그
- `TestStorageConfiguration`, `TestcontainersConfiguration` import

용도:

- 서비스 유즈케이스 플로우
- 실제 Repository / 트랜잭션 / 설정 / Fake 스토리지 연동 검증

현재 주력 통합 테스트는 대부분 이 어노테이션을 사용한다.

### 1.3 `@JobIntegrationTest`

- `@ServiceIntegrationTest` 기반
- `job-integration` 태그 추가

용도:

- 배치/스케줄러/잡성 플로우

현재 상태:

- 어노테이션은 존재하지만 구체 테스트 클래스는 아직 없다.

### 1.4 `@MessageQueueFlowTest`

- `test` 프로필 + `integration` 태그
- 개별 테스트가 자체 `@SpringBootTest(classes = TestConfig.class, webEnvironment = NONE)`를 함께 선언

용도:

- MQ 발행/구독 wiring
- topic / consumer-group / payload 직렬화 계약
- producer/consumer mock을 통한 publish/subscribe 연결 검증

### 1.5 `@PerformanceTest`

- 전체 Spring Boot 컨텍스트(webEnvironment NONE)
- `perf` 태그

용도:

- 동시성, 멱등성, 경합 상황의 일관성
- 기본 `test` 태스크에서는 제외하고 별도로 실행

## 2. 무엇을 검증하는가

- 유즈케이스 성공/실패 결과
- 실제 빈 연결, 트랜잭션 경계, DB 상태 변화
- Fake 스토리지/Fake 외부 컴포넌트가 포함된 실제 서비스 흐름
- MQ 라우팅/구독 배선
- 동시 요청 상황에서의 최종 상태 일관성

## 3. 무엇을 검증하지 않는가

- 모든 HTTP 세부 필드 계약
- 모든 도메인 경계값과 정책 판단
- 모든 JPA 쿼리 세부
- 구현 내부 호출 순서

이 항목은 각각 WebMvc, 도메인/단위, Repository 테스트가 담당한다.

## 4. 작성 패턴

### 4.1 서비스 통합

- Given: Fixture + 실제 Repository 저장
- When: 서비스 진입점 호출
- Then: 핵심 결과 + DB 상태 + 의미 있는 부작용 검증

### 4.2 HTTP 통합

- Given: 실제 DB 상태 구성
- When: `MockMvc` 호출
- Then: 상태 코드 + 핵심 응답 + DB 상태

### 4.3 MQ 플로우

- Given: 테스트 전용 `TestConfig`에 producer/consumer mock과 serializer/policy 빈 정의
- When: 이벤트 발행 또는 handler 호출
- Then: publish payload, topic, subscription, downstream collaborator 호출 검증

### 4.4 성능/동시성

- Given: 실제 DB/토큰/회원/검색 데이터 준비
- When: 멀티 스레드 동시 호출
- Then: 최종 활성 상태, 히스토리 적재, 토큰 저장 상태 등 일관성 검증

## 5. 외부 협력자 처리

- 내부 서비스와 Repository는 실제 빈 사용이 기본이다.
- 외부 시스템은 Fake/Stub를 우선한다.
  - `TestStorageConfiguration.FakeStorageClient`
  - `com.tasteam.config.fake` 패키지 구현체
- 통합 테스트에서 `@MockitoBean`은 최소화한다.

## 6. 격리와 실행

- 많은 서비스 통합 테스트가 `@Transactional`로 롤백 격리를 사용한다.
- 성능 테스트는 별도 정리 코드를 두는 경우가 많다.
- 전역 포크 수는 `1`로 제한되어 있다.
- Testcontainers는 재사용을 전제로 동작한다.

## 7. 현재 인벤토리 해석 원칙

- 어떤 클래스가 어떤 시나리오를 보장하는지는 `USECASE_TEST_TARGETS.md`를 기준으로 본다.
- 구현이 끝나지 않은 테스트는 계약 완료로 보지 않고 “구현 예정 갭”으로 기록한다.

## 8. 현재 갭

- `@JobIntegrationTest`는 현재 예비 어노테이션에 가깝다.
- `SearchServiceConcurrencyIntegrationTest`는 성능 태그(`perf`)를 사용하지만 클래스명/표시는 통합 테스트처럼 보인다.
