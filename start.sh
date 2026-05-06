#!/bin/bash

# Render Startup Script
# Converts DATABASE_URL from postgresql:// to jdbc:postgresql://
# Supports Neon (sslmode=require is preserved from the URL)

echo "🚀 Starting Chat Application..."

# Check if DATABASE_URL is set
if [ -n "$DATABASE_URL" ]; then
    echo "✅ DATABASE_URL found, converting format..."

    if [[ $DATABASE_URL == postgresql://* || $DATABASE_URL == postgres://* ]]; then
        raw_url="${DATABASE_URL#postgresql://}"
        raw_url="${raw_url#postgres://}"

        creds="${raw_url%@*}"
        host_and_path="${raw_url#*@}"

        db_host="${host_and_path%%/*}"
        db_path="${host_and_path#*/}"
        db_name="${db_path%%\?*}"
        db_params=""
        if [[ "$db_path" == *\?* ]]; then
            db_params="${db_path#*\?}"
        fi

        db_user="${creds%%:*}"
        db_pass=""
        if [[ "$creds" == *:* ]]; then
            db_pass="${creds#*:}"
        fi

        jdbc_url="jdbc:postgresql://${db_host}/${db_name}"
        jdbc_query=""
        if [ -n "$db_user" ]; then
            jdbc_query="user=${db_user}"
        fi
        if [ -n "$db_pass" ]; then
            jdbc_query="${jdbc_query:+${jdbc_query}&}password=${db_pass}"
        fi
        # Preserve any existing query params (e.g. sslmode=require from Neon)
        if [ -n "$db_params" ]; then
            jdbc_query="${jdbc_query:+${jdbc_query}&}${db_params}"
        fi
        if [ -n "$jdbc_query" ]; then
            jdbc_url="${jdbc_url}?${jdbc_query}"
        fi

        export JDBC_DATABASE_URL="$jdbc_url"
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
