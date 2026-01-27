# SecurityConfig CGLIB + DevTools 클래스로더 충돌

## 에러

```
NoClassDefFoundError: com/tasteam/global/security/logout/filter/CustomLogoutFilter
```

Spring Boot 기동 시 `SecurityConfig`의 CGLIB 프록시 생성 과정에서 `CustomLogoutFilter` 클래스를 찾지 못함.

## 원인

`@Configuration`은 기본적으로 `proxyBeanMethods=true`이므로 CGLIB 프록시를 생성한다.
DevTools의 `RestartClassLoader`와 CGLIB의 클래스로더가 달라 필드 타입인 `CustomLogoutFilter`를 로딩하지 못함.

## 해결

```java
// before
@Configuration

// after
@Configuration(proxyBeanMethods = false)
```

`SecurityConfig`에 `@Bean` 간 상호 참조가 없으므로 CGLIB 프록시가 불필요하다.
