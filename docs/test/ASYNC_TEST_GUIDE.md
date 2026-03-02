# 비동기 컴포넌트 테스트 가이드

## 목차

1. [핵심 전제: 비동기에서 무엇을 테스트하는가](#1-핵심-전제)
2. [유형별 테스트 전략](#2-유형별-테스트-전략)
   - 2.1 [@Async + @EventListener (fire-and-forget)](#21-async--eventlistener)
   - 2.2 [@Async + @TransactionalEventListener(AFTER_COMMIT)](#22-async--transactionaleventlistenerafter_commit--핵심)
   - 2.3 [@Async + @Transactional (SearchHistoryRecorder)](#23-async--transactional)
   - 2.4 [수동 TransactionSynchronization (ReviewEventPublisher)](#24-수동-transactionsynchronization)
   - 2.5 [@TransactionalEventListener + MQ 발행 (GroupMemberJoinedMessageQueuePublisher)](#25-transactionaleventlistener--mq-발행--모범-패턴)
3. [SyncTaskExecutor 사용 가이드](#3-synctaskexecutor-사용-가이드)
4. [Awaitility — 진짜 비동기가 필요할 때](#4-awaitility--진짜-비동기가-필요할-때)
5. [안티패턴](#5-안티패턴)

---

## 1. 핵심 전제

> **비동기 실행 자체를 테스트하는 것이 아니라, 비동기가 수행해야 하는 결과/부수효과를 테스트한다.**

`@Async`가 다른 스레드에서 실행되는지는 Spring이 보장한다. 테스트에서 검증할 대상:

| 검증 대상 | 예시 | 방법 |
|-----------|------|------|
| **비즈니스 로직** | `SearchHistoryRecorder`가 중복 키워드를 카운트업하는가 | 직접 메서드 호출 (동기) |
| **이벤트 발행-구독 연결** | 리뷰 생성 후 AI 분석이 트리거되는가 | 통합 테스트 (실제 커밋 필요) |
| **에러 격리** | 웹훅 실패가 메인 플로우에 영향을 주지 않는가 | 직접 호출 + 예외 주입 |

**검증할 필요 없는 것:**
- 다른 스레드에서 실행되는지 (Spring 책임)
- `Thread.currentThread().getName()` 같은 실행 컨텍스트
- `@Async` 자체의 비동기성

---

## 2. 유형별 테스트 전략

### 프로젝트 비동기 컴포넌트 현황

| 컴포넌트 | 유형 | Executor | 테스트 현황 |
|---------|------|----------|------------|
| `SearchHistoryRecorder` | `@Async` + `@Transactional` | `searchHistoryExecutor` (ThreadPool) | ❌ 없음 |
| `NotificationEventListener` | `@Async` + `@EventListener` | `notificationExecutor` (VirtualThread) | ❌ 없음 |
| `ReviewCreatedAiAnalysisEventListener` | `@Async` + `@TransactionalEventListener(AFTER_COMMIT)` | `aiAnalysisExecutor` (ThreadPool 1-1) | 🟡 Facade만 |
| `BatchReportWebhookEventListener` | `@Async` + `@TransactionalEventListener(AFTER_COMMIT, fallback)` | `webhookExecutor` (VirtualThread) | ❌ 없음 |
| `WebhookErrorEventListener` | `@Async` + `@EventListener` | `webhookExecutor` (VirtualThread) | 🟡 Publisher만 |
| `ReviewEventPublisher` | 수동 `TransactionSynchronization` | - | ❌ 없음 |
| `GroupMemberJoinedMessageQueuePublisher` | `@TransactionalEventListener(AFTER_COMMIT, fallback)` | - | ✅ 완전 |

---

### 2.1 @Async + @EventListener

**대상:** `NotificationEventListener`, `WebhookErrorEventListener`

**전략:** 리스너를 **직접 호출**해 로직만 검증한다. 직접 호출 시 `@Async`는 무시되므로 동기로 실행된다.

```java
// NotificationEventListenerTest.java
@UnitTest
@DisplayName("NotificationEventListener")
class NotificationEventListenerTest {

    @Mock
    NotificationService notificationService;

    @InjectMocks
    NotificationEventListener listener;

    @Test
    @DisplayName("그룹 가입 이벤트 수신 시 알림이 생성된다")
    void onGroupMemberJoined_createsNotification() {
        GroupMemberJoinedEvent event = new GroupMemberJoinedEvent(1L, 2L, "스터디", Instant.now());

        listener.onGroupMemberJoined(event);  // @Async 무시됨 — 동기 실행

        verify(notificationService).createNotification(
            2L,
            NotificationType.SYSTEM,
            "그룹 가입 완료",
            "스터디 그룹에 가입되었습니다.",
            "/groups/1"
        );
    }

    @Test
    @DisplayName("알림 생성이 실패해도 예외가 전파되지 않는다")
    void onGroupMemberJoined_doesNotPropagateException() {
        GroupMemberJoinedEvent event = new GroupMemberJoinedEvent(1L, 2L, "스터디", Instant.now());
        willThrow(new RuntimeException("DB 장애")).given(notificationService)
            .createNotification(any(), any(), any(), any(), any());

        // NotificationEventListener는 try-catch로 감싸므로 예외가 전파되지 않는다
        assertThatCode(() -> listener.onGroupMemberJoined(event)).doesNotThrowAnyException();
    }
}
```

**포인트:**
- `@UnitTest` + `@InjectMocks` — Spring Context 불필요
- `@Async` 없이 직접 호출하므로 별도 설정 없음
- 에러 격리(fire-and-forget 특성) 검증은 `assertThatCode(...).doesNotThrowAnyException()` 사용

---

### 2.2 @Async + @TransactionalEventListener(AFTER_COMMIT) ← 핵심

**대상:** `ReviewCreatedAiAnalysisEventListener`, `BatchReportWebhookEventListener`

```java
// ReviewCreatedAiAnalysisEventListener.java
@Async("aiAnalysisExecutor")
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onReviewCreated(ReviewCreatedEvent event) {
    restaurantAnalysisFacade.onReviewCreated(event.restaurantId());
}
```

**핵심 문제:** `@Transactional + @Rollback` 테스트에서는 커밋이 일어나지 않으므로 `AFTER_COMMIT` 리스너가 **절대 실행되지 않는다**.

#### 권장 전략 — 2계층 분리

##### 계층 1 — 리스너 내부 로직 단위테스트 (항상 필요)

리스너 메서드를 직접 호출해 "리스너가 무엇을 하는가"만 검증한다.
현재 `RestaurantReviewCreatedAiAnalysisServiceTest`가 이 방식을 사용하고 있으나, Facade를 직접 테스트하므로 리스너-Facade 연결이 검증되지 않는다.

```java
// ReviewCreatedAiAnalysisEventListenerTest.java
@UnitTest
@DisplayName("ReviewCreatedAiAnalysisEventListener")
class ReviewCreatedAiAnalysisEventListenerTest {

    @Mock
    RestaurantAnalysisFacade restaurantAnalysisFacade;

    @InjectMocks
    ReviewCreatedAiAnalysisEventListener listener;

    @Test
    @DisplayName("리뷰 이벤트 수신 시 AI 분석 Facade를 호출한다")
    void onReviewCreated_delegatesToFacade() {
        ReviewCreatedEvent event = new ReviewCreatedEvent(42L);

        listener.onReviewCreated(event);  // 직접 호출 — @Async, @TransactionalEventListener 모두 무시됨

        verify(restaurantAnalysisFacade).onReviewCreated(42L);
    }
}
```

현재 `RestaurantReviewCreatedAiAnalysisServiceTest`는 Facade 내부 로직(락 획득/해제)을 검증한다. 두 테스트가 다른 레벨을 다루므로 둘 다 유효하다.

##### 계층 2 — 발행-구독 연결 통합테스트 (선택적)

"이벤트 발행 → 트랜잭션 커밋 → 리스너 실행" 전체 플로우를 검증해야 할 때만 작성한다.

**핵심:** `@Transactional` 없이 테스트하여 실제 커밋이 발생하게 한다.

```java
// ReviewCreatedEventIntegrationTest.java
@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
@DisplayName("리뷰 생성 이벤트 통합 테스트")
class ReviewCreatedEventIntegrationTest {

    // @Transactional 없음 — 실제 커밋 발생

    @MockBean
    RestaurantAnalysisFacade restaurantAnalysisFacade;  // DB 의존성 없이 검증

    @Autowired
    ReviewService reviewService;

    @AfterEach
    void cleanup() {
        // 수동 DB 정리 (또는 @Sql로 처리)
    }

    @Test
    @DisplayName("리뷰 생성 후 커밋되면 AI 분석 리스너가 실행된다")
    void afterReviewCreated_aiAnalysisIsTriggered() {
        long restaurantId = 1L;

        reviewService.createReview(...);  // 실제 커밋 발생 → AFTER_COMMIT 리스너 트리거

        // Awaitility로 비동기 완료 대기
        await().atMost(5, SECONDS).untilAsserted(() ->
            verify(restaurantAnalysisFacade).onReviewCreated(restaurantId));
    }
}
```

**계층 2가 필요한 경우:**
- "이벤트가 올바른 타이밍에(커밋 후에만) 리스너를 트리거하는가" 검증
- `fallbackExecution = false`인 경우 — 트랜잭션 밖에서는 리스너가 실행되지 않음을 검증

**계층 1만으로 충분한 경우 (대부분):**
- "리스너가 무엇을 하는가"만 검증하면 되는 경우
- 이벤트 발행 타이밍보다 비즈니스 로직에 집중하는 경우

---

### 2.3 @Async + @Transactional

**대상:** `SearchHistoryRecorder`

```java
// SearchHistoryRecorder.java
@Async("searchHistoryExecutor")
@Transactional
public void recordSearchHistory(Long memberId, String keyword) { ... }
```

**전략:** 직접 메서드 호출로 비즈니스 로직만 검증.

```java
// SearchHistoryRecorderTest.java
@UnitTest
@DisplayName("SearchHistoryRecorder")
class SearchHistoryRecorderTest {

    @Mock
    MemberSearchHistoryRepository memberSearchHistoryRepository;

    @InjectMocks
    SearchHistoryRecorder recorder;

    @Test
    @DisplayName("검색 기록이 없으면 새로 생성된다")
    void recordSearchHistory_whenNoHistory_createsNew() {
        Long memberId = 1L;
        given(memberSearchHistoryRepository.findAllByMemberIdAndKeywordAndDeletedAtIsNull(memberId, "치킨"))
            .willReturn(List.of());

        recorder.recordSearchHistory(memberId, "치킨");  // 직접 호출 — @Async, @Transactional 무시됨

        verify(memberSearchHistoryRepository).save(any(MemberSearchHistory.class));
    }

    @Test
    @DisplayName("같은 키워드가 있으면 카운트를 증가시킨다")
    void recordSearchHistory_whenExists_incrementsCount() {
        Long memberId = 1L;
        MemberSearchHistory existing = MemberSearchHistory.create(memberId, "치킨");
        given(memberSearchHistoryRepository.findAllByMemberIdAndKeywordAndDeletedAtIsNull(memberId, "치킨"))
            .willReturn(List.of(existing));

        recorder.recordSearchHistory(memberId, "치킨");

        // 기존 레코드를 save하지 않고 incrementCount()만 호출 (JPA 더티체킹)
        verify(memberSearchHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("memberId가 null이면 스킵한다")
    void recordSearchHistory_withNullMemberId_skips() {
        recorder.recordSearchHistory(null, "치킨");

        verifyNoInteractions(memberSearchHistoryRepository);
    }

    @Test
    @DisplayName("예외가 발생해도 전파되지 않는다")
    void recordSearchHistory_onException_doesNotPropagate() {
        given(memberSearchHistoryRepository.findAllByMemberIdAndKeywordAndDeletedAtIsNull(any(), any()))
            .willThrow(new RuntimeException("DB 오류"));

        assertThatCode(() -> recorder.recordSearchHistory(1L, "치킨")).doesNotThrowAnyException();
    }
}
```

**`@RepositoryJpaTest`에서 통합 검증이 필요한 경우 (SyncTaskExecutor 교체):**

이벤트 발행을 통해 `@Async` 리스너가 트리거되는 흐름을 검증해야 할 때 사용한다.

```java
@TestConfiguration
static class TestConfig {
    @Bean
    @Primary
    TaskExecutor searchHistoryExecutor() {
        return new SyncTaskExecutor();  // @Async를 동기로 실행
    }
}
```

단, 직접 호출로 충분한 경우에는 `SyncTaskExecutor`를 사용하지 않는다.

---

### 2.4 수동 TransactionSynchronization

**대상:** `ReviewEventPublisher`

```java
// ReviewEventPublisher.java
public void publishReviewCreated(long restaurantId) {
    publishAfterCommit(new ReviewCreatedEvent(restaurantId));
}

private void publishAfterCommit(Object event) {
    if (TransactionSynchronizationManager.isActualTransactionActive()) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publisher.publishEvent(event);
            }
        });
        return;
    }
    publisher.publishEvent(event);  // 트랜잭션 없으면 즉시 발행
}
```

**전략:** Mock `ApplicationEventPublisher` + `TransactionTemplate`으로 실제 트랜잭션 경계를 만들어 검증.

```java
// ReviewEventPublisherTest.java
@UnitTest
@DisplayName("ReviewEventPublisher")
class ReviewEventPublisherTest {

    @Mock
    ApplicationEventPublisher applicationEventPublisher;

    ReviewEventPublisher publisher;

    // 실제 트랜잭션을 생성하기 위해 PlatformTransactionManager 필요
    // → UnitTest에서는 복잡하므로, 트랜잭션 유무에 따른 분기만 검증

    @BeforeEach
    void setUp() {
        publisher = new ReviewEventPublisher(applicationEventPublisher);
    }

    @Test
    @DisplayName("트랜잭션이 없으면 즉시 이벤트를 발행한다")
    void publishReviewCreated_withoutTransaction_publishesImmediately() {
        // 트랜잭션 없는 상태 (기본)
        publisher.publishReviewCreated(1L);

        verify(applicationEventPublisher).publishEvent(any(ReviewCreatedEvent.class));
    }
}
```

트랜잭션 활성 상태에서 커밋 후 발행 검증은 실제 `PlatformTransactionManager`가 필요하므로, 통합테스트 수준에서 작성한다:

```java
// ReviewEventPublisherIntegrationTest.java (통합테스트 필요 시)
@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class ReviewEventPublisherIntegrationTest {

    @Autowired
    ReviewEventPublisher publisher;

    @MockBean
    ApplicationEventPublisher applicationEventPublisher;  // Spring Context의 publisher를 Mock으로 교체

    @Autowired
    TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("트랜잭션 활성 상태에서 발행하면 커밋 후 이벤트가 발행된다")
    void publishReviewCreated_withActiveTransaction_publishesAfterCommit() {
        transactionTemplate.execute(status -> {
            publisher.publishReviewCreated(1L);
            verifyNoInteractions(applicationEventPublisher);  // 커밋 전 — 미발행
            return null;
        }); // 커밋 발생

        verify(applicationEventPublisher).publishEvent(any(ReviewCreatedEvent.class));
    }
}
```

---

### 2.5 @TransactionalEventListener + MQ 발행 — 모범 패턴

**대상:** `GroupMemberJoinedMessageQueuePublisher`

현재 프로젝트에서 가장 잘 된 패턴. 2계층을 명확히 분리한다.

#### 단위테스트 — `GroupMemberJoinedMessageQueuePublisherTest`

```java
// 직접 호출 + Mock MessageQueueProducer
@UnitTest
@DisplayName("GroupMemberJoined MQ 퍼블리셔")
class GroupMemberJoinedMessageQueuePublisherTest {

    @Test
    @DisplayName("provider가 none이면 메시지를 발행하지 않는다")
    void onGroupMemberJoined_withNoneProvider_skipsPublish() {
        MessageQueueProducer producer = mock(MessageQueueProducer.class);
        MessageQueueProperties properties = new MessageQueueProperties();
        properties.setProvider("none");
        GroupMemberJoinedMessageQueuePublisher publisher =
            new GroupMemberJoinedMessageQueuePublisher(producer, properties, new ObjectMapper());

        publisher.onGroupMemberJoined(new GroupMemberJoinedEvent(10L, 20L, "테스트 그룹", Instant.now()));

        verifyNoInteractions(producer);
    }

    @Test
    @DisplayName("provider가 redis-stream이면 GroupMemberJoined 이벤트를 MQ로 발행한다")
    void onGroupMemberJoined_withRedisStreamProvider_publishesMessage() throws Exception {
        // ... ArgumentCaptor로 발행된 메시지 구조 검증
    }
}
```

#### 통합테스트 — `NotificationMessageQueueFlowIntegrationTest`

```java
// @SpringBootTest + 커스텀 TestConfig + Mock MQ
@SpringBootTest(classes = NotificationMessageQueueFlowIntegrationTest.TestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Tag("integration")
class NotificationMessageQueueFlowIntegrationTest {

    @Resource
    ApplicationEventPublisher applicationEventPublisher;

    @Resource
    MessageQueueProducer messageQueueProducer;  // Mock

    @Test
    @DisplayName("GroupMemberJoined 이벤트 발행 시 MQ publish와 notification 소비 처리까지 이어진다")
    void groupMemberJoinedEvent_publishAndConsume() throws Exception {
        applicationEventPublisher.publishEvent(
            new GroupMemberJoinedEvent(10L, 20L, "스터디 그룹", Instant.parse("2026-02-15T00:00:00Z")));

        verify(messageQueueProducer).publish(messageCaptor.capture());
        // 발행된 메시지 구조 검증...

        // 컨슈머 핸들러도 직접 호출해 역직렬화 + 알림 생성까지 검증
        handlerCaptor.getValue().handle(MessageQueueMessage.of(...));
        verify(notificationService).createNotification(...);
    }

    @Configuration
    static class TestConfig {
        @Bean MessageQueueProducer messageQueueProducer() { return Mockito.mock(MessageQueueProducer.class); }
        @Bean NotificationService notificationService() { return Mockito.mock(NotificationService.class); }
        // ... 실제 필요한 빈만 등록
    }
}
```

**이 패턴의 핵심:**
- `@SpringBootTest`이지만 전체 Context가 아닌 **최소 필요 빈만 `TestConfig`에 등록**
- `@TransactionalEventListener(fallbackExecution = true)`이므로 트랜잭션 없이도 리스너가 실행됨
- MQ Producer/Consumer는 Mock으로 교체해 외부 의존성 제거

---

## 3. SyncTaskExecutor 사용 가이드

### 언제 필요한가

`@Async` 메서드가 **이벤트 발행**을 통해 트리거될 때:

```
applicationEventPublisher.publishEvent(event)
    → @EventListener 메서드 실행 (비동기)
    → 리스너 내부 로직 실행
```

테스트에서 `publishEvent()` 호출 후 리스너가 **같은 스레드에서 즉시** 실행되어야 할 때 사용한다.

```java
@TestConfiguration
static class TestConfig {
    @Bean
    @Primary
    TaskExecutor notificationExecutor() {
        return new SyncTaskExecutor();
    }
}
```

### 언제 필요 없는가

- 리스너를 **직접 호출**하는 경우 (대부분의 단위테스트)
- 비동기 실행 여부가 테스트 목적이 아닌 경우
- `@TransactionalEventListener`에는 효과 없음 (Executor 교체로는 커밋 타이밍을 바꿀 수 없음)

---

## 4. Awaitility — 진짜 비동기가 필요할 때

`@Transactional` 없는 통합테스트에서 실제 비동기 완료를 기다려야 할 때 사용한다.

```groovy
// build.gradle
testImplementation 'org.awaitility:awaitility:4.2.0'
```

```java
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

@Test
void afterBatchFinished_webhookIsSent() {
    batchExecutionService.finish(batchExecutionId);

    await().atMost(5, SECONDS)
        .pollInterval(100, MILLISECONDS)
        .untilAsserted(() ->
            verify(batchReportWebhookClient, atLeastOnce()).send(any()));
}
```

**사용 기준:**
- `@Transactional` 없는 통합테스트에서 비동기 완료를 기다릴 때
- VirtualThread Executor처럼 `SyncTaskExecutor`로 대체할 수 없는 경우
- `Thread.sleep()`을 사용하고 싶어지는 순간

**Awaitility도 불필요한 경우:**
- 리스너를 직접 호출하면 동기로 실행되므로 대기 불필요
- `SyncTaskExecutor`로 동기화된 경우

---

## 5. 안티패턴

### ❌ Thread.sleep()으로 비동기 대기

```java
// BAD
publisher.publishEvent(event);
Thread.sleep(1000);  // 느리고 불안정
verify(service).doSomething();

// GOOD
publisher.publishEvent(event);
await().atMost(5, SECONDS).untilAsserted(() -> verify(service).doSomething());
```

### ❌ @Transactional + @Rollback 테스트에서 AFTER_COMMIT 리스너 검증 시도

```java
// BAD — 리스너가 절대 실행되지 않음
@Transactional
@Rollback
@Test
void afterCommit_listenerIsTriggered() {
    reviewService.createReview(...);  // 커밋 안 됨
    verify(restaurantAnalysisFacade).onReviewCreated(any());  // ← 절대 검증 불가
}

// GOOD — 계층 1: 리스너 직접 호출
listener.onReviewCreated(new ReviewCreatedEvent(1L));
verify(restaurantAnalysisFacade).onReviewCreated(1L);
```

### ❌ 비동기 실행 스레드 검증

```java
// BAD — Spring 내부 구현에 결합
@Test
void runsOnSeparateThread() {
    AtomicReference<String> threadName = new AtomicReference<>();
    // ... 스레드 이름 캡처 시도
    assertThat(threadName.get()).contains("aiAnalysisExecutor");  // Spring 내부 구현에 의존
}
```

### ❌ 직접 호출로 충분한 경우에 SyncTaskExecutor 통합테스트 추가

```java
// BAD — 불필요한 복잡도
@SpringBootTest
@Tag("integration")
class NotificationListenerTest {
    @TestConfiguration
    static class Config {
        @Bean @Primary
        TaskExecutor notificationExecutor() { return new SyncTaskExecutor(); }
    }

    // 리스너를 직접 호출하면 되는데 굳이 SpringBootTest + SyncTaskExecutor
    @Test
    void test() { ... }
}

// GOOD — UnitTest로 충분
@UnitTest
class NotificationListenerTest {
    @InjectMocks NotificationEventListener listener;
    @Test
    void test() { listener.onGroupMemberJoined(event); ... }
}
```

---

## 요약 — 빠른 참조

```
비동기 컴포넌트 테스트 선택 트리

@Async + @EventListener
└─→ 리스너 직접 호출 (@UnitTest)
    └─→ 에러 격리도 assertThatCode().doesNotThrowAnyException()

@Async + @TransactionalEventListener(AFTER_COMMIT)
├─→ [계층 1] 리스너 직접 호출 (@UnitTest) — 항상
└─→ [계층 2] @Transactional 없는 통합테스트 + Awaitility — 발행-구독 연결이 필요할 때만

@Async + @Transactional
└─→ 리스너 직접 호출 (@UnitTest)
    └─→ 이벤트 발행부터 검증할 때만 SyncTaskExecutor 추가

수동 TransactionSynchronization
├─→ 트랜잭션 없는 경우: 직접 호출 (@UnitTest)
└─→ 트랜잭션 있는 경우: TransactionTemplate + @SpringBootTest

@TransactionalEventListener + MQ
├─→ 발행자 로직: 직접 호출 (@UnitTest)
└─→ 발행-소비 연결: 최소 TestConfig @SpringBootTest
```
