# Phase1 Soak

phase1 통합 여정을 유지한 채 24시간/48시간 장기 부하를 주는 phase1 전용 소크 테스트입니다.

## 목적

- Cache/Kafka on/off 상태에서 phase1 여정이 장시간 안정적으로 유지되는지 비교합니다.
- realistic 기반 소크와 달리 phase1이 요구하는 그룹/서브그룹/채팅방 맥락을 그대로 사용합니다.

## 특성

- 실행 시간: 24h 또는 48h
- 실행 스크립트:
  - `run-long-24h.sh`
  - `run-long-24h-off.sh`
  - `run-long-24h-on.sh`
  - `run-long-48h.sh`
  - `run-long-48h-off.sh`
  - `run-long-48h-on.sh`

## 전제조건

- phase1과 동일하게 그룹/서브그룹/채팅방 데이터가 필수입니다.
- `GROUP_SEARCH_KEYWORDS`로 찾은 그룹 중 적어도 하나에서 `TEST_GROUP_CODE`가 유효해야 합니다.
- 장시간 관측이 가능해야 하며, Prometheus/Grafana 연동을 권장합니다.

## 실행

```bash
cd loadtest/suites/phase1-soak
./run-long-24h-off.sh
```
