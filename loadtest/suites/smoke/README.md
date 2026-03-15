# Smoke

배포 직후 가장 먼저 돌리는 짧은 연결성 확인 테스트입니다.

## 목적

- 테스트 계정 로그인, 그룹 가입, 주요 읽기/쓰기 API가 최소한으로 살아 있는지 확인합니다.
- CI/CD 직후나 운영 반영 직후 빠른 헬스체크 용도로 사용합니다.

## 특성

- 실행 시간: 10초
- 부하: 1 VU
- 그룹, 음식점, 리뷰 키워드 같은 시드 데이터가 있어야 의미 있게 동작합니다.
- 실패한 check가 있으면 threshold 위반으로 비정상 종료됩니다.

## 전제조건

- `POST /api/v1/test/auth/token` 동작
- `GROUP_SEARCH_KEYWORDS`로 찾은 그룹 중 적어도 하나에서 `TEST_GROUP_CODE`가 유효
- main/search에서 조회 가능한 음식점 데이터 유효
- smoke는 그룹 가입 응답 `201`을 기대합니다.

## 실행

```bash
cd loadtest/suites/smoke
./run-smoke.sh
```

환경변수 예시:

```bash
cd loadtest/suites/smoke
GROUP_SEARCH_KEYWORDS=테스트 TEST_GROUP_CODE=LOCAL-1234 ./run-smoke.sh
```

직접 실행도 가능합니다.

```bash
cd loadtest/suites/smoke
BASE_URL=https://stg.tasteam.kr k6 run smoke_test.js
```
