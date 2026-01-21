@echo off
SET "JAVA_HOME=C:\Users\maros\.jdks\temurin-17.0.17"
SET "APP_NAME=spring-boot-docker-app"
SET "PORT=8080"

SET "PATH=%JAVA_HOME%\bin;%PATH%"

echo [1/3] CLEANING AND BUILDING JAR...
call ./mvnw.cmd clean package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Maven build failed. Check your Java version or code.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo [2/3] BUILDING DOCKER IMAGE...
docker build -t %APP_NAME% .

echo.
echo [3/3] STARTING CONTAINER...
docker stop %APP_NAME% >nul 2>&1
docker rm %APP_NAME% >nul 2>&1

docker run -d -p %PORT%:%PORT% --name %APP_NAME% -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/testdb %APP_NAME%

echo.
echo =====================================================
echo SUCCESS! Your app is running at: http://localhost:%PORT%
echo =====================================================