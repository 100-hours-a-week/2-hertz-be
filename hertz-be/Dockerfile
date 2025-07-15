# 1단계: 빌드 이미지
FROM gradle:8.5-jdk21 AS builder

WORKDIR /app
COPY . .
RUN ./gradlew clean build -x test

# 2단계: 실행 이미지
FROM eclipse-temurin:21-jdk

WORKDIR /app

# OpenTelemetry Java Agent 다운로드
RUN curl -L -o opentelemetry-javaagent.jar \
  https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar

# 빌드된 JAR 복사 (단일 JAR 가정)
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-Dloader.path=/app/resources","-javaagent:opentelemetry-javaagent.jar","-jar","app.jar"]

#ENTRYPOINT ["java", "-jar", "app.jar"]