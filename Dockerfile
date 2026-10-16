# Multi-stage build: compile with full Maven + JDK, then ship only the JRE + jar -
# keeps the final image small and avoids bundling the whole build toolchain.

# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy just the POM first so Docker can cache the downloaded dependencies as a
# separate layer - subsequent builds only re-download if pom.xml itself changes.
COPY pom.xml .
RUN mvn -q dependency:go-offline

COPY src ./src
RUN mvn -q clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/smartreply-ai-*.jar app.jar

# Render (and most PaaS hosts) inject PORT at runtime - Spring Boot reads it via
# server.port=${PORT:8080} in application.properties, so no extra config is needed here.
ENTRYPOINT ["java", "-jar", "app.jar"]
