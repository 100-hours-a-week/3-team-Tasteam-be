# AI 서버 API 스펙 변경 대응 구현 계획

## 1. 변경사항 요약

| 구분 | 현재 구현 | 새 스펙 | 조치 |
|------|-----------|---------|------|
| **Strength** | `POST /api/v1/llm/comparison` | `POST /api/v1/llm/extract/strengths` | URI 변경 + 요청 DTO 단순화 |
| **Strength 요청** | restaurant_id + category_filter, region_filter, price_band_filter, top_k, max_candidates, months_back | restaurant_id + **top_k**(1~50, 기본 10) 만 | DTO 필드 제거, 호출부 수정 |
| **Summary 단일/배치** | URI·응답 구조 동일 | 단일: 평탄 요청 유지. 배치: 요청에 **min_score** 추가, 항목은 `restaurant_id`만 | 배치 요청 DTO에 min_score 추가, 배치 항목 단순화(선택) |
| **Sentiment 요청** | ReviewContent(id, restaurant_id, content, **created_at**) | id, restaurant_id, content 만 | created_at 제거(직렬화 제외) |
| **Vector Search 요청** | query_text, restaurant_id, limit, **dense_prefetch_limit**, **sparse_prefetch_limit**, **fallback_min_score** | query_text, restaurant_id?, limit, **min_score** | 필드 제거·이름 통일(min_score) |
| **Vector Upload 요청** | ReviewPayload(id, restaurant_id, content, **created_at**) | id?, restaurant_id, content | created_at 제거(선택) |
| **Vector** | `/vector/reviews/upsert`, `/vector/reviews/delete`, `/vector/reviews/delete/batch` | **스펙에 없음** | AiClient에서 해당 메서드 제거 |

---

## 2. 상세 변경 목록

### 2.1 Strength (비교 분석)

- **AiClient**
  - `extractStrengths` 호출 URI: `/api/v1/llm/comparison` → **`/api/v1/llm/extract/strengths`**
- **AiStrengthsRequest**
  - 유지: `restaurant_id`, `top_k`(Integer, 선택)
  - 제거: `categoryFilter`, `regionFilter`, `priceBandFilter`, `maxCandidates`, `monthsBack`
  - 생성자/호출부: `new AiStrengthsRequest(restaurantId, topK)` 형태로 변경 (기본 top_k=10 권장)
- **AiStrengthsResponse**
  - 이미 `strengths`, `total_candidates`, `validated_count`, `category_lift`, `strength_display` 매핑됨. **변경 없음**
- **RestaurantReviewAnalysisService**
  - `extractStrengths(new AiStrengthsRequest(restaurantId, null, null, null, null, null, null))`  
    → `extractStrengths(new AiStrengthsRequest(restaurantId, 10))` (또는 설정값 주입)

### 2.2 Summary

- **단일** `POST /api/v1/llm/summarize`
  - 요청: `restaurant_id`, `limit`, `min_score` — **현재와 동일**
  - 응답: `SummaryDisplayResponse` — **현재와 동일**
  - **변경 없음**
- **배치** `POST /api/v1/llm/summarize/batch`
  - 요청: `restaurants: [{ restaurant_id }, ...]`, **`limit`**(선택), **`min_score`**(선택)
  - **AiSummaryBatchRequest**: 최상위에 **`min_score`**(Double, `@JsonProperty("min_score")`) 추가
  - **AiSummaryBatchItem**: 스펙상 항목은 `restaurant_id`만 있으면 됨. 기존에 `limit` per item 있으면 제거하거나 선택으로 두고, 상위 `limit`만 사용해도 됨

### 2.3 Sentiment

- **AiSentimentRequest.ReviewContent**
  - 새 스펙: `id?`, `restaurant_id`, `content` 만.
  - **`created_at`** 은 스펙에 없으므로 직렬화에서 제외:
    - 옵션 A: `created_at` 필드에 `@JsonIgnore` 적용
    - 옵션 B: API 전용 record `SentimentReviewInput(id, restaurant_id, content)` 를 두고, 서비스에서 `ReviewContent` → `SentimentReviewInput` 변환 후 전송
  - 권장: **옵션 A** 로 기존 `ReviewContent` 유지하고 `@JsonIgnore` 로 `created_at` 제외
- 단일/배치 응답 구조는 스펙과 현재 DTO 일치로 가정. **변경 없음** (필요 시 배치 응답이 `SentimentAnalysisResponse` 리스트인지만 확인)

### 2.4 Vector

- **AiVectorSearchRequest**
  - 유지: `query_text`, `restaurant_id`, `limit`
  - 제거: `densePrefetchLimit`, `sparsePrefetchLimit`
  - 이름 통일: `fallbackMinScore` → **`min_score`** (`@JsonProperty("min_score")` 한 필드로)
- **AiVectorUploadRequest.ReviewPayload**
  - 스펙: `id?`, `restaurant_id`, `content`. **created_at 제거** (필드 삭제 또는 `@JsonIgnore`)
- **AiClient**
  - 아래 세 메서드는 **새 스펙에 없음** → **제거**
    - `upsertVectorReviews`
    - `deleteVectorReview`
    - `deleteVectorReviews`
  - 해당 메서드를 호출하는 코드가 app-api 내에 없으므로 **호출부 수정 없음**
  - 관련 DTO(`AiVectorUpsertRequest/Response`, `AiVectorDeleteRequest/Response`, `AiVectorDeleteBatchRequest/Response`)는 당장 삭제하지 않고, 나중에 정리하거나 deprecated 표시 후 제거 가능

### 2.5 에러 응답

- 새 스펙: 공통 포맷 `code`, `message`, `details`, `request_id`.
- 현재: `AiServerException.from(r, requestId)` 등으로 body 문자열 전달.
- **선택**: 에러 파싱을 `AiErrorResponse` 등으로 통일해 `details`까지 활용할 수 있으나, 기존 처리 유지만 해도 동작에는 문제 없음. **우선 변경 없음**, 필요 시 별도 작업으로 정리.

---

## 3. 구현 순서 제안

1. **DTO 수정 (스펙 정합성)**
   - **AiStrengthsRequest**: 필드 제거 후 `restaurant_id`, `top_k` 만 남기기
   - **AiSummaryBatchRequest**: `min_score` 추가, 배치 항목은 `restaurant_id`만 유지(선택)
   - **AiSentimentRequest.ReviewContent**: `created_at`에 `@JsonIgnore` 적용
   - **AiVectorSearchRequest**: `densePrefetchLimit`, `sparsePrefetchLimit` 제거, `min_score` 단일 필드로 정리
   - **AiVectorUploadRequest.ReviewPayload**: `created_at` 제거 또는 `@JsonIgnore`

2. **AiClient 수정**
   - Strength URI를 `/api/v1/llm/extract/strengths` 로 변경
   - Vector: `upsertVectorReviews`, `deleteVectorReview`, `deleteVectorReviews` 메서드 및 해당 `execute` 호출 제거
   - (필요 시 import 정리)

3. **호출부 수정**
   - **RestaurantReviewAnalysisService**
     - `extractStrengths` 호출 시 `new AiStrengthsRequest(restaurantId, 10)` 사용 (또는 `RestaurantReviewAnalysisPolicyProperties`에 `strengthTopK` 추가 후 주입)

4. **테스트**
   - `RestaurantReviewCreatedAiAnalysisServiceTest` 등 AiClient를 mock하는 테스트에서 `AiStrengthsRequest` 생성자 인자 변경 반영
   - (가능하면) 로컬 또는 스테이징 AI 서버로 Strength/Summary/Sentiment/Vector search·upload 한 번씩 호출 검증

5. **문서/설정 (선택)**
   - `application.yml` 등에 AI 서버 문서 경로 안내 추가: Swagger `{BASE_URL}/docs`, ReDoc `{BASE_URL}/redoc`, OpenAPI `{BASE_URL}/openapi.json`

---

## 4. 체크리스트

- [ ] AiStrengthsRequest: restaurant_id, top_k 만 사용
- [ ] AiClient: extractStrengths URI → `/api/v1/llm/extract/strengths`
- [ ] AiSummaryBatchRequest: min_score 추가
- [ ] AiSentimentRequest.ReviewContent: created_at 직렬화 제외
- [ ] AiVectorSearchRequest: min_score 단일 필드, dense/sparse prefetch 제거
- [ ] AiVectorUploadRequest.ReviewPayload: created_at 제거 또는 제외
- [ ] AiClient: upsertVectorReviews, deleteVectorReview, deleteVectorReviews 제거
- [ ] RestaurantReviewAnalysisService: extractStrengths 호출 인자 수정
- [ ] 단위/통합 테스트 수정 및 실행

위 항목 반영 후, 배포 전에 새 AI 서버 스펙 문서(/docs, /redoc)와 실제 요청/응답을 한 번씩 맞춰 보는 것을 권장합니다.
