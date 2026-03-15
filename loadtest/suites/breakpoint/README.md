# Breakpoint

시스템이 SLO를 위반하기 시작하는 구간을 찾는 한계점 탐색 테스트입니다.

## 목적

- 조회 API와 쓰기 API를 동시에 밀어 넣어 임계 구간을 파악합니다.
- 용량 산정 전, 어디서부터 p95와 에러율이 무너지기 시작하는지 확인할 때 사용합니다.

## 특성

- 실행 시간: 약 5분
- 시나리오 비율: 조회 80% / 쓰기 20%
- 최대 부하: 조회 3000 RPS, 쓰기 600 RPS

## 전제조건

- 테스트 계정 로그인 가능
- 리뷰 작성에 필요한 그룹/키워드 데이터 존재
- 필요 시 `--reset-db`로 개발 DB 초기화 가능
- 그룹 컨텍스트는 `내 그룹 조회 -> 그룹 검색 -> 검색 결과 그룹 가입 시도` 순서로 확보합니다.
- 그룹 컨텍스트를 확보하지 못하면 `setup()`에서 즉시 중단됩니다.

## 실행

```bash
cd loadtest/suites/breakpoint
./run-breakpoint.sh --no-prometheus
```

옵션:

- `--reset-db`: 개발 DB 초기화 후 실행
- `--no-prometheus`: Prometheus remote write 비활성화
