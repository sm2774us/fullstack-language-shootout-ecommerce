# syntax=docker/dockerfile:1
FROM gradle:8.11-jdk21 AS builder
WORKDIR /app
COPY . .
RUN gradle build -x test --no-daemon

FROM eclipse-temurin:21-jre
COPY --from=builder /app/build/libs/*.jar /app.jar
EXPOSE 8080
CMD ["java", "-jar", "/app.jar"]
