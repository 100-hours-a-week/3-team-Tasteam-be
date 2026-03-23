# Favorite Stress

찜 목록 조회, 찜 대상 조회, 찜 토글을 집중적으로 검증하는 k6 스위트입니다.

## 목적

- 동일 음식점에 대한 찜 추가/삭제 경쟁을 관찰합니다.
- 찜 페이지 진입 시 사용하는 target 조회 API의 읽기 부하를 분리해서 봅니다.
- 내 찜 목록과 서브그룹 찜 목록의 읽기 병목을 별도로 확인합니다.

## 특성

- `TEST_TYPE=toggle-only|targets-read|list-read|mixed`
- `toggle-only`: 찜 추가/삭제 경합 집중
- `targets-read`: 전체 찜 대상/음식점별 찜 대상 조회 집중
- `list-read`: 내 찜 목록/서브그룹 찜 목록 조회 집중
- `mixed`: 찜 대상 조회, 찜 목록 조회, 토글 쓰기를 함께 섞음

## 실행

```bash
cd loadtest/suites/favorite-stress
TEST_TYPE=mixed ./run-favorite-stress.sh --no-prometheus
```
