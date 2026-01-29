# Main Page Tech Spec

## 개요

메인 페이지(`GET /api/v1/main`)는 사용자에게 레스토랑 섹션 데이터를 제공한다.
위치 정보는 선택이며, 인증 없이도 접근 가능하다.

## 엔드포인트

```
GET /api/v1/main?latitude={lat}&longitude={lon}
```

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| latitude | Double | 선택 | 위도 (-90.0 ~ 90.0) |
| longitude | Double | 선택 | 경도 (-180.0 ~ 180.0) |

## 위치 결정 로직

위치 정보는 다음 우선순위로 결정된다:

1. **요청 파라미터**: `latitude`, `longitude`가 모두 존재하면 해당 좌표 사용
2. **소속 그룹 위치**: 로그인 유저의 ACTIVE 그룹 중 location이 존재하는 첫 번째 그룹의 좌표 사용
3. **위치 없음**: 위 조건 모두 해당하지 않으면 위치 없이 동작 (랜덤 기반)

## 섹션 구성

| 섹션 | type | title | 쿼리 기준 |
|------|------|-------|-----------|
| Sponsored | SPONSORED | Sponsored | 항상 빈 배열 |
| Hot | HOT | 이번주 Hot | 리뷰 수 내림차순 |
| New | NEW | 신규 개장 | 생성일 내림차순 |
| AI 추천 | AI_RECOMMEND | AI 추천 | 긍정 리뷰 비율 내림차순 |

각 섹션(SPONSORED 제외)은 **20개** 레스토랑 데이터를 반환한다.

## 채움 전략

각 섹션은 독립적으로 20개를 채우며, 섹션 간 중복을 허용한다.

### 위치 있는 경우

반경을 단계적으로 확장하며 조회한다:

```
3km -> 5km -> 10km -> 20km
```

각 단계에서 해당 섹션 기준으로 정렬된 결과를 가져오고, 이미 수집된 항목과 중복되지 않는 것만 추가한다.
모든 반경을 순회한 후에도 20개 미만이면 **랜덤 데이터**로 나머지를 채운다.

### 위치 없는 경우

위치 필터 없이 해당 섹션 기준으로 전체 조회한다.
20개 미만이면 랜덤 데이터로 나머지를 채운다.

## 응답 구조

```json
{
  "data": {
    "banners": {
      "enabled": false,
      "items": []
    },
    "sections": [
      {
        "type": "SPONSORED",
        "title": "Sponsored",
        "items": []
      },
      {
        "type": "HOT",
        "title": "이번주 Hot",
        "items": [
          {
            "restaurantId": 1,
            "name": "레스토랑명",
            "distanceMeter": 1500.0,
            "category": "한식",
            "thumbnailImageUrl": "https://...",
            "isFavorite": false,
            "reviewSummary": "AI 리뷰 요약"
          }
        ]
      }
    ]
  }
}
```

- `distanceMeter`: 위치 없는 경우 `null`
- `isFavorite`: 현재 항상 `false`

## 관련 파일

| 파일 | 역할 |
|------|------|
| `MainController.java` | REST 엔드포인트 |
| `MainService.java` | 위치 결정, 섹션별 조회, 채움 전략 |
| `MainPageRequest.java` | 요청 DTO |
| `MainPageResponse.java` | 응답 DTO |
| `RestaurantRepository.java` | 섹션별 네이티브 쿼리 (PostGIS) |
| `RestaurantSearchPolicy.java` | 반경 확장 상수, 섹션 크기 |
