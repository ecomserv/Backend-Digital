# Build stage
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Create storage directory with proper permissions BEFORE switching user
RUN mkdir -p /app/cotizaciones && chown -R spring:spring /app/cotizaciones

# Copy the built JAR
COPY --from=build /app/target/*.jar app.jar
RUN chown spring:spring /app/app.jar

# Switch to non-root user
USER spring:spring

# Expose port (Render will set PORT env var)
EXPOSE 8080

# Health check for Render
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT:-8080}/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
