#!/bin/bash

# Render Startup Script
# Converts DATABASE_URL from postgresql:// to jdbc:postgresql://

echo "🚀 Starting Chat Application..."

# Check if DATABASE_URL is set
if [ -n "$DATABASE_URL" ]; then
    echo "✅ DATABASE_URL found, converting format..."
    
    # Convert postgresql:// to jdbc:postgresql://
    if [[ $DATABASE_URL == postgresql://* ]]; then
        export JDBC_DATABASE_URL="jdbc:$DATABASE_URL"
        echo "✅ Converted to JDBC format"
    else
        export JDBC_DATABASE_URL="$DATABASE_URL"
        echo "⚠️  DATABASE_URL already in JDBC format"
    fi
    
    echo "📊 Database host: $(echo $DATABASE_URL | sed 's/.*@\([^/]*\).*/\1/')"
else
    echo "⚠️  DATABASE_URL not set, using default configuration"
fi

# Print active profile
echo "🔧 Active profile: ${SPRING_PROFILES_ACTIVE:-default}"

# Start the application
echo "🎯 Starting Spring Boot application..."
exec java -jar /app/app.jar
