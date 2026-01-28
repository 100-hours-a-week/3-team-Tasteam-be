# 테스트 코드 작성 공통 컨벤션

테스트는 단순 검증이 아니라 **명세(Specification)**다. 아래 원칙을 따른다.

- 테스트만 읽어도 기능 요구사항이 이해되어야 한다.
- 실패 메시지는 디버깅 로그가 아니라 **원인 설명 문서**여야 한다.
- 테스트는 구현이 아니라 **행위(Behavior)**를 검증한다.

---

## 1. 목표

- 테스트는 요구사항을 드러내는 살아 있는 문서다.
- 행위와 기대 결과가 선명하게 드러나는지, 실패 시 원인이 설명되는지 항상 점검한다.

---

## 2. 테스트 구조 원칙 (Given–When–Then)

모든 테스트는 논리적으로 **Given – When – Then** 구조를 따른다.

```java
@Test
@DisplayName("포인트가 부족하면 결제에 실패한다")
void failPaymentWhenUserHasInsufficientPoints() {
    // given
    User user = userWithPoints(100);
    Order order = orderPrice(500);

    // when
    PaymentResult result = paymentService.pay(user, order);

    // then
    assertThat(result.isSuccess()).isFalse();
}
```

| 구분  | 규칙                                   |
| ----- | -------------------------------------- |
| given | 테스트 전제 조건 및 객체 준비          |
| when  | 검증 대상 메서드 호출은 **한 번만**    |
| then  | 검증 로직만 위치 (검증 외 로직 금지)   |

---

## 3. 테스트 메서드 명명 규칙

- 기본 형식: **행위 기반 서술형 + camelCase**
- 금지: `should_행위_when_상황`, `행위_상황`, `test1`, `successCase`, `doTest`, `checkLogic`, 구현 세부사항 노출(`callRepository`, `useHashMap` 등).
- ✅ 권장 형식: `methodUnderTest_condition_expectedResult`
- ✅ 또는 한국어 서술형: `동작_조건_결과`

예시

- `void pay_fails_when_point_is_insufficient()`
- `void withdraw_reduces_balance_when_account_is_valid()`
- `void 포인트가_부족하면_결제에_실패한다()`

---

## 4. @DisplayName 규칙 (가장 중요)

@DisplayName은 **비개발자도 이해 가능한 요구사항 문장**이어야 한다.

| 항목     | 규칙                              |
| -------- | --------------------------------- |
| 문장 형태 | 평서문                            |
| 시제     | 현재형                            |
| 주어     | 생략 가능                         |
| 용어     | 구현 용어 금지                    |
| 상세도   | 예외 상황일수록 구체적으로 작성   |

좋은 예

- `@DisplayName("이미 탈퇴한 회원은 로그인할 수 없다")`
- `@DisplayName("재고가 0이면 주문이 실패한다")`
- `@DisplayName("관리자는 모든 사용자의 정보를 조회할 수 있다")`

나쁜 예

- `@DisplayName("login test")`
- `@DisplayName("exception case")`
- `@DisplayName("when status = 400")`

---

## 5. 하나의 테스트가 검증해야 하는 것

**테스트 1개 = 개념 1개**.

- 허용: 성공 케이스 1개, 예외 1종, 상태 변화 1개.
- 금지: 여러 성공 시나리오 혼합, 여러 예외 혼합, 여러 필드 동시 검증.

---

## 6. @Nested 사용 규칙

기능 단위 그룹핑을 위해 사용한다.

```java
@Nested
@DisplayName("회원 가입")
class SignUp {

    @Test
    @DisplayName("이메일 형식이 아니면 가입에 실패한다")
    void failWhenEmailIsInvalid() {}

    @Test
    @DisplayName("중복 이메일이면 가입할 수 없다")
    void failWhenEmailAlreadyExists() {}
}
```

규칙

- 도메인 **기능 단위**로만 중첩.
- **2 depth 초과 금지**.

---

## 7. 테스트 데이터 작성 규칙

- 직관적이지 않은 **생성자 직접 호출 금지**  
  - `User user = new User("abc123", "pw", 37, true);` → 의미 불명.
- ✅ 의미 있는 이름의 **팩토리 메서드 / Fixture Builder 필수**  
  - `User user = normalUser();`  
  - `User admin = adminUser();`  
  - `Order order = orderPrice(10000);`

---

## 8. Assertion 규칙

- 기본: **AssertJ `assertThat`** 사용.
- `boolean` 직접 비교 금지 (`assertTrue/False` 지양).

예시

```java
assertThat(result).isEqualTo(SUCCESS);
assertThat(user.getPoint()).isZero();
assertThatThrownBy(() -> service.pay(user, order))
        .isInstanceOf(InsufficientPointException.class);
```

---

## 9. Mock 사용 규칙

| 상황             | Mock 사용 여부                     |
| ---------------- | ---------------------------------- |
| 외부 API         | 사용                               |
| DB               | 사용 **금지** (통합 테스트 별도)   |
| 같은 도메인 서비스 | 가능하면 실제 객체 사용           |

- Mock은 **협력 객체 검증용**이지 로직 대체용이 아니다.

---

## 10. 테스트 금지 사항

- 랜덤값 사용 금지.
- 시간 의존 테스트 금지 (`LocalDateTime.now()` 직접 사용 X).
- 로그 출력 테스트 금지.
- private 메서드 직접 테스트 금지.
- 테스트 간 상태 공유 금지.

---

## 11. 커밋 기준 (PR 차단 조건)

아래 중 하나라도 어기면 PR 반려 가능:

- DisplayName이 요구사항 문장이 아님.
- 테스트 이름이 의미를 설명하지 못함.
- Given/When/Then 구조가 아님.
- 테스트가 2개 이상 개념을 검증함.
