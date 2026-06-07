# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml ./
COPY src ./src

RUN mvn -DskipTests=true package

# Runtime stage
# Pin to a stable base OS to avoid inheriting new distro packages/vulns from
# floating tags (e.g. ubuntu 26.04 currently ships a vulnerable `pebble` binary).
FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app

COPY --from=build /app/target/first-java-proj-1.0-SNAPSHOT.jar ./app.jar
COPY start.sh ./start.sh

# Generate CDS archive to speed up class loading on cold start.
# Exclude DB auto-configuration since no database is available during the Docker build.
# This still captures JVM, Spring Boot, Tomcat, WebSocket, Security, and app classes.
RUN java -Dspring.context.exit=onRefresh -XX:ArchiveClassesAtExit=/app/app.jsa \
    -jar /app/app.jar --spring.profiles.active=prod \
    --spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration \
    || true

# Make start script executable
RUN chmod +x /app/start.sh

EXPOSE 8080

ENTRYPOINT ["/app/start.sh"]
