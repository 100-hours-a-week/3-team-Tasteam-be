# Recovery

스파이크 이후 시스템이 정상 상태로 얼마나 빨리 돌아오는지 보는 복구 관찰 테스트입니다.

## 목적

- 급부하 후 램프다운 구간에서 read p95와 오류율이 안정화되는지 확인합니다.
- 캐시 워밍, 커넥션 회복, 큐 적체 해소 여부를 볼 때 적합합니다.

## 특성

- 실행 시간: 약 16분
- Phase 1: 50 -> 500 VU 스파이크
- Phase 2: 500 VU 유지
- Phase 3: 500 -> 50 VU 램프다운
- Phase 4: 50 VU 복구 관찰

## 전제조건

- 테스트 계정 로그인 가능
- 그룹/채팅 컨텍스트가 있으면 더 많은 여정을 검증할 수 있습니다.

## 실행

```bash
cd loadtest/suites/recovery
./run-recovery.sh --no-prometheus
```

옵션:

- `--reset-db`
- `--no-prometheus`
