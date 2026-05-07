FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY target/librelog.jar app.jar

EXPOSE 8080

# IPv4 loopback (127.0.0.1) is required - localhost resolves to ::1 in this image,
# but Spring Boot binds 0.0.0.0 which leaves IPv6 unanswered. Carried forward from v1.
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=5 \
  CMD wget --no-verbose --tries=1 --spider http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
