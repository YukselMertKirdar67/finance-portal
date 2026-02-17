# =======================
# Build stage
# =======================
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

# =======================
# Runtime stage
# =======================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

# OpenTelemetry Java Agent (TEK KAYNAK)
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.2.0/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]