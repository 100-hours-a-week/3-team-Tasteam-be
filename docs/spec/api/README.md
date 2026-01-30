| 항목 | 내용 |
|---|---|
| 문서 제목 | Tasteam API 명세서 |
| 문서 목적 | Tasteam 서비스 API 인터페이스 명세 |
| 작성 및 관리 | Backend Team |
| 최초 작성일 | 2026.01.15 |
| 최종 수정일 | 2026.01.15 |
| 문서 버전 | v1.0 |

---

# **[1] 공통 규약**

## **[1-1] 기본 정보**

| 항목 | 내용 |
|---|---|
| Base URL | `<환경별 Base URL>` |
| API Prefix | `/api/v1` (기본, 단 일부 API는 예외) |
| API 문서(공통) | [API 공통 컨벤션 문서](https://github.com/100-hours-a-week/3-team-tasteam-wiki/wiki/%5BBE-%E2%80%90-API%5D-API-%EC%84%A4%EA%B3%84-%EC%BB%A8%EB%B2%A4%EC%85%98-%EC%9D%B8%ED%84%B0%ED%8E%98%EC%9D%B4%EC%8A%A4-%EA%B7%9C%EC%95%BD) |

## **[1-2] 인증/인가**

- **인증 방식:** `Authorization: Bearer {accessToken}`
- **권한 표기:** `PUBLIC / USER / ADMIN`

## **[1-3] 공통 Headers**

| 이름 | 필수 | 예시 | 설명 |
|---|---:|---|---|
| `Content-Type` | 선택 | `application/json; charset=utf-8` | 요청 본문이 JSON인 경우 |
| `Authorization` | 선택 | `Bearer {accessToken}` | 인증이 필요한 경우 |

## **[1-4] 공통 응답 포맷**

### 성공 응답(예시)

```json
{
  "data": {}
}
```

### 에러 응답(예시)

```json
{
  "code": "INVALID_REQUEST",
  "message": "요청 값이 올바르지 않습니다.",
  "errors": []
}
```

---

# **[2] API 목록**

- 소스: `../프로젝트 정의서/API/API_명세_추출_상세_갱신본.json`

- [1. 액세스 토큰 발급 (POST /api/v1/auth/token)](#api-1)
- [2. 액세스 토큰 재발급 (POST /api/v1/auth/token/refresh)](#api-2)
- [3. 로그아웃 (DELETE /api/v1/auth/token)](#api-3)
- [4. 메인페이지 진입 (GET /api/v1/main)](#api-4)
- [5. 통합검색 (POST /api/v1/search)](#api-5)
- [6. 최근 검색어 조회 (GET /api/v1/recent-searches)](#api-6)
- [7. 최근 검색어 삭제 (DELETE /api/v1/recent-searches/{id})](#api-7)
- [8. 음식점 단건 조회 (GET /api/v1/restaurants/{restaurantId})](#api-8)
- [9. 음식점 목록 조회 (GET /api/v1/restaurants)](#api-9)
- [10. 음식점 리뷰 목록 조회 (GET /api/v1/restaurants/{restaurantId}/reviews)](#api-10)
- [11. 음식점 작성 (내부) (POST /api/v1/restaurants)](#api-11)
- [12. 음식점 수정 (내부) (PATCH /api/v1/restaurants/{restaurantId})](#api-12)
- [13. 음식점 작성 (내부) (DELETE /api/v1/restaurants/{restaurantId})](#api-13)
- [14. 리뷰 작성 (POST /api/v1/restaurants/{restaurantId}/reviews)](#api-14)
- [15. 리뷰 단건 조회 (GET /api/v1/reviews/{reviewId})](#api-15)
- [16. 리뷰 삭제 (DELETE /api/v1/reviews/{reviewId})](#api-16)
- [17. 음식점 즐겨찾기 추가 (POST /api/v1/restaurants/{restaurantId}/favorite)](#api-17)
- [18. 그룹 생성 신청 (POST /group-requests)](#api-18)
- [19. 그룹 생성(관리자) (POST /groups)](#api-19)
- [20. 그룹 상세 조회 (GET /groups/{groupId})](#api-20)
- [21. 그룹 정보 수정(관리자) (PATCH /groups/{groupId})](#api-21)
- [22. 그룹 삭제 (DELETE /groups/{groupId})](#api-22)
- [23. 그룹 탈퇴 (DELETE /groups/{groupId}/members/me)](#api-23)
- [24. 그룹 멤버 조회(관리자) (GET /groups/{groupId}/members)](#api-24)
- [25. 그룹 멤버 삭제(관리자) (GET /groups/{groupId}/members/{userId})](#api-25)
- [26. 그룹 리뷰 목록 조회 (GET /groups/{groupId}/reviews)](#api-26)
- [27. 내 그룹 조회 (GET /members/me/groups)](#api-27)
- [28. 내 하위그룹 목록 조회 (GET /members/me/groups/{groupId}/subgroups)](#api-28)
- [29. 하위그룹 목록 조회 (GET /groups/{groupId}/subgroups)](#api-29)
- [30. 하위그룹 상세 조회 (GET /subgroups/{subgroupId})](#api-30)
- [31. 하위그룹 탈퇴 (DELETE /subgroups/{subgroupId}/members/me)](#api-31)
- [32. 하위 그룹 리뷰 전체 조회 (GET /subgroups/{subgroupId}/reviews)](#api-32)
- [33. 하위그룹 생성 (POST /groups/{groupId}/subgroups)](#api-33)
- [34. 내가 찜한 음식점 목록 조회 (GET /members/me/favorites/restaurants)](#api-34)
- [35. 내가 찜 목록에 음식점 등록 (POST /members/me/favorites/restaurants)](#api-35)
- [36. 내 찜 목록에서 음식점 삭제 (DELETE /me/favorites/restaurants/{restaurantId})](#api-36)
- [37. 음식점 찜 상태 조회 (GET /restaurants/{restaurantId}/favorite-status)](#api-37)
- [38. 하위 그룹 찜 목록 조회 (GET /subgroups/{subgroupId}/favorites)](#api-38)
- [39. 하위 그룹 찜 목록에 음식점 등록 (POST /subgroups/{subgroupId}/favorites)](#api-39)
- [40. 하위 그룹 찜 삭제 (DELETE /subgroups/{subgroupId}/favorites/{restaurantId})](#api-40)
- [41. 이메일 인증번호 발송 (POST /groups/{groupId}/email-verifications)](#api-41)
- [42. 이메일 인증번호 검증 후 가입 (POST /groups/{groupId}/email-authentications)](#api-42)
- [43. 비밀번호 검증 후 가입 (POST /groups/{groupId}/password-authentications)](#api-43)
- [44. 채팅 메시지 목록 조회 (GET /chat-rooms/{chatRoomId}/messages)](#api-44)
- [45. 메시지 전송 (POST /chat-rooms/{chatRoomId}/messages)](#api-45)
- [46. 마지막으로 읽은 메세지 아이디 갱신 (PATCH /chat-rooms/{chatRoomId}/read-cursor)](#api-46)
- [47. 알림 목록 조회 (GET /api/v1/members/me/notifications)](#api-47)
- [48. 개별 알림 읽음 (PATCH /api/v1/members/me/notifications/{id})](#api-48)
- [49. 전체 알림 읽음 (PATCH /api/v1/members/me/notifications)](#api-49)
- [50. 읽지 않은 알림 개수 조회 (GET /api/v1/members/me/notifications/unread)](#api-50)
- [51. 알림 선호도 목록 조회 (GET /api/v1/members/me/notification-preferences)](#api-51)
- [52. 알림 선호도 목록 수정 (PUT /api/v1/members/me/notification-preferences)](#api-52)
- [53. 푸시 알림 대상 등록 (POST /api/v1/members/me/push-notification-targets)](#api-53)
- [54. 마이페이지 회원 정보 조회 (GET /api/v1/members/me)](#api-54)
- [55. 회원 탈퇴 (DELETE /api/v1/members/me)](#api-55)
- [56. 회원 정보 수정 (PATCH /api/v1/members/me/profile)](#api-56)
- [57. 회원 그룹 목록 조회 (GET /api/v1/members/me/groups)](#api-57)
- [58. 회원 그룹 요청 목록 조회 (GET /api/v1/members/me/group-requests)](#api-58)
- [59. 회원 음식점 리뷰 목록 조회 (GET /api/v1/members/me/reviews)](#api-59)
- [60. 업로드 권한 생성 (POST /api/v1/uploads)](#api-60)

---

# **[3] API 상세**

<a id="api-1"></a>

## **[3-1] 액세스 토큰 발급 (POST /api/v1/auth/token)**

| 항목 | 내용 |
|---|---|
| 설명 | 인증(auth) - 액세스 토큰 발급 |
| Method | `POST` |
| Path | `/api/v1/auth/token` |
| 인증 | `PUBLIC` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | N | (선택) Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | Y | JSON Body | `application/json; charset=utf-8` |

#### Request Body(선택)

- content-type: `application/json`

| 필드 | 타입 | 필수 | 기본값 | 제약 | 설명 | 예시 |
|---|---|---:|---|---|---|---|
| `provider` | `string` | Y | - | - | - | `KAKAO` |
| `authorizationCode` | `string` | Y | - | - | - | `SplxlOBeZQQYbYS6WxSbIA` |
| `redirectUri` | `string` | Y | - | - | - | `https://frontend.example.com/auth/callback` |

### 요청 예시

```
POST /api/v1/auth/token
Content-Type: application/json

{
    "provider": "KAKAO",
    "authorizationCode": "SplxlOBeZQQYbYS6WxSbIA",
    "redirectUri": "https://frontend.example.com/auth/callback"
}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 200 | 성공 | Response Body 포함 |

#### Response Headers(선택)

| 이름 | 값 | 설명 |
|---|---|---|
| `Set-Cookie` | `refresh_token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...;` | `-` |

#### Response Body(200/201인 경우)

| 필드 | 타입 | Nullable | 설명 | 예시 |
|---|---|---:|---|---|
| `data` | `object` | N | `-` | `-` |

- `data.groups[].logoImage`: object with the file UUID and public URL for each group logo.


```json
{
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

### 응답 예시(에러, 선택)

```json
{
    "code": "INVALID_REQUEST",
    "message": "요청 값이 올바르지 않습니다.",
    "errors": [
        { "field": "provider", "reason": "REQUIRED" }
    ]
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 400 | `INVALID_REQUEST` | `-` |
| 429 | `TOO_MANY_REQUESTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |
| 500 | `OAUTH_TOKEN_ISSUE_FAILED` | `-` |
| 500 | `OAUTH_USER_INFO_FETCH_FAILED` | `-` |

---

<a id="api-2"></a>

## **[3-2] 액세스 토큰 재발급 (POST /api/v1/auth/token/refresh)**

| 항목 | 내용 |
|---|---|
| 설명 | 인증(auth) - 액세스 토큰 재발급 |
| Method | `POST` |
| Path | `/api/v1/auth/token/refresh` |
| 인증 | `PUBLIC` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | N | (선택) Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | N | JSON Body가 있는 경우 | `application/json; charset=utf-8` |

#### Cookies(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `refresh_token` | `string` | Y | Refresh Token | `{jwt}` |

### 요청 예시

```
POST /api/v1/auth/token/refresh
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 201 | 생성 성공 | Response Body 포함 |

### 응답 예시(에러, 선택)

```json
{
    "code": "UNAUTHORIZED",
    "message": "유효하지 않거나 만료된 액세스 토큰입니다",
    "detail": null
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 401 | `UNAUTHORIZED` | `-` |
| 403 | `FORBIDDEN` | `-` |
| 429 | `TOO_MANY_REQUESTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-3"></a>

## **[3-3] 로그아웃 (DELETE /api/v1/auth/token)**

| 항목 | 내용 |
|---|---|
| 설명 | 인증(auth) - 로그아웃 |
| Method | `DELETE` |
| Path | `/api/v1/auth/token` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | N | JSON Body가 있는 경우 | `application/json; charset=utf-8` |

### 요청 예시

```
DELETE /api/v1/auth/token
Authorization: Bearer {accessToken}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 201 | 생성 성공 | Response Body 포함 |

### 응답 예시(에러, 선택)

```json
{
    "code": "UNAUTHORIZED",
    "message": "유효하지 않거나 만료된 액세스 토큰입니다",
    "detail": null
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 401 | `UNAUTHORIZED` | `-` |
| 429 | `TOO_MANY_REQUESTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-4"></a>

## **[3-4] 메인페이지 진입 (GET /api/v1/main)**

| 항목 | 내용 |
|---|---|
| 설명 | 메인페이지(main) - 메인페이지 진입 |
| Method | `GET` |
| Path | `/api/v1/main` |
| 인증 | `PUBLIC` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Query Parameters(선택)

| 이름 | 타입 | 필수 | 기본값 | 제약 | 설명 | 예시 |
|---|---|---:|---|---|---|---|
| `latitude` | `number` | Y | - | - | - | `0` |
| `longitude` | `number` | Y | - | - | - | `0` |

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | N | (선택) Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | N | JSON Body가 있는 경우 | `application/json; charset=utf-8` |

### 요청 예시

```
GET /api/v1/main?latitude=0&longitude=0
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 200 | 성공 | Response Body 포함 |

#### Response Body(200/201인 경우)

| 필드 | 타입 | Nullable | 설명 | 예시 |
|---|---|---:|---|---|
| `data` | `object` | N | `-` | `-` |

```json
{
  "data": {
    "banners": {
      "enabled": true,
      "items": [
        {
          "id": 1,
          "imageUrl": "https://cdn.xxx/banner1.png",
          "landingUrl": "https://event.xxx",
          "order": 1
        }
      ]
    },
    "sections": [
      {
        "type": "SPONSORED",
        "title": "Sponsored",
        "items": [
          {
            "restaurantId": 10,
            "name": "버거킹 판교유스페이스점",
            "distanceMeter": 870,
            "category": "KOR",
            "thumbnailImageUrl": "https://cdn.xxx/r1.png",
            "isFavorite": false,
            "reviewSummary": "손빠르지만 대기시간도 길고 별로 맛없는 집"
          }
        ]
      },
      {
        "type": "HOT",
        "title": "이번주 Hot",
        "items": []
      },
      {
        "type": "NEW",
        "title": "신규 개장",
        "items": []
      },
      {
        "type": "AI_RECOMMEND",
        "title": "AI 추천",
        "items": []
      }
    ]
  }
}
```

### 응답 예시(에러, 선택)

```json
{
  "code": "INVALID_REQUEST",
  "message": "Validation failed",
  "errors": [
    { "field": "keyword", "reason": "REQUIRED" },
    { "field": "size", "reason": "OUT_OF_RANGE" }
  ]
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 400 | `INVALID_REQUEST` | `-` |
| 429 | `TOO_MANY_REQUESTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-5"></a>

## **[3-5] 통합검색 (POST /api/v1/search)**

| 항목 | 내용 |
|---|---|
| 설명 | 검색(search) - 통합검색 |
| Method | `POST` |
| Path | `/api/v1/search` |
| 인증 | `PUBLIC` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Query Parameters(선택)

| 이름 | 타입 | 필수 | 기본값 | 제약 | 설명 | 예시 |
|---|---|---:|---|---|---|---|
| `keyword` | `string` | Y | - | - | - | `string` |
| `cursor` | `string` | N | - | - | - | `string` |
| `size` | `number` | N | - | - | - | `0` |

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | N | (선택) Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | N | JSON Body가 있는 경우 | `application/json; charset=utf-8` |

### 요청 예시

```
POST /api/v1/search?keyword=string&cursor=string&size=0
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 200 | 성공 | Response Body 포함 |

#### Response Body(200/201인 경우)

| 필드 | 타입 | Nullable | 설명 | 예시 |
|---|---|---:|---|---|
| `data` | `object` | N | `-` | `-` |

```json
{
  "data": {
      "groups": [
        {
          "groupId": 1,
          "name": "카카오 판교 크루",
          "logoImage": {
            "id": "a3f1c9e0-7a9b-4e9c-bc2e-1f2c33aa9012",
            "url": "https://cdn.xxx/group1.png"
          },
          "memberCount": 12
        }
      ],
    "restaurants": {
      "items": [
        {
          "restaurantId": 101,
          "name": "버거킹 판교점",
          "address": "성남시 분당구",
          "imageUrl": "https://cdn.xxx/r101.png"
        }
      ],
      "page": {
        "nextCursor": "opaque",
        "size": 10,
        "hasNext": true
      }
    }
  }
}
```

### 응답 예시(에러, 선택)

```json
{
  "code": "INVALID_REQUEST",
  "message": "Validation failed",
  "errors": [
    { "field": "keyword", "reason": "REQUIRED" },
    { "field": "size", "reason": "OUT_OF_RANGE" }
  ]
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 400 | `INVALID_REQUEST` | `-` |
| 429 | `TOO_MANY_REQUESTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-6"></a>

## **[3-6] 최근 검색어 조회 (GET /api/v1/recent-searches)**

| 항목 | 내용 |
|---|---|
| 설명 | 검색 보조(search) - 최근 검색어 조회 |
| Method | `GET` |
| Path | `/api/v1/recent-searches` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | N | JSON Body가 있는 경우 | `application/json; charset=utf-8` |

### 요청 예시

```
GET /api/v1/recent-searches
Authorization: Bearer {accessToken}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 200 | 성공 | Response Body 포함 |

#### Response Body(200/201인 경우)

| 필드 | 타입 | Nullable | 설명 | 예시 |
|---|---|---:|---|---|
| `data` | `array` | N | `-` | `-` |

```json
{
  "data": [
    {
      "id": 1,
      "keyword": "판교",
      "createdAt": "2025-12-28T10:00:00+09:00"
    }
  ]
}
```

### 응답 예시(에러, 선택)

```json
{
    "code": "UNAUTHORIZED",
    "message": "유효하지 않거나 만료된 액세스 토큰입니다",
    "detail": null
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 401 | `UNAUTHORIZED` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-7"></a>

## **[3-7] 최근 검색어 삭제 (DELETE /api/v1/recent-searches/{id})**

| 항목 | 내용 |
|---|---|
| 설명 | 검색 보조(search) - 최근 검색어 삭제 |
| Method | `DELETE` |
| Path | `/api/v1/recent-searches/{id}` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Path Parameters(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `id` | `number` | Y | `-` | `1` |

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | N | JSON Body가 있는 경우 | `application/json; charset=utf-8` |

### 요청 예시

```
DELETE /api/v1/recent-searches/{id}
Authorization: Bearer {accessToken}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 204 | 성공 | Response Body 없음 |

### 응답 예시(에러, 선택)

```json
{
    "code": "UNAUTHORIZED",
    "message": "유효하지 않거나 만료된 액세스 토큰입니다",
    "detail": null
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 401 | `UNAUTHORIZED` | `-` |
| 404 | `RECENT_SEARCH_NOT_FOUND` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-8"></a>

## **[3-8] 음식점 단건 조회 (GET /api/v1/restaurants/{restaurantId})**

| 항목 | 내용 |
|---|---|
| 설명 | 음식점(restaurants) - 음식점 단건 조회 |
| Method | `GET` |
| Path | `/api/v1/restaurants/{restaurantId}` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Path Parameters(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `restaurantId` | `number` | Y | `-` | `1` |

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | N | JSON Body가 있는 경우 | `application/json; charset=utf-8` |

### 요청 예시

```
GET /api/v1/restaurants/{restaurantId}
Authorization: Bearer {accessToken}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 200 | 성공 | Response Body 포함 |

#### Response Body(200/201인 경우)

```
{
  "data": {
    "id": 10,
    "name": "판교 버거킹",
    "address": "경기 성남시 분당구 대왕판교로 660",
    "location": {
      "latitude": 37.395,
      "longitude": 127.11
    },
    "distanceMeter": 870,
    "foodCategories": [
      "패스트푸드",
      "햄버거"
    ],
    "businessHours": [
      {
        "day": "MON",
        "open": "09:00",
        "close": "22:00"
      }
    ],
    "images": [
      {
        "id": "a3f1c9e0-7a9b...",
        "url": "https://cdn.xxx..."
      }
    ],
    "isFavorite": true,   // v2 이후 제거 예정
    "recommendStat": {
      "recommendedCount": 78,
      "notRecommendedCount": 12,
      "positiveRatio": 87
    },
    "aiSummary": "가성비가 좋고....",
    "aiFeatures": "주변 음식점 대비...",
    "createdAt": "2025-12-01T10:15:00Z",
    "updatedAt": "2026-01-12T11:20:00Z"
  }
}
```

### 응답 예시(에러, 선택)

```json
{
    "code": "UNAUTHORIZED",
    "message": "유효하지 않거나 만료된 액세스 토큰입니다",
    "detail": null
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 401 | `UNAUTHORIZED` | `-` |
| 404 | `RESTAURANT_NOT_FOUND` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-9"></a>

## **[3-9] 음식점 목록 조회 (GET /api/v1/restaurants)**

| 항목 | 내용 |
|---|---|
| 설명 | 음식점(restaurants) - 음식점 목록 조회 |
| Method | `GET` |
| Path | `/api/v1/restaurants` |
| 인증 | `PUBLIC` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Query Parameters(선택)

| 이름 | 타입 | 필수 | 기본값 | 제약 | 설명 | 예시 |
|---|---|---:|---|---|---|---|
| `latitude` | `number` | Y | - | - | - | `0` |
| `longitude` | `number` | Y | - | - | - | `0` |
| `cursor` | `string` | N | - | - | - | `string` |
| `size` | `number` | N | - | -; 기본값: 10, 최대 100 | - | `0` |
| `category` | `string` | N | - | - | - | `string` |

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | N | (선택) Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | N | JSON Body가 있는 경우 | `application/json; charset=utf-8` |

### 요청 예시

```
GET /api/v1/restaurants?latitude=0&longitude=0&cursor=string&size=0&category=string
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 200 | 성공 | Response Body 포함 |

#### Response Body(200/201인 경우)

```
{
  "data": [
    {
      "id": 10,
      "name": "판교 버거킹",
      "address": "경기 성남시 분당구",
      "distanceMeter": 870,
      "foodCategories": [
        "패스트푸드"
      ],
      "thumbnailImage": {
        "id": "a3f1c9e0-7a9b...",
        "url": "https://cdn.xxx..."
      },
    }
  ],
  "page": {
    "nextCursor": "opaque...",
    "size": 10,
    "hasNext": true
  }
}
```

### 응답 예시(에러, 선택)

```json
{
    "code": "INTERNAL_SERVER_ERROR",
    "message": "서버에서 에러가 발생했습니다.",
    "detail": null
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-10"></a>

## **[3-10] 음식점 리뷰 목록 조회 (GET /api/v1/restaurants/{restaurantId}/reviews)**

| 항목 | 내용 |
|---|---|
| 설명 | 음식점(restaurants) - 음식점 리뷰 목록 조회 |
| Method | `GET` |
| Path | `/api/v1/restaurants/{restaurantId}/reviews` |
| 인증 | `PUBLIC` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Path Parameters(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `restaurantId` | `number` | Y | `-` | `1` |

#### Query Parameters(선택)

| 이름 | 타입 | 필수 | 기본값 | 제약 | 설명 | 예시 |
|---|---|---:|---|---|---|---|
| `cursor` | `string` | N | - | - | - | `string` |
| `size` | `number` | N | - | -; 기본값: 10, 최대 100 | - | `0` |

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | N | (선택) Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | N | JSON Body가 있는 경우 | `application/json; charset=utf-8` |

### 요청 예시

```
GET /api/v1/restaurants/{restaurantId}/reviews?cursor=string&size=0
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 201 | 생성 성공 | Response Body 포함 |

#### Response Body(200/201인 경우)

| 필드 | 타입 | Nullable | 설명 | 예시 |
|---|---|---:|---|---|
| `data` | `array` | N | `-` | `-` |
| `page` | `object` | N | `-` | `-` |

```json
{
  "data": [
    {
      "id": 555,
      "author": {
        "nickname": "user123"
      },
      "contentPreview": "리뷰 내용 ...",
      "isRecommended": true,
      "keywords": [
        "가성비",
        "점심"
      ],
      "thumbnailImage": {
        "id": "a3f1c9e0-7a9b...",
        "url": "https://cdn.xxx..."
      },
      "createdAt": "2026-01-12T13:10:00Z"
    }
  ],

  "page": {
    "nextCursor": "opaque...",
    "size": 10,
    "hasNext": true
  }
}
```

### 응답 예시(에러, 선택)

```json
{
    "code": "RESTAURANT_NOT_FOUND",
    "message": "음식점을 찾을 수 없습니다",
    "detail": null
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 404 | `RESTAURANT_NOT_FOUND` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-11"></a>

## **[3-11] 음식점 작성 (내부) (POST /api/v1/restaurants)**

| 항목 | 내용 |
|---|---|
| 설명 | 음식점(restaurants) - 음식점 작성 (내부) |
| Method | `POST` |
| Path | `/api/v1/restaurants` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | Y | JSON Body | `application/json; charset=utf-8` |

#### Request Body(선택)

- content-type: `application/json`

| 필드 | 타입 | 필수 | 기본값 | 제약 | 설명 | 예시 |
|---|---|---:|---|---|---|---|
| `foodCategoryIds` | `array` | N | - | - | - | `[]` |
| `imageIds` | `array` | N | - | - | - | `[]` |
| `businissHoures` | `Arrays` | N | - | - | - | `-` |

### 요청 예시

```
POST /api/v1/restaurants
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "name": "판교 버거킹",
  "address": {
    "text": "경기 성남시 분당구 대왕판교로 660",
    "sido": "경기",
    "sigungu": "성남시 분당구",
    "postalCode": "13524"
  },
  "location": {
    "latitude": 37.395,
    "longitude": 127.11
  },
  "foodCategoryIds": [1, 3],
  "imageIds": ["a3f1c9e0-7a9b..."],
  "businessHours": [
    {
      "day": "MON",
      "open": "09:00",
      "close": "22:00"
    }
  ]
}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 200 | 성공 | Response Body 포함 |

#### Response Body(200/201인 경우)

| 필드 | 타입 | Nullable | 설명 | 예시 |
|---|---|---:|---|---|
| `data` | `object` | N | `-` | `-` |

```json
{
  "data": {
    "id": 987,
    "createdAt": "2026-01-10T15:10:30Z"
  }
}
```

### 응답 예시(에러, 선택)

```json
{
  "code": "INVALID_REQUEST",
  "message": "유효하지 않은 요청입니다",
  "errors": [
    { "field": "latitude", "reason": "INVALID" }
  ]
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 400 | `INVALID_REQUEST` | `-` |
| 401 | `UNAUTHORIZED` | `-` |
| 403 | `FORBIDDEN` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-12"></a>

## **[3-12] 음식점 수정 (내부) (PATCH /api/v1/restaurants/{restaurantId})**

| 항목 | 내용 |
|---|---|
| 설명 | 음식점(restaurants) - 음식점 수정 (내부) |
| Method | `PATCH` |
| Path | `/api/v1/restaurants/{restaurantId}` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Path Parameters(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `restaurantId` | `number` | Y | `-` | `1` |

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | Y | JSON Body | `application/json; charset=utf-8` |

#### Request Body(선택)

- content-type: `application/json`

| 필드 | 타입 | 필수 | 기본값 | 제약 | 설명 | 예시 |
|---|---|---:|---|---|---|---|
| `foodCategoryIds` | `array` | N | - | - | - | `[]` |
| `imageIds` | `array` | N | - | - | - | `[]` |
| `businissHoures` | `Arrays` | N | - | - | - | `-` |

### 요청 예시

```
PATCH /api/v1/restaurants/{restaurantId}
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "name": "판교 버거킹",
  "address": {
    "text": "경기 성남시 분당구 대왕판교로 660",
    "sido": "경기",
    "sigungu": "성남시 분당구",
    "postalCode": "13524"
  },
  "location": {
    "latitude": 37.395,
    "longitude": 127.11
  },
  "foodCategoryIds": [1, 3],
  "imageIds": ["a3f1c9e0-7a9b..."],
  "businessHours": [
    {
      "day": "MON",
      "open": "09:00",
      "close": "22:00"
    }
  ]
}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 200 | 성공 | Response Body 포함 |

#### Response Body(200/201인 경우)

```
{
  "data": {
    "id": 987,
    "createdAt": "2026-01-10T15:10:30Z"
    "updatedAt": "2026-01-10T16:40:00Z"
  }
}
```

### 응답 예시(에러, 선택)

```json
{
  "code": "INVALID_REQUEST",
  "message": "유효하지 않은 요청입니다",
  "errors": [
    { "field": "latitude", "reason": "INVALID" }
  ]
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 400 | `INVALID_REQUEST` | `-` |
| 401 | `UNAUTHORIZED` | `-` |
| 403 | `FORBIDDEN` | `-` |
| 404 | `RESTAURANT_NOT_FOUND` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-13"></a>

## **[3-13] 음식점 작성 (내부) (DELETE /api/v1/restaurants/{restaurantId})**

| 항목 | 내용 |
|---|---|
| 설명 | 음식점(restaurants) - 음식점 작성 (내부) |
| Method | `DELETE` |
| Path | `/api/v1/restaurants/{restaurantId}` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Path Parameters(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `restaurantId` | `number` | Y | `-` | `1` |

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | N | JSON Body가 있는 경우 | `application/json; charset=utf-8` |

### 요청 예시

```
DELETE /api/v1/restaurants/{restaurantId}
Authorization: Bearer {accessToken}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 204 | 성공 | Response Body 없음 |

### 응답 예시(에러, 선택)

```json
{
    "code": "UNAUTHORIZED",
    "message": "유효하지 않거나 만료된 액세스 토큰입니다",
    "detail": null
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 401 | `UNAUTHORIZED` | `-` |
| 403 | `FORBIDDEN` | `-` |
| 404 | `RESTAURANT_NOT_FOUND` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-14"></a>

## **[3-14] 리뷰 작성 (POST /api/v1/restaurants/{restaurantId}/reviews)**

| 항목 | 내용 |
|---|---|
| 설명 | 리뷰(review) - 리뷰 작성 |
| Method | `POST` |
| Path | `/api/v1/restaurants/{restaurantId}/reviews` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Path Parameters(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `restaurantId` | `number` | Y | `-` | `1` |

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | Y | JSON Body | `application/json; charset=utf-8` |

#### Request Body(선택)

- content-type: `application/json`

| 필드 | 타입 | 필수 | 기본값 | 제약 | 설명 | 예시 |
|---|---|---:|---|---|---|---|
| `restaurantId` | `number` | N | - | - | - | `10` |
| `groupId` | `number` | N | - | - | - | `1` |
| `subgroupId` | `number` | N | - | - | - | `1` |
| `content` | `string` | N | - | - | - | `리뷰 내용 ...` |
| `isRecommended` | `boolean` | N | - | - | - | `True` |
| `keywordIds` | `array` | N | - | - | - | `[]` |
| `imageIds` | `array` | N | - | - | - | `[]` |

### 요청 예시

```
POST /api/v1/restaurants/{restaurantId}/reviews
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "restaurantId": 10,
  "groupId": 1,
  "subgroupId": 1,
  "content": "리뷰 내용 ...",
  "isRecommended": true,
  "keywordIds": [
    3,7,12
  ],
  "imageIds": [
    "a3f1c9e0-7a9b...",
    "b92d77c4-12a2..."
  ]
}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 201 | 생성 성공 | Response Body 포함 |

#### Response Body(200/201인 경우)

| 필드 | 타입 | Nullable | 설명 | 예시 |
|---|---|---:|---|---|
| `data` | `object` | N | `-` | `-` |

```json
{
  "data": {
    "id": 987,
    "createdAt": "2026-01-10T15:10:30Z"
  }
}
```

### 응답 예시(에러, 선택)

```json
{
  "code": "INVALID_REQUEST",
  "message": "Validation failed",
  "errors": [
    {
      "field": "content",
      "reason": "LENGTH_EXCEEDED"
    }
  ]
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 400 | `INVALID_REQUEST` | `-` |
| 401 | `UNAUTHORIZED` | `-` |
| 404 | `RESTAURANT_NOT_FOUND` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-15"></a>

## **[3-15] 리뷰 단건 조회 (GET /api/v1/reviews/{reviewId})**

| 항목 | 내용 |
|---|---|
| 설명 | 리뷰(review) - 리뷰 단건 조회 |
| Method | `GET` |
| Path | `/api/v1/reviews/{reviewId}` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Path Parameters(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `reviewId` | `number` | Y | `-` | `1` |

#### Query Parameters(선택)

| 이름 | 타입 | 필수 | 기본값 | 제약 | 설명 | 예시 |
|---|---|---:|---|---|---|---|
| `latitude` | `number` | Y | - | - | - | `0` |
| `longitude` | `number` | Y | - | - | - | `0` |
| `cursor` | `string` | N | - | - | - | `string` |
| `size` | `number` | N | - | -; 기본값: 10, 최대 100 | - | `0` |
| `category` | `string` | N | - | - | - | `string` |

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | N | JSON Body가 있는 경우 | `application/json; charset=utf-8` |

### 요청 예시

```
GET /api/v1/reviews/{reviewId}?latitude=0&longitude=0&cursor=string&size=0&category=string
Authorization: Bearer {accessToken}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 200 | 성공 | Response Body 포함 |

#### Response Body(200/201인 경우)

| 필드 | 타입 | Nullable | 설명 | 예시 |
|---|---|---:|---|---|
| `data` | `object` | N | `-` | `-` |

```json
{
  "data": {
    "id": 555,
    "restaurant": {
      "id": 10,
      "name": "판교 버거킹"
    },
    "author": {
      "id": 3,
      "nickname": "user123"
    },
    "content": "리뷰 내용 ...",
    "isRecommended": true,
    "keywords": [
      "가성비", "점심식사", "회전율 빠름"
    ],
    "images": [
      {
        "id": "a3f1c9e0-7a9b...",
        "url": "https://cdn.xxx..."
      }
    ],
    "createdAt": "2026-01-12T13:10:00Z",
    "updatedAt": "2026-01-12T13:10:00Z"
  }
}
```

### 응답 예시(에러, 선택)

```json
{
    "code": "UNAUTHORIZED",
    "message": "유효하지 않거나 만료된 액세스 토큰입니다",
    "detail": null
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 401 | `UNAUTHORIZED` | `-` |
| 404 | `RESTAURANT_NOT_FOUND` | `-` |
| 409 | `FAVORITE_ALREADY_EXISTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-16"></a>

## **[3-16] 리뷰 삭제 (DELETE /api/v1/reviews/{reviewId})**

| 항목 | 내용 |
|---|---|
| 설명 | 리뷰(review) - 리뷰 삭제 |
| Method | `DELETE` |
| Path | `/api/v1/reviews/{reviewId}` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Path Parameters(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `reviewId` | `number` | Y | `-` | `1` |

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | N | JSON Body가 있는 경우 | `application/json; charset=utf-8` |

### 요청 예시

```
DELETE /api/v1/reviews/{reviewId}
Authorization: Bearer {accessToken}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 204 | 성공 | Response Body 없음 |

### 응답 예시(에러, 선택)

```json
{
    "code": "UNAUTHORIZED",
    "message": "유효하지 않거나 만료된 액세스 토큰입니다",
    "detail": null
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 401 | `UNAUTHORIZED` | `-` |
| 404 | `RESTAURANT_NOT_FOUND` | `-` |
| 404 | `REVIEW_NOT_FOUND` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-17"></a>

## **[3-17] 음식점 즐겨찾기 추가 (POST /api/v1/restaurants/{restaurantId}/favorite)**

| 항목 | 내용 |
|---|---|
| 설명 | 찜(favorite)() - 음식점 즐겨찾기 추가 |
| Method | `POST` |
| Path | `/api/v1/restaurants/{restaurantId}/favorite` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Path Parameters(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `restaurantId` | `number` | Y | `-` | `1` |

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | N | JSON Body가 있는 경우 | `application/json; charset=utf-8` |

### 요청 예시

```
POST /api/v1/restaurants/{restaurantId}/favorite
Authorization: Bearer {accessToken}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 201 | 생성 성공 | Response Body 포함 |

#### Response Body(200/201인 경우)

| 필드 | 타입 | Nullable | 설명 | 예시 |
|---|---|---:|---|---|
| `data` | `object` | N | `-` | `-` |

```json
{
  "data": {
    "id": 1234
  }
}
```

### 응답 예시(에러, 선택)

```json
{
    "code": "UNAUTHORIZED",
    "message": "유효하지 않거나 만료된 액세스 토큰입니다"
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 401 | `UNAUTHORIZED` | `-` |
| 404 | `RESTAURANT_NOT_FOUND` | `-` |
| 409 | `FAVORITE_ALREADY_EXISTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-18"></a>

## **[3-18] 그룹 생성 신청 (POST /group-requests)**

| 항목 | 내용 |
|---|---|
| 설명 | 그룹(group) - 그룹 생성 신청 |
| Method | `POST` |
| Path | `/group-requests` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | Y | JSON Body | `application/json; charset=utf-8` |

### 요청 예시

```
POST /group-requests
Authorization: Bearer {accessToken}
Content-Type: application/json

{
    "memberId": 10
    "name": "카카오",
    "joinType": "WORK-EMAIL",
    "email": "user@kakao.com",
    "address": {
      "address": "경기 성남시 분당구 판교역로 166",
      "detailAddress": null,
      "postalCode": "13529"
    }
}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 201 | 생성 성공 | Response Body 포함 |

#### Response Body(200/201인 경우)

```
{
  "data": {
    "id": 9876,
    "status": "PENDING",
    "createdAt": "2026-01-11T02:21:10+09:00",
  }
}
```

### 응답 예시(에러, 선택)

```json
{
    "code": "UNAUTHORIZED",
    "message": "유효하지 않거나 만료된 액세스 토큰입니다"
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 401 | `UNAUTHORIZED` | `-` |
| 409 | `REQUEST_ALREADY_EXISTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-19"></a>

## **[3-19] 그룹 생성(관리자) (POST /groups)**

| 항목 | 내용 |
|---|---|
| 설명 | 그룹(group) - 그룹 생성(관리자) |
| Method | `POST` |
| Path | `/groups` |
| 인증 | `ADMIN` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | Y | JSON Body | `application/json; charset=utf-8` |

### 요청 예시

```
POST /groups
Authorization: Bearer {accessToken}
Content-Type: application/json

{
    "name": "카카오 부트캠프",
    "logoImageId": "a3f1c9e0-7a9b-4e9c-bc2e-1f2c33aa9012",
    "address": "경기 성남시 분당구 판교역로 166",
    "detailAddress": null,
    "location": {
      "latitude": 37.401219,
      "longitude": 127.108622
    },
    joinType: "PASSWORD",
    "emailDomain": null
}
```

- `logoImageId`: 업로드 API(`/api/v1/files/uploads/presigned`)로 생성된 파일 UUID를 전달합니다.

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 201 | 생성 성공 | Response Body 포함 |

#### Response Body(200/201인 경우)

```
{
  "data": {
    "id": 98761,
    "status": "ACTIVE",
    "createdAt": "2026-01-11T02:21:10+09:00",
  }
}
```

### 응답 예시(에러, 선택)

```json
{
    "code": "UNAUTHORIZED",
    "message": "유효하지 않거나 만료된 액세스 토큰입니다"
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 401 | `UNAUTHORIZED` | `-` |
| 404 | `GROUP_NOT_FOUND` | `-` |
| 409 | `ALREADY_EXISTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-20"></a>

## **[3-20] 그룹 상세 조회 (GET /groups/{groupId})**

| 항목 | 내용 |
|---|---|
| 설명 | 그룹(group) - 그룹 상세 조회 |
| Method | `GET` |
| Path | `/groups/{groupId}` |
| 인증 | `PUBLIC` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Path Parameters(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `groupId` | `number` | Y | `-` | `1` |

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | N | (선택) Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | N | JSON Body가 있는 경우 | `application/json; charset=utf-8` |

### 요청 예시

```
GET /groups/{groupId}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 200 | 성공 | Response Body 포함 |

#### Response Body(200/201인 경우)

```
{
  "data": {
    "groupId": 10,
    "name": "카카오 부트캠프",
    "logoImage": {
      "id": "a3f1c9e0-7a9b-4e9c-bc2e-1f2c33aa9012",
      "url": "https://cdn.xxx..."
    },
    "address": "경기 성남시 분당구 대왕판교로 660" ,
    "detail_address": "유스페이스 1 A동 405호",
    "emailDomain": null,
    "status": active,
    "createdAt": "2026-01-01T10:00:00+09:00",
    "updatedAt": "2026-01-10T11:20:00+09:00",
  }
}
```

### 응답 예시(에러, 선택)

```json
{
  "code": "GROUP_NOT_FOUND",
  "message": "해당 그룹을 찾을 수 없습니다."
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 404 | `GROUP_NOT_FOUND` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-21"></a>

## **[3-21] 그룹 정보 수정(관리자) (PATCH /groups/{groupId})**

| 항목 | 내용 |
|---|---|
| 설명 | 그룹(group) - 그룹 정보 수정(관리자) |
| Method | `PATCH` |
| Path | `/groups/{groupId}` |
| 인증 | `ADMIN` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Path Parameters(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `groupId` | `number` | Y | `-` | `1` |

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | Y | JSON Body | `application/json; charset=utf-8` |

#### Request Body(선택)

- content-type: `application/json`

| 필드 | 타입 | 필수 | 기본값 | 제약 | 설명 | 예시 |
|---|---|---:|---|---|---|---|
| `name` | `string` | N | - | - | - | `새 그룹명` |
| `imageIds` | `string` | N | - | - | - | `a3f1c9e0-7a9b...` |
| `address` | `string` | N | - | - | - | `경기 성남시 분당구 대왕판교로 660` |
| `detail_address` | `string` | N | - | - | - | `유스페이스 1 A동 405호` |
| `emailDomain` | `null` | N | - | - | - | `-` |
| `status` | `string` | N | - | - | - | `ACTIVE` |

### 요청 예시

```
PATCH /groups/{groupId}
Authorization: Bearer {accessToken}
Content-Type: application/json

{
    "name": "새 그룹명",
    "imageIds": "a3f1c9e0-7a9b...",
    "address": "경기 성남시 분당구 대왕판교로 660" ,
    "detail_address": "유스페이스 1 A동 405호",
    "emailDomain": null,
    "status": "ACTIVE"
}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 201 | 생성 성공 | Response Body 포함 |

### 응답 예시(에러, 선택)

```json
{
    "code": "UNAUTHORIZED",
    "message": "유효하지 않거나 만료된 액세스 토큰입니다"
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 401 | `UNAUTHORIZED` | `-` |
| 404 | `GROUP_NOT_FOUND` | `-` |
| 409 | `ALREADY_EXISTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-22"></a>

## **[3-22] 그룹 삭제 (DELETE /groups/{groupId})**

| 항목 | 내용 |
|---|---|
| 설명 | 그룹(group) - 그룹 삭제 |
| Method | `DELETE` |
| Path | `/groups/{groupId}` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Path Parameters(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `groupId` | `number` | Y | `-` | `1` |

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | N | JSON Body가 있는 경우 | `application/json; charset=utf-8` |

### 요청 예시

```
DELETE /groups/{groupId}
Authorization: Bearer {accessToken}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 200 | 성공 | Response Body 포함 |

### 응답 예시(에러, 선택)

```json
{
    "code": "UNAUTHORIZED",
    "message": "유효하지 않거나 만료된 액세스 토큰입니다"
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 401 | `UNAUTHORIZED` | `-` |
| 404 | `GROUP_NOT_FOUND` | `-` |
| 409 | `ALREADY_EXISTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-23"></a>

## **[3-23] 그룹 탈퇴 (DELETE /groups/{groupId}/members/me)**

| 항목 | 내용 |
|---|---|
| 설명 | 그룹(group) - 그룹 탈퇴 |
| Method | `DELETE` |
| Path | `/groups/{groupId}/members/me` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Path Parameters(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `groupId` | `number` | Y | `-` | `1` |

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | N | JSON Body가 있는 경우 | `application/json; charset=utf-8` |

### 요청 예시

```
DELETE /groups/{groupId}/members/me
Authorization: Bearer {accessToken}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 200 | 성공 | Response Body 포함 |

### 응답 예시(에러, 선택)

```json
{
    "code": "UNAUTHORIZED",
    "message": "유효하지 않거나 만료된 액세스 토큰입니다"
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 401 | `UNAUTHORIZED` | `-` |
| 404 | `GROUP_NOT_FOUND` | `-` |
| 409 | `ALREADY_EXISTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-24"></a>

## **[3-24] 그룹 멤버 조회(관리자) (GET /groups/{groupId}/members)**

| 항목 | 내용 |
|---|---|
| 설명 | 그룹(group) - 그룹 멤버 조회(관리자) |
| Method | `GET` |
| Path | `/groups/{groupId}/members` |
| 인증 | `ADMIN` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Path Parameters(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `groupId` | `number` | Y | `-` | `1` |

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | N | JSON Body가 있는 경우 | `application/json; charset=utf-8` |

### 요청 예시

```
GET /groups/{groupId}/members
Authorization: Bearer {accessToken}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 200 | 성공 | Response Body 포함 |

#### Response Body(200/201인 경우)

```
{
  "data": [
    {
      "memberId": 5,
      "nickname": "세이",
      "profileImage": {
        "id": "a3f1c9e0-7a9b-4e9c-bc2e-1f2c33aa9012",
        "url": "https://cdn.xxx..."
      },
      "createdAt": "2026-01-02T09:00:00+09:00"
    },
    {
      "memberId": 8,
      "nickname": "데브온",
      "profileImage": {
        "id": "b4f1c9e0-7a9b-4e9c-bc2e-1f2c33aa9012",
        "url": "https://cdn.xxx..."
      },
      "createdAt": "2026-01-03T09:00:00+09:00"
    }
  ],
  "page": {
    "nextCursor": "opaque",
    "size": 20,
    "hasNext": true
  }
}
```

### 응답 예시(에러, 선택)

```json
{
    "code": "UNAUTHORIZED",
    "message": "유효하지 않거나 만료된 액세스 토큰입니다"
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 401 | `UNAUTHORIZED` | `-` |
| 404 | `RESOURCE_NOT_FOUND` | `-` |
| 409 | `ALREADY_EXISTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-25"></a>

## **[3-25] 그룹 멤버 삭제(관리자) (GET /groups/{groupId}/members/{userId})**

| 항목 | 내용 |
|---|---|
| 설명 | 그룹(group) - 그룹 멤버 삭제(관리자) |
| Method | `GET` |
| Path | `/groups/{groupId}/members/{userId}` |
| 인증 | `ADMIN` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Path Parameters(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `groupId` | `number` | Y | `-` | `1` |
| `userId` | `number` | Y | `-` | `1` |

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | N | JSON Body가 있는 경우 | `application/json; charset=utf-8` |

### 요청 예시

```
GET /groups/{groupId}/members/{userId}
Authorization: Bearer {accessToken}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 200 | 성공 | Response Body 포함 |

### 응답 예시(에러, 선택)

```json
{
    "code": "UNAUTHORIZED",
    "message": "유효하지 않거나 만료된 액세스 토큰입니다"
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 401 | `UNAUTHORIZED` | `-` |
| 404 | `USER_NOT_FOUND` | `-` |
| 409 | `ALREADY_EXISTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-26"></a>

## **[3-26] 그룹 리뷰 목록 조회 (GET /groups/{groupId}/reviews)**

| 항목 | 내용 |
|---|---|
| 설명 | 그룹(group) - 그룹 리뷰 목록 조회 |
| Method | `GET` |
| Path | `/groups/{groupId}/reviews` |
| 인증 | `PUBLIC` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Path Parameters(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `groupId` | `number` | Y | `-` | `1` |

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | N | (선택) Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | N | JSON Body가 있는 경우 | `application/json; charset=utf-8` |

### 요청 예시

```
GET /groups/{groupId}/reviews
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 200 | 성공 | Response Body 포함 |

#### Response Body(200/201인 경우)

| 필드 | 타입 | Nullable | 설명 | 예시 |
|---|---|---:|---|---|
| `data` | `array` | N | `-` | `-` |
| `page` | `object` | N | `-` | `-` |

```json
{
  "data": [
    {
      "id": 555,
      "author": {
        "nickname": "user123"
      },
      "contentPreview": "리뷰 내용 ...",
      "isRecommended": true,
      "keywords": [
        "가성비",
        "점심"
      ],
      "thumbnailImage": {
        "id": "a3f1c9e0-7a9b...",
        "url": "https://cdn.xxx..."
      },
      "createdAt": "2026-01-12T13:10:00Z"
    }
  ],

  "page": {
    "nextCursor": "opaque...",
    "size": 20,
    "hasNext": true
  }
}
```

### 응답 예시(에러, 선택)

```json
{
  "code": "RESOURCE_NOT_FOUND",
  "message": "리소스를 찾을 수 없습니다."
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 404 | `RESOURCE_NOT_FOUND` | `-` |
| 409 | `ALREADY_EXISTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-27"></a>

## **[3-27] 내 그룹 조회 (GET /members/me/groups)**

| 항목 | 내용 |
|---|---|
| 설명 | 그룹(group) - 내 그룹 조회 |
| Method | `GET` |
| Path | `/members/me/groups` |
| 인증 | `PUBLIC` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | N | (선택) Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | N | JSON Body가 있는 경우 | `application/json; charset=utf-8` |

### 요청 예시

```
GET /members/me/groups
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 200 | 성공 | Response Body 포함 |

#### Response Body(200/201인 경우)

```
{
  "data": [
   {
       "groupId": 10,
       "name": "카카오 부트캠프",
       "logoImage": {
         "id": "a3f1c9e0-7a9b-4e9c-bc2e-1f2c33aa9012",
         "url": "https://cdn.xxx..."
       },
       "address": "경기 성남시 분당구 대왕판교로 660" ,
       "detailAddress": "유스페이스 1 A동 405호",
       "emailDomain": null,
       "status": "ACTIVE",
       "createdAt": "2026-01-01T10:00:00+09:00",
       "updatedAt": "2026-01-10T11:20:00+09:00",
       "subgroups":[
        { 
            "subgroupId": 77,
            "name": "판교 저녁팟",
            "description": "판교 근처 저녁 맛집 공유",
            "memberCount": 12,
            "thumbnailImage": {
                "id": "a3f1c9e0-7a9b...",
                "url": "https://cdn.xxx..."
              },
            "createdAt": "2026-01-05T10:00:00+09:00"
        }
       ]
   }
    
       .
       .
       .
    ],
       "page": {
         "sort": "NAME_ASC_ID_ASC",
         "nextCursor":"opaque",
         "size": 20,
         "hasNext": true
     }
}
```

### 응답 예시(에러, 선택)

```json
{
  "code": "RESOURCE_NOT_FOUND",
  "message": "리소스를 찾을 수 없습니다."
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 404 | `RESOURCE_NOT_FOUND` | `-` |
| 409 | `ALREADY_EXISTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-28"></a>

## **[3-28] 내 하위그룹 목록 조회 (GET /members/me/groups/{groupId}/subgroups)**

| 항목 | 내용 |
|---|---|
| 설명 | 그룹(group) - 내 하위그룹 목록 조회 |
| Method | `GET` |
| Path | `/members/me/groups/{groupId}/subgroups` |
| 인증 | `PUBLIC` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Path Parameters(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `groupId` | `number` | Y | `-` | `1` |

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | N | (선택) Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | N | JSON Body가 있는 경우 | `application/json; charset=utf-8` |

### 요청 예시

```
GET /members/me/groups/{groupId}/subgroups
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 200 | 성공 | Response Body 포함 |

#### Response Body(200/201인 경우)

```
{
  "data": [
    {
      "subgroupId": 77,
      "name": "판교 저녁팟",
      "description": "판교 근처 저녁 맛집 공유",
      "memberCount": 12,
      "thumnailImage": {
        "id": "a3f1c9e0-7a9b...",
        "url": "https://cdn.xxx..."
      },
      "createdAt": "2026-01-05T10:00:00+09:00"
    }
    .
    .
    .
 ],
    "page": {
      "sort": "NAME_ASC_ID_ASC",
      "nextCursor": "opaque...",
      "size": 20,
      "hasNext": true
  }
}
```

### 응답 예시(에러, 선택)

```json
{
  "code": "RESOURCE_NOT_FOUND",
  "message": "리소스를 찾을 수 없습니다."
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 404 | `RESOURCE_NOT_FOUND` | `-` |
| 409 | `ALREADY_EXISTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-29"></a>

## **[3-29] 하위그룹 목록 조회 (GET /groups/{groupId}/subgroups)**

| 항목 | 내용 |
|---|---|
| 설명 | 하위그룹(subgroup) - 하위그룹 목록 조회 |
| Method | `GET` |
| Path | `/groups/{groupId}/subgroups` |
| 인증 | `PUBLIC` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Path Parameters(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `groupId` | `number` | Y | `-` | `1` |

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | N | (선택) Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | N | JSON Body가 있는 경우 | `application/json; charset=utf-8` |

### 요청 예시

```
GET /groups/{groupId}/subgroups
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 200 | 성공 | Response Body 포함 |

#### Response Body(200/201인 경우)

```
{
  "data": [
    {
      "subgroupId": 77,
      "name": "판교 저녁팟",
      "description": "판교 근처 저녁 맛집 공유",
      "memberCount": 12,
      "thumnailImage": {
        "id": "a3f1c9e0-7a9b...",
        "url": "https://cdn.xxx..."
      },
      "createdAt": "2026-01-05T10:00:00+09:00"
    }
    .
    .
    .
 ],
    "page": {
      "sort": "NAME_ASC_ID_ASC",
      "nextCursor": "opaque...",
      "size": 20,
      "hasNext": true
  }
}
```

### 응답 예시(에러, 선택)

```json
{
  "code": "RESOURCE_NOT_FOUND",
  "message": "리소스를 찾을 수 없습니다."
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 404 | `RESOURCE_NOT_FOUND` | `-` |
| 409 | `ALREADY_EXISTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-30"></a>

## **[3-30] 하위그룹 상세 조회 (GET /subgroups/{subgroupId})**

| 항목 | 내용 |
|---|---|
| 설명 | 하위그룹(subgroup) - 하위그룹 상세 조회 |
| Method | `GET` |
| Path | `/subgroups/{subgroupId}` |
| 인증 | `PUBLIC` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Path Parameters(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `subgroupId` | `number` | Y | `-` | `1` |

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | N | (선택) Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | N | JSON Body가 있는 경우 | `application/json; charset=utf-8` |

### 요청 예시

```
GET /subgroups/{subgroupId}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 200 | 성공 | Response Body 포함 |

#### Response Body(200/201인 경우)

| 필드 | 타입 | Nullable | 설명 | 예시 |
|---|---|---:|---|---|
| `data` | `object` | N | `-` | `-` |

```json
{
  "data": {
      "groupId": 77,
      "subgroupId": 1077,
      "name": "판교 저녁팟",
      "description": "판교 근처 저녁 맛집 공유",
      "memberCount": 12,
      "thumnailImage": {
        "id": "a3f1c9e0-7a9b...",
        "url": "https://cdn.xxx..."
      },
      "createdAt": "2026-01-05T10:00:00+09:00"
  }

}
```

### 응답 예시(에러, 선택)

```json
{
  "code": "GROUP_NOT_FOUND",
  "message": "해당 그룹을 찾을 수 없습니다."
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 404 | `GROUP_NOT_FOUND` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-31"></a>

## **[3-31] 하위그룹 탈퇴 (DELETE /subgroups/{subgroupId}/members/me)**

| 항목 | 내용 |
|---|---|
| 설명 | 하위그룹(subgroup) - 하위그룹 탈퇴 |
| Method | `DELETE` |
| Path | `/subgroups/{subgroupId}/members/me` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Path Parameters(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `subgroupId` | `number` | Y | `-` | `1` |

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | N | JSON Body가 있는 경우 | `application/json; charset=utf-8` |

### 요청 예시

```
DELETE /subgroups/{subgroupId}/members/me
Authorization: Bearer {accessToken}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 200 | 성공 | Response Body 포함 |

### 응답 예시(에러, 선택)

```json
{
    "code": "UNAUTHORIZED",
    "message": "유효하지 않거나 만료된 액세스 토큰입니다"
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 401 | `UNAUTHORIZED` | `-` |
| 404 | `GROUP_NOT_FOUND` | `-` |
| 409 | `ALREADY_EXISTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-32"></a>

## **[3-32] 하위 그룹 리뷰 전체 조회 (GET /subgroups/{subgroupId}/reviews)**

| 항목 | 내용 |
|---|---|
| 설명 | 하위그룹(subgroup) - 하위 그룹 리뷰 전체 조회 |
| Method | `GET` |
| Path | `/subgroups/{subgroupId}/reviews` |
| 인증 | `PUBLIC` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Path Parameters(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `subgroupId` | `number` | Y | `-` | `1` |

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | N | (선택) Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | N | JSON Body가 있는 경우 | `application/json; charset=utf-8` |

### 요청 예시

```
GET /subgroups/{subgroupId}/reviews
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 200 | 성공 | Response Body 포함 |

#### Response Body(200/201인 경우)

| 필드 | 타입 | Nullable | 설명 | 예시 |
|---|---|---:|---|---|
| `data` | `array` | N | `-` | `-` |
| `page` | `object` | N | `-` | `-` |

```json
{
  "data": [
    {
      "id": 555,
      "author": {
        "nickname": "user123"
      },
      "contentPreview": "리뷰 내용 ...",
      "isRecommended": true,
      "keywords": [
        "가성비",
        "점심"
      ],
      "thumbnailImage": {
        "id": "a3f1c9e0-7a9b...",
        "url": "https://cdn.xxx..."
      },
      "createdAt": "2026-01-12T13:10:00Z"
    }
  ],

  "page": {
    "nextCursor": "opaque...",
    "size": 20,
    "hasNext": true
  }
}
```

### 응답 예시(에러, 선택)

```json
{
  "code": "RESOURCE_NOT_FOUND",
  "message": "리소스를 찾을 수 없습니다."
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 404 | `RESOURCE_NOT_FOUND` | `-` |
| 409 | `ALREADY_EXISTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-33"></a>

## **[3-33] 하위그룹 생성 (POST /groups/{groupId}/subgroups)**

| 항목 | 내용 |
|---|---|
| 설명 | 하위그룹(subgroup) - 하위그룹 생성 |
| Method | `POST` |
| Path | `/groups/{groupId}/subgroups` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Path Parameters(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `groupId` | `number` | Y | `-` | `1` |

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | Y | JSON Body | `application/json; charset=utf-8` |

#### Request Body(선택)

- content-type: `application/json`

| 필드 | 타입 | 필수 | 기본값 | 제약 | 설명 | 예시 |
|---|---|---:|---|---|---|---|
| `name` | `string` | N | - | - | - | `카카오 부트캠프` |
| `description` | `string` | N | - | - | - | `설명...` |
| `profileImageId` | `string` | N | - | - | - | `a3f1c9e0-...` |
| `joinType` | `string` | N | - | - | - | `PASSWORD` |
| `groupPassword` | `string` | N | - | - | - | `1234` |

### 요청 예시

```
POST /groups/{groupId}/subgroups
Authorization: Bearer {accessToken}
Content-Type: application/json

{
    "name": "카카오 부트캠프",
    "description": "설명...",
    "profileImageId": "a3f1c9e0-...",
    "joinType": "PASSWORD",
    "groupPassword": "1234"
}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 200 | 성공 | Response Body 포함 |

#### Response Body(200/201인 경우)

| 필드 | 타입 | Nullable | 설명 | 예시 |
|---|---|---:|---|---|
| `data` | `object` | N | `-` | `-` |

```json
{
        "data":{
                "id":123,
                "createdAt": "2026-01-09T11:06:00+09:00"
        }
}
```

### 응답 예시(에러, 선택)

```json
{
    "code": "UNAUTHORIZED",
    "message": "유효하지 않거나 만료된 액세스 토큰입니다"
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 401 | `UNAUTHORIZED` | `-` |
| 404 | `GROUP_NOT_FOUND` | `-` |
| 409 | `ALREADY_EXISTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-34"></a>

## **[3-34] 내가 찜한 음식점 목록 조회 (GET /members/me/favorites/restaurants)**

| 항목 | 내용 |
|---|---|
| 설명 | 찜(favorite) - 내가 찜한 음식점 목록 조회 |
| Method | `GET` |
| Path | `/members/me/favorites/restaurants` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | N | JSON Body가 있는 경우 | `application/json; charset=utf-8` |

### 요청 예시

```
GET /members/me/favorites/restaurants
Authorization: Bearer {accessToken}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 200 | 성공 | Response Body 포함 |

#### Response Body(200/201인 경우)

```
{
  "data": [
    {
      "restaurantId": 101,
      "name": "국밥집",
      "thumbnailUrl": "https://cdn.example.com/restaurants/101.jpg",
      "createdAt": "2026-01-11T17:05:10+09:00"
    },
      .
      .
      .
  ],
  "page": {
    "nextCursor": "opaque",
    "size": 20,
    "hasNext": true
  }
}
```

### 응답 예시(에러, 선택)

```json
{
    "code": "UNAUTHORIZED",
    "message": "유효하지 않거나 만료된 액세스 토큰입니다"
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 401 | `UNAUTHORIZED` | `-` |
| 404 | `RESTAURANT_NOT_FOUND` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-35"></a>

## **[3-35] 내가 찜 목록에 음식점 등록 (POST /members/me/favorites/restaurants)**

| 항목 | 내용 |
|---|---|
| 설명 | 찜(favorite) - 내가 찜 목록에 음식점 등록 |
| Method | `POST` |
| Path | `/members/me/favorites/restaurants` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | Y | JSON Body | `application/json; charset=utf-8` |

#### Request Body(선택)

- content-type: `application/json`

| 필드 | 타입 | 필수 | 기본값 | 제약 | 설명 | 예시 |
|---|---|---:|---|---|---|---|
| `restaurantId` | `number` | N | - | - | - | `1234` |

### 요청 예시

```
POST /members/me/favorites/restaurants
Authorization: Bearer {accessToken}
Content-Type: application/json

{
    "restaurantId": 1234
}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 201 | 생성 성공 | Response Body 포함 |

#### Response Body(200/201인 경우)

| 필드 | 타입 | Nullable | 설명 | 예시 |
|---|---|---:|---|---|
| `data` | `object` | N | `-` | `-` |

```json
{
  "data": {
    "id": 901,               
    "restaurantId": 1234,
    "createdAt": "2026-01-11T17:05:10+09:00"
  }
}
```

### 응답 예시(에러, 선택)

```json
{
    "code": "UNAUTHORIZED",
    "message": "유효하지 않거나 만료된 액세스 토큰입니다"
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 401 | `UNAUTHORIZED` | `-` |
| 404 | `RESTAURANT_NOT_FOUND` | `-` |
| 409 | `FAVORITE_ALREADY_EXISTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-36"></a>

## **[3-36] 내 찜 목록에서 음식점 삭제 (DELETE /me/favorites/restaurants/{restaurantId})**

| 항목 | 내용 |
|---|---|
| 설명 | 찜(favorite) - 내 찜 목록에서 음식점 삭제 |
| Method | `DELETE` |
| Path | `/me/favorites/restaurants/{restaurantId}` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Path Parameters(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `restaurantId` | `number` | Y | `-` | `1` |

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | N | JSON Body가 있는 경우 | `application/json; charset=utf-8` |

### 요청 예시

```
DELETE /me/favorites/restaurants/{restaurantId}
Authorization: Bearer {accessToken}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 200 | 성공 | Response Body 포함 |

### 응답 예시(에러, 선택)

```json
{
    "code": "UNAUTHORIZED",
    "message": "유효하지 않거나 만료된 액세스 토큰입니다"
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 401 | `UNAUTHORIZED` | `-` |
| 403 | `FORBIDDEN` | `-` |
| 404 | `RESTAURANT_NOT_FOUND` | `-` |
| 409 | `FAVORITE_ALREADY_DELETE` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-37"></a>

## **[3-37] 음식점 찜 상태 조회 (GET /restaurants/{restaurantId}/favorite-status)**

| 항목 | 내용 |
|---|---|
| 설명 | 찜(favorite) - 음식점 찜 상태 조회 |
| Method | `GET` |
| Path | `/restaurants/{restaurantId}/favorite-status` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Path Parameters(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `restaurantId` | `number` | Y | `-` | `1` |

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | N | JSON Body가 있는 경우 | `application/json; charset=utf-8` |

### 요청 예시

```
GET /restaurants/{restaurantId}/favorite-status
Authorization: Bearer {accessToken}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 200 | 성공 | Response Body 포함 |

#### Response Body(200/201인 경우)

| 필드 | 타입 | Nullable | 설명 | 예시 |
|---|---|---:|---|---|
| `data` | `object` | N | `-` | `-` |

```json
{
  "data": {
    "restaurantId": 101,
    "my_favorite":{
        "favoriteState": "NOT_FAVORITED"
      },
    "group_favorites": [
      {
        "subgroupId": 1007,
        "subgroupName": "3기 3팀",
        "groupName": "카카오부트캠프",
        "favoriteState": "FAVORITED_BY_ME"
      },
      {
        "subgroupId": 1011,
        "subgroupName": "평냉팟",
        "groupName": "카카오부트캠프",
        "favoriteState": "NOT_FAVORITED"
      }
    ]
  }
}
```

### 응답 예시(에러, 선택)

```json
{
    "code": "UNAUTHORIZED",
    "message": "유효하지 않거나 만료된 액세스 토큰입니다"
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 401 | `UNAUTHORIZED` | `-` |
| 404 | `RESTAURANT_NOT_FOUND` | `-` |
| 409 | `FAVORITE_ALREADY_EXISTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-38"></a>

## **[3-38] 하위 그룹 찜 목록 조회 (GET /subgroups/{subgroupId}/favorites)**

| 항목 | 내용 |
|---|---|
| 설명 | 찜(favorite) - 하위 그룹 찜 목록 조회 |
| Method | `GET` |
| Path | `/subgroups/{subgroupId}/favorites` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Path Parameters(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `subgroupId` | `number` | Y | `-` | `1` |

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | N | JSON Body가 있는 경우 | `application/json; charset=utf-8` |

### 요청 예시

```
GET /subgroups/{subgroupId}/favorites
Authorization: Bearer {accessToken}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 200 | 성공 | Response Body 포함 |

#### Response Body(200/201인 경우)

```
{
  "data": [
    {
      "restaurantId": 101,
      "name": "국밥집",
      "thumbnailUrl": "https://cdn.example.com/restaurants/101.jpg",
      "subgroupId": 12,
      "favoritedAt": "2026-01-11T17:05:10+09:00"
    },
      .
      .
      .
  ],
  "page": {
    "nextCursor": "opaque",
    "size": 20,
    "hasNext": true
  }
}
```

### 응답 예시(에러, 선택)

```json
{
    "code": "UNAUTHORIZED",
    "message": "유효하지 않거나 만료된 액세스 토큰입니다"
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 401 | `UNAUTHORIZED` | `-` |
| 403 | `FORBIDDEN` | `-` |
| 404 | `LIST_NOT_FOUND` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-39"></a>

## **[3-39] 하위 그룹 찜 목록에 음식점 등록 (POST /subgroups/{subgroupId}/favorites)**

| 항목 | 내용 |
|---|---|
| 설명 | 찜(favorite) - 하위 그룹 찜 목록에 음식점 등록 |
| Method | `POST` |
| Path | `/subgroups/{subgroupId}/favorites` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Path Parameters(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `subgroupId` | `number` | Y | `-` | `1` |

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | Y | JSON Body | `application/json; charset=utf-8` |

#### Request Body(선택)

- content-type: `application/json`

| 필드 | 타입 | 필수 | 기본값 | 제약 | 설명 | 예시 |
|---|---|---:|---|---|---|---|
| `restaurantId` | `number` | N | - | - | - | `1234` |

### 요청 예시

```
POST /subgroups/{subgroupId}/favorites
Authorization: Bearer {accessToken}
Content-Type: application/json

{
    "restaurantId": 1234
}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 201 | 생성 성공 | Response Body 포함 |

#### Response Body(200/201인 경우)

| 필드 | 타입 | Nullable | 설명 | 예시 |
|---|---|---:|---|---|
| `data` | `object` | N | `-` | `-` |

```json
{
  "data": {
    "id": 901,               
    "restaurantId": 1234,
    "createdAt": "2026-01-11T17:05:10+09:00"
  }
}
```

### 응답 예시(에러, 선택)

```json
{
    "code": "UNAUTHORIZED",
    "message": "유효하지 않거나 만료된 액세스 토큰입니다"
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 401 | `UNAUTHORIZED` | `-` |
| 403 | `FORBIDDEN` | `-` |
| 404 | `RESTAURANT_NOT_FOUND` | `-` |
| 409 | `FAVORITE_ALREADY_EXISTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-40"></a>

## **[3-40] 하위 그룹 찜 삭제 (DELETE /subgroups/{subgroupId}/favorites/{restaurantId})**

| 항목 | 내용 |
|---|---|
| 설명 | 찜(favorite) - 하위 그룹 찜 삭제 |
| Method | `DELETE` |
| Path | `/subgroups/{subgroupId}/favorites/{restaurantId}` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Path Parameters(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `subgroupId` | `number` | Y | `-` | `1` |
| `restaurantId` | `number` | Y | `-` | `1` |

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | N | JSON Body가 있는 경우 | `application/json; charset=utf-8` |

### 요청 예시

```
DELETE /subgroups/{subgroupId}/favorites/{restaurantId}
Authorization: Bearer {accessToken}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 200 | 성공 | Response Body 포함 |

### 응답 예시(에러, 선택)

```json
{
    "code": "UNAUTHORIZED",
    "message": "유효하지 않거나 만료된 액세스 토큰입니다"
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 401 | `UNAUTHORIZED` | `-` |
| 403 | `FORBIDDEN` | `-` |
| 404 | `RESTAURANT_NOT_FOUND` | `-` |
| 409 | `FAVORITE_ALREADY_DELETED` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-41"></a>

## **[3-41] 이메일 인증번호 발송 (POST /groups/{groupId}/email-verifications)**

| 항목 | 내용 |
|---|---|
| 설명 | 그룹 가입(Join) - 이메일 인증번호 발송 |
| Method | `POST` |
| Path | `/groups/{groupId}/email-verifications` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Path Parameters(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `groupId` | `number` | Y | `-` | `1` |

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | Y | JSON Body | `application/json; charset=utf-8` |

#### Request Body(선택)

- content-type: `application/json`

| 필드 | 타입 | 필수 | 기본값 | 제약 | 설명 | 예시 |
|---|---|---:|---|---|---|---|
| `email` | `string` | N | - | - | - | `user@kakao.com` |

### 요청 예시

```
POST /groups/{groupId}/email-verifications
Authorization: Bearer {accessToken}
Content-Type: application/json

{
    "email": "user@kakao.com"
}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 200 | 성공 | Response Body 포함 |

#### Response Body(200/201인 경우)

| 필드 | 타입 | Nullable | 설명 | 예시 |
|---|---|---:|---|---|
| `data` | `object` | N | `-` | `-` |

```json
{
  "data": {
    "id": 9876,
    "createdAt": "2026-01-11T02:21:10+09:00",
    "expiresAt": "2026-01-12T02:21:10+09:00"
  }
}
```

### 응답 예시(에러, 선택)

```json
{
    "code": "UNAUTHORIZED",
    "message": "유효하지 않거나 만료된 액세스 토큰입니다"
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 401 | `UNAUTHORIZED` | `-` |
| 404 | `GROUP_NOT_FOUND` | `-` |
| 409 | `EMAIL_ALREADY_EXISTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-42"></a>

## **[3-42] 이메일 인증번호 검증 후 가입 (POST /groups/{groupId}/email-authentications)**

| 항목 | 내용 |
|---|---|
| 설명 | 그룹 가입(Join) - 이메일 인증번호 검증 후 가입 |
| Method | `POST` |
| Path | `/groups/{groupId}/email-authentications` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Path Parameters(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `groupId` | `number` | Y | `-` | `1` |

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | Y | JSON Body | `application/json; charset=utf-8` |

#### Request Body(선택)

- content-type: `application/json`

| 필드 | 타입 | 필수 | 기본값 | 제약 | 설명 | 예시 |
|---|---|---:|---|---|---|---|
| `code` | `string` | N | - | - | - | `123456` |

### 요청 예시

```
POST /groups/{groupId}/email-authentications
Authorization: Bearer {accessToken}
Content-Type: application/json

{
    "code": "123456"
}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 201 | 생성 성공 | Response Body 포함 |

#### Response Body(200/201인 경우)

| 필드 | 타입 | Nullable | 설명 | 예시 |
|---|---|---:|---|---|
| `data` | `object` | N | `-` | `-` |

```json
{
  "data": {
    "verified": true,
    "joinedAt": "2026-01-11T02:25:00+09:00"
  }
}
```

### 응답 예시(에러, 선택)

```json
{
    "code": "UNAUTHORIZED",
    "message": "유효하지 않거나 만료된 액세스 토큰입니다"
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 401 | `UNAUTHORIZED` | `-` |
| 404 | `GROUP_NOT_FOUND` | `-` |
| 409 | `EMAIL_CODE_MISMATCH` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-43"></a>

## **[3-43] 비밀번호 검증 후 가입 (POST /groups/{groupId}/password-authentications)**

| 항목 | 내용 |
|---|---|
| 설명 | 그룹 가입(Join) - 비밀번호 검증 후 가입 |
| Method | `POST` |
| Path | `/groups/{groupId}/password-authentications` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Path Parameters(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `groupId` | `number` | Y | `-` | `1` |

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | Y | JSON Body | `application/json; charset=utf-8` |

#### Request Body(선택)

- content-type: `application/json`

| 필드 | 타입 | 필수 | 기본값 | 제약 | 설명 | 예시 |
|---|---|---:|---|---|---|---|
| `code` | `string` | N | - | - | - | `123456` |

### 요청 예시

```
POST /groups/{groupId}/password-authentications
Authorization: Bearer {accessToken}
Content-Type: application/json

{
    "code": "123456"
}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 201 | 생성 성공 | Response Body 포함 |

#### Response Body(200/201인 경우)

| 필드 | 타입 | Nullable | 설명 | 예시 |
|---|---|---:|---|---|
| `data` | `object` | N | `-` | `-` |

```json
{
  "data": {
    "verified": true,
    "joinedAt": "2026-01-11T02:25:00+09:00"
  }
}
```

### 응답 예시(에러, 선택)

```json
{
    "code": "UNAUTHORIZED",
    "message": "유효하지 않거나 만료된 액세스 토큰입니다"
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 401 | `UNAUTHORIZED` | `-` |
| 404 | `GROUP_NOT_FOUND` | `-` |
| 409 | `GROUP_PASSWORD_MISMATCH` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-44"></a>

## **[3-44] 채팅 메시지 목록 조회 (GET /chat-rooms/{chatRoomId}/messages)**

| 항목 | 내용 |
|---|---|
| 설명 | 채팅(chat) - 채팅 메시지 목록 조회 |
| Method | `GET` |
| Path | `/chat-rooms/{chatRoomId}/messages` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Path Parameters(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `chatRoomId` | `number` | Y | `-` | `1` |

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | N | JSON Body가 있는 경우 | `application/json; charset=utf-8` |

### 요청 예시

```
GET /chat-rooms/{chatRoomId}/messages
Authorization: Bearer {accessToken}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 200 | 성공 | Response Body 포함 |

#### Response Body(200/201인 경우)

```
{
        "data":[
         {
               "id": 101, 
               "memberId": 5, 
               "memberNickname": "세이",
               "memberProfileImageUrl": "https://cdn.example.com/profile/5.png"
               "content": "점심 뭐먹죠",
               "messageType": "TEXT", 
              "createdAt": "2026-01-09T11:06:00+09:00"
        },{
               "id": 102, 
               "memberId": 8, 
               "memberNickname": "데브온",
               "memberProfileImageUrl": "https://cdn.example.com/profile/8.png"
               "content": "국밥 어때요",
               "messageType": "TEXT", 
              "createdAt": "2026-01-09T11:06:00+09:00"
        },
             .
             .
             .             
      ], "page": {
               "nextCursor": "opaque", 
               "size": 20, 
               "hasNext": true
}

}
```

### 응답 예시(에러, 선택)

```json
{
    "code": "UNAUTHORIZED",
    "message": "유효하지 않거나 만료된 액세스 토큰입니다"
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 401 | `UNAUTHORIZED` | `-` |
| 403 | `FORBIDDEN` | `-` |
| 404 | `CHAT_ROOM_NOT_FOUND` | `-` |
| 429 | `TOO_MANY_REQUESTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-45"></a>

## **[3-45] 메시지 전송 (POST /chat-rooms/{chatRoomId}/messages)**

| 항목 | 내용 |
|---|---|
| 설명 | 채팅(chat) - 메시지 전송 |
| Method | `POST` |
| Path | `/chat-rooms/{chatRoomId}/messages` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Path Parameters(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `chatRoomId` | `number` | Y | `-` | `1` |

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | Y | JSON Body | `application/json; charset=utf-8` |

#### Request Body(선택)

- content-type: `application/json`

| 필드 | 타입 | 필수 | 기본값 | 제약 | 설명 | 예시 |
|---|---|---:|---|---|---|---|
| `messageType` | `string` | N | - | - | - | `TEXT` |
| `content` | `string` | N | - | - | - | `점심 뭐먹죠` |

### 요청 예시

```
POST /chat-rooms/{chatRoomId}/messages
Authorization: Bearer {accessToken}
Content-Type: application/json

{
          "messageType": "TEXT",
          "content": "점심 뭐먹죠"
}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 201 | 생성 성공 | Response Body 포함 |

#### Response Body(200/201인 경우)

```
{
        "data":{
                "id":123,
                "messageType": "TEXT",
                "content": "점심 뭐먹죠"
                "image": null,
                "createdAt": "2026-01-09T11:06:00+09:00"
        }
}
```

### 응답 예시(에러, 선택)

```json
{
    "code": "UNAUTHORIZED",
    "message": "유효하지 않거나 만료된 액세스 토큰입니다"
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 401 | `UNAUTHORIZED` | `-` |
| 404 | `CHAT_ROOM_NOT_FOUND` | `-` |
| 429 | `TOO_MANY_REQUESTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-46"></a>

## **[3-46] 마지막으로 읽은 메세지 아이디 갱신 (PATCH /chat-rooms/{chatRoomId}/read-cursor)**

| 항목 | 내용 |
|---|---|
| 설명 | 채팅(chat) - 마지막으로 읽은 메세지 아이디 갱신 |
| Method | `PATCH` |
| Path | `/chat-rooms/{chatRoomId}/read-cursor` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Path Parameters(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `chatRoomId` | `number` | Y | `-` | `1` |

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | Y | JSON Body | `application/json; charset=utf-8` |

#### Request Body(선택)

- content-type: `application/json`

| 필드 | 타입 | 필수 | 기본값 | 제약 | 설명 | 예시 |
|---|---|---:|---|---|---|---|
| `lastReadMessageId` | `number` | N | - | - | - | `101` |

### 요청 예시

```
PATCH /chat-rooms/{chatRoomId}/read-cursor
Authorization: Bearer {accessToken}
Content-Type: application/json

{
    "lastReadMessageId": 101
}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 200 | 성공 | Response Body 포함 |

#### Response Body(200/201인 경우)

| 필드 | 타입 | Nullable | 설명 | 예시 |
|---|---|---:|---|---|
| `data` | `object` | N | `-` | `-` |

```json
{
        "data":{
                "roomId": 10,
                "memberId": 5,
                "lastReadMessageId": 101,
                "updatedAt": "2026-01-11T17:05:10+09:00"
        }
}
```

### 응답 예시(에러, 선택)

```json
{
    "code": "UNAUTHORIZED",
    "message": "유효하지 않거나 만료된 액세스 토큰입니다"
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 401 | `UNAUTHORIZED` | `-` |
| 404 | `CHAT_ROOM_NOT_FOUND` | `-` |
| 429 | `TOO_MANY_REQUESTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-47"></a>

## **[3-47] 알림 목록 조회 (GET /api/v1/members/me/notifications)**

| 항목 | 내용 |
|---|---|
| 설명 | 알림(notification) - 알림 목록 조회 |
| Method | `GET` |
| Path | `/api/v1/members/me/notifications` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | N | JSON Body가 있는 경우 | `application/json; charset=utf-8` |

### 요청 예시

```
GET /api/v1/members/me/notifications
Authorization: Bearer {accessToken}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 200 | 성공 | Response Body 포함 |

#### Response Body(200/201인 경우)

| 필드 | 타입 | Nullable | 설명 | 예시 |
|---|---|---:|---|---|
| `data` | `array` | N | `-` | `-` |

```json
{
  "data": [
    {
      "id": 101,
      "notificationType": "CHAT",
      "title": "새 메시지가 도착했습니다",
      "body": "팀 채팅방에 새로운 메시지가 있습니다.",
      "deepLink": "/chat/rooms/12",
      "createdAt": "2026-01-09T10:15:30Z",
      "readAt": null
    }
  ]
}
```

### 응답 예시(에러, 선택)

```json
{
    "code": "AUTHENTICATION_REQUIRED",
    "message": "로그인이 필요한 요청입니다.",
    "detail": null
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 401 | `AUTHENTICATION_REQUIRED` | `-` |
| 403 | `ACCESS_DENIED` | `-` |
| 429 | `TOO_MANY_REQUESTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-48"></a>

## **[3-48] 개별 알림 읽음 (PATCH /api/v1/members/me/notifications/{id})**

| 항목 | 내용 |
|---|---|
| 설명 | 알림(notification) - 개별 알림 읽음 |
| Method | `PATCH` |
| Path | `/api/v1/members/me/notifications/{id}` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Path Parameters(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `id` | `number` | Y | `-` | `1` |

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | Y | JSON Body | `application/json; charset=utf-8` |

#### Request Body(선택)

- content-type: `application/json`

| 필드 | 타입 | 필수 | 기본값 | 제약 | 설명 | 예시 |
|---|---|---:|---|---|---|---|
| `readAt` | `string` | Y | - | - | - | `2026-01-09T19:20:00` |

### 요청 예시

```
PATCH /api/v1/members/me/notifications/{id}
Authorization: Bearer {accessToken}
Content-Type: application/json

{
    "readAt": "2026-01-09T19:20:00"
}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 204 | 성공 | Response Body 없음 |

### 응답 예시(에러, 선택)

```json
{
    "code": "INVALID_REQUEST",
    "message": "요청 값이 올바르지 않습니다.",
    "errors": [
        { "field": "readAt", "reason": "REQUIRED" }
    ]
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 400 | `INVALID_REQUEST` | `-` |
| 401 | `AUTHENTICATION_REQUIRED` | `-` |
| 403 | `ACCESS_DENIED` | `-` |
| 429 | `TOO_MANY_REQUESTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-49"></a>

## **[3-49] 전체 알림 읽음 (PATCH /api/v1/members/me/notifications)**

| 항목 | 내용 |
|---|---|
| 설명 | 알림(notification) - 전체 알림 읽음 |
| Method | `PATCH` |
| Path | `/api/v1/members/me/notifications` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | Y | JSON Body | `application/json; charset=utf-8` |

#### Request Body(선택)

- content-type: `application/json`

| 필드 | 타입 | 필수 | 기본값 | 제약 | 설명 | 예시 |
|---|---|---:|---|---|---|---|
| `readAt` | `string` | Y | - | - | - | `2026-01-09T19:20:00` |

### 요청 예시

```
PATCH /api/v1/members/me/notifications
Authorization: Bearer {accessToken}
Content-Type: application/json

{
    "readAt": "2026-01-09T19:20:00"
}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 204 | 성공 | Response Body 없음 |

### 응답 예시(에러, 선택)

```json
{
    "code": "INVALID_REQUEST",
    "message": "요청 값이 올바르지 않습니다.",
    "errors": [
        { "field": "readAt", "reason": "REQUIRED" }
    ]
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 400 | `INVALID_REQUEST` | `-` |
| 401 | `AUTHENTICATION_REQUIRED` | `-` |
| 403 | `ACCESS_DENIED` | `-` |
| 429 | `TOO_MANY_REQUESTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-50"></a>

## **[3-50] 읽지 않은 알림 개수 조회 (GET /api/v1/members/me/notifications/unread)**

| 항목 | 내용 |
|---|---|
| 설명 | 알림(notification) - 읽지 않은 알림 개수 조회 |
| Method | `GET` |
| Path | `/api/v1/members/me/notifications/unread` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | N | JSON Body가 있는 경우 | `application/json; charset=utf-8` |

### 요청 예시

```
GET /api/v1/members/me/notifications/unread
Authorization: Bearer {accessToken}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 200 | 성공 | Response Body 포함 |

#### Response Body(200/201인 경우)

| 필드 | 타입 | Nullable | 설명 | 예시 |
|---|---|---:|---|---|
| `count` | `number` | N | `-` | `-` |

```json
{
    "count": 10
}
```

### 응답 예시(에러, 선택)

```json
{
    "code": "AUTHENTICATION_REQUIRED",
    "message": "로그인이 필요한 요청입니다.",
    "detail": null
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 401 | `AUTHENTICATION_REQUIRED` | `-` |
| 403 | `ACCESS_DENIED` | `-` |
| 429 | `TOO_MANY_REQUESTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-51"></a>

## **[3-51] 알림 선호도 목록 조회 (GET /api/v1/members/me/notification-preferences)**

| 항목 | 내용 |
|---|---|
| 설명 | 알림(notification) - 알림 선호도 목록 조회 |
| Method | `GET` |
| Path | `/api/v1/members/me/notification-preferences` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | N | JSON Body가 있는 경우 | `application/json; charset=utf-8` |

### 요청 예시

```
GET /api/v1/members/me/notification-preferences
Authorization: Bearer {accessToken}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 200 | 성공 | Response Body 포함 |

#### Response Body(200/201인 경우)

| 필드 | 타입 | Nullable | 설명 | 예시 |
|---|---|---:|---|---|
| `data` | `array` | N | `-` | `-` |

```json
{
  "data": [
    {
      "channel": "PUSH",
      "notificationType": "CHAT",
      "isEnabled": false
    }
  ]
}
```

### 응답 예시(에러, 선택)

```json
{
    "code": "AUTHENTICATION_REQUIRED",
    "message": "로그인이 필요한 요청입니다.",
    "detail": null
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 401 | `AUTHENTICATION_REQUIRED` | `-` |
| 403 | `ACCESS_DENIED` | `-` |
| 429 | `TOO_MANY_REQUESTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-52"></a>

## **[3-52] 알림 선호도 목록 수정 (PUT /api/v1/members/me/notification-preferences)**

| 항목 | 내용 |
|---|---|
| 설명 | 알림(notification) - 알림 선호도 목록 수정 |
| Method | `PUT` |
| Path | `/api/v1/members/me/notification-preferences` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | Y | JSON Body | `application/json; charset=utf-8` |

#### Request Body(선택)

- content-type: `application/json`

| 필드 | 타입 | 필수 | 기본값 | 제약 | 설명 | 예시 |
|---|---|---:|---|---|---|---|
| `notificationPreferences` | `array` | Y | - | - | - | `[]` |

### 요청 예시

```
PUT /api/v1/members/me/notification-preferences
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "notificationPreferences": [
    {
      "channel": "PUSH",
      "notificationType": "CHAT",
      "isEnabled": true
    }
  ]
}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 204 | 성공 | Response Body 없음 |

### 응답 예시(에러, 선택)

```json
{
    "code": "INVALID_REQUEST",
    "message": "요청 값이 올바르지 않습니다.",
    "errors": [
        { "field": "notificationPreferences", "reason": "REQUIRED" }
    ]
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 400 | `INVALID_REQUEST` | `-` |
| 401 | `AUTHENTICATION_REQUIRED` | `-` |
| 403 | `ACCESS_DENIED` | `-` |
| 429 | `TOO_MANY_REQUESTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-53"></a>

## **[3-53] 푸시 알림 대상 등록 (POST /api/v1/members/me/push-notification-targets)**

| 항목 | 내용 |
|---|---|
| 설명 | 알림(notification) - 푸시 알림 대상 등록 |
| Method | `POST` |
| Path | `/api/v1/members/me/push-notification-targets` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | Y | JSON Body | `application/json; charset=utf-8` |

#### Request Body(선택)

- content-type: `application/json`

| 필드 | 타입 | 필수 | 기본값 | 제약 | 설명 | 예시 |
|---|---|---:|---|---|---|---|
| `fcmToken` | `string` | Y | - | - | - | `token` |

### 요청 예시

```
POST /api/v1/members/me/push-notification-targets
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "fcmToken": "token"
}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 204 | 성공 | Response Body 없음 |

### 응답 예시(에러, 선택)

```json
{
    "code": "INVALID_REQUEST",
    "message": "요청 값이 올바르지 않습니다.",
    "errors": [
        { "field": "fcmToken", "reason": "REQUIRED" }
    ]
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 400 | `INVALID_REQUEST` | `-` |
| 401 | `AUTHENTICATION_REQUIRED` | `-` |
| 403 | `ACCESS_DENIED` | `-` |
| 429 | `TOO_MANY_REQUESTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-54"></a>

## **[3-54] 마이페이지 회원 정보 조회 (GET /api/v1/members/me)**

| 항목 | 내용 |
|---|---|
| 설명 | 마이페이지(me) - 마이페이지 회원 정보 조회 |
| Method | `GET` |
| Path | `/api/v1/members/me` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | N | JSON Body가 있는 경우 | `application/json; charset=utf-8` |

### 요청 예시

```
GET /api/v1/members/me
Authorization: Bearer {accessToken}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 200 | 성공 | Response Body 포함 |

#### Response Body(200/201인 경우)

| 필드 | 타입 | Nullable | 설명 | 예시 |
|---|---|---:|---|---|
| `data` | `object` | N | `-` | `-` |

```json
{
  "data": {
    "member": {
      "nickname": "홍길동",
      "profileImage": {
        "id": "a3f1c9e0-7a9b-4e9c-bc2e-1f2c33aa9012",
        "url": "https://..."
      }
    },
    "groupRequests": {
      "data": [
        {
          "id": 10,
          "groupName": "카카오테크부트캠프",
          "groupAddress": "성남시 분당구 대왕판교로",
          "status": "PENDING"
        }
      ],
      "page": {
        "nextCursor": "2019-07-04 17:21:00",
        "size": 1,
        "hasNext": true
      }
    },
    "reviews": {
      "data": [
        {
          "id": 10,
          "restaurantName": "버거킹 판교점",
          "restaurantAddress": "성남시 분당구 대왕판교로",
          "reviewContent": "최고의 맛집. 추천합니다."
        }
      ],
      "page": {
        "nextCursor": "2019-07-04 17:21:00",
        "size": 1,
        "hasNext": true
      }
    }
  }
}
```

### 응답 예시(에러, 선택)

```json
{
    "code": "AUTHENTICATION_REQUIRED",
    "message": "로그인이 필요한 요청입니다.",
    "detail": null
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 401 | `AUTHENTICATION_REQUIRED` | `-` |
| 403 | `ACCESS_DENIED` | `-` |
| 429 | `TOO_MANY_REQUESTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-55"></a>

## **[3-55] 회원 탈퇴 (DELETE /api/v1/members/me)**

| 항목 | 내용 |
|---|---|
| 설명 | 마이페이지(me) - 회원 탈퇴 |
| Method | `DELETE` |
| Path | `/api/v1/members/me` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | N | JSON Body가 있는 경우 | `application/json; charset=utf-8` |

### 요청 예시

```
DELETE /api/v1/members/me
Authorization: Bearer {accessToken}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 204 | 성공 | Response Body 없음 |

### 응답 예시(에러, 선택)

```json
{
    "code": "AUTHENTICATION_REQUIRED",
    "message": "로그인이 필요한 요청입니다.",
    "detail": null
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 401 | `AUTHENTICATION_REQUIRED` | `-` |
| 403 | `ACCESS_DENIED` | `-` |
| 429 | `TOO_MANY_REQUESTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-56"></a>

## **[3-56] 회원 정보 수정 (PATCH /api/v1/members/me/profile)**

| 항목 | 내용 |
|---|---|
| 설명 | 마이페이지(me) - 회원 정보 수정 |
| Method | `PATCH` |
| Path | `/api/v1/members/me/profile` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | Y | JSON Body | `application/json; charset=utf-8` |

#### Request Body(선택)

- content-type: `application/json`

| 필드 | 타입 | 필수 | 기본값 | 제약 | 설명 | 예시 |
|---|---|---:|---|---|---|---|
| `profileImageId` | `string` | N | - | UUID | `a3f1c9e0-7a9b-4e9c-bc2e-1f2c33aa9012` |
| `email` | `string` | N | - | - | - | `example@gmail.com` |

### 요청 예시

```
PATCH /api/v1/members/me/profile
Authorization: Bearer {accessToken}
Content-Type: application/json

{
    "profileImageId": "a3f1c9e0-7a9b-4e9c-bc2e-1f2c33aa9012",
    "email": "example@gmail.com"
}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 204 | 성공 | Response Body 없음 |

### 응답 예시(에러, 선택)

```json
{
    "code": "INVALID_REQUEST",
    "message": "요청 값이 올바르지 않습니다.",
    "errors": [
        { "field": "email", "reason": "INVALID_FORMAT" }
    ]
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 400 | `INVALID_REQUEST` | `-` |
| 401 | `AUTHENTICATION_REQUIRED` | `-` |
| 403 | `ACCESS_DENIED` | `-` |
| 429 | `TOO_MANY_REQUESTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-57"></a>

## **[3-57] 회원 그룹 목록 조회 (GET /api/v1/members/me/groups)**

| 항목 | 내용 |
|---|---|
| 설명 | 마이페이지(me) - 회원 그룹 목록 조회 |
| Method | `GET` |
| Path | `/api/v1/members/me/groups` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Query Parameters(선택)

| 이름 | 타입 | 필수 | 기본값 | 제약 | 설명 | 예시 |
|---|---|---:|---|---|---|---|
| `cursor` | `String` | N | - | - | - | `-` |

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | N | JSON Body가 있는 경우 | `application/json; charset=utf-8` |

### 요청 예시

```
GET /api/v1/members/me/groups?cursor=-
Authorization: Bearer {accessToken}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 200 | 성공 | Response Body 포함 |

#### Response Body(200/201인 경우)

| 필드 | 타입 | Nullable | 설명 | 예시 |
|---|---|---:|---|---|
| `data` | `array` | N | `-` | `-` |
| `page` | `object` | N | `-` | `-` |

```json
{
  "data": [
    {
      "id": 10,
      "groupName": "버거킹 판교점",
      "groupAddress": "성남시 분당구 대왕판교로"
    }
  ],
  "page": {
    "nextCursor": "2019-07-04 17:21:00",
    "size": 1,
    "hasNext": true
  }
}
```

### 응답 예시(에러, 선택)

```json
{
    "code": "AUTHENTICATION_REQUIRED",
    "message": "로그인이 필요한 요청입니다.",
    "detail": null
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 401 | `AUTHENTICATION_REQUIRED` | `-` |
| 403 | `ACCESS_DENIED` | `-` |
| 429 | `TOO_MANY_REQUESTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-58"></a>

## **[3-58] 회원 그룹 요청 목록 조회 (GET /api/v1/members/me/group-requests)**

| 항목 | 내용 |
|---|---|
| 설명 | 마이페이지(me) - 회원 그룹 요청 목록 조회 |
| Method | `GET` |
| Path | `/api/v1/members/me/group-requests` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Query Parameters(선택)

| 이름 | 타입 | 필수 | 기본값 | 제약 | 설명 | 예시 |
|---|---|---:|---|---|---|---|
| `cursor` | `String` | N | - | - | - | `-` |

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | N | JSON Body가 있는 경우 | `application/json; charset=utf-8` |

### 요청 예시

```
GET /api/v1/members/me/group-requests?cursor=-
Authorization: Bearer {accessToken}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 200 | 성공 | Response Body 포함 |

#### Response Body(200/201인 경우)

| 필드 | 타입 | Nullable | 설명 | 예시 |
|---|---|---:|---|---|
| `data` | `array` | N | `-` | `-` |
| `page` | `object` | N | `-` | `-` |

```json
{
  "data": [
    {
      "id": 10,
      "groupName": "버거킹 판교점",
      "groupAddress": "성남시 분당구 대왕판교로",
      "status": "PENDING"
    }
  ],
  "page": {
    "nextCursor": "2019-07-04 17:21:00",
    "size": 1,
    "hasNext": true
  }
}
```

### 응답 예시(에러, 선택)

```json
{
    "code": "AUTHENTICATION_REQUIRED",
    "message": "로그인이 필요한 요청입니다.",
    "detail": null
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 401 | `AUTHENTICATION_REQUIRED` | `-` |
| 403 | `ACCESS_DENIED` | `-` |
| 429 | `TOO_MANY_REQUESTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-59"></a>

## **[3-59] 회원 음식점 리뷰 목록 조회 (GET /api/v1/members/me/reviews)**

| 항목 | 내용 |
|---|---|
| 설명 | 마이페이지(me) - 회원 음식점 리뷰 목록 조회 |
| Method | `GET` |
| Path | `/api/v1/members/me/reviews` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Query Parameters(선택)

| 이름 | 타입 | 필수 | 기본값 | 제약 | 설명 | 예시 |
|---|---|---:|---|---|---|---|
| `cursor` | `String` | N | - | - | - | `-` |

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | N | JSON Body가 있는 경우 | `application/json; charset=utf-8` |

### 요청 예시

```
GET /api/v1/members/me/reviews?cursor=-
Authorization: Bearer {accessToken}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 200 | 성공 | Response Body 포함 |

#### Response Body(200/201인 경우)

| 필드 | 타입 | Nullable | 설명 | 예시 |
|---|---|---:|---|---|
| `data` | `array` | N | `-` | `-` |
| `page` | `object` | N | `-` | `-` |

```json
{
  "data": [
    {
      "id": 10,
      "restaurantName": "버거킹 판교점",
      "restaurantAddress": "성남시 분당구 대왕판교로",
      "reviewContent": "최고의 맛집. 추천합니다."
    }
  ],
  "page": {
    "nextCursor": "2019-07-04 17:21:00",
    "size": 1,
    "hasNext": true
  }
}
```

### 응답 예시(에러, 선택)

```json
{
    "code": "AUTHENTICATION_REQUIRED",
    "message": "로그인이 필요한 요청입니다.",
    "detail": null
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 401 | `AUTHENTICATION_REQUIRED` | `-` |
| 403 | `ACCESS_DENIED` | `-` |
| 429 | `TOO_MANY_REQUESTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

<a id="api-60"></a>

## **[3-60] 업로드 권한 생성 (POST /api/v1/uploads)**

| 항목 | 내용 |
|---|---|
| 설명 | 업로드(uploads) - 업로드 권한 생성 |
| Method | `POST` |
| Path | `/api/v1/uploads` |
| 인증 | `USER` |
| Rate Limit(선택) | `-` |
| 관련 화면/기획(선택) | `-` |

### 요청(Request)

#### Headers(선택)

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---:|---|---|
| `Authorization` | `string` | Y | Bearer 토큰 | `Bearer {accessToken}` |
| `Content-Type` | `string` | Y | JSON Body | `application/json; charset=utf-8` |

#### Request Body(선택)

- content-type: `application/json`

| 필드 | 타입 | 필수 | 기본값 | 제약 | 설명 | 예시 |
|---|---|---:|---|---|---|---|
| `purpose` | `string` | Y | - | - | - | `REVIEW_IMAGE` |
| `files` | `array` | Y | - | - | - | `[]` |

### 요청 예시

```
POST /api/v1/uploads
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "purpose": "REVIEW_IMAGE",
  "files": [
    {
      "contentType": "image/jpeg",
      "size": 123456
    }
  ]
}
```

### 응답(Response)

#### 성공 응답

| HTTP Status | 조건 | 비고 |
|---:|---|---|
| 200 | 성공 | Response Body 포함 |

#### Response Body(200/201인 경우)

| 필드 | 타입 | Nullable | 설명 | 예시 |
|---|---|---:|---|---|
| `uploads` | `array` | N | `-` | `-` |

```json
{
  "uploads": [
    {
      "url": "https://my-bucket.s3.ap-northeast-2.amazonaws.com",
      "fields": {
        "key": "review/123/uuid-1.jpg",
        "policy": "eyJleHBpcmF0aW9uIjoi...",
        "x-amz-algorithm": "AWS4-HMAC-SHA256",
        "x-amz-credential": "AKIA.../20260111/ap-northeast-2/s3/aws4_request",
        "x-amz-date": "20260111T145500Z",
        "x-amz-signature": "abcdef123456",
        "Content-Type": "image/jpeg"
      },
      "expiresAt": "2026-01-11T15:05:00Z",
      "objectKey": "review/123/uuid-1.jpg"
    }
  ]
}
```

### 응답 예시(에러, 선택)

```json
{
    "code": "INVALID_REQUEST",
    "message": "요청 값이 올바르지 않습니다.",
    "errors": [
        { "field": "purpose", "reason": "REQUIRED" },
        { "field": "sortOrder", "reason": "INVALID_VALUE" },
        { "field": "contentType", "reason": "INVALID_FORMAT" }
    ]
}
```

#### 에러 응답

| HTTP Status | code | 발생 조건 |
|---:|---|---|
| 400 | `INVALID_REQUEST` | `-` |
| 401 | `AUTHENTICATION_REQUIRED` | `-` |
| 403 | `ACCESS_DENIED` | `-` |
| 429 | `TOO_MANY_REQUESTS` | `-` |
| 500 | `INTERNAL_SERVER_ERROR` | `-` |

---

# **[4] 변경이력**

| 버전 | 일자 | 작성자 | 변경 내역 | 비고 |
|---|---|---|---|---|
| v1.0 | 2026.01.15 | Backend Team | API 명세서 초안 생성 | - |
