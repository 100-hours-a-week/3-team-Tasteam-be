# Client Activity DB Store Stress

`POST /api/v1/analytics/events`만 사용해서 `user_activity_event` 저장 루트의 DB 내구성을 확인하는 k6 스위트입니다.

## 목적

- 클라이언트 이벤트 적재량이 늘 때 DB insert 경로가 어디서 먼저 병목이 나는지 확인합니다.
- DB CPU, connection pool, JDBC timeout, slow query 같은 저장 계층 문제를 먼저 드러내는 데 초점을 둡니다.

## 특성

- `TEST_TYPE=store-only-cold|store-hot-identity|store-mixed-realistic|batch-size-sweep`
- `store-only-cold`: anonymous 중심, cold restaurant 비중이 높은 baseline insert
- `store-hot-identity`: member/session/restaurant 쏠림이 큰 쓰기 부하
- `store-mixed-realistic`: hot/cold와 member/anonymous를 섞은 운영 유사 패턴
- `batch-size-sweep`: 배치 크기를 1/5/10/20 식으로 섞어 요청당 이벤트 수 영향 확인

## 주 관측 포인트

- API latency, error rate
- DB CPU, connection pool, commit latency, slow query, lock wait, deadlock
- 앱 CPU, memory, GC
- 서버 로그의 JDBC timeout, connection acquire timeout, insert 실패

## 실행

```bash
cd loadtest/suites/client-activity-db-store-stress
TEST_TYPE=store-mixed-realistic ./run-client-activity-db-store-stress.sh --no-prometheus
```
