@echo off
echo ========================================
echo Finance Portal Backend with OpenTelemetry
echo ========================================

echo.
echo [1/3] Building application...
call mvn clean package -DskipTests

if %ERROR LEVEL% NEQ 0 (
    echo Build failed!
    pause
    exit /b 1
)

echo.
echo [2/3] Checking OpenTelemetry Agent...
if not exist opentelemetry-javaagent.jar (
    echo ERROR: opentelemetry-javaagent.jar not found!
    echo Please download it first:
    echo curl -L -O https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar
    pause
    exit /b 1
)

echo.
echo [3/3] Starting application with OpenTelemetry...
echo.

java -javaagent:opentelemetry-javaagent.jar ^
     -Dotel.service.name=finance-portal-backend ^
     -Dotel.resource.attributes=service.version=1.0.0,deployment.environment=development ^
     -Dotel.exporter.otlp.endpoint=http://localhost:4318 ^
     -Dotel.exporter.otlp.protocol=http/protobuf ^
     -Dotel.traces.exporter=otlp ^
     -Dotel.metrics.exporter=otlp ^
     -Dotel.logs.exporter=otlp ^
     -Dotel.instrumentation.common.default-enabled=true ^
     -Dotel.instrumentation.spring-webmvc.enabled=true ^
     -Dotel.instrumentation.jdbc.enabled=true ^
     -Dotel.instrumentation.redis.enabled=true ^
     -Dotel.instrumentation.kafka.enabled=true ^
     -jar target\backend-0.0.1-SNAPSHOT.jar

pause