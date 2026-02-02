## 병렬 테스트에서 Mockito Bean 충돌

### 상황
`@ControllerWebMvcTest` 기반의 컨트롤러 테스트(`RestaurantControllerTest`)가 하나의 `@MockitoBean ReviewService`를 공유하고 있으며, Gradle이 테스트를 CPU 코어 수만큼 병렬로 돌리도록 설정되어 있습니다. 이 상태에서 서로 다른 테스트가 `ReviewService`의 서로 다른 메서드를 모킹하면, Mockito 내부에서 어떤 스텁이 어떤 메서드에 대한 것인지 혼란이 생기고 `WrongTypeOfReturnValue` 예외가 발생합니다.

```
ReviewCreateResponse cannot be returned by getRestaurantReviews()
getRestaurantReviews() should return CursorPageResponse
```

### 발생 원인
병렬 실행 중 두 테스트가 동시에 같은 Mockito 빈을 조작함.
- `음식점 리뷰 목록 조회` 테스트: `reviewService.getRestaurantReviews(...)`을 `CursorPageResponse<ReviewResponse>`로 위임.
- `리뷰 작성` 테스트: 같은 `reviewService`에 `createReview(...)`을 `ReviewCreateResponse`로 stub.

Mockito는 같은 Mockito 객체에서 타입과 메서드를 구분하지 못하고 마지막 설정을 적용하기 때문에 `getRestaurantReviews()`가 `ReviewCreateResponse`를 반환한다는 오류가 납니다. 이 오류는 Mockito가 `getRestaurantReviews`에 대한 stub의 리턴 타입과 실제 타입이 다르다고 판단했기 때문에 발생합니다.

### 해결 전략
1. **병렬 실행을 끄거나 최소화**  
   - Gradle `maxParallelForks`를 1로 조정하거나, 테스트 환경에서 `org.junit.jupiter.execution.parallel.enabled=false`를 설정하여 `@MockitoBean` 공유를 피합니다.
2. **Mockito 빈을 테스트마다 분리**  
   - `@Nested` 대신 테스트 클래스를 분리하고, 각 클래스에서 `@MockBean ReviewService`를 선언하거나 `@DirtiesContext`를 붙여 각각의 컨텍스트를 별도로 로딩하게 합니다.
   - 또는 병렬 테스트에 적합한 `@TestConfiguration` 레이어를 만들어 테스트 전용 mock을 개별적으로 주입합니다.

### 권장
- 동일한 Mockito 빈을 여러 테스트가 공유하는 경우, 병렬 실행을 강하게 제한하든가 `@DirtiesContext`/별도 테스트 클래스로 각 테스트가 독립된 컨텍스트를 갖도록 해서 stub 충돌을 예방하세요.
