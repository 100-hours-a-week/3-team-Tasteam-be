# Flyway 컨벤션

## 1) 목적
- DB 변경을 **예측 가능**하고 **재현 가능**하게 만들기 위한 규칙을 정의한다.
- 버전 충돌/누락/순서 오류를 예방한다.

---

## 2) 디렉터리 규칙

```
app-api/
  src/main/resources/db/migration/    # SQL 마이그레이션
  src/main/java/db/migration/          # Java 마이그레이션
```

- SQL은 반드시 `app-api/src/main/resources/db/migration` 하위에 둔다.
- Java 마이그레이션은 반드시 `db.migration` 패키지에 둔다.

---

## 3) 파일명 컨벤션

### 3-1. Versioned SQL
- 형식: `VyyyyMMddHHmmss__short_description.sql`
- 예시: `V20260203091530__add_member_profile_image.sql`
- 규칙:
  - `yyyyMMddHHmmss`는 **KST 기준 14자리**(초 단위) 타임스탬프 사용.
  - `short_description`은 **lower_snake_case**로 작성.
  - 한 파일에 **한 목적**만 담는다.

### 3-2. Repeatable SQL
- 형식: `R__short_description.sql`
- 예시: `R__refresh_views.sql`
- 규칙:
  - idempotent하게 작성(여러 번 실행되어도 결과 동일).

### 3-3. Java Migration
- 클래스명 규칙은 SQL과 동일한 패턴을 따른다.
- 예시:
  - `V20260203091530__add_member_profile_image` (클래스명)
  - `R__sync_enum_check_constraints`
- 패키지: `db.migration`

---

## 4) 버전 전략
- **타임스탬프 버전**을 기본으로 한다.
- 같은 시각에 여러 파일이 필요하면 **초 단위를 조정하거나**, 뒤에 `_1`, `_2`를 추가한다.
- 적용 순서: 버전 번호 오름차순.

---

## 5) SQL 작성 컨벤션

### 5-1. 일반 규칙
- DDL과 DML을 **분리**한다.
- 반드시 **명시적 제약 이름**을 사용한다.
  - 예: `chk_image_purpose`, `uq_domain_image_link`, `fk_domain_image_image_id` 등
- 가능한 경우 `IF EXISTS` / `IF NOT EXISTS` 사용.
- 대용량 테이블 변경 시 **락 범위**와 **다운타임**을 고려한다.

### 5-2. 트랜잭션
- 기본은 트랜잭션 실행.
- Postgres에서 `CREATE INDEX CONCURRENTLY` 등 **비트랜잭션**이 필요한 경우:

```sql
-- flyway:transactional=false
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_member_email ON member (email);
```

### 5-3. 데이터 마이그레이션
- **백필은 별도 버전 파일**로 분리한다.
- 대용량 업데이트는 배치/분할 처리 고려.
- 삭제/정리 작업은 반드시 사전 검증 쿼리와 함께 설계한다.

---

## 6) Java Migration 작성 컨벤션

### 6-1. 사용 기준
- SQL이 **enum/설정값 기반**으로 동적으로 생성되어야 하는 경우.
- 다중 DB 대응이나 런타임 결정이 필요한 경우.

### 6-2. 기본 구조
```java
package db.migration;

import java.sql.Connection;
import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class R__example extends BaseJavaMigration {
	@Override
	public void migrate(Context context) throws Exception {
		try (Statement stmt = context.getConnection().createStatement()) {
			stmt.execute("ALTER TABLE ...");
		}
	}
}
```

### 6-3. 체크섬 규칙
- Repeatable Java Migration은 **getChecksum()**을 사용해 재실행 트리거를 명확히 한다.
- enum 기반 제약 갱신 시 enum 목록 문자열을 체크섬에 포함한다.

---

## 7) 스키마 변경 설계 원칙

1. **Backward Compatible → Backfill → Switch** 순서 준수
   - 기존 코드와 동시에 안전하게 동작할 수 있게 변경한다.
2. 컬럼 제거는 **2단계 이상**으로 진행
   - (1) 사용 중지 → (2) 제거 마이그레이션
3. 인덱스/제약 추가 시 **온라인 적용 가능 여부** 검토

---

## 8) 리뷰 체크리스트
- 파일명/버전이 컨벤션에 맞는가?
- 변경이 단일 목적에 집중되어 있는가?
- 대용량 테이블에 대한 락/성능 이슈는 없는가?
- 롤백이 필요한 경우 대체 마이그레이션으로 복구 가능한가?

---

## 9) 금지/주의 사항
- 이미 적용된 **버전 파일 수정 금지**
- 운영 DB에 수동 SQL 실행 금지(긴급 대응 제외)
- Flyway 실패 후 **repair 남용 금지** (원인 분석 후 적절한 수정)

---

## 10) 예시 템플릿

### Versioned SQL 예시
```sql
-- V20260203091530__add_member_profile_image.sql
ALTER TABLE member
  ADD COLUMN profile_image_url VARCHAR(512);
```

### Repeatable SQL 예시
```sql
-- R__refresh_member_views.sql
DROP VIEW IF EXISTS v_member_profile;
CREATE VIEW v_member_profile AS
SELECT id, email, nickname FROM member;
```

### Repeatable Java 예시
```java
public class R__sync_enum_check_constraints extends BaseJavaMigration {
	@Override
	public Integer getChecksum() {
		return 123456789; // enum 기반 해시 값
	}
	@Override
	public void migrate(Context context) throws Exception {
		// enum 기반 CHECK 제약 재생성
	}
}
```

---

## 11) 추가 운영 지침

- 마이그레이션은 반드시 **PR 단위로 리뷰**한다.
- 배포 전 **마이그레이션 스크립트 단독 검토** 시간을 확보한다.
- 문제가 발생하면 **새 버전 마이그레이션**으로 보정한다. (수정/재작성 금지)
