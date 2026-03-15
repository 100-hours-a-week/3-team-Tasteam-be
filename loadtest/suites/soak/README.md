# Soak

realistic 여정을 기반으로 24시간 또는 48시간 동안 장기 운영 안정성을 보는 소크 테스트입니다.

## 목적

- 메모리 누수, 커넥션 고갈, 장시간 누적 지연 증가를 확인합니다.
- 운영형 시나리오를 오랫동안 유지했을 때의 안정성을 측정합니다.

## 특성

- 실행 시간: 24h 또는 48h
- 시나리오: realistic와 동일한 여정 가중치
- 모드: `SOAK_MODE=24h|48h`
- 태깅: `CACHE_MODE=on|off`

## 전제조건

- 테스트 계정 로그인 가능
- 장시간 Grafana/Prometheus 관측 환경 준비

## 실행

```bash
cd loadtest/suites/soak
SOAK_MODE=24h CACHE_MODE=on ./run-soak.sh
```
