# Hibernate 파생 삭제(Derived Delete)와 INSERT 순서 역전 문제

## 에러

```
ERROR o.h.e.j.s.SqlExceptionHelper : ERROR: duplicate key value violates unique constraint "ukikxii0i8ctnnet12us8m8v8xf"
Detail: Key (restaurant_id, food_category_id)=(15, 2) already exists.
```

어드민에서 음식점 수정(`PATCH /api/v1/admin/restaurants/{id}`) 호출 시 500 에러 발생.
음식 카테고리를 변경하거나 이미지를 교체할 때 재현됨.

## 원인

### Hibernate 액션 큐의 flush 순서

Hibernate는 트랜잭션 내에서 발생한 변경을 즉시 DB에 반영하지 않고 **액션 큐(Action Queue)**에 쌓아두었다가 flush 시 일괄 실행한다. 이때 실행 순서는 다음과 같이 고정되어 있다.

```
INSERT → UPDATE → DELETE
```

### 파생 삭제(Derived Delete)의 동작 방식

Spring Data JPA의 파생 삭제 메서드(`deleteByXxx`)는 내부적으로 다음 두 단계로 동작한다.

```
1. SELECT ... WHERE restaurant_id = ?   (대상 엔티티 조회)
2. EntityManager.remove(entity)         (각 엔티티에 remove 호출)
```

`remove()`는 즉시 DELETE SQL을 실행하는 것이 아니라 `EntityDeleteAction`을 큐에 추가한다.

### 문제가 발생하는 시나리오

```java
// AdminRestaurantService.updateRestaurant()
restaurantFoodCategoryRepository.deleteByRestaurantId(restaurantId);
// → EntityDeleteAction 2건 큐에 추가 (restaurant_id=15, food_category_id=1, 2)

List<RestaurantFoodCategory> mappings = ...;
restaurantFoodCategoryRepository.saveAll(mappings);
// → EntityInsertAction 2건 큐에 추가 (restaurant_id=15, food_category_id=2, 3)

// flush 시 실행 순서:
// 1. INSERT (restaurant_id=15, food_category_id=2) ← 이미 존재! → 중복 키 위반
// 2. INSERT (restaurant_id=15, food_category_id=3)
// 3. DELETE (restaurant_id=15, food_category_id=1)
// 4. DELETE (restaurant_id=15, food_category_id=2)
```

`saveAll()` 호출 후 `deleteByRestaurantId()`를 호출해도 동일한 문제가 발생한다. Hibernate가 최적화 목적으로 INSERT를 먼저 수행하기 때문이다.

## 해결

`@Modifying @Query`를 사용해 파생 삭제 대신 **즉시 실행되는 bulk DELETE JPQL**로 교체한다.

`@Modifying` + `@Query`는 액션 큐를 우회하여 쿼리를 즉시(flush 전에) 실행하므로, 이후에 추가되는 INSERT보다 항상 먼저 실행된다.

### RestaurantFoodCategoryRepository.java

```java
// Before
void deleteByRestaurantId(Long restaurantId);

// After
@Modifying
@Query("DELETE FROM RestaurantFoodCategory r WHERE r.restaurant.id = :restaurantId")
void deleteByRestaurantId(@Param("restaurantId") Long restaurantId);
```

### DomainImageRepository.java

이미지 교체 시 동일한 패턴이 존재하여 같이 수정한다.

```java
// Before
void deleteAllByDomainTypeAndDomainId(DomainType domainType, Long domainId);

// After
@Modifying
@Query("DELETE FROM DomainImage d WHERE d.domainType = :domainType AND d.domainId = :domainId")
void deleteAllByDomainTypeAndDomainId(
    @Param("domainType") DomainType domainType,
    @Param("domainId") Long domainId);
```

## 주의사항

`@Modifying @Query`로 bulk DELETE를 실행하면 영속성 컨텍스트(1차 캐시)가 갱신되지 않는다. 삭제 후 같은 트랜잭션 내에서 해당 엔티티를 다시 조회하면 삭제 전 상태가 반환될 수 있다.

이를 방지하려면 `@Modifying(clearAutomatically = true)`를 사용한다.

```java
@Modifying(clearAutomatically = true)
@Query("DELETE FROM RestaurantFoodCategory r WHERE r.restaurant.id = :restaurantId")
void deleteByRestaurantId(@Param("restaurantId") Long restaurantId);
```

현재 서비스 코드에서는 삭제 직후 해당 엔티티를 재조회하지 않으므로 `clearAutomatically`는 생략해도 무방하다.

## 파생 삭제를 사용해도 안전한 경우

파생 삭제는 **삭제 후 같은 트랜잭션에서 동일한 제약조건을 가진 INSERT가 없을 때**는 정상 동작한다. 다만 delete-then-insert 패턴에서는 항상 bulk DELETE로 교체하는 것이 안전하다.

## 참고

- Hibernate ORM 문서 - Action Queue: https://docs.jboss.org/hibernate/orm/6.6/userguide/html_single/Hibernate_User_Guide.html#flushing-order
- Spring Data JPA - `@Modifying`: https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html#jpa.modifying-queries
