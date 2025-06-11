# 1단계: 빌드 이미지
FROM gradle:8.5-jdk21 AS builder

WORKDIR /app
COPY . .
RUN ./gradlew clean build -x test

# 2단계: 실행 이미지
FROM eclipse-temurin:21-jdk

WORKDIR /app

# 빌드된 JAR 복사 (단일 JAR 가정)
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]