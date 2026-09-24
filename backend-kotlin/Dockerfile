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
# Explicit container-aware heap sizing instead of the JVM's default
# MaxRAMPercentage=25.0 — under sustained concurrent load (this project's
# k6 benchmark: 30 VUs for 60s) that default can leave too little heap
# headroom in a resource-constrained CI container, causing recurring GC
# pressure that shows up as a small but persistent request failure rate
# for the whole run, not just at startup. 75% leaves comfortable headroom
# below any container memory limit while giving the JVM real room to work.
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -XX:+UseG1GC -XX:MaxGCPauseMillis=100"
CMD ["java", "-jar", "/app.jar"]
