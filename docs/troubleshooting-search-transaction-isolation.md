# SearchService - 검색 히스토리 트랜잭션 격리 문제

## 에러

```
org.springframework.orm.jpa.JpaSystemException: JDBC exception executing SQL [...]
[ERROR: current transaction is aborted, commands ignored until end of transaction block]
```

검색 API 호출 시 검색 히스토리 저장 실패로 인해 후속 검색 쿼리가 실행되지 않는 문제 발생.

## 원인

### 1. 단일 트랜잭션 내에서 실패 전파

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

### 2. 검색 히스토리는 부가 기능

검색 히스토리 저장은 검색 결과 조회와 독립적인 기능:
- 히스토리 저장 실패 ≠ 검색 실패
- 사용자는 검색 결과만 정상적으로 받으면 됨
- 히스토리 실패가 검색 결과에 영향을 주면 안 됨

### 3. 읽기/쓰기 혼합 트랜잭션

`@Transactional`은 기본적으로 쓰기 트랜잭션:
- 검색 결과 조회는 읽기 전용이지만 쓰기 트랜잭션으로 실행
- 불필요한 잠금(lock) 및 성능 저하

## 해결

### 1. 트랜잭션 격리: `REQUIRES_NEW` 전파 레벨

검색 히스토리 저장을 별도의 독립적인 트랜잭션으로 분리:

```java
// SearchService.java
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
```

**동작 방식:**
- `REQUIRES_NEW`: 기존 트랜잭션을 일시 중단하고 새로운 트랜잭션 시작
- 새 트랜잭션 커밋/롤백은 부모 트랜잭션과 독립적
- 히스토리 저장 실패 시에도 부모 트랜잭션은 정상 진행

### 2. 읽기 전용 트랜잭션 적용

검색 메서드를 읽기 전용으로 변경:

```java
// SearchService.java
@Transactional(readOnly = true)
public SearchResponse search(Long memberId, SearchRequest request) {
    String keyword = request.keyword().trim();
    int pageSize = request.size() == null ? DEFAULT_PAGE_SIZE : request.size();
    SearchCursor cursor = cursorCodec.decodeOrNull(request.cursor(), SearchCursor.class);
    if (request.cursor() != null && !request.cursor().isBlank() && cursor == null) {
        return SearchResponse.emptyResponse();
    }

    recordSearchHistory(memberId, keyword);  // 별도 트랜잭션에서 실행

    List<SearchGroupSummary> groups = searchGroups(keyword, pageSize);
    CursorPageResponse<SearchRestaurantItem> restaurants = searchRestaurants(keyword, cursor, pageSize);
    return new SearchResponse(groups, restaurants);
}
```

**장점:**
- DB 최적화 힌트 제공 (읽기 전용 쿼리)
- 불필요한 쓰기 잠금 방지
- 의도를 명확하게 표현

### 3. 트랜잭션 흐름 개선

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

## 개선 효과

1. **견고성 향상**: 검색 히스토리 저장 실패가 검색 결과에 영향을 주지 않음
2. **사용자 경험 개선**: 부가 기능 실패로 인한 주요 기능 중단 방지
3. **성능 최적화**: 읽기 전용 트랜잭션으로 DB 최적화 가능
4. **트랜잭션 분리**: 읽기/쓰기 작업의 명확한 격리
5. **장애 격리**: 히스토리 저장 문제가 전체 검색 시스템에 전파되지 않음

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
   - 본 케이스는 정상적으로 동작 (Spring AOP 프록시를 통한 호출)

## 참고

- Spring Transaction Propagation: https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html
- PostgreSQL Transaction States: https://www.postgresql.org/docs/current/tutorial-transactions.html
