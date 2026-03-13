FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built JAR file
    COPY librelog-api/target/librelog-api-0.1.4.jar app.jar

# Expose the application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

