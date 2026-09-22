# syntax=docker/dockerfile:1
FROM eclipse-temurin:23-jdk AS builder
WORKDIR /app
COPY pom.xml ./
COPY src ./src
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
RUN mvn -q -DskipTests package

FROM eclipse-temurin:23-jre
COPY --from=builder /app/target/backend-java.jar /app.jar
EXPOSE 8080 50051
CMD ["java", "-jar", "/app.jar"]
