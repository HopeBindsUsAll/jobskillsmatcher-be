# syntax=docker/dockerfile:1.6
# Stage 1: build the Spring Boot fat JAR.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Resolve dependencies first to keep this layer cacheable.
COPY pom.xml pom.xml
RUN mvn -B -q -DskipTests dependency:go-offline

COPY src src
RUN mvn -B -q -DskipTests package \
    && cp target/*.jar /workspace/app.jar

# Stage 2: minimal runtime layer.
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

RUN addgroup --system --gid 1001 jsm \
    && adduser --system --uid 1001 --ingroup jsm jsm

COPY --from=build /workspace/app.jar /app/app.jar
USER jsm:jsm

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
