# Chat Bottleneck PromQL

신규 메트릭 4종 기준으로 Grafana 패널/알림에 바로 사용할 PromQL 모음입니다.

## 1) DB Query Count (`db_query_count_total`)

### 패널: `send_chat_message` 초당 DB 쿼리 수
```promql
sum(rate(db_query_count_total{api="send_chat_message", environment=~"$env", instance=~"$instance"}[1m]))
```

### 패널: 인스턴스별 `send_chat_message` 초당 DB 쿼리 수
```promql
sum by (instance) (rate(db_query_count_total{api="send_chat_message", environment=~"$env", instance=~"$instance"}[1m]))
```

### 패널: 요청 1건당 평균 DB 쿼리 수 (5m)
```promql
(
  sum(rate(db_query_count_total{api="send_chat_message", environment=~"$env", instance=~"$instance"}[5m]))
)
/
clamp_min(
  sum(rate(http_server_requests_seconds_count{
    environment=~"$env",
    instance=~"$instance",
    method="POST",
    uri="/api/v1/chat-rooms/{chatRoomId}/messages"
  }[5m])),
  1
)
```

## 2) Redis Stream Pending (`chat_stream_pending_messages`)

### 패널: 전체 pending 합계
```promql
sum(chat_stream_pending_messages{environment=~"$env", instance=~"$instance"})
```

### 패널: 파티션별 pending
```promql
sum by (partition) (chat_stream_pending_messages{environment=~"$env", instance=~"$instance"})
```

### 패널: 최악 파티션 Top 5
```promql
topk(5, sum by (partition) (chat_stream_pending_messages{environment=~"$env", instance=~"$instance"}))
```

## 3) Notification Executor Queue (`notification_executor_queue_size`)

### 패널: 현재 queue size
```promql
sum(notification_executor_queue_size{environment=~"$env", instance=~"$instance"})
```

### 패널: 인스턴스별 queue size
```promql
sum by (instance) (notification_executor_queue_size{environment=~"$env", instance=~"$instance"})
```

### 패널: 최근 10분 최대 queue size
```promql
max_over_time(
  sum(notification_executor_queue_size{environment=~"$env", instance=~"$instance"})[10m:]
)
```

## 4) Notification Processing Latency (`notification_processing_latency_seconds_*`)

### 패널: p95 latency (전체)
```promql
histogram_quantile(
  0.95,
  sum(rate(notification_processing_latency_seconds_bucket{environment=~"$env", instance=~"$instance"}[5m])) by (le)
)
```

### 패널: p99 latency (전체)
```promql
histogram_quantile(
  0.99,
  sum(rate(notification_processing_latency_seconds_bucket{environment=~"$env", instance=~"$instance"}[5m])) by (le)
)
```

### 패널: outcome별 처리량
```promql
sum by (outcome) (
  rate(notification_processing_latency_seconds_count{environment=~"$env", instance=~"$instance"}[5m])
)
```

### 패널: 에러율
```promql
(
  sum(rate(notification_processing_latency_seconds_count{outcome="error", environment=~"$env", instance=~"$instance"}[5m]))
)
/
clamp_min(
  sum(rate(notification_processing_latency_seconds_count{environment=~"$env", instance=~"$instance"}[5m])),
  1
)
```

## Alert 추천 (옵션)

### Stream pending backlog 증가
```promql
sum(chat_stream_pending_messages{environment=~"$env", instance=~"$instance"}) > 1000
```

### Notification queue 적체
```promql
sum(notification_executor_queue_size{environment=~"$env", instance=~"$instance"}) > 200
```

### Notification p95 지연
```promql
histogram_quantile(
  0.95,
  sum(rate(notification_processing_latency_seconds_bucket{environment=~"$env", instance=~"$instance"}[5m])) by (le)
) > 1.0
```

### `send_chat_message` 1건당 DB 쿼리 급증
```promql
(
  sum(rate(db_query_count_total{api="send_chat_message", environment=~"$env", instance=~"$instance"}[5m]))
)
/
clamp_min(
  sum(rate(http_server_requests_seconds_count{
    environment=~"$env",
    instance=~"$instance",
    method="POST",
    uri="/api/v1/chat-rooms/{chatRoomId}/messages"
  }[5m])),
  1
) > 20
```
