# Restaurant Read Stress

음식점 목록/상세/리뷰 조회 경로를 집중적으로 검증하는 k6 스위트입니다.

## 목적

- 음식점 상세 조회에서 AI 요약/비교 데이터 읽기 비용을 집중 관찰합니다.
- 홈/목록 진입 후 상세로 이어지는 읽기 경로의 병목을 분리해서 봅니다.
- 음식점/서브그룹 리뷰 목록 조회의 tail latency를 별도로 확인합니다.

## 특성

- `TEST_TYPE=detail-only|list-then-detail|review-list-heavy`
- `detail-only`: 음식점 상세 + 메뉴 + 리뷰 조회를 반복
- `list-then-detail`: 홈/카테고리 진입 후 노출 음식점 상세로 이어지는 흐름
- `review-list-heavy`: 음식점 리뷰 목록과 서브그룹 리뷰 목록을 큰 페이지 크기로 집중 호출

## 실행

```bash
cd loadtest/suites/restaurant-read-stress
TEST_TYPE=detail-only ./run-restaurant-read-stress.sh --no-prometheus
```
