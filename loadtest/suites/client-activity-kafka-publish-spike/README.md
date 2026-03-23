# Client Activity Kafka Publish Spike

`POST /api/v1/analytics/events` 호출 후 저장 완료 hook가 이어지는 Kafka publish 경로와 Kafka Connect downstream의 순간 피크 내구성을 보는 k6 스위트입니다.

## 목적

- event API 자체는 성공하지만 Kafka 이후 lag, sink batch time, DLQ가 밀리는지 확인합니다.
- 작은 배치 burst와 큰 배치 burst의 병목 위치 차이를 분리해서 봅니다.

## 특성

- `TEST_TYPE=single-burst-small-batch|single-burst-large-batch|double-spike-recovery|mixed-identity-spike|steady-soak`
- `single-burst-small-batch`: 작은 배치로 요청 수 자체를 급증시킴
- `single-burst-large-batch`: 요청당 이벤트 수를 키워 serialize/publish 양을 키움
- `double-spike-recovery`: 첫 burst 회복 전후를 포함한 2회 피크
- `mixed-identity-spike`: member/anonymous를 섞은 운영 유사 spike
- `steady-soak`: 낮거나 중간 수준 rate를 장시간 유지하며 publish 실패, lag 누적, executor 포화를 확인

## 주 관측 포인트

- [kafka-pipeline-lag.json](/Users/gy/Study/tasteam-be/monitoring/grafana/dashboards/kafka/kafka-pipeline-lag.json)
- [kafka-connect.json](/Users/gy/Study/tasteam-be/monitoring/grafana/dashboards/kafka/kafka-connect.json)
- [kafka-dlq.json](/Users/gy/Study/tasteam-be/monitoring/grafana/dashboards/kafka/kafka-dlq.json)
- [kafka-cluster.json](/Users/gy/Study/tasteam-be/monitoring/grafana/dashboards/kafka/kafka-cluster.json)
- 서버 로그의 publish timeout, serialization error, broker error

## 실행

```bash
cd loadtest/suites/client-activity-kafka-publish-spike
TEST_TYPE=mixed-identity-spike ./run-client-activity-kafka-publish-spike.sh --no-prometheus
```

```bash
cd loadtest/suites/client-activity-kafka-publish-spike
TEST_TYPE=steady-soak \
SOAK_TARGET_RATE=40 \
SOAK_DURATION=30m \
BATCH_SIZE=2 \
./run-client-activity-kafka-publish-spike.sh --no-prometheus
```
