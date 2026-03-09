# Grafana 대시보드 명세

## 로컬 모니터링 스택 시작

```bash
docker-compose -f docker-compose.local.yml -f docker-compose.monitoring.yml up -d
```

| 서비스 | URL | 계정 |
|---|---|---|
| Grafana | http://localhost:3001 | 익명 접근 (Admin) |
| Prometheus | http://localhost:9090 | - |
| Tempo | http://localhost:3200 | - |

Grafana에 접속하면 Prometheus + Tempo 데이터소스가 자동 프로비저닝됨.

---

## 대시보드 1: API Overview

**목적**: API 요청 흐름, 응답시간, 에러율 추세 모니터링

### RPS

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

---

## 대시보드 3: Trace Drill-down (Tempo)

**목적**: 특정 요청 1건의 실제 흐름을 폭포수 다이어그램으로 확인

### 흐름

1. **대시보드 2**에서 P99 히스토그램 이상치 점(Exemplar) 클릭
2. 팝업의 `traceId` 클릭 → Grafana Tempo 자동 이동
3. Tempo 폭포수 다이어그램에서 확인:

```
GET /api/v1/search ──────────────── 500ms
  └─ tasteam.tx search ─────────── 480ms
       ├─ tasteam.tx findRestaurants 350ms  ← 병목
       └─ tasteam.tx findKeywords    100ms
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

## Exemplar 활성화 확인

Prometheus에 `--enable-feature=exemplar-storage` 플래그가 설정되어 있어야 함 (docker-compose.monitoring.yml에 포함).

```bash
# Exemplar 포함 scrape 확인
curl -H "Accept: application/openmetrics-text; version=1.0.0" \
  localhost:8080/actuator/prometheus | grep -A5 "tasteam_transaction_duration"
# # {traceID="abc123..."} 형태가 보이면 정상
```

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
