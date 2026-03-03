# RedisClient

Redis 연산을 위한 추상화 인터페이스입니다. RedisTemplate을 직접 사용하는 대신 이 인터페이스를 통해 Redis 작업을 수행합니다.

## 사용법

```java
@RequiredArgsConstructor
public class MyService {
    private final RedisClient redisClient;

    public void example() {
        redisClient.set("user:1", user, Duration.ofHours(1));
        Optional<User> cached = redisClient.get("user:1", User.class);
    }
}
```

## 메서드 분류

### String 연산
- `set(key, value)`: 값 저장
- `set(key, value, ttl)`: TTL과 함께 값 저장
- `setIfAbsent(key, value)`: 키가 없을 때만 저장 (분산 락 구현에 유용)
- `setIfAbsent(key, value, ttl)`: TTL과 함께 조건부 저장
- `get(key, type)`: 값 조회 (타입 안전)
- `increment(key)`: 값을 1 증가
- `increment(key, delta)`: 값을 delta만큼 증가
- `decrement(key)`: 값을 1 감소
- `decrement(key, delta)`: 값을 delta만큼 감소

### 키 관리
- `delete(key)`: 단일 키 삭제
- `deleteAll(keys)`: 여러 키 일괄 삭제
- `exists(key)`: 키 존재 여부 확인
- `expire(key, ttl)`: 키에 만료 시간 설정
- `getExpire(key)`: 남은 만료 시간 조회 (초 단위)
- `keys(pattern)`: 패턴에 매칭되는 키 검색 (프로덕션 사용 주의)

### Hash 연산
Hash는 필드-값 쌍의 컬렉션을 저장하는 데 유용합니다.

- `hSet(key, field, value)`: 해시 필드 설정
- `hSetAll(key, map)`: 여러 필드 일괄 설정
- `hGet(key, field, type)`: 해시 필드 조회
- `hGetAll(key)`: 모든 필드-값 쌍 조회
- `hDelete(key, fields...)`: 해시 필드 삭제
- `hExists(key, field)`: 해시 필드 존재 여부

```java
// 예시: 사용자 세션 정보 저장
Map<String, Object> session = Map.of(
    "userId", 123,
    "role", "ADMIN",
    "loginTime", Instant.now()
);
redisClient.hSetAll("session:token123", session);
```

### List 연산
List는 순서가 있는 요소 컬렉션입니다. 큐, 스택, 타임라인 구현에 유용합니다.

- `lPush(key, value)`: 리스트 왼쪽에 추가 (head)
- `rPush(key, value)`: 리스트 오른쪽에 추가 (tail)
- `lPop(key, type)`: 리스트 왼쪽에서 제거 및 반환
- `rPop(key, type)`: 리스트 오른쪽에서 제거 및 반환
- `lRange(key, start, end, type)`: 범위 조회 (0부터 시작, -1은 끝)
- `lSize(key)`: 리스트 크기

```java
// 예시: 최근 알림 저장 (최대 100개)
redisClient.lPush("notifications:user:123", notification);
List<Notification> recent = redisClient.lRange("notifications:user:123", 0, 99, Notification.class);
```

### Set 연산
Set은 중복 없는 요소 컬렉션입니다. 태그, 팔로워, 좋아요 구현에 유용합니다.

- `sAdd(key, values...)`: Set에 요소 추가
- `sMembers(key)`: 모든 요소 조회
- `sIsMember(key, value)`: 요소 포함 여부 확인
- `sRemove(key, values...)`: 요소 제거
- `sSize(key)`: Set 크기

```java
// 예시: 게시글 좋아요 관리
redisClient.sAdd("post:456:likes", userId);
boolean liked = redisClient.sIsMember("post:456:likes", userId);
Long likeCount = redisClient.sSize("post:456:likes");
```

## 사용 사례

### 분산 락
```java
boolean acquired = redisClient.setIfAbsent("lock:resource:123", "owner-id", Duration.ofSeconds(30));
if (acquired) {
    try {
        // 임계 영역 작업
    } finally {
        redisClient.delete("lock:resource:123");
    }
}
```

### 캐싱
```java
public User getUser(Long userId) {
    String key = "user:" + userId;
    return redisClient.get(key, User.class)
        .orElseGet(() -> {
            User user = userRepository.findById(userId).orElseThrow();
            redisClient.set(key, user, Duration.ofMinutes(30));
            return user;
        });
}
```

### 카운터
```java
Long viewCount = redisClient.increment("article:123:views");
```

### 속도 제한 (Rate Limiting)
```java
String key = "rate-limit:api:" + userId;
Long requestCount = redisClient.increment(key);
if (requestCount == 1) {
    redisClient.expire(key, Duration.ofMinutes(1));
}
if (requestCount > 100) {
    throw new TooManyRequestsException();
}
```

## 주의사항

1. **keys() 메서드**: 프로덕션 환경에서 keys() 사용 시 성능 문제 발생 가능. SCAN 명령 사용 고려.
2. **TTL 설정**: 메모리 관리를 위해 적절한 TTL 설정 권장.
3. **직렬화**: GenericJackson2JsonRedisSerializer 사용으로 객체 자동 직렬화.
4. **타입 안전성**: 제네릭 메서드로 타입 안전성 보장하지만, 잘못된 타입 캐스팅 시 ClassCastException 발생 가능.
