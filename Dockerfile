# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml ./
COPY src ./src

RUN mvn -DskipTests=true package

# Runtime stage
# Pin to a stable base OS to avoid inheriting new distro packages/vulns from
# floating tags (e.g. ubuntu 26.04 currently ships a vulnerable `pebble` binary).
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /app/target/first-java-proj-1.0-SNAPSHOT.jar ./app.jar
COPY start.sh ./start.sh

# Make start script executable
RUN chmod +x /app/start.sh

EXPOSE 8080

ENTRYPOINT ["/app/start.sh"]
