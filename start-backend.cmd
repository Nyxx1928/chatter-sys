@echo off
REM Real-Time Chat System - Backend Startup Script (Windows)
REM This script helps you start the Spring Boot backend

echo ==========================================
echo Real-Time Chat System - Backend Startup
echo ==========================================
echo.

REM Check if Java is installed
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: Java is not installed or not in PATH
    echo Please install Java 17 or higher
    echo Download from: https://adoptium.net/
    pause
    exit /b 1
)

echo Java version:
java -version 2>&1 | findstr /C:"version"
echo.

REM Check if PostgreSQL is accessible
psql --version >nul 2>&1
if %errorlevel% neq 0 (
    echo Warning: PostgreSQL client (psql) not found
    echo Make sure PostgreSQL is installed and running
) else (
    echo PostgreSQL client found
)

echo.
echo Checking database connection...
psql -U chatuser -d chatdb -h localhost -c "SELECT 1" >nul 2>&1
if %errorlevel% neq 0 (
    echo Cannot connect to database
    echo.
    echo Please ensure:
    echo 1. PostgreSQL is running
    echo 2. Database 'chatdb' exists
    echo 3. User 'chatuser' has access
    echo.
    echo Run these commands in PostgreSQL:
    echo   CREATE DATABASE chatdb;
    echo   CREATE USER chatuser WITH PASSWORD 'chatpass';
    echo   GRANT ALL PRIVILEGES ON DATABASE chatdb TO chatuser;
    echo.
    set /p continue="Continue anyway? (y/n): "
    if /i not "%continue%"=="y" exit /b 1
) else (
    echo Database connection successful
)

echo.
echo ==========================================
echo Starting Spring Boot Backend...
echo ==========================================
echo.
echo Server will start on: http://localhost:8080
echo Press Ctrl+C to stop the server
echo.

REM Load environment variables from .env if present (for local dev)
REM This matches the repo's application.yml which reads MAIL_*, DB_*, JWT_SECRET, etc.
if exist ".env" (
    echo Loading environment from .env...
    setlocal EnableDelayedExpansion
    for /f "usebackq eol=# tokens=1,* delims==" %%A in (".env") do (
        set "k=%%A"
        set "v=%%B"
        if not "!k!"=="" (
            REM Trim wrapping quotes if present
            if "!v:~0,1!"=="\"" set "v=!v:~1!"
            if "!v:~-1!"=="\"" set "v=!v:~0,-1!"
            set "!k!=!v!"
        )
    )
)

REM Check if Maven wrapper exists
if exist "mvnw.cmd" (
    echo Using Maven wrapper...
    call mvnw.cmd spring-boot:run
) else (
    echo Using system Maven...
    call mvn spring-boot:run
)
