# 의존성 버전 선택 이유

이 문서는 `app-api/build.gradle`에 **고정(하드코딩)된 버전**을 선택한 이유를 설명합니다.

## Spring Boot

- `org.springframework.boot` 플러그인: `3.5.9`
  - 요청된 목표 버전이며, 모든 Spring Boot 스타터를 동일 버전으로 맞춰 정합성 확보.

## Spring Boot 3.5.9 BOM 기준 정렬

Spring Boot 3.5.9 BOM에 포함된 버전과 동일하게 맞춘 항목입니다.

- `org.postgresql:postgresql` = `42.7.8`
- `com.github.ben-manes.caffeine:caffeine` = `3.2.3`
- `org.projectlombok:lombok` = `1.18.42`
- `jakarta.annotation:jakarta.annotation-api` = `2.1.1`
- `jakarta.persistence:jakarta.persistence-api` = `3.1.0`
- `org.testcontainers:testcontainers-bom` = `1.21.4`
- `org.assertj:assertj-core` = `3.27.6` (`assertj-bom` 기준)
- `org.mockito:mockito-core` = `5.17.0` (`mockito-bom` 기준)
- `org.junit.platform:junit-platform-launcher` = `1.12.2` (`junit-bom` 기준)

## Spring Boot BOM 외 고정 버전

Spring Boot BOM에서 관리하지 않아, 현재 Maven Central 안정 릴리즈로 고정했습니다.

- `io.jsonwebtoken:jjwt-*` = `0.13.0`
- `com.querydsl:querydsl-*` = `5.1.0`
- `com.google.firebase:firebase-admin` = `9.7.0`
- `nl.martijndwars:web-push` = `5.1.2`
- `org.springdoc:springdoc-openapi-starter-webmvc-ui` = `2.8.15`
  - Spring Boot 3.x 호환을 위해 2.x 최신 라인 사용
- `net.logstash.logback:logstash-logback-encoder` = `9.0`
