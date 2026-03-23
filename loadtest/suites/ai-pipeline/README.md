# AI Pipeline k6 Suite

AI 분석·export·추천 import 병목을 로컬에서 빠르게 확인하기 위한 k6 suite다.

## 목적

- 리뷰 생성이 몰릴 때 AI 후속 처리 병목을 확인한다.
- 음식점/메뉴 raw export 호출 비용을 본다.
- 추천 결과 import 호출 비용을 본다.

## 시나리오

### `review-create-burst`

- 같은 음식점에 리뷰 생성 요청을 집중시킨다.
- 목적: 리뷰 생성 후 AI 분석 경로 병목, 중복 처리, executor 포화 확인

필수 환경변수:

- `TEST_RESTAURANT_ID`

권장 환경변수:

- `BASE_URL`
- `QUICK_RUN_DURATION`

### `raw-export`

- 어드민 raw export API를 1회 호출한다.
- 목적: full snapshot export의 단일 실행 비용과 후속 메트릭 확인

필수 환경변수:

- `ADMIN_USERNAME`
- `ADMIN_PASSWORD`

선택 환경변수:

- `RAW_EXPORT_DT`
- `RAW_EXPORT_TARGETS`

반복 호출이 필요하면 `raw-export-repeat`를 사용한다.

### `recommendation-import`

- 어드민 추천 import API를 1회 호출한다.
- 목적: import의 단일 실행 비용과 import 단계 메트릭 확인

필수 환경변수:

- `ADMIN_USERNAME`
- `ADMIN_PASSWORD`
- `RECOMMENDATION_MODEL_VERSION`
- `RECOMMENDATION_S3_URI`

반복 호출이 필요하면 `recommendation-import-repeat`를 사용한다.

## 실행 예시

리뷰 생성 집중:

```bash
cd loadtest/suites/ai-pipeline
TEST_TYPE=review-create-burst \
BASE_URL=http://127.0.0.1:8080 \
TEST_RESTAURANT_ID=504956 \
QUICK_RUN_DURATION=30s \
./run-ai-pipeline.sh
```

raw export:

```bash
cd loadtest/suites/ai-pipeline
TEST_TYPE=raw-export \
BASE_URL=http://127.0.0.1:8080 \
ADMIN_USERNAME=admin \
ADMIN_PASSWORD=admin \
RAW_EXPORT_TARGETS=RESTAURANTS,MENUS \
./run-ai-pipeline.sh
```

raw export 반복 호출:

```bash
cd loadtest/suites/ai-pipeline
TEST_TYPE=raw-export-repeat \
BASE_URL=http://127.0.0.1:8080 \
ADMIN_USERNAME=admin \
ADMIN_PASSWORD=admin \
RAW_EXPORT_TARGETS=RESTAURANTS,MENUS \
./run-ai-pipeline.sh
```

추천 import:

```bash
cd loadtest/suites/ai-pipeline
TEST_TYPE=recommendation-import \
BASE_URL=http://127.0.0.1:8080 \
ADMIN_USERNAME=admin \
ADMIN_PASSWORD=admin \
RECOMMENDATION_MODEL_VERSION=deepfm-local \
RECOMMENDATION_S3_URI=s3://tasteam-dev-analytics/result/ \
./run-ai-pipeline.sh
```

추천 import 반복 호출:

```bash
cd loadtest/suites/ai-pipeline
TEST_TYPE=recommendation-import-repeat \
BASE_URL=http://127.0.0.1:8080 \
ADMIN_USERNAME=admin \
ADMIN_PASSWORD=admin \
RECOMMENDATION_MODEL_VERSION=deepfm-local \
RECOMMENDATION_S3_URI=s3://tasteam-dev-analytics/result/ \
./run-ai-pipeline.sh
```

## 같이 봐야 할 것

- `ai.initial_analysis.*`
- `ai.vector_upload.*`
- `ai.review_analysis.*`
- `analytics.raw_export.*`
- `recommendation.import.*`
- executor queue / active threads
- DB pool / transaction latency
