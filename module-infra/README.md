# module-infra

## 이 레이어는 무엇인가

**외부 시스템 어댑터** 레이어다.
각 모듈은 하나의 외부 기술 또는 서비스에만 집중하며, 도메인 로직 없이 순수한 기술 통합만 담당한다.
애플리케이션(module-app)과 도메인(module-domain) 사이에서 데이터베이스, 메시지 브로커, 클라우드 서비스 등을 추상화한다.

---

## 의존성 위치

```
module-app:* / module-internal:*
        ↓ (이 레이어를 참조)
   module-infra:persistence / redis / messaging / storage / notification / geocode / ai / webhook / analytics
        ↓
   module-domain:core (persistence, messaging만)
   module-common:support (전체)
```

---

## 포함된 모듈

| 모듈 | 담당 외부 기술 | 주요 내용 |
|---|---|---|
| `module-infra:persistence` | PostgreSQL + JPA + QueryDSL | 전 도메인 JPA 리포지토리, QueryDSL 구현체, Flyway 설정 |
| `module-infra:redis` | Redis | RedisClient, 직렬화 설정, 연결 설정 |
| `module-infra:messaging` | Apache Kafka | 프로듀서/컨슈머, 아웃박스 패턴, 분산 추적, 재처리 |
| `module-infra:storage` | AWS S3 + 이미지 처리 | 파일 업로드, Presigned URL, WebP 변환, 썸네일 생성 |
| `module-infra:notification` | AWS SES + FCM + Web Push | 이메일, Firebase 푸시, 브라우저 Web Push |
| `module-infra:geocode` | Naver Maps API + Nominatim | 주소→좌표 변환, 좌표→주소 역변환 |
| `module-infra:ai` | LLM API (OpenAI, Claude 등) | AI 리뷰 분석, 키워드 추출, 추천 생성 |
| `module-infra:webhook` | Discord Webhook | 이벤트 알림 발송, 페이로드 템플릿 |
| `module-infra:analytics` | 사용자 이벤트 추적 | (현재 최소 구현, 추후 분리 예정) |

### 모듈별 상세

#### module-infra:persistence
```
domain/
└── <각 도메인>/
    ├── repository/       JPA Repository 인터페이스
    ├── repository/impl/  QueryDSL 커스텀 구현체
    └── repository/projection/ 쿼리 결과 프로젝션 DTO
global/config/            JPA, QueryDSL, Flyway 설정
infra/flyway/             마이그레이션 유틸
```
- QueryDSL 5.1.0 + Hibernate Spatial (PostGIS 공간 쿼리)
- 전체 도메인 리포지토리 20+ 개

#### module-infra:redis
```
global/config/   RedisConfig (ConnectionFactory, Template)
infra/redis/     RedisClient 인터페이스 + 구현체
```

#### module-infra:messaging
```
infra/messagequeue/
├── serialization/   JSON 직렬화/역직렬화
├── trace/           분산 추적 + 관리 API
└── exception/       메시징 전용 예외
infra/messaging/     ChatEventPublisher 등
```
- 트랜잭셔널 아웃박스 패턴 지원
- Kafka 토픽별 프로듀서/컨슈머 추상화

#### module-infra:storage
```
infra/storage/
├── s3/         S3StorageClient, Presigned URL
│   └── policy/ 접근 정책
└── dummy/      로컬 개발용 더미 구현체
```
- 이미지 최적화: WebP 변환 + 썸네일 생성 포함

#### module-infra:notification
```
infra/
├── email/
│   ├── ses/   AWS SES 발송 클라이언트
│   └── log/   이메일 감사 로그
└── firebase/  Firebase Admin SDK (FCM)
```
- Thymeleaf 템플릿으로 이메일 렌더링

---

## 포함되면 안 되는 것

| 금지 항목 | 이유 |
|---|---|
| 비즈니스 로직, 도메인 규칙 | → module-domain:core에서 담당 |
| `module-infra:*` → `module-infra:*` 의존 | 모듈 간 결합도 증가, 의존 사이클 위험 |
| `@RestController`, 서비스 오케스트레이션 | → module-app에서 담당 |
| Spring Security 설정 | → module-internal:security에서 담당 |

유일한 예외: `module-internal:web`이 rate-limiting을 위해 `module-infra:redis`를 사용한다.
infra 모듈끼리 서로 의존하는 것은 금지다.

---

## 의존 관계

### 이 레이어가 의존할 수 있는 것

| 허용 대상 | 조건 |
|---|---|
| `module-common:support` | 전체 모듈 허용 (`api` 의존) |
| `module-domain:core` | `persistence`, `messaging`만 허용 |
| 해당 외부 기술 SDK | 각 모듈이 담당하는 기술만 |

### 이 레이어를 의존하는 것

| 의존 주체 | 사용 모듈 |
|---|---|
| `module-internal:web` | redis (Rate Limiting) |
| `module-internal:security` | redis (토큰 블랙리스트), webhook (감사 로그) |
| `module-app:*` | 전체 infra 모듈 필요에 따라 |

---

## 새 infra 모듈 추가 가이드

1. `module-infra/<name>/` 디렉터리 생성
2. `settings.gradle`에 `include 'module-infra:<name>'` 추가
3. `build.gradle` 최소 구성:
   ```gradle
   dependencies {
       api project(':module-common:support')
       compileOnly 'org.projectlombok:lombok:1.18.42'
       annotationProcessor 'org.projectlombok:lombok:1.18.42'
       // 해당 기술 SDK만 추가
   }
   ```
4. **단일 책임 원칙**: 모듈 하나 = 외부 기술/서비스 하나
5. 다른 `module-infra:*` 의존은 금지
