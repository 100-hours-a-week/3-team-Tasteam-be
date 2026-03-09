# API + 트랜잭션 메트릭/트레이싱 수집 기술 명세

## 개요

AOP 기반으로 컨트롤러(API 계층)와 `@Transactional` 메서드(트랜잭션 계층)의 성능 지표를 수집한다.
비즈니스 로직에 메트릭/트레이싱 코드를 0줄로 유지한다.

| 계층 | 수집 방식 | 목적 |
|---|---|---|
| **방법 1** Prometheus 메트릭 | `TransactionMetricsAspect` | URI별 트랜잭션 집계 통계 (P99 알림, 추세) |
| **방법 2** Distributed Tracing | `TransactionTracingAspect` | 특정 요청 1건의 실제 흐름 (폭포수) |

두 방법은 상호보완 관계:
- 방법 1로 "search API에서 뭔가 느리다" 감지 → 방법 2 traceId로 드릴다운

---

## 수집 메트릭 명세

### API 계층 (Controller) — `ApiMetricsAspect`

| 메트릭명 | 유형 | 태그 |
|---|---|---|
| `tasteam.api.request.duration` | Timer | `method`, `uri`, `domain`, `outcome` |
| `tasteam.api.request.total` | Counter | 동일 |

> `http.server.requests`(Spring 자동 수집)와 역할이 겹치나, `domain` 태그 추가가 목적.

### 트랜잭션 계층 (@Transactional) — `TransactionMetricsAspect`

| 메트릭명 | 유형 | 태그 |
|---|---|---|
| `tasteam.transaction.duration` | Timer | `uri`, `domain`, `method_name`, `read_only`, `outcome` |
| `tasteam.transaction.query.count` | DistributionSummary | 동일 |
| `tasteam.transaction.total` | Counter | 동일 |

**`uri` 태그**: `RequestContextHolder`에서 정규화된 URI 템플릿 추출.
배치/비동기 컨텍스트(HTTP 요청 없음)에서는 `none` 으로 기록.

---

## Distributed Tracing — `TransactionTracingAspect`

`ObservationRegistry`를 사용해 각 `@Transactional` 메서드를 Span으로 기록한다.
Grafana Tempo에서 폭포수 다이어그램으로 특정 요청의 트랜잭션 흐름 확인 가능:

```
GET /api/v1/search ──────────────────────── 500ms
  └─ SearchService.search ─────────────── 480ms
       ├─ [TX] findRestaurants ────────── 350ms  ← 병목
       └─ [TX] findKeywords ────────────  100ms
```

**Span 속성**:
- `name`: `tasteam.tx <method_name>` (예: `tasteam.tx findRestaurants`)
- `domain`: 패키지에서 추출
- `method_name`: 서비스 메서드명
- `read_only`: readOnly 여부

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

`TransactionTracingAspect`가 `Order=9`로 바깥을 감싸 → 트레이스 컨텍스트가 열린 상태에서 메트릭 기록.

---

## 구현 파일

| 파일 | 역할 |
|---|---|
| `global/aop/ApiMetricsAspect.java` | Controller API 메트릭 |
| `global/aop/TransactionMetricsAspect.java` | 트랜잭션 메트릭 (`uri` 태그 포함) |
| `global/aop/TransactionTracingAspect.java` | 트랜잭션 Distributed Tracing Span |
| `global/metrics/MetricLabelPolicy.java` | 허용 라벨 화이트리스트 |

---

## 인프라 현황

### 애플리케이션 (이미 준비됨)

```yaml
# application.yml — 이미 존재
management:
  tracing:
    enabled: ${TRACING_ENABLED:false}
  otlp:
    tracing:
      endpoint: ${OTEL_ENDPOINT:http://localhost:4318/v1/traces}
```

의존성도 이미 존재:
- `micrometer-tracing-bridge-otel`
- `opentelemetry-exporter-otlp`

### 외부 인프라 (별도 구축 필요)

| 서비스 | 역할 | 로컬 포트 |
|---|---|---|
| **Grafana Tempo** | Trace 수신 및 조회 | 4318 (OTLP HTTP) |
| **Prometheus** | 메트릭 수집 | 9090 |
| **Grafana** | 대시보드 | 3001 |

로컬 실행:
```bash
docker-compose -f docker-compose.local.yml -f docker-compose.monitoring.yml up -d
```

---

## 환경변수

| 변수명 | 기본값 | 설명 |
|---|---|---|
| `METRICS_AOP_ENABLED` | `true` | API/트랜잭션 메트릭 AOP 전체 |
| `METRICS_API_ENABLED` | `true` | API 메트릭 AOP |
| `METRICS_TX_ENABLED` | `true` | 트랜잭션 메트릭 AOP |
| `TRACING_AOP_ENABLED` | `false` | 트랜잭션 트레이싱 AOP (Tempo 필요) |
| `TRACING_TX_ENABLED` | `false` | 트랜잭션 트레이싱 AOP 세부 |
| `TRACING_ENABLED` | `false` | Spring 트레이싱 전체 |
| `OTEL_ENDPOINT` | `http://localhost:4318/v1/traces` | Tempo OTLP 엔드포인트 |

---

## trace-id 기반 필터링 전략

traceId는 **Prometheus 라벨로 사용하지 않는다** → Prometheus OOM 방지.
대신 Micrometer Exemplar로 히스토그램에 주입 → Grafana에서 이상치 점 클릭 시 Tempo 드릴다운.

---

## 카디널리티 제어 방침 (MetricLabelPolicy)

허용 라벨: `method`, `uri`, `domain`, `outcome`, `read_only`, `method_name`

금지 라벨: `memberId`, `chatRoomId`, `eventId` — 사용자 식별자는 카디널리티 무제한

---

## 쿼리 카운트 정확도 한계

`tasteam.transaction.query.count`는 Hibernate `SessionFactory` 전역 통계 기반.
동시 요청 환경에서 ±N 오차 가능. 정확한 분석은 `pg_stat_statements` 사용.

---

## 검증 방법

```bash
# 모니터링 스택 시작
docker-compose -f docker-compose.local.yml -f docker-compose.monitoring.yml up -d

# 애플리케이션 실행 (트레이싱 활성화)
TRACING_AOP_ENABLED=true TRACING_TX_ENABLED=true TRACING_ENABLED=true \
OTEL_ENDPOINT=http://localhost:4318/v1/traces \
./gradlew bootRun --args='--spring.profiles.active=local'

# 메트릭 확인
curl localhost:8080/actuator/prometheus | grep tasteam_transaction
# uri 태그 포함 여부 확인: uri="/api/v1/search" 있어야 함

# Exemplar 포함 여부 확인
curl -H "Accept: application/openmetrics-text; version=1.0.0" \
  localhost:8080/actuator/prometheus | grep -A5 "tasteam_transaction_duration"

# Grafana 접속: http://localhost:3001
# Explore → Tempo → Search → 서비스명 tasteam-api 조회
```
