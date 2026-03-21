# module-domain

## 이 레이어는 무엇인가

**순수 도메인 모델** 레이어다.
JPA 엔티티, 도메인 이벤트, 정책(Policy), 타입(Enum), 요청/응답 DTO를 포함한다.
외부 인프라에 의존하지 않으며, 비즈니스의 핵심 개념과 규칙이 여기서 정의된다.

---

## 의존성 위치

```
module-app:* / module-internal:security / module-infra:persistence / module-infra:messaging
        ↓ (이 레이어를 참조)
   module-domain:core
        ↓
   module-common:support
```

---

## 포함된 모듈

| 모듈 | 설명 |
|---|---|
| `module-domain:core` | 전체 도메인 엔티티, 이벤트, 정책, DTO |

### module-domain:core 주요 도메인 패키지

| 도메인 | 내용 |
|---|---|
| `auth` | 리프레시 토큰, OAuth 계정 |
| `member` | 회원 프로필, 이벤트 |
| `restaurant` | 식당 (PostGIS 공간 데이터), 메뉴, 영업시간, 정책 |
| `review` | 리뷰, 키워드, 평점 |
| `group` | 그룹 관리, 멤버십, 이벤트 |
| `subgroup` | 소그룹 |
| `chat` | 채팅 메시지, 타입 |
| `file` | 파일/이미지 메타데이터 |
| `notification` | 알림 설정, 히스토리 |
| `recommendation` | 개인화 추천 |
| `search` | 검색 최적화 엔티티 |
| `promotion` | 프로모션/특가 |
| `announcement` | 공지사항 |
| `favorite` | 즐겨찾기 식당 |
| `report` | 신고 |
| `batch` | 배치 작업 추적 |
| `admin` | 관리자 정책 |
| `common` | 기본 Repository 인터페이스, 공유 타입 |

각 도메인은 아래 구조를 따른다:

```
domain/<name>/
├── entity/       JPA 엔티티 (setters 없음, semantic method 사용)
├── event/        도메인 이벤트
├── policy/       비즈니스 정책 (Policy)
├── type/         Enum 타입
└── dto/
    ├── request/  HTTP 입력 DTO (*Request)
    └── response/ HTTP 출력 DTO (*Response)
```

---

## 포함되면 안 되는 것

| 금지 항목 | 이유 |
|---|---|
| REST 컨트롤러(`@RestController`) | → module-app 레이어에서 담당 |
| 서비스 오케스트레이션(`@Service`) | → module-app 레이어에서 담당 |
| Redis, Kafka, S3 등 외부 인프라 의존 | → module-infra 레이어에서 담당 |
| Spring `@Configuration`, `@Component` Bean | 도메인은 순수 모델이어야 함 (JPA 어노테이션은 허용) |
| `module-infra:*`, `module-internal:*` 의존 | 순환 의존 발생 위험 |

---

## 의존 관계

### 이 레이어가 의존할 수 있는 것

| 허용 | 용도 |
|---|---|
| `module-common:support` | 공통 예외, 응답 타입 |
| `spring-boot-starter-data-jpa` | JPA/Hibernate 어노테이션 |
| `hibernate-spatial` | PostGIS 공간 타입 (`Point` 등) |
| `querydsl-jpa` | QueryDSL Q타입 생성 |
| `jackson-databind` | DTO `@JsonCreator` 등 |
| `spring-boot-starter-validation` | DTO `@NotNull`, `@Size` 등 |

### 이 레이어를 의존하는 것

| 모듈 | 이유 |
|---|---|
| `module-infra:persistence` | JPA 리포지토리가 엔티티 참조 |
| `module-infra:messaging` | 도메인 이벤트 발행 |
| `module-internal:security` | 회원/인증 엔티티 참조 |
| `module-app:*` | 서비스/컨트롤러에서 도메인 사용 |

---

## 엔티티 설계 규칙

- **Setter 금지** — 상태 변경은 `changeXxx()`, `activate()`, `deactivate()` 같은 의미 있는 메서드만 허용
- **정적 팩토리** — 생성은 `create(...)` 메서드로만
- **ID 생성** — `@GeneratedValue(strategy = GenerationType.IDENTITY)`
- **Enum** — `@Enumerated(EnumType.STRING)` 필수
- **시간** — `Instant` 타입 (UTC 저장)
- **유효성** — 생성/변경 시점에 검사, 유효하지 않은 상태로 생성 불가

## 새 도메인 추가 가이드

1. `domain/<new-domain>/` 패키지 생성
2. 위 폴더 구조(`entity/`, `event/`, `policy/`, `type/`, `dto/`) 준수
3. 외부 인프라 의존 없이 순수 Java + JPA + Jackson만 사용
4. 테스트 픽스처는 `src/testFixtures/`에 `*Fixture` 클래스로 작성
