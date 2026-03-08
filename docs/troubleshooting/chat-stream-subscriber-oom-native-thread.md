# ChatStreamSubscriber 부팅 시 OOM: unable to create native thread

## 증상

- 애플리케이션 기동 직후 `Application run failed` 로그 출력 후 프로세스 종료.
- `BeanCreationException` → 원인: `java.lang.OutOfMemoryError: unable to create native thread`.
- `HikariPool-1:connection-closer`, SIGTERM 핸들러 등 여러 스레드 생성 실패 로그 반복.
- Redis 스트림/그룹 상태가 어긋난 경우 `ensureGroupExists`에서 예외가 추가로 발생.

## 원인

### 연쇄 구조

```
Spring 빈 초기화 단계 (@PostConstruct)
  └─ ChatStreamSubscriber.start()
       ├─ container.start()            ← 컨테이너 폴링 스레드 생성
       └─ refreshSubscriptions()       ← @PostConstruct 안에서 즉시 호출
            └─ chatRoomRepository.findActiveRoomIds()  ← DB 쿼리 (HikariPool 점유)
                 └─ N개 활성 채팅방 × registerRoomSubscription()
                      ├─ ensureGroupExists()            ← Redis 연결
                      └─ container.receive(...)         ← 구독당 스레드/연결 추가 생성
```

### 문제 1: 빈 초기화 단계에서 DB + Redis + 스레드를 동시에 점유

`@PostConstruct`는 Spring 컨테이너가 빈을 등록하는 도중에 실행된다. 이 시점에는 Tomcat, HikariPool 등 다른 인프라 빈이 완전히 준비되지 않은 상태일 수 있다. 여기서 `refreshSubscriptions()`를 호출하면:
- DB 커넥션 선점 → HikariPool 내부 스레드(`connection-closer`) 생성 시도
- Redis 연결 N개 생성
- `container.receive()` N회 호출 → 폴링/디스패치 스레드 추가 생성

채팅방 수가 많을수록 스레드 생성 요청이 폭발적으로 증가한다.

### 문제 2: 스레드/PID 자원 제약

컨테이너 환경에서 JVM 기본 옵션은 컨테이너의 cgroup 제한을 인식하지 못한다 (`-XX:+UseContainerSupport` 미설정). 스레드 스택 기본값(1MB/스레드)도 그대로여서, 채팅방이 수십 개만 되어도 가용 메모리 범위를 초과해 `unable to create native thread`가 발생한다.

### 문제 3: 단일 방 실패가 전체 구독 루프를 중단

`refreshSubscriptions()` 루프에 예외 격리가 없어서, 한 채팅방의 `ensureGroupExists` 실패(BUSYGROUP 외 예외)가 나머지 방 등록 전체를 막았다.

## 조치 (2026-03-08)

### 1. `refreshSubscriptions()` 호출 시점을 `ApplicationReadyEvent`로 이동

```java
// 변경 전
@PostConstruct
public void start() {
    container.start();
    refreshSubscriptions();  // ← 빈 초기화 도중 실행
}

// 변경 후
@PostConstruct
public void start() {
    container.start();
}

@EventListener(ApplicationReadyEvent.class)
public void onApplicationReady() {
    refreshSubscriptions();  // ← 모든 빈이 준비된 후 실행
}
```

`ApplicationReadyEvent`는 Tomcat, HikariPool, Scheduler 등 모든 인프라가 완전히 기동된 후에 발행된다. 부팅 경쟁 상태가 해소된다.

### 2. 방별 예외 격리

```java
for (Long roomId : currentRoomIds) {
    try {
        subscriptions.computeIfAbsent(roomId, this::registerRoomSubscription);
    } catch (Exception ex) {
        log.warn("채팅방 구독 등록 실패, 다음 주기에 재시도합니다. roomId={}", roomId, ex);
    }
}
```

한 방의 Redis/DB 오류가 나머지 방 등록을 막지 않는다. 실패한 방은 30초 후 `@Scheduled` 재시도로 복구된다.

### 3. `StreamMessageListenerContainer`에 bounded executor 설정

```java
.executor(Executors.newFixedThreadPool(4))
```

기본 unbounded executor 대신 4개 고정 스레드로 폴링을 처리한다. 채팅방 수에 관계없이 스레드 수가 증가하지 않는다.

### 4. Dockerfile에 JVM 플래그 추가

```dockerfile
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Xss256k", \
  "-jar", "/app/app.jar"]
```

| 플래그 | 효과 |
|--------|------|
| `-XX:+UseContainerSupport` | cgroup 메모리/CPU 제한을 JVM이 인식 |
| `-XX:MaxRAMPercentage=75.0` | 컨테이너 메모리의 75%를 힙으로 사용 |
| `-Xss256k` | 스레드 스택 1MB → 256KB로 축소, 동일 메모리에서 4배 더 많은 스레드 생성 가능 |

## 적용 파일

- `app-api/src/main/java/com/tasteam/domain/chat/stream/ChatStreamSubscriber.java`
- `app-api/src/main/java/com/tasteam/domain/chat/stream/ChatStreamConfig.java`
- `app-api/Dockerfile`

## 재발 방지 체크리스트

- `@PostConstruct`에서 DB 쿼리·외부 연결·대량 스레드 생성 금지. 인프라 의존 초기화는 `ApplicationReadyEvent` 또는 `SmartLifecycle`로 이동.
- `StreamMessageListenerContainer` 생성 시 항상 bounded executor 명시.
- 루프 내 외부 자원(DB, Redis) 접근은 반드시 방별 예외 격리 적용.
- 컨테이너 배포 시 `-XX:+UseContainerSupport` + `-Xss256k` 기본 포함.
