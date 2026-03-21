# Loadtest

`loadtest/` 아래의 부하테스트 자산을 `shared/` 공통 모듈과 `suites/` 개별 시나리오 폴더로 재정리했습니다.

## 구조

- [`shared/`](shared): k6 공통 시나리오, 메트릭 유틸리티
- [`suites/`](suites): 실행 가능한 부하테스트 단위
- [`seed/`](seed): 부하테스트용 더미 데이터 생성 및 DB 주입 자산
- [`loadtest/.envrc`](.envrc): 공통 환경변수 예시

## 어떤 테스트를 써야 하나

| 폴더 | 목적 | 대략 실행 시간 | 비고 |
|---|---|---:|---|
| [`suites/smoke/`](suites/smoke) | 배포 직후 연결성/기본 API 확인 | 10초 | 테스트 계정과 그룹 시드 필요 |
| [`suites/realistic/`](suites/realistic) | 실제 사용자 여정 기반 통합 부하 | 약 42분 | 기본 추천 시작점 |
| [`suites/breakpoint/`](suites/breakpoint) | SLO 위반이 시작되는 지점 탐색 | 약 5분 | 조회/쓰기 비율 80:20 |
| [`suites/search-stress/`](suites/search-stress) | 검색 API 집중 부하 | 약 6분 | 비로그인 실행 가능 |
| [`suites/restaurant-read-stress/`](suites/restaurant-read-stress) | 음식점 목록/상세 조회 집중 부하 | 약 16분 | AI 요약/상세 읽기 관찰용 |
| [`suites/review-write-stress/`](suites/review-write-stress) | 리뷰 작성 집중 부하 | 약 16분 | 리뷰 작성 후 분석 트리거 관찰용 |
| [`suites/favorite-stress/`](suites/favorite-stress) | 찜 토글/찜 대상 조회 집중 부하 | 약 16분 | 찜 읽기/쓰기 분리 검증용 |
| [`suites/client-activity-db-store-stress/`](suites/client-activity-db-store-stress) | 클라이언트 이벤트 DB 저장 루트 집중 부하 | 약 16분 | `analytics/events` DB 저장 내구성 검증용 |
| [`suites/client-activity-kafka-publish-spike/`](suites/client-activity-kafka-publish-spike) | 클라이언트 이벤트 Kafka publish spike | 약 5분 | `analytics/events` 저장 후 MQ/Connect 피크 관찰용 |
| [`suites/stress/`](suites/stress) | read-heavy / write-heavy / search-only 지속 부하 | 약 20분 | `TEST_TYPE` 선택 |
| [`suites/spike/`](suites/spike) | 순간 급증 트래픽 대응 확인 | 약 3분 | `SPIKE_TARGET` 선택 |
| [`suites/recovery/`](suites/recovery) | 스파이크 후 회복력 관찰 | 약 16분 | 저부하 복구 구간 포함 |
| [`suites/soak/`](suites/soak) | realistic 여정 기반 24h/48h 장기 부하 | 24h / 48h | `SOAK_MODE`, `CACHE_MODE` 사용 |
| [`suites/phase1/`](suites/phase1) | 1차 병목 탐색 및 Cache/Kafka 비교 | suite별 상이 | 그룹/서브그룹/채팅방 필수 |
| [`suites/phase1-soak/`](suites/phase1-soak) | phase1 시나리오 기반 24h/48h 장기 Soak | 24h / 48h | Cache on/off 비교용 |
| [`suites/locust/`](suites/locust) | Locust 기반 사용자 여정 시뮬레이션 | 기본 35분 | k6가 아닌 Locust 사용 |

## 사용 규칙

- suite 폴더 안에는 실행 스크립트, 엔트리 테스트 스크립트, 해당 테스트 설명용 `README.md`만 둡니다.
- 공통 함수는 [`shared/`](shared)에서만 관리합니다.
- 새 테스트를 추가할 때도 `suites/<suite-name>/` 아래에 넣고, 반드시 폴더별 `README.md`를 함께 작성합니다.
- 시드/SQL 자산은 [`seed/`](seed)에서만 관리합니다.

## 데이터 시드

- 로그인 기반 suite를 돌리기 전에는 [`seed/README.md`](seed/README.md)의 표준 절차를 따릅니다.
- 표준 기준은 `generate_dummy_seed_sql.py + default_seed_profile.json`입니다.
- 기본 seed에는 `test-user-001` 계정군, 그룹 `2002`, 서브그룹 `4002`, 채팅방 bootstrap이 포함됩니다.
- `join_type=PASSWORD` 그룹의 가입 코드는 기본적으로 `1234`이며, DB에는 bcrypt 해시로 저장됩니다.
- 비동기 로그성 테이블까지 포함한 대용량 seed가 기본 프리셋에 포함됩니다.
- 생성 결과물은 `loadtest/results/generated-seed/...` 아래에만 둡니다.

## 공통 환경변수

- `BASE_URL`: 기본 대상 서버
- `TEST_GROUP_CODE`: 그룹 가입 시도에 사용하는 비밀번호/코드. 기본 시드 기준값은 `1234`
- `GROUP_SEARCH_KEYWORDS`: 가입 후보 그룹을 찾을 검색 키워드 목록
- `TEST_GROUP_ID`: 고정 그룹 직접 가입이 필요한 예외 스크립트용 식별자
- `TEST_RESTAURANT_ID`: 예외적으로 고정 음식점 ID를 강제해야 할 때만 사용하는 선택값
- `USER_POOL`: 로그인에 사용할 테스트 계정 수

대부분의 suite는 `내 그룹 조회 -> 없으면 그룹 검색 -> 검색 결과 그룹에 TEST_GROUP_CODE로 가입 시도` 흐름을 사용합니다.
필수 그룹 컨텍스트가 필요한 suite는 위 값이 맞지 않으면 `setup()`에서 즉시 중단되도록 fail-fast 동작을 사용합니다.

## 예시

```bash
cd loadtest/suites/realistic
./run-realistic.sh --no-prometheus
```

## 권장 원클릭 실행

매번 동일하게 `smoke -> realistic` 순서로 검증한다면 `loadtest/` 루트 wrapper를 사용하세요.

```bash
cd loadtest
./run-smoke-realistic.sh --no-prometheus
```

실행 순서:

1. `suites/smoke/run-smoke.sh`
2. smoke 성공 시 `suites/realistic/run-realistic.sh`
3. smoke 실패 시 realistic는 실행하지 않고 즉시 종료

실행 산출물은 기본적으로 `loadtest/results/smoke-realistic/<run-id>/` 아래에 저장됩니다.

- `smoke.log`
- `smoke-summary.json`
- `realistic.log`
- `realistic-summary.json`

결과 루트를 직접 바꾸고 싶다면:

```bash
cd loadtest
./run-smoke-realistic.sh --results-dir ./results/manual-runs
```

기존 공통 환경변수는 그대로 적용됩니다.

- `BASE_URL`
- `TEST_GROUP_CODE`
- `GROUP_SEARCH_KEYWORDS`
- `USER_POOL`
- `REVERSE_GEOCODE_MODE`


## BE 내부 최초 부하테스트 환경 세팅

다음은 BE 저장소에서 부하테스트를 바로 실행하기 위한 1회 세팅 체크리스트입니다.

```bash
cd ./loadtest

# 1) 종속성 확인
./setup.sh

# 2) 시드 생성 준비 (파이썬은 표준 라이브러리만 사용)
cd seed
python3 generate_dummy_seed_sql.py --print-config

# 3) 시나리오 실행 (예시)
cd ../suites/smoke
./run-smoke.sh --no-prometheus
```

필요 시 `setup.sh`에서 설치할 툴(예: k6, locust) 확인 로그를 기준으로 로컬 패키지를 따로 깔아주세요.
