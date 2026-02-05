# 통합 테스트 컨벤션 (Integration Test)

> 대상: `@IntegrationTest`, `@ServiceIntegrationTest`, `@JobIntegrationTest` 기반 통합 테스트
> 예: `RestaurantServiceIntegrationTest`, `ReviewServiceIntegrationTest`, `AdminGroupImageIntegrationTest` 등

이 문서는 통합 테스트가 **무엇을 보장해야 하는지**를 정의합니다.  
통합 테스트는 **유즈케이스 관점에서 비즈니스 플로우가 실제 환경과 동일하게 동작하는지**를 확인하는 레이어입니다.  
즉, “이 기능이 운영 환경에서 처음부터 끝까지 제대로 돈다”를 보장하는 것이 핵심 목적입니다.

---

## 1. 통합 테스트의 본질

통합 테스트는 다음을 검증합니다.

- **유즈케이스 플로우의 성공/실패 결과**
- **레이어 간 협력(Controller/Service/Repository/DB/보안/스토리지)**
- **트랜잭션 경계 이후의 실제 상태 변화**
- **인프라 설정이 비즈니스 로직에 미치는 영향**

통합 테스트는 **비즈니스 관점의 결과**를 검증하는 것이 핵심이며, 구현 세부나 계약의 모든 디테일을 다루지 않습니다.  
즉, “유즈케이스를 수행했을 때 비즈니스 규칙이 실제로 지켜지는가”를 끝까지 확인하는 테스트입니다.

---

## 2. 통합 테스트의 검증 범위

**검증 대상**

- End-to-End 유즈케이스 플로우
- 상태 변화 및 부작용
- Security/인증/인가 플로우
- 실제 DB 반영 및 트랜잭션 결과
- 외부 인프라와의 협력(테스트용 Stub/Fake 포함)

**검증 제외**

- 모든 분기/경계값 케이스의 완전한 커버리지
- HTTP 계약의 모든 세부 필드
- Validation/예외 매핑의 모든 조건
- JPA 매핑/쿼리 자체의 세부 구현

통합 테스트는 대표적인 성공/실패 플로우만 검증하고, 세밀한 분기 검증은 도메인/서비스/WebMvc 테스트에 위임합니다.

---

## 3. 통합 테스트 메타 어노테이션

### 3.1 @IntegrationTest (HTTP 플로우)

`app-api/src/test/java/com/tasteam/config/annotation/IntegrationTest.java`

- `@SpringBootTest` + `@AutoConfigureMockMvc`
- `@ActiveProfiles("test")`
- `@Import(TestSecurityConfig, JpaAuditingTestConfig, TestStorageConfiguration)`
- `@Tag("integration")`

HTTP 기반 유즈케이스 플로우를 통합 테스트로 검증할 때 사용합니다.

### 3.2 @ServiceIntegrationTest (비HTTP 플로우)

`app-api/src/test/java/com/tasteam/config/annotation/ServiceIntegrationTest.java`

- `@SpringBootTest(webEnvironment = NONE)`
- `@ActiveProfiles("test")`
- `@Import(TestStorageConfiguration, TestcontainersConfiguration)`
- `@Tag("service-integration")`

컨트롤러가 없는 서비스/배치/메시지 리스너 플로우를 대상으로 합니다.

### 3.3 @JobIntegrationTest (배치/잡 전용)

`app-api/src/test/java/com/tasteam/config/annotation/JobIntegrationTest.java`

- `@ServiceIntegrationTest` 기반
- `@Tag("job-integration")` 추가

배치/잡/메시지 리스너와 같은 잡성 플로우 검증 시 사용합니다.

---

## 4. 유즈케이스 중심 작성 원칙

통합 테스트는 반드시 **유즈케이스 중심으로 작성**합니다.

- 테스트 클래스는 유즈케이스 또는 기능 단위로 구성합니다.
- `@Nested`는 유즈케이스 하위 시나리오 묶음으로 사용합니다.
- 테스트는 한 번의 액션과 한 개의 비즈니스 결과만 검증합니다.
- “이 기능을 수행했을 때 비즈니스적으로 무엇이 보장되어야 하는가”를 기준으로 작성합니다.
- @DisplayName은 비즈니스 문장이어야 하며, 구현 세부 용어를 피합니다.

대표 패턴

- `게시글 생성 → DB 반영 + 연관 상태 변화`
- `회원 탈퇴 → 접근 불가 + soft delete 상태`
- `인증 실패 → 토큰 재발급 차단`

---

## 5. 테스트 작성 패턴

### 5.1 HTTP 플로우

- **Given**: 실제 Repository로 초기 상태 구성
- **When**: `MockMvc`로 엔드포인트 호출
- **Then**: 상태 코드 + 핵심 응답 필드 + DB 상태 검증

HTTP 응답 전체 스펙을 검증하지 않고, 핵심 결과만 확인합니다.

### 5.2 비HTTP 플로우

- **Given**: DB 상태 + 테스트용 Stub/Fake 구성
- **When**: 서비스/리스너/잡 진입점 호출
- **Then**: DB 상태/부작용/외부 호출 결과 검증

---

## 6. Assertion 원칙

- **핵심 결과만 검증**한다.
- 성공 플로우는 “업무적으로 중요한 결과” 1~2개만 확인한다.
- 실패 플로우는 “유즈케이스 수준의 실패 조건”만 검증한다.
- 반드시 **DB 상태 또는 부작용**을 함께 확인한다.

---

## 7. 외부 협력자 처리

- 내부 서비스는 가능한 한 실제 빈을 사용한다.
- 외부 API/클라이언트는 `@MockitoBean` 또는 Fake로 대체한다.
- 통합 테스트는 Mocking 최소화가 원칙이다.

---

## 8. 테스트 픽스처 사용

- `src/testFixtures/java`의 Fixture를 적극 재사용한다.
- Given 단계에서 데이터 생성을 간결하게 유지한다.
- 랜덤 값, 현재 시간 의존을 금지한다.

---

## 9. 트랜잭션/격리 원칙

- 기본적으로 `@Transactional`로 테스트 간 상태 격리를 유지한다.
- 커밋 이후 동작 검증이 필요하면 해당 테스트에서 트랜잭션 전략을 명시한다.

---

## 10. 언제 통합 테스트를 추가/수정할까?

- **핵심 유즈케이스가 새로 추가될 때**
- **Security/인프라 설정이 변경될 때**
- **트랜잭션/상태 변화가 복잡해질 때**
- **배치/비동기 플로우가 새로 생길 때**

---

## 11. 요약

- 통합 테스트는 **유즈케이스 플로우 + 실제 인프라 협력**을 검증하는 레이어다.
- 통합 테스트의 목적은 **비즈니스 결과가 실제 환경에서 보장되는지**를 확인하는 것이다.
- 상세 계약/모든 분기/세부 쿼리는 다른 테스트 레이어가 담당한다.
