# Grafana 대시보드 명세

> 이 문서는 `3-team-Tasteam-be/docker-compose.local-monitoring.yml` 기반의 로컬 개발용 스택 기준이다.
> 클라우드 v2 운영 대시보드 자동 프로비저닝은 `3-team-Tasteam-cloud/v2-docker/monitoring/grafana/`를 기준으로 관리한다.

## 전제 조건 & 파일 구조

모니터링 스택은 아래 파일로 구성된다.

```
3-team-Tasteam-be/
├── docker-compose.local-monitoring.yml    ← 로컬 모니터링 오버레이 정의
└── monitoring/
    ├── grafana/
    │   ├── dashboards/
    │   │   ├── application/               ← Tasteam Application
    │   │   ├── infrastructure/            ← Tasteam Infrastructure
    │   │   ├── logs-tracing/              ← Tasteam Logs & Tracing
    │   │   └── async-events/              ← Tasteam Async & Events
    │   └── provisioning/
    │       ├── dashboards/
    │       │   └── dashboards.yml         ← 폴더별 provider 정의
    │       └── datasources/
    │           └── datasources.yml        ← 데이터소스 자동 프로비저닝
    ├── prometheus/
    │   └── prometheus.yml                 ← Prometheus 수신기/룰 설정
    ├── tempo/
    │   └── config.yaml                    ← Tempo OTLP 수신 설정
    └── loki/
        └── loki.yml                       ← 로컬 Loki 저장소 설정
```

---

## 빠른 시작 (10분 세팅)

### Step 1: 환경변수 설정

`.env.local` 파일에 아래 항목을 추가한다.

```bash
# Tracing AOP 활성화
TRACING_AOP_ENABLED=true
TRACING_TX_ENABLED=true

# Spring OTLP Export 활성화
TRACING_ENABLED=true

# docker-compose 내 api 컨테이너에서 실행할 경우:
OTEL_ENDPOINT=http://tempo:4318/v1/traces

# ./gradlew bootRun (로컬 직접 실행)할 경우:
# OTEL_ENDPOINT=http://localhost:4318/v1/traces
```

> 메트릭(Prometheus scrape)은 별도 설정 없이 항상 활성화된다. tracing만 opt-in.

### Step 2: 스택 실행

```bash
# local 인프라(PostgreSQL 등) + 모니터링 스택 함께 실행
docker-compose -f docker-compose.local.yml -f docker-compose.local-monitoring.yml up -d
```

### Step 3: 메트릭 노출 확인

```bash
curl localhost:8080/actuator/prometheus | grep tasteam_
```

정상 출력 예시:
```
tasteam_api_request_duration_seconds_count{domain="search",method="GET",...} 42
tasteam_transaction_duration_seconds_count{domain="search",method_name="findRestaurants",...} 42
```

### Step 4: Grafana 접속

| 서비스 | URL | 계정 |
|---|---|---|
| Grafana | http://localhost:3001 | 익명 접근 (Admin 권한) |
| Prometheus | http://localhost:9090 | - |
| Tempo | http://localhost:3200 | - |

Grafana 접속 시 Prometheus + Tempo 데이터소스가 **자동 프로비저닝**되어 있다 (`monitoring/grafana/provisioning/datasources/datasources.yml` 기준).
대시보드는 provider 기준으로 아래 4개 폴더에 나뉜다.

- `Tasteam Application`
- `Tasteam Infrastructure`
- `Tasteam Logs & Tracing`
- `Tasteam Async & Events`

### Step 5: 트레이싱 확인

```bash
# Exemplar(traceID) 포함 여부 확인
curl -H "Accept: application/openmetrics-text; version=1.0.0" \
  localhost:8080/actuator/prometheus | grep -A5 "tasteam_transaction_duration"
# # {traceID="abc123..."} 형태가 보이면 정상
```

Grafana → Explore → Tempo → Search:
- Service Name: `tasteam-api`
- Span Name: `tasteam.tx findRestaurants`

---

## 인프라 설정 파일 전체 내용

### monitoring/prometheus/prometheus.yml

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: tasteam-api
    scrape_interval: 15s
    metrics_path: /actuator/prometheus
    # OpenMetrics 형식으로 scrape → Exemplar 수집 활성화
    params:
      format: [ openmetrics ]
    static_configs:
      - targets:
          - host.docker.internal:8080   # compose 컨테이너에서 호스트 API 접근
```

### monitoring/tempo/config.yaml

```yaml
server:
  http_listen_port: 3200        # Tempo UI / Grafana 쿼리 포트

distributor:
  receivers:
    otlp:
      protocols:
        http:
          endpoint: 0.0.0.0:4318   # OTLP HTTP 수신 (Spring Boot → Tempo)
        grpc:
          endpoint: 0.0.0.0:4317   # OTLP gRPC 수신

ingester:
  max_block_duration: 5m

compactor:
  compaction:
    block_retention: 1h          # 로컬 개발용: trace 1시간 보관

storage:
  trace:
    backend: local               # 로컬 개발용 (운영은 S3/GCS)
    local:
      path: /tmp/tempo/blocks
    wal:
      path: /tmp/tempo/wal
```

### monitoring/grafana/provisioning/datasources/datasources.yml

```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    uid: prometheus
    url: http://prometheus:9090   # Grafana → Prometheus (컨테이너 간 통신)
    isDefault: true
    jsonData:
      exemplarTraceIdDestinations:
        - name: traceID            # Exemplar의 traceID 필드를 Tempo로 연결
          datasourceUid: tempo     # 클릭 시 Tempo 자동 이동

  - name: Tempo
    type: tempo
    uid: tempo
    url: http://tempo:3200         # Grafana → Tempo (컨테이너 간 통신)
    jsonData:
      tracesToLogsV2:
        datasourceUid: prometheus  # Trace → Logs 연결 (Prometheus 메트릭 참조)
      serviceMap:
        datasourceUid: prometheus  # 서비스 맵 기능 활성화
      nodeGraph:
        enabled: true              # 노드 그래프 활성화
```

> **Exemplar → Tempo 드릴다운 필수 조건**:
> 1. Prometheus에 `--enable-feature=exemplar-storage` 플래그 (`docker-compose.local-monitoring.yml`에 포함)
> 2. Prometheus scrape 시 OpenMetrics 형식 (`params.format: [openmetrics]`)
> 3. `datasources.yml`의 `exemplarTraceIdDestinations` 설정
> 4. Grafana Feature Toggle: `exemplarTraceIdDestinations traceqlEditor` (`docker-compose.local-monitoring.yml`에 포함)

---

## Prometheus target 주소 — 환경별 차이

Prometheus는 컨테이너 안에서 실행되므로, API 서버 위치에 따라 target 주소가 달라진다.

| 실행 환경 | Prometheus target | OTEL_ENDPOINT (API → Tempo) |
|---|---|---|
| `./gradlew bootRun` (호스트 직접) | `host.docker.internal:8080` | `http://localhost:4318/v1/traces` |
| docker-compose 내 api 컨테이너 | `api:8080` (서비스명) | `http://tempo:4318/v1/traces` |

> 현재 `prometheus.yml`은 `host.docker.internal:8080` 설정 — `./gradlew bootRun` 사용 시 기본 동작.
> api를 컨테이너로 올릴 경우 `prometheus.yml`의 target을 `api:8080`으로 변경 필요.

---

## 대시보드 1: API Overview

**목적**: API 요청 흐름, 응답시간, 에러율 추세 모니터링

### RPS (초당 요청 수)

```promql
sum(rate(tasteam_api_request_total[1m])) by (domain)
```

### 응답시간 P50 / P95 / P99

```promql
histogram_quantile(0.99,
  sum(rate(tasteam_api_request_duration_seconds_bucket[5m])) by (le, uri, method)
)
```

### 에러율 (5xx)

```promql
sum(rate(tasteam_api_request_total{outcome="server_error"}[1m]))
/
sum(rate(tasteam_api_request_total[1m]))
```

### URI별 평균 응답시간 Top 10

```promql
topk(10,
  rate(tasteam_api_request_duration_seconds_sum[5m])
  /
  rate(tasteam_api_request_duration_seconds_count[5m])
)
```

**필터 변수**: `domain`, `method`, `outcome`

#### Grafana 변수 설정 방법

| 변수명 | 유형 | Query |
|---|---|---|
| `domain` | Query | `label_values(tasteam_api_request_total, domain)` |
| `method` | Query | `label_values(tasteam_api_request_total, method)` |
| `outcome` | Query | `label_values(tasteam_api_request_total, outcome)` |

패널 PromQL에 `{domain=~"$domain"}` 형태로 변수 적용.

---

## 대시보드 2: Transaction Analysis

**목적**: URI별 트랜잭션 병목 파악

### 핵심 쿼리: URI별 트랜잭션 P99

```promql
# /api/v1/search 에서 각 트랜잭션의 P99
histogram_quantile(0.99,
  sum(rate(tasteam_transaction_duration_seconds_bucket{uri="/api/v1/search"}[5m]))
    by (le, method_name)
)
```

→ `findRestaurants = 340ms`, `findKeywords = 95ms` 형태로 병목 메서드 바로 확인 가능.

### URI별 트랜잭션 평균 소요시간

```promql
sum by (uri, method_name) (
  rate(tasteam_transaction_duration_seconds_sum[5m])
)
/
sum by (uri, method_name) (
  rate(tasteam_transaction_duration_seconds_count[5m])
)
```

### N+1 의심 메서드 Top 10 (쿼리 수 상위)

```promql
topk(10,
  rate(tasteam_transaction_query_count_sum[5m])
  /
  rate(tasteam_transaction_query_count_count[5m])
)
```

### readOnly 비율

```promql
sum(rate(tasteam_transaction_total{read_only="true"}[1m]))
/
sum(rate(tasteam_transaction_total[1m]))
```

**필터 변수**: `uri`, `domain`, `method_name`, `read_only`, `outcome`

#### Grafana 변수 설정 방법

| 변수명 | 유형 | Query |
|---|---|---|
| `uri` | Query | `label_values(tasteam_transaction_total, uri)` |
| `domain` | Query | `label_values(tasteam_transaction_total, domain)` |
| `method_name` | Query | `label_values(tasteam_transaction_total, method_name)` |
| `read_only` | Custom | `true, false` |
| `outcome` | Custom | `success, error` |

---

## 대시보드 3: Trace Drill-down (Tempo)

**목적**: 특정 요청 1건의 실제 흐름을 폭포수 다이어그램으로 확인

### 흐름

1. **대시보드 2**에서 P99 히스토그램 이상치 점(Exemplar) 클릭
2. 팝업의 `traceId` 클릭 → Grafana Tempo 자동 이동 (`datasources.yml`의 `exemplarTraceIdDestinations` 설정으로 동작)
3. Tempo 폭포수 다이어그램에서 확인:

```
GET /api/v1/search ──────────────────────── 500ms
  └─ tasteam.tx search ─────────────────── 480ms
       ├─ tasteam.tx findRestaurants ────── 350ms  ← 병목
       └─ tasteam.tx findKeywords ─────────  100ms
```

### Tempo에서 직접 검색

Grafana → Explore → Tempo → Search:
- Service Name: `tasteam-api`
- Span Name: `tasteam.tx findRestaurants`
- 기간/태그 필터 설정 후 조회

### TraceQL 쿼리 예시

```
# findRestaurants span이 300ms 초과인 trace 조회
{ span.method_name = "findRestaurants" && duration > 300ms }

# search URI에서 에러 발생한 trace
{ resource.service.name = "tasteam-api" && span.uri = "/api/v1/search" && status = error }
```

---

## 대시보드 4: Cache Intelligence

**목적**: API 계열별 로컬 캐시 TTL, hit ratio, miss rate, size, eviction을 한 화면에서 점검

> 대시보드 소스 파일:
> `3-team-Tasteam-cloud/monitoring/cache-monito.json`

### 전체 hit ratio (5분)

```promql
100 *
sum(increase(tasteam_cache_requests_total{result="hit"}[5m]))
/
clamp_min(sum(increase(tasteam_cache_requests_total[5m])), 1)
```

### 캐시별 TTL

```promql
max by (cache, uri) (tasteam_cache_ttl_seconds)
```

### 캐시별 miss rate

```promql
sum by (cache, uri) (
  rate(tasteam_cache_requests_total{result="miss"}[5m])
)
```

### 캐시별 eviction rate

```promql
sum by (cache, uri) (
  rate(tasteam_cache_evictions_total[5m])
)
```

### 캐시별 현재 엔트리 수

```promql
sum by (cache, uri) (tasteam_cache_size)
```

**필터 변수**: `DS_PROMETHEUS`, `env`, `instance`, `domain`, `uri`, `cache`

#### Grafana 변수 설정 방법

| 변수명 | 유형 | Query |
|---|---|---|
| `DS_PROMETHEUS` | Datasource | `prometheus` |
| `env` | Custom | `prod,stg,dev` |
| `instance` | Query | `label_values(up{job="spring", environment=~"$env"}, instance)` |
| `domain` | Query | `label_values(tasteam_cache_requests_total{job="spring", environment=~"$env", instance=~"$instance"}, domain)` |
| `uri` | Query | `label_values(tasteam_cache_requests_total{job="spring", environment=~"$env", instance=~"$instance", domain=~"$domain"}, uri)` |
| `cache` | Query | `label_values(tasteam_cache_requests_total{job="spring", environment=~"$env", instance=~"$instance", domain=~"$domain", uri=~"$uri"}, cache)` |

---

## Exemplar 활성화 확인

Exemplar(히스토그램 이상치 점 → traceId 연결)가 동작하려면 아래 세 조건이 모두 충족되어야 한다:

| 조건 | 설정 위치 | 현재 상태 |
|---|---|---|
| Prometheus `exemplar-storage` 활성화 | `docker-compose.local-monitoring.yml` → `--enable-feature=exemplar-storage` | 포함됨 |
| OpenMetrics 형식 scrape | `prometheus.yml` → `params.format: [openmetrics]` | 포함됨 |
| Grafana Exemplar 연결 설정 | `datasources.yml` → `exemplarTraceIdDestinations` | 포함됨 |
| Grafana Feature Toggle | `docker-compose.local-monitoring.yml` → `GF_FEATURE_TOGGLES_ENABLE=exemplarTraceIdDestinations traceqlEditor` | 포함됨 |

```bash
# Exemplar 포함 scrape 확인
curl -H "Accept: application/openmetrics-text; version=1.0.0" \
  localhost:8080/actuator/prometheus | grep -A5 "tasteam_transaction_duration"
# # {traceID="abc123..."} 형태가 보이면 정상
```

---

## 대시보드 4: Cache 모니터링

**목적**: Caffeine 캐시별 히트율, 미스율, 크기, 제거 수를 추적하여 캐시 효율성과 TTL 적정성을 판단

### 사전 조건 — 추가 코드 없음

`LocalCacheConfig`에 `recordStats()` + `micrometer-registry-prometheus` 의존성이 충족되어 있으므로
Spring Boot `CacheMetricsAutoConfiguration`이 자동으로 아래 메트릭을 `/actuator/prometheus`에 노출한다.

| Prometheus 메트릭 | 태그 | 설명 |
|---|---|---|
| `cache_gets_total` | `name`, `result=hit\|miss`, `cacheManager` | 히트/미스 누적 수 |
| `cache_puts_total` | `name`, `cacheManager` | 캐시 저장 누적 수 |
| `cache_evictions_total` | `name`, `cacheManager` | 만료/제거 누적 수 |
| `cache_size` | `name`, `cacheManager` | 현재 저장 항목 수 |

노출 확인:
```bash
curl -s localhost:8080/actuator/prometheus | grep 'cache_gets_total{.*reverse-geocode'
# cache_gets_total{cacheManager="caffeineCacheManager",name="reverse-geocode",result="hit",...} 142.0
# cache_gets_total{cacheManager="caffeineCacheManager",name="reverse-geocode",result="miss",...} 31.0
```

---

### 패널 1: 캐시별 히트율 (%)

```promql
100 * (
  rate(cache_gets_total{result="hit"}[5m])
  /
  (rate(cache_gets_total{result="hit"}[5m]) + rate(cache_gets_total{result="miss"}[5m]))
)
```

- 패널 유형: **Gauge** (0 ~ 100%)
- 임계값: 녹색 ≥ 70%, 노란색 50 ~ 70%, 빨간색 < 50%
- `name` 태그를 legend로 설정하면 캐시별 히트율 동시 표시

---

### 패널 2: 시간대별 히트/미스 추이

```promql
# 히트 수
rate(cache_gets_total{name="reverse-geocode", result="hit"}[5m])

# 미스 수
rate(cache_gets_total{name="reverse-geocode", result="miss"}[5m])
```

- 패널 유형: **Time series** (line)
- 두 쿼리를 같은 패널에 표시 (hit=파란색, miss=주황색)
- 앱 시작 직후 miss가 높다가 워밍업 후 hit가 지배적으로 올라가는 패턴 확인 용도

---

### 패널 3: 초당 Nominatim 실제 호출 수

미스 = 캐시 미스 = 외부 Nominatim API 호출이 발생한 시점이다.

```promql
rate(cache_gets_total{name="reverse-geocode", result="miss"}[1m])
```

- 패널 유형: **Time series** (bar)
- 기대값: 앱 초기 1분 이후 거의 0에 수렴해야 정상

---

### 패널 4: 캐시 현재 크기

```promql
cache_size{name="reverse-geocode"}
```

- 패널 유형: **Stat**
- `maximumSize=1000` 대비 현재 사용률 파악
- 1000에 근접하면 가장 오래된 항목이 LRU로 제거됨 → `cache_evictions_total` 증가와 함께 확인

---

### 패널 5: 전체 캐시 히트율 비교 (presigned-url vs reverse-geocode)

```promql
100 * (
  sum by (name) (rate(cache_gets_total{result="hit"}[5m]))
  /
  sum by (name) (rate(cache_gets_total[5m]))
)
```

- 패널 유형: **Bar gauge** (horizontal)
- 캐시별 효율성 한눈에 비교

---

### 예상 히트율 분석

| 시점 | 예상 히트율 | 이유 |
|------|------------|------|
| 앱 시작 후 0 ~ 10분 | 0 ~ 20% | 캐시 콜드 스타트 |
| 운영 정상 상태 | **70 ~ 90%** | 아래 근거 참고 |
| 심야 (트래픽 저조) | 40 ~ 60% | 캐시 항목 만료 후 재워밍 필요 |

**운영 중 70 ~ 90% 근거**

- 클라이언트는 소수점 3자리 truncate → 약 110 m 반경의 격자 셀이 캐시 키
- 테이스팀 사용 패턴: 회사 → 점심 식당 → 집 (3 ~ 5개 고정 위치 반복)
- 같은 건물·블록의 유저들은 동일 캐시 셀을 공유
- TTL 24h: 하루 동안 재방문할 때마다 히트 → 첫 방문 이후 당일 모든 요청이 캐시 히트
- 예외: 이동 중 위치 변경(도보/차량) → 셀 전환마다 미스. 전체 비율은 낮음

---

### Grafana 변수 설정 (대시보드 4)

| 변수명 | 유형 | Query | 용도 |
|--------|------|-------|------|
| `cache_name` | Query | `label_values(cache_gets_total, name)` | 캐시 선택 필터 |

패널 PromQL에 `{name=~"$cache_name"}` 적용.

---

### 알림 룰 — 캐시 히트율 저하

```yaml
- alert: CacheHitRateLow
  expr: |
    100 * (
      rate(cache_gets_total{name="reverse-geocode", result="hit"}[10m])
      /
      (
        rate(cache_gets_total{name="reverse-geocode", result="hit"}[10m])
        + rate(cache_gets_total{name="reverse-geocode", result="miss"}[10m])
      )
    ) < 50
  for: 15m
  labels:
    severity: warning
  annotations:
    summary: "reverse-geocode 캐시 히트율 50% 미만 (10m 평균)"
    description: "TTL 조정 또는 캐시 무효화 이슈 확인 필요"
```

> 히트율이 50% 아래로 15분 이상 지속되면 TTL이 너무 짧거나 트래픽 패턴이 변화한 것. 즉각 확인 필요.

---

## 알림 룰 예시

```yaml
groups:
  - name: tasteam-transaction-alerts
    rules:
      - alert: TransactionHighP99
        expr: |
          histogram_quantile(0.99,
            sum(rate(tasteam_transaction_duration_seconds_bucket[5m])) by (le, uri, method_name)
          ) > 3
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "트랜잭션 P99 > 3s: {{ $labels.uri }} / {{ $labels.method_name }}"

      - alert: ApiHighErrorRate
        expr: |
          sum(rate(tasteam_api_request_total{outcome="server_error"}[1m]))
          /
          sum(rate(tasteam_api_request_total[1m]))
          > 0.05
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "5xx 에러율 5% 초과"

      - alert: HighQueryCountTransaction
        expr: |
          topk(1,
            rate(tasteam_transaction_query_count_sum[5m])
            /
            rate(tasteam_transaction_query_count_count[5m])
          ) > 20
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "N+1 의심: 평균 쿼리 수 20회 초과"
```
