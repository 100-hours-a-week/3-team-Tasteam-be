| 항목 | 내용 |
|---|---|
| 문서 제목 | API 설계 컨벤션 / 공통 규약 가이드 |
| 문서 목적 | 본 문서는 Tasteam 서비스의 API 설계 및 구현 시 공통으로 준수해야 할 규약을 정의하여, <br> 일관된 인터페이스 제공과 유지보수 비용 절감을 목적으로 한다. |
| 작성 및 관리 | Backend Team |
| 최초 작성일 | 2026.01.07 |
| 최종 수정일 | 2026.01.14 |
| 문서 버전 | v1.1 |


<br>


## 공통 베이스

- Base URL: `/api/v1`
- Content-Type: `application/json; charset=utf-8`
- Auth Header: `Authorization: Bearer {accessToken}`
- 시간: ISO-8601, UTC 또는 서비스 표준 TZ(+09:00)로 통일
- ID: 숫자형 PK는 응답에 그대로 노출 가능 (`id: number`)

---

# 1. HTTP Method 규약

| Method | 규약 |
| --- | --- |
| GET | 서버 상태 변경 금지, 캐시 가능(정책에 따름) |
| POST | 비멱등, 새 리소스 생성 또는 작업 생성 |
| PUT | 리소스 “전체 상태 교체” (부분 업데이트 목적 사용 금지) |
| PATCH | 전달된 필드만 변경 (JSON Merge Patch, RFC7396 해석) |
| DELETE | 리소스 제거 (Soft delete는 도메인 규칙으로 허용) |

---

# 2. 응답 포맷 표준

## 2.1 단건/비목록 응답 (200)

```json
{
    "data":{}
}
```

## 2.2 생성 응답 (201)

- 원칙: 생성된 리소스의 식별자 반환

```json
{
	"data":{
		"id":123
	}
}
```

## 2.3 본문 없는 성공 (204)

- 삭제/상태변경/토큰폐기 등: `204 No Content`

---

# 3. 에러 응답 표준

## 3.1 기본 에러 포맷

```json
{
	"code":"MEMBER_NOT_FOUND",
	"message":"회원 정보를 찾을 수 없습니다.",
	"errors":{
		"memberId":123
	}
}
```

## 3.2 Validation 에러 확장 포맷

- `400` + `code=INVALID_REQUEST` 고정

```json
{
    "code": "INVALID_REQUEST",
    "message": "요청 값이 올바르지 않습니다.",
    "errors": [
        { "field": "email", "reason": "INVALID_FORMAT" },
        { "field": "size", "reason": "OUT_OF_RANGE" },
        { "field": "readAt", "reason": "REQUIRED" }
    ]
}
```

## 3.3 Status vs Business Error 분리 원칙

- HTTP Status: 프로토콜/리소스 상태
- error.code: 도메인 의미

| 상황 | Status | code |
| --- | --- | --- |
| 리소스 없음 | 404 | *_NOT_FOUND |
| 권한 없음 | 403 | FORBIDDEN / *_NOT_ALLOWED |
| 인증 실패 | 401 | UNAUTHORIZED |
| 비즈니스 충돌 | 409 | *CONFLICT / DUPLICATE* |
| 검증 실패 | 400 | INVALID_REQUEST |

---

# 4. 조회 API 규약 (Pagination / Sorting / Filtering / Search)

## 4.1 Pagination

- **목록 조회 API는 반드시 Pagination 사용** (전체 반환 금지)

### 4.1.1 Page 기반 (기본)

```
GET /api/v1/admin/users?page=1&size=20
```

규칙:

- page는 1부터 시작
- `totalCount` 제공 여부는 API별로 문서에 명시(기본은 미제공)

### 4.1.2 Cursor 기반 (무한 스크롤 UI)

```
GET /api/v1/reviews?cursor=...&size=10
```

규칙:

- `size`: 기본 10, 최대 100
- `cursor`: 서버가 발급한 opaque 값(클라 조작 금지)
- page 방식과 혼용 금지

## 4.2 목록 응답 포맷 표준

Cursor 기반:

```json
{
	"items":[],
	"pagination":{
		"nextCursor":"opaque",
		"size":10,
		"hasNext":true
	}
}
```

Page 기반

```json
{
    "items":[],
    "pagination": {
      "page": 1,
      "size": 10,
      "totalPages": 5,
      "totalElements": 87
    },
}
```

## 4.3 Sorting 규약

- **단일 sort만 허용**

```
GET /api/v1/reviews?sort=createdAt,desc
```

- 기본 정렬은 API 문서에 반드시 명시
- 실무적으로 Cursor 페이징과 함께 사용할 경우, 커서 기준 컬럼과 정렬 컬럼의 정합성을 보장해야 함(예: `createdAt desc, id desc`를 내부 기준으로 사용하되, 외부 파라미터는 단일만 노출)

## 4.4 Filtering 규약

- Query Param 기반, 의미가 명확한 이름 사용
- 금지: `type=1`, JSON 문자열 필터

예:

- `status=ACTIVE`
- `createdFrom=2025-01-01&createdTo=2025-01-31`
- `isDeleted=false`

## 4.5 Search 분리 기준

- 단순 조건: `GET /resources`
- 복잡 조건 조합/DSL: `POST /resources/search`

---

# 5. PUT / PATCH 규약

## 5.1 PUT (전체 교체)

- 리소스 전체 상태를 전달
- 누락 필드는 null/초기화로 간주
- 부분 수정 목적 사용 금지

## 5.2 PATCH (부분 수정)

- JSON Merge Patch 스타일
- 전달된 필드만 변경
- 필드 삭제는 `null`

예:

```json
{
	"nickname":"kim",
	"profileImageUrl":null
}
```

---

# 6. 인증 / 보안 규약

- 외부 API: JWT/OAuth2
- Refresh Token은 DB 저장 (`auth_refresh_token`)
- 내부/외부 구분:
    - 외부: `/api/v1/**`
    - 내부: `/internal/**` (운영/배치/관리자 등)


## 변경 이력

| 버전 | 일자 | 작성자 | 비고 |
|---|---|---|---|
| v1.0 | 2026.01.07 | Deveon(우승화) - Backend | API 설계 컨벤션 문서 작성 |
| v1.1 | 2026.01.14 | Deveon(우승화) - Backend | Pagination 순서 재정리(Page → Cursor), 목록 응답 키 `data` → `items`, Cursor 강제 문구 제거 |
