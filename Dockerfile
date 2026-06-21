# Build with JDK 21 inside Docker so deploy hosts do not need Java 21 installed.
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -DskipTests -q package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/target/librelog.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=5 \
  CMD wget --no-verbose --tries=1 --spider http://127.0.0.1:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
