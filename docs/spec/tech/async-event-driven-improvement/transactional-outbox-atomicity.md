# 트랜잭셔널 아웃박스 원자성 개선

## 1. 목적

현재 알림과 사용자 이벤트 수집에서 outbox 테이블은 존재하지만, 비즈니스 변경과 outbox row 생성이 같은 트랜잭션으로 묶이지 않는 지점을 정리한다.

## 2. 현재 구현 근거

| 위치 | 현재 구현 | 원자성 관점 해석 |
|---|---|---|
| `GroupEventPublisher` | `TransactionSynchronization.afterCommit()`에서 publish | 그룹 멤버십 commit 후 이벤트 publish |
| `ReviewEventPublisher` | 리뷰 생성 후 `afterCommit()` publish | 리뷰 row와 이벤트 publish가 분리 |
| `NotificationDomainEventListener` | ~~`@TransactionalEventListener(AFTER_COMMIT)`~~ → **`BEFORE_COMMIT` 수정 완료** | ~~알림 outbox 적재가 비즈니스 트랜잭션 바깥~~ → **동일 TX에서 원자적 저장** |
| `NotificationOutboxService.enqueue()` | ~~`@Transactional(REQUIRES_NEW)`~~ → **`MANDATORY` 수정 완료** | ~~outbox insert가 별도 트랜잭션~~ → **도메인 TX 참여 강제** |
| `ActivityDomainEventListener` | `@TransactionalEventListener(AFTER_COMMIT)` | 사용자 이벤트 source outbox 적재도 동일 구조 (미수정) |
| `UserActivityDispatchOutboxEnqueueHook.afterStored()` | `@Transactional(REQUIRES_NEW)` | 최종 저장과 dispatch enqueue가 분리 (미수정) |

## 3. 왜 문제인가

- business row commit과 outbox insert가 분리돼 "도메인 변경은 반영됐지만 outbox row가 없다"는 케이스가 남는다.
- 재시도는 outbox row가 이미 있는 경우에만 가능하다.
- API는 성공이고 DB 사실도 존재하지만 MQ trace에는 흔적이 남지 않는 유실 구간이 생긴다.

## 4. 실제 장애 시나리오

### 시나리오 A. 그룹 가입 직후 프로세스 종료

1. 그룹 멤버 row commit
2. `GroupEventPublisher.afterCommit()` 호출 대기
3. 프로세스 종료
4. `GroupMemberJoinedEvent` 미발행
5. `notification_outbox` row 미생성

### 시나리오 B. 리뷰 생성 후 analytics listener 예외

1. 리뷰 row commit
2. `ReviewEventPublisher`가 `ReviewCreatedEvent` 발행
3. `ActivityDomainEventListener` 또는 `ActivityEventOrchestrator`에서 예외
4. `user_activity_source_outbox` 적재 실패 또는 이후 sink 누락

## 5. 목표 상태

- 서비스 트랜잭션 안에서 business row와 outbox row를 함께 commit
- scanner / publisher는 outbox row만 읽음
- retry 기준을 outbox row 존재 여부로 단일화

## 6. 개선 방향

| 도메인 | 현재 | 목표 |
|---|---|---|
| 그룹 가입/심사 알림 | `GroupEventPublisher` -> `NotificationDomainEventListener` | 그룹 관련 서비스 내부에서 notification outbox append |
| 리뷰 활동 이벤트 | `ReviewEventPublisher` -> `ActivityDomainEventListener` | 리뷰 생성 서비스 내부에서 source outbox append |
| 채팅 알림 | `ChatNotificationEventListener` direct async | `ChatService.sendMessage()` 내부 request outbox append |

핵심:
- 서비스 메서드에서 상태 변경 직후 `OutboxAppender.append(...)`
- 같은 트랜잭션에서 outbox insert
- 이후 scanner가 후속 비동기 전달 전담

## 7. 구현 순서

1. ~~공통 outbox append 계약 정리~~ (완료)
2. ~~서비스 내부 append 추가~~ → **알림 도메인 `BEFORE_COMMIT + MANDATORY` 수정 완료** (2026.03.15)
3. 기존 listener 경로와 병행 운영 → 알림 도메인은 완료, user-activity 도메인은 미수정
4. dual-write 차이 모니터링
5. `afterCommit()` / `AFTER_COMMIT` 기반 outbox 적재 제거 → user-activity 도메인 잔여

**알림 도메인 수정 요약 (2026.03.15)**:
- `NotificationDomainEventListener`: 4개 메서드 `AFTER_COMMIT` → `BEFORE_COMMIT`, try-catch 제거
- `NotificationOutboxService.enqueue()`: `REQUIRES_NEW` → `MANDATORY`
- 트러블슈팅 문서: [알림 아웃박스 원자성 결함 수정 및 알림 중복 경로 제거](https://github.com/100-hours-a-week/3-team-tasteam-wiki/wiki/%5BTroubleshooting%5D-알림-아웃박스-원자성-결함-수정-및-알림-중복-경로-제거)

## 8. 검증 포인트

- rollback 시 outbox row도 함께 rollback 되는가 ← **알림 도메인 수정 완료**
- commit 직후 강제 종료 테스트에서 business row만 남는 경우가 사라지는가 ← **알림 도메인 수정 완료**
- 동일 `event_id` 재시도 시 unique 키가 기대대로 동작하는가 ← `insertIfAbsent` (ON CONFLICT DO NOTHING) 기존 유지
