# 검색 API 최적화 트러블슈팅 - 2026-03-08

검색 API 성능 최적화 과정에서 발생한 두 가지 이슈와 해결 방법을 기록한다.

---

## 이슈 1: Hibernate 6 JPQL이 `ST_DWithin(geography(...))` 을 boolean predicate로 인식하지 못함

### 증상

`ST_DWithin` 을 JPQL WHERE 절의 boolean predicate로 직접 사용했을 때 다음 예외가 발생했다.

```
org.hibernate.query.SemanticException: The type of the expression 'ST_DWithin(...)' is
not boolean but must be for use as a predicate
```

### 원인 분석

Hibernate 6의 JPQL 파서(SQM, Semantic Query Model)는 WHERE 절에 오는 식의 반환 타입을 정적으로 검사한다.
PostgreSQL의 `ST_DWithin` 은 SQL 레벨에서 `BOOLEAN` 을 반환하지만, Hibernate는 이를 boolean predicate로 인식하지 못한다.

- Hibernate 6에서 PostGIS 공간 함수들은 별도의 SPI 없이 사용할 경우 반환 타입이 등록되지 않은 상태로 처리된다.
- Hibernate Spatial 모듈이 일부 함수를 등록하지만 `geography` 타입을 사용하는 `ST_DWithin` 오버로드는 포함되지 않는다.
- JPQL 파서는 반환 타입을 boolean으로 판단하지 못하면 predicate 자리에 사용을 거부한다.

### 해결 방법

`FunctionContributor` SPI를 이용해 커스텀 함수를 `CASE WHEN ... THEN 1 ELSE 0 END` 패턴으로 등록하고, INTEGER 반환값과 `.eq(1)` 비교로 boolean 판단을 우회했다.

**`SpatialFunctionContributor` 등록** (`app-api/src/main/java/com/tasteam/global/config/SpatialFunctionContributor.java`)

```java
public class SpatialFunctionContributor implements FunctionContributor {

    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {
        functionContributions.getFunctionRegistry().registerPattern(
            "st_dwithin_geo",
            "CASE WHEN ST_DWithin(geography(?1), geography(ST_MakePoint(?2, ?3)), ?4) THEN 1 ELSE 0 END",
            functionContributions.getTypeConfiguration()
                .getBasicTypeRegistry()
                .resolve(StandardBasicTypes.INTEGER));
    }
}
```

**SPI 파일 등록** (`app-api/src/main/resources/META-INF/services/org.hibernate.boot.model.FunctionContributor`)

```
com.tasteam.global.config.SpatialFunctionContributor
```

**QueryDSL에서 사용** (`SearchQueryRepositoryImpl.java:266`)

```java
private BooleanExpression distanceFilter(QRestaurant r, Double latitude, Double longitude, Double radiusMeters) {
    if (radiusMeters == null || latitude == null || longitude == null) {
        return null;
    }
    return Expressions.numberTemplate(Integer.class,
        "function('st_dwithin_geo', {0}, {1}, {2}, {3})",
        r.location, longitude, latitude, radiusMeters).eq(1);
}
```

### 핵심 원칙

| 방법 | 결과 |
|------|------|
| `ST_DWithin(...)` 을 WHERE에 직접 사용 | SemanticException (boolean predicate 불인식) |
| `CASE WHEN ST_DWithin(...) THEN 1 ELSE 0 END` + `.eq(1)` | 정상 동작 |

Hibernate 6 JPQL에서 PostGIS 공간 함수를 boolean 조건으로 사용하려면 반드시 `FunctionContributor` SPI로 함수를 등록하거나 INTEGER 래핑을 통해 우회해야 한다.

---

## 이슈 2: `CompletableFuture.supplyAsync` 병렬 실행이 `@Transactional` 테스트의 미커밋 데이터를 조회하지 못함

### 증상

`SearchService`가 group 조회와 restaurant 조회를 `CompletableFuture.supplyAsync`로 병렬 실행하도록 리팩터링한 후, 통합 테스트에서 검색 결과가 빈 값으로 반환됐다.

```
AssertionError: expected: [3] but was: [0]
```

테스트는 `@Transactional` 로 래핑되어 있었고, 데이터를 `save` 한 뒤 검색 API를 호출하는 구조였다.

### 원인 분석

Spring의 `@Transactional` 테스트는 트랜잭션을 시작한 뒤 테스트 메서드를 실행하고, 종료 시 롤백한다.
이때 테스트 스레드의 트랜잭션은 `TransactionSynchronizationManager` 의 `ThreadLocal` 에 바인딩된다.

`CompletableFuture.supplyAsync(task, executor)` 는 기본적으로 별도 스레드에서 태스크를 실행한다.

- 별도 스레드는 테스트 트랜잭션의 `ThreadLocal` 컨텍스트를 공유하지 않는다.
- `SearchDataService` 의 `fetchGroups` / `fetchRestaurants` 는 각각 `@Transactional(readOnly = true)` 로 선언되어 있어 새 트랜잭션을 시작한다.
- 새 트랜잭션은 아직 커밋되지 않은 테스트 트랜잭션의 데이터를 읽지 못한다(PostgreSQL 기본 격리 수준: `READ COMMITTED`).

결과적으로 테스트에서 저장한 데이터가 미커밋 상태이므로 병렬 스레드에서 조회 시 빈 결과가 반환됐다.

### 해결 방법

테스트 환경에서는 `searchQueryExecutor` 빈을 **동기 Executor** (`Runnable::run`) 로 교체한다.
`Runnable::run` 은 태스크를 현재 스레드에서 직접 실행하므로 테스트 트랜잭션의 `ThreadLocal` 컨텍스트를 그대로 이어받는다.

**프로덕션 빈에 `@ConditionalOnMissingBean` 적용** (`AsyncConfig.java:38-42`)

```java
@Bean(name = "searchQueryExecutor")
@ConditionalOnMissingBean(name = "searchQueryExecutor")
public Executor searchQueryExecutor() {
    return new TaskExecutorAdapter(
        java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());
}
```

**테스트 전용 동기 Executor 등록** (`TestStorageConfiguration.java:26-29`)

```java
@Bean(name = "searchQueryExecutor")
Executor searchQueryExecutor() {
    return Runnable::run;  // 현재 스레드에서 동기 실행 → 테스트 트랜잭션 공유
}
```

`@TestConfiguration` + `@Profile("test")` 로 선언된 `TestStorageConfiguration` 이 `searchQueryExecutor` 를 먼저 등록하면, 프로덕션 `AsyncConfig` 의 `@ConditionalOnMissingBean` 조건에 의해 가상 스레드 Executor는 등록되지 않는다.

### 데이터 흐름 비교

```
[수정 전]
테스트 트랜잭션(스레드 A)
  └─ save(restaurant)          ← 미커밋
  └─ supplyAsync(fetchGroups)  ← 스레드 B 실행 (새 TX, 미커밋 데이터 안 보임)
  └─ supplyAsync(fetchRestaurants) ← 스레드 C 실행 (새 TX, 미커밋 데이터 안 보임)
  결과: 빈 리스트

[수정 후]
테스트 트랜잭션(스레드 A)
  └─ save(restaurant)                      ← 미커밋
  └─ supplyAsync(fetchGroups, Runnable::run) ← 스레드 A에서 직접 실행 (같은 TX 공유)
  └─ supplyAsync(fetchRestaurants, Runnable::run) ← 스레드 A에서 직접 실행 (같은 TX 공유)
  결과: 정상 조회
```

### 주의 사항

- `Runnable::run` Executor는 비동기 병렬 실행의 이점을 완전히 제거한다. 테스트 전용으로만 사용해야 한다.
- 실제로 병렬 성능이 필요한 통합 테스트라면 `@Transactional` 을 제거하고 `@BeforeEach`/`@AfterEach` 에서 명시적으로 커밋/정리해야 한다.
- 이 패턴(`@ConditionalOnMissingBean` + 테스트 오버라이드)은 Executor 외에 `Clock`, `IdGenerator` 등 인프라 빈을 테스트에서 교체할 때도 동일하게 적용할 수 있다.
