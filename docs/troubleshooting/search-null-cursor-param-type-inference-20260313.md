# 검색 500 — PostgreSQL NULL 커서 파라미터 타입 추론 실패 (2026-03-13)

## 증상

- API: `GET /api/v1/search?keyword=<kw>&latitude=<lat>&longitude=<lon>&radiusKm=<r>`
- 첫 페이지 요청(cursor 없음)에서 500 INTERNAL_SERVER_ERROR
- 로그에서 잡힌 PostgreSQL 에러:
  ```
  ERROR 42P18: could not determine data type of parameter $24
  ```

---

## 재현 조건

- `cursor` 쿼리 파라미터가 없는 첫 페이지 요청
- 기본 전략 `FTS_MV_RANKED` (application.yml line 105) 활성 시 **반드시** 재현
- 전략이 `HYBRID_SPLIT_CANDIDATES`, `GEO_FIRST_HYBRID`, `READ_MODEL_TWO_STEP`, `MV_SINGLE_PASS`일 때도 동일 조건이면 잠재적으로 동일 에러 발생

---

## 원인 분석

### Java 코드 흐름

`cursor == null` 시 `executeFtsNativeStrategy()`(또는 `executeNativeStrategy()`)에서:

```java
Double cursorScore = cursor == null ? null : cursorScoreFts(cursor, radiusMeters);

query.setParameter("cursor_score",      cursorScore);          // null
query.setParameter("cursor_updated_at", cursor == null ? null : cursor.updatedAt()); // null
query.setParameter("cursor_id",         cursor == null ? null : cursor.id());        // null
```

세 파라미터가 모두 `null`로 바인딩됨.

### SQL 문제 구간

```sql
WHERE (
    :cursor_score IS NULL                          -- PostgreSQL이 타입 추론 불가
    OR total_score < :cursor_score
    OR (total_score = :cursor_score AND updated_at < :cursor_updated_at)
    OR (total_score = :cursor_score AND updated_at = :cursor_updated_at
        AND restaurant_id < :cursor_id)
)
```

### PostgreSQL 타입 추론 실패 메커니즘

PostgreSQL의 named parameter(`:name` → JDBC `$N`)는 플래너가 실행 계획 수립 시점에 파라미터 타입을 추론한다. 추론 방법:
1. 비교 연산자 우변의 컬럼 타입 참고 (예: `total_score < :cursor_score` → `double precision`)
2. `:cursor_score IS NULL` 단독으로는 비교 대상 컬럼이 없어 타입 추론 불가

문제는 **`:cursor_score IS NULL`이 WHERE 조건의 첫 번째 절**일 때 발생한다. PostgreSQL은 OR 체인을 순서대로 파싱하며 첫 번째 참조 시점에 타입을 확정하려 하는데, `IS NULL`만으로는 타입 정보가 없어 `42P18` 에러를 반환한다.

`cursor_updated_at`(timestamp), `cursor_id`(bigint)도 동일한 이유로 잠재적으로 같은 에러가 발생할 수 있다.

---

## 영향 범위

`SearchQueryRepositoryImpl.java`의 native SQL 빌더 5개 전부:

| SQL 빌더 메서드 | 구 라인 | 사용 전략 |
|---|---|---|
| `buildFtsMvRankedSql` | 197 | `FTS_MV_RANKED` (기본값) |
| `buildHybridSplitSql` | 455 | `HYBRID_SPLIT_CANDIDATES` |
| `buildGeoFirstSql` | 532 | `GEO_FIRST_HYBRID` |
| `buildReadModelSql` | 650 | `READ_MODEL_TWO_STEP` |
| `buildMvSinglePassSql` | 727 | `MV_SINGLE_PASS` |

QueryDSL 기반 전략(`ONE_STEP`, `TWO_STEP`, `JOIN_AGGREGATE`)은 `BooleanExpression`을 사용하므로 영향 없음.

---

## 수정

각 SQL 빌더의 커서 조건에 명시적 `CAST`를 추가했다.

```sql
-- 수정 전
WHERE (
    :cursor_score IS NULL
    OR total_score < :cursor_score
    OR (total_score = :cursor_score AND updated_at < :cursor_updated_at)
    OR (total_score = :cursor_score AND updated_at = :cursor_updated_at
        AND restaurant_id < :cursor_id)
)

-- 수정 후
WHERE (
    CAST(:cursor_score AS double precision) IS NULL
    OR total_score < CAST(:cursor_score AS double precision)
    OR (total_score = CAST(:cursor_score AS double precision)
        AND updated_at < CAST(:cursor_updated_at AS timestamptz))
    OR (total_score = CAST(:cursor_score AS double precision)
        AND updated_at = CAST(:cursor_updated_at AS timestamptz)
        AND restaurant_id < CAST(:cursor_id AS bigint))
)
```

`CAST`를 사용하면 값이 `null`이더라도 PostgreSQL이 파라미터 타입을 결정할 수 있다. `CAST(null AS double precision)`은 `null::double precision`으로 처리되어 `IS NULL`이 정상적으로 `true`를 반환한다.

**수정 파일**: `app-api/src/main/java/com/tasteam/domain/search/repository/impl/SearchQueryRepositoryImpl.java`

---

## 검증

```bash
# 단위 테스트 (cursor=null 첫 페이지 포함)
./gradlew test --tests SearchQueryRepositoryTest

# 전략별 첫 페이지 (cursor=null) 확인
# strategy: FTS_MV_RANKED, MV_SINGLE_PASS, READ_MODEL_TWO_STEP 각각 테스트
```

---

## 재발 방지 가이드라인

**native query 작성 시 null이 될 수 있는 파라미터는 반드시 `CAST` 명시**

```sql
-- ❌ 타입 추론 실패 가능
:some_param IS NULL
OR column < :some_param

-- ✅ 명시적 CAST
CAST(:some_param AS <type>) IS NULL
OR column < CAST(:some_param AS <type>)
```

| 파라미터 용도 | CAST 타입 |
|---|---|
| score (double) | `CAST(:x AS double precision)` |
| timestamp (Instant) | `CAST(:x AS timestamptz)` |
| id (Long) | `CAST(:x AS bigint)` |
| 문자열 | `CAST(:x AS text)` |

이 규칙은 Spring Data JPA의 `@Query` native query, `EntityManager.createNativeQuery()` 모두에 적용된다.
