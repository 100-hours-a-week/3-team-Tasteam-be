# module-common

## 이 레이어는 무엇인가

전체 모듈이 공유하는 **최소한의 공통 타입 라이브러리** 레이어다.
응답 래퍼, 예외 타입, 페이지네이션, 공통 유효성 검사처럼 어느 레이어에서든 참조하는 순수 타입/유틸만 포함한다.
Spring Bean이나 인프라 기술에 의존하지 않아 의존성 사이클 없이 어디서든 `api` 의존으로 사용할 수 있다.

---

## 의존성 위치

```
module-app:* / module-internal:* / module-infra:* / module-domain:*
        ↓ (모두 이 레이어를 참조)
   module-common:support
        ↓ (참조 가능)
   표준 라이브러리만 (Spring-web, Jackson, Jakarta Validation)
```

---

## 포함된 모듈

| 모듈 | 설명 |
|---|---|
| `module-common:support` | 공통 응답 래퍼, 예외 타입, 페이지네이션, 유효성 검사 |

### module-common:support

```
global/
├── dto/
│   ├── api/          ApiResponse<T>, SuccessResponse, ErrorResponse, ErrorDetail
│   └── pagination/   PageResponse<T>, CursorPageResponse<T>, PageSortRequest
├── exception/
│   ├── business/     BusinessException, NotFoundException
│   ├── code/         ErrorCode (에러 코드 enum)
│   └── external/     ExternalApiException
└── validation/       SafeKeyword (XSS 방어용 커스텀 검사 어노테이션)
```

---

## 포함되면 안 되는 것

| 금지 항목 | 이유 |
|---|---|
| Spring `@Component`, `@Bean`, `@Configuration` | 인스턴스 의존성이 생겨 테스트/재사용성이 떨어짐 |
| JPA, Redis, Kafka 등 인프라 기술 의존 | 모듈 의존 순서 위반 — infra 레이어에서 담당 |
| 도메인 엔티티, 비즈니스 규칙 | domain 레이어에서 담당 |
| 특정 앱/모듈 전용 타입 | 범용성이 없으면 각 모듈 내에서 정의 |

---

## 의존 관계

### 이 레이어가 의존할 수 있는 것

| 허용 | 예시 |
|---|---|
| JDK 표준 라이브러리 | `java.util`, `java.time` |
| Spring-web (`HttpStatus` 등 값 타입) | `org.springframework:spring-web` |
| Jackson (DTO 직렬화) | `com.fasterxml.jackson.core:jackson-databind` |
| Jakarta Validation | `jakarta.validation:jakarta.validation-api` |

다른 내부 모듈은 **절대 의존 불가**.

### 이 레이어를 의존하는 것

사실상 전체 모듈.
- `module-domain:core` — api 의존
- `module-infra:*` — api 의존
- `module-internal:*` — api 의존
- `module-app:*` — implementation 의존

---

## 새 공통 타입 추가 가이드

이 레이어에 추가해도 되는 코드:
- **모든 모듈에서 사용**하는 순수 Java 타입 (DTO, enum, interface)
- **인프라 기술에 의존하지 않는** 유틸 (날짜 포맷, 문자열 처리 등)
- **테스트 픽스처** — `src/testFixtures/`에 작성하면 `testFixtures(project(':module-common:support'))`로 참조 가능

이 레이어에 추가하면 안 되는 코드:
- 특정 도메인 한정 타입 → 해당 도메인 모듈에 정의
- 2개 이하 모듈에서만 사용하는 타입 → 사용 모듈 내에서 정의
