# Locust

k6가 아니라 Locust로 사용자 여정을 시뮬레이션하는 대체 부하테스트입니다.

## 목적

- Locust의 사용자 모델과 spawn control을 활용해 장시간 사용자 여정을 흉내 냅니다.
- k6 시나리오와 다른 관점에서 동시 사용자 증가 패턴을 확인할 때 사용합니다.

## 특성

- 엔진: Locust
- 기본값: `USERS=1000`, `SPAWN_RATE=50`, `RUN_TIME=35m`
- 로그인 후 그룹/서브그룹/채팅방 맥락을 확보하려고 시도합니다.
- 검색 호출은 기본적으로 약 491,520 조합 (384 keywords = 336 primary + 48 typing-state, x 320 locations x 4 radii)을 사용합니다.
- 타이핑 중/오타성 검색어는 기본적으로 약 5% 희소 분포로 섞습니다.

## 전제조건

- Locust 설치
- 테스트 계정 로그인 가능
- 그룹 컨텍스트는 `내 그룹 조회 -> 그룹 검색 -> 검색 결과 그룹 가입 시도` 순서로 확보합니다.

## 실행

```bash
cd loadtest/suites/locust
BASE_URL=https://stg.tasteam.kr USERS=1000 SPAWN_RATE=50 RUN_TIME=35m ./run-locust.sh
```
