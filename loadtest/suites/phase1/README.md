# Phase1

1차 병목 탐색과 Cache/Kafka on/off 비교를 위한 통합 k6 스위트입니다.

## 목적

- `smoke`, `spike`, `separated`, `mixed`, `breakpoint`, `full` 스위트를 하나의 진입점에서 실행합니다.
- Cache/Kafka 끄기/켜기 비교 실험의 기준선으로 사용합니다.

## 특성

- 엔트리 스크립트: `phase1_test.js`
- 실행 스위트: `TEST_SUITE` 또는 `--suite`
- 보조 실행 스크립트:
  - `run-phase1.sh`
  - `run-phase1-off.sh`
  - `run-phase1-on.sh`
  - `run-cache-compare.sh`

## 전제조건

- 테스트 계정 로그인 가능
- 그룹, 서브그룹, 채팅방 데이터가 반드시 확보되어야 합니다.
- `GROUP_SEARCH_KEYWORDS`로 찾은 그룹 중 적어도 하나에서 `TEST_GROUP_CODE`가 유효해야 합니다.
- 필수 데이터가 없으면 `setup()`에서 즉시 중단됩니다.

## 실행

```bash
cd loadtest/suites/phase1
./run-phase1-off.sh --suite full --no-prometheus
```

환경변수 예시:

```bash
cd loadtest/suites/phase1
GROUP_SEARCH_KEYWORDS=테스트 TEST_GROUP_CODE=LOCAL-1234 USER_POOL=100 ./run-phase1-off.sh --suite full --no-prometheus
```

Cache/Kafka 비교:

```bash
cd loadtest/suites/phase1
./run-cache-compare.sh --suite mixed --no-prometheus
```

비교 실행 결과는 `results/` 아래 summary와 리포트 파일로 생성됩니다.
