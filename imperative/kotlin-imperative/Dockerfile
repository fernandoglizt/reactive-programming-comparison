# Multi-stage build for Kotlin Imperative application
FROM gradle:8.10.2-jdk21-alpine AS builder

WORKDIR /app

# Copy gradle wrapper files first
COPY imperative/kotlin-imperative/gradlew imperative/kotlin-imperative/gradlew.bat ./
COPY imperative/kotlin-imperative/gradle/ gradle/
COPY imperative/kotlin-imperative/build.gradle.kts imperative/kotlin-imperative/settings.gradle.kts ./

# Make gradlew executable
RUN chmod +x gradlew

# Copy source code
COPY imperative/kotlin-imperative/src/ src/

# Build the application using gradlew wrapper
RUN ./gradlew clean build -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

# Install curl for health checks
RUN apk add --no-cache curl

WORKDIR /app

# Copy the built JAR
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose port
EXPOSE 8092

# Environment variables
ENV PORT=8092
ENV MAX_COUNT=200000
ENV CONCURRENCY=128
ENV DOWNSTREAM_TIMEOUT_MS=2000
ENV RETRY_ATTEMPTS=1
ENV LOG_LEVEL=INFO

# Health check
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8092/healthz || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

