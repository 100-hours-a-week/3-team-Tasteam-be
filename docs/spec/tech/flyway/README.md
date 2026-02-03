| 항목 | 내용 |
|---|---|
| 문서 제목 | Flyway 사용 가이드 |
| 문서 목적 | DB 스키마/데이터 변경을 Flyway로 일관되게 관리하기 위한 사용 방법과 운영 기준을 명확히 한다 |
| 작성 및 관리 | Tasteam BE |
| 최초 작성일 | 2026.02.03 |
| 최종 수정일 | 2026.02.03 |
| 문서 버전 | v1.0 |

<br>

# Flyway 사용 가이드

---

## 1) 개요
Flyway는 DB 스키마/데이터 변경을 **버전 관리**하고, 배포 시점에 **순서 보장** 및 **재현 가능**하도록 해준다.
애플리케이션 코드와 DB 변경의 싱크를 맞추고, 수동 SQL 적용에서 발생하는 누락/순서 오류를 줄이는 것이 목적이다.

---

## 2) 언제 Flyway를 사용하나
다음 변경은 **무조건 Flyway로 관리**한다.

- 테이블 생성/변경/삭제 (DDL)
- 인덱스 추가/삭제
- 제약 조건 추가/수정 (PK/UK/FK/CHECK)
- 데이터 마이그레이션 (기존 데이터 보정/백필)
- 뷰/함수/트리거 등 DB 객체 변경

예외(즉시 대응 필요, 운영 장애 대응 등)는 **긴급 운영 절차**로 수행하되, 사후에 반드시 Flyway 마이그레이션으로 복기한다.

---

## 3) 환경별 적용 정책

### 3-1. 운영(Prod)
- **Flyway 활성화** 상태로 애플리케이션 기동 시 자동 적용.
- JPA `ddl-auto`는 `validate`(또는 `none`)로 고정해 **스키마 변경은 Flyway만** 담당하도록 한다.

### 3-2. 스테이징/개발 통합 환경
- 운영과 동일하게 Flyway 활성화.
- 데이터 초기화가 필요하면 별도의 seed SQL 또는 테스트 데이터 로더 사용.

### 3-3. 로컬(Local)
- 기본 설정은 `spring.flyway.enabled=false` (현재 로컬 프로필 기준).
- **실제 운영과 동일한 스키마 검증이 필요할 때**:
  - `spring.flyway.enabled=true`로 변경
  - `spring.jpa.hibernate.ddl-auto=validate` 권장
  - `spring.sql.init.mode=always` 유지 여부는 팀 합의(로컬 seed 필요 여부)에 따라 선택

### 3-4. 테스트(Test)
- 기본 설정은 `spring.flyway.enabled=false` (현재 테스트 프로필 기준).
- 이유: 테스트는 `ddl-auto=create-drop`으로 스키마를 생성하며, 현재는 버전 마이그레이션( `V__` )이 충분히 갖춰져 있지 않아 Flyway를 먼저 실행하면 실패할 수 있다.
- 마이그레이션 경로 검증이 필요한 통합 테스트에서는 Flyway를 활성화하고 `ddl-auto=validate`로 전환하는 **전용 프로필**을 사용한다.

---

## 4) Flyway 실행 방법

### 4-1. 애플리케이션 기동 시 자동 적용 (권장)
- Flyway는 애플리케이션 시작 시점에 자동으로 마이그레이션을 실행한다.
- 활성 조건: `spring.flyway.enabled=true` + 데이터소스 구성 완료.

예시(로컬에서 Flyway 활성화):

```bash
SPRING_PROFILES_ACTIVE=local \
SPRING_FLYWAY_ENABLED=true \
SPRING_JPA_HIBERNATE_DDL_AUTO=validate \
./gradlew :app-api:bootRun
```

### 4-2. CLI/별도 실행
- 현재 프로젝트에는 Flyway Gradle Plugin이 설정되어 있지 않다.
- **앱 기동 없이 별도로 실행하고 싶다면** Flyway CLI를 사용하거나, 필요 시 Gradle 플러그인 도입을 별도 RFC로 진행한다.

---

## 5) 마이그레이션 종류

### 5-1. Versioned Migration (`V...__...`)
- **스키마 변경** 및 **데이터 보정**에 사용.
- 한 번 적용되면 재실행되지 않는다(버전 고정).

### 5-2. Repeatable Migration (`R__...`)
- **파생 객체/정의 재생성**에 사용.
- 체크섬이 변경되면 재실행된다.
- 예: View, Function, CHECK 제약, Enum 기반 제약 갱신

---

## 6) 디렉터리 구조

```
app-api/
  src/main/resources/db/migration/    # SQL 마이그레이션
  src/main/java/db/migration/          # Java 마이그레이션
```

- SQL: `classpath:db/migration` 경로에 위치.
- Java: 패키지 `db.migration` 하위에 위치.

---

## 7) 작성 방법 요약

### 7-1. SQL 마이그레이션 작성
- 파일명 컨벤션은 컨벤션 문서 참조.
- **한 파일에 한 목적** 원칙.
- DDL과 DML이 모두 필요하면 **분리 권장**.

### 7-2. Java 마이그레이션 작성
- enum/설정값에서 SQL을 동적으로 생성해야 하는 경우에 사용.
- 예시: `R__sync_enum_check_constraints.java`

---

## 8) 실무 작업 절차 (언제, 어떻게)

1. **요구사항 확인**: 스키마 변경인지, 데이터 보정인지 구분한다.
2. **마이그레이션 유형 결정**:
   - 스키마/데이터 변경: `V...` (Versioned)
   - 파생 객체/enum 기반 제약: `R__...` (Repeatable)
3. **파일 생성**:
   - SQL: `app-api/src/main/resources/db/migration`에 생성
   - Java: `app-api/src/main/java/db/migration`에 클래스 추가
4. **로컬 검증**:
   - Flyway 활성화 후 앱 기동으로 적용 확인
   - 필요 시 `ddl-auto=validate`로 스키마 불일치 확인
5. **리뷰 & 병합**:
   - 마이그레이션만 단독 리뷰하는 시간을 확보
6. **배포 시점 적용**:
   - 운영/스테이징에서 자동 적용
   - 실패 시 수정하지 말고 **새 버전 마이그레이션**으로 보정

---

## 9) 운영/배포 체크리스트

- 마이그레이션 파일명/순서가 컨벤션에 맞는가?
- 큰 테이블 변경 시 **락 시간/인덱스 생성 비용**을 고려했는가?
- 데이터 백필/정합성 검증 쿼리를 포함했는가?
- 롤백이 필요한 경우, **대체 마이그레이션**(보정용 버전 파일)로 대응 가능한가?

---

## 10) FAQ

### Q1. Flyway가 실패하면 어떻게 하나?
- 실패 지점 SQL 로그 확인 → 수정 후 **새 버전 마이그레이션 추가**로 보정.
- 이미 적용된 버전 파일은 수정하지 않는다.

### Q2. 로컬에서 스키마를 초기화해야 하는데?
- 로컬에서는 `ddl-auto=create`를 임시로 사용할 수 있으나, 운영과 차이가 생길 수 있다.
- **스키마 일관성 검증이 필요하면 Flyway 활성화** 후 `ddl-auto=validate` 사용.

---

## 11) 관련 문서
- `docs/convention/flyway/README.md` (Flyway 컨벤션)
- `docs/troubleshooting-flyway-enum-check-constraint.md` (enum CHECK 제약 자동화 이슈)
