# Multi-stage build for production-ready Docker image
FROM eclipse-temurin:17-jdk-alpine AS builder

# Install Maven (Alpine uses apk)
RUN apk add --no-cache maven

# Set working directory
WORKDIR /app

# Copy POM first (for better layer caching)
COPY pom.xml .

# Download dependencies (cached layer if pom.xml unchanged)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (skip tests for faster builds)
RUN mvn clean package -DskipTests

# Production runtime stage  
FROM eclipse-temurin:17-jre-alpine

# Install only necessary packages for production (Alpine uses apk)
RUN apk --no-cache add curl ca-certificates

# Set working directory
WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Create non-root user for security (Alpine uses addgroup/adduser)
RUN addgroup -S mirrorproduct&& \
    adduser -S mirrorproduct -G mirrorproduct && \
    chown -R mirrorproduct:mirrorproduct /app

# Switch to non-root user
USER mirrorproduct

# Expose application port
EXPOSE 8082

# Health check for container orchestration
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8082/actuator/health || exit 1

# JVM tuning for container environment
ENV JAVA_OPTS="-server -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+UseStringDeduplication"

# Run the application with optimized JVM settings
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]