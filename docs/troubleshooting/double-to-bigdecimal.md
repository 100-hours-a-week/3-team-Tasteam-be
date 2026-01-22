## BigDecimal 정밀도/스케일 트러블 슈팅

* 해결 일자 : 26.01.22
* 관련 커밋 : #20

### 문제

긍정 리뷰 비율과 같은 도메인 값에 `double` 타입을 사용하면서 `precision`, `scale`을 함께 지정해 스키마를 정의하려 했고, 이로 인해
`scale has no meaning for SQL floating point types` 오류가 발생했습니다.
`DOUBLE/FLOAT` 계열은 고정 소수점 개념이 없어 `scale`을 적용할 수 없으며, 값의 허용 범위 또한 DB 차원에서 보장되지 않는 문제가 있었습니다.

### 적용된 변경

* 비율/점수처럼 **정확한 자릿수와 범위가 중요한 값**의 타입을 `double` → `BigDecimal`로 변경
* DB 컬럼을 `NUMERIC(precision, scale)`로 정의하여 **도메인 허용 범위**를 스키마에서 강제

### 주의

* `new BigDecimal(double)` 생성자는 부동소수점 오차를 그대로 가져오므로 사용 금지
  (`BigDecimal.valueOf()` 또는 문자열 생성자 사용)
* 값 비교 시 `equals()`는 scale까지 비교하므로 사용 주의
  (의미적 값 비교는 `compareTo()` 사용)

### 참고

* DB는 계산이 아닌 **도메인 불변식(허용 범위) 보장** 역할만 담당
* 비율에서 퍼센트 변환은 애플리케이션 코드에서 처리