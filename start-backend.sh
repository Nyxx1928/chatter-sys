#!/bin/bash

# Real-Time Chat System - Backend Startup Script
# This script helps you start the Spring Boot backend

echo "=========================================="
echo "Real-Time Chat System - Backend Startup"
echo "=========================================="
echo ""

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "❌ Error: Java is not installed or not in PATH"
    echo "Please install Java 17 or higher"
    echo "Download from: https://adoptium.net/"
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ Error: Java version must be 17 or higher"
    echo "Current version: $JAVA_VERSION"
    exit 1
fi

echo "✅ Java version: $(java -version 2>&1 | head -n 1)"
echo ""

# Check if PostgreSQL is running
if ! command -v psql &> /dev/null; then
    echo "⚠️  Warning: PostgreSQL client (psql) not found"
    echo "Make sure PostgreSQL is installed and running"
else
    echo "✅ PostgreSQL client found"
fi

echo ""
echo "Checking database connection..."
if psql -U chatuser -d chatdb -h localhost -c "SELECT 1" &> /dev/null; then
    echo "✅ Database connection successful"
else
    echo "❌ Cannot connect to database"
    echo ""
    echo "Please ensure:"
    echo "1. PostgreSQL is running"
    echo "2. Database 'chatdb' exists"
    echo "3. User 'chatuser' has access"
    echo ""
    echo "Run these commands in PostgreSQL:"
    echo "  CREATE DATABASE chatdb;"
    echo "  CREATE USER chatuser WITH PASSWORD 'chatpass';"
    echo "  GRANT ALL PRIVILEGES ON DATABASE chatdb TO chatuser;"
    echo ""
    read -p "Continue anyway? (y/n) " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

echo ""
echo "=========================================="
echo "Starting Spring Boot Backend..."
echo "=========================================="
echo ""
echo "Server will start on: http://localhost:8080"
echo "Press Ctrl+C to stop the server"
echo ""

# Check if Maven wrapper exists
if [ -f "./mvnw" ]; then
    echo "Using Maven wrapper..."
    ./mvnw spring-boot:run
else
    echo "Using system Maven..."
    mvn spring-boot:run
fi
