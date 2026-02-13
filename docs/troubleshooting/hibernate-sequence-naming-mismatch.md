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
