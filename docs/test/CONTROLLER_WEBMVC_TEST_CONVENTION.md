# Controller WebMvc 테스트 컨벤션

- `@ControllerWebMvcTest`는 **지정한 컨트롤러 + 최소 보안 설정만 올린 WebMvc 슬라이스 테스트 환경**이다.
- 이 테스트는 **요청 매핑, 바인딩, Validation, 예외 처리, 응답 포맷**을 검증하는 **HTTP 계약 테스트**이자,
  각 엔드포인트의 **최종 HTTP 계약을 문서화하는 테스트**입니다.

1) “Spring MVC 레이어(요청 매핑, 바인딩, 검증, 예외 처리)”에 집중하고,  
2) **각 엔드포인트의 최종 HTTP 계약(Request/Response)을 문서화하는 테스트** 역할을 하도록 하는 것이 목적입니다.


> 대상: `@ControllerWebMvcTest` 를 사용하는 컨트롤러 슬라이스 테스트  
> 예: `PostControllerTest`, `CommentControllerTest`, `MemberControllerTest`, `AuthControllerTest`, `ImageControllerTest` 등

---

## 0. @ControllerWebMvcTest 메타 어노테이션

우리 프로젝트의 컨트롤러 WebMvc 테스트는 모두 `@ControllerWebMvcTest`를 사용합니다.

`src/test/java/com/devon/techblog/config/annotation/ControllerWebMvcTest.java` 정의:

- `@WebMvcTest`
  - 지정한 컨트롤러만 로드하는 **WebMvc 슬라이스 테스트**입니다.
  - Service/Repository/외부 클라이언트 등은 기본적으로 빈으로 올리지 않습니다.
- `@AutoConfigureMockMvc(addFilters = false)`
  - Spring Security 필터 체인을 비활성화한 상태에서 `MockMvc`를 자동 구성합니다.
  - 컨트롤러 테스트에서 **보안 필터로 인한 잡음 없이** 요청/응답/검증 흐름에 집중할 수 있습니다.
- `@Import(TestSecurityConfig.class)`
  - 테스트용 보안 설정을 주입합니다.
  - 예: 테스트에서 사용할 `TestCurrentUserContext`, 인증 관련 Fake/Stub 빈 등을 구성해,  
    실제 Security 전체 설정 없이도 “현재 사용자” 컨텍스트를 흉내낼 수 있습니다.
- `@AliasFor(WebMvcTest.value/controllers)`
  - `@ControllerWebMvcTest(SomeController.class)` 형태로 대상 컨트롤러를 지정할 수 있도록,  
    `@WebMvcTest`의 `value`/`controllers` 속성을 그대로 위임합니다.

정리하면, `@ControllerWebMvcTest`는 **“지정한 컨트롤러 + 최소한의 보안 테스트 설정”만 올린 WebMvc 슬라이스 테스트 환경**을 한 번에 구성해 주는 메타 어노테이션입니다.

---

## 1. Controller WebMvc 테스트의 역할

`@ControllerWebMvcTest` 기반 테스트는 다음을 검증합니다.

- **요청 매핑/경로/HTTP 메서드**
  - `/api/v1/posts`, `/api/v1/comments/{id}` 등 URL과 `GET/POST/PATCH/DELETE` 매핑이 기대대로 동작하는지.
  - PathVariable, RequestParam, RequestBody가 올바르게 바인딩되는지 (예: `page`, `size`, `commentId` 등).
- **요청 본문/검증(Validation)**
  - 잘못된 입력 시 `400 Bad Request`가 발생하는지.
  - `@Valid`/검증 어노테이션이 붙은 DTO가 Validation 실패 시 적절한 에러 응답을 반환하는지  
    (예: `content` 누락 시 `validation_failed` 메시지).
- **응답 상태 코드와 응답 포맷**
  - 성공 시 적절한 HTTP 상태 코드(200/201/204 등)를 반환하는지.
  - 프로젝트에서 정의한 래퍼(`ApiResponse`) 구조를 충실히 따르는지:  
    `success`, `message`, `data` 필드 등이 예상대로 채워지는지.
- **예외 처리/에러 응답 매핑**
  - 서비스/도메인에서 발생한 `CustomException`이 올바른 상태 코드와 메시지로 매핑되는지  
    (예: `POST_NOT_FOUND` → 404, `NO_PERMISSION` → 403 등).
- **(필요 시) 현재 사용자 컨텍스트와 권한 흐름의 최소 검증**
  - `TestSecurityConfig` / `TestCurrentUserContext` 등을 활용해,  
    “로그인한 사용자” 가정하에 성공/권한 실패 흐름을 일부 검증할 수 있습니다.

요약하면, Controller WebMvc 테스트는 **“HTTP 레이어에서의 계약(Request/Response/Validation/예외 매핑)”**을 검증하고,  
각 엔드포인트의 **최종 HTTP 계약을 문서화하는 레이어**입니다.

---

## 2. Controller WebMvc 테스트에서 “하지 않는” 것들

반대로, `@ControllerWebMvcTest`로는 다음을 검증하지 않습니다.

- **서비스/도메인 비즈니스 로직 자체**
  - 좋아요 가능 여부, 소유권 검증, 엔티티 상태 전이 등은 Service/Policy 테스트에서 검증합니다.
  - 컨트롤러 테스트에서는 Service를 보통 `@MockBean` 또는 테스트 전용 Stub Bean으로 주입해  
    “서비스가 이런 응답/예외를 던졌을 때 컨트롤러가 어떻게 매핑하는지”만 봅니다.
- **JPA 매핑/쿼리 동작**
  - Repository, JPA 매핑, QueryDSL 쿼리의 정확성은 `@RepositoryJpaTest`와 통합 테스트의 책임입니다.
- **보안/필터/전체 스프링 컨텍스트**
  - 실제 Security filter chain, JWT 검증, CORS, Logging Filter 등 전체 인프라를 올리지 않습니다.
  - 이런 부분은 `@IntegrationTest` + `MockMvc` 기반 통합 테스트에서 검증합니다.

이 경계를 지키면, 컨트롤러 테스트가 **“HTTP 계약 테스트”** 역할에 집중할 수 있고, 구현 변경에도 덜 깨지게 됩니다.

---

## 3. 테스트 작성 패턴 (예시 기반 규칙)

### 3.1 성공 시나리오: 정상 요청/응답 플로우

예: `PostControllerTest`, `CommentControllerTest`의 “생성/수정/조회/삭제 성공” 테스트들

- **목적**
  - 올바른 요청을 보냈을 때, 컨트롤러가 Service를 호출하고 기대하는 응답 포맷/상태 코드를 반환하는지 검증.
- **패턴**
  - Given:
    - Service 빈을 Mock/Stub하여, 특정 요청에 대해 미리 정해둔 응답 DTO를 반환하게 설정.
    - Request DTO도 Fixture를 사용해 의도를 드러냄.
  - When:
    - `mockMvc.perform(...)`로 HTTP 요청을 구성 (`post`, `get`, `patch`, `delete` 등).
  - Then:
    - `status().isOk()/isCreated()/isNoContent()` 등 HTTP 상태 코드 검증.
    - `jsonPath("$.success").value(true)` 와 같이 공통 래퍼 구조 검증.
    - `jsonPath("$.data.xxx")`로 핵심 필드가 올바르게 내려오는지 확인.
- **규칙**
  - 응답 구조/상태 코드/핵심 필드까지는 꼼꼼히 검증하되,
  - 서비스 내부 구현(몇 번 호출되는지, 어떤 순서인지 등)은 검증하지 않는다.

### 3.2 실패/검증/권한 시나리오

예:  
- 잘못된 입력 → `400 Bad Request` (`validation_failed`)  
- 존재하지 않는 리소스 → `404 Not Found`  
- 권한 없음 → `403 Forbidden`  

- **목적**
  - Validation/예외/권한 에러가 올바른 HTTP 상태 코드와 에러 응답 구조로 노출되는지 확인.
- **패턴**
  - Validation:
    - 필수 필드 누락/형식 오류 등의 Request DTO를 만들고 요청.
    - `status().isBadRequest()`, 에러 코드/메시지 필드 검증.
  - CustomException:
    - Service Mock이 특정 `CustomException`을 던지도록 설정.
    - 기대하는 상태 코드/`success=false`/에러 메시지 검증.
- **규칙**
  - “어떤 예외가 어떻게 HTTP 응답으로 변환되는가”에 초점을 맞추고,
  - 예외가 발생하는 **도메인 조건** 자체는 Service/Policy 테스트에 맡긴다.

### 3.3 현재 사용자/보안 흐름 최소 검증

- `TestSecurityConfig`, `TestCurrentUserContext`를 통해:
  - “로그인된 사용자 ID”를 주입하고, 해당 사용자의 요청이 성공/실패하는지 정도만 확인할 수 있습니다.
- 자세한 인증/인가 로직(JWT 파싱, 권한 매트릭스 등)은 통합 테스트/보안 전용 테스트의 책임입니다.

---

## 4. 구현 세부에 과하게 결합되지 않기 위한 가이드

- **컨트롤러 내부 구현보다는 HTTP 계약에 집중**
  - 컨트롤러 안에서 Service를 몇 번 호출하는지, 어떤 순서로 호출하는지는 검증하지 않는다.
  - 대신 “이 입력 → 이 HTTP 상태 코드 + 이 응답 바디”를 계약으로 본다.
- **`@WebMvcTest` 환경에서 올리지 않을 것**
  - 불필요한 Bean (`@Configuration`, 복잡한 Security 설정 등)을 임의로 Import 하지 않는다.
  - 필요하면 테스트 전용 설정/Stub만 최소한으로 Import 한다.
- **리팩토링 내성**
  - 컨트롤러 내부 코드 구조가 바뀌어도, 같은 URL/요청/응답 계약이 유지되면 테스트가 그대로 통과하도록 작성한다.

---

## 5. 테스트 픽스처(Request/Response) 사용 컨벤션

- **Request/도메인 객체**
  - `CommentRequestFixture`, `PostFixture` 와 같이 **입력/도메인 쪽은 적극적으로 Fixture로 추출**한다.
  - 이유: 여러 레이어/테스트에서 반복 생성되며, 기본값과 차이점을 명확히 표현할 수 있기 때문이다.
- **Response/HTTP 응답**
  - 기본 원칙:
    - WebMvc 테스트에서는 **핵심 필드를 `jsonPath`로 직접 검증**해, HTTP 계약을 테스트 본문에서 바로 읽을 수 있게 한다.
    - 예: `jsonPath("$.data.commentId").value(1L)`, `jsonPath("$.data.content").value("댓글내용")`.
  - 예외적으로:
    - 여러 테스트에서 반복되는 **크고 복잡한 응답 구조**(예: 공통 Page 응답, 중첩된 DTO)가 있다면,
      Response용 Fixture/헬퍼(`SomeResponseFixture.created()`, `toJson()`)로 추출해도 좋다.
  - 주의:
    - Response Fixture를 도입하더라도, 테스트를 읽는 사람이 “이 엔드포인트가 어떤 JSON을 반환하는지”를 쉽게 이해할 수 있도록,
      핵심 필드는 여전히 `jsonPath` 검증으로 드러내는 것을 권장한다.

---

## 6. 언제 @ControllerWebMvcTest를 쓸까?

- DTO 바인딩/검증/예외 처리/응답 포맷 등 **HTTP 레이어의 동작**을 실제 Spring MVC 환경과 유사하게 검증하고 싶을 때.
- 전체 컨텍스트를 올리는 `@IntegrationTest`까지 가지 않고도,  
  컨트롤러와 인프라(MVC, Validation, ExceptionHandler)를 함께 확인하고 싶을 때.
- 특정 엔드포인트의 Request/Response 계약을 문서화하는 테스트가 필요할 때.

반대로, 다음과 같은 경우에는 다른 테스트 방식을 고려합니다.

- 복잡한 비즈니스 분기 로직만 빠르게 검증하고 싶다 → 순수 단위 테스트(Standalone MockMvc).
- Security/Filter/DB/외부 시스템까지 포함한 end-to-end 플로우를 검증하고 싶다 → `@IntegrationTest` + `MockMvc`.

