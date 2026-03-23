# MQ 재처리 · DLQ · pending reclaim 개선

## 1. 목적

Redis Stream consumer group 기반 소비에서 실패 메시지의 재처리, DLQ, pending reclaim 전략을 영속 상태 기준으로 정리한다.

## 2. 현재 구현 근거

| 위치 | 현재 구현 | 문제 포인트 |
|---|---|---|
| `RedisStreamMessageQueueConsumer` | handler 성공 시 ack, 실패 시 warn 로그 | 실패 상태를 저장하지 않음 |
| `RedisStreamMessageQueueConsumer` | `ReadOffset.lastConsumed()` 기반 구독 | pending reclaim 루프 없음 |
| `NotificationMessageQueueConsumer` | `retryCountMap`으로 retry count 관리 | 메모리 기반이라 재기동/scale-out 취약 |
| `NotificationMessageQueueConsumer` | max retry 초과 시 DLQ publish | 현재 프로세스 관점 실패 횟수 |
| `consumed_notification_event` | `(consumer_group, event_id)` PK | 성공/실패/claim 상태 표현 불가 |
| `Kafka Connect S3 Sink Connector` | `UserActivityEventStoreService.store()` 호출 | pending 고착 시 최종 저장 누락 가능 |

## 3. 왜 문제인가

- pending 메시지가 장시간 고착될 수 있다.
- retry count가 JVM 메모리에만 있어 재기동 시 초기화된다.
- 운영자가 기대하는 "총 실패 횟수"와 현재 DLQ 기준이 어긋난다.

## 4. 실제 장애 시나리오

### 시나리오 A. handler 중간 pod 종료

1. consumer가 메시지를 읽음
2. handler 도중 pod 종료
3. ack 안 됨
4. pending list에 메시지가 남음
5. reclaim worker 없음

### 시나리오 B. retry 횟수 왜곡

1. 인스턴스 A에서 2회 실패
2. 인스턴스 B에서 다시 2회 실패
3. 각 인스턴스는 자기 메모리만 봄
4. 실제 누적 실패 횟수와 로컬 retry count가 다름

## 5. 목표 상태

- message 처리 상태가 DB에 영속 저장
- pending reclaim scheduler 존재
- 최대 시도 초과 시 DLQ로 단 한 번 이동
- notification과 user-activity가 동일한 운영 용어를 사용

## 6. 개선 방향

권장 필드:

| 필드 | 의미 |
|---|---|
| `consumer_group` | consumer group |
| `message_id` | MQ message 식별자 |
| `event_id` | business event 식별자 |
| `status` | `PROCESSING`, `FAILED`, `SUCCEEDED`, `DEAD` |
| `attempt_count` | 누적 시도 횟수 |
| `next_retry_at` | 다음 재시도 시각 |
| `claimed_by` | 현재 consumer |
| `claimed_at` | claim 시각 |
| `last_error` | 마지막 오류 |

핵심 방향:
- 성공 후 상태를 `SUCCEEDED`로 기록한 다음 ack
- 실패 시 ack하지 않고 상태만 `FAILED`
- idle timeout 초과 pending을 reclaim
- 최대 시도 초과 시 DLQ publish 후 `DEAD`

## 7. 구현 순서

1. notification 소비 상태 테이블 확장 또는 신규 도입
2. handler 성공/실패 시 DB 상태 반영
3. pending reclaim scheduler 추가
4. DLQ payload에 메타데이터 포함
5. user activity consumer에도 같은 모델 적용

## 8. 검증 포인트

- pod 종료 후 idle pending이 reclaim 되는가
- retry count가 재기동 이후에도 이어지는가
- 최대 시도 초과 메시지가 DLQ로 1회만 이동하는가
