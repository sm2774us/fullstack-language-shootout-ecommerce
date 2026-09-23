# syntax=docker/dockerfile:1
FROM gradle:8.11-jdk21 AS builder
WORKDIR /app
COPY . .
RUN gradle build -x test --no-daemon

FROM eclipse-temurin:21-jre
# The Ktor Gradle plugin auto-applies Shadow packaging, producing TWO jars
# in build/libs/: the plain `jar` task output (no dependencies bundled, no
# Main-Class manifest entry — not runnable on its own) and a `*-all.jar`
# fat jar (dependencies bundled, Main-Class set — the one that actually
# runs). `*.jar` alone is ambiguous between the two; `*-all.jar` picks the
# right one specifically.
COPY --from=builder /app/build/libs/*-all.jar /app.jar
EXPOSE 8080
CMD ["java", "-jar", "/app.jar"]
