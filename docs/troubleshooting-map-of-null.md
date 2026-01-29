# RestaurantQueryRepositoryImpl - Map.of() NullPointerException

## 에러

```
java.lang.NullPointerException
    at java.base/java.util.ImmutableCollections$MapN.<init>(ImmutableCollections.java:1193)
    at java.base/java.util.Map.of(Map.java:1518)
    at com.tasteam.domain.restaurant.repository.impl.RestaurantQueryRepositoryImpl
        .findRestaurantsWithDistance(RestaurantQueryRepositoryImpl.java:77)
```

음식점 목록 조회 API 호출 시 NPE 발생.

## 원인

`Map.of()`는 null key/value를 허용하지 않는다.
`cursor`가 null인 경우(첫 페이지 조회) `cursorDistance`와 `cursorId`에 null이 전달되면서 예외 발생.

```java
// before - Map.of()는 null 불가
Map.of(
    "cursorDistance", cursor == null ? null : cursor.distanceMeter(), // ← NPE
    "cursorId", cursor == null ? null : cursor.id()                  // ← NPE
);
```

## 해결

`Map.of()` 대신 null 값을 허용하는 `HashMap`으로 변경.

```java
Map<String, Object> params = new HashMap<>();
params.put("latitude", latitude);
params.put("longitude", longitude);
params.put("radiusMeter", radiusMeter);
params.put("categories", categories);
params.put("cursorDistance", cursor == null ? null : cursor.distanceMeter());
params.put("cursorId", cursor == null ? null : cursor.id());
params.put("pageSize", pageSize);
```

동일 클래스 내 두 개의 오버로드된 `findRestaurantsWithDistance` 메서드 모두 동일하게 수정.
