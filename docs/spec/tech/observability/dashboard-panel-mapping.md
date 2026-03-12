# Grafana Dashboard Panel Mapping

## 목적

현재 backend monitoring 대시보드는 역할 중복이 크다.
정리 목표는 아래 4영역만 남기고, 현재 Spring 런타임에서 실제로 보이는 메트릭 family 중심으로 패널을 재배치하는 것이다.

- 앱 핵심
- 비동기
- 캐시
- 트랜잭션 상세

질문 문구에는 `비동기, 캐시, 트랜잭션 상세`만 직접 언급되었지만, 직전 구조 합의상 4번째 영역은 `앱 핵심`으로 본다.

## 설계 원칙

1. 하나의 메트릭 family는 하나의 대표 대시보드에서만 소유한다.
2. 현재 로컬 Spring runtime 기준으로 실제 노출되는 family를 우선한다.
3. `MQ_ENABLED`, `ANALYTICS_POSTHOG_ENABLED`에 묶인 conditional 패널은 기본 보드에서 제거하거나 접힌 row로 격리한다.
4. 인프라 상관관계는 독립 infra dashboard가 이미 있으므로 async/dashboard 내부에는 최소한만 남긴다.

## 목표 정보구조

### 1. 앱 핵심

운영자가 가장 먼저 보는 기본 보드다.
목표는 `애플리케이션 생존`, `JVM 상태`, `DB 풀 상태`, `관측 파이프라인 상태`만 한 화면에서 확인하는 것이다.

권장 섹션:

- Service Health
- JVM / Runtime
- DB Pool
- Observability Health

### 2. 비동기

executor, outbox backlog, notification, user-activity, websocket를 한 보드에서 본다.
현재 실제 노출 family 기준으로 `executor`, `notification_outbox_*`, `analytics_user_activity_*_outbox_*`, `outbox_*_snapshot_*`, `ws_connections_active`를 기본 축으로 쓴다.

권장 섹션:

- Executors
- Notification Outbox
- User Activity Outbox
- WebSocket
- Conditional Pipelines (optional, collapsed)

### 3. 캐시

Spring local cache 전용 보드다.
현재 `tasteam_cache_*` family만 전담한다.

권장 섹션:

- Cache Efficiency
- Cache Footprint

### 4. 트랜잭션 상세

API latency와 transaction latency/query count를 drill-down 하는 보드다.
기존 `tasteam_transaction_*` 외에 현재 실제 노출되는 `tasteam_tx_*` method family를 이 보드의 상세 섹션으로 승격한다.

권장 섹션:

- API Overview
- Transaction Overview
- Transaction Method Detail
- Trace Drill-down

## 소스 대시보드별 매핑

### Tasteam Spring Boot Statistics

유지/이동:

- `Uptime` -> 앱 핵심 / Service Health
- `Start Time` -> 앱 핵심 / Service Health
- `Heap Used` -> 앱 핵심 / JVM / Runtime
- `Non-Heap Used` -> 앱 핵심 / JVM / Runtime
- `CPU Usage` -> 앱 핵심 / JVM / Runtime
- `Load / CPU Cores` -> 앱 핵심 / JVM / Runtime
- `JVM Threads` -> 앱 핵심 / JVM / Runtime
- `GC Pause Duration` -> 앱 핵심 / JVM / Runtime
- `HikariCP Connections` -> 앱 핵심 / DB Pool
- `HikariCP Connection Timing` -> 앱 핵심 / DB Pool
- `Executor Active Threads` -> 비동기 / Executors
- `Executor Queue Depth` -> 비동기 / Executors
- `Executor Completed Tasks / sec` -> 비동기 / Executors

제거:

- `Process Open Files`
- `JVM Heap Memory`
- `JVM Non-Heap Memory`
- `GC Pause Rate`
- `Classes Loaded`
- `Direct / Mapped Buffer Memory`
- `Direct / Mapped Buffer Count`
- `HTTP Request Rate`
- `HTTP Avg Response Time`
- `HTTP p95 / p99 Latency`
- `HTTP Error Rate`
- `ERROR Logs`
- `WARN Logs`
- `INFO Logs`
- `MQ Publish / Consume Rate`
- `MQ Consume Fail Rate`
- `MQ Consume p95 Latency`
- `Analytics Outbox / Dispatch Counters`
- `Dispatch Circuit Open / WS Timeout`

비고:

- HTTP row는 `트랜잭션 상세`의 API Overview가 더 구체적이므로 제거한다.
- Logback row는 `Server Logs` dashboard가 이미 책임진다.
- MQ/dispatch row는 현재 local runtime에서는 family가 비어 있으므로 기본 보드에서 제외한다.

### Tasteam Spring Inventory

유지/이동:

- `Spring Target Up` -> 앱 핵심 / Observability Health
- `Alloy Forwarded Samples/s` -> 앱 핵심 / Observability Health
- `Scraped Metric Families` -> 앱 핵심 / Observability Health
- `Alloy Spring Pipeline Throughput` -> 앱 핵심 / Observability Health

제거:

- `Conditional Pipeline Families Active`
- `Scraped Spring Metric Family Inventory`
- `Conditional or Event-driven Families Present`

비고:

- inventory table 2개는 설계 검토/감사용으로는 유용하지만 일상 운영 보드로는 과하다.

### Tasteam Spring Cache (스프링 캐시)

유지/이동:

- `Cache Hit Ratio` -> 캐시 / Cache Efficiency
- `Cache Hit and Miss Rate` -> 캐시 / Cache Efficiency
- `Cache Size` -> 캐시 / Cache Footprint
- `Cache TTL` -> 캐시 / Cache Footprint

제거:

- 없음

### Tasteam - Async/Event-Driven Architecture Observability

유지/이동:

- `Executor Active Threads (활성 워커)` -> 비동기 / Executors
- `Executor Queue (대기열)` -> 비동기 / Executors
- `Executor Throughput (처리량)` -> 비동기 / Executors
- `User Activity Source Outbox (소스 아웃박스)` -> 비동기 / User Activity Outbox
- `WebSocket Core Signals (연결 핵심 신호)` -> 비동기 / WebSocket

제거:

- `Spring Up (앱 인스턴스 수)`
- `Process CPU Avg (앱 CPU 평균)`
- `JVM Live Threads (활성 스레드)`
- `DB Pending Conn (DB 대기 커넥션)`
- `Redis Up (Redis 인스턴스 수)`
- `Postgres Up (DB exporter 수)`
- `MQ Publish Rate (발행 속도)`
- `MQ Consume Rate (소비 속도)`
- `MQ Consume Latency p95 (소비 지연 p95)`
- `Notification Chat Events (알림 이벤트)`
- `User Activity Dispatch (디스패치/서킷)`
- `Heartbeat Timeout Ratio (타임아웃 비율)`
- `Infra Correlation (Redis 오류/메모리)`

비고:

- `User Activity Dispatch` 패널은 현재 visible family보다 conditional metric 의존도가 높다.
  실제 통합본에서는 `analytics_user_activity_dispatch_outbox_*` gauge 기반 backlog 패널로 재작성하는 편이 낫다.
- `WebSocket Core Signals` 역시 현 시점 기본 family는 `ws_connections_active`만 안정적이므로 active connection 중심으로 축소한다.

### Tasteam - Notification Async Pipeline

유지/이동:

- `Outbox Pending (대기)` -> 비동기 / Notification Outbox
- `Outbox Failed (실패)` -> 비동기 / Notification Outbox
- `Outbox Retrying (재시도 대기)` -> 비동기 / Notification Outbox
- `Outbox Status Trend (아웃박스 상태)` -> 비동기 / Notification Outbox

제거:

- `Consumer Success Ratio (5m)`
- `DLQ Enqueue (5m)`
- `Consumer Latency p95`
- `Spring Up`
- `Redis Up`
- `Consumer Process/DLQ Rate (처리/DLQ 속도)`
- `Consumer Latency Trend (처리 지연)`

비고:

- consumer 계열은 `MQ_ENABLED=true`일 때 의미가 생긴다.
  현재 기본 통합 보드에서는 제외하고, 필요 시 `Conditional Pipelines` row로 되돌리는 편이 낫다.

### Tasteam - User Activity Event Pipeline

유지/이동:

- `Source Outbox Pending` -> 비동기 / User Activity Outbox
- `Source Outbox Failed` -> 비동기 / User Activity Outbox
- `Dispatch Outbox Pending` -> 비동기 / User Activity Outbox
- `Dispatch Outbox Failed` -> 비동기 / User Activity Outbox
- `Source Outbox Backlog (소스 아웃박스 적체)` -> 비동기 / User Activity Outbox
- `Dispatch Outbox Backlog (타겟별 적체)` -> 비동기 / User Activity Outbox

제거:

- `Replay Success (5m)`
- `Replay Failed (5m)`
- `MQ Publish Rate`
- `MQ Consume Rate`
- `Replay/Dispatch Throughput (재처리/디스패치 처리량)`

비고:

- replay/dispatch execute 계열은 conditional metric 의존도가 높아 기본 merged dashboard에서는 제외한다.

### Tasteam - WebSocket/STOMP Chat Observability

유지/이동:

- `Active Connections (활성 연결 수)` -> 비동기 / WebSocket
- `Active Connections by Instance (인스턴스별 활성 연결)` -> 비동기 / WebSocket

조건부 보존:

- `Connect vs Disconnect Rate (연결/해제 속도)`
- `Disconnect by Reason Rate (사유별 연결해제 속도)`
- `Session Lifetime Quantiles (세션 유지시간 분위수)`

제거:

- `Spring Up (앱 인스턴스 수)`
- `App CPU Avg (앱 CPU 평균)`
- `Redis Up (Redis 인스턴스 수)`
- `Redis Cmd Latency (Redis 평균 지연)`
- `Node Up (노드 exporter 수)`
- `RDS Conn Saturation (RDS 연결 포화율)`
- `Connect Rate/s (초당 연결)`
- `Disconnect Rate/s (초당 연결해제)`
- `Reconnect (5m) (5분 재연결)`
- `Heartbeat Timeout Ratio (하트비트 타임아웃 비율)`
- `Session Lifetime p95 (세션 유지시간 p95)`
- `Heartbeat Timeout by Instance (인스턴스별 타임아웃)`
- `Reconnect Burst (재연결 급증)`
- `Disconnect Spike (연결해제 급증)`
- `Average Session Lifetime (평균 세션 유지시간)`
- `Heartbeat Timeout Ratio Trend (타임아웃 비율 추이)`
- `Active Connection Drop Ratio (활성 연결 급감 비율)`
- `Disconnect Count 5m (5분 연결해제 건수)`

비고:

- 현재 기본 runtime에서는 `ws_connections_active`만 안정적으로 보인다.
- websocket event family가 실제로 꾸준히 발생하는 환경이면 `조건부 보존` 패널만 별도 row로 다시 여는 것이 맞다.

### Tasteam - API Transaction Tracing

유지/이동:

- `RPS by Domain` -> 트랜잭션 상세 / API Overview
- `API Latency Quantiles` -> 트랜잭션 상세 / API Overview
- `5xx Error Rate` -> 트랜잭션 상세 / API Overview
- `Top 10 Avg API Latency` -> 트랜잭션 상세 / API Overview
- `Transaction P99 by Method` -> 트랜잭션 상세 / Transaction Overview
- `Avg Transaction Duration` -> 트랜잭션 상세 / Transaction Overview
- `Top 10 Avg Query Count` -> 트랜잭션 상세 / Transaction Overview
- `ReadOnly Ratio` -> 트랜잭션 상세 / Transaction Overview
- `Trace Entry Point (P99)` -> 트랜잭션 상세 / Trace Drill-down
- `Tempo Drill-down Guide` -> 트랜잭션 상세 / Trace Drill-down

신규 추가:

- `Top Transaction Active Time by Method`
  - source family: `tasteam_tx_*_active_seconds`
- `Top Transaction End-to-End Time by Method`
  - source family: `tasteam_tx_*_seconds`
- `Transaction Active vs End-to-End Gap`
  - source family: `tasteam_tx_*_active_seconds`, `tasteam_tx_*_seconds`

비고:

- 기존 tracing 보드는 `tasteam_transaction_*` 기반 상위 분석은 좋지만, 현재 runtime에서 실제 보이는 `tasteam_tx_*` method family를 거의 활용하지 못한다.
- 따라서 `Transaction Method Detail` 섹션은 신규 패널 2~3개를 추가해 보강하는 것이 핵심이다.

## 최종 권장 결과물

### 앱 핵심

- Service Health: `Spring Target Up`, `Uptime`, `Start Time`
- JVM / Runtime: `Heap Used`, `Non-Heap Used`, `CPU Usage`, `Load / CPU Cores`, `JVM Threads`, `GC Pause Duration`
- DB Pool: `HikariCP Connections`, `HikariCP Connection Timing`
- Observability Health: `Scraped Metric Families`, `Alloy Forwarded Samples/s`, `Alloy Spring Pipeline Throughput`

### 비동기

- Executors: `Executor Active Threads`, `Executor Queue`, `Executor Completed Tasks / sec`
- Notification Outbox: `Outbox Pending`, `Outbox Failed`, `Outbox Retrying`, `Outbox Status Trend`
- User Activity Outbox: `Source Outbox Pending`, `Source Outbox Failed`, `Dispatch Outbox Pending`, `Dispatch Outbox Failed`, `Source Outbox Backlog`, `Dispatch Outbox Backlog`
- WebSocket: `Active Connections`, `Active Connections by Instance`
- Optional collapsed row: websocket event panels, MQ/consumer panels

### 캐시

- Cache Efficiency: `Cache Hit Ratio`, `Cache Hit and Miss Rate`
- Cache Footprint: `Cache Size`, `Cache TTL`

### 트랜잭션 상세

- API Overview: `RPS by Domain`, `API Latency Quantiles`, `5xx Error Rate`, `Top 10 Avg API Latency`
- Transaction Overview: `Transaction P99 by Method`, `Avg Transaction Duration`, `Top 10 Avg Query Count`, `ReadOnly Ratio`
- Transaction Method Detail: 신규 `tasteam_tx_*` 기반 2~3개 패널
- Trace Drill-down: `Trace Entry Point (P99)`, `Tempo Drill-down Guide`

## 구현 우선순위

1. `Spring Boot Statistics`를 `앱 핵심`용으로 축소
2. `Async/Event Observability`를 새 대표 `비동기` 보드로 재구성
3. `API Transaction Tracing`에 `tasteam_tx_*` 기반 상세 패널 추가
4. `Spring Inventory`는 앱 핵심 하단 row로 흡수
5. `Notification Async`, `User Activity Event`, `WebSocket` 원본 보드는 제거 또는 archive
