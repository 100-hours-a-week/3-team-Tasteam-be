| 항목 | 내용 |
|---|---|
| 문서 제목 | 이벤트/공지(Event & Notice) 테크 스펙 |
| 문서 목적 | 이벤트/공지 API, 이벤트 본체와 디스플레이 제어 분리 모델, 배너/상세 미디어 모델을 백엔드 기준으로 고정하여 구현/리뷰/테스트의 기준으로 사용한다. |
| 작성 및 관리 | Backend Team |
| 최초 작성일 | 2026.02.11 |
| 최종 수정일 | 2026.02.11 |
| 문서 버전 | v1.1 |

<br>

# 이벤트/공지(Event & Notice) - BE 테크스펙

---

# **[1] 배경 (Background)**

## **[1-1] 프로젝트 목표 (Objective)**

이벤트 도메인에서 실제 이벤트 기간과 화면 노출(디스플레이) 기간을 분리하고, 메인 배너와 이벤트 상세 이미지를 독립적으로 관리해 운영 유연성을 확보한다.

- **핵심 결과 (Key Result) 1:** 이벤트 상태(`UPCOMING/ONGOING/ENDED`) 계산 정확도 `100%`
- **핵심 결과 (Key Result) 2:** 디스플레이 상태(`SCHEDULED/DISPLAYING/ENDED/HIDDEN`) 계산 정확도 `100%`
- **핵심 결과 (Key Result) 3:** 이벤트 목록 조회 p95 `<= 200ms` (`size=20`)
- **핵심 결과 (Key Result) 4:** 운영자가 이벤트 기간 변경 없이 노출 기간만 독립 수정 가능

## **[1-2] 문제 정의 (Problem)**

- 이벤트 실제 기간과 노출 기간이 동일하다는 가정은 운영 요구와 맞지 않는다.
- 단일 `event` 테이블에서 노출 여부/노출 기간까지 함께 관리하면 정책 변경 시 회귀 위험이 커진다.
- 이벤트 카드 배너 이미지와 상세 페이지 이미지는 용도/비율/갯수가 달라 단일 URL 필드로는 확장성이 낮다.

## **[1-3] 가설 (Hypothesis)**

`event(본체)`와 `event_display(노출 제어)`를 분리하고, `event_asset(미디어)`를 도입하면, 운영 정책 변경과 화면 확장에 대응하는 비용을 줄일 수 있다.

<br>

---

# **[2] 목표가 아닌 것 (Non-goals)**

- 쿠폰/응모/당첨 등 이벤트 참여 트랜잭션
- 관리자 CMS UI 구현
- 마케팅 푸시/이메일 자동 발송 오케스트레이션
- 다국어 본문 템플릿 렌더링

<br>

---

# **[3] 설계 및 기술 자료 (Architecture and Technical Documentation)**

## **[3-1] 핵심 도메인 개념**

- `이벤트 본체(Event)`
  - 이벤트의 실질적 유효기간, 제목, 본문, 랜딩 링크를 관리
- `디스플레이(EventDisplay)`
  - 어떤 기간에, 어떤 채널에서, 어떤 우선순위로 노출할지 관리
- `이벤트 미디어(EventAsset)`
  - 배너 이미지와 상세 페이지 이미지를 타입 기반으로 분리 관리

## **[3-2] 상태 모델**

### 이벤트 상태(`eventStatus`)

- `UPCOMING`: `now < event_start_at`
- `ONGOING`: `event_start_at <= now <= event_end_at`
- `ENDED`: `now > event_end_at`

### 디스플레이 상태(`displayStatus`)

- `HIDDEN`: `display_enabled=false` 또는 `publish_status != PUBLISHED`
- `SCHEDULED`: `display_enabled=true` 이고 `now < display_start_at`
- `DISPLAYING`: `display_enabled=true` 이고 `display_start_at <= now <= display_end_at`
- `DISPLAY_ENDED`: `display_enabled=true` 이고 `now > display_end_at`

원칙:
- 이벤트 상태와 디스플레이 상태는 서로 독립이다.
- Public 목록/상세 노출은 `displayStatus=DISPLAYING` 조건을 따른다.
- 메인 스플래시 광고(`splashEvent`)는 `display_channel=MAIN_BANNER` 또는 `BOTH`만 대상으로 한다.

## **[3-3] 데이터 모델 (분리 설계)**

### `notice`

| 컬럼 | 타입 | Nullable | 설명 |
|---|---|---:|---|
| `id` | BIGINT | N | PK |
| `title` | VARCHAR(200) | N | 제목 |
| `content` | TEXT | N | 본문 |
| `created_at` | TIMESTAMP | N | 생성 시각 |
| `updated_at` | TIMESTAMP | N | 수정 시각 |
| `deleted_at` | TIMESTAMP | Y | 소프트 삭제 |

### `event`

| 컬럼 | 타입 | Nullable | 설명 |
|---|---|---:|---|
| `id` | BIGINT | N | PK |
| `title` | VARCHAR(200) | N | 이벤트명 |
| `content` | TEXT | N | 상세 본문 |
| `landing_url` | VARCHAR(500) | Y | 외부/내부 랜딩 URL |
| `event_start_at` | TIMESTAMP | N | 실제 이벤트 시작 시각 |
| `event_end_at` | TIMESTAMP | N | 실제 이벤트 종료 시각 |
| `publish_status` | VARCHAR(20) | N | `DRAFT`, `PUBLISHED`, `ARCHIVED` |
| `created_at` | TIMESTAMP | N | 생성 시각 |
| `updated_at` | TIMESTAMP | N | 수정 시각 |
| `deleted_at` | TIMESTAMP | Y | 소프트 삭제 |

제약:
- `CHECK (event_start_at <= event_end_at)`

### `event_display`

| 컬럼 | 타입 | Nullable | 설명 |
|---|---|---:|---|
| `id` | BIGINT | N | PK |
| `event_id` | BIGINT | N | FK -> `event.id` |
| `display_enabled` | BOOLEAN | N | 노출 on/off 토글 |
| `display_start_at` | TIMESTAMP | N | 노출 시작 시각 |
| `display_end_at` | TIMESTAMP | N | 노출 종료 시각 |
| `display_channel` | VARCHAR(20) | N | `MAIN_BANNER`, `EVENT_LIST`, `BOTH` |
| `display_priority` | INTEGER | N | 정렬 우선순위(작을수록 상위) |
| `created_at` | TIMESTAMP | N | 생성 시각 |
| `updated_at` | TIMESTAMP | N | 수정 시각 |
| `deleted_at` | TIMESTAMP | Y | 소프트 삭제 |

제약:
- `CHECK (display_start_at <= display_end_at)`
- `UNIQUE(event_id)` (현재 활성 display 정책 1개)

### `event_asset`

| 컬럼 | 타입 | Nullable | 설명 |
|---|---|---:|---|
| `id` | BIGINT | N | PK |
| `event_id` | BIGINT | N | FK -> `event.id` |
| `asset_type` | VARCHAR(30) | N | `BANNER`, `DETAIL` |
| `image_url` | VARCHAR(500) | N | 이미지 URL |
| `alt_text` | VARCHAR(200) | Y | 접근성 텍스트 |
| `sort_order` | INTEGER | N | 상세 이미지 순서 |
| `is_primary` | BOOLEAN | N | 대표 이미지 여부 |
| `created_at` | TIMESTAMP | N | 생성 시각 |
| `updated_at` | TIMESTAMP | N | 수정 시각 |
| `deleted_at` | TIMESTAMP | Y | 소프트 삭제 |

제약:
- 배너 대표 1장 규칙: `event_id + asset_type=BANNER + is_primary=true` 유니크(Partial Unique)

## **[3-4] 인덱스 전략**

- `idx_event_publish_period (publish_status, event_start_at, event_end_at)`
- `idx_event_display_window (display_enabled, display_start_at, display_end_at)`
- `idx_event_display_channel_priority (display_channel, display_priority)`
- `idx_event_asset_type_order (event_id, asset_type, sort_order)`

## **[3-5] API 계약 (Read)**

### `GET /api/v1/events`

- 권한: `PUBLIC`
- Query
  - `page`, `size`
  - `eventStatus` (optional)
- 기본 필터
  - `event.deleted_at is null`
  - `event.publish_status = PUBLISHED`
  - `event_display.display_enabled = true`
  - `now between display_start_at and display_end_at`
- 응답 필드(권장)
  - `id`, `title`, `summary`, `bannerImageUrl`, `eventStartAt`, `eventEndAt`, `eventStatus`, `displayStartAt`, `displayEndAt`

### `GET /api/v1/events/{id}`

- 권한: `PUBLIC`
- 노출 조건
  - 목록과 동일한 display 조건을 통과한 이벤트만 접근 허용
- 응답 필드(권장)
  - 목록 필드 + `content`, `landingUrl`, `detailImages[]`

### `GET /api/v1/notices`, `GET /api/v1/notices/{id}`

- 기존 계약 유지

### `GET /api/v1/main` 내 `splashEvent`

- 목적: 메인 진입 시 스플래시 팝업용 이벤트 1건 제공
- 선정 규칙
  - `event.publish_status = PUBLISHED`
  - `event_display.display_enabled = true`
  - `now between display_start_at and display_end_at`
  - `display_channel in (MAIN_BANNER, BOTH)`
  - `event_asset`에서 `asset_type = BANNER` 대표 1건 사용
  - 정렬 우선순위: `display_priority asc`, `display_start_at desc`, `event.id desc`
- 응답 필드(권장)
  - `id`, `title`, `content`, `thumbnailImageUrl`, `startAt`, `endAt`

## **[3-6] Admin API 계약 (후속 구현 권장)**

- `POST /api/v1/admin/events`
- `PATCH /api/v1/admin/events/{id}`
- `PATCH /api/v1/admin/events/{id}/display`
- `POST /api/v1/admin/events/{id}/assets`
- `PATCH /api/v1/admin/events/{id}/assets/{assetId}`
- `DELETE /api/v1/admin/events/{id}/assets/{assetId}`

원칙:
- 이벤트 본체 수정과 디스플레이 수정은 별도 API로 분리한다.

## **[3-7] 유효성 검증 규칙**

- `event_start_at <= event_end_at`
- `display_start_at <= display_end_at`
- `display_end_at`는 과거여도 저장 가능(종료 이벤트 이력 관리)
- `display_channel=MAIN_BANNER`이면 `BANNER` asset 최소 1건 필요
- 상세 API 노출 전 `DETAIL` asset 0건도 허용(텍스트만 이벤트)
- `landing_url`는 allowlist 또는 내부 경로 정책을 통과해야 함

## **[3-8] 운영 정책**

- 노출 중지 우선순위
  1. `display_enabled=false`
  2. `publish_status != PUBLISHED`
  3. display window 만료
- 종료 이벤트 보관
  - `eventStatus=ENDED`라도 display window를 미래로 잡으면 노출 가능(기획 허용 시)
- 소프트 삭제
  - `event`, `event_display`, `event_asset` 모두 `deleted_at` 기반

## **[3-9] 에러 코드 제안**

| code | status | 의미 |
|---|---:|---|
| `EVENT_NOT_FOUND` | 404 | 이벤트 없음 또는 비노출 |
| `INVALID_EVENT_PERIOD` | 400 | 이벤트 기간 역전 |
| `INVALID_DISPLAY_PERIOD` | 400 | 디스플레이 기간 역전 |
| `EVENT_DISPLAY_POLICY_MISSING` | 400 | 디스플레이 정책 누락 |
| `EVENT_BANNER_ASSET_REQUIRED` | 400 | 배너 노출인데 배너 이미지 없음 |
| `INVALID_EVENT_STATUS_FILTER` | 400 | 상태 필터 오류 |

## **[3-10] 테스트 전략**

- Unit
  - `eventStatus`와 `displayStatus` 계산 경계 시각 테스트
- Repository
  - display 조건 조합(`enabled/window/publish`) 필터 정확성
  - 배너 대표 이미지 선택 쿼리 정확성
- Integration
  - `eventStatus=ONGOING`인데 `displayStatus=SCHEDULED`인 이벤트 비노출 검증
  - `eventStatus=ENDED`여도 display 기간 내 노출 허용 정책 검증(옵션)

## **[3-11] 부트스트랩 연계**

샘플 데이터는 상태 조합이 모두 포함되어야 한다.

권장 조합:
- `eventStatus`: `UPCOMING`, `ONGOING`, `ENDED` 각 5건
- `displayStatus`: `SCHEDULED`, `DISPLAYING`, `DISPLAY_ENDED`, `HIDDEN` 각 3건 이상
- 각 이벤트별 미디어
  - `BANNER` 1건 이상
  - `DETAIL` 1~5건

부트스트랩 절차는 `docs/spec/tech/bootstrap/README.md`를 따른다.

## **[3-12] 오픈 이슈**

1. Public 상세 API에서 비노출 이벤트 접근을 404로 통일할지 403으로 분리할지
2. display 정책을 이력형(`event_display_history`)으로 확장할지
3. 배너 이미지 리사이징/비율 강제 정책(예: 3:1) 저장 위치
