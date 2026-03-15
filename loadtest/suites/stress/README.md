# Stress

지속적인 고부하 상황을 재현하는 read-heavy / write-heavy / search-only 테스트입니다.

## 목적

- 특정 부하 유형에서 시스템 한계를 찾습니다.
- 조회 중심, 쓰기 중심, 검색 중심 패턴을 나눠서 병목을 비교할 수 있습니다.

## 특성

- 실행 시간: 약 20분
- `TEST_TYPE=read-heavy|write-heavy|search-only`
- `USER_POOL`로 로그인 사용자 풀 크기 조정 가능
- read-heavy는 browsing/searching 혼합, write-heavy는 리뷰/채팅/즐겨찾기 중심입니다.

## 전제조건

- 테스트 계정 로그인 가능
- 그룹/서브그룹/채팅방 또는 대체 가능한 데이터가 준비되어 있어야 합니다.
- 그룹 컨텍스트는 `내 그룹 조회 -> 그룹 검색 -> 검색 결과 그룹 가입 시도` 순서로 확보합니다.
- `write-heavy`는 그룹 컨텍스트를 확보하지 못하면 `setup()`에서 즉시 중단됩니다.

## 실행

```bash
cd loadtest/suites/stress
TEST_TYPE=read-heavy ./run-stress.sh --no-prometheus
```

옵션:

- `--reset-db`
- `--no-prometheus`
