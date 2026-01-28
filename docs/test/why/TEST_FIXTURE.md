# Test Fixture 작성 가이드

이 문서는 프로젝트 전반에서 Test Fixture를 언제, 어떻게 사용할지에 대한 최소한의 규칙을 정리한 가이드입니다.  
각 프로젝트의 도메인 구조와 패키지 구성은 달라질 수 있으므로, 여기서는 범용적으로 적용할 수 있는 원칙만을 다룹니다.

## 1. 언제 Fixture를 쓰는지

다음 중 하나라도 해당하면 Fixture 사용을 적극적으로 고려합니다.

- 같은 엔티티나 DTO를 두 번 이상 만들어야 할 때
- 필드가 많은 엔티티라서 Given 절이 길어질 때
- 이후에도 비슷한 테스트를 계속 추가할 가능성이 높을 때

반대로 다음과 같은 경우에는 굳이 Fixture를 만들지 않습니다.

- 특정 테스트 하나에서만 사용되는 값일 때
- 도메인 규칙과 무관한, 일회성 설명용 임시 데이터일 때

실무에서는 주로 “엔티티”, “요청 DTO”, 그리고 반복해서 생성해야 하는 일부 “응답/조회 DTO”에 대해 Fixture를 두는 정도로도 충분합니다.

## 2. java-test-fixtures 설정 방법과 선택 이유

여러 모듈에서 공통으로 사용하는 테스트 코드를 관리할 때는 Gradle의 `java-test-fixtures` 플러그인을 사용하는 것을 기본 전략으로 삼습니다.

### 2.1 Gradle 설정 예시

멀티 모듈 환경에서 공용 도메인 모듈을 하나 두고, 그 모듈의 픽스처를 다른 모듈 테스트에서 재사용하는 상황을 예로 들면 다음과 같습니다.

```groovy
// 공용 도메인 모듈
plugins {
    id 'java'
    id 'java-test-fixtures'
}

// 다른 모듈에서 공용 픽스처 의존
dependencies {
    testImplementation(testFixtures(project(":domain")))
}
```

테스트 전용 의존성이 필요하다면 `testFixturesImplementation`, `testFixturesRuntimeOnly` 등을 사용하여 픽스처 소스셋에만 의존성을 추가한 뒤,  
상위 모듈에서는 `testImplementation(testFixtures(project(":domain")))` 또는 `testRuntimeOnly`로 가져오면 됩니다.

### 2.2 java-test-fixtures를 쓰는 이유

- **테스트 전용 코드 분리**  
  - 픽스처, 테스트 전용 헬퍼, Fake/Stub 구현체를 main 코드와 분리해 관리할 수 있습니다.
  - 생성된 test-fixtures JAR는 프로덕션 아티팩트에 포함되지 않고, 테스트에서만 사용됩니다.
- **멀티 모듈 간 재사용**  
  - 한 번 정의한 픽스처를 여러 모듈의 테스트에서 재사용할 수 있어 중복을 줄일 수 있습니다.
  - “어느 모듈이 어떤 테스트 자원을 제공하는지”를 Gradle 의존성으로 명시적으로 표현할 수 있습니다.
- **테스트 전용 의존성 전파 구조화**  
  - H2, Testcontainers, WireMock 같은 테스트 전용 의존성을 픽스처 모듈에만 선언한 뒤,
    이를 사용하는 상위 모듈에서는 픽스처에만 의존하도록 구성할 수 있습니다.

요약하면, `java-test-fixtures`는 테스트 코드를 별도의 소스셋으로 관리하면서,  
여러 모듈에서 픽스처와 테스트 전용 의존성을 재사용하기 위한 표준적인 방법입니다.

## 3. Fixture 클래스와 메서드 규칙

여기서는 하나의 예시 도메인(`Order`)을 기준으로 규칙을 설명합니다. 실제 프로젝트에서는 도메인 이름만 바꾸어 동일한 패턴으로 적용하면 됩니다.

### 3.1 클래스 규칙

- 클래스 이름은 **대상 + Fixture** 형태로 작성합니다.  
  - 예: `OrderFixture`, `OrderRequestFixture`, `OrderQueryDtoFixture`
- 클래스는 `final`로 두고, 기본 생성자는 `private`으로 막습니다.
- 인스턴스 상태를 가지지 않고, 모두 `static` 메서드만 제공합니다.

### 3.2 기본값 규칙

- 자주 사용하는 값은 `DEFAULT_` 접두어를 붙인 상수로 둡니다.
  - 예: `OrderFixture.DEFAULT_ORDER_ID`, `DEFAULT_TOTAL_AMOUNT`, `DEFAULT_USER_ID`
- 업데이트 시나리오가 잦다면 `UPDATED_` 접두어 상수를 추가로 둡니다.
  - 예: `OrderFixture.UPDATED_TOTAL_AMOUNT`

### 3.3 메서드 규칙

- **엔티티용 Fixture**
  - `create()`  
    - 유효한 기본값을 사용해 새 엔티티를 생성합니다.  
    - 예: `OrderFixture.create()`는 저장되지 않은 주문 엔티티를 생성합니다.
  - `createWithId()`  
    - 테스트 편의를 위해 식별자까지 채워진 엔티티를 생성합니다.  
    - 내부에서 Reflection 등을 사용하더라도, 테스트 본문에서는 이를 의식하지 않게 숨깁니다.
- **요청 DTO용 Fixture**
  - `createRequest()`  
    - 정상 시나리오에 사용되는 기본 요청 DTO를 생성합니다.  
    - 예: `OrderRequestFixture.createRequest()`
  - `updateRequest()`  
    - 수정 시나리오에 사용하는 요청 DTO를 생성합니다.  
    - 예: `OrderRequestFixture.updateRequest()`
  - 실패/경계값은 메서드 이름으로 의도를 드러냅니다.  
    - 예: `OrderRequestFixture.createRequestWithoutItems()`, `createRequestWithTooManyItems()`
- **응답/조회 DTO용 Fixture**
  - 여러 테스트에서 반복해서 사용하는 조회 결과가 있을 때만 별도 Fixture를 둡니다.
  - `create()`는 기본값으로, `createWithAllFields(...)`는 모든 필드를 받아 덮어쓰는 형태로 구성합니다.

테스트 코드에서는 가능하면 생성자를 직접 호출하기보다 Fixture 메서드를 사용합니다.  
Reflection 관련 유틸리티는 Fixture 내부에만 숨기고, 테스트 본문에서는 드러나지 않도록 유지합니다.

## 4. 어디에서 어떻게 쓰는지

Fixture는 테스트 종류에 따라 다음과 같이 사용합니다.

- **도메인 테스트**  
  - 순수 도메인 규칙을 검증할 때는 `OrderFixture`와 같은 도메인 Fixture를 사용합니다.
  - 저장 여부가 중요하지 않다면 `create()` 계열만 사용하고, 저장된 상태를 가정해야 할 때만 `createWithId()`를 사용합니다.
- **서비스·리포지토리 테스트**  
  - 서비스 테스트에서는 도메인 Fixture와 DTO Fixture를 함께 사용하여 시나리오를 구성합니다.
  - 리포지토리나 조회 전용 테스트에서는 `OrderQueryDtoFixture`와 같은 조회용 Fixture를 사용해 결과 형태를 맞춥니다.
- **컨트롤러·통합 테스트**  
  - 컨트롤러 테스트에서는 요청 DTO Fixture를 우선으로 사용합니다.  
    예: `OrderRequestFixture.createRequest()`  
  - 같은 시나리오를 서비스 테스트와 컨트롤러 테스트에서 함께 검증하고 싶다면,  
    두 테스트가 동일한 Fixture 메서드를 사용하도록 맞추어 일관성을 유지합니다.

새 도메인이나 API를 추가할 때는 대략 다음 순서를 추천합니다.

1. 도메인 엔티티를 정의합니다.
2. 해당 엔티티용 Fixture(`OrderFixture`)를 함께 만듭니다.
3. 요청 DTO를 정의합니다.
4. 요청 DTO Fixture(`OrderRequestFixture`)를 만듭니다.
5. 위 Fixture들을 사용하여 도메인/서비스/컨트롤러 테스트를 작성합니다.

## 5. 왜 이렇게 쓰는지

Fixture를 사용하는 목적은 크게 다음과 같습니다.

- **Given 절을 짧고 명확하게 만들기 위해**  
  - 테스트 본문이 준비 코드로 가득 차는 것을 막고, 검증 로직에 집중할 수 있게 합니다.
- **도메인·DTO 변경에 덜 깨지는 테스트를 만들기 위해**  
  - 필드가 추가되거나 이름이 바뀌어도 Fixture 한 곳만 수정하면 대부분의 테스트가 함께 정리됩니다.
- **계층 간에 같은 데이터를 공유하기 위해**  
  - 서비스, 컨트롤러, 통합 테스트가 서로 다른 값으로 같은 시나리오를 검증하는 상황을 줄입니다.
- **테스트에서 자주 쓰는 값에 의미 있는 이름을 붙이기 위해**  
  - `DEFAULT_TOTAL_AMOUNT`와 같은 상수 이름이 곧 “해당 프로젝트에서 기본 주문 금액”과 같은 의미를 가지게 됩니다.

요약하면, Test Fixture는 테스트 데이터를 한 곳에 모아 재사용하면서,  
테스트를 읽기 쉽고 변경에 강하게 만드는 도구로 사용합니다.  
각 프로젝트는 이 가이드를 기본으로 삼되, 도메인 특성에 맞추어 필요한 부분만 얇게 확장해서 사용하는 것을 권장합니다.


## Reference

https://toss.tech/article/how-to-manage-test-dependency-in-gradle
