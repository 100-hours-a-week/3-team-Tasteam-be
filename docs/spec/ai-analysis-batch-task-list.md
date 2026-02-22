# AI 분석 배치 설계 문서 기반 작업 리스트

현재 구조(이벤트/스케줄 + 통합 스냅샷 테이블)를 설계 문서의 **Job 단위 + Epoch 기반 + 분석 결과 테이블 분리** 구조로 옮기기 위한 작업 목록과 진행 방식이다.

---

## 1. 작업 리스트 요약

| # | 작업 | 유형 | 의존 |
|---|------|------|------|
| 1 | Restaurant / Review 벡터 필드 추가 (마이그레이션) | DB | - |
| 2 | BatchExecution 테이블 생성 (마이그레이션) | DB | - |
| 3 | AI Job 테이블 생성 (마이그레이션) | DB | 2 |
| 4 | 분석 결과 테이블 3종 생성 (마이그레이션) | DB | - |
| 5 | Restaurant / Review 엔티티에 벡터 필드 반영 | 도메인 | 1 |
| 6 | BatchExecution 엔티티 및 Repository | 도메인 | 2 |
| 7 | AI Job 엔티티 및 Repository | 도메인 | 3 |
| 8 | 감정/요약/비교 분석 결과 엔티티 및 Repository | 도메인 | 4 |
| 9 | 벡터 업로드 Job 생성·실행 (24시간 주기) | 배치 | 5, 6, 7 |
| 10 | 벡터 업로드 성공 시 감정/요약 Job 생성 | 배치 | 7, 9 |
| 11 | 감정 분석 Job 실행기 (Epoch 검증, STALE 처리) | 배치 | 6, 7, 8 |
| 12 | 요약 분석 Job 실행기 (Epoch 검증, STALE 처리) | 배치 | 6, 7, 8 |
| 13 | 비교 분석 Job 생성·실행 (1주일 주기) | 배치 | 6, 7, 8 |
| 14 | Job 동시성 제어 (Restaurant 단위 RUNNING 1개) | 배치 | 7 |
| 15 | 실패 처리 (즉시 재시도 없음, 다음 배치 재처리) | 배치 | 11, 12, 13 |
| 16 | 조회 경로 이전 (상세/메인/AI추천 → 새 테이블) | 서비스 | 8 |
| 17 | 기존 분석 트리거 제거 및 정리 | 정리 | 9~16 |

---

## 1.1 작업 분류: 에이전트 위임 vs 직접 구현(학습)

| 구분 | 작업 번호 | 이유 |
|------|-----------|------|
| **에이전트 위임 추천** (단순·스펙 명확) | 1, 2, 3, 4, 5, 6, 7, 8, 17 | 마이그레이션·엔티티·Repository는 스키마·컨벤션이 문서로 정해져 있어 단순 구현에 가깝다. 17번(기존 트리거 제거)도 제거 대상이 명확해 에이전트가 진행하기 좋다. |
| **직접 구현 추천** (설계·트레이드오프·학습 가치) | 3단계(Job·Worker 분리), 9, 10, 11, 12, 13, 14, 15, 16 | 아래 상세 참고. |

### 에이전트에게 넘기기 좋은 단순 작업

- **1, 2, 3, 4** — DB 마이그레이션: 스키마가 2장·설계 문서에 명시되어 있다. Flyway 규칙만 지키면 된다. (4번은 PK/유니크 선택 시 “최신 1건” 조회 용도만 고려하면 됨.)
- **5** — Restaurant/Review 엔티티: 컬럼 추가 + `incrementVectorEpoch(Instant)` 등 비즈니스 메서드. ENTITY_CONVENTION.md 따라 구현하면 된다.
- **6, 7** — BatchExecution, AiJob 엔티티·Repository: 스키마·관계가 정해져 있고, JpaRepository + 필요한 쿼리 메서드 추가 수준이다.
- **8** — 감정/요약/비교 결과 엔티티·Repository: JSONB 매핑은 프로젝트 기존 방식(예: `@JdbcTypeCode`)을 따르고, “최신 1건” 조회 시그니처만 정하면 된다.
- **17** — 기존 분석 트리거 제거: 제거/비활성화 대상이 문서에 나와 있어, 기능 플래그·참조 제거 순서만 정하면 에이전트가 수행하기 적합하다.

### 학습·직접 구현 추천 (보조만 받고 직접 해볼 작업)

- **3단계 — Job·Worker 분리 및 트리거 계층**  
  설계가 핵심이다. “Job 생성”과 “Job 소비”를 어떻게 나누고, 스케줄러 → MQ로 바꿀 때 어떤 인터페이스만 바꾸면 되는지 경험하는 것이 백엔드 설계 감각에 도움이 된다. 에이전트는 인터페이스 초안·패키지 구조 제안 정도만 하고, 실제 분리와 MQ 대체 가능 형태는 직접 구현해 보는 것을 권장한다.

- **9 — 벡터 업로드 Job 생성·실행**  
  “업로드 대상 레스토랑” 판단(증분 업로드 조건), 사전 작업(고아·미종료 FAILED)·이전 배치 완료 대기·Worker 호출 순서, Discord 회수 건수 보고 등을 한 흐름으로 만드는 경험이 중요하다. 트레이드오프(대상 조회 쿼리, 배치 크기 등)를 직접 결정해 보는 것이 좋다.

- **10 — 벡터 업로드 성공 시 감정/요약 Job 생성**  
  `batch_execution_id`를 “지금 넣을지 vs 다음 ANALYSIS_DAILY에서 PENDING 가져갈 때 부여할지” 같은 **시점/정합성 트레이드오프**를 경험할 수 있다. 설계 문서에 “정책 확정 시 반영”으로 남아 있는 부분이라, 직접 선택하고 구현해 보면 좋다.

- **11, 12 — 감정/요약 Job 실행기**  
  Epoch 재검증 → STALE vs 저장, Worker의 “PENDING 가져오기 → RUNNING 선점 → AI 호출 → 결과 저장” 루프, 트랜잭션 경계(REQUIRES_NEW)를 몸으로 익히기 좋다. 동시성(14)과 함께 구현하면 DB 락·선점 패턴을 체감할 수 있다.

- **13 — 비교 분석 Job 생성·실행**  
  1주일 주기·대상 레스토랑 범위(전체 vs 조건), Job 개수와 Worker 부하 균형 등을 고려하는 작업이다. 9·11과 패턴은 비슷하지만 batch_type·정책이 달라서, 한 번 9·11을 구현한 뒤 13을 직접 맞추어 보면 배치 런너 설계가 머릿속에 잘 잡힌다.

- **14 — Job 동시성 제어**  
  “UPDATE … WHERE status = 'PENDING' 후 affected rows = 1” 선점 vs “선점 전 (restaurant_id, job_type) RUNNING 존재 여부 조회” vs 서브쿼리 조건 등 **구현 방식 선택**이 성능·정합성에 영향을 준다. 여러 방법을 비교해 보면서 구현해 보면 동시성 제어 감각이 늘어난다.

- **15 — 실패 처리**  
  “FAILED Job을 다음 배치에서 PENDING으로 되돌릴지”, “attempt_count < 2만 재시도할지”, “동일 epoch 2회 실패 시 스킵/표시할지” 등 **재처리 정책**을 결정하는 부분이다. 운영 관점에서 어떤 실패를 다시 시도하고 어떤 것은 건너뛸지 직접 정해 보는 것이 좋다.

- **16 — 조회 경로 이전**  
  “최신 1건”을 **vector_epoch 최대**로 할지 **analyzed_at DESC**로 할지, 기존 API 응답 계약을 유지하면서 새 테이블만 바라보게 할지 등 **조회 정의와 API 호환**을 경험할 수 있다. 상세/메인/AI추천 각 경로를 직접 바꿔 보면 읽기 경로 설계 감이 생긴다.

---

## 2. 데이터 모델

### 4.3 BatchExecution 테이블

#### 4.3.1 목적

**BatchExecution**은 하나의 배치 실행 세션을 정의하는 상위 실행 단위이다.

- 배치 **시작/종료 시각** 기록 → 총 실행 시간 산출
- 실행 단위 **통계 집계** (성공/실패/STALE 건수)
- **리포트 생성** 기준 제공
- **Job 실행 그룹화** → 한 번의 스케줄 런에서 처리된 Job들을 하나의 BatchExecution으로 묶음

실제 처리 단위는 AI Job이며, BatchExecution은 그룹과 통계/리포트용 메타데이터를 담는다.

#### 4.3.2 스키마

```text
batch_execution
-----------------------------------------
id                  BIGINT PK
batch_type          VARCHAR(50)
status              VARCHAR(32)
started_at          TIMESTAMP NOT NULL
finished_at         TIMESTAMP
total_jobs          INT
success_count       INT
failed_count        INT
stale_count         INT
created_at          TIMESTAMP
updated_at          TIMESTAMP
```

- **started_at**: 배치 런 시작 시각 (스케줄러/Runner 진입 시)
- **finished_at**: 해당 배치 런에서 처리 완료한 시각 (모든 대상 Job 처리 완료 또는 종료 조건 도달 시)
- **total_jobs**: 이 배치에 포함된(생성·할당된) Job 총 개수
- **success_count** / **failed_count** / **stale_count**: 각각 COMPLETED / FAILED / STALE 로 끝난 Job 수. 배치 종료 시점에 집계하거나, Job 상태 전이 시마다 갱신

#### 4.3.3 batch_type 정의

| batch_type       | 설명             | 주기       |
|------------------|------------------|------------|
| VECTOR_DAILY     | 벡터 업로드      | 24시간     |
| ANALYSIS_DAILY   | 감정/요약 분석   | 벡터 성공 대상 |
| COMPARISON_WEEKLY| 음식점 비교      | 1주일      |

---

### 4.4 AI Job 테이블

| 필드               | 설명 |
|--------------------|------|
| id                 | PK |
| **batch_execution_id** | FK → batch_execution (이 배치 런에 속한 Job임을 표시) |
| job_type           | 4종 (VECTOR_UPLOAD / REVIEW_SENTIMENT / REVIEW_SUMMARY / RESTAURANT_COMPARISON) |
| restaurant_id      | 작업 단위 |
| status             | PENDING / RUNNING / COMPLETED / FAILED / STALE |
| base_epoch         | 생성 시점 epoch |
| attempt_count      | 실행 횟수 |
| created_at         | 생성 시각 |

- 즉시 재시도는 수행하지 않는다.
- Job 상태가 COMPLETED / FAILED / STALE 로 바뀔 때마다 해당 BatchExecution의 success_count / failed_count / stale_count 를 갱신하거나, 배치 종료 시 ai_job 테이블을 집계해 batch_execution 에 반영한다.

---

## 3. 작업별 진행 방식

### 1) Restaurant / Review 벡터 필드 추가 (마이그레이션)

- **내용**
  - `restaurant`: `vector_epoch BIGINT NOT NULL DEFAULT 0`, `vector_synced_at TIMESTAMPTZ`
  - `review`: `vector_synced_at TIMESTAMPTZ`
- **진행**
  - Flyway 마이그레이션 1개 추가.
  - 기존 행은 `vector_epoch = 0`, `vector_synced_at = NULL`로 두고, 새 배치가 채우도록 함.
- **주의**
  - `review.vector_synced_at`은 벡터 업로드 성공 시 해당 리뷰만 갱신하는 용도. 인덱스는 “벡터 미반영 리뷰 조회” 필요 시 나중에 추가.

---

### 2) BatchExecution 테이블 생성 (마이그레이션)

- **내용**
  - `batch_execution`: 2장 4.3.2 스키마대로 생성. `batch_type`, `status`, `started_at`, `finished_at`, `total_jobs`, `success_count`, `failed_count`, `stale_count`, `created_at`, `updated_at`.
- **진행**
  - Flyway 마이그레이션 1개. `status`는 RUNNING / COMPLETED / FAILED 등 (배치 런 전체 상태).
  - AI Job 테이블보다 먼저 생성(또는 동일 마이그레이션에서 batch_execution 먼저, ai_job 다음).

---

### 3) AI Job 테이블 생성 (마이그레이션)

- **내용**
  - `ai_job`: `id`, **`batch_execution_id`**(FK → batch_execution), `job_type`, `restaurant_id`, `status`, `base_epoch`, `attempt_count`, `created_at` 등.
  - 설계상 “동일 restaurant_id RUNNING 1개”이므로, `(restaurant_id, job_type, status)` 또는 `(restaurant_id, job_type)` + status 조건으로 조회/락에 사용.
- **진행**
  - Flyway로 테이블 생성. `batch_execution_id`는 NOT NULL로 두어 “모든 Job은 어떤 배치 런에 속한다”를 보장.
  - `job_type`, `status`는 CHECK 또는 enum 타입으로 고정.

---

### 4) 분석 결과 테이블 3종 생성 (마이그레이션)

- **내용**
  - `restaurant_review_sentiment`: 설계 5.1 (restaurant_id, vector_epoch, model_version, positive/negative/neutral count·ratio, analyzed_at).
  - `restaurant_review_summary`: 설계 5.2 (restaurant_id, vector_epoch, model_version, summary_json, analyzed_at).
  - `restaurant_comparison_analysis`: 설계 5.3 (restaurant_id, model_version, comparison_json, analyzed_at). vector_epoch 없음.
- **진행**
  - Flyway 1개(또는 3개) 마이그레이션으로 생성.
  - PK/유니크: 감정·요약은 (restaurant_id, vector_epoch) 또는 (restaurant_id, vector_epoch, model_version) 후보. 비교는 (restaurant_id) 또는 (restaurant_id, model_version).
  - 기존 `ai_restaurant_review_analysis` / `ai_restaurant_comparison`는 **당장 삭제하지 않고** 새 테이블만 추가. 조회 전환 후 별도 작업에서 제거.

---

### 5) Restaurant / Review 엔티티에 벡터 필드 반영

- **내용**
  - `Restaurant`: `vectorEpoch`(Long), `vectorSyncedAt`(Instant) 추가. 벡터 업로드 성공 시 호출할 `incrementVectorEpoch(Instant)` 등 비즈니스 메서드 추가.
  - `Review`: `vectorSyncedAt`(Instant) 추가. 엔티티 컨벤션에 맞게 setter 없이 의미 있는 메서드로만 갱신.
- **진행**
  - 엔티티 컨벤션(ENTITY_CONVENTION.md) 준수: protected 생성자, 정적 팩토리, 변경 메서드로만 상태 변경.

---

### 6) BatchExecution 엔티티 및 Repository

- **내용**
  - `BatchExecution` 엔티티: batch_type(enum), status, started_at, finished_at, total_jobs, success_count, failed_count, stale_count, created_at, updated_at.
  - `BatchExecutionRepository`: JpaRepository + “진행 중인 배치 조회”(선택), “리포트용 기간별 조회” 등.
- **진행**
  - 배치 런 **시작 시**: `BatchExecution` 생성 (status=RUNNING, started_at=NOW(), total_jobs=0, success_count=0, failed_count=0, stale_count=0). 이후 생성·실행하는 Job에는 이 배치 ID를 부여.
  - Job 상태 전이 시 실시간 증분 갱신은 하지 않는다. 배치 종료 시 ai_job 집계로 한 번만 반영 (7장 6번).
  - 배치 **종료 시**: 해당 batch_execution_id로 ai_job 집계 후 success_count, failed_count, stale_count, total_jobs 반영. finished_at=NOW(), status=COMPLETED(또는 FAILED). 총 실행 시간은 finished_at - started_at. **배치 종료 후 항상 Discord 웹훅** 보고. 배치 중단 시에도 Discord 알림 (7장 7.2).
  - 총 실행 시간은 `finished_at - started_at`으로 산출. 리포트·대시보드에서 기간별 조회 시 이 테이블을 사용.

---

### 7) AI Job 엔티티 및 Repository

- **내용**
  - `AiJob` 엔티티: **batch_execution_id**(FK), job_type(enum), restaurant_id, status(enum), base_epoch, attempt_count, created_at 등.
  - `AiJobRepository`: JpaRepository + “PENDING인 Job 조회”, “동일 restaurant_id + job_type 에서 RUNNING 존재 여부”, “batch_execution_id로 Job 목록 조회”(집계용) 등.
- **진행**
  - Job 생성 시 반드시 현재 배치 런의 `BatchExecution.id`를 넣음. 상태 전이 시 6번에 따라 해당 BatchExecution 통계 갱신.
  - 즉시 재시도 없음이므로 `attempt_count`는 “다음 배치” 실행 시 1 증가 등 규칙을 한 곳에 정의.

---

### 8) 감정/요약/비교 분석 결과 엔티티 및 Repository

- **내용**
  - `RestaurantReviewSentiment`, `RestaurantReviewSummary`, `RestaurantComparisonAnalysis` 엔티티 및 각 Repository.
  - 조회: “restaurant_id 기준 최신 1건” (요약/감정은 최신 vector_epoch 또는 최신 analyzed_at), 비교는 최신 1건.
- **진행**
  - 설계의 JSONB는 JPA `@JdbcTypeCode(JsonTypes.JSON)` 또는 프로젝트 공통 방식으로 매핑.
  - 기존 `AiRestaurantReviewAnalysis` / `AiRestaurantComparison` 사용처를 새 엔티티로 바꿀 수 있도록 Repository 메서드 시그니처를 먼저 정의 (예: `findLatestByRestaurantId`).

---

### 9) 벡터 업로드 Job 생성·실행 (24시간 주기)

- **내용**
  - 24시간 주기 스케줄러에서 **배치 시작**: `BatchExecution` 1건 생성 (batch_type=VECTOR_DAILY, status=RUNNING, started_at=NOW()).
  - “업로드 대상 레스토랑” 판단 후 `VECTOR_UPLOAD` Job 생성 (각 Job에 `batch_execution_id` 부여). Job 실행: 해당 레스토랑의 리뷰 목록 조회 → AI 서버 `POST /api/v1/vector/upload` 호출 → 성공 시 Restaurant `vector_epoch++`, `vector_synced_at` 갱신, Review `vector_synced_at` 갱신.
  - **배치 종료**: 모든 대상 Job 처리 후 해당 BatchExecution에 `finished_at`, `total_jobs`, `success_count`, `failed_count`, `stale_count` 반영, `status=COMPLETED`(또는 FAILED).
- **진행**
  - **Job·Worker 분리**: Job 생성(스케줄러)과 Job 소비(Worker)는 분리. Worker는 스레드 풀 상한(기본 4)으로 PENDING Job을 DB에서 가져와 실행, AI 호출은 동기 블로킹+타임아웃 (재시도·Circuit Breaker 보류, 7장 4번). 추후 트리거만 MQ consumer로 교체 가능 (5장).
  - 업로드 대상: “리뷰가 있으나 vector_synced_at이 오래됐거나, 새 리뷰가 있는” 레스토랑 등 정책을 한 곳에 정의 (설계 6.2 “증분 업로드”).
  - **사전 작업**: 같은 batch_type에서 RUNNING Job 전부 FAILED, finished_at 없는 BatchExecution 전부 FAILED. **이전 배치 완료 대기** 후 새 배치 시작 (7장 1·2·5번). Worker 스레드 풀 상한 기본 4, AI 호출 타임아웃 (7장 3·4번). 트랜잭션: Job RUNNING 표시는 REQUIRES_NEW, 실제 업로드 후 Restaurant/Review 갱신도 REQUIRES_NEW. **배치 종료 시** ai_job 집계로 BatchExecution 통계 한 번만 반영, **배치 종료 후 Discord 웹훅** 보고 (7장 6·8번).

---

### 10) 벡터 업로드 성공 시 감정/요약 Job 생성

- **내용**
  - 벡터 업로드 성공·`vector_epoch` 증가 직후, 해당 `restaurant_id`에 대해 `REVIEW_SENTIMENT`, `REVIEW_SUMMARY` Job을 PENDING으로 생성. `base_epoch` = 방금 증가한 `vector_epoch`. 이 Job들은 **다음 ANALYSIS_DAILY 배치**에서 처리되므로, 생성 시점의 BatchExecution이 아니라 “다음에 돌아가는 ANALYSIS_DAILY 배치”에 묶이거나, 배치 실행 시 PENDING Job을 가져와 그때 생성된 BatchExecution에 연결하는 방식 중 하나로 정한다.
- **진행**
  - 9번과 같은 트랜잭션에 넣지 말고, 9번 커밋 후 별도 트랜잭션에서 Job 2개 생성. `batch_execution_id`는 “다음 ANALYSIS_DAILY 런에서 Runner가 BatchExecution 생성 후 Job을 가져갈 때 갱신”하거나, “Job만 먼저 생성(batch_execution_id=NULL 허용)” 후 분석 배치 시작 시 PENDING Job에 현재 batch_execution_id를 세팅하는 방식으로 처리. (정책 확정 시 문서 반영)

---

### 11) 감정 분석 Job 실행기 (Epoch 검증, STALE 처리)

- **내용**
  - **배치 시작**: `BatchExecution` 1건 생성 (batch_type=ANALYSIS_DAILY, status=RUNNING, started_at=NOW()). 이 배치에 속할 PENDING `REVIEW_SENTIMENT` Job을 가져와 `batch_execution_id` 부여(또는 이미 부여된 Job만 조회).
  - Job RUNNING 변경 (REQUIRES_NEW) → AI 감정 분석 API 호출 → 저장 직전 epoch 재검증. 다르면 Job STALE, 같으면 `restaurant_review_sentiment` 저장 후 Job COMPLETED. COMPLETED/FAILED/STALE 시 해당 BatchExecution의 success_count / failed_count / stale_count 갱신.
  - **배치 종료**: 모든 대상 Job 처리 후 BatchExecution에 finished_at, total_jobs, 통계 반영, status 갱신.
- **진행**
  - **사전 작업**: RUNNING Job 전부 FAILED, finished_at 없는 BatchExecution 전부 FAILED, 이전 배치 완료 대기 (7장 1·2·5번). Worker는 **스레드 풀 상한(기본 4)** 으로 PENDING Job 소비. AI 감정 분석 호출은 **동기 블로킹+타임아웃** (7장 4번). “가져오기” 시 동시성 제어(14번)와 맞춰 DB 락으로 RUNNING 1개만 허용. 실패 시 Job FAILED, 즉시 재시도 없음 (15번). **배치 종료 시** ai_job 집계로 통계 반영, **Discord 웹훅** 보고 (7장 6·8번).

---

### 12) 요약 분석 Job 실행기 (Epoch 검증, STALE 처리)

- **내용**
  - 11번과 동일 패턴: 배치 시작 시 BatchExecution(ANALYSIS_DAILY) 생성 → PENDING `REVIEW_SUMMARY` Job 처리 → RUNNING → AI 요약 API → epoch 재검증 → STALE 또는 `restaurant_review_summary` 저장 → COMPLETED. Job 상태 전이 시 BatchExecution 통계 갱신, 배치 종료 시 finished_at·total_jobs·status 반영.
- **진행**
  - 감정과 요약은 서로 독립. 별도 Runner/스케줄러로 두고, 각각 사전 작업(고아 Job·미종료 배치 FAILED, 이전 배치 완료 대기), Worker 스레드 풀 상한(기본 4), AI 동기 블로킹+타임아웃, 배치 종료 시 ai_job 집계·Discord 웹훅 적용 (7장).
---

### 13) 비교 분석 Job 생성·실행 (1주일 주기)

- **내용**
  - 1주일 주기 스케줄러에서 **배치 시작**: `BatchExecution` 1건 생성 (batch_type=COMPARISON_WEEKLY, status=RUNNING, started_at=NOW()). 모든(또는 조건에 맞는) 레스토랑에 대해 `RESTAURANT_COMPARISON` Job 생성 (PENDING, batch_execution_id 부여).
  - 실행: RUNNING → AI 비교 API → `restaurant_comparison_analysis` 저장 (vector_epoch 무관) → COMPLETED. Job 완료/실패 시 BatchExecution 통계 갱신.
  - **배치 종료**: finished_at, total_jobs, success_count, failed_count, stale_count, status 반영.
- **진행**
  - 비교는 Epoch 무효화 대상 아님. 사전 작업·이전 배치 완료 대기·스레드 풀 상한(기본 4)·AI 타임아웃·배치 종료 시 집계·Discord 웹훅 동일 적용 (7장). 기존 `@Scheduled` 비교 분석 로직을 이 Job 실행기로 대체.

---

### 14) Job 동시성 제어 (Restaurant 단위 RUNNING 1개)

- **내용**
  - “동일 restaurant_id + job_type 에서 RUNNING 1개만 허용.” 한 배치 안에서 **여러 Job을 동시에 처리**(멀티 스레드)하므로, 이 제약은 **멀티 스레드 + DB 락**으로 구현한다.
- **진행**
  - RUNNING 선점: `UPDATE ai_job SET status = 'RUNNING' WHERE id = ? AND status = 'PENDING'` 실행 후 affected rows = 1일 때만 해당 Job 실행. 0이면 다른 워커가 선점한 것이므로 스킵.
  - 동일 (restaurant_id, job_type)에 대해 RUNNING이 이미 있는지 확인하려면, 선점 전에 “해당 (restaurant_id, job_type)으로 RUNNING 존재 여부” 조회하거나, UPDATE 조건에 서브쿼리로 “동일 restaurant_id, job_type 인 RUNNING이 없을 때만” 조건을 넣는 방식으로 구현. 구현 시 DB 락(선택적 FOR UPDATE) 또는 위 UPDATE 경쟁으로 보장.
  - **워커 스레드 풀 상한**을 둔다 (기본 4, 7장 3번). 동일 restaurant_id + job_type RUNNING 1개만 보장한다.

---

### 15) 실패 처리 (즉시 재시도 없음, 다음 배치 재처리)

- **내용**
  - AI 호출 또는 저장 실패 시 해당 Job을 FAILED로 두고, 즉시 재시도하지 않음.
  - 다음 배치(24시간 또는 1주일)에서 “FAILED인 Job을 다시 PENDING으로 되돌리거나”, “같은 restaurant_id + base_epoch에 대해 새 Job을 만들지 않고 기존 FAILED를 재시도” 등 정책 선택.
  - “동일 epoch 2회 실패 시 표시”: attempt_count를 증가시키고, 2회 이상이면 별도 플래그/상태로 표시하거나 스킵 리스트에 넣기.
- **진행**
  - 실패 시 Job 상태만 FAILED + attempt_count 증가. 재처리 정책은 스케줄러/Runner에서 “PENDING + (FAILED이고 attempt_count < 2)” 등을 조회하는 방식으로 구현 가능. Job 상태 전이 시 해당 BatchExecution의 failed_count 등도 갱신 (6·7번).

---

### 16) 조회 경로 이전 (상세/메인/AI추천 → 새 테이블)

- **내용**
  - `RestaurantAiSummaryService`: 현재 `AiRestaurantReviewAnalysis` / `AiRestaurantComparison`에서 읽는 부분을, `RestaurantReviewSummary`(또는 Sentiment) / `RestaurantComparisonAnalysis`의 “restaurant_id 기준 최신 1건”으로 변경.
  - `MainService`의 `fetchSummaries`, AI 추천 섹션: 동일하게 새 요약/감정 테이블에서 최신 데이터 조회.
  - `RestaurantRepository.findAiRecommendRestaurants`: 정렬/조인을 `restaurant_review_sentiment`(또는 요약) 최신 행과 조인하도록 변경.
- **진행**
  - “최신” 정의: 감정/요약은 `(restaurant_id, vector_epoch)` 최대 1건 또는 `analyzed_at DESC LIMIT 1`. 비교는 `restaurant_id`당 1건.
  - 기존 통합 스냅샷 테이블은 읽지 않도록만 전환. 삭제는 17번 이후로 미룸.

---

### 17) 기존 분석 트리거 제거 및 정리

- **내용**
  - `ReviewCreatedEvent` → `ReviewCreatedAiAnalysisEventListener` / 메시지큐 `ReviewCreatedMessageQueueConsumerRegistrar`에서 `RestaurantReviewAnalysisService.onReviewCreated` 호출 제거 또는 비활성화.
  - `RestaurantReviewAnalysisService`의 `onReviewCreated`, `runScheduledComparisonAnalysis` 제거 또는 “새 Job 생성”으로 대체.
  - `RestaurantReviewAnalysisSnapshotService`, `RestaurantReviewAnalysisStateService` 등 기존 스냅샷/상태 서비스 제거 또는 deprecated.
  - `AiRestaurantReviewAnalysis`, `AiRestaurantComparison` 엔티티 및 Repository는 조회 경로 이전(16) 후 사용처가 없으면 제거. 테이블 삭제는 별도 마이그레이션으로 진행 (기존 데이터 백업/이관 정책 확인 후).
- **진행**
  - 기능 플래그로 “기존 분석 비활성화” 후 새 배치만 동작하게 한 뒤, 모니터링 후 기존 코드 제거.
  - data.sql 등에서 `ai_restaurant_review_analysis` / `ai_restaurant_comparison` 참조 제거.

---

## 4. 구현 순서 제안

1. **DB 먼저 (1, 2, 3, 4)**  
   마이그레이션 적용: Restaurant/Review 벡터 필드 → **batch_execution** → ai_job (batch_execution_id FK) → 분석 결과 3종. 기존 테이블 유지하고 새 테이블·컬럼만 추가.

2. **엔티티·Repository (5, 6, 7, 8)**  
   Restaurant/Review 벡터 필드, **BatchExecution**, AiJob(batch_execution_id), 감정/요약/비교 결과 엔티티·Repository 추가.

3. **Job·Worker 분리 설계 및 트리거 계층**  
   **Job(생성/저장)** 과 **Worker(Job 소비·실행)** 를 계층적으로 분리한다. 현재는 스케줄러가 “배치 시작 → BatchExecution 생성 → Job 생성 → Worker 호출”까지 한 흐름에서 수행하지만, Worker는 “PENDING Job을 DB에서 조회해 처리”하는 인터페이스로만 의존하도록 구현한다. 나중에 트리거를 스케줄러에서 **MQ consumer**로 바꿀 수 있도록, “Job 생성”과 “Job 소비” 경로만 교체 가능하게 둔다.

4. **벡터 업로드 파이프라인 (9, 10)**  
   24시간 스케줄러에서 배치 시작 → BatchExecution(VECTOR_DAILY) 생성 → Job 생성(PENDING, batch_execution_id 부여) → **Worker**가 해당 배치의 Job을 멀티 스레드로 소비·실행. AI 호출은 **동기 블로킹**. 업로드 성공 시 감정/요약 Job 생성 → 배치 종료 시 통계·finished_at 반영.

5. **분석 실행기 (11, 12, 13)**  
   감정/요약/비교 각각: 배치 시작 시 BatchExecution 생성 → PENDING Job 생성 또는 기존 PENDING 조회 후 batch_execution_id 부여 → **Worker**가 Job을 **멀티 스레드 + DB 락**(14번)으로 동시 처리. AI 호출은 **동기 블로킹**. 배치 종료 시 finished_at·total_jobs·통계·status 반영. 실패 처리(15) 반영.

6. **조회 전환 (16)**  
   상세/메인/AI추천이 새 테이블만 보도록 전환. 이때까지는 기존 테이블과 병행 가능.

7. **기존 경로 제거 (17)**  
   이벤트/스케줄 기반 분석 비활성화 및 코드·테이블 정리.

---

## 5. 실행 모델 및 비동기 구현 정책

배치 실행·동시성·AI 호출·MQ 전환에 대한 확정 정책이다.

| 항목 | 정책 |
|------|------|
| **실행 모델** | 한 배치 안에서 **여러 Job을 동시에 처리**한다. 동일 `restaurant_id` + `job_type` 에서 RUNNING 1개만 허용하는 제약은 **멀티 스레드 + DB 락**으로 구현한다 (Job 선점 시 `UPDATE ... WHERE status = 'PENDING'` 경쟁, affected rows = 1일 때만 실행). |
| **AI 호출 방식** | **동기 블로킹**이다. 분석 작업 전용 스레드가 할당되므로, 그 안에서 논블로킹 I/O를 쓸 이점이 없다고 보며, RestClient 등 동기 호출로 통일한다. |
| **MQ 사용 여부** | 사실상 **확정**이다. 마이그레이션을 쉽게 하려면 **Job과 Worker를 분리**하고, 현재는 **스케줄러**가 배치 시작·Job 생성·Worker 호출을 트리거하지만, **나중에 MQ로 트리거만 교체**할 수 있도록 구현한다. 즉 Worker는 “Job 목록을 DB에서 가져와 처리”하는 인터페이스에만 의존하고, “누가 Worker를 깨우는지”(스케줄러 vs MQ consumer)는 추상화해 교체 가능하게 둔다. |
| **전역 동시성** | **워커 스레드 풀 상한**을 둔다. 기본값 **4** (3~5 중 적절한 값). 설정으로 조정 가능. 동일 `restaurant_id` + `job_type` RUNNING 1개만 보장 (7장 3번). |

---

## 6. 참고

- 트랜잭션 경계는 설계 9장대로: Job RUNNING/결과 저장/실패 처리 각각 REQUIRES_NEW.
- 옵저버빌리티(설계 10): 로깅 정책(logging-policy.md)에 맞춰 Job 시작/완료/실패/STALE 로그와, 필요 시 메트릭 추가.
- MQ 전환 시: Job·Worker 분리가 되어 있으므로, 스케줄러 대신 MQ consumer가 “배치 시작 또는 Job 단위 메시지”를 받아 Worker를 호출하도록만 바꾸면 된다. 동시성 제어(14)는 그대로 DB 락으로 유지.

---

## 7. 운영 안정성 우려 사항 및 확정 보완 정책

### 7.1 확정 보완 정책

아래는 운영 안정성을 위해 **확정한** 보완 방안이다.

| # | 우려 | 확정 정책 |
|---|------|-----------|
| 1 | **RUNNING 고아(Orphan) Job** | **다음 배치 실행 시 사전 작업**으로, 해당 batch_type에서 상태가 RUNNING인 Job을 **전부 FAILED**로 변경한 뒤 본 배치(Job 생성·Worker 실행)를 진행한다. |
| 2 | **BatchExecution 미종료** | **다음 배치 실행 시 사전 작업**으로, 같은 batch_type에서 **finished_at이 없는** BatchExecution을 **전부 FAILED**로 상태 변경한 뒤 새 BatchExecution을 생성·실행한다. |
| 3 | **전역 동시성 미제한** | **워커 스레드 풀 상한**을 둔다. 기본값은 **4** (3~5 중 적절한 값). 설정으로 조정 가능하게 구현. |
| 4 | **AI 서버 의존성** | 재시도·Circuit Breaker는 **일단 보류**. **AI 호출 타임아웃만** 추가한다. 추후 운영 중 배치 실패가 잦으면 재시도·Circuit Breaker 도입을 검토한다. |
| 5 | **배치 중복 실행** | **이전 배치 완료 대기**가 필요하다. 같은 batch_type으로 이미 RUNNING인 BatchExecution이 있으면 새 배치는 시작하지 않고, 이전 배치가 finished_at을 채울 때까지 대기(또는 스킵 후 다음 주기까지 대기)한다. |
| 6 | **BatchExecution 카운트 동시 갱신** | **배치 종료 시점에만** 해당 batch_execution_id에 대한 ai_job을 집계해 success_count, failed_count, stale_count, total_jobs를 **한 번만** 반영한다. (실시간 증분 갱신 없음) |
| 8 | **모니터링·알림** | **배치 종료 후 항상** 배치 실행 통계를 **Discord 웹훅**으로 보고한다. **배치 작업 중단 시에도** 알림을 보내며, 구현상 가능하다 (아래 7.2 참고). |

### 7.2 배치 중단 시 알림 (가능 여부)

**가능하다.**

- **정상 종료**: 배치 종료 시 finished_at·통계 반영 후 Discord 웹훅 호출 (batch_type, started_at, finished_at, total_jobs, success_count, failed_count, stale_count, 소요 시간 등).
- **중단 시**: 다음 두 가지로 감지·알림 가능.
  - **Graceful shutdown**: JVM shutdown hook 또는 `@PreDestroy`에서 “현재 RUNNING인 배치가 있으면” 해당 BatchExecution id·batch_type·started_at·이미 처리된 job 수 등을 담아 Discord 웹훅으로 “배치 중단” 알림 전송.
  - **비정상 종료**(OOM, kill -9 등): 프로세스가 죽으면 당장은 알림 불가. **다음 배치 사전 작업**(1·2번)에서 고아 RUNNING Job·미종료 BatchExecution을 FAILED로 정리한 뒤, **회수 건수가 1건 이상이면** Discord 웹훅으로 **회수된 고아 Job N건, 회수된 미종료 BatchExecution M건**(batch_type 포함)을 보고하여 비정상 종료를 간접 인지한다. 사전 작업 시 회수한 내용은 이렇게 Discord에 항상 보고한다.

### 7.3 배포 시 인프라 배치 (미해결·참고)

- **스케줄러 방식**: 롤링 배포 시 인스턴스가 내려가면 그 인스턴스에서 처리 중이던 Job은 RUNNING 고아가 된다. **Graceful shutdown**을 넣으면 “새 Job 가져오기 중단 → 진행 중인 Job만 완료 후 종료”는 가능하지만, 강제 종료(kill -9, OOM 등)에는 대응할 수 없다. 따라서 **고아는 다음 배치 사전 작업(1번)** 으로 전부 FAILED 처리하는 방식에 의존하게 된다.
- **MQ 도입 후**: Consumer가 메시지 처리 중단/재큐/가시성 타임아웃 등을 지원하면, 인스턴스 종료 시 “처리 중이던 메시지”가 다시 노출되어 다른 인스턴스에서 재처리되는 식으로 **자연스럽게** 해결된다. 스케줄러만 쓸 때는 1번 사전 작업으로 회수하는 수준이 현실적인 선이다.
