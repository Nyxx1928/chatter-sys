#!/bin/bash

# Render Startup Script
# - Converts DATABASE_URL from postgresql://... to jdbc:postgresql://...
# - Supports Neon (sslmode=require is preserved from the URL query string)

set -euo pipefail

echo "🚀 Starting Chat Application..."

if [ -z "${DATABASE_URL:-}" ]; then
  echo "❌ ERROR: DATABASE_URL is not set. Add it in the Render dashboard environment variables."
  echo "   Expected format: postgresql://user:pass@host/dbname?sslmode=require"
  exit 1
fi

# Guard against accidentally deploying with placeholder text from render.yaml/docs.
if echo "$DATABASE_URL" | grep -qiE '<paste your neon connection string here>|paste your neon connection string|your neon connection string'; then
  echo "❌ ERROR: DATABASE_URL is still set to a placeholder value."
  echo "   Set DATABASE_URL in Render to your real Neon connection string:"
  echo "   postgresql://user:pass@host/dbname?sslmode=require"
  exit 1
fi

echo "✅ DATABASE_URL found, converting format..."

if [[ "$DATABASE_URL" == postgresql://* || "$DATABASE_URL" == postgres://* ]]; then
  raw_url="${DATABASE_URL#postgresql://}"
  raw_url="${raw_url#postgres://}"

  creds="${raw_url%@*}"
  host_and_path="${raw_url#*@}"

  db_host="${host_and_path%%/*}"
  db_path="${host_and_path#*/}"
  db_name="${db_path%%\?*}"

  if [ -z "$db_host" ] || [ -z "$db_name" ] || [ "$host_and_path" = "$raw_url" ]; then
    echo "❌ ERROR: DATABASE_URL doesn't look like 'postgresql://user:pass@host/dbname?...'."
    echo "   Got: $DATABASE_URL"
    exit 1
  fi

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
elif [[ "$DATABASE_URL" == jdbc:postgresql://* ]]; then
  export JDBC_DATABASE_URL="$DATABASE_URL"
  echo "⚠️  DATABASE_URL already in JDBC format"
else
  echo "❌ ERROR: DATABASE_URL must start with 'postgresql://', 'postgres://', or 'jdbc:postgresql://'."
  echo "   Got: $DATABASE_URL"
  exit 1
fi

echo "📊 Database host: $(echo "$JDBC_DATABASE_URL" | sed -E 's#^jdbc:postgresql://([^/?]+).*#\\1#')"
echo "🔧 Active profile: ${SPRING_PROFILES_ACTIVE:-default}"

echo "🎯 Starting Spring Boot application..."
exec java -jar /app/app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod}
