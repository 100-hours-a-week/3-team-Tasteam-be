# 사용자 이벤트 수집 완결성 개선

## 1. 목적

server-side domain event와 client ingest가 최종 저장소(`user_activity_event`) 기준으로 같은 완결성을 갖도록 환경 정책과 fallback 전략을 정리한다.

## 2. 현재 구현 근거

| 위치 | 현재 구현 | 완결성 관점 의미 |
|---|---|---|
| `application.yml` | `tasteam.message-queue.enabled=false`, `provider=none` 기본값 | MQ 경로는 기본 비활성 |
| `ActivityDomainEventListener` | `AFTER_COMMIT`에 도메인 이벤트 전달 | 후속 단계 실패가 request 성공과 분리 |
| `ActivityEventOrchestrator` | mapper -> `ActivitySink` 순회, 예외는 로그 후 계속 | silent gap 가능 |
| `UserActivitySourceOutboxSink` | 기본 활성 | domain event는 우선 source outbox로 수렴 |
| `UserActivityMessageQueuePublisher` | MQ on일 때만 활성 | MQ off면 최종 저장까지 가지 않을 수 있음 |
| `UserActivityMessageQueueConsumerRegistrar` | MQ 소비 후 `UserActivityEventStoreService.store()` | final store 도달 핵심 경로 |
| `ClientActivityIngestService` | `UserActivityEventStoreService`로 direct store | client ingest는 final store 기준 완결 |

## 3. 왜 문제인가

- client ingest는 저장 완료가 명확하지만 server-side domain event는 source outbox에만 남고 끝날 수 있다.
- 개발/스테이징에서 MQ 없이 기동하면 리뷰 생성, 그룹 가입 같은 사실 이벤트가 최종 분석 저장소에 반영되지 않을 수 있다.
- 서버 사실 이벤트가 빠지면 추천/분석 데이터셋이 편향된다.

## 4. 실제 데이터 갭 시나리오

### 시나리오 A. MQ off 개발 환경

1. 리뷰 생성 성공
2. `ReviewCreatedEvent`가 `ActivityDomainEventListener`로 전달
3. `UserActivitySourceOutboxSink`가 `user_activity_source_outbox` 적재
4. MQ publisher / consumer는 비활성
5. `user_activity_event`에는 row가 없음

### 시나리오 B. sink 예외 흡수

1. mapper 또는 sink 예외 발생
2. `ActivityEventOrchestrator`가 로그만 남김
3. API는 성공 응답
4. 데이터셋 차이로 늦게 발견

## 5. 목표 상태

- client ingest와 server-side domain event가 모두 최종 저장소 기준으로 완결성 확보
- local/test와 dev/stg/prod의 허용 정책을 분리
- backlog, pending age, completeness delta를 언제나 볼 수 있음

## 6. 권장 정책

| 환경 | MQ | 권장 동작 |
|---|---|---|
| local | optional | direct final-store fallback 허용 |
| test | optional | direct final-store fallback 허용 |
| dev | required | 없으면 startup fail |
| stg | required | 없으면 startup fail |
| prod | required | 없으면 startup fail |

## 7. 개선 방향

- local/test에서는 server-side domain event를 `UserActivityEventStoreService`로 직접 저장하는 fallback 제공
- dev/stg/prod에서는 MQ 없는 기동을 차단하는 startup validation 추가
- source outbox backlog와 final store write count를 MQ on/off와 무관하게 조회 가능하게 유지
- completeness metric 추가

## 8. 구현 순서

1. 환경 정책 문서/코드 고정
2. local/test fallback sink 추가
3. dev/stg/prod startup fail-fast 추가
4. outbox admin summary 조건부 제거
5. completeness metric / alert 추가

## 9. 검증 포인트

- local/test에서 MQ off 시 domain event가 final store에 남는가
- dev/stg/prod에서 MQ off 시 startup이 차단되는가
- source outbox backlog가 admin API에서 항상 보이는가
