# Repository 테스트 컨벤션 (JPA 중심)

> 대상: `@RepositoryJpaTest`로 작성된 JPA 리포지토리 테스트  
> 예: `PostRepositoryTest`, `CommentRepositoryTest`, `PostTagRepositoryTest`, `MemberRepositoryTest` 등

이 문서는 우리 프로젝트에서 **리포지토리 테스트가 무엇을 검증하고, 무엇은 검증하지 않을지**에 대한 기준을 정리합니다.  
단위/서비스/통합 테스트와 역할을 나누어, 리포지토리 테스트가 “JPA 매핑·쿼리·트랜잭션”에 집중하도록 하는 것이 목적입니다.

---

## 0. @RepositoryJpaTest 메타 어노테이션

우리 프로젝트의 리포지토리 테스트는 모두 `@RepositoryJpaTest`를 사용합니다.

`src/test/java/com/devon/techblog/config/annotation/RepositoryJpaTest.java` 정의:

- `@DataJpaTest`
  - JPA 관련 빈(엔티티 매니저, 리포지토리 등)만 로드하는 **슬라이스 테스트**입니다.
  - 서비스/컨트롤러/보안 빈을 띄우지 않으므로, 빠르고 **JPA·쿼리 검증에만 집중**할 수 있습니다.
- `@ActiveProfiles("test")`
  - `application-test.yml` 설정을 사용합니다.
  - 테스트용 DB(H2, Testcontainers 등) 및 테스트 환경 설정을 자동으로 적용합니다.
- `@Import(QueryDslConfig.class, JpaAuditingTestConfig.class)`
  - 실제 코드에서 사용하는 QueryDSL 설정과, 테스트용 JPA Auditing 설정을 함께 주입합니다.
  - 따라서 Repository 테스트에서도 **실서비스와 동일한 QueryDSL/JPA Auditing 환경**에서 쿼리를 검증할 수 있습니다.
- `@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)`
  - Spring Boot가 임의로 H2 인메모리 DB로 바꾸지 않고,  
    우리가 설정한 테스트용 DB 설정을 그대로 사용합니다.
- `@Tag("repository")`
  - Gradle/JUnit에서 `-t repository` 등으로 리포지토리 테스트만 골라 실행할 수 있도록 태깅합니다.

정리하면, `@RepositoryJpaTest`는 **“리포지토리/JPA 전용 통합 테스트 환경”을 한 번에 구성해 주는 메타 어노테이션**입니다.  
이 덕분에 각 Repository 테스트 클래스는 `@RepositoryJpaTest`만 붙이면, 일관된 설정으로 JPA 매핑·쿼리를 검증할 수 있습니다.

---

## 1. Repository 테스트의 역할

리포지토리 테스트는 **“버그가 가장 많이 터지는 구간”을 우선적으로 검증**합니다.  
즉, **조건/정렬/페이징/조인**, **연관관계 저장/조회**, **제약조건 예외**, **N+1 방지 여부** 같은  
운영 장애로 직결되는 부분을 잡아내는 것이 핵심입니다.

### 1.1 커스텀 쿼리 테스트 (가치 최상)

예: `List<Order> findTop10ByUserIdAndStatusOrderByCreatedAtDesc(...)`

- **검증 포인트**
  - 조건(WHERE)이 기대대로 적용되는지
  - 정렬/페이징이 의도대로 동작하는지
  - 조인/필터 조건으로 인해 누락/중복이 발생하지 않는지
- **왜 중요한가**
  - 실제 버그가 가장 많이 발생하는 구간이기 때문

### 1.2 연관관계 저장/조회 테스트

- **검증 포인트**
  - `cascade` 동작 여부
  - `orphanRemoval` 삭제 동작
  - 양방향 매핑 동기화(부모-자식 모두 정상 연결되는지)

### 1.3 제약조건 예외 테스트

예:

```java
assertThatThrownBy(() -> repository.save(duplicateUser))
    .isInstanceOf(DataIntegrityViolationException.class);
```

- **검증 포인트**
  - 유니크/NOT NULL 등 제약조건 위반 시 예외가 발생하는지
- **왜 중요한가**
  - 운영 장애 예방용 테스트

### 1.4 N+1 방지 검증 (고급)

- **검증 포인트**
  - `fetch join`, `@EntityGraph` 적용 여부 확인
  - 단건/리스트 조회 시 의도치 않은 추가 쿼리 폭발 방지

### 1.5 굳이 안 해도 되는 Repository 테스트

| 테스트 | 가치 |
| --- | --- |
| `save()` 호출됐는지 `verify` | 거의 0 |
| `findById` mock 테스트 | 의미 없음 |
| 단순 CRUD happy path | 프레임워크가 이미 보장 |

요약하면, Repository 테스트는 **“조건/정렬/페이징/조인 + 연관관계 + 제약조건 + N+1 방지”**에 집중합니다.

---

## 2. Repository 테스트에서 “하지 않는” 것들

리포지토리 테스트는 다음을 검증하지 않습니다.

- **서비스/도메인 정책 로직**
  - 좋아요 가능 여부(`PostLikePolicy`), 소유권 검증(`OwnershipPolicy`), 비즈니스 규칙(권한/상태 전이 등)은 Repository 테스트에서 다루지 않습니다.
  - 이런 로직은 **서비스 테스트/정책 유닛 테스트**의 책임입니다.
- **컨트롤러/DTO 매핑/응답 포맷**
  - API 레이어에서의 Request/Response 매핑, 유효성 검증, 응답 wrapper(`ApiResponse`) 구조는 Repository 테스트의 대상이 아닙니다.
  - 이런 부분은 Controller/Integration 테스트에서 검증합니다.
- **스프링 빈 설정/보안/필터/트랜잭션 경계 설계**
  - Repository 테스트는 `@RepositoryJpaTest`로 JPA 슬라이스만 구동하며, 전체 컨텍스트/보안/필터는 포함하지 않습니다.
  - 이러한 설정은 `@IntegrationTest` 기반 통합 테스트의 책임입니다.

이렇게 경계를 명확히 해야 테스트가 서로 역할을 침범하지 않고, 유지보수가 쉬워집니다.

---

## 3. 테스트 작성 패턴 (예시 기반 규칙)

### 3.1 저장/조회 기본 시나리오 (`saveAndFind*`)

예시: `PostTagRepositoryTest.saveAndFind`, `PostRepositoryTest.saveAndFind`, `MemberRepositoryTest.saveAndFindWithNewFields`

- **목적**
  - 엔티티가 설계한 매핑대로 DB에 저장·조회되는지 확인하는 “스모크 테스트”.
  - 새 필드/연관관계가 추가될 때, 기본적인 `save`/`findById` 흐름이 깨지지 않았는지 빠르게 감지.
- **패턴**
  - Given: 관련 엔티티를 생성하고, 필요한 부모/연관 엔티티를 먼저 저장.
  - When: `repository.save()` 호출 후, `findById`/`save` 반환 값으로 엔티티 획득.
  - Then:
    - `id`가 발급되었는지 (`assertThat(saved.getId()).isNotNull()`).
    - 중요 필드/연관관계가 기대값과 일치하는지 (`getPost()`, `getTag()`, `getMember()`, 기본값/nullable 등).
- **규칙**
  - 비즈니스 규칙(예: “누가 저장할 수 있는가”)은 여기서 다루지 않는다.
  - “새 필드가 추가되었을 때 최소 한 번은 여기에서 저장/조회가 검증되도록” 유지한다.

### 3.2 커스텀 조회 메서드 테스트

예시:  
- `PostRepositoryTest.findByIdWithMember`, `findByIdWithMember_deletedPost`  
- `CommentRepositoryTest.findByIdWithMember`, `findPostIdByCommentId`, `findByMemberIdWithPost_onlyOwnComments`  
- `PostTagRepositoryTest.findByPostIdWithTag`

- **목적**
  - Query 메서드(`findBy*`, `findBy*With*`)가 **의도한 도메인 규칙을 그대로 반영하는지** 확인.
  - Join, fetch join, where 조건, soft delete 조건 등 쿼리 레벨의 동작을 검증.
- **패턴**
  - Given:
    - 필요한 엔티티/연관 엔티티를 충분히 저장해 “긍정/부정” 케이스를 준비.
    - soft delete/다른 회원의 데이터 등 경계 케이스 포함.
  - When: 해당 쿼리 메서드 한 번만 호출.
  - Then:
    - 결과 리스트 크기/존재 여부.
    - 필요하다면 정렬/필터 조건이 반영되었는지 (`onlyOwnComments`, soft delete 시 조회되지 않는지 등).
    - 연관 엔티티가 필요한 만큼 초기화 되어 있는지.
- **규칙**
  - 쿼리 메서드 하나당 **대표 시나리오 1~2개**만 둔다. 너무 많은 조합은 서비스/도메인 계층에서 검증.
  - “쿼리가 이런 방식으로 구현되었다”를 검증하는 것이 아니라,  
    “쿼리 결과가 도메인에서 약속한 규칙(본인만 조회, 삭제된 건 제외 등)을 만족하는가”를 검증한다.

### 3.3 카운터/상태 변경 쿼리 테스트

예시: `PostRepositoryTest.incrementLikeCount`, `decrementLikeCount_whenZero`, `incrementViewCount`,  
`incrementCommentCount`, `decrementCommentCount_whenZero`

- **목적**
  - `@Modifying` 쿼리의 영향을 받는 row 수와 실제 엔티티 필드 값 변경을 함께 검증.
  - 경합/동시성은 별도의 전략 문서(예: `ADR_POSTLIKE_CONCURRENCY.md`)가 다루고, 여기서는 단일 쓰레드 기준 동작만 확인.
- **패턴**
  - Given: 초기 값이 0 또는 특정 값인 엔티티 저장.
  - When:
    - 카운터 메서드 호출 (`increment*`, `decrement*`).
    - `flush()` 후 다시 `findById`로 재조회.
  - Then:
    - 반환값(영향 받은 row 수) 검증.
    - 재조회된 엔티티의 카운트 필드가 기대값인지 검증.
- **규칙**
  - “0일 때 감소해도 0을 유지한다”처럼 **경계값 케이스**를 반드시 한 번은 포함한다.
  - 카운트 증가/감소가 여러 곳에서 호출된다고 해서, Repository 테스트에서 모든 조합을 검증하지는 않는다  
    (서비스/통합 테스트에서 전체 흐름을 다룬다).

### 3.4 삭제/Cascade/orphanRemoval 테스트

예시: `PostTagRepositoryTest.deleteByPostId`, `cascadeDeleteWithPost`

- **목적**
  - 커스텀 삭제 쿼리(`deleteBy*`)와 JPA cascade 설정이 실제 DB에서 기대한 대로 동작하는지 확인.
  - 부모/자식 관계에서 “누가 같이 삭제되고, 누가 남는지”를 명확히 한다.
- **패턴**
  - Given:
    - 양방향 연관관계가 있다면 반드시 양쪽을 동기화 (`post.addPostTag(pt)` 등).
    - 부모/자식 엔티티를 저장한 뒤 flush로 DB에 반영.
  - When:
    - 커스텀 삭제 메서드 또는 부모 삭제를 호출.
    - flush 후 관련 엔티티를 재조회.
  - Then:
    - 삭제 대상이 비어 있는지.
    - 삭제되면 안 되는 엔티티(tag, member 등)는 그대로 남아 있는지.
- **규칙**
  - H2 등 테스트 DB의 제약으로 일부 시나리오는 Disabled 주석을 남기더라도, **의도를 문서화**해 둔다.
  - Cascade 설정은 Repository 테스트에서 최소 1~2개 대표 시나리오만 검증하고,  
    나머지는 엔티티 설계(Annotation)와 코드 리뷰에 의존한다.

---

## 4. 구현 세부에 과하게 결합되지 않기 위한 가이드

- **어떤 JPA 구현 세부를 검증하지 않는가**
  - “몇 번의 SQL 쿼리가 나갔는지”, “LAZY vs EAGER 전략 자체”는 Repository 테스트에서 직접 assert하지 않는다.
  - N+1 문제, 성능 최적화는 별도의 성능/쿼리 로그 기반 점검으로 다룬다.
- **테스트는 “쿼리 내용”이 아니라 “쿼리 결과”를 검증**
  - `findByMemberIdWithPost`가 JPQL로 구현되었는지, Querydsl로 구현되었는지는 테스트가 신경 쓰지 않는다.
  - 오로지 “이 메서드를 호출했을 때, 도메인 규칙에 맞는 결과를 돌려주는지”만 본다.
- **리팩토링 내성을 고려한 이름과 시나리오**
  - 테스트 이름/DisplayName은 “쿼리 구현 방식”보다 “도메인 규칙”을 서술한다.  
    예: `findByMemberIdWithPost_onlyOwnComments`, `findByIdWithMember_deletedPost`.
  - 쿼리 구현을 변경해도(예: JPQL → Querydsl), 테스트는 그대로 통과해야 한다.

---

## 5. 테스트 픽스처(Entity/Repository) 사용 컨벤션

- **엔티티/도메인 Fixture**
  - `MemberFixture`, `PostFixture`, `CommentFixture`, `TagFixture` 처럼 **엔티티 생성은 Fixture를 적극 활용**한다.
  - 이유:
    - 테스트마다 엔티티 생성 로직을 중복하지 않고,
    - “기본값과 달라지는 부분”만 드러내어 의도를 명확히 표현할 수 있기 때문이다.
- **ID/상태를 가진 엔티티**
  - Repository 테스트에서는 실제 DB에 저장되며 ID가 발급되므로,  
    `createWithId(1L)` 같은 “이미 ID가 있는 Fixture”보다는,  
    **저장 전 상태의 엔티티를 생성하는 Fixture (`create(...)`)**를 선호한다.
  - ID, 카운터 값 등은 실제 저장/쿼리 결과를 통해 검증한다.
- **복잡한 초기 상태 구성**
  - 여러 개의 Post/Comment/Tag/Member를 섞어 저장해야 하는 경우:
    - 가능한 한 Fixture를 조합해 구성하고,
    - 테스트 본문에서는 **핵심 차이(예: 다른 회원, 삭제 플래그 설정)**만 눈에 들어오도록 정리한다.
- **Repository Stub/Fake**
  - Repository 테스트 자체는 실제 JPA 구현을 대상으로 하므로,  
    별도의 Stub/Fake Repository는 사용하지 않는다.
  - Stub/Fake Repository는 서비스/도메인 유닛 테스트에서 사용하는 도구로 남긴다.
