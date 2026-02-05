# Enum 수정 시 CHECK 제약 자동 반영이 안 되던 문제

## 에러/증상
- `DomainType`, `FilePurpose` enum 값을 추가/변경하면 DB의 CHECK 제약이 그대로라서 `INSERT/UPDATE` 시 `violates check constraint` 오류 발생.
- 로컬/테스트 환경마다 수동 DDL을 적용해야 해 누락이 잦았음.

## 원인
- CHECK 제약을 하드코딩한 SQL로 한 번만 생성해 놓고, enum 변경 시 재적용이 자동화되어 있지 않음.

## 해결
- Flyway **repeatable Java migration**(`R__sync_enum_check_constraints.java`)을 추가해 enum 목록을 읽어 CHECK 제약을 매번 재생성하도록 변경.
- 구현 방식
  - enum 값 목록을 `'VALUE'` 문자열로 합쳐 `ALTER TABLE ... DROP CONSTRAINT IF EXISTS ... ADD CONSTRAINT ... CHECK (col IN (...))` SQL을 생성.
  - `getChecksum()`에 enum 문자열 해시를 사용하여 enum이 바뀌면 Flyway가 자동으로 재실행.

## 적용 커밋/파일
- `app-api/src/main/java/db/migration/R__sync_enum_check_constraints.java`
- 테스트로 `./gradlew :app-api:test --tests "com.tasteam.domain.member.controller.MemberControllerTest" --tests "com.tasteam.domain.member.service.MemberProfileImageIntegrationTest"` 실행 성공.

## 사용 방법/운영 팁
- Flyway가 활성화된 프로필에서 앱 기동 시 자동 적용됨.
- enum을 수정하면 별도 SQL 없이 제약이 자동 갱신된다.
