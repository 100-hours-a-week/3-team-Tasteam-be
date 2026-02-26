# ===============================
# 1. Build stage
# ===============================
FROM gradle:9.2.1-jdk21 AS builder

WORKDIR /app

# build.gradle의 git 명령 실행을 위해 git 설치
USER root
RUN apt-get update && apt-get install -y --no-install-recommends git \
    && rm -rf /var/lib/apt/lists/*

# Gradle wrapper & 캐시 레이어
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY app-api/build.gradle app-api/build.gradle

RUN ./gradlew dependencies --no-daemon || true

# 소스 복사
COPY . .

# app-api 기준 bootJar
RUN ./gradlew :app-api:bootJar --no-daemon

# ===============================
# 2. Runtime stage
# ===============================
FROM amazoncorretto:21

WORKDIR /app

# 빌드 결과 복사
COPY --from=builder /app/app-api/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
