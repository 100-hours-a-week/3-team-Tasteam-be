# Instant to PostgreSQL TIMESTAMPTZ 타입 캐스팅 오류

## 증상

사용자 활동 이벤트 저장 시 다음 에러 발생:

```
org.postgresql.util.PSQLException: Cannot cast an instance of java.time.Instant to type Types.TIMESTAMP_WITH_TIMEZONE
```

또는:

```
org.postgresql.util.PSQLException: Bad value for type timestamp/date/time: 2026-02-21T11:29:24.976Z

Caused by: java.lang.NumberFormatException: Trailing junk on timestamp: 'T11:29:24.976Z'
```

프론트엔드 로그:
```
POST http://localhost:8081/api/v1/analytics/events 500 (Internal Server Error)
[activity] 재시도 예약 {retryAttempt: 1, delayMs: 3068, queuedCount: 5}
```

## 원인 분석

### 문제 코드

**UserActivityEventJdbcRepository.java (초기 버전)**:
```java
MapSqlParameterSource params = new MapSqlParameterSource()
    .addValue("occurredAt", event.occurredAt(), Types.TIMESTAMP_WITH_TIMEZONE);
```

**ActivityEvent.java**:
```java
public record ActivityEvent(
    String eventId,
    String eventName,
    String eventVersion,
    Instant occurredAt,  // java.time.Instant
    Long memberId,
    String anonymousId,
    Map<String, Object> properties
) {}
```

**DB 스키마** (V202602181100__create_user_activity_event_tables.sql):
```sql
CREATE TABLE user_activity_event (
    occurred_at TIMESTAMPTZ NOT NULL,
    ...
);
```

### 근본 원인

1. **PostgreSQL JDBC 드라이버의 타입 매핑 제한**
   - `Instant`를 `Types.TIMESTAMP_WITH_TIMEZONE`으로 직접 변환 불가
   - JDBC 드라이버는 `Instant` → `TIMESTAMP` 변환만 지원

2. **ISO 8601 문자열 파싱 실패**
   - `Instant.toString()`이 `2026-02-21T11:29:24.976Z` 형식으로 직렬화
   - PostgreSQL이 ISO 8601 형식의 'T'와 'Z'를 타임스탬프로 파싱하지 못함

3. **JPA Entity 미존재**
   - JDBC 기반 Repository 사용
   - 로컬 환경에서 Flyway 비활성화 (`spring.flyway.enabled: false`)
   - `ddl-auto: create-drop`이지만 Entity가 없어 테이블 자동 생성 안 됨

## 해결 방법

### 1단계: JDBC 타입 변환 수정

**Before**:
```java
.addValue("occurredAt", event.occurredAt(), Types.TIMESTAMP_WITH_TIMEZONE)
```

**After**:
```java
import java.sql.Timestamp;

.addValue("occurredAt", Timestamp.from(event.occurredAt()))
```

**이유**:
- `Timestamp.from(Instant)`: Instant를 JDBC Timestamp 객체로 변환
- PostgreSQL JDBC 드라이버가 정확하게 인식
- `TIMESTAMPTZ` 컬럼에 자동으로 UTC 기준 저장

### 2단계: 로컬 개발용 JPA Entity 추가

**UserActivityEventEntity.java**:
```java
@Entity
@Table(name = "user_activity_event")
@Getter
@NoArgsConstructor
public class UserActivityEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 64, unique = true)
    private String eventId;

    @Column(name = "event_name", nullable = false, length = 100)
    private String eventName;

    @Column(name = "event_version", nullable = false, length = 20)
    private String eventVersion;

    @Column(name = "occurred_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private Instant occurredAt;

    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "anonymous_id", length = 100)
    private String anonymousId;

    @Column(name = "properties", nullable = false, columnDefinition = "JSONB DEFAULT '{}'::jsonb")
    private String properties;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT NOW()")
    private Instant createdAt;

    // ... 기타 필드
}
```

**목적**:
- 로컬 환경에서 `ddl-auto: create-drop`을 통한 테이블 자동 생성
- Flyway 없이도 개발 가능
- **실제 데이터 접근은 JDBC Repository 사용 (성능 유지)**

### 환경별 전략

| 환경 | 테이블 생성 방식 | Entity 용도 |
|-----|--------------|-----------|
| **로컬** | JPA DDL (`ddl-auto: create-drop`) | 테이블 자동 생성 |
| **개발/운영** | Flyway Migration | 스키마 정의만 |

## 타임존 처리 검증

### Instant vs TIMESTAMPTZ

```java
// Java
Instant instant = Instant.parse("2026-02-21T11:29:24.976Z");
// UTC 기준: 2026-02-21 11:29:24.976
```

```sql
-- PostgreSQL
INSERT INTO user_activity_event (occurred_at)
VALUES ('2026-02-21 11:29:24.976+00');
-- 저장: 2026-02-21 11:29:24.976+00 (UTC)
```

**검증**:
- Instant는 항상 UTC 기준
- TIMESTAMPTZ는 입력 타임존을 UTC로 변환하여 저장
- 데이터 손실 없음 ✅

## 대안 검토

### 대안 1: OffsetDateTime 사용 (❌ 채택 안 함)

```java
@Column(name = "occurred_at")
private OffsetDateTime occurredAt;

.addValue("occurredAt", OffsetDateTime.ofInstant(event.occurredAt(), ZoneOffset.UTC))
```

**단점**:
- 불필요한 타입 변환
- `Instant`가 이미 UTC 기준이므로 `OffsetDateTime` 불필요

### 대안 2: Types.TIMESTAMP 사용 (✅ 최종 채택)

```java
.addValue("occurredAt", Timestamp.from(event.occurredAt()))
```

**장점**:
- 가장 간단하고 명확
- PostgreSQL JDBC 드라이버가 자동으로 TIMESTAMPTZ 처리
- 추가 의존성 없음

## 검증 방법

### 1. 삽입 테스트

```java
@Test
void insertUserActivityEvent() {
    ActivityEvent event = new ActivityEvent(
        UUID.randomUUID().toString(),
        "ui.page.viewed",
        "v1",
        Instant.now(),
        1L,
        null,
        Map.of("path", "/home")
    );

    boolean inserted = repository.insertIgnoreDuplicate(event);
    assertThat(inserted).isTrue();
}
```

### 2. 데이터베이스 확인

```sql
SELECT
    event_id,
    occurred_at,
    pg_typeof(occurred_at) as type,
    timezone('UTC', occurred_at) as utc_time
FROM user_activity_event
LIMIT 1;
```

**예상 결과**:
```
event_id  | occurred_at                  | type                        | utc_time
----------|------------------------------|-----------------------------|----------------------------
abc-123   | 2026-02-21 11:29:24.976+00   | timestamp with time zone    | 2026-02-21 11:29:24.976
```

### 3. 프론트엔드 연동 확인

```javascript
// 브라우저 콘솔
POST /api/v1/analytics/events
{
  "events": [{
    "eventId": "test-123",
    "eventName": "ui.page.viewed",
    "occurredAt": "2026-02-21T11:29:24.976Z"
  }]
}

// 응답: 200 OK
```

## 교훈

### 1. Java 시간 타입과 JDBC 타입 매핑 이해

| Java 타입 | JDBC 타입 | PostgreSQL 타입 | 비고 |
|----------|----------|----------------|------|
| `Instant` | `Timestamp` | `TIMESTAMPTZ` | ✅ 권장 (UTC 기준) |
| `LocalDateTime` | `Timestamp` | `TIMESTAMP` | 타임존 정보 없음 |
| `OffsetDateTime` | `Timestamp` | `TIMESTAMPTZ` | 타임존 포함 |
| `ZonedDateTime` | `Timestamp` | `TIMESTAMPTZ` | 타임존 + 지역 정보 |

### 2. 글로벌 서비스에서 시간 처리 원칙

- **서버**: 모든 시간을 UTC(`Instant`)로 처리
- **DB**: `TIMESTAMPTZ` 사용 (타임존 변환 지원)
- **API**: ISO 8601 형식 (`2026-02-21T11:29:24.976Z`)
- **클라이언트**: 사용자 타임존으로 변환하여 표시

### 3. JDBC vs JPA 전략

**비정규화 데이터 (이벤트 로그 등)**:
- ✅ **쓰기**: JDBC (성능 우선)
- ✅ **읽기**: JPA (편의성, 선택적)
- ✅ **스키마**: Entity 정의 (로컬 DDL, 문서화)

**정규화 데이터 (도메인 모델)**:
- ✅ JPA 우선 사용

### 4. 로컬 개발 환경 설정

```yaml
# application.local.yml
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop  # Entity 기반 자동 DDL
  flyway:
    enabled: false  # Flyway 비활성화
```

- Entity는 스키마 정의 + 로컬 개발 편의성 제공
- 운영에서는 Flyway Migration으로 엄격하게 관리

## 참고 자료

- [PostgreSQL JDBC Types](https://jdbc.postgresql.org/documentation/head/java8-date-time.html)
- [Java Time API Best Practices](https://docs.oracle.com/javase/tutorial/datetime/)
- Spring Data JDBC Reference Documentation
