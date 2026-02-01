# JPA Auditing Bean 중복 등록 문제

## 증상

`@ServiceIntegrationTest` 사용 시 다음 에러 발생:

```
BeanDefinitionOverrideException: Invalid bean definition with name 'jpaAuditingHandler' defined in null:
Cannot register bean definition [...] for bean 'jpaAuditingHandler' since there is already [...] bound.
```

## 원인 분석

### 설정 파일 구조

**메인 설정** (`JpaAuditingConfig.java`):
```java
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {}
```

**테스트 설정** (`JpaAuditingTestConfig.java`):
```java
@TestConfiguration
@EnableJpaAuditing(dateTimeProviderRef = "testDateTimeProvider")
public class JpaAuditingTestConfig {
    @Bean
    public DateTimeProvider testDateTimeProvider() {
        return () -> Optional.of(FIXED_TEST_TIME);
    }
}
```

**테스트 어노테이션** (`ServiceIntegrationTest.java`):
```java
@Import({JpaAuditingTestConfig.class, TestStorageConfiguration.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public @interface ServiceIntegrationTest {}
```

### 근본 원인

1. `@SpringBootTest`가 전체 애플리케이션 컨텍스트 로드
2. 메인 `JpaAuditingConfig`의 `@EnableJpaAuditing`이 `jpaAuditingHandler` 빈 등록
3. `@Import`로 `JpaAuditingTestConfig`도 로드
4. 테스트 설정의 `@EnableJpaAuditing`이 동일한 `jpaAuditingHandler` 빈 등록 시도
5. Spring Boot 2.1+에서 빈 오버라이딩 기본 비활성화로 충돌 발생

### @EnableJpaAuditing이 등록하는 빈들

- `jpaAuditingHandler` - 감사 처리 핸들러
- `jpaMappingContext` - JPA 매핑 컨텍스트
- `auditingEntityListener` - 엔티티 리스너

## 해결 방법

### 방법 1: 테스트용 JpaAuditingConfig Import 제거 (채택)

```java
@Import({TestStorageConfiguration.class, TestcontainersConfiguration.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public @interface ServiceIntegrationTest {}
```

메인 `JpaAuditingConfig`를 그대로 사용. 테스트에서 고정 시간이 필요 없다면 가장 단순한 해결책.

### 방법 2: 메인 설정에서 테스트 프로파일 제외

```java
@Configuration
@EnableJpaAuditing
@Profile("!test")  // test 프로파일에서 제외
public class JpaAuditingConfig {}
```

테스트용 설정만 로드되도록 함.

### 방법 3: 빈 오버라이딩 허용

`application-test.yml`:
```yaml
spring:
  main:
    allow-bean-definition-overriding: true
```

권장하지 않음 - 다른 빈 충돌 문제 숨길 수 있음.

### 방법 4: 조건부 테스트 설정

```java
@TestConfiguration
@ConditionalOnMissingBean(AuditingHandler.class)
@EnableJpaAuditing(dateTimeProviderRef = "testDateTimeProvider")
public class JpaAuditingTestConfig {}
```

## 검증 방법

```java
@ServiceIntegrationTest
@Transactional
class JpaAuditingConflictTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
        // 컨텍스트 로드 성공 = 충돌 없음
        assertThat(context).isNotNull();
    }

    @Test
    void jpaAuditingHandlerExists() {
        assertThat(context.containsBean("jpaAuditingHandler")).isTrue();
    }
}
```

## 교훈

1. `@EnableJpaAuditing`은 한 번만 선언해야 함
2. `@SpringBootTest`는 메인 설정을 모두 로드함
3. 테스트 설정 추가 시 메인 설정과 중복 여부 확인 필요
4. 테스트용 DateTimeProvider가 필요하면 별도 빈으로 등록하고 메인 설정에서 참조
