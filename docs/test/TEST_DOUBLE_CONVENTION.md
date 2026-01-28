# 테스트 더블 컨벤션 (우리 프로젝트 기준)

> 목적: **우리 코드에서 실제로 어떻게 Test Double(Mock/Stub/Fake/Spy/Dummy)을 선택·사용할지**에 대한 실천 가이드

핵심 원칙은 다음과 같다.

- **실제 객체 우선**
- 안 되면 **Fake/Stub**
- 그래도 어려우면 **Mock (그리고 Spy는 정말 드물게)**  

---

## 1. 우선순위: 실제 객체 → Fake/Stub → Mock

1. **실제 객체**
   - 도메인(Entity/Policy), 단순 Service, Helper 등은 가능한 한 실제 객체만으로 테스트한다.
2. **Fake / Stub**
   - DB/HTTP/메시지/외부 시스템처럼 느리거나 제어가 어려운 의존성은 Fake/Stub를 우선 고려한다.
3. **Mock**
   - Fake/Stub로 표현하기 어렵거나, **“어떻게 호출되었는지(행위)” 자체를 검증해야 할 때**만 사용한다.

Spy는 특별한 경우에만 사용하며, 기본 선택지에는 두지 않는다(6 참고)

---

## 2. 계층별 기본 전략

- **Domain (Entity/Policy)**  
  - Test Double 금지. 항상 **실제 객체**만 사용.

- **Repository (`@RepositoryJpaTest`, `REPOSITORY_TEST_CONVENTION.md`)**  
  - 실제 JPA + 테스트 DB 사용. Test Double 사용 안 함.

- **Service 단위 테스트 (`@UnitTest`, `SERVICE_TEST_CONVENTION.md`)**
  - Repository / 외부 클라이언트 / 복잡 Policy → `@Mock`
  - 단순 Policy(외부 의존 없음) → 실제 객체 허용.

- **Controller WebMvc (`@ControllerWebMvcTest`, `CONTROLLER_WEBMVC_TEST_CONVENTION.md`)**
  - Service → `@MockitoBean`
  - MVC 인프라(바인딩, Validation, Advice)는 실제 빈 사용.

- **통합 테스트 (`@IntegrationTest` / `@IntegrationSecurityTest`, `INTEGRATION_TEST_CONVENTION.md`)**
  - Controller/Service/Repository/Security/Auditing 등은 실제 빈.
  - 외부 시스템은 Fake/Stub Config로 교체 (`@Import(FakeXxxConfig)`).

- **서비스/잡 통합 테스트 (`@ServiceIntegrationTest` / `@JobIntegrationTest`)**
  - Service/Repository/비동기/배치/리스너는 실제 빈.
  - 외부 시스템은 Fake/Stub로 교체.

---

## 3. Mock 사용 컨벤션

### 3.1 Mock 사용 시점

- **Service 단위 테스트**
  - Repository, 외부 클라이언트, 복잡한 Policy(`PostLikePolicy` 등)는 기본적으로 `@Mock`.
  - 예:
    - `PostRepository`, `MemberRepository`, `PostLikeRepository`
    - 외부 HTTP 클라이언트 인터페이스

- **Controller WebMvc 테스트**
  - Service를 `@MockitoBean`으로 교체해, 컨트롤러가 Service 응답/예외를 어떻게 HTTP로 매핑하는지 검증한다.

### 3.2 행위 검증(verify) 기준

- **허용되는 경우 (비즈니스적으로 의미 있는 행위)**
  - 댓글/좋아요 카운트 증가·감소 (`incrementCommentCount`, `decrementLikeCount` 등).
  - 이메일/알림/결제 요청 등 “실행 자체”가 도메인 규칙인 행위.

- **지양하는 경우**
  - Repository 메서드 호출 횟수/순서 등 **내부 구현** 검증.
  - “이 메서드가 몇 번 불렸는지”보다 “결과/상태가 어떻게 변했는지”를 우선한다.

정리: Mock은 **“무엇을 호출해야 하는지가 규칙인 경우”**에만, 그리고 최소한으로 사용한다.

---

## 4. Stub / Fake 사용 컨벤션

### 4.1 Stub

- 정의: “이 입력에 이 값을 돌려준다”에만 관심 있는 **대답 전용 Test Double**.

- 사용처:
  - 외부 정책 서비스, 환율/할인율 제공자 등.
  - HTTP 클라이언트의 응답을 고정해 비즈니스 로직만 테스트할 때.

- 구현 방법:
  - 인터페이스를 구현하는 간단한 테스트용 클래스.
  - 또는 테스트 내부에서 람다/익명 클래스로 구현.

### 4.2 Fake

- 정의: 실제와 비슷하지만 간단하게 구현된 **인메모리/로컬 구현**.

- 사용처:
  - 인메모리 Repository, 인메모리 큐/캐시 등.
  - 통합 테스트까지 가지 않고도, DB 없는 “가벼운 플로우 테스트”를 하고 싶을 때.

- 우리 프로젝트 기준:
  - 이미 Fixture + Repository 통합 테스트가 잘 구성되어 있어, Fake Repository를 적극적으로 만들 필요는 크지 않다.
  - 다만, **복잡한 Service 플로우를 DB 없이 빠르게 검증하고 싶을 때**는 인메모리 Fake를 도입하는 것을 허용한다.

---

## 5. @MockitoBean (구 @MockBean) 사용 컨벤션

- Spring Boot 3.4+에서는 전통적인 `@MockBean` 대신 `@MockitoBean`을 사용한다.

- **주요 사용처**
  - `@ControllerWebMvcTest`에서 Service를 대체할 때:
    - 실제 Service 빈을 올리지 않고, 컨트롤러만 슬라이스로 검증하기 위함.

- **통합 테스트에서는 되도록 사용하지 않는다**
  - `@IntegrationTest` / `@ServiceIntegrationTest`에서는 실제 빈 구성을 유지하는 것이 기본이다.
  - 반드시 필요할 때는:
    - `@TestConfiguration` + 테스트 전용 Stub/Fake Bean을 정의하고,
    - `@Primary`로 등록해 일부 의존성만 교체하는 패턴을 우선 고려한다.

정리: `@MockitoBean`은 주로 **WebMvc 슬라이스 테스트에서 Service를 대체할 때만** 사용하고,  
통합 테스트에서는 컨텍스트 오염을 최소화하기 위해 남용하지 않는다.

---

## 6. Spy 사용 컨벤션

Spy는 **실제 객체를 감싸 호출 기록을 남기는 Test Double**이다. 우리 프로젝트에서는 Spy를 다음처럼 제한적으로만 사용한다.

- **사용을 고려할 수 있는 경우**
  - 실제 구현을 대부분 그대로 쓰되,
    - 특정 메서드만 Stub 하고 싶거나,
    - 특정 메서드 호출 여부를 검증해야 할 때.
  - 예:
    - 콜백/리스너 내부에서 특정 메서드가 실제로 호출되었는지 확인해야 하지만,
    - 전체 객체를 Mock으로 바꾸면 나머지 동작이 너무 많이 사라지는 경우.

- **지양하는 경우**
  - 순수 도메인/Service 코드에 Spy를 남발해 내부 구현에 강하게 결합되는 경우.
  - Repository/외부 클라이언트에 Spy를 적용해 호출 여부를 과도하게 추적하는 경우.

기본 원칙:

- “실제 객체로 충분히 검증 가능하면 Spy를 쓰지 않는다.”
- Spy는 **마지막 수단**에 가깝고, 사용 시 “왜 Spy가 필요한지”가 설명 가능해야 한다.

---

## 7. Dummy / 기타

- **Dummy**
  - 사용되지 않는 파라미터를 채우기 위한 자리 채우기 객체.
  - 우리 프로젝트에서는 가능하면 설계/테스트를 단순화해서 Dummy 사용 자체를 줄이는 것을 기본으로 한다.

- **기타**
  - Proxy, Stub 서버 등 다른 형태의 Test Double은 상황에 따라 도입할 수 있으나,
  - 이 문서의 기본 원칙(실제 → Fake/Stub → Mock, Spy 최소)을 우선 적용한다.

