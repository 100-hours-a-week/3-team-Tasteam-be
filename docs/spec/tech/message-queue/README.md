| 항목 | 내용 |
|---|---|
| 문서 제목 | 메시지 큐(Message Queue) 모듈 테크 스펙 |
| 문서 목적 | 메시지 큐 모듈의 경계, 구성 요소, 실행 흐름, 확장 전략(Redis Stream -> Kafka)을 정의한다. |
| 작성 및 관리 | Backend Team |
| 최초 작성일 | 2026.02.14 |
| 최종 수정일 | 2026.02.14 |
| 문서 버전 | v0.1 |

<br>

# 메시지 큐(Message Queue) - BE 테크스펙

---

# **[1] 배경 (Background)**

## **[1-1] 목표 (Objective)**

- 애플리케이션 계층이 특정 MQ 인프라 구현에 직접 결합되지 않도록 추상 경계를 고정한다.
- 초기에는 Redis Stream 기반 구현을 붙이고, 이후 Kafka로 전환/병행 가능하도록 모듈 구조를 선행한다.
- 모듈 계약(Producer/Consumer/Message/Subscription)을 팀 공통 언어로 문서화한다.

## **[1-2] 현재 상태 (As-Is)**

- `MessageQueueProducer`, `MessageQueueConsumer` 인터페이스와 메시지/구독 계약이 준비되어 있다.
- `tasteam.message-queue.provider` 값에 따라 Bean 선택이 동작한다.
- `none`은 NoOp 구현으로 안전하게 동작한다.
- `redis-stream`은 Producer/Consumer 구현이 반영되어 발행/구독이 가능하다.
- `kafka`는 현재 `Unsupported*` 구현으로 명시적 예외를 반환한다.
- `ReviewCreatedEvent`는 도메인 이벤트 -> MQ 발행 -> MQ 소비 핸들러 처리 흐름으로 연결되어 있다.

---

# **[2] 모듈 구성 (Module Structure)**

## **[2-1] 패키지 구성**

- 경로: `app-api/src/main/java/com/tasteam/infra/messagequeue`
- 핵심 파일
  - 계약: `MessageQueueProducer`, `MessageQueueConsumer`, `QueueMessageHandler`
  - 데이터 계약: `QueueMessage`, `MessageQueueSubscription`
  - 설정: `MessageQueueConfig`, `MessageQueueProperties`, `MessageQueueProviderType`
  - 구현체: `NoOp*`, `Unsupported*`

## **[2-2] 아키텍처 다이어그램**

```mermaid
flowchart LR
    A["Application Services"] --> B["MessageQueueProducer (Interface)"]
    C["Application Consumers"] --> D["MessageQueueConsumer (Interface)"]

    E["MessageQueueConfig"] --> F["Provider Selection"]
    F -->|"none"| G["NoOpMessageQueueProducer/Consumer"]
    F -->|"redis-stream"| H["RedisStreamMessageQueueProducer/Consumer"]
    F -->|"kafka"| I["UnsupportedMessageQueueProducer/Consumer"]

    B --> E
    D --> E
    J["QueueMessage / MessageQueueSubscription"] --> B
    J --> D
```

## **[2-3] 계약 다이어그램**

```mermaid
classDiagram
    class MessageQueueProducer {
      +publish(message)
      +publish(topic, key, payload)
    }

    class MessageQueueConsumer {
      +subscribe(subscription, handler)
      +unsubscribe(subscription)
    }

    class QueueMessageHandler {
      +handle(message)
    }

    class QueueMessage {
      +topic
      +key
      +payload
      +headers
      +occurredAt
      +messageId
    }

    class MessageQueueSubscription {
      +topic
      +consumerGroup
      +consumerName
    }

    MessageQueueConsumer --> QueueMessageHandler
    MessageQueueProducer --> QueueMessage
    MessageQueueConsumer --> MessageQueueSubscription
```

---

# **[3] 실행 흐름 (Runtime Flow)**

## **[3-1] 설정 기반 구현체 선택**

```mermaid
sequenceDiagram
    autonumber
    participant Boot as Spring Boot
    participant Props as MessageQueueProperties
    participant Config as MessageQueueConfig
    participant Bean as MQ Bean

    Boot->>Props: bind(tasteam.message-queue.*)
    Boot->>Config: create producer/consumer bean
    Config->>Props: providerType()
    alt provider = none
      Config-->>Bean: NoOp Producer/Consumer
    else provider = redis-stream or kafka
      Config-->>Bean: Unsupported Producer/Consumer
    end
```

## **[3-2] 메시지 발행/소비 개념 흐름**

```mermaid
flowchart LR
    A["Domain Event/UseCase"] --> B["MessageQueueProducer.publish(...)"]
    B --> C["Provider Implementation"]
    C --> D["MQ Infra (Redis Stream/Kafka)"]
    D --> E["Provider Consumer"]
    E --> F["QueueMessageHandler.handle(...)"]
    F --> G["Application Logic"]
```

---

# **[4] 설정 계약 (Configuration Contract)**

`application.yml`

- `tasteam.message-queue.enabled`
- `tasteam.message-queue.provider` (`none` | `redis-stream` | `kafka`)
- `tasteam.message-queue.topic-prefix`
- `tasteam.message-queue.default-consumer-group`
- `tasteam.message-queue.poll-timeout-millis`
- `tasteam.message-queue.max-retries`

운영 기본값은 `provider=none`으로 두고, 구현체가 준비된 환경에서만 provider를 활성화한다.

## **[4-1] 공용 변수 vs 도메인 변수**

- 공용(플랫폼) 변수는 인프라 접속/활성화에만 사용한다.
  - `MQ_ENABLED`, `MQ_PROVIDER`, `MQ_TOPIC_PREFIX`, `MQ_DEFAULT_CONSUMER_GROUP`
  - `KAFKA_BOOTSTRAP_SERVERS`, `KAFKA_CONNECT_URL`
- 도메인 전용 변수는 접두사로 용도를 명확히 분리한다.
  - 이벤트 로그 적재: `ANALYTICS_EVENT_LOG_*`
  - 알림: `NOTIFICATION_MQ_*` (또는 `NOTIFICATION_KAFKA_*`)
  - 유저활동: `USER_ACTIVITY_MQ_*`
- 금지 예시:
  - `KAFKA_TOPIC`, `CONSUMER_GROUP`처럼 도메인 소유가 드러나지 않는 변수명

## **[4-2] 로컬 실행 오버레이**

- 기본 로컬 스택: `docker-compose.local.yml`
- Kafka/Connect 확장 스택: `docker-compose.kafka.yml`
- 실행 예시:
  - `docker compose -f docker-compose.local.yml -f docker-compose.kafka.yml up -d`
  - `docker compose -f docker-compose.local.yml -f docker-compose.kafka.yml down`

오버레이 분리를 통해 기본 로컬 개발 흐름(DB/Redis/API)과 선택 스택(Kafka/Connect, Monitoring)의 변경 영향을 분리한다.

## **[4-3] Kafka 실패 처리 정책 (Foundation)**

foundation 단계에서 Kafka consumer 실패 처리는 공통 `DefaultErrorHandler`로 통일한다.

| 실패 유형 | 예외 타입 | 재시도 | 최종 처리 |
|---|---|---|---|
| 입력/포맷 불량 | `MessageQueueNonRetryableException` | 하지 않음 | 즉시 DLQ 전송 |
| 역직렬화/직렬화 실패 | `DeserializationException`, `SerializationException` | 하지 않음 | 즉시 DLQ 전송 |
| 일시적 인프라/네트워크 실패 | 기타 런타임 예외 | 설정된 횟수만큼 재시도 | 재시도 초과 시 DLQ 전송 |

- 재시도 기본값:
  - `KAFKA_CONSUMER_RETRY_MAX_ATTEMPTS=3`
  - `KAFKA_CONSUMER_RETRY_BACKOFF_MILLIS=1000`
- DLQ 토픽 규칙:
  - 도메인별 설정 존재 시 해당 DLQ 토픽 사용
  - 미설정 시 `<source-topic>.dlq`

---

# **[5] 확장 전략 (Redis Stream -> Kafka)**

## **[5-1] 확장 원칙**

- 애플리케이션은 인터페이스만 의존하고 구현체를 직접 참조하지 않는다.
- provider별 구현 추가는 `MessageQueueConfig`의 선택 분기와 구현체 클래스 추가로 한정한다.
- 메시지 계약(`QueueMessage`, `MessageQueueSubscription`)은 하위 호환을 우선한다.

## **[5-2] 단계**

1. Redis Stream Producer/Consumer 구현 추가
2. 통합 테스트 추가(발행/구독/재시도)
3. Kafka Producer/Consumer 구현 추가
4. provider 전환 검증(동일 애플리케이션 코드로 설정만 변경)

---

# **[6] 체크리스트 (Review Checklist)**

- 인터페이스/메서드 주석이 계약을 명확히 설명하는가
- 구현체 교체 시 애플리케이션 코드 변경이 최소화되는가
- provider 설정 변경이 빈 선택 결과와 일치하는가
- 테스트가 계약/설정 분기를 검증하는가

---

# **[7] 구현 현황 (Implementation Status)**

## **[7-1] Phase 1 완료 범위**

- 메시지큐 추상 계약(Producer/Consumer/Message/Subscription) 반영 완료
- Redis Stream provider 분기 및 Producer/Consumer 1차 구현 완료
- MQ 전용 Stream listener container 설정 추가
- 인터페이스 계약 Javadoc 반영
- MQ 모듈 테스트(설정/producer/consumer) 추가

## **[7-2] Phase 2 진행 범위 (#330)**

- 도메인 이벤트 발행 지점 연동:
  - `ReviewCreatedEvent` 수신 시 MQ 토픽(`domain.review.created`)으로 발행
- MQ consumer 핸들러 등록/수신 처리:
  - 애플리케이션 시작 시 `ReviewCreated` 구독 등록
  - 수신 payload 역직렬화 후 `RestaurantReviewAnalysisService`로 위임
- 실행 경로 정합성:
  - MQ 활성화 시 기존 `ReviewCreatedAiAnalysisEventListener` 직접 경로 비활성화
  - MQ 비활성화 시 기존 직접 경로 유지
## **[7-3] 검증**

- `./gradlew :app-api:test --tests 'com.tasteam.infra.messagequeue.*'` 통과
- 컨텍스트 로딩 회귀 검증:
  - `./gradlew :app-api:test --tests com.tasteam.ApiApplicationTests --tests com.tasteam.config.JpaAuditingConflictTest` 통과

## **[7-4] 다음 단계**

- Group/Restaurant 도메인 이벤트 MQ 연동 확장
- 실제 구독 라이프사이클/재시도/DLQ 정책 고도화
- Kafka provider 구현 단계 진행

---

# **[8] 운영 추적 (Observability)**

## **[8-1] 발행/소비 추적 로그**

- 발행 시 `messageId` 기준 로그를 남긴다.
  - `메시지큐 발행 완료. stream, topic, messageId, key`
- 소비 시 시작/성공/실패 로그를 남긴다.
  - `메시지큐 수신 처리 시작. stream, topic, messageId, consumerGroup`
  - `메시지큐 수신 처리 성공. ... processingMillis`
  - `메시지큐 수신 처리 실패. ... processingMillis`

## **[8-2] 추적 이력 저장소**

- 테이블: `message_queue_trace_log`
- 저장 항목:
  - `message_id`, `topic`, `provider`, `stage(PUBLISH/CONSUME_SUCCESS/CONSUME_FAIL)`
  - `consumer_group`, `processing_millis`, `error_message`, `created_at`
- 목적:
  - 이벤트 생성/처리 여부를 로그가 아닌 데이터로 조회 가능
  - 운영 중 장애/누락 구간 식별

## **[8-3] 운영 조회 API**

- 경로: `GET /api/v1/admin/mq-traces`
- 권한: `ADMIN`
- 쿼리 파라미터:
  - `messageId`(optional): 특정 메시지 이력 조회
  - `limit`(optional, default `50`, max `200`)

## **[8-4] 메트릭 수집 아키텍처**

메트릭 수집은 AOP로 비즈니스 코드에서 완전히 분리되어 있다.
전체 메트릭 카탈로그, 수집 방식 결정 가이드, Aspect/Collector 설계 상세는 아래 문서를 참조한다.

→ **[비동기 이벤트 아키텍처 & AOP 메트릭 수집 설계](./async-observability.md)**

### 핵심 MQ 메트릭 요약

| 메트릭 | 타입 | 라벨 | 수집 주체 |
|---|---|---|---|
| `mq.publish.count` | Counter | `topic`, `provider`, `result` | `MessageQueueMetricsAspect` |
| `mq.consume.count` | Counter | `topic`, `provider`, `result` | `MessageQueueMetricsAspect` |
| `mq.consume.latency` | Timer | `topic`, `provider` | `MessageQueueMetricsAspect` |
| `mq.end_to_end.latency` | Timer | `topic`, `provider`, `result` | `MessageQueueMetricsAspect` |
| `notification.consumer.process` | Counter | `result` | `MessageQueueMetricsAspect` |
| `notification.consumer.dlq` | Counter | `result` | `MessageQueueMetricsAspect` |

## **[8-5] 관심사 분리**

- `MessageQueueTraceService`: DB 저장 전담 — `io.micrometer` import 없음
- `TracingMessageQueueProducer` / `TracingMessageQueueConsumer`: Decorator 패턴으로 TraceService 호출
- `MessageQueueMetricsAspect`: TraceService `record*()` 메서드에 `@AfterReturning`으로 메트릭 수집
- 비즈니스 클래스(`NotificationMessageProcessor`, `NotificationDlqPublisher`)는 마커 어노테이션만 부착

---

# **[9] 환경별 운영 모델**

- `local`
  - Docker 오버레이로 Kafka/Connect를 선택 실행
  - 앱은 기본 `MQ_ENABLED=false`, 필요 시에만 `provider` 활성화
- `dev`
  - CodeDeploy `docker-compose.dev-infra.yml`에서 DB/Redis + (선택) Kafka/Connect 기동
  - `MQ_ENABLED=true` + `MQ_PROVIDER=kafka`인 경우 `KAFKA_BOOTSTRAP_SERVERS` 필수
- `stg`/`prod`
  - 앱 컨테이너 배포와 Kafka 인프라를 분리 운영
  - Kafka/Connect는 managed 혹은 별도 운영 스택으로 관리

---

# **[10] Foundation 온보딩 가이드**

## **[10-1] foundation에서 제공하는 것**

- Kafka 공통 설정 빈
  - `ProducerFactory`, `KafkaTemplate`
  - `ConsumerFactory`, `ConcurrentKafkaListenerContainerFactory`
- 메시지 직렬화 공통 인터페이스 및 JSON 기본 구현
- 공통 에러핸들러 + 재시도/백오프 기본 정책
- DLQ 토픽 네이밍 정책 인터페이스 및 기본 구현

## **[10-2] Producer PR에서 해야 할 일**

- 도메인 payload를 `QueueMessage`로 매핑
- 공통 serializer/`KafkaPublishSupport`를 이용해 발행
- 발행 실패 시 `MessageQueuePublishException` 흐름으로 운영 로그 연결

## **[10-3] Consumer PR에서 해야 할 일**

- Listener 등록과 payload 역직렬화 연결
- 재시도 가능/불가 예외를 명확히 분리
- DLQ 토픽 규칙(`TopicNamingPolicy`)을 따르는지 검증
