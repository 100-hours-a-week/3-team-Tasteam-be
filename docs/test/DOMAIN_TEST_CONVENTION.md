# 도메인 테스트 컨벤션 (Entity / Policy)

- 도메인 Entity/Policy 테스트는 **순수 도메인 규칙과 불변식, 상태 전이를 빠르게 검증하는 레이어**다.
- JPA 매핑/쿼리, 서비스 로직, HTTP 응답 등은 다른 테스트 레이어에 맡기고,  
  이 레이어에서는 “이 입력/행위가 도메인 객체/정책에 어떤 상태 변화를 일으켜야 하는가”만 본다.
- Fixture와 Guard/경계값 테스트를 적절히 활용해, 도메인 규칙을 코드 수준에서 명확히 문서화한다.

> 대상: 순수 도메인 테스트  
> - Entity 테스트 (`PostTest`, `CommentTest`, `MemberTest`, `PostTagTest`, `SeriesTest`, `TagTest` 등)  
> - Policy 테스트 (`OwnershipPolicyTest`, `PostLikePolicyTest`, `ViewCountPolicyTest` 등)

---

## 1. 도메인 Entity 테스트의 역할

Entity 테스트(예: `PostTest`, `CommentTest`, `MemberTest`)는 다음을 검증합니다.

- **생성 팩토리/정적 메서드의 규칙**
  - `Post.create`, `Comment.create`, `Member.create`, `Series.create`, `Tag.create` 등에서:
    - 필수 값 누락 시 예외가 발생하는지 (`create_requiresMandatoryFields` 류 테스트).
    - 기본값/초기 상태가 의도대로 설정되는지  
      (예: 카운터 0, 삭제 상태 false, 공개 범위 기본값, 프로필 필드 기본값 등).
- **도메인 메서드의 상태 전이**
  - 게시글 수정, 댓글 내용 수정, 프로필 변경, 시리즈 설정/제거 등:
    - 유효한 입력에 대해 필드가 올바르게 변경되는지 (`updatePost_updatesFields`, `updateContent_works` 등).
    - Guard 조건(길이 제한, null/blank 허용 여부 등)이 위반될 경우 적절한 예외를 던지는지  
      (`titleLengthGuard`, `updateHandle_lengthGuard`, `updateThumbnail_setsUrl` 등).
- **도메인 불변식/경계값**
  - 카운터/플래그가 허용 범위를 벗어나지 않는지:
    - 좋아요 수/조회수/댓글 수가 0 미만으로 내려가지 않는지 (`decrementLikes_doesNotGoBelowZero`).
    - 문자열/리스트 길이 제한(핸들/회사/위치/요약 등)과 null 처리 규칙.
- **삭제/복구/상태 플래그 전이**
  - soft delete 플래그 및 복구 메서드가 기대대로 동작하는지 (`deleteAndRestore`).
  - 공개 범위, 임시저장/발행 상태, 댓글 허용 여부 등 플래그 전환 메서드.

요약하면, Entity 테스트는 **“도메인 객체 스스로가 지켜야 하는 규칙과 상태 전이”**를 빠르게 검증하는 레이어입니다.  
이 레이어에서는 DB/스프링 의존 없이 순수 자바 객체로만 테스트를 작성합니다.

---

## 2. Policy 테스트의 역할

Policy 테스트(예: `OwnershipPolicyTest`, `PostLikePolicyTest`, `ViewCountPolicyTest`)는 다음을 검증합니다.

- **권한/소유권/사용 가능 여부와 같은 도메인 정책**
  - `OwnershipPolicy.validateOwnership`:
    - 소유자 ID와 요청자 ID가 같으면 예외가 발생하지 않고,
    - 다르면 `CustomException(CommonErrorCode.NO_PERMISSION)`을 던지는지.
  - `PostLikePolicy.validateCanLike` / `validateCanUnlike`:
    - 이미 좋아요를 누른 상태/누르지 않은 상태에 따라 예외 발생 여부가 올바른지.
- **정책이 의존하는 리포지토리/외부 의존성 상호작용**
  - `PostLikePolicy`는 `PostLikeRepository.existsByPostIdAndMemberId` 호출 결과에 따라 정책 결과를 결정한다.
  - 테스트에서는 Repository를 Mock 처리하고,  
    “exists가 true/false일 때 정책이 어떤 예외/성공 경로를 선택하는지”만 검증한다.
- **순수 정책 객체(외부 의존성 없는 경우)의 순수 로직**
  - `OwnershipPolicy`, `ViewCountPolicy`처럼 외부 의존성이 없는 정책은:
    - 입력 조합에 따라 예외/true/false 같은 결과가 올바른지 순수 함수 수준에서 검증한다.

Policy 테스트는 **“특정 상황에서 허용/거부/예외 판단을 어떻게 내릴지”**를 명확하게 문서화하는 역할을 합니다.

---

## 3. 도메인 테스트에서 “하지 않는” 것들

- **JPA 매핑/쿼리/트랜잭션**
  - Entity 테스트에서는 JPA 애너테이션(`@Entity`, `@ManyToOne` 등)의 실제 동작을 검증하지 않습니다.
  - JPA 매핑/쿼리/트랜잭션은 `@RepositoryJpaTest` 기반 Repository 테스트의 책임입니다.
- **서비스/컨트롤러/HTTP 응답**
  - 도메인 테스트는 Service/Controller 계층을 올리지 않습니다.
  - HTTP 상태 코드나 응답 포맷(`ApiResponse`) 검증은 WebMvc/통합 테스트에서 다룹니다.
- **보안/필터/인프라 설정**
  - Authentication/Authorization, 필터 체인, 로깅/AOP 등은 도메인 테스트의 대상이 아닙니다.

이렇게 경계를 나누어, 도메인 테스트는 **순수한 비즈니스 규칙과 객체 상태**에만 집중합니다.

---

## 4. 테스트 작성 패턴 (Entity)

### 4.1 생성/팩토리 메서드 테스트

- **목적**
  - `create`/`of` 정적 팩토리가 도메인 규칙(필수 값, 기본값, 초기 상태)을 지키는지 확인.
- **패턴**
  - Given:
    - 필요한 연관 도메인 객체를 생성 (`Member.create`, `Post.create`, `Series.create` 등).
  - When:
    - 정상 케이스: `Entity.create(...)` 호출.
    - 실패 케이스: 필수 인자 누락/빈 값/길이 초과 등으로 `create` 호출.
  - Then:
    - 정상 케이스: 주요 필드/초기 상태 검증.
    - 실패 케이스: `IllegalArgumentException` (또는 도메인에서 정한 예외 타입) 발생 검증.

### 4.2 도메인 메서드/상태 전이 테스트

- **목적**
  - 도메인 메서드(`update*`, `delete`, `restore`, `increment*`, `decrement*` 등)가  
    올바른 상태 전이와 Guard를 적용하는지 확인.
- **패턴**
  - Given: 유효한 초기 상태의 엔티티 생성.
  - When:
    - 정상 입력 → 메서드 호출.
    - 비정상 입력(길이 초과, 빈 문자열 등) → 메서드 호출.
  - Then:
    - 정상 입력: 관련 필드 값이 기대대로 변경되었는지.
    - 비정상 입력: 예외 타입/메시지 일부를 검증해 Guard가 제대로 작동하는지.

### 4.3 불변식/경계값 테스트

- **목적**
  - “0 미만으로 내려가지 않는다”, “길이는 N자를 넘을 수 없다” 같은 도메인 불변식을 경계값까지 확인.
- **패턴**
  - 카운터:
    - 0에서 decrement → 그대로 0 유지.
    - 여러 번 increment/decrement 후 기대 카운트 확인.
  - 문자열 길이:
    - 허용 최대 길이(N)로는 성공, N+1에서는 예외.

### 4.4 단순 상태 변경(setter)에 대한 기준

- **테스트 대상이 되는 경우**
  - 메서드 내부에 **검증/가드/파생 로직**이 있다.
    - 예: 길이 제한, null 처리 규칙, 값 정규화, 카운트/플래그 일관성 유지 등.
  - 메서드가 도메인에서 의미 있는 상태 전이(`publish`, `deactivate`, `restore` 등)를 표현한다.
  - 과거에 버그가 있었거나, 실수하기 쉬운 부분이라 회귀 방지가 필요하다.
- **별도 테스트를 생략해도 되는 경우**
  - 메서드가 정말로 `this.field = value;` 수준의 **순수 setter**에 가깝고,
  - 규칙/가드/파생 계산이 전혀 없으며,
  - 해당 필드는 레포지토리/서비스/통합 테스트에서 **간접적으로 값이 저장·조회되는지**가 이미 검증되고 있다.
- 정리하면, 도메인 테스트는 “규칙/불변식/의미 있는 상태 전이”에 집중하고,  
  단순 데이터 대입 수준의 메서드까지 모두 테스트 대상으로 삼지는 않는다.

---

## 5. 테스트 작성 패턴 (Policy)

- **OwnershipPolicyTest**
  - Very simple policy: `resourceOwnerId == requesterId` 여부만 본다.
  - 외부 의존성 없으므로 **순수 객체 생성 후 바로 호출**.
  - 성공/실패 각각에 대해 “예외 없음 / CustomException 발생”만 검증.
- **PostLikePolicyTest**
  - `PostLikeRepository`를 Mock 처리.
  - `existsByPostIdAndMemberId` 결과(true/false)에 따라:
    - `validateCanLike` / `validateCanUnlike`가 예외를 던지는지/넘어가는지 검증.
  - Repository 쿼리 내용/횟수는 관심 없고, “이 상황에서 어떤 예외/성공 경로인지”만 본다.
- **ViewCountPolicyTest**
  - 정책이 의존하는 입력 컨텍스트(ViewContext) 조합에 따라,  
    `shouldCount` 결과(true/false)를 검증하는 식으로 확장 가능.

Policy 테스트는 “입력 조건 → 허용/거부/예외”의 매트릭스를 명확히 해 주는 것을 목표로 합니다.

---

## 6. 테스트 픽스처 사용 컨벤션 (Entity/Policy)

- **엔티티 생성 Fixture**
  - **테스트 대상(SUT)인 엔티티 자체**에 대해서는, 가능하면 Fixture 대신 **실제 정적 팩토리/생성자**를 직접 호출해 생성한다.
    - 예: `PostTest`에서 `PostFixture`로 Post를 만들기보다는, `Post.create(member, "제목", "내용")`을 직접 호출한다.
    - 이유: Fixture 내부도 동일한 팩토리를 호출하는 경우가 많아, 구현 버그를 Fixture가 그대로 따라가면 테스트가 버그를 놓칠 수 있기 때문이다.
  - 대신 **연관 엔티티/부수적인 도메인 객체**는 Fixture를 적극 사용한다.
    - 예: `PostTest`에서 `MemberFixture.create()`로 Member를 만들고, 그 Member를 이용해 `Post.create`를 호출.
  - Fixture는 “도메인에서 의미 있는 기본 상태”를 표현하고,  
    개별 테스트에서는 이 기본 상태에서 달라지는 부분만 드러내는 식으로 작성한다.
- **ID가 이미 있는 Fixture**
  - Entity 테스트에서는 보통 ID가 필요하지 않으므로, `createWithId`보다 순수 `create`를 많이 사용한다.
  - 복합키(`PostTagId` 등)처럼 ID 로직 자체를 검증할 때는,  
    `createWithId` 계열 Fixture를 사용할 수 있다 (`PostTagTest` 패턴).
- **Policy 테스트에서의 Fixture**
  - Policy 테스트는 주로 원시 타입(Long 등)과 Repository Mock에 의존하므로,
  - 별도의 도메인 Fixture는 최소한만 사용하고,  
    “어떤 postId/memberId 조합인지”가 잘 드러나도록 숫자를 직접 쓰는 경우가 많다.
- **중복/복잡도 관리**
  - 도메인 테스트에서도 Given 절이 복잡해지면 Fixture/헬퍼로 추출하되,
  - 테스트 본문에서 “어떤 규칙/불변식을 검증하는지”가 눈에 잘 들어오도록 유지한다.

---

## 7. 언제 도메인 테스트를 추가/수정할까?

- **엔티티에 새로운 필드/도메인 메서드가 추가되었을 때**
  - 생성 시 기본값/초기 상태가 중요하면, `create_*` 테스트에 필드 검증을 추가.
  - 새 메서드는 정상/실패/경계값 케이스를 한두 개씩 가지도록 테스트 추가.
- **도메인 정책(Policy)이 추가/변경되었을 때**
  - `OwnershipPolicy`, `PostLikePolicy`, `ViewCountPolicy`와 같은 정책 클래스가 생기거나 수정되면,
  - 해당 정책의 허용/거부/예외 경우를 명확히 하는 테스트를 함께 작성/수정.
- **불변식/비즈니스 규칙이 변경되었을 때**
  - 예: 제목 최대 길이 변경, 요약/핸들/회사/위치 길이 제한 변경 등.
  - 관련 테스트의 기대값을 수정하고, 경계값 케이스를 다시 확인.

