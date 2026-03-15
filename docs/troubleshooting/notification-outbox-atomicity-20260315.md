| 항목 | 내용 |
|---|---|
| 문서 제목 | 알림 아웃박스 원자성 결함 수정 및 알림 중복 경로 제거 |
| 작성일 | 2026.03.15 |
| 관련 이슈 | #567 (feat: kafka-eos-notification) |
| 영향 범위 | `domain/notification/event`, `domain/notification/outbox`, `domain/group/event`, `infra/messagequeue` |
| 상태 | 해결 완료 |

<br>

# 알림 아웃박스 원자성 결함 수정 및 알림 중복 경로 제거

---

## 1. 문제 배경

### 1-1. 알림 중복 발생 (MQ enabled 환경)

`GroupMemberJoinedEvent` 발생 시 두 개의 독립적인 알림 경로가 동시에 동작하고 있었다.

```
경로 A (정상): NotificationDomainEventListener → notification_outbox → NOTIFICATION_REQUESTED → NotificationDispatcher
경로 B (중복): GroupMemberJoinedMessageQueuePublisher → GROUP_MEMBER_JOINED → NotificationMessageQueueConsumerRegistrar → NotificationService.createNotification()
```

`MQ enabled` 환경에서 그룹 가입 1건에 알림이 **2번** 생성되는 결함이 존재했다.

### 1-2. 계층 역전 (infra → domain 직접 의존)

`infra/messagequeue/NotificationMessageQueueConsumerRegistrar`가 `domain/notification/service/NotificationService`를 직접 의존하고 있었다. 인프라 계층이 도메인 계층을 직접 호출하는 구조로, 추후 모듈 분리 시 강결합 문제를 유발할 설계였다.

### 1-3. Outbox 원자성 결함 (핵심)

`AFTER_COMMIT + REQUIRES_NEW` 조합은 outbox row 저장과 도메인 트랜잭션이 **별개의 트랜잭션**으로 처리되는 구조였다.

```
[문제 구조]
도메인 TX commit
  → (TX 종료)
  → AFTER_COMMIT 호출 (TX 바깥)
  → REQUIRES_NEW: 별도 TX 시작
  → outbox insert
  → 별도 TX commit

유실 시나리오:
  1. 그룹 멤버 row commit
  2. 프로세스 종료 또는 outbox insert 실패
  3. 도메인 변경은 반영, notification_outbox row는 미생성
  4. 알림 스캐너가 처리할 row 없음 → 알림 유실
```

### 1-4. 로그 명칭 오류

`KafkaMessageQueueProducer`의 `executeInTransaction()`은 **Kafka 프로듀서 측 idempotency**만 제공하는데, 로그에 `"Kafka EOS publish"` 로 표기하여 마치 DB↔Kafka 간 EOS(Exactly-Once Semantics)가 보장되는 것처럼 오해를 유발했다.

---

## 2. 근본 원인 분석

### AFTER_COMMIT + REQUIRES_NEW의 트랜잭션 타임라인

```
Thread: 도메인 서비스 스레드
─────────────────────────────────────────────
1. @Transactional 시작 (도메인 TX)
2. 도메인 상태 변경 (group_member insert)
3. applicationEventPublisher.publishEvent(GroupMemberJoinedEvent)
   └─ Spring은 이벤트를 큐에 넣음 (아직 호출 X)
4. 도메인 TX commit ← 여기서 이미 DB 상태 확정
5. AFTER_COMMIT phase 진입 (TX 바깥)
6. NotificationDomainEventListener.onGroupMemberJoined() 호출
7. outboxService.enqueue() 호출
8. REQUIRES_NEW: 새 TX 시작
9. notification_outbox insert
10. 새 TX commit (또는 실패)
─────────────────────────────────────────────
4번과 9번 사이에서 프로세스 종료 시 → 알림 유실
```

### BEFORE_COMMIT + MANDATORY의 올바른 타임라인

```
Thread: 도메인 서비스 스레드
─────────────────────────────────────────────
1. @Transactional 시작 (도메인 TX)
2. 도메인 상태 변경 (group_member insert)
3. applicationEventPublisher.publishEvent(GroupMemberJoinedEvent)
4. BEFORE_COMMIT phase 진입 (TX 안)
5. NotificationDomainEventListener.onGroupMemberJoined() 호출
6. outboxService.enqueue() 호출
7. MANDATORY: 기존 TX 참여 (새 TX 없음)
8. notification_outbox insert ← 도메인 변경과 동일 TX
9. 도메인 TX commit (group_member + notification_outbox 동시 commit)
   └─ 실패 시 양쪽 모두 rollback
─────────────────────────────────────────────
도메인 변경과 outbox row가 원자적으로 처리됨
```

---

## 3. 수정 내용

### 3-1. 중복 알림 경로 제거

| 파일 | 변경 |
|---|---|
| `infra/messagequeue/NotificationMessageQueueConsumerRegistrar.java` | **삭제** |
| `infra/messagequeue/NotificationMessageQueueConsumerRegistrarTest.java` | **삭제** |

`NotificationDomainEventListener`가 outbox 경유로 동일 이벤트를 처리하므로 완전한 중복이었다.

### 3-2. Producer 패키지 이동 (계층 정리)

`GroupMemberJoinedEvent`를 MQ에 발행하는 클래스는 group 도메인의 producer이므로 `infra/` 에서 `domain/group/event/`로 이동했다.

| 기존 | 변경 후 |
|---|---|
| `infra/messagequeue/GroupMemberJoinedMessageQueuePublisher.java` | `domain/group/event/GroupMemberJoinedMqPublisher.java` |
| `infra/messagequeue/GroupMemberJoinedMessagePayload.java` | `domain/group/event/GroupMemberJoinedMessagePayload.java` |
| `infra/messagequeue/GroupMemberJoinedMessageQueuePublisherTest.java` | `domain/group/event/GroupMemberJoinedMqPublisherTest.java` |

### 3-3. Outbox 원자성 수정

**`NotificationDomainEventListener.java`** — 4개 메서드 전부 변경

```java
// Before
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
public void onGroupMemberJoined(GroupMemberJoinedEvent event) {
    try {
        outboxService.enqueue(payload);
    } catch (Exception ex) {
        log.error("GroupMemberJoinedEvent 알림 아웃박스 등록 실패...", ex);
    }
}

// After
@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
public void onGroupMemberJoined(GroupMemberJoinedEvent event) {
    outboxService.enqueue(payload);
}
```

변경 포인트:
- `AFTER_COMMIT` → `BEFORE_COMMIT`: 도메인 TX 내부에서 실행되도록 변경
- `fallbackExecution = true` 제거: TX 없이 호출 시 정상 실행되지 않도록 의도적으로 제거
- try-catch 제거: 예외가 도메인 TX에 전파되어 rollback을 유도하도록 변경

**`NotificationOutboxService.java`** — `enqueue()` 전파 방식 변경

```java
// Before
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void enqueue(NotificationRequestedPayload payload) { ... }

// After
@Transactional(propagation = Propagation.MANDATORY)
public void enqueue(NotificationRequestedPayload payload) { ... }
```

`MANDATORY`: 활성 TX가 없으면 `IllegalTransactionStateException` 발생 → enqueue가 반드시 도메인 TX 안에서만 호출되도록 강제. TX 없이 호출하면 즉시 실패하여 설계 의도를 코드로 표현.

### 3-4. 로그 명칭 정정

**`KafkaMessageQueueProducer.java`**

```java
// Before
log.debug("Kafka EOS publish 성공. topic={}, messageId={}", ...);
log.error("Kafka EOS publish 실패. topic={}, messageId={}", ...);

// After
log.debug("Kafka publish 성공. topic={}, messageId={}", ...);
log.error("Kafka publish 실패. topic={}, messageId={}", ...);
```

---

## 4. EOS(Exactly-Once Semantics) 실제 보장 수준

이번 수정과 함께 EOS 용어 사용에 대한 명확한 기준을 정리한다.

### 진정한 EOS가 불가능한 이유

DB와 MQ는 서로 다른 트랜잭션 시스템이다. 두 시스템에 걸친 진정한 EOS는 XA(2-Phase Commit) 또는 Kafka Transactions + DB XA 조합이 필요하며, 이는 Kafka의 `isolation.level=read_committed` 설정과 함께 DB 트랜잭션을 Kafka Transaction에 종속시키는 복잡한 구조를 요구한다. 운영 복잡도가 높아 일반적으로 채택하지 않는다.

### 현재 구현의 실제 보장 수준: **effectively-once**

| 보장 계층 | 구현 | 설명 |
|---|---|---|
| 도메인 변경 + outbox 원자성 | `BEFORE_COMMIT` + `MANDATORY` | 도메인 row와 outbox row가 동일 TX에서 commit |
| MQ 발행 at-least-once | `NotificationOutboxScanner` polling + retry | outbox row 기반 재시도, 최소 1회 발행 보장 |
| Kafka 프로듀서 idempotency | `ENABLE_IDEMPOTENCE_CONFIG=true` + `executeInTransaction()` | 프로듀서 재시도로 인한 Kafka 측 중복 제거 |
| Kafka 소비 격리 | `ISOLATION_LEVEL_CONFIG=read_committed` | uncommitted 메시지 읽지 않음 |
| Consumer-side idempotency | `ConsumedNotificationEventJdbcRepository.tryInsert()` (ON CONFLICT DO NOTHING) | 동일 event_id 재처리 방지 |

```
최종 전달 보장:
at-least-once (발행) + consumer-side idempotency (중복 제거) = effectively-once (실질적 1회 전달)
```

"EOS"는 진정한 Kafka EOS 의미가 아니므로 코드와 문서에서 사용하지 않는다.

---

## 5. 현재 알림 파이프라인 전체 흐름

```
[도메인 서비스 TX 내부]
  1. 도메인 변경 (e.g., group_member INSERT)
  2. ApplicationEventPublisher.publishEvent(GroupMemberJoinedEvent)
  3. BEFORE_COMMIT: NotificationDomainEventListener.onGroupMemberJoined()
  4. NotificationOutboxService.enqueue() (MANDATORY — 동일 TX 참여)
  5. notification_outbox INSERT (status=PENDING)
  6. TX COMMIT → group_member + notification_outbox 원자적 저장

[스케줄러 — 30초 간격]
  7. NotificationOutboxScanner.scan()
  8. PENDING/FAILED 엔트리 조회 (지수 백오프 존중)
  9. QueueEventPublisher → evt.notification.v1 발행
  10. 성공 → markPublished() / 실패 → markFailed() (최대 5회, 최대 300s 백오프)

[MQ Consumer]
  11. NotificationMessageQueueConsumer.handleMessage()
  12. NotificationMessageProcessor.process()
  13. NotificationDispatcher.dispatch()
      a. ConsumedNotificationEventJdbcRepository.tryInsert() — 중복 차단
      b. WEB: NotificationService.createNotification()
      c. PUSH: FcmPushService (circuit breaker)
      d. EMAIL: EmailSender (circuit breaker)

[DLQ]
  - Kafka: DefaultErrorHandler → DeadLetterPublishingRecoverer → evt.notification.v1.dlq
  - Redis: NotificationDlqPublisher → notification DLQ topic
```

---

## 6. MQ disabled 경로 (로컬/기본 환경)

`tasteam.message-queue.enabled=false` (기본값) 시에는 `NotificationEventListener`가 활성화된다.

```
ApplicationEventPublisher.publishEvent(GroupMemberJoinedEvent)
  → @EventListener + @Async("notificationExecutor")
  → NotificationService.createNotification() 직접 호출
```

이 경로는 outbox를 거치지 않으며, MQ 없이 비동기 스레드에서 알림을 생성한다. 로컬 개발 및 단순 배포 환경을 위한 fallback이다.

---

## 7. 테스트 커버리지

이번 수정과 함께 3개의 비동기 리스너 단위 테스트를 추가했다.

| 테스트 파일 | 케이스 수 | 검증 내용 |
|---|---|---|
| `NotificationDomainEventListenerTest` | 11개 | 4개 이벤트 핸들러의 payload 필드, channels, templateKey 검증 |
| `ChatNotificationEventListenerTest` | 15개 | SYSTEM 필터, WEB 알림 생성, PUSH 발송 조건, 메시지 프리뷰 처리 |
| `AdminBroadcastNotificationEventListenerTest` | 8개 | WEB/PUSH/EMAIL 채널별 분기, templateKey null/blank 처리, 채널 실패 격리 |

전략: `@UnitTest` + 리스너 직접 호출 (비동기 실행 자체는 Spring이 보장하므로 테스트하지 않음).

---

## 8. 잔여 과제 (후속 이슈)

이번 수정으로 원자성 결함과 중복 경로는 해결됐으나, `async-event-driven-improvement` 허브에 정의된 다음 과제는 별도 이슈에서 처리한다.

| 과제 | 우선순위 | 비고 |
|---|---|---|
| 알림 파이프라인 단일화 (Chat, Admin 경로도 outbox 경유) | 중 | `notification-pipeline-unification.md` 참조 |
| `notification_delivery` 테이블 도입 (채널별 상태 분리) | 중 | partial failure 재처리 개선 |
| Redis Stream pending reclaim 영속화 | 중 | `mq-retry-dlq-reclaim.md` 참조 |
| CDC/Debezium 기반 outbox relay | 보류 | Redis Stream은 Debezium 미지원 → Polling Outbox 유지 |
