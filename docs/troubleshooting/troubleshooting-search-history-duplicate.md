# SearchService - 검색 히스토리 중복 레코드 문제

## 에러

```
{"@timestamp":"2026-01-31T10:40:59.800721959Z","level":"WARN","message":"검색 히스토리 업데이트에 실패했습니다: Query did not return a unique result: 4 results were returned","logger_name":"com.tasteam.domain.search.service.SearchService"}
```

검색 API 호출 시 `NonUniqueResultException` 발생.

## 원인

### 1. Race Condition으로 인한 중복 데이터 생성

동일한 사용자가 같은 키워드를 거의 동시에 검색할 때 발생:

```java
// before - 동시성 문제 발생 가능
MemberSearchHistory history = memberSearchHistoryRepository
    .findByMemberIdAndKeywordAndDeletedAtIsNull(memberId, keyword)
    .orElseGet(() -> MemberSearchHistory.create(memberId, keyword));
```

**실행 시나리오:**
1. Thread A: 조회 → 결과 없음 → 새 레코드 생성 준비
2. Thread B: 조회 → 결과 없음 → 새 레코드 생성 준비
3. Thread A: `save()` 실행 → DB에 저장
4. Thread B: `save()` 실행 → DB에 중복 저장

### 2. 유니크 제약조건 부재

`member_serach_history` 테이블에 `(member_id, keyword)` 조합에 대한 유니크 인덱스가 없어서 중복 삽입이 허용됨.

### 3. `Optional<>` 메서드의 한계

`findByMemberIdAndKeywordAndDeletedAtIsNull`은 `Optional<MemberSearchHistory>`를 반환하므로, 중복 데이터가 존재하면 `NonUniqueResultException` 발생.

## 해결

### 1. DB 레벨: Partial Unique Index 생성

Soft delete를 고려한 부분 유니크 인덱스 추가:

```sql
CREATE UNIQUE INDEX idx_member_search_history_unique
ON member_serach_history(member_id, keyword)
WHERE deleted_at IS NULL;
```

- `deleted_at IS NULL`인 경우에만 유니크 제약 적용
- Soft delete된 레코드는 중복 허용

### 2. 애플리케이션 레벨: UPSERT 패턴 적용

PostgreSQL의 `ON CONFLICT` 구문을 활용한 atomic upsert:

```java
// MemberSearchHistoryRepository.java
@Modifying
@Query(value = """
    INSERT INTO member_serach_history (member_id, keyword, count, created_at, updated_at, deleted_at)
    VALUES (:memberId, :keyword, 1, NOW(), NOW(), NULL)
    ON CONFLICT (member_id, keyword) WHERE deleted_at IS NULL
    DO UPDATE SET count = member_serach_history.count + 1, updated_at = NOW()
    """, nativeQuery = true)
void upsertSearchHistory(@Param("memberId") Long memberId, @Param("keyword") String keyword);
```

**동작 방식:**
- 신규 검색: `INSERT` → count=1로 새 레코드 생성
- 중복 검색: `ON CONFLICT` 감지 → count 증가 + `updated_at` 갱신

### 3. 서비스 로직 간소화

```java
// SearchService.java - after
private void recordSearchHistory(Long memberId, String keyword) {
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

**변경 전:** 조회 → 분기 처리 → 저장/업데이트 → 중복 삭제 (복잡, Race Condition 가능)
**변경 후:** 단일 UPSERT 쿼리로 처리 (간결, Atomic)

## 개선 효과

1. **동시성 안전**: DB 레벨에서 유니크 보장 + atomic UPSERT로 Race Condition 해결
2. **자동 갱신**: 중복 검색 시 `updated_at` 자동 업데이트 (최근 검색 순서 유지)
3. **성능 향상**: 조회 + 저장 2개 쿼리 → 1개 UPSERT 쿼리로 단축
4. **코드 간결화**: 복잡한 분기 로직 제거, 유지보수성 향상
5. **에러 방지**: `NonUniqueResultException` 원천 차단

## 참고

- PostgreSQL partial index: https://www.postgresql.org/docs/current/indexes-partial.html
- PostgreSQL UPSERT (INSERT ... ON CONFLICT): https://www.postgresql.org/docs/current/sql-insert.html
