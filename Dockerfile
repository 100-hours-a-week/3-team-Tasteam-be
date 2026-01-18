# ===============================
# 1. Build stage
# ===============================
FROM amazoncorretto:21 AS builder

WORKDIR /app

# Gradle wrapper & 캐시 레이어
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

RUN ./gradlew dependencies --no-daemon || true

# 소스 복사
COPY . .

# app-api 기준 bootJar
RUN ./gradlew bootJar --no-daemon

# ===============================
# 2. Runtime stage
# ===============================
FROM amazoncorretto:21

WORKDIR /app

# 빌드 결과 복사
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
