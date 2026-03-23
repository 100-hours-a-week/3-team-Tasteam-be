# Search Stress

검색 API만 집중적으로 두드리는 검색 전용 고부하 테스트입니다.

## 목적

- 검색 인덱스, 검색 API, 검색 캐시 계층의 병목을 분리해서 확인합니다.
- 로그인/그룹 맥락 없이 검색 경로 자체만 빠르게 압박할 때 적합합니다.

## 특성

- 실행 시간: 약 5분 30초
- 시나리오: 검색 100%
- 최대 부하: 3000 iterations/s 목표
- 비로그인 모드로 실행 가능
- 기본 검색 variation: 약 491,520 조합 (384 keywords = 336 primary + 48 typing-state, x 320 locations x 4 radii)
- 타이핑 중/오타성 검색어는 기본적으로 약 5% 희소 분포로 섞습니다.

## 전제조건

- `/api/v1/search`가 인증 없이 호출 가능하거나 비로그인 정책이 정리되어 있어야 합니다.

## 실행

```bash
cd loadtest/suites/search-stress
./run-search-stress.sh --no-prometheus
```
