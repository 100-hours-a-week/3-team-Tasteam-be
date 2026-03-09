# API + 트랜잭션 메트릭/트레이싱 수집 기술 명세

## 개요

AOP 기반으로 컨트롤러(API 계층)와 `@Transactional` 메서드(트랜잭션 계층)의 성능 지표를 수집한다.
비즈니스 로직에 메트릭/트레이싱 코드를 0줄로 유지한다.

| 계층 | 수집 방식 | 목적 |
|---|---|---|
| **방법 1** Prometheus 메트릭 | `ApiMetricsAspect`, `TransactionMetricsAspect` | URI별 집계 통계 (P99 알림, 추세) |
| **방법 2** Distributed Tracing | `TransactionTracingAspect` | 특정 요청 1건의 실제 흐름 (폭포수) |

두 방법은 상호보완 관계:
- 방법 1로 "search API에서 뭔가 느리다" 감지 → 방법 2 traceId로 드릴다운

---

## 의존성

### Gradle (app-api/build.gradle)

```groovy
// Observability
implementation "org.springframework.boot:spring-boot-starter-actuator:3.5.9"  // MeterRegistry, ActuatorEndpoint
implementation 'io.micrometer:micrometer-registry-prometheus'                  // Prometheus 포맷 노출
// 분산 트레이싱 (TRACING_ENABLED=true + OTEL_ENDPOINT 설정 시 활성화)
implementation 'io.micrometer:micrometer-tracing-bridge-otel'                 // Micrometer → OpenTelemetry 브릿지
implementation 'io.opentelemetry:opentelemetry-exporter-otlp'                 // OTLP HTTP 익스포터
```

> `micrometer-core`와 `micrometer-observation`은 `spring-boot-starter-actuator`의 트랜지티브 의존성으로 자동 포함된다.

---

## application.yml 설정 블록

```yaml
## application.yml (실제 값 기준)

tasteam:
  aop:
    metrics:
      enabled: ${METRICS_AOP_ENABLED:true}           # 메트릭 AOP 전체 마스터 스위치
      api-metrics:
        enabled: ${METRICS_API_ENABLED:true}         # ApiMetricsAspect 개별 스위치
      transaction-metrics:
        enabled: ${METRICS_TX_ENABLED:true}          # TransactionMetricsAspect 개별 스위치
    tracing:
      enabled: ${TRACING_AOP_ENABLED:false}          # 트레이싱 AOP 전체 마스터 스위치
      transaction-tracing:
        enabled: ${TRACING_TX_ENABLED:false}         # TransactionTracingAspect 개별 스위치

management:
  metrics:
    distribution:
      percentiles-histogram:
        http.server.requests: true
        tasteam.api.request.duration: true
        tasteam.transaction.duration: true
        tasteam.transaction.query.count: true
    tags:
      application: tasteam-api
      environment: ${spring.profiles.active:local}   # 전역 태그 (모든 메트릭에 자동 추가)
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,caches        # /actuator/prometheus 활성화
  tracing:
    enabled: ${TRACING_ENABLED:false}
    sampling:
      probability: 0.1                               # 10% 샘플링 (기본값)
  otlp:
    tracing:
      endpoint: ${OTEL_ENDPOINT:http://localhost:4318/v1/traces}
```

---

## Aspect 상세 구현 명세

### ApiMetricsAspect

| 항목 | 값 |
|---|---|
| 클래스 | `global/aop/ApiMetricsAspect.java` |
| Pointcut | `execution(* com.tasteam.domain.*.controller.*.*(..))` |
| `@Order` | `10` |
| `@ConditionalOnProperty` prefix | `tasteam.aop.metrics` |
| `@ConditionalOnProperty` name | `["enabled", "api-metrics.enabled"]` |
| `havingValue` | `"true"` |

**동작 흐름**:
1. `Timer.start()` — 시작 시각 기록
2. `joinPoint.proceed()` — 컨트롤러 메서드 실행
3. 정상 반환 시: `resolveHttpOutcome()` 호출 → HTTP 응답 상태 코드로 outcome 결정
4. 예외 발생 시: outcome = `"server_error"` 고정
5. `finally`: Tags 빌드 → Timer/Counter 기록

**outcome 분류 로직**:

```
예외 발생         → "server_error"
HTTP status ≥ 500 → "server_error"
HTTP status ≥ 400 → "client_error"
그 외            → "success"
```

**uri 태그 fallback**:

| 상황 | 반환값 |
|---|---|
| HTTP 컨텍스트 없음 (`RequestContextHolder` null) | `"unknown"` |
| `HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE` null | `"unknown"` |
| 정상 | URI 템플릿 (예: `"/api/v1/search"`) |

---

### TransactionMetricsAspect

| 항목 | 값 |
|---|---|
| 클래스 | `global/aop/TransactionMetricsAspect.java` |
| Pointcut | `@annotation(transactional)` (`@Transactional` 메서드) |
| `@Order` | `10` |
| `@ConditionalOnProperty` prefix | `tasteam.aop.metrics` |
| `@ConditionalOnProperty` name | `["enabled", "transaction-metrics.enabled"]` |
| `havingValue` | `"true"` |

**동작 흐름**:
1. `SessionFactory.getStatistics().getPrepareStatementCount()` — 시작 쿼리 수 스냅샷
2. `Timer.start()` — 시작 시각 기록
3. `joinPoint.proceed()` — 서비스 메서드 실행 (`@Transactional` 프록시 포함)
4. 예외 발생 시: outcome = `"error"` 고정
5. `finally`: 쿼리 수 diff 계산 → Tags 빌드 → Timer/DistributionSummary/Counter 기록

**outcome 분류 로직**:

```
예외 발생 → "error"
정상 반환 → "success"
```

**uri 태그 fallback** (ApiMetricsAspect와 상이):

| 상황 | 반환값 |
|---|---|
| HTTP 컨텍스트 없음 (배치/비동기) | `"none"` |
| `HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE` null | `"unknown"` |
| 정상 | URI 템플릿 (예: `"/api/v1/search"`) |

---

### TransactionTracingAspect

| 항목 | 값 |
|---|---|
| 클래스 | `global/aop/TransactionTracingAspect.java` |
| Pointcut | `@annotation(transactional)` (`@Transactional` 메서드) |
| `@Order` | `9` (TransactionMetricsAspect=10보다 바깥) |
| `@ConditionalOnProperty` prefix | `tasteam.aop.tracing` |
| `@ConditionalOnProperty` name | `["enabled", "transaction-tracing.enabled"]` |
| `havingValue` | `"true"` |

**Span 구조**:

```
Span Name: "tasteam.tx <method_name>"
예: "tasteam.tx findRestaurants"

Attributes (lowCardinalityKeyValue):
  domain      = 패키지에서 추출한 도메인명 (예: "search")
  method_name = 서비스 메서드명 (예: "findRestaurants")
  read_only   = @Transactional(readOnly=...) 값 (예: "true")
```

> `lowCardinalityKeyValue`를 사용하므로 Span 태그가 추적 백엔드에서 저카디널리티 속성으로 인덱싱된다.

**예외 처리**: `observation.error(throwable)` 호출 → Tempo에서 에러 Span으로 표시

---

## 수집 메트릭 명세

### API 계층 — ApiMetricsAspect

| Java 이름 | Prometheus 노출 이름 | 유형 | 태그 |
|---|---|---|---|
| `tasteam.api.request.duration` | `tasteam_api_request_duration_seconds` | Histogram (Timer) | `method`, `uri`, `domain`, `outcome` |
| `tasteam.api.request.total` | `tasteam_api_request_total` | Counter | `method`, `uri`, `domain`, `outcome` |

> Prometheus 이름 변환 규칙: `.` → `_`, Timer에는 `_seconds` suffix 자동 추가.

**태그 가능 값**:

| 태그 | 가능한 값 |
|---|---|
| `method` | `GET`, `POST`, `PUT`, `DELETE`, `PATCH` 등 HTTP 메서드 |
| `uri` | URI 템플릿 (`/api/v1/search`), `"unknown"` (컨텍스트 없음/pattern null) |
| `domain` | `search`, `restaurant`, `member`, `review`, `group`, `auth`, ... (도메인명), `"unknown"` |
| `outcome` | `success`, `client_error` (4xx), `server_error` (5xx 또는 예외) |

**Prometheus에서 노출되는 실제 메트릭명** (예시):
```
tasteam_api_request_duration_seconds_bucket{le="0.005", method="GET", uri="/api/v1/search", ...}
tasteam_api_request_duration_seconds_count{...}
tasteam_api_request_duration_seconds_sum{...}
tasteam_api_request_total{...}
```

> `http.server.requests`(Spring 자동 수집)와 역할이 겹치나, `domain` 태그 추가가 목적.

### 트랜잭션 계층 — TransactionMetricsAspect

| Java 이름 | Prometheus 노출 이름 | 유형 | 태그 |
|---|---|---|---|
| `tasteam.transaction.duration` | `tasteam_transaction_duration_seconds` | Histogram (Timer) | `uri`, `domain`, `method_name`, `read_only`, `outcome` |
| `tasteam.transaction.query.count` | `tasteam_transaction_query_count` | Histogram (DistributionSummary) | 동일 |
| `tasteam.transaction.total` | `tasteam_transaction_total` | Counter | 동일 |

**태그 가능 값**:

| 태그 | 가능한 값 |
|---|---|
| `uri` | URI 템플릿, `"none"` (배치/비동기), `"unknown"` (pattern null) |
| `domain` | 도메인명 (패키지 추출), `"unknown"` |
| `method_name` | `@Transactional` 메서드명 (예: `findRestaurants`) |
| `read_only` | `"true"`, `"false"` |
| `outcome` | `success`, `error` (예외 발생) |

---

## AOP 적용 순서 (Order)

```
HTTP Request
  └─ [Filter] TraceIdFilter (MDC traceId 주입)
       └─ [Controller AOP] ApiMetricsAspect (Order=10)
            └─ [Controller Method]
                 └─ [Service AOP] TransactionTracingAspect (Order=9)  ← Span 먼저 열림
                      └─ [Service AOP] TransactionMetricsAspect (Order=10)
                           └─ [@Transactional Proxy] (Order=MAX_INT)
                                └─ [Service Method 비즈니스 로직]
```

`TransactionTracingAspect`가 `Order=9`로 바깥을 감싸 → 트레이스 컨텍스트가 열린 상태에서 메트릭이 기록된다.
두 TX Aspect 모두 `@Transactional` 프록시 외부에 위치 → 커밋 시간까지 포함한 전체 트랜잭션 시간 측정 가능.

---

## 카디널리티 제어 — MetricLabelPolicy

`global/metrics/MetricLabelPolicy.java`에서 허용/금지 라벨을 정적으로 관리한다.
**새 태그를 추가하려면 반드시 `ALLOWED_LABELS`에 등록해야 한다** (미등록 시 `IllegalArgumentException` 발생).

### 허용 라벨 전체 목록 (ALLOWED_LABELS)

| 라벨 키 | 사용 위치 |
|---|---|
| `environment` | 전역 태그 (application.yml) |
| `instance` | 전역 태그 |
| `result` | 범용 |
| `topic` | 메시지 큐 관련 |
| `provider` | 외부 서비스 구분 |
| `target` | 범용 |
| `reason` | 에러 분류 |
| `executor` | 비동기 실행기 구분 |
| `method` | HTTP 메서드 (ApiMetricsAspect) |
| `uri` | URI 템플릿 |
| `domain` | 도메인명 |
| `outcome` | 요청/트랜잭션 결과 |
| `read_only` | 읽기 전용 트랜잭션 여부 |
| `method_name` | 서비스 메서드명 |

### 금지 라벨 (FORBIDDEN_LABELS)

| 라벨 키 | 금지 이유 |
|---|---|
| `memberId` | 사용자 ID → 카디널리티 무제한 |
| `chatRoomId` | 채팅방 ID → 카디널리티 무제한 |
| `eventId` | 이벤트 ID → 카디널리티 무제한 |

---

## Distributed Tracing — TransactionTracingAspect

`ObservationRegistry`를 사용해 각 `@Transactional` 메서드를 Span으로 기록한다.
Grafana Tempo에서 폭포수 다이어그램으로 특정 요청의 트랜잭션 흐름 확인 가능:

```
GET /api/v1/search ──────────────────────── 500ms
  └─ SearchService.search ─────────────── 480ms
       ├─ [TX] findRestaurants ────────── 350ms  ← 병목
       └─ [TX] findKeywords ────────────  100ms
```

---

## trace-id 기반 필터링 전략

traceId는 **Prometheus 라벨로 사용하지 않는다** → Prometheus OOM 방지.
대신 Micrometer Exemplar로 히스토그램에 주입 → Grafana에서 이상치 점 클릭 시 Tempo 드릴다운.

---

## 환경변수

### 변수 전체 목록

| 변수명 | 기본값 | 설명 |
|---|---|---|
| `METRICS_AOP_ENABLED` | `true` | API/트랜잭션 메트릭 AOP 마스터 스위치 |
| `METRICS_API_ENABLED` | `true` | ApiMetricsAspect 개별 스위치 |
| `METRICS_TX_ENABLED` | `true` | TransactionMetricsAspect 개별 스위치 |
| `TRACING_AOP_ENABLED` | `false` | TransactionTracingAspect 마스터 스위치 (Tempo 필요) |
| `TRACING_TX_ENABLED` | `false` | TransactionTracingAspect 개별 스위치 |
| `TRACING_ENABLED` | `false` | Spring 트레이싱 전체 (OTLP export) |
| `OTEL_ENDPOINT` | `http://localhost:4318/v1/traces` | Tempo OTLP HTTP 엔드포인트 |

### 환경별 권장 설정

**로컬 직접 실행 (`./gradlew bootRun`) — .env.local에 추가**:
```bash
TRACING_AOP_ENABLED=true
TRACING_TX_ENABLED=true
TRACING_ENABLED=true
OTEL_ENDPOINT=http://localhost:4318/v1/traces   # Tempo가 호스트에서 직접 수신
```

**docker-compose 내 api 컨테이너 — .env.local에 추가**:
```bash
TRACING_AOP_ENABLED=true
TRACING_TX_ENABLED=true
TRACING_ENABLED=true
OTEL_ENDPOINT=http://tempo:4318/v1/traces       # 컨테이너 간 서비스 이름으로 접근
```

> 메트릭(Prometheus)은 기본값(`METRICS_AOP_ENABLED=true`)으로 항상 활성화된다. tracing만 opt-in.

---

## 구현 파일

| 파일 | 역할 |
|---|---|
| `global/aop/ApiMetricsAspect.java` | Controller API 메트릭 수집 |
| `global/aop/TransactionMetricsAspect.java` | 트랜잭션 메트릭 + 쿼리 수 수집 |
| `global/aop/TransactionTracingAspect.java` | 트랜잭션 Distributed Tracing Span |
| `global/metrics/MetricLabelPolicy.java` | 허용 라벨 화이트리스트 |

---

## 쿼리 카운트 정확도 한계

`tasteam.transaction.query.count`는 Hibernate `SessionFactory` 전역 통계 기반.
동시 요청 환경에서 ±N 오차 가능. 정확한 분석은 `pg_stat_statements` 사용.

---

## 검증 방법

```bash
# 1. 모니터링 스택 시작
docker-compose -f docker-compose.local.yml -f docker-compose.monitoring.yml up -d

# 2. 애플리케이션 실행 (트레이싱 활성화)
TRACING_AOP_ENABLED=true TRACING_TX_ENABLED=true TRACING_ENABLED=true \
OTEL_ENDPOINT=http://localhost:4318/v1/traces \
./gradlew bootRun --args='--spring.profiles.active=local'

# 3. 메트릭 확인 (tasteam_ prefix 필터)
curl localhost:8080/actuator/prometheus | grep tasteam_

# 4. 트랜잭션 메트릭 + uri 태그 확인
curl localhost:8080/actuator/prometheus | grep tasteam_transaction
# uri="/api/v1/search" 형태의 태그가 보이면 정상

# 5. Exemplar 포함 여부 확인
curl -H "Accept: application/openmetrics-text; version=1.0.0" \
  localhost:8080/actuator/prometheus | grep -A5 "tasteam_transaction_duration"
# # {traceID="abc123..."} 형태가 보이면 정상

# 6. Grafana 접속: http://localhost:3001
# Explore → Tempo → Search → 서비스명 tasteam-api 조회
```
