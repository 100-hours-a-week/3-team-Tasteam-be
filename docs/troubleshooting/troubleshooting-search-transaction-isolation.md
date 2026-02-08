# SearchService - 검색 히스토리 및 그룹 검색 쿼리 트러블슈팅

## 개요

검색 기능 구현 중 발생한 세 가지 주요 문제:
1. JPQL 파라미터 바인딩 오류로 인한 그룹 검색 실패
2. 검색 히스토리 저장 실패로 인한 전체 검색 트랜잭션 중단
3. 테스트 환경에서 PostGIS geometry 타입 처리 실패

## 문제 1: JPQL 파라미터 바인딩 오류

### 에러

```
org.postgresql.util.PSQLException: ERROR: syntax error at or near ")"
  Detail: lower(g1_0.name) like lower(('%'?'%'))
```

그룹 검색 쿼리 실행 시 SQL 파라미터가 잘못된 형식으로 생성되어 구문 오류 발생.

### 원인

JPQL의 `concat()` 함수를 사용한 LIKE 패턴 생성 시 파라미터 바인딩 처리 오류:

```java
// GroupRepository.java (before)
@Query("""
    SELECT g FROM Group g
    WHERE g.deletedAt IS NULL
      AND g.status = :status
      AND (LOWER(g.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
       OR LOWER(g.address) LIKE LOWER(CONCAT('%', :keyword, '%')))
    ORDER BY g.updatedAt DESC, g.id DESC
    LIMIT :pageSize
    """)
List<Group> searchByKeyword(
    @Param("keyword") String keyword,
    @Param("status") GroupStatus status,
    @Param("pageSize") int pageSize
);
```

**실제 생성된 SQL:**
```sql
lower(g1_0.name) like lower(('%'?'%'))  -- 잘못된 파라미터 바인딩
```

**의도한 SQL:**
```sql
lower(g1_0.name) like lower(? || '%' || ?)  -- 정상적인 파라미터 바인딩
```

### 해결: QueryDSL 전환

JPQL 대신 QueryDSL을 사용하여 타입 안전하고 명확한 쿼리 작성:

```java
// GroupQueryRepository.java (interface)
public interface GroupQueryRepository {
    List<Group> searchByKeyword(String keyword, GroupStatus status, int pageSize);
}

// GroupQueryRepositoryImpl.java (implementation)
@Repository
@RequiredArgsConstructor
public class GroupQueryRepositoryImpl implements GroupQueryRepository {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<Group> searchByKeyword(String keyword, GroupStatus status, int pageSize) {
        String pattern = "%" + keyword.toLowerCase() + "%";

        return queryFactory
            .selectFrom(group)
            .where(
                group.deletedAt.isNull(),
                group.status.eq(status),
                group.name.lower().like(pattern)
                    .or(group.address.lower().like(pattern)))
            .orderBy(
                group.updatedAt.desc(),
                group.id.desc())
            .limit(pageSize)
            .fetch();
    }
}
```

**장점:**
- 파라미터 바인딩 문제 해결: 패턴을 Java에서 직접 생성하여 SQL 파라미터로 전달
- 타입 안전성: 컴파일 타임에 쿼리 검증
- 명확한 쿼리 구조: 메서드 체이닝으로 가독성 향상
- PostGIS 지원: Entity의 `Point` 타입을 Hibernate Spatial이 자동 처리

## 문제 2: 검색 히스토리 트랜잭션 격리

### 에러

```
org.springframework.orm.jpa.JpaSystemException: JDBC exception executing SQL [...]
[ERROR: current transaction is aborted, commands ignored until end of transaction block]
```

검색 API 호출 시 검색 히스토리 저장 실패로 인해 후속 검색 쿼리가 실행되지 않는 문제 발생.

### 원인

#### 1. 단일 트랜잭션 내에서 실패 전파

검색 히스토리 저장과 검색 결과 조회가 같은 트랜잭션에서 실행:

```java
// before
@Transactional
public SearchResponse search(Long memberId, SearchRequest request) {
    // ...
    recordSearchHistory(memberId, keyword);  // ← 실패 시 트랜잭션 abort

    List<SearchGroupSummary> groups = searchGroups(keyword, pageSize);  // ← 실행 불가
    CursorPageResponse<SearchRestaurantItem> restaurants = searchRestaurants(...);  // ← 실행 불가
    return new SearchResponse(groups, restaurants);
}

private void recordSearchHistory(Long memberId, String keyword) {
    // UPSERT 실패 시 전체 트랜잭션 롤백
    memberSearchHistoryRepository.upsertSearchHistory(memberId, keyword);
}
```

**실행 시나리오:**
1. `search()` 메서드 시작 → 쓰기 트랜잭션 시작
2. `recordSearchHistory()` 실행 → UPSERT 실패 (예: unique constraint violation)
3. 트랜잭션 상태가 `aborted`로 변경
4. `searchGroups()`, `searchRestaurants()` 실행 시도
5. PostgreSQL 에러: "current transaction is aborted, commands ignored until end of transaction block"

#### 2. 검색 히스토리는 부가 기능

검색 히스토리 저장은 검색 결과 조회와 독립적인 기능:
- 히스토리 저장 실패 ≠ 검색 실패
- 사용자는 검색 결과만 정상적으로 받으면 됨
- 히스토리 실패가 검색 결과에 영향을 주면 안 됨

#### 3. 읽기/쓰기 혼합 트랜잭션

`@Transactional`은 기본적으로 쓰기 트랜잭션:
- 검색 결과 조회는 읽기 전용이지만 쓰기 트랜잭션으로 실행
- 불필요한 잠금(lock) 및 성능 저하

### 해결: SearchHistoryRecorder 컴포넌트 분리

#### 1. 트랜잭션 격리: `REQUIRES_NEW` 전파 레벨

검색 히스토리 저장을 별도의 독립적인 트랜잭션으로 분리:

```java
// SearchHistoryRecorder.java (새로 생성)
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchHistoryRecorder {
    private final MemberSearchHistoryRepository memberSearchHistoryRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSearchHistory(Long memberId, String keyword) {
        if (memberId == null) {
            return;
        }
        try {
            memberSearchHistoryRepository.upsertSearchHistory(memberId, keyword);
        } catch (Exception ex) {
            log.warn("검색 히스토리 업데이트에 실패했습니다: {}", ex.getMessage());
        }
    }
}
```

**Why separate component?**
- Self-invocation 문제 해결: 같은 클래스 내 메서드 호출 시 Spring AOP 프록시가 동작하지 않음
- 별도 컴포넌트로 분리하여 외부 호출 보장

**동작 방식:**
- `REQUIRES_NEW`: 기존 트랜잭션을 일시 중단하고 새로운 트랜잭션 시작
- 새 트랜잭션 커밋/롤백은 부모 트랜잭션과 독립적
- 히스토리 저장 실패 시에도 부모 트랜잭션은 정상 진행

#### 2. 읽기 전용 트랜잭션 적용

검색 메서드를 읽기 전용으로 변경하고 외부 컴포넌트 호출:

```java
// SearchService.java (after)
@RequiredArgsConstructor
public class SearchService {
    private final SearchHistoryRecorder searchHistoryRecorder;  // 외부 컴포넌트 주입
    private final GroupQueryRepository groupQueryRepository;     // QueryDSL 리포지토리로 변경

    @Transactional(readOnly = true)
    public SearchResponse search(Long memberId, SearchRequest request) {
        String keyword = request.keyword().trim();
        int pageSize = request.size() == null ? DEFAULT_PAGE_SIZE : request.size();
        SearchCursor cursor = cursorCodec.decodeOrNull(request.cursor(), SearchCursor.class);
        if (request.cursor() != null && !request.cursor().isBlank() && cursor == null) {
            return SearchResponse.emptyResponse();
        }

        searchHistoryRecorder.recordSearchHistory(memberId, keyword);  // 별도 트랜잭션

        List<SearchGroupSummary> groups = searchGroups(keyword, pageSize);
        CursorPageResponse<SearchRestaurantItem> restaurants = searchRestaurants(keyword, cursor, pageSize);
        return new SearchResponse(groups, restaurants);
    }
}
```

**장점:**
- DB 최적화 힌트 제공 (읽기 전용 쿼리)
- 불필요한 쓰기 잠금 방지
- 의도를 명확하게 표현

#### 3. 트랜잭션 흐름 개선

**변경 전:**
```
[검색 트랜잭션 시작]
  ├─ 검색 히스토리 저장 (쓰기)  ← 실패 시 전체 트랜잭션 abort
  ├─ 그룹 검색 (읽기)            ← 실행 불가
  └─ 음식점 검색 (읽기)          ← 실행 불가
[검색 트랜잭션 커밋/롤백]
```

**변경 후:**
```
[검색 트랜잭션 시작 - readOnly]
  ├─ [히스토리 트랜잭션 시작 - REQUIRES_NEW]
  │    └─ 검색 히스토리 저장 (쓰기)  ← 실패해도 독립적
  │  [히스토리 트랜잭션 커밋/롤백]
  ├─ 그룹 검색 (읽기)                ← 정상 실행
  └─ 음식점 검색 (읽기)              ← 정상 실행
[검색 트랜잭션 커밋]
```

## 문제 3: 테스트 환경 PostGIS 지원

### 에러

```
org.postgresql.util.PSQLException: Unknown type geometry.
  Detail: No enum constant org.postgresql.geometric.PGgeometry
```

`GroupQueryRepositoryTest` 실행 시 PostGIS `Point` 타입을 가진 `Group` 엔티티 저장 실패.

### 원인

Testcontainers가 기본 PostgreSQL 이미지를 사용하여 PostGIS extension이 설치되지 않음:

```java
// TestcontainersConfiguration.java (before)
@Bean
@ServiceConnection
PostgreSQLContainer<?> postgresContainer() {
    return new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"));
}
```

**문제:**
- `Group` 엔티티의 `location` 필드는 JTS `Point` 타입
- Hibernate Spatial이 PostGIS의 `geometry` 타입으로 매핑
- 기본 PostgreSQL은 `geometry` 타입을 인식하지 못함

### 해결: PostGIS Docker 이미지 사용

PostGIS extension이 포함된 공식 이미지로 변경:

```java
// TestcontainersConfiguration.java (after)
@Bean
@ServiceConnection
PostgreSQLContainer<?> postgresContainer() {
    return new PostgreSQLContainer<>(DockerImageName.parse("postgis/postgis:15-3.3")
        .asCompatibleSubstituteFor("postgres"));
}
```

**변경 사항:**
- `postgres:latest` → `postgis/postgis:15-3.3`
- `.asCompatibleSubstituteFor("postgres")`: Testcontainers가 PostgreSQL 컨테이너로 인식하도록 설정
- PostGIS 3.3 버전 사용 (프로덕션 환경과 동일)

## 리포지토리 테스트 작성

### 테스트 전략

프로젝트의 [Repository Test Convention](test/REPOSITORY_TEST_CONVENTION.md)을 기반으로 작성:

1. **MemberSearchHistoryRepositoryTest**: 기본 JPA 메서드 테스트
2. **MemberSearchHistoryQueryRepositoryTest**: QueryDSL 커스텀 쿼리 테스트
3. **GroupQueryRepositoryTest**: QueryDSL 그룹 검색 쿼리 테스트

### 1. Fixture 생성

테스트 데이터 생성을 위한 Fixture 클래스:

```java
// MemberSearchHistoryFixture.java
public final class MemberSearchHistoryFixture {
    public static final Long DEFAULT_MEMBER_ID = 1L;
    public static final String DEFAULT_KEYWORD = "테스트검색어";

    public static MemberSearchHistory create() {
        return MemberSearchHistory.create(DEFAULT_MEMBER_ID, DEFAULT_KEYWORD);
    }

    public static MemberSearchHistory create(Long memberId, String keyword) {
        return MemberSearchHistory.create(memberId, keyword);
    }
}

// GroupFixture.java
public final class GroupFixture {
    public static final String DEFAULT_NAME = "테스트그룹";
    public static final String DEFAULT_ADDRESS = "서울특별시 강남구";
    public static final double DEFAULT_LATITUDE = 37.5665;
    public static final double DEFAULT_LONGITUDE = 126.9780;

    public static Group create(String name, String address) {
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        Point location = geometryFactory.createPoint(
            new Coordinate(DEFAULT_LONGITUDE, DEFAULT_LATITUDE)
        );

        return Group.builder()
            .name(name)
            .type(GroupType.UNOFFICIAL)
            .address(address)
            .location(location)
            .joinType(GroupJoinType.PASSWORD)
            .status(GroupStatus.ACTIVE)
            .build();
    }
}
```

### 2. Repository 테스트 구조

```java
@RepositoryJpaTest
@Import(GroupQueryRepositoryImpl.class)  // QueryDSL 구현체 로드
@DisplayName("GroupQueryRepository 테스트")
class GroupQueryRepositoryTest {

    @Autowired
    private GroupQueryRepository groupQueryRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("searchByKeyword - 그룹명으로 검색")
    void searchByKeyword_byName() {
        groupRepository.save(GroupFixture.create("맛집탐방모임", "서울시 강남구"));
        groupRepository.save(GroupFixture.create("독서모임", "서울시 종로구"));
        entityManager.flush();
        entityManager.clear();

        List<Group> results = groupQueryRepository.searchByKeyword(
            "맛집", GroupStatus.ACTIVE, 10
        );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("맛집탐방모임");
    }
}
```

### 3. 테스트로 발견한 문제와 해결

#### @RepositoryJpaTest에 Testcontainers 설정 누락

**문제:**
```
DataSourceBeanCreationException: Failed to determine a suitable driver class
```

**원인:**
- `@RepositoryJpaTest`가 `TestcontainersConfiguration`을 import하지 않음
- DataSource 설정이 없어 테스트 실행 불가

**해결:**
```java
// RepositoryJpaTest.java (before)
@Import({QueryDslConfig.class, JpaAuditingTestConfig.class})

// RepositoryJpaTest.java (after)
@Import({QueryDslConfig.class, JpaAuditingTestConfig.class, TestcontainersConfiguration.class})
```

#### QueryDSL 구현체 Bean 등록 누락

**문제:**
```
UnsatisfiedDependencyException: No qualifying bean of type 'GroupQueryRepository'
```

**원인:**
- `@DataJpaTest`는 Spring Data JPA 리포지토리만 스캔
- `@Repository` 어노테이션이 있는 QueryDSL 구현체는 자동 스캔되지 않음

**해결:**
```java
// 각 테스트 클래스에 명시적 import
@RepositoryJpaTest
@Import(GroupQueryRepositoryImpl.class)  // QueryDSL 구현체 추가
class GroupQueryRepositoryTest {
    // ...
}
```

### 4. 작성된 테스트 (총 16개)

#### MemberSearchHistoryRepositoryTest (3개)
- 검색 히스토리 저장 및 조회
- `findByMemberIdAndKeywordAndDeletedAtIsNull` - 삭제된 검색어 제외
- `findByIdAndMemberIdAndDeletedAtIsNull` - 본인의 검색어만 조회

#### MemberSearchHistoryQueryRepositoryTest (4개)
- `findRecentSearches` - updated_at 내림차순 정렬
- 삭제된 검색어 제외
- 다른 회원의 검색어 제외
- pageSize 제한

#### GroupQueryRepositoryTest (9개)
- 그룹명으로 검색
- 주소로 검색
- 대소문자 구분 없이 검색
- ACTIVE 상태만 조회
- 삭제된 그룹 제외
- updated_at 내림차순 정렬
- pageSize 제한
- OR 조건 (그룹명 또는 주소)
- location 필드 정상 조회

### 5. UPSERT 쿼리 Partial Index 문제

#### 추가 발견된 문제

트랜잭션 격리 후에도 UPSERT 쿼리 자체가 실패하는 문제 발생:

```
ERROR: there is no unique or exclusion constraint matching the ON CONFLICT specification
```

**원래 쿼리:**
```sql
INSERT INTO member_serach_history (member_id, keyword, count, created_at, updated_at, deleted_at)
VALUES (?, ?, 1, NOW(), NOW(), NULL)
ON CONFLICT (member_id, keyword) WHERE deleted_at IS NULL  -- ❌ 에러 발생
DO UPDATE SET count = member_serach_history.count + 1, updated_at = NOW()
```

**원인:**
- Flyway 마이그레이션에서 생성한 것은 **partial unique index**
- PostgreSQL의 `ON CONFLICT`는 partial index를 inference target으로 사용 가능
- 하지만 `ON CONFLICT (column) WHERE condition` 구문은 지원하지 않음
- WHERE 절은 index 정의에만 포함되어야 함

**해결:**
```sql
-- Partial unique index 정의 (Flyway migration)
CREATE UNIQUE INDEX idx_member_search_history_unique
ON member_serach_history(member_id, keyword)
WHERE deleted_at IS NULL;

-- UPSERT 쿼리 (WHERE 절 제거)
INSERT INTO member_serach_history (member_id, keyword, count, created_at, updated_at, deleted_at)
VALUES (?, ?, 1, NOW(), NOW(), NULL)
ON CONFLICT (member_id, keyword)  -- ✅ WHERE 절 제거
DO UPDATE SET count = member_serach_history.count + 1, updated_at = NOW()
```

**동작 방식:**
1. Partial index가 `deleted_at IS NULL` 조건의 행에만 적용됨
2. `ON CONFLICT (member_id, keyword)`는 이 partial index를 자동으로 inference
3. 삭제된 행(`deleted_at IS NOT NULL`)은 index에 포함되지 않으므로 충돌 검사에서 제외
4. 정상적으로 UPSERT 동작

### 6. UPSERT 의존성 제거 - 애플리케이션 레벨 처리

#### 추가 발견된 문제

Flyway 마이그레이션이 실행되지 않은 환경에서는 partial unique index가 존재하지 않아 UPSERT 쿼리가 계속 실패합니다. 이 경우에도 검색 기능이 정상 동작해야 합니다.

**근본 문제:**
- UPSERT는 **DB에 unique index/constraint가 반드시 존재**해야 작동
- 마이그레이션 미실행, DB 동기화 문제 등으로 인덱스 부재 가능
- 인덱스가 없으면 SQL 문법 오류 발생 → try-catch로 잡혀도 트랜잭션 매니저 오염
- `REQUIRES_NEW`로 격리했지만 여전히 부모 트랜잭션에 영향

**최종 해결: 애플리케이션 레벨 UPSERT**

DB native UPSERT 대신 JPA를 사용한 애플리케이션 레벨 처리로 변경:

```java
// SearchHistoryRecorder.java (final)
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void recordSearchHistory(Long memberId, String keyword) {
    if (memberId == null) {
        return;
    }
    try {
        var existing = memberSearchHistoryRepository
            .findByMemberIdAndKeywordAndDeletedAtIsNull(memberId, keyword);

        if (existing.isPresent()) {
            existing.get().incrementCount();  // UPDATE
        } else {
            memberSearchHistoryRepository.save(
                MemberSearchHistory.create(memberId, keyword));  // INSERT
        }
    } catch (Exception ex) {
        log.warn("검색 히스토리 업데이트에 실패했습니다: {}", ex.getMessage());
    }
}
```

```java
// SearchService.java - 추가 방어 로직
@Transactional(readOnly = true)
public SearchResponse search(Long memberId, SearchRequest request) {
    // ...

    try {
        searchHistoryRecorder.recordSearchHistory(memberId, keyword);
    } catch (Exception ex) {
        log.warn("검색 히스토리 기록 중 예외 발생 (검색 결과에는 영향 없음): {}", ex.getMessage());
    }

    // 검색 로직 계속 실행
    List<SearchGroupSummary> groups = searchGroups(keyword, pageSize);
    // ...
}
```

**장점:**
1. **DB 스키마 독립적**: 인덱스 유무와 관계없이 동작
2. **완벽한 격리**: SQL 문법 오류 가능성 제거
3. **명시적 로직**: SELECT → UPDATE or INSERT 흐름이 명확
4. **안정성**: 어떤 예외가 발생해도 검색 기능은 정상 동작

**단점:**
1. **동시성**: 동일 검색어 동시 입력 시 unique constraint violation 가능
   - 하지만 try-catch로 처리되므로 검색 기능에는 영향 없음
2. **성능**: SELECT + UPDATE/INSERT 두 번의 쿼리
   - 검색 히스토리는 부가 기능이므로 허용 가능한 트레이드오프

## 전체 개선 효과

1. **견고성 향상**: 검색 히스토리 저장 실패가 검색 결과에 영향을 주지 않음
2. **사용자 경험 개선**: 부가 기능 실패로 인한 주요 기능 중단 방지
3. **성능 최적화**: 읽기 전용 트랜잭션으로 DB 최적화 가능
4. **트랜잭션 분리**: 읽기/쓰기 작업의 명확한 격리
5. **장애 격리**: 히스토리 저장 문제가 전체 검색 시스템에 전파되지 않음
6. **쿼리 안정성**: QueryDSL로 파라미터 바인딩 문제 해결
7. **테스트 커버리지**: 16개 리포지토리 테스트로 검색 로직 검증

## 주의사항

### `REQUIRES_NEW` 사용 시 고려사항

1. **트랜잭션 오버헤드**
   - 새 트랜잭션 시작/커밋에 따른 성능 비용
   - 본 케이스는 부가 기능이므로 트레이드오프 수용 가능

2. **데이터 일관성**
   - 부모 트랜잭션이 롤백되어도 자식 트랜잭션은 커밋됨
   - 검색 히스토리는 검색 결과와 강한 일관성이 필요 없으므로 문제 없음

3. **Self-Invocation 문제**
   - 같은 클래스 내 메서드 호출 시 트랜잭션 프록시가 동작하지 않음
   - 별도 컴포넌트(`SearchHistoryRecorder`)로 분리하여 해결

### Partial Unique Index 사용 시 주의사항

1. **ON CONFLICT 구문 제약**
   - `ON CONFLICT (columns) WHERE condition` 형식은 PostgreSQL에서 지원하지 않음
   - WHERE 절은 index 정의에만 포함
   - `ON CONFLICT (columns)`만 사용하여 partial index inference

2. **Index Inference**
   - PostgreSQL이 자동으로 적절한 index를 선택
   - Partial index는 조건에 맞는 행에만 적용되므로 안전

## 참고

- Spring Transaction Propagation: https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html
- PostgreSQL Transaction States: https://www.postgresql.org/docs/current/tutorial-transactions.html
