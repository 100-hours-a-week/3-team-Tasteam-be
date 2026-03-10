# 이벤트 ID · 멱등성 계약 개선

## 1. 목적

business event, recipient request, channel delivery 식별자를 분리하고 `event_id` 생성 시점과 멱등성 키의 의미를 고정한다.

## 2. 현재 구현 근거

| 위치 | 현재 구현 | 문제 포인트 |
|---|---|---|
| `ReviewCreatedEvent` | `restaurantId` 하나만 보유 | business fact 식별 정보 부족 |
| `ReviewCreatedActivityEventMapper` | mapper 시점 `UUID.randomUUID()` | 동일 리뷰 사실 재발행 dedupe 어려움 |
| `NotificationDomainEventListener` | `NotificationRequestedPayload` 생성 시 `UUID.randomUUID()` | business event id와 recipient request id 혼재 |
| `notification_outbox` | `event_id` unique | request 단위인지 business fact 단위인지 의미 불명확 |
| `consumed_notification_event` | `(consumer_group, event_id)` PK | delivery 단위 멱등성 표현 어려움 |
| `user_activity_event` / `user_activity_source_outbox` | `event_id` unique | upstream event id 품질이 낮으면 중복 억제도 약함 |

## 3. 왜 문제인가

- 현재 `event_id`는 경로에 따라 business fact, request, outbox row, consumer dedupe 키처럼 사용된다.
- listener나 mapper에서 새 UUID를 만들면 같은 business fact 재발행을 중복으로 인식하지 못한다.
- `ReviewCreatedEvent`는 `reviewId`, `memberId`, `createdAt`이 없어 replay와 debugging 품질이 떨어진다.

## 4. 실제 문제 시나리오

### 시나리오 A. 리뷰 생성 이벤트 재발행

1. 동일 리뷰 생성 사실이 두 번 수집 흐름에 들어옴
2. mapper가 매번 새 UUID 생성
3. `user_activity_event`는 서로 다른 `event_id`로 저장

### 시나리오 B. 알림 request와 delivery 혼선

1. 그룹 가입 1건 발생
2. 동일 수신자에게 WEB, PUSH 두 채널 전송 필요
3. `NotificationRequestedPayload.eventId` 하나가 전체를 대표
4. WEB 성공 후 PUSH 실패 시 request와 delivery 단위 재처리 구분이 어려움

## 5. 목표 상태

- `event_id`: business fact 식별자
- `request_id`: recipient 단위 알림 요청 식별자
- `delivery_id`: recipient + channel 단위 발송 식별자
- `event_id`는 서비스 write boundary에서 생성

## 6. 권장 계약

| 레벨 | 키 | 의미 |
|---|---|---|
| business event | `event_id` | business fact 식별자 |
| request | `(event_id, recipient_id)` 또는 `request_id` | recipient 단위 알림 요청 |
| delivery | `(request_id, channel)` 또는 `delivery_id` | 채널 단위 실제 발송 |

리뷰 생성 이벤트 권장 필드:
- `event_id`
- `review_id`
- `member_id`
- `restaurant_id`
- `group_id`
- `subgroup_id`
- `created_at`

## 7. 구현 순서

1. domain event envelope 표준화
2. `ReviewCreatedEvent`, 그룹 관련 이벤트 payload 확장
3. mapper / listener의 late UUID 생성 제거
4. notification request / delivery 식별자 분리
5. unique key와 migration 재정렬

## 8. 검증 포인트

- 같은 business fact 재발행 시 `event_id` 기준 dedupe가 되는가
- 같은 request 재처리 시 delivery만 중복 없이 다시 수행되는가
- replay 시 `aggregate_id`, `actor_id`, `occurred_at`이 유지되는가
