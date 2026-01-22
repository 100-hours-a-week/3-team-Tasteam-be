## OAuth2 인증 요청 쿠키 역직렬화 실패

- 해결 일자 : 26.01.22
- 관련 커밋 : ?

### 문제
`oauth2_auth_request` 쿠키를 Jackson으로 역직렬화할 때 `Serialization failed` (또는 `Deserialization failed`) 예외가 발생했습니다. Spring Security의 `OAuth2AuthorizationRequest`는 Java `Serializable`을 구현한 객체이므로 Jackson 대신 Java 직렬화/역직렬화를 사용해야 합니다.

### 적용된 변경
`SerializationUtil`에서 Jackson `ObjectMapper`를 제거하고 `SerializationUtils.serialize()` / `SerializationUtils.deserialize()` (Java 직렬화)로 교체하여 쿠키에 저장된 바이트 배열을 안정적으로 복원하게 했습니다.

### 주의
기존 방식(Jackson 직렬화)으로 저장된 쿠키는 더 이상 읽을 수 없기 때문에 브라우저의 `oauth2_auth_request` 또는 `redirect_uri` 쿠키를 삭제하고 다시 시도해야 합니다.

### 참고
- `app-api/src/main/java/com/tasteam/global/utils/SerializationUtil.java`
  - `SerializationUtils` 기반 직렬화/역직렬화 구현

