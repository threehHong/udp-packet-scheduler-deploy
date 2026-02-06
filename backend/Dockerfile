# 1. Build stage (Gradle 8.14+ 필요)
FROM gradle:8.14.0-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle clean build -x test

# 2. Run stage
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
