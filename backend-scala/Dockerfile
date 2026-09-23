# syntax=docker/dockerfile:1
FROM sbtscala/scala-sbt:eclipse-temurin-21.0.5_11_1.10.5_3.5.2 AS builder
WORKDIR /app
COPY . .
RUN sbt assembly

FROM eclipse-temurin:21-jre
COPY --from=builder /app/target/scala-3.5.2/backend-scala.jar /app.jar
EXPOSE 8080
CMD ["java", "-jar", "/app.jar"]
