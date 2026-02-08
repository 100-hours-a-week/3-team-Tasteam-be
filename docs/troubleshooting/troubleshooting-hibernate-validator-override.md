# Controller Docs Interface 구현 시 @Valid 제약 조건 충돌

## 에러

```
HV000151: A method overriding another method must not redefine the parameter constraint configuration,
but method MainController#getMain(Long, MainPageRequest) redefines the configuration of
MainControllerDocs#getMain(Long, MainPageRequest).
```

## 원인

Hibernate Validator는 JSR-380 Bean Validation 스펙에 따라 **override된 메서드에서 파라미터 제약 조건을 재정의하는 것을 금지**한다.

Controller가 Docs interface를 구현할 때:
- Interface에 `@ParameterObject` 등 어노테이션이 있는 파라미터에
- Controller에서 `@Valid`를 추가로 붙이면 "제약 조건 재정의"로 간주되어 에러 발생

```java
// Docs interface
SuccessResponse<MainPageResponse> getMain(
    @ParameterObject MainPageRequest request);

// Controller - 에러 발생!
public SuccessResponse<MainPageResponse> getMain(
    @Valid @ModelAttribute MainPageRequest request) { ... }
```

## 해결

`@Valid`는 Docs interface에만 선언하고, Controller에서는 제거한다.

```java
// Docs interface
SuccessResponse<MainPageResponse> getMain(
    @Valid @ParameterObject MainPageRequest request);

// Controller - @Valid 제거
@Override
public SuccessResponse<MainPageResponse> getMain(
    @ModelAttribute MainPageRequest request) { ... }
```

Validation은 interface의 `@Valid`가 상속되어 정상 동작한다.
