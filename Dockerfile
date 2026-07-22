# --- Build stage -----------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies separately from source for faster rebuilds.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# --- Runtime stage ---------------------------------------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Run as a non-root user.
RUN useradd --system --uid 1001 appuser
USER appuser

COPY --from=build /app/target/url-shortener-api-1.0.0.jar app.jar

EXPOSE 8080

# Cap the heap relative to the container's memory limit so the JVM stays within
# small free-tier instances (e.g. 512 MB) instead of being OOM-killed.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
