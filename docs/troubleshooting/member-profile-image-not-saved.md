# Member 프로필 이미지 저장 후 조회 실패 문제

## 증상

프로필 이미지 업데이트 API 호출 후, 프로필 조회 시 이미지가 반환되지 않음.

```
// 업데이트 시점 로그 - 정상 저장된 것처럼 보임
{"message":"Image: ac27c1e9-d388-43a5-a85f-24a9926849d4 | image/png | ACTIVE"}

// 조회 시점 로그 - 빈 결과
{"message":"Warn![]"}
```

## 원인 분석

### 코드 흐름

```java
// MemberService.updateMyProfile()
domainImageRepository.deleteAllByDomainTypeAndDomainId(DomainType.MEMBER, memberId);
DomainImage domainImage = domainImageRepository.save(DomainImage.create(...));

if (image.getStatus() == ImageStatus.PENDING) {
    image.activate();  // status를 ACTIVE로 변경
}
```

### DomainImageRepository의 delete 메서드

```java
@Modifying(clearAutomatically = true, flushAutomatically = true)
@Query("delete from DomainImage di where di.domainType = :domainType and di.domainId = :domainId")
void deleteAllByDomainTypeAndDomainId(...);
```

### 조회 쿼리

```java
@Query("""
    select di from DomainImage di
    join fetch di.image i
    where di.domainType = :domainType
      and di.domainId in :domainIds
      and i.status = 'ACTIVE'  -- 여기가 문제
    """)
List<DomainImage> findAllByDomainTypeAndDomainIdIn(...);
```

### 근본 원인: 영속성 컨텍스트 클리어로 인한 Dirty Checking 실패

1. `deleteAllByDomainTypeAndDomainId` 실행
2. `clearAutomatically = true`로 인해 **영속성 컨텍스트가 클리어됨**
3. 기존에 조회했던 `image` 엔티티가 **Detached 상태**로 전환
4. `image.activate()` 호출 - 메모리상 객체의 status만 변경
5. Detached 상태이므로 **Dirty Checking이 동작하지 않음**
6. 트랜잭션 커밋 시 `image.status` 변경사항이 DB에 반영되지 않음
7. 조회 쿼리의 `i.status = 'ACTIVE'` 조건에 맞지 않아 빈 결과 반환

### JPA 영속성 컨텍스트 상태

| 상태 | 설명 | Dirty Checking |
|------|------|----------------|
| Managed | 영속성 컨텍스트에서 관리 중 | O |
| Detached | 영속성 컨텍스트에서 분리됨 | X |
| Transient | 한 번도 영속화되지 않음 | X |
| Removed | 삭제 예정 | X |

## 해결 방법

Detached 상태의 엔티티를 명시적으로 저장:

```java
if (image.getStatus() == ImageStatus.PENDING) {
    image.activate();
    imageRepository.save(image);  // 명시적 저장 추가
}
```

`save()` 호출 시:
- Detached 엔티티가 다시 Managed 상태로 전환 (merge)
- 변경사항이 DB에 반영됨

## 대안적 해결 방법

### 1. clearAutomatically 제거

```java
@Modifying(flushAutomatically = true)  // clearAutomatically 제거
void deleteAllByDomainTypeAndDomainId(...);
```

단점: 삭제된 엔티티가 영속성 컨텍스트에 남아 있어 조회 시 혼란 발생 가능

### 2. delete 후 image 재조회

```java
domainImageRepository.deleteAllByDomainTypeAndDomainId(...);
image = imageRepository.findById(image.getId()).orElseThrow();  // 재조회
image.activate();
```

### 3. JPQL UPDATE 쿼리 사용

```java
@Modifying
@Query("update Image i set i.status = 'ACTIVE' where i.id = :id")
void activateById(@Param("id") Long id);
```

## 교훈

1. `@Modifying(clearAutomatically = true)` 사용 시 영속성 컨텍스트 클리어 인지
2. 클리어 후 기존 엔티티는 Detached 상태 - Dirty Checking 불가
3. Detached 엔티티 수정 시 명시적 `save()` 또는 `merge()` 필요
