# syntax=docker/dockerfile:1
FROM sbtscala/scala-sbt:eclipse-temurin-21.0.5_11_1.10.5_3.5.2 AS builder
WORKDIR /app
COPY . .
RUN sbt assembly || sbt stage

FROM eclipse-temurin:21-jre
COPY --from=builder /app/target/scala-3.5.2 /app
WORKDIR /app
EXPOSE 8080
CMD ["sh", "-c", "java -cp '*' com.example.webapi.Main"]
