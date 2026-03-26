@echo off
REM ================================================
REM Script chạy Backend trên Windows (không qua WSL)
REM ================================================

echo ========================================
echo    MIRROR PRODUCTS SERVICE - WINDOWS
echo ========================================
echo.

REM Set JAVA_HOME
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot

REM Set AWS credentials
set AWS_ACCESS_KEY_ID=AKIARFXSXG7JP3ODYOBM
set AWS_SECRET_ACCESS_KEY=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
set AWS_REGION=ap-southeast-1

REM Set database config
set USE_SECRETS_MANAGER=true
set RDS_SECRET_NAME=rds!db-3c41e561-738d-4ff2-8e5f-ed6649af51b1

echo [INFO] JAVA_HOME=%JAVA_HOME%
echo [INFO] AWS credentials configured
echo [INFO] Starting Spring Boot...
echo.

REM Run with Maven Wrapper
mvnw.cmd spring-boot:run

pause
