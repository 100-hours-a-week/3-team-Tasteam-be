# 엔티티 작성 컨벤션

> Member 엔티티 분석 기반 표준 컨벤션

## 1. 클래스 레벨

### Lombok 어노테이션
```java
@Entity
@Getter
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "테이블명")
```
- **Setter 금지**: 불변성 보장, 비즈니스 메서드로만 상태 변경
- **Builder/생성자 접근제한**: `PROTECTED` - 직접 생성 방지, 팩토리 메서드 강제
- **@Table**: snake_case 테이블명 명시

## 2. 필드 정의

### ID
```java
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE)
private Long id;
```
- **PostgreSQL 기준**: `SEQUENCE` 기본 사용 (배치 insert/성능 유리)
  - 공통 시퀀스 사용: 기본 `hibernate_sequence`(또는 프로젝트 표준 global seq)로 통일
  - `allocationSize`는 기본값(50) 사용, 변경 필요 시 공통 설정으로만 관리

#### 정교한 배치가 필요한 경우 (추가 설정)
```java
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "entity_seq")
@SequenceGenerator(
        name = "entity_seq",
        sequenceName = "entity_seq",
        allocationSize = 50
)
private Long id;
```
- 엔티티별 시퀀스를 사용하거나 `allocationSize`를 조정해야 하는 경우에만 적용

### 일반 컬럼
```java
@Column(name = "컬럼명", [속성들])
private 타입 필드명;
```
**속성 원칙**:
- `name`: snake_case
- `nullable`: 필수 여부 명시
- `unique`: 유니크 제약
- `length`: 문자열 최대 길이

### Enum
```java
@Enumerated(EnumType.STRING)
@Column(name = "컬럼명", nullable = false, length = 20)
private EnumType 필드명;
```
- **STRING 타입 사용** (순서 변경에 안전)
- **길이 제한**: 기본 20

### 시간 필드
```java
@Column(name = "컬럼명")
private Instant 필드명;
```
- **Instant 사용** (LocalDateTime 대신)
- BaseEntity 상속 시 createdAt/updatedAt 자동 관리
- **이유**: 타임존 이슈 없이 UTC 기준 저장, 서버/클라이언트 환경 차이로 인한 시간 오류 방지

## 3. 팩토리 메서드 패턴

```java
public static Entity create(필수파라미터들) {
    validateCreate(파라미터들);
    return Entity.builder()
            .필드1(값1)
            .필드2(값2)
            .기본값필드(DEFAULT_VALUE)
            .build();
}

private static void validateCreate(파라미터들) {
    Assert.hasText(필수문자열, "필드명은 필수입니다");
    if (문자열.length() > 제한) {
        throw new IllegalArgumentException("필드명 too long");
    }
}
```

## 4. 비즈니스 메서드

### 변경 메서드
```java
public void change필드명(타입 새값) {
    Assert.hasText(새값, "필드명은 필수입니다");
    if (새값.length() > 제한) {
        throw new IllegalArgumentException("필드명 too long");
    }
    this.필드명 = 새값;
}
```

### 상태 변경 메서드
```java
public void 동사() {
    this.status = NewStatus;
}
```

### 도메인 로직 메서드
```java
public void 비즈니스동작() {
    // 비즈니스 로직 수행
    this.관련필드 = Instant.now();
}
```

### 조회 메서드
```java
public boolean is상태() {
    return this.status == ExpectedStatus;
}
```

## 5. 검증 규칙

### 필수값 검증
```java
Assert.hasText(값, "필드명은 필수입니다");
```

### 길이 검증
```java
if (값.length() > 제한) {
    throw new IllegalArgumentException("필드명 too long");
}
```

### null 허용 검증
```java
if (값 != null && 값.length() > 제한) {
    throw new IllegalArgumentException("필드명 too long");
}
```

## 6. 코드 구조 순서

1. 필드 선언 (ID → 일반 컬럼 → Enum → 시간)
2. 팩토리 메서드 (`create`)
3. 비즈니스 메서드 (public)
4. 검증 메서드 (private static)

## 7. 주요 원칙

- **캡슐화**: 모든 변경은 비즈니스 메서드를 통해서만
- **불변성**: Setter 사용 금지
- **생성 통제**: 팩토리 메서드 + protected 생성자
- **검증 집중화**: 생성/변경 시점에 즉시 검증
- **명확한 네이밍**: 의도가 드러나는 메서드명
- **단일 책임**: 한 메서드는 한 가지 책임만

## 8. Member 엔티티 적용 예시

### 필드
- ID: `Long id` (SEQUENCE)
- 문자열: email(unique), password(20), nickname(10), profileImageUrl(500)
- Enum: `MemberStatus status` (ACTIVE/INACTIVE/SUSPENDED/WITHDRAWN)
- 시간: `Instant lastLoginAt`

### 메서드
- 생성: `create(email, password, nickname)` → status=ACTIVE 기본값
- 변경: `changeNickname`, `changeProfileImage`, `changePassword`
- 상태: `deactivate()`, `withdraw()`
- 로직: `loginSuccess()` → lastLoginAt 갱신
- 조회: `isActive()`
