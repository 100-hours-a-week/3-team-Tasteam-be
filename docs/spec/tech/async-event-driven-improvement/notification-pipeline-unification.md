# 알림 비동기 파이프라인 단일화 개선

## 1. 목적

현재 알림 생성/발송 경로가 direct async, outbox + MQ, legacy MQ, chat 전용 경로로 분산된 상태를 단일 운영 경로로 수렴시키기 위한 개선 방향을 정리한다.

## 2. 현재 구현 근거

| 위치 | 조건 | 현재 역할 | 문제 포인트 |
|---|---|---|---|
| `NotificationEventListener` | MQ off | 그룹 가입/회원 가입 인앱 알림 direct async 생성 | MQ 경로와 완전히 다른 모델 |
| `NotificationDomainEventListener` | MQ on | `notification_outbox` enqueue | ~~`AFTER_COMMIT` 의존~~ → **`BEFORE_COMMIT` 수정 완료** |
| `NotificationOutboxScanner` | MQ on | outbox -> MQ publish | request 발행 담당 |
| `NotificationMessageQueueConsumer` | MQ on | notification request 소비 | retry 상태 인메모리 |
| `NotificationDispatcher` | MQ on | consumed insert 후 채널별 발송 | 부분 실패를 event-level consumed가 덮음 |
| ~~`GroupMemberJoinedMessageQueuePublisher`~~ | - | ~~legacy group joined topic publish~~ | **삭제 완료** (2026.03.15) |
| ~~`NotificationMessageQueueConsumerRegistrar`~~ | - | ~~legacy topic 소비 후 바로 알림 생성~~ | **삭제 완료** (2026.03.15) |
| `ChatNotificationEventListener` | 항상 | 채팅 인앱 알림 + FCM push | outbox / delivery 상태 미공유 (잔여 과제) |

## 3. 왜 문제인가

- 그룹 가입은 notification request 경로와 legacy MQ 경로가 동시에 존재한다.
- 채팅 알림은 공통 request / delivery 모델을 우회한다.
- `NotificationDispatcher`는 `consumed_notification_event`를 먼저 기록하고 채널별 예외를 로그만 남겨 partial failure 재처리가 어렵다.

## 4. 실제 문제 시나리오

### 시나리오 A. 그룹 가입 알림 중복

1. 그룹 가입 성공
2. `NotificationDomainEventListener`가 request outbox 적재
3. `GroupMemberJoinedMessageQueuePublisher`가 legacy topic publish
4. `NotificationMessageQueueConsumerRegistrar`가 동일 사실로 인앱 알림 생성

### 시나리오 B. 채팅 메시지 롤백 이후 유령 알림

1. `ChatService.sendMessage()`가 메시지 저장 중 `ChatMessageSentEvent` 발행
2. `ChatNotificationEventListener`가 비동기 알림/푸시 전송
3. 이후 트랜잭션 롤백

### 시나리오 C. PUSH/EMAIL 누락

1. request 1건 소비
2. WEB 성공
3. PUSH/EMAIL 실패
4. event는 consumed 처리
5. 일부 채널만 누락

## 5. 목표 상태

- request 생성은 단일 경로
- recipient request와 channel delivery를 분리
- chat도 같은 request outbox 경로 사용
- legacy group-joined 알림 경로 제거

## 6. 개선 방향

| worker | 책임 |
|---|---|
| request consumer | request 수용, delivery row fan-out |
| WEB delivery worker | 인앱 알림 row 생성 |
| PUSH delivery worker | FCM 발송과 결과 반영 |
| EMAIL delivery worker | 이메일 발송과 결과 반영 |

추가 방향:
- `notification_outbox`를 recipient request 기준으로 정리
- `notification_delivery`로 channel 상태 분리
- `ChatNotificationEventListener`를 request outbox append 기반으로 전환
- `GroupMemberJoinedMessageQueuePublisher`, `NotificationMessageQueueConsumerRegistrar` 제거

## 7. 구현 순서

1. request / delivery 상태 모델 확정
2. `NotificationDispatcher` 역할 분리
3. WEB delivery row 도입
4. PUSH/EMAIL worker 분리
5. chat 경로 이관
6. ~~legacy group-joined 경로 제거~~ → **완료** (2026.03.15): `GroupMemberJoinedMessageQueuePublisher`, `NotificationMessageQueueConsumerRegistrar` 삭제

## 8. 검증 포인트

- 그룹 가입 1회에 request row가 1건만 생기는가
- 같은 request 재처리 시 delivery row가 중복되지 않는가
- WEB 성공 + PUSH 실패 시 PUSH만 재처리되는가
- chat message rollback 시 request / delivery row가 남지 않는가
