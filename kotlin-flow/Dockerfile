# Multi-stage build for Kotlin Flow application
FROM gradle:8.10.2-jdk21-alpine AS builder

WORKDIR /app

# Copy gradle wrapper files first
COPY kotlin-flow/gradlew kotlin-flow/gradlew.bat ./
COPY kotlin-flow/gradle/ gradle/
COPY kotlin-flow/build.gradle.kts kotlin-flow/settings.gradle.kts ./

# Make gradlew executable
RUN chmod +x gradlew

# Copy source code
COPY kotlin-flow/src/ src/

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
EXPOSE 8082

# Environment variables
ENV PORT=8082
ENV MAX_COUNT=200000
ENV BATCH_CONCURRENCY=4
ENV ITEM_CONCURRENCY=128
ENV DOWNSTREAM_TIMEOUT_MS=2000
ENV RETRY_ATTEMPTS=1
ENV LOG_LEVEL=INFO

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8082/healthz || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
