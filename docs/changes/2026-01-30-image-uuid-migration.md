# 프론트 전달용 변경사항 정리 (이미지 UUID 기반)

- 작성일: 2026-01-30
- 목적: 기존 "이미지 URL 직접 전달" 방식에서 "업로드로 발급된 파일 UUID(fileUuid) 전달" 방식으로 전환된 구간을 프론트에 공유

## 핵심 요약

- **이미지 업로드 결과로 내려오는 `fileUuid`(UUID 문자열)를 API에 전달**하는 형태로 변경되었습니다.
- **단일 이미지(그룹 로고/회원 프로필/서브그룹 썸네일)** 는 *요청은 UUID로*, *조회 응답은 `{ id, url }` 객체*로 반환합니다.
- 로컬/개발 환경에서 S3 자격증명이 없으면 presigned 생성/URL 조회 시 AWS credential 오류가 날 수 있습니다(환경 변수/프로파일 필요).

## 용어

- `fileUuid`: 업로드 도메인에서 발급하는 외부 식별자(UUID). 프론트에서 문자열로 보관/전달.
- `public url`: 서버가 `storage.baseUrl` + `storageKey`로 조합해 내려주는 공개 조회 URL.

---

## 0) 파일(업로드) 도메인 변경사항

### 0-1. Presigned 업로드 생성 목적(purpose) Enum 확장

- **변경:** `purpose`에 아래 값이 추가되었습니다.
  - `GROUP_IMAGE`
  - `PROFILE_IMAGE`

프론트는 그룹/프로필 이미지 업로드 시 위 purpose를 사용해 presigned 업로드를 생성할 수 있습니다.

### 0-2. DomainType 확장(내부 도메인 매핑용)

- **추가:** `DomainType`에 아래 값이 추가되었습니다.
  - `MEMBER`, `GROUP`, `SUBGROUP`

> 참고: 그룹/서브그룹/프로필 변경 API들은 내부에서 도메인 링크를 수행하므로, 프론트가 별도 “도메인 링크 API”를 호출하지 않아도 동작하도록 구성되어 있습니다(요청에 `*_ImageId`만 전달).

---

## 1) 회원(Member) 관련

### 1-1. 회원 정보 수정 (PATCH `/api/v1/members/me/profile`)

**Request 변경 (Breaking)**

- Before
  - `profileImageUrl?: string`
- After
  - `profileImageId?: string` (UUID 문자열)

예시:
```json
{
  "email": "example@gmail.com",
  "profileImageId": "a3f1c9e0-7a9b-4e9c-bc2e-1f2c33aa9012"
}
```

주의:
- 현재 구현은 `profileImageId`가 **존재할 때만** 변경합니다. (JSON에서 필드 누락/`null`은 “변경 없음”과 동일하게 처리됨)

### 1-2. 내 정보 조회 (GET `/api/v1/members/me`)

**Response 변경 (profileImage 객체로 반환)**

예시:
```json
{
  "data": {
    "member": {
      "nickname": "홍길동",
      "profileImage": {
        "id": "a3f1c9e0-7a9b-4e9c-bc2e-1f2c33aa9012",
        "url": "https://cdn.example.com/uploads/temp/a3f1c9e0-7a9b-4e9c-bc2e-1f2c33aa9012.jpg"
      }
    }
  }
}
```

---

## 2) 그룹(Group) 관련

### 2-1. 그룹 생성 (POST `/api/v1/groups`)

**Request 변경 (Breaking)**

- Before
  - `logoImageURL`/`logoImageUrl`: string (URL)
- After
  - `logoImageId`: string (UUID 문자열)

예시:
```json
{
  "name": "카카오 부트캠프",
  "logoImageId": "a3f1c9e0-7a9b-4e9c-bc2e-1f2c33aa9012",
  "type": "OFFICIAL",
  "address": "경기 성남시 분당구 판교역로 166",
  "detailAddress": null,
  "location": { "latitude": 37.401219, "longitude": 127.108622 },
  "joinType": "EMAIL",
  "emailDomain": "example.com",
  "code": null
}
```

### 2-2. 그룹 정보 수정 (PATCH `/api/v1/groups/{groupId}`)

**Request 변경 (Breaking)**

- Before
  - `logoImageURL`/`logoImageUrl`: string (URL)
- After
  - `logoImageId`: string (UUID 문자열) 또는 `null`

동작:
- 필드 미전달: 변경 없음
- `logoImageId: "uuid"`: 로고 교체
- `logoImageId: null`: 로고 제거(삭제)

### 2-3. 그룹 상세 조회 (GET `/api/v1/groups/{groupId}`)

**Response 변경 (logoImage 객체로 반환)**

예시:
```json
{
  "data": {
    "groupId": 10,
    "name": "카카오 부트캠프",
    "logoImage": {
      "id": "a3f1c9e0-7a9b-4e9c-bc2e-1f2c33aa9012",
      "url": "https://cdn.example.com/groups/logo.png"
    }
  }
}
```

### 2-4. 그룹 멤버 목록 (GET `/api/v1/groups/{groupId}/members`)

**Response 변경 (profileImage 객체로 반환)**

즉 각 아이템은 다음처럼 내려옵니다:
```json
{
  "memberId": 5,
  "nickname": "세이",
  "profileImage": {
    "id": "a3f1c9e0-7a9b-4e9c-bc2e-1f2c33aa9012",
    "url": "https://cdn.example.com/profile/5.png"
  },
  "createdAt": "2026-01-02T09:00:00+09:00"
}
```

---

## 3) 하위그룹(Subgroup) 관련

### 3-1. 하위그룹 생성 (POST `/api/v1/groups/{groupId}/subgroups`)

**Request 변경 (Breaking)**

- Before
  - `profileImageUrl?: string`
- After
  - `profileImageId?: string` (UUID 문자열)

### 3-2. 하위그룹 수정 (PATCH `/api/v1/groups/{groupId}/subgroups/{subgroupId}`)

**Request 변경 (Breaking)**

- Before
  - `profileImageUrl?: string | null`
- After
  - `profileImageId?: string | null`

동작:
- 필드 미전달: 변경 없음
- `profileImageId: "uuid"`: 썸네일 교체
- `profileImageId: null`: 썸네일 제거(삭제)

### 3-3. 하위그룹 상세 조회 (GET `/api/v1/subgroups/{subgroupId}`)

**Response 변경 (profileImage 객체로 반환)**

### 3-4. 하위그룹 목록 (GET `/api/v1/groups/{groupId}/subgroups`, GET `/api/v1/members/me/groups/{groupId}/subgroups`)

**Response 변경 (profileImage 객체로 반환)**

### 3-5. 하위그룹 멤버 목록 (GET `/api/v1/subgroups/{subgroupId}/members`)

**Response 변경 (profileImage 객체로 반환)**

---

## 4) 검색(Search) 관련

### 4-1. 통합 검색 (POST `/api/v1/search`)

**Response 변경 (logoImage 객체로 반환)**

---

## 프론트 작업 체크리스트

- 요청 바디에서 URL을 보내던 구간을 **UUID 전송으로 교체**
  - `profileImageUrl` -> `profileImageId`
  - `logoImageUrl`/`logoImageURL` -> `logoImageId`
- 응답은 단일 이미지의 경우 `{ id, url }` 객체로 반환
