# Hibernate SEQUENCE 시퀀스 이름 불일치 문제

## 증상

`dev`/`prod` 환경에서 애플리케이션 시작 시 다음 에러 발생:

```
Schema-validation: missing sequence [chat_message_file_seq]
```

## 원인 분석

### 환경별 설정 차이

**Local/Test 환경:**
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop  # Hibernate가 스키마 자동 생성
```

**Dev/Prod 환경:**
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # 기존 스키마 검증만
```

### 시퀀스 이름 불일치

**Flyway 마이그레이션 (SQL):**
```sql
CREATE TABLE chat_message_file (
    id BIGSERIAL PRIMARY KEY,  -- PostgreSQL이 'chat_message_file_id_seq' 생성
    ...
);
```

**엔티티 (JPA):**
```java
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE)  // Hibernate가 'chat_message_file_seq' 기대
private Long id;
```

| 생성 방식 | 시퀀스 이름 |
|-----------|-------------|
| PostgreSQL `BIGSERIAL` | `테이블명_id_seq` |
| Hibernate 기본 SEQUENCE | `테이블명_seq` |

### 근본 원인

1. Flyway 마이그레이션에서 `BIGSERIAL`로 테이블 생성
2. PostgreSQL이 `chat_message_file_id_seq` 시퀀스 자동 생성
3. `ddl-auto: validate` 환경에서 Hibernate가 `chat_message_file_seq` 시퀀스 검증 시도
4. 시퀀스 이름 불일치로 검증 실패

### Local에서 문제가 없던 이유

`ddl-auto: create-drop`은 Hibernate가 스키마를 직접 생성하므로 Hibernate 명명 규칙에 맞는 시퀀스가 생성됨.

## 해결 방법

### 방법 1: @SequenceGenerator로 정확한 시퀀스 이름 지정 (채택)

```java
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "chat_message_file_seq_gen")
@SequenceGenerator(
    name = "chat_message_file_seq_gen",
    sequenceName = "chat_message_file_id_seq",  // PostgreSQL BIGSERIAL이 생성한 이름
    allocationSize = 1
)
@Column(name = "id", nullable = false)
private Long id;
```

### 방법 2: GenerationType.IDENTITY 사용

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

`IDENTITY`는 PostgreSQL `SERIAL`/`BIGSERIAL`과 호환됨. 단, 배치 INSERT 최적화 불가.

### 방법 3: Flyway 마이그레이션에서 시퀀스 별도 생성

```sql
CREATE SEQUENCE chat_message_file_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE chat_message_file (
    id BIGINT PRIMARY KEY DEFAULT nextval('chat_message_file_seq'),
    ...
);
```

## 적용된 변경사항

다음 엔티티에 `@SequenceGenerator` 추가:

| 엔티티 | 시퀀스 이름 |
|--------|-------------|
| ChatMessageFile | `chat_message_file_id_seq` |
| ChatMessage | `chat_message_id_seq` |
| ChatRoom | `chat_room_id_seq` |
| ChatRoomMember | `chat_room_member_id_seq` |
| Member | `member_id_seq` |
| Subgroup | `subgroup_id_seq` |
| SubgroupMember | `subgroup_member_id_seq` |

## 교훈

1. `GenerationType.SEQUENCE` 사용 시 `@SequenceGenerator`로 시퀀스 이름 명시 권장
2. Flyway 마이그레이션과 JPA 엔티티 간 시퀀스 명명 규칙 일치 필요
3. `ddl-auto: validate` 환경에서는 DB 스키마와 엔티티 정의가 정확히 일치해야 함
4. Local(`create-drop`)에서 성공해도 Dev/Prod(`validate`)에서 실패할 수 있음

## 부록: DB 계정 권한 분리 전략

### 문제 상황

```
Application 계정 (A): DML만 가능 (SELECT, INSERT, UPDATE, DELETE)
Flyway 계정 (B): DDL + DML 모두 가능 (CREATE, ALTER, DROP + DML)
```

- `ddl-auto: validate`는 DDL을 실행하지 않고 스키마만 검증
- Hibernate가 시퀀스 존재 여부를 검증할 때, 시퀀스가 DB에 없으면 실패
- Application 계정에 DDL 권한이 없어도 검증 자체는 가능하지만, 시퀀스가 존재해야 함

### 현업 권한 분리 전략

#### 전략 1: 단일 계정 (소규모/스타트업)

```
Application = Flyway = 동일 계정 (DDL + DML 모두 가능)
```

단순하지만 보안상 권장되지 않음.

#### 전략 2: 분리 계정 (권장)

```
Flyway 계정: DDL 권한 (스키마 마이그레이션 전용)
Application 계정: DML 권한만 (런타임 전용)
```

**핵심**: Flyway가 모든 스키마 객체(테이블, 시퀀스, 인덱스)를 생성해야 함.

```sql
-- Flyway 마이그레이션에서
CREATE SEQUENCE chat_message_file_id_seq;
CREATE TABLE chat_message_file (...);

-- Application 계정에 권한 부여
GRANT USAGE ON SEQUENCE chat_message_file_id_seq TO app_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON chat_message_file TO app_user;
```

#### 전략 3: 역할(Role) 기반

```sql
-- 역할 생성
CREATE ROLE app_readonly;
CREATE ROLE app_readwrite;
CREATE ROLE app_admin;

-- 권한 부여
GRANT SELECT ON ALL TABLES TO app_readonly;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES TO app_readwrite;
GRANT ALL PRIVILEGES TO app_admin;

-- 계정에 역할 부여
GRANT app_readwrite TO spring_app;
GRANT app_admin TO flyway_user;
```

### 권장 설정

```yaml
# application.yml
spring:
  datasource:
    username: app_user  # DML만 가능

  flyway:
    user: flyway_admin  # DDL 가능 (별도 credentials)
    password: ${FLYWAY_PASSWORD}
```

```sql
-- 초기 설정 (DBA가 실행)
CREATE USER app_user WITH PASSWORD '...';
CREATE USER flyway_admin WITH PASSWORD '...';

-- Flyway에게 스키마 생성 권한
GRANT CREATE ON DATABASE mydb TO flyway_admin;

-- Application에게 사용 권한 (Flyway 마이그레이션 후)
GRANT USAGE ON SCHEMA public TO app_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO app_user;
GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO app_user;

-- 향후 생성될 객체에도 자동 적용
ALTER DEFAULT PRIVILEGES FOR ROLE flyway_admin
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app_user;
ALTER DEFAULT PRIVILEGES FOR ROLE flyway_admin
  GRANT USAGE ON SEQUENCES TO app_user;
```

### 핵심 포인트

1. **Flyway가 모든 DDL 담당**: 테이블, 시퀀스, 인덱스 등 모든 스키마 객체 생성
2. **Application은 DML만**: 런타임에 DDL 실행 불필요 (`ddl-auto: validate` 또는 `none`)
3. **DEFAULT PRIVILEGES 활용**: 새로 생성되는 객체에 자동으로 권한 부여
4. **시퀀스 USAGE 권한 필수**: `nextval()` 호출을 위해 Application 계정에 시퀀스 사용 권한 필요
