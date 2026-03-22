# Review Write Stress

리뷰 작성과 후속 분석 트리거 영향을 집중적으로 검증하는 k6 스위트입니다.

## 목적

- 리뷰 작성 API의 쓰기 성능을 측정합니다.
- 리뷰 생성 직후 after-commit 분석 트리거가 붙는 경로의 영향을 관찰합니다.

## 특성

- 단일 목적 write-heavy 스위트
- `FOLLOWUP_READ_RATIO`로 일부 요청 뒤 음식점 상세 재조회를 섞을 수 있습니다.

## 실행

```bash
cd loadtest/suites/review-write-stress
FOLLOWUP_READ_RATIO=0.2 ./run-review-write-stress.sh --no-prometheus
```

