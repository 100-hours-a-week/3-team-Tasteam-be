| 항목 | 내용 |
|---|---|
| 문서 제목 | 데이터 부트스트랩(Bootstrap) 테크 스펙 |
| 문서 목적 | 로컬/개발 환경에서 메인/이벤트/공지 기능 검증에 필요한 초기 데이터 세트를 안전하게 적재하는 절차와 책임 경계를 정의한다. |
| 작성 및 관리 | Backend Team |
| 최초 작성일 | 2026.02.11 |
| 최종 수정일 | 2026.02.11 |
| 문서 버전 | v1.0 |

<br>

# 데이터 부트스트랩(Bootstrap) - BE 테크스펙

---

# **[1] 배경 (Background)**

## **[1-1] 목표**

- 신규 기능 개발 시 공통 seed를 빠르게 주입해 API 검증 시간을 단축한다.
- Flyway 스키마 마이그레이션과 테스트/샘플 데이터를 분리해 운영 리스크를 낮춘다.
- 환경별(로컬/개발/스테이징) 데이터 생성 기준을 표준화한다.

## **[1-2] 문제 정의**

- `spring.sql.init.mode=never` 정책 하에서 `data.sql` 자동 주입을 사용할 수 없다.
- 마이그레이션 SQL에 샘플 데이터를 섞으면 운영 DB 오염 가능성이 높다.
- 팀원별 임의 seed로 인해 재현성이 떨어진다.

<br>

---

# **[2] 목표가 아닌 것 (Non-goals)**

- 운영(Prod) 환경 자동 seed
- 대용량 성능 벤치마크 데이터 생성
- BI/분석용 데이터셋 구성

<br>

---

# **[3] 설계 및 기술 자료**

## **[3-1] 원칙**

- 스키마 변경은 Flyway, 샘플 데이터는 Bootstrap 스크립트로 분리한다.
- Bootstrap은 명시적 실행 방식만 허용한다.
- 동일 스크립트를 여러 번 실행해도 결과가 안정적(멱등)이어야 한다.

## **[3-2] 대상 도메인과 최소 데이터**

### Main 검증 세트

- `restaurant`: 60건 이상
- `restaurant_food_category`: 식당당 최소 1건
- `ai_restaurant_review_analysis`: 식당 30건 이상
- `domain_image`: 식당 40건 이상
- `group/group_member`: 위치 fallback 검증용 1세트 이상

### Event/Notice 검증 세트

- `notice`: 10건 (최신/과거 공지 혼합)
- `event`: 15건
- `event` 상태 분포
  - `UPCOMING` 5건
  - `ONGOING` 5건
  - `ENDED` 5건

## **[3-3] 실행 방식**

권장 경로:
- `scripts/bootstrap/local/`
- `scripts/bootstrap/dev/`

권장 파일 구성:
- `001-main-seed.sql`
- `002-notice-event-seed.sql`
- `apply.sh`

`apply.sh` 역할:
1. 대상 DB 연결 확인
2. 트랜잭션 단위로 SQL 실행
3. 건수 검증 쿼리 실행
4. 실패 시 롤백 및 종료 코드 반환

## **[3-4] 멱등 전략**

- 기준 키 기반 UPSERT 사용 (`ON CONFLICT DO UPDATE`)
- 삭제가 필요한 경우 hard delete 대신 소프트 delete 우선
- 상태 분포 데이터는 고정 키(`seed_key`) 또는 제목 prefix(`BOOTSTRAP_`)로 관리

## **[3-5] 환경 안전장치**

- `APP_ENV=local|dev|staging` 외 값에서는 실행 거부
- `prod` 문자열이 포함된 DB URL에서 실행 거부
- dry-run 모드 제공 (`BOOTSTRAP_DRY_RUN=true`)

## **[3-6] 검증 체크리스트**

- 메인 API
  - `GET /api/v1/main`
  - `GET /api/v1/main/home`
  - `GET /api/v1/main/ai-recommend`
- 공지/이벤트 API
  - `GET /api/v1/notices`
  - `GET /api/v1/events`
  - `GET /api/v1/events?status=ONGOING`
  - `GET /api/v1/events?status=UPCOMING`
  - `GET /api/v1/events?status=ENDED`

## **[3-7] 운영 절차 (권장)**

1. Flyway 마이그레이션 적용 완료 확인
2. Bootstrap dry-run 실행
3. Bootstrap 실제 실행
4. API smoke 테스트 실행
5. 실패 시 seed 롤백 또는 삭제 스크립트 실행

## **[3-8] 오픈 이슈**

1. bootstrap 스크립트를 SQL 중심으로 둘지, Kotlin/Java 실행기로 둘지
2. 테스트 fixture와 bootstrap seed를 분리 저장할지 단일화할지
3. 스테이징 환경에서 주기적 재적재 정책 필요 여부
