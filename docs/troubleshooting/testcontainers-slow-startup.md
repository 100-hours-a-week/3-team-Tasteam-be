# Testcontainers 테스트 속도 개선

## 증상

`@ServiceIntegrationTest` 실행 시 테스트가 매우 느림 (30초 이상).

## 원인 분석

### Testcontainers 기본 동작

매 테스트 실행마다:
1. Docker 이미지 확인/pull
2. 컨테이너 생성 및 시작
3. 데이터베이스 초기화 대기
4. 테스트 실행
5. 컨테이너 종료 및 삭제

PostgreSQL + PostGIS 이미지는 약 500MB로 초기화에 10-20초 소요.

### 시간 소요 분석

| 단계 | 소요 시간 |
|------|----------|
| 컨테이너 시작 | 5-10초 |
| DB 초기화 대기 | 3-5초 |
| 스키마 생성 (Hibernate) | 2-5초 |
| 실제 테스트 | 1초 미만 |
| 컨테이너 정리 | 1-2초 |

## 해결 방법: 컨테이너 재사용

### 1. Testcontainers 전역 설정

`~/.testcontainers.properties`:
```properties
testcontainers.reuse.enable=true
```

### 2. 컨테이너 설정에 재사용 옵션 추가

```java
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:15-3.3")
                .asCompatibleSubstituteFor("postgres"))
            .withReuse(true);  // 컨테이너 재사용 활성화
    }
}
```

### 재사용 동작 방식

1. 첫 실행: 컨테이너 생성 후 종료하지 않고 유지
2. 이후 실행: 기존 컨테이너 재사용
3. 컨테이너 식별: 이미지명 + 설정 해시값으로 동일 컨테이너 매칭

### 속도 개선 효과

| 구분 | 재사용 전 | 재사용 후 |
|------|----------|----------|
| 첫 실행 | 30초+ | 30초+ |
| 이후 실행 | 30초+ | 5-10초 |

## 추가 최적화 방법

### 1. 싱글톤 컨테이너 패턴

```java
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:15-3.3")
                .asCompatibleSubstituteFor("postgres"))
            .withReuse(true);
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
```

### 2. 테스트 병렬 실행 시 컨테이너 공유

`junit-platform.properties`:
```properties
junit.jupiter.execution.parallel.enabled=true
junit.jupiter.execution.parallel.mode.default=concurrent
```

동일 컨테이너를 여러 테스트가 공유하므로 `@Transactional`로 데이터 격리 필수.

### 3. 경량 이미지 사용 (PostGIS 불필요 시)

```java
new PostgreSQLContainer<>("postgres:15-alpine")  // 150MB vs 500MB
```

### 4. 테스트 프로파일별 DB 분리

PostGIS가 필요한 테스트만 Testcontainers 사용, 나머지는 H2:

```yaml
# application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
```

```java
@ServiceIntegrationTest  // Testcontainers 사용
class SpatialQueryTest {}

@DataJpaTest  // H2 사용
class SimpleRepositoryTest {}
```

## 주의사항

### 재사용 컨테이너 수동 정리

```bash
# 재사용 컨테이너 목록 확인
docker ps -a --filter "label=org.testcontainers.reused=true"

# 수동 삭제
docker rm -f $(docker ps -aq --filter "label=org.testcontainers.reused=true")
```

### 스키마 변경 시

재사용 컨테이너는 이전 스키마를 유지. DDL 변경 시 컨테이너 재생성 필요:

```bash
docker rm -f $(docker ps -aq --filter "label=org.testcontainers.reused=true")
```

또는 `spring.jpa.hibernate.ddl-auto=create` 사용.

## 교훈

1. Testcontainers는 실제 DB 환경 테스트에 유용하나 느림
2. 재사용 설정으로 2회차부터 크게 개선
3. 단순 JPA 테스트는 H2로 충분
4. 테스트 성격에 맞는 DB 전략 선택 필요
