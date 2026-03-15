# Realistic

실제 사용자 행동 비율을 흉내 내는 기본 통합 부하테스트입니다.

## 목적

- browsing, searching, group, subgroup, personal, chat, writing 여정을 가중치 기반으로 섞어서 실사용에 가까운 부하를 만듭니다.
- 새로 부하테스트를 시작할 때 가장 먼저 선택할 기본 시나리오입니다.

## 특성

- 실행 시간: 약 42분
- 부하: 100 -> 1000 VU
- 기본 역지오코딩 모드: `REVERSE_GEOCODE_MODE=per-vu-once`

## 전제조건

- 테스트 계정 로그인 가능
- 그룹이 없어도 실행은 가능하지만, group/subgroup/chat 커버리지는 줄어듭니다.

## 실행

```bash
cd loadtest/suites/realistic
./run-realistic.sh --no-prometheus
```

주요 환경변수:

- `BASE_URL`
- `REVERSE_GEOCODE_MODE=per-vu-once|per-token-once|always|off`
