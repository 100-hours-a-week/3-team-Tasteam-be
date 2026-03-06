# 더미 데이터 DB 크기 분석

> 기준: High 프리셋 / PostgreSQL 행 크기 실측 계산
> 작성일: 2026-03-06

---

## 프리셋 비교

| 항목 | Low | Medium | High |
|------|-----|--------|------|
| 멤버 수 | 500 | 5,000 | 100,000 |
| 음식점 수 | 200 | 11,000 | 50,000 |
| 그룹 수 | 20 | 50 | 200 |
| 그룹당 하위그룹 | 5 | 5 | 10 |
| 그룹당 멤버 | 30 | 30 | 100 |
| 리뷰 수 | 1,000 | 50,000 | 1,000,000 |
| 채팅방당 메시지 | 50 | 200 | 1,000 |
| 알림 수 | 5,000 | 100,000 | 1,500,000 |
| 즐겨찾기 수 | 1,000 | 20,000 | 400,000 |

> **참고**: 음식점 수를 High 500,000 → 50,000, Medium 10,000 → 11,000으로 재설정
> (기존 500,000개 기준 restaurant 단독 5~10 GB 예상 → 디스크 과부하 방지)

---

## 행 크기 계산 기준

### PostgreSQL 행 저장 구조

```
[튜플 헤더 23B] [Null bitmap ceil(컬럼수/8)B] [데이터] [패딩(8B 정렬)]
```

- `bigint` (Long): 8B
- `integer` (int): 4B
- `boolean`: 1B
- `timestamptz` (Instant): 8B
- `varchar` 단문(≤126B 실데이터): 1-byte varlena header
- `varchar` 장문(>126B): 4-byte varlena header
- `geometry(Point,4326)`: NULL이면 GiST 인덱스 미생성

### 인덱스 항목 크기 (B-tree 기준)

| 키 타입 | 리프 항목 크기 |
|---------|--------------|
| bigint 단일 | ~30B |
| bigint 복합 2개 | ~38B |
| bigint 복합 3개 | ~46B |
| varchar(64) | ~30B |

---

## 실제 INSERT 대상 테이블

더미 시딩에서 실제 INSERT가 발생하는 테이블 목록 (`DummyDataJdbcRepository` 기준):

```
member, restaurant, group, group_member,
subgroup, chat_room, subgroup_member, chat_room_member,
chat_message, review, review_keyword,
notification, member_favorite_restaurant
```

**미삽입 테이블** (실서비스 데이터만 존재):
- `restaurant_address` — 주소 파싱 결과 별도 저장
- `restaurant_weekly_schedule` — 영업시간
- `restaurant_food_category` — 카테고리 매핑
- `restaurant_schedule_override` — 임시 영업시간
- `restaurant_review_sentiment/summary` — AI 분석 결과

---

## High 프리셋 테이블별 크기 계산

### Step 1 — member (100,000행)

실제 INSERT 컬럼: `id, email, nickname, status, role, created_at, updated_at`
NULL 컬럼 (6개): `profile_image_url, introduction, last_login_at, agreed_terms_at, agreed_privacy_at, deleted_at`

| 컬럼 | 실제값 예시 | 저장 크기 |
|------|------------|---------|
| id (bigint) | | 8B |
| email (varchar 255) | `dummy-{UUID}-12345@dummy.tasteam.kr` | 68B |
| nickname (varchar 50) | `더미유저-{UUID}-12345` (UTF-8 ~57B) | 58B |
| status (varchar 20) | `ACTIVE` | 7B |
| role (varchar 20) | `USER` | 5B |
| created_at, updated_at | | 16B |
| NULL 6개 | null bitmap만 | 0B |

```
헤더(23B) + null bitmap(2B) + 데이터(162B) + 정렬 패딩 = ~200B/행
인덱스: PK(30B) + unique(email)(90B) = 120B/행
행당 합계: 320B
```

**100,000행 × 320B = 32 MB**

---

### Step 2 — restaurant (50,000행)

실제 INSERT 컬럼: `id, name, full_address, location, vector_epoch, created_at, updated_at`
NULL 컬럼: `phone_number, deleted_at, vector_synced_at`

> **변경**: `location = ST_SetSRID(ST_MakePoint(lon, lat), 4326)` — 한국 영역 내 랜덤 좌표 삽입
> → PostGIS GiST 인덱스 생성됨 (기존 NULL → 미생성 대비 +GiST)

| 컬럼 | 실제값 예시 | 저장 크기 |
|------|------------|---------|
| id (bigint) | | 8B |
| name (varchar 100) | `더미식당-{UUID}-12345` (UTF-8 ~55B) | 56B |
| full_address (varchar 255) | `서울시 강남구 더미로 12345번길 1` (~47B) | 48B |
| location (geometry Point) | `ST_MakePoint(127.x, 36.x)` | ~32B |
| vector_epoch (bigint) | 0 | 8B |
| created_at, updated_at | | 16B |

```
헤더(23B) + null bitmap(2B) + 데이터(168B) + 정렬 패딩 = ~200B/행
인덱스: PK(30B) + GiST spatial (~80B/행)
행당 합계: ~310B
```

> PostGIS GiST 리프 엔트리: geometry bbox + heap pointer ≈ 80B/행
> 50,000행 × 80B = ~4MB (GiST 추가분)

**50,000행 × 310B ≈ 15.5 MB (기존 10 MB → +5.5 MB)**

---

### Step 3 — group (200행) — 무시 가능

200행 × ~250B = **0.05 MB**

---

### Step 4 — group/subgroup/chat 관련 관계 테이블

파생 행수 계산:
- 하위그룹: 200그룹 × 10 = **2,000행**
- 채팅방: 하위그룹당 1개 = **2,000행**
- 그룹멤버: 200그룹 × 100 = **20,000행**
- 하위그룹멤버: 2,000 × 100 = **200,000행**
- 채팅방멤버: 2,000 × 100 = **200,000행**
- 채팅메시지: 2,000방 × 1,000 = **2,000,000행**

| 테이블 | 행수 | INSERT 컬럼 | 행당 크기 | 소계 |
|--------|-----|------------|---------|------|
| subgroup | 2,000 | id, group_id, name, join_type, status, member_count, created_at, updated_at | ~142B | 0.3 MB |
| chat_room | 2,000 | id, subgroup_id, created_at | ~78B | 0.2 MB |
| group_member | 20,000 | id, group_id, member_id, created_at | 124B | 2.5 MB |
| subgroup_member | 200,000 | id, subgroup_id, member_id, created_at | 122B | 24 MB |
| chat_room_member | 200,000 | member_id, chat_room_id, created_at, updated_at | 92B | 18 MB |
| **chat_message** | **2,000,000** | chat_room_id, member_id, type, content, created_at | **156B** | **312 MB** |

> `chat_message.content` = `"더미 채팅 메시지 {seq}"` ≈ 30B
> 인덱스: PK(30B) + idx(chat_room_id, id)(36B)

**Step 4 소계: 357 MB**

---

### Step 5 — review / review_keyword

**review (1,000,000행)**

실제 INSERT: `id, member_id, restaurant_id, group_id, is_recommended, content, created_at, updated_at`
NULL: `subgroup_id, deleted_at, vector_synced_at`

| 컬럼 | 크기 |
|------|------|
| id, member_id, restaurant_id, group_id (bigint×4) | 32B |
| is_recommended (boolean) | 1B |
| content `"더미 리뷰 내용입니다. 12345"` (~40B) | 41B |
| created_at, updated_at | 16B |

```
헤더(25B) + 데이터(90B) + 패딩(6B) = ~128B/행
인덱스: PK(30B) + idx_member_id(30B) + idx_restaurant_id(30B) = 90B
행당 합계: 218B
```

**1,000,000행 × 218B = 218 MB**

---

**review_keyword (~2,000,000행)**

- `1 + nextInt(3)` → 평균 2 키워드/리뷰
- 중복 가능성 낮음 (reviewId % keywordSize 기반 선택)

INSERT: `review_id, keyword_id` (id는 IDENTITY 자동생성)

```
헤더(23B) + id(8B) + review_id(8B) + keyword_id(8B) = 47B → 48B/행
인덱스: PK(30B) + unique(review_id, keyword_id)(36B) = 66B
행당 합계: 114B
```

**2,000,000행 × 114B = 228 MB**

**Step 5 소계: 446 MB**

---

### Step 6 — notification (1,500,000행)

실제 INSERT: `member_id, notification_type, title, body, event_id, created_at`
NULL: `deep_link, read_at`

| 컬럼 | 실제값 예시 | 크기 |
|------|------------|------|
| id (bigint IDENTITY) | | 8B |
| member_id (bigint) | | 8B |
| notification_type (varchar 20) | `CHAT` / `SYSTEM` / `NOTICE` | 5B |
| title (varchar 100) | `더미 알림 제목 12345` (~27B) | 28B |
| body (varchar 500) | `더미 알림 내용입니다. 12345` (~40B) | 41B |
| event_id (varchar 64) | `dummy-0-fb803a1d` (~22B) | 23B |
| created_at | | 8B |

```
헤더(25B) + 데이터(121B) + 패딩(4B) = ~160B/행
인덱스 4개:
  - PK(id): 30B
  - idx_member_id (member_id, id DESC): 36B
  - idx_member_unread (member_id, read_at, id): 46B
  - partial unique (event_id) WHERE NOT NULL: 30B
인덱스 합계: 142B
행당 합계: 302B
```

**1,500,000행 × 302B = 453 MB**

---

### Step 7 — member_favorite_restaurant (400,000행)

실제 INSERT: `member_id, restaurant_id, created_at`
NULL: `deleted_at`

```
헤더(24B) + id(8B) + member_id(8B) + restaurant_id(8B) + created_at(8B) = 56B/행
인덱스: PK(30B) + unique(member_id, restaurant_id)(36B) = 66B
행당 합계: 122B
```

**400,000행 × 122B = 49 MB**

---

## 전체 합산 (High 프리셋)

| Step | 주요 테이블 | 행수 | 크기 | 비율 |
|------|------------|-----|------|-----|
| 1 멤버 | member | 100,000 | 32 MB | 2% |
| 2 식당 | restaurant | 50,000 | 15.5 MB | 1% |
| 3 그룹 | group | 200 | ~0 MB | — |
| 4 관계/채팅 | subgroup_member + chat_room_member + chat_message 등 | ~2,622,000 | 357 MB | 26% |
| 5 리뷰 | review + review_keyword | ~3,000,000 | 446 MB | 33% |
| 6 알림 | notification | 1,500,000 | 453 MB | 33% |
| 7 즐겨찾기 | member_favorite_restaurant | 400,000 | 49 MB | 4% |
| **합계** | | **~7,672,200** | **≈ 1.36 GB** | 100% |

---

## 주요 인사이트

### 예상보다 작은 이유

| 예상 요소 | 실제 |
|----------|------|
| restaurant 500K × GiST 인덱스 → 5~10 GB | **5만개 + GiST 포함 → 15.5 MB** |
| restaurant_address / weekly_schedule 삽입 | **더미 시딩에서 미삽입** |
| 음식점 50만개 | **재설정: 5만개** |
| location = NULL | **랜덤 한국 좌표로 삽입 → GiST 인덱스 생성 (+5.5 MB)** |

### 크기 상위 3개 테이블

```
notification          453 MB  (34%)  ← 가장 큼
review_keyword        228 MB  (17%)
review                218 MB  (16%)
chat_message          312 MB  (23%)
```

> **notification이 단독으로 가장 큰 이유**:
> 4개 인덱스(PK + member_id + unread복합 + partial unique) 보유
> 행당 인덱스 오버헤드가 140B → 데이터(160B)보다 거의 동일 수준

### dev/stg 인스턴스 운영 가이드

| 인스턴스 디스크 | 권장 프리셋 |
|---------------|-----------|
| 10 GB 미만 | Low |
| 20~30 GB | Medium 또는 High |
| 50 GB 이상 | High 안전 |

> High 프리셋 기준 **≈ 1.36 GB**, PostgreSQL 자체 시스템 카탈로그/WAL 포함 시 **≈ 1.5~2 GB** 예상
