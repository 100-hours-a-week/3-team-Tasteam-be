# Repository 테스트 컨벤션

> 대상: `@RepositoryJpaTest` 기반 JPA/QueryDSL 테스트

Repository 테스트는 **쿼리 경계와 영속성 계약**을 검증한다.
핵심은 “이 메서드가 의도한 DB 결과를 반환하는가”다.

## 1. 현재 메타 어노테이션 계약

정의 위치:

- `app-api/src/test/java/com/tasteam/config/annotation/RepositoryJpaTest.java`

현재 구성:

- `@DataJpaTest`
- `@ActiveProfiles("test")`
- `@Import(QueryDslConfig.class, JpaAuditingTestConfig.class, TestcontainersConfiguration.class, QueryDslRepositoryTestConfig.class)`
- `@AutoConfigureTestDatabase(replace = NONE)`
- `@Tag("repository")`

의미:

- 리포지토리/JPA 슬라이스만 로드한다.
- 테스트는 임의 인메모리 DB가 아니라 현재 설정된 Testcontainers PostgreSQL을 사용한다.
- QueryDSL과 JPA Auditing을 실제 프로젝트 설정과 최대한 맞춘다.

## 2. 무엇을 검증하는가

- QueryDSL / JPQL / 파생 쿼리 결과
- 조건, 정렬, 페이징, 조인 결과
- soft delete, cascade, orphanRemoval, 연관관계 저장/조회
- 유니크/NOT NULL 등 제약조건 위반
- `@Modifying` 카운터/상태 변경 쿼리

가치가 큰 경우:

1. QueryDSL이나 Native SQL이 복잡한 경우
2. 여러 엔티티를 조인하거나 집계하는 경우
3. soft delete / cascade / fetch join이 중요한 경우

## 3. 무엇을 검증하지 않는가

- 서비스 정책과 권한 로직
- HTTP 요청/응답 구조
- 보안 필터와 전체 애플리케이션 컨텍스트
- 리포지토리 호출 횟수 같은 mock 기반 구현 세부

## 4. 작성 패턴

### 4.1 저장/조회 스모크

- 새 필드/연관관계가 추가되면 최소 한 번은 저장/조회가 깨지지 않는지 확인한다.

### 4.2 쿼리 결과 검증

- 쿼리 구현 방식보다 결과 의미를 검증한다.
- 예: “삭제된 데이터는 제외된다”, “본인 데이터만 조회된다”, “정렬이 예상대로다”

### 4.3 상태 변경 쿼리

- 영향 row 수와 실제 재조회 결과를 함께 본다.
- `flush()` 또는 재조회로 DB 반영 결과를 확인한다.

### 4.4 제약조건

- 중복 저장, 필수값 누락 등 운영 장애로 이어질 제약조건 위반을 명시적으로 검증한다.

## 5. Fixture 사용 계약

- 엔티티 생성은 `com.tasteam.fixture` 아래 Fixture를 적극 사용한다.
- Repository 테스트에서는 `createWithId(...)`보다 저장 전 상태의 `create(...)`를 우선한다.
- 실제 JPA 구현을 검증하므로 Fake/Stub Repository는 사용하지 않는다.

## 6. 실행 시 유의사항

- 현재 태그는 `repository`다.
- `./gradlew integrationTest`에는 포함되지 않는다.
- 기본 회귀는 `./gradlew test` 또는 개별 `--tests` 실행에 의존한다.

## 7. 현재 갭

- 과거 문서에 남아 있던 `com/devon/techblog` 경로 기준 설명은 현재 `com/tasteam` 기준으로 정리했다.
- 전용 `repositoryTest` 태스크는 아직 없으므로, 문서와 실행 경험 사이에 이름 차이가 남아 있다.
