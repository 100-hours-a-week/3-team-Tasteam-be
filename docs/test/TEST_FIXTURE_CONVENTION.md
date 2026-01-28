# 테스트 픽스처 컨벤션

## 1. 공통 규칙

- 위치
  - 공용 픽스처 코드는 Gradle `java-test-fixtures` 소스셋(`src/testFixtures/java`)에 둡니다.
  - 모듈 간 공유는 `testImplementation(testFixtures(project(":xxx")))`로만 합니다.
- 클래스
  - 네이밍: **대상 + Fixture**
    - 예: `PostFixture`, `MemberFixture`, `OrderRequestFixture`, `OrderQueryDtoFixture`
  - `final` 클래스, `private` 기본 생성자
  - 인스턴스 상태를 가지지 않고, `static` 메서드와 `static final` 상수만 사용합니다.
- 상수
  - 유효한 기본값: `DEFAULT_` 접두어 (`DEFAULT_TITLE`, `DEFAULT_TOTAL_AMOUNT` 등)
  - 대표적인 변경 값: `UPDATED_` 접두어 (`UPDATED_TITLE` 등)
- 메서드 네이밍
  - 엔티티: `create(...)`, `createWithId(...)`, 필요 시 `createCompleted()`, `createCanceled()` 등 도메인 상태를 이름에 포함
  - 요청 DTO: `createRequest()`, `updateRequest()`, 실패/경계값은 `createRequestWithoutXxx()`, `createRequestWithInvalidXxx()`
  - 응답/조회 DTO: `create()`, `createWithAllFields(...)`

---

## 2. Gradle / java-test-fixtures

- 플러그인
  - 공용 도메인/애플리케이션 모듈에는 `java` + `java-test-fixtures`를 적용합니다.
- 의존성
  - 픽스처 정의 모듈:
    - 테스트 전용 라이브러리: `testFixturesImplementation`, `testFixturesRuntimeOnly`
  - 픽스처 사용하는 모듈:
    - `testImplementation(testFixtures(project(":domain")))`
    - 필요 시 `testRuntimeOnly(testFixtures(project(":domain")))`

```groovy
plugins {
    id 'java'
    id 'java-test-fixtures'
}

dependencies {
    testImplementation(testFixtures(project(":domain")))
}
```

---

## 3. 레이어별 사용 규칙

### 3.1 도메인 테스트 (Entity / Policy)

- 대상 엔티티(SUT)
  - 가능한 한 Fixture 대신 **실제 정적 팩토리/생성자**를 직접 호출합니다.
  - 예: `PostFixture`로 Post를 만들지 말고, `Post.create(member, "제목", "내용")`을 직접 사용합니다.
- 협력 엔티티
  - 연관 엔티티/부수적인 도메인 객체는 Fixture를 적극 사용합니다.
  - 예: `MemberFixture.create()`로 Member 생성 후, 이를 이용해 `Post.create(...)` 호출
- ID
  - 도메인 테스트에서는 보통 ID가 필요 없으므로 `create()` 위주로 사용합니다.
  - 복합키/ID 로직 검증이 필요할 때만 `createWithId(...)`를 사용합니다.
- Policy 테스트
  - 원시 타입(Long 등)과 Repository Mock 조합이 주가 되며, Fixture는 최소한만 사용합니다.

### 3.2 Repository 테스트

- 엔티티 생성
  - JPA 저장 전 엔티티는 Fixture로 생성합니다 (`MemberFixture.create()`, `PostFixture.create(member)` 등).
  - Repository 테스트에서는 `createWithId(...)`보다 **저장 전 상태를 만드는 `create(...)`**를 선호합니다.
- 복잡한 초기 상태
  - 여러 엔티티가 얽힌 초기 상태는 Fixture 조합으로 만들고,
  - 테스트 본문에서는 핵심 차이(다른 회원, 삭제 플래그 등)만 드러나도록 구성합니다.
- Stub/Fake Repository
  - Repository 테스트에서는 실제 JPA 구현만 사용하며, Stub/Fake Repository는 사용하지 않습니다.

### 3.3 서비스 단위 테스트

- Given 단계
  - 요청 DTO: `*RequestFixture.createRequest()`, `updateRequest()`를 기본으로 사용합니다.
  - 도메인 객체: `*Fixture.create()` / `createWithId()`로 준비합니다.
- Then 단계
  - 반환 DTO는 Fixture로 감추지 않고, 핵심 필드를 직접 검증합니다.
  - 여러 테스트에서 동일한 Response 구성이 반복될 때만 Response용 Fixture/헬퍼를 도입할 수 있습니다.

### 3.4 Controller WebMvc 테스트

- Request
  - Request DTO는 Fixture를 기본으로 사용합니다.
  - 예: `PostRequestFixture.createRequest()`, `CommentRequestFixture.createRequestWithoutContent()`
- Response
  - HTTP 계약을 테스트 본문에서 직접 읽을 수 있도록, `jsonPath`로 핵심 필드를 검증합니다.
  - 복잡한 공통 응답 구조(페이지 응답 등)에 한해 Response Fixture/헬퍼를 허용하되,
    핵심 필드는 여전히 `jsonPath`로 직접 검증합니다.

### 3.5 통합 테스트

- Given
  - 도메인 Fixture로 엔티티 생성 후 실제 Repository를 통해 저장합니다.
  - Request DTO는 Application 레벨 Fixture(`*RequestFixture`)를 사용합니다.
- Then
  - HTTP 상태 코드와 응답의 핵심 필드만 가볍게 검증합니다.
  - DB 상태 검증에 Fixture를 직접 사용하지 않고, 실제 Repository 조회 결과로 검증합니다.

---

## 4. Fixture 작성·변경 시 기준

- 언제 새 Fixture를 만드는지
  - 같은 엔티티/DTO를 두 번 이상 생성해야 할 때
  - 필드가 많아 Given 절이 길어질 때
  - 여러 테스트/레이어에서 같은 데이터 패턴을 사용할 때
- 메서드는 최소 단위로 유지합니다.
  - 기본 시나리오: `create()`, `createRequest()`, `updateRequest()`
  - 실패/경계값: 의미 있는 것만 메서드로 분리합니다.
- 비즈니스 규칙은 Fixture에 넣지 않습니다.
  - 복잡한 도메인 로직/정책은 도메인/서비스 코드와 테스트에서 직접 검증하고,
  - Fixture는 “유효한 기본값 + 편의 생성” 수준으로만 유지합니다.
