| 항목 | 내용 |
|---|---|
| 문서 제목 | 사용자 이벤트 수집 구현 추적성 문서 |
| 문서 목적 | 이슈/PR/커밋/코드/테스트를 한 문서에서 역추적 가능하게 정리하여, 변경 이력과 현재 구현 상태를 빠르게 검증할 수 있게 한다. |
| 작성 및 관리 | Backend Team |
| 최종 수정일 | 2026.02.20 |
| 문서 버전 | v1.0 |

<br>

# 사용자 이벤트 수집 추적성(Traceability)

---

# **[1] 타임라인 (Issue #350~#355 / PR #356~#362)**

## **[1-1] 이슈 타임라인**

| Issue | 제목 | 상태 | 종료 시각(UTC) |
|---|---|---|---|
| #350 | 사용자 이벤트 저장소 스키마 및 Outbox 마이그레이션 | CLOSED | 2026-02-18T11:25:57Z |
| #351 | Activity 포트/매퍼 레지스트리/오케스트레이터 구현 | CLOSED | 2026-02-19T07:32:01Z |
| #352 | USER_ACTIVITY MQ Publisher/Consumer 및 멱등 저장 구현 | CLOSED | 2026-02-19T08:39:36Z |
| #353 | 수집 장애 격리·재처리·운영 지표 구현 | CLOSED | 2026-02-19T08:47:16Z |
| #354 | PostHog Sink Dispatcher 및 재시도 정책 구현 | CLOSED | 2026-02-19T10:36:20Z |
| #355 | 클라이언트 이벤트 Ingest API(Allowlist/Rate Limit) 구현 | CLOSED | 2026-02-19T13:07:49Z |

## **[1-2] PR 타임라인**

| PR | 제목 | 연결 Issue | 머지 시각(UTC) | 브랜치 |
|---|---|---|---|---|
| #356 | 사용자 이벤트 저장소 스키마 및 Outbox 마이그레이션 추가 | #350 | 2026-02-18T11:25:56Z | `feat/#350/user-activity-schema-outbox` |
| #357 | Activity 매퍼 레지스트리 및 오케스트레이터 구현 | #351 | 2026-02-19T07:32:00Z | `feat/#351/activity-mapper-orchestrator` |
| #358 | USER_ACTIVITY MQ publisher/consumer 및 멱등 저장 구현 | #352 | 2026-02-19T08:39:34Z | `feat/#352/user-activity-mq-pipeline` |
| #359 | 사용자 이벤트 수집 장애 격리 및 재처리 운영 경로 구현 | #353 | 2026-02-19T08:47:15Z | `feat/#353/activity-resilience-replay-metrics` |
| #361 | PostHog Sink Dispatcher 및 재시도 정책 구현 | #354 | 2026-02-19T10:36:19Z | `feat/#354/posthog-sink-dispatcher` |
| #362 | 클라이언트 이벤트 Ingest API(Allowlist/Rate Limit) 구현 | #355 | 2026-02-19T13:07:48Z | `feat/#355/client-analytics-ingest-api` |

---

# **[2] 요구사항 대비 구현 매핑 (Issue -> Code/Test Evidence)**

## **[2-1] #350 스키마/아웃박스**

요구사항:
- `user_activity_event`, `user_activity_source_outbox`, `user_activity_dispatch_outbox` 마이그레이션
- 멱등 제약/운영 인덱스

구현 근거:
- `app-api/src/main/resources/db/migration/V202602181100__create_user_activity_event_tables.sql`

검증 근거:
- PR #356 본문 테스트 실행 기록

## **[2-2] #351 포트/매퍼/오케스트레이터**

요구사항:
- `ActivityEvent`, `ActivitySink`, `ActivityEventMapper<T>`
- registry/orchestrator
- Review/Group 가입 매퍼

구현 근거:
- `app-api/src/main/java/com/tasteam/domain/analytics/api/ActivityEvent.java`
- `app-api/src/main/java/com/tasteam/domain/analytics/api/ActivitySink.java`
- `app-api/src/main/java/com/tasteam/domain/analytics/api/ActivityEventMapper.java`
- `app-api/src/main/java/com/tasteam/domain/analytics/application/ActivityEventMapperRegistry.java`
- `app-api/src/main/java/com/tasteam/domain/analytics/application/ActivityEventOrchestrator.java`
- `app-api/src/main/java/com/tasteam/domain/analytics/application/mapper/ReviewCreatedActivityEventMapper.java`
- `app-api/src/main/java/com/tasteam/domain/analytics/application/mapper/GroupMemberJoinedActivityEventMapper.java`

검증 근거:
- `app-api/src/test/java/com/tasteam/domain/analytics/application/ActivityEventMapperRegistryTest.java`
- `app-api/src/test/java/com/tasteam/domain/analytics/application/ActivityEventOrchestratorTest.java`
- `app-api/src/test/java/com/tasteam/domain/analytics/application/mapper/ReviewCreatedActivityEventMapperTest.java`
- `app-api/src/test/java/com/tasteam/domain/analytics/application/mapper/GroupMemberJoinedActivityEventMapperTest.java`

## **[2-3] #352 MQ 파이프라인 + 멱등 저장**

요구사항:
- `domain.user.activity` 토픽
- publisher/consumer
- `ON CONFLICT(event_id) DO NOTHING`

구현 근거:
- `app-api/src/main/java/com/tasteam/infra/messagequeue/MessageQueueTopics.java`
- `app-api/src/main/java/com/tasteam/infra/messagequeue/UserActivityMessageQueuePublisher.java`
- `app-api/src/main/java/com/tasteam/infra/messagequeue/UserActivityMessageQueueConsumerRegistrar.java`
- `app-api/src/main/java/com/tasteam/domain/analytics/persistence/UserActivityEventJdbcRepository.java`

검증 근거:
- `app-api/src/test/java/com/tasteam/infra/messagequeue/UserActivityMessageQueueFlowIntegrationTest.java`
- `app-api/src/test/java/com/tasteam/infra/messagequeue/UserActivityMessageQueuePublisherTest.java`
- `app-api/src/test/java/com/tasteam/infra/messagequeue/UserActivityMessageQueueConsumerRegistrarTest.java`
- `app-api/src/test/java/com/tasteam/domain/analytics/persistence/UserActivityEventJdbcRepositoryTest.java`

## **[2-4] #353 장애 격리/재처리/운영 지표**

요구사항:
- source outbox 기반 격리
- 재처리 service/scheduler/API
- 운영 지표 보강

구현 근거:
- `app-api/src/main/java/com/tasteam/infra/messagequeue/UserActivitySourceOutboxSink.java`
- `app-api/src/main/java/com/tasteam/domain/analytics/resilience/UserActivitySourceOutboxService.java`
- `app-api/src/main/java/com/tasteam/domain/analytics/resilience/UserActivityReplayService.java`
- `app-api/src/main/java/com/tasteam/infra/messagequeue/UserActivityReplayScheduler.java`
- `app-api/src/main/java/com/tasteam/infra/messagequeue/UserActivityOutboxAdminController.java`

검증 근거:
- `app-api/src/test/java/com/tasteam/domain/analytics/resilience/UserActivityReplayServiceTest.java`
- `app-api/src/test/java/com/tasteam/infra/messagequeue/UserActivityOutboxAdminControllerTest.java`
- `app-api/src/test/java/com/tasteam/infra/messagequeue/UserActivitySourceOutboxSinkTest.java`

## **[2-5] #354 PostHog dispatch + retry/circuit**

요구사항:
- PostHog sink/client
- dispatch outbox dispatcher
- retry/backoff/circuit
- 내부 저장과 외부 전송 분리

구현 근거:
- `app-api/src/main/java/com/tasteam/domain/analytics/persistence/UserActivityStoredHook.java`
- `app-api/src/main/java/com/tasteam/infra/analytics/posthog/UserActivityDispatchOutboxEnqueueHook.java`
- `app-api/src/main/java/com/tasteam/domain/analytics/dispatch/UserActivityDispatchOutboxDispatcher.java`
- `app-api/src/main/java/com/tasteam/domain/analytics/dispatch/UserActivityDispatchCircuitBreaker.java`
- `app-api/src/main/java/com/tasteam/infra/analytics/posthog/PosthogClient.java`
- `app-api/src/main/java/com/tasteam/infra/analytics/posthog/PosthogSink.java`

검증 근거:
- `app-api/src/test/java/com/tasteam/domain/analytics/dispatch/UserActivityDispatchOutboxDispatcherTest.java`
- `app-api/src/test/java/com/tasteam/domain/analytics/dispatch/UserActivityDispatchCircuitBreakerTest.java`
- `app-api/src/test/java/com/tasteam/infra/analytics/posthog/PosthogClientTest.java`
- `app-api/src/test/java/com/tasteam/domain/analytics/persistence/UserActivityEventStoreServiceTest.java`

## **[2-6] #355 클라이언트 ingest API**

요구사항:
- `POST /api/v1/analytics/events`
- allowlist / batch / rate limit
- 인증/익명 정책
- 동일 저장 경로 사용

구현 근거:
- `app-api/src/main/java/com/tasteam/domain/analytics/ingest/ClientActivityIngestController.java`
- `app-api/src/main/java/com/tasteam/domain/analytics/ingest/ClientActivityIngestService.java`
- `app-api/src/main/java/com/tasteam/domain/analytics/ingest/ClientActivityIngestRateLimiter.java`
- `app-api/src/main/java/com/tasteam/global/security/common/constants/ApiEndpoints.java`
- `app-api/src/main/java/com/tasteam/global/security/common/constants/ApiEndpointSecurityPolicy.java`

검증 근거:
- `app-api/src/test/java/com/tasteam/domain/analytics/ingest/ClientActivityIngestServiceTest.java`
- `app-api/src/test/java/com/tasteam/domain/analytics/ingest/ClientActivityIngestRateLimiterTest.java`
- `app-api/src/test/java/com/tasteam/domain/analytics/ingest/ClientActivityIngestControllerTest.java`

---

# **[3] 커밋 분할 이력 요약 (feat/test/fix)**

## **[3-1] 주요 feat 커밋**

- `377bac0` `feat(analytics): 사용자 이벤트 저장소 및 아웃박스 스키마 추가`
- `84da1f8` `feat(analytics): 활동 이벤트 매퍼 레지스트리와 오케스트레이터 추가`
- `0fc7196` `feat(analytics): USER_ACTIVITY MQ 수집 파이프라인 구현`
- `c7af35d` `feat(analytics): 수집 장애 격리 및 outbox 재처리 경로 추가`
- `6b7f09f` `feat(analytics): dispatch outbox 디스패처 핵심 구조 추가`
- `736ebc6` `feat(analytics): PostHog sink 및 전송 클라이언트 연동`
- `a2e3d73` `feat(analytics): 클라이언트 이벤트 ingest API 및 정책 추가`

## **[3-2] 주요 test 커밋**

- `46f6e35` `test(analytics): dispatch 재시도·서킷 및 PostHog 요청 검증 추가`
- `ac594f2` `test(analytics): ingest allowlist·레이트리밋 정책 테스트 추가`

## **[3-3] 주요 fix 커밋**

- `1614f37` `fix(analytics): MQ 비활성 환경에서 재처리 빈 로딩 실패 수정`
- `f35acc6` `fix(analytics): ingest 레이트리미터 생성자 주입 대상을 명시`

---

# **[4] 핵심 클래스 변경 이력 (Class-level Change Highlights)**

| 클래스 | 변경 의도 | 연결 이슈 |
|---|---|---|
| `ActivityEventOrchestrator` | 매핑/전달 중심 오케스트레이션 + sink 예외 격리 | #351 |
| `UserActivityMessageQueuePublisher` | MQ publish + source outbox 상태 갱신 | #352, #353 |
| `UserActivityEventStoreService` | 멱등 저장 + 저장 후 hook 확장점 | #352, #354 |
| `UserActivityReplayService` | source outbox 재처리 경로 | #353 |
| `UserActivityDispatchOutboxDispatcher` | dispatch 후보 처리 + retry + circuit 연동 | #354 |
| `ClientActivityIngestService` | allowlist/배치/식별/rate-limit 정책 | #355 |

---

# **[5] 테스트 근거 목록 (What Each Test Guarantees)**

## **[5-1] 서버 이벤트/MQ/저장 경로**

- `UserActivityMessageQueueFlowIntegrationTest`
  - 도메인 이벤트 발행 시 USER_ACTIVITY 토픽 발행 및 소비 저장 경로 연계 검증
- `UserActivityEventJdbcRepositoryTest`
  - `event_id` 멱등 저장 동작 검증

## **[5-2] 장애 격리/재처리**

- `UserActivityReplayServiceTest`
  - provider none skip
  - payload 역직렬화 실패 시 failed 마킹
  - 재발행 성공/실패 카운트 검증
- `UserActivityOutboxAdminControllerTest`
  - outbox 요약/재처리 API 반환값 검증

## **[5-3] dispatch/PostHog**

- `UserActivityDispatchOutboxDispatcherTest`
  - 정상 dispatch
  - payload 손상 실패 경로
  - sink 연속 실패 시 circuit open
- `UserActivityDispatchCircuitBreakerTest`
  - 임계치/열림기간 동작 검증
- `PosthogClientTest`
  - `distinct_id = event_id` 계약 검증

## **[5-4] 클라이언트 ingest 정책**

- `ClientActivityIngestServiceTest`
  - allowlist, batch size, anonymous 필수, rate-limit 초과 동작 검증
- `ClientActivityIngestRateLimiterTest`
  - 윈도우 내 차단/윈도우 경과 후 회복 검증
- `ClientActivityIngestControllerTest`
  - request/header anonymousId 해석 우선순위 검증

---

# **[6] 갭 리포트 (Spec/Migration/Code Mismatch)**

## **[6-1] 필수 갭 항목**

| ID | 항목 | 현재 상태 | 영향도 | 우선순위 |
|---|---|---|---|---|
| GAP-01 | `user_activity_dispatch_outbox` 컬럼명 불일치 | migration은 `sink_type`, 코드 SQL은 `dispatch_target` | 높음 (dispatch 쿼리 실패 가능) | P0 |
| GAP-02 | dispatch 상태명 불일치 | migration 주석 `SENT`, 코드 enum `DISPATCHED` | 중간 (운영 혼선/대시보드 불일치) | P1 |
| GAP-03 | source 분류 저장 불일치 가능성 | client ingest는 `properties.source=CLIENT`, DB `source` 컬럼은 저장 로직에서 `SERVER` 고정 | 중간 (분석 왜곡) | P1 |
| GAP-04 | 설정 문서 누락 가능성 | `tasteam.analytics.outbox.*`, `tasteam.analytics.replay.*`가 코드에서 사용되나 application.yml에 명시적 블록 없음 | 중간 (운영 설정 누락 위험) | P1 |
| GAP-05 | 이벤트 카탈로그 불일치 | 스펙의 `search.executed`는 매퍼 미구현 | 낮음~중간 (기대 기능 미충족) | P2 |

## **[6-2] 근거 파일**

- GAP-01, GAP-02
  - `app-api/src/main/resources/db/migration/V202602181100__create_user_activity_event_tables.sql`
  - `app-api/src/main/java/com/tasteam/domain/analytics/dispatch/UserActivityDispatchOutboxJdbcRepository.java`
  - `app-api/src/main/java/com/tasteam/domain/analytics/dispatch/UserActivityDispatchOutboxStatus.java`
- GAP-03
  - `app-api/src/main/java/com/tasteam/domain/analytics/ingest/ClientActivityIngestService.java`
  - `app-api/src/main/java/com/tasteam/domain/analytics/persistence/UserActivityEventJdbcRepository.java`
- GAP-04
  - `app-api/src/main/java/com/tasteam/infra/messagequeue/UserActivitySourceOutboxSink.java`
  - `app-api/src/main/java/com/tasteam/infra/messagequeue/UserActivityReplayScheduler.java`
  - `app-api/src/main/resources/application.yml`
- GAP-05
  - `docs/spec/tech/user-activity/README.md` (기대 카탈로그)
  - `app-api/src/main/java/com/tasteam/domain/analytics/application/mapper/*`

---

# **[7] 후속 개선 백로그 (Documentation TODO)**

1. Dispatch outbox 스키마/코드 컬럼명 정합성 수정 이슈 생성
2. Dispatch 상태명(`SENT` vs `DISPATCHED`) 용어 통일 이슈 생성
3. `user_activity_event.source` 컬럼 저장 규칙 개선 이슈 생성
4. outbox/replay 설정 키를 `application.yml` 예시 블록으로 문서화 이슈 생성
5. `search.executed` 매퍼/발행 경로 구현 이슈 생성
6. 운영 대시보드(적체/실패/서킷) 표준 패널 정의 이슈 생성

---

# **[8] 명시적 가정 및 기본값**

1. 본 문서는 코드와 GitHub 이슈/PR/커밋을 기준으로 작성했다.
2. 채팅 이력 기반 의사결정은 Issue/PR 본문과 커밋 메시지로 대체 추적했다.
3. 본 문서 범위는 문서화와 갭 식별이며, 코드/마이그레이션 수정은 포함하지 않는다.
4. 분석 기준 브랜치는 `develop` 머지 결과(최신 머지: PR #362)이다.
