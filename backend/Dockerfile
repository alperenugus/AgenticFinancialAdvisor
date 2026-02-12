# Multi-stage build for Spring Boot backend
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom.xml first for better caching
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# Find the JAR file name
RUN find /app/target -name "*.jar" -not -name "*sources.jar" -not -name "*javadoc.jar" | head -1 > /app/jarfile.txt

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

# Install curl for health checks
RUN apk add --no-cache curl

WORKDIR /app

# Copy the JAR from build stage
COPY --from=build /app/target/financialadvisor-*.jar app.jar

# Expose port
EXPOSE 8080

# Health check - increased start period for Spring Boot startup (3 minutes)
# Railway also has its own health check configured in railway.toml
HEALTHCHECK --interval=30s --timeout=10s --start-period=180s --retries=5 \
  CMD curl -f http://localhost:8080/api/advisor/status || exit 1

# Run the application
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]

