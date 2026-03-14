# LettuceConnectionFactory is STOPPING

## 증상

- 애플리케이션 종료 또는 테스트 컨텍스트 종료 시 아래 로그 출력:
  ```
  java.lang.IllegalStateException: LettuceConnectionFactory is STOPPING
  ```
- 스택 트레이스에 `t-stream-*` 또는 `chat-stream-*` 스레드명이 포함됨.
- 오류 발생 후 정상 종료됨 (프로세스 중단 없음).

## 원인

### 연쇄 구조

```
ApplicationContext 종료 시작
  └─ LettuceConnectionFactory.destroy()  ← STOPPING 상태 전환
       └─ StreamMessageListenerContainer 내 poll 스레드(chat-stream-*) 아직 실행 중
            └─ poll 루프에서 Redis 연결 요청
                 └─ IllegalStateException: LettuceConnectionFactory is STOPPING
```

### 근본 원인: `chatStreamListenerContainer` 빈의 `destroyMethod` 누락

Spring은 `destroyMethod`가 명시된 빈의 경우 **의존 역순**으로 종료를 보장한다.
`chatStreamListenerContainer`가 `RedisConnectionFactory`에 의존하고 있으므로,
`destroyMethod = "stop"`을 명시하면 container가 factory보다 먼저 종료된다.

그러나 `chatStreamListenerContainer`에는 해당 설정이 없었다:

```java
// 수정 전 — destroyMethod 누락
@Bean
public StreamMessageListenerContainer<String, MapRecord<String, String, String>> chatStreamListenerContainer(...) {
```

반면 동일 프로젝트의 `messageQueueStreamListenerContainer`는 이미 올바르게 설정되어 있었다:

```java
// MessageQueueStreamContainerConfig — 올바른 설정
@Bean(destroyMethod = "stop")
public StreamMessageListenerContainer<String, MapRecord<String, String, String>> messageQueueStreamListenerContainer(...) {
```

`ChatStreamSubscriber`에 `@PreDestroy stop()`이 존재하지만, `@PreDestroy`와
`LettuceConnectionFactory.destroy()` 간 실행 순서는 보장되지 않는다.

## 조치 (2026-03-14)

### `chatStreamListenerContainer`에 `destroyMethod = "stop"` 추가

```java
// 수정 후
@Bean(destroyMethod = "stop")
public StreamMessageListenerContainer<String, MapRecord<String, String, String>> chatStreamListenerContainer(
    RedisConnectionFactory connectionFactory,
    ThreadPoolTaskExecutor chatStreamExecutor) {
```

Spring이 빈 종료 시 의존 역순을 보장하므로:
1. `chatStreamListenerContainer.stop()` 호출 → poll 스레드 종료
2. `LettuceConnectionFactory.destroy()` 호출

`ChatStreamSubscriber.stop()`의 `@PreDestroy`는 유지한다.
`DefaultStreamMessageListenerContainer.stop()`은 멱등적이므로 이중 호출은 무해하다.

## 적용 파일

- `app-api/src/main/java/com/tasteam/domain/chat/stream/ChatStreamConfig.java`

## 재발 방지 체크리스트

- `StreamMessageListenerContainer`를 `@Bean`으로 등록할 때는 항상 `@Bean(destroyMethod = "stop")`을 명시한다.
- `RedisConnectionFactory`에 의존하는 빈이 내부에 별도 스레드를 운용하는 경우, `destroyMethod` 또는 `SmartLifecycle`로 종료 순서를 명시적으로 제어한다.
- 동일 패턴의 다른 컨테이너 빈(`messageQueueStreamListenerContainer`)과 설정을 일치시킨다.
