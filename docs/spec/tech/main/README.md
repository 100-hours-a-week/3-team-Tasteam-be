| 항목 | 내용 |
|---|---|
| 문서 제목 | 메인(Main) 테크 스펙 |
| 문서 목적 | 메인/홈/AI 추천 화면 API의 데이터 계약, 추천 섹션 구성 원칙, 위치 fallback, 품질 목표를 백엔드 기준으로 고정한다. |
| 작성 및 관리 | Backend Team |
| 최초 작성일 | 2026.02.11 |
| 최종 수정일 | 2026.02.11 |
| 문서 버전 | v2.0 |

<br>

# 메인(Main) - BE 테크스펙

---

# **[1] 배경 (Background)**

## **[1-1] 프로젝트 목표 (Objective)**

메인 화면에서 사용자 위치/취향 맥락에 맞는 레스토랑 섹션을 안정적으로 제공하고, 화면별 API를 분리해 클라이언트 렌더링 비용을 줄인다.

- **핵심 결과 (Key Result) 1:** `GET /api/v1/main` p95 `<= 350ms`
- **핵심 결과 (Key Result) 2:** `GET /api/v1/main/home` p95 `<= 250ms`
- **핵심 결과 (Key Result) 3:** `GET /api/v1/main/ai-recommend` p95 `<= 250ms`
- **핵심 결과 (Key Result) 4:** 섹션 채움률(목표 20개) `>= 99.9%`

## **[1-2] 문제 정의 (Problem)**

- 위치 정보가 없거나 신뢰도가 낮은 요청에서 빈 섹션이 자주 발생하면 홈 진입 경험이 나빠진다.
- 메인 화면 요구가 빠르게 변해도 API 계약이 고정되어 있지 않으면 BE/FE 변경 비용이 커진다.
- 섹션별 쿼리 기준이 문서화되지 않으면 랭킹 회귀(regression)를 탐지하기 어렵다.

## **[1-3] 가설 (Hypothesis)**

섹션 기준/위치 fallback/랜덤 보강 전략을 명시적으로 고정하면, 데이터 부족 상황에서도 안정적인 메인 화면을 제공할 수 있다.

<br>

---

# **[2] 목표가 아닌 것 (Non-goals)**

- 개인화 모델 학습/실시간 피처 엔지니어링 파이프라인 구현
- 광고 서버 연동(스폰서 배너 실시간 입찰)
- A/B 테스트 플랫폼 자체 구축
- 추천 결과 explainability 고도화(UI 실험 포함)

<br>

---

# **[3] 설계 및 기술 자료 (Architecture and Technical Documentation)**

## **[3-1] API 범위**

- `GET /api/v1/main`
  - 메인 화면 통합 응답 (배너 + HOT/NEW/AI)
- `GET /api/v1/main/home`
  - 홈 경량 응답 (NEW/HOT)
- `GET /api/v1/main/ai-recommend`
  - AI 추천 화면 전용 응답

권한:
- 세 API 모두 `PUBLIC`
- 토큰이 있으면 즐겨찾기/그룹 fallback 등 부가 정보 계산에 사용 가능

설계 원칙:
- 메인 화면 렌더링에 필요한 섹션 데이터를 단일 API 호출로 조합해 반환한다.
- 클라이언트 다중 호출 대신 서버 조합 방식을 사용해 네트워크 왕복을 줄인다.

## **[3-2] 요청 파라미터 계약**

| 파라미터 | 타입 | 필수 | 제약 |
|---|---|---:|---|
| `latitude` | `Double` | X | `-90.0 <= latitude <= 90.0` |
| `longitude` | `Double` | X | `-180.0 <= longitude <= 180.0` |

정책:
- 위도/경도는 쌍으로 유효해야 한다.
- 한쪽만 들어오면 위치 없음으로 처리한다.

## **[3-3] 위치 결정 전략**

우선순위:
1. 요청 좌표 (`latitude`, `longitude`)
2. 로그인 사용자의 ACTIVE 그룹 좌표
3. 위치 없음 모드

위치 없음 모드에서도 섹션은 비우지 않고 랜덤 보강으로 채운다.

## **[3-4] 섹션 계약**

| 섹션 타입 | 타이틀 | 기본 개수 | 랭킹 기준 |
|---|---|---:|---|
| `SPONSORED` | Sponsored | 0~N | 운영 배너/광고 정책 |
| `HOT` | 이번주 Hot | 20 | 최근 리뷰/반응 지표 내림차순 |
| `NEW` | 신규 개장 | 20 | 생성일/오픈일 내림차순 |
| `AI_RECOMMEND` | AI 추천 | 20 | AI 분석 점수 내림차순 |

원칙:
- 섹션 간 중복 허용
- 섹션 내부 중복 금지
- 데이터 부족 시 랜덤 보강 허용

## **[3-5] 섹션 채움 알고리즘**

위치 있음:
- 반경 단계 확장: `3km -> 5km -> 10km -> 20km`
- 각 반경에서 섹션 기준 정렬 결과를 누적
- 목표 개수 미달 시 랜덤 쿼리로 보강

위치 없음:
- 섹션별 전체 집합에서 랭킹 조회
- 목표 개수 미달 시 랜덤 보강

보강 규칙:
- `excludeIds`를 사용해 섹션 내부 중복을 차단
- 보강 실패 시(데이터 부족) 가용 데이터만 반환

## **[3-6] 응답 계약**

### `GET /api/v1/main`

```json
{
  "data": {
    "banners": {
      "enabled": false,
      "items": []
    },
    "sections": [
      {
        "type": "HOT",
        "title": "이번주 Hot",
        "items": [
          {
            "restaurantId": 10,
            "name": "식당명",
            "distanceMeter": 870.0,
            "category": "한식",
            "thumbnailImageUrl": "https://cdn...",
            "isFavorite": false,
            "reviewSummary": "요약"
          }
        ]
      }
    ]
  }
}
```

### `GET /api/v1/main/home`

- `sections`만 반환
- 섹션 타입: `NEW`, `HOT`

### `GET /api/v1/main/ai-recommend`

- `section` 단건 반환
- 섹션 타입: `AI_RECOMMEND`

필드 정책:
- `distanceMeter`: 위치 없음 모드에서 `null`
- `thumbnailImageUrl`: 이미지 없으면 `null`
- `reviewSummary`: AI 분석 데이터 없으면 `null`
- `isFavorite`: 로그인 사용자일 때만 계산 가능, 익명은 `false`

## **[3-7] 데이터 의존성**

- `restaurant`
- `restaurant_food_category`
- `ai_restaurant_review_analysis`
- `domain_image` (restaurant 썸네일)
- `group/group_member` (위치 fallback)

## **[3-8] 성능 및 캐싱 정책**

- 쿼리 단위 목표
  - 섹션 메인 조회: `<= 100ms`
  - 부가 데이터(category/thumbnail/summary) 결합: `<= 80ms`
- 캐시 전략(권장)
  - 익명 + 좌표 버킷 기준으로 30~60초 단기 캐시
  - 로그인 사용자 개인화 필드는 캐시 제외 또는 분리 캐시
- Rate limit
  - `main` 계열 API 공통 QPS 제한 적용

## **[3-9] 에러 코드 제안**

| code | status | 의미 |
|---|---:|---|
| `INVALID_LOCATION` | 400 | 좌표 포맷/범위 오류 |
| `MAIN_SECTION_BUILD_FAILED` | 500 | 섹션 구성 실패 |

## **[3-10] 테스트 전략**

- Unit
  - 위치 우선순위 결정 로직 검증
  - 반경 확장/보강 알고리즘 검증
- Repository
  - 섹션별 정렬/limit/중복 제외 쿼리 검증
- Integration
  - 좌표 있음/없음/로그인/비로그인 시 응답 구조 검증
  - 데이터 부족 환경에서 fallback 동작 검증

## **[3-11] 부트스트랩 연계 설계**

메인 화면 품질 검증을 위해 로컬/개발 환경에 최소 데이터 세트가 필요하다.

권장 bootstrap 세트:
- `restaurant` 60건 이상
- `restaurant_food_category` 식당당 최소 1건
- `ai_restaurant_review_analysis` 식당의 50% 이상
- `domain_image` 식당의 70% 이상
- 위치 fallback 테스트용 `group/group_member` 샘플

적용 방식:
- Flyway와 분리된 운영 스크립트 기반 seed
- 자동 실행 대신 명시적 실행(운영 실수 방지)
- 상세 절차: `docs/spec/tech/bootstrap/README.md`

## **[3-12] 오픈 이슈**

1. `SPONSORED` 섹션의 데이터 소스(광고/운영 배너) 확정
2. `isFavorite` 계산을 메인 API에서 실시간 처리할지, 별도 API로 분리할지
3. AI 추천 점수 원본 컬럼 및 정렬 tie-breaker 확정
