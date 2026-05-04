# Backend Setup and Testing Guide

## Overview

This guide explains how to set up, run, and test the Spring Boot backend for the real-time chat system.

## Prerequisites

### Required Software

1. **Java 17 or higher**
   - Check version: `java -version`
   - Download: [OpenJDK](https://adoptium.net/) or [Oracle JDK](https://www.oracle.com/java/technologies/downloads/)

2. **Maven 3.6+**
   - Check version: `mvn -version`
   - Download: [Apache Maven](https://maven.apache.org/download.cgi)
   - Or use the included Maven wrapper: `./mvnw` (Linux/Mac) or `mvnw.cmd` (Windows)

3. **PostgreSQL 12+**
   - Check version: `psql --version`
   - Download: [PostgreSQL](https://www.postgresql.org/download/)

## Database Setup

### Step 1: Install PostgreSQL

If you don't have PostgreSQL installed:

**Windows:**
```bash
# Download installer from https://www.postgresql.org/download/windows/
# Or use Chocolatey:
choco install postgresql
```

**Mac:**
```bash
brew install postgresql@15
brew services start postgresql@15
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
sudo systemctl start postgresql
```

### Step 2: Create Database and User

Connect to PostgreSQL as the postgres user:

```bash
# Windows/Linux
psql -U postgres

# Mac (if installed via Homebrew)
psql postgres
```

Then run these SQL commands:

```sql
-- Create database
CREATE DATABASE chatdb;

-- Create user
CREATE USER chatuser WITH PASSWORD 'chatpass';

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE chatdb TO chatuser;

-- Connect to the database
\c chatdb

-- Grant schema privileges (PostgreSQL 15+)
GRANT ALL ON SCHEMA public TO chatuser;

-- Exit
\q
```

### Step 3: Verify Database Connection

Test the connection:

```bash
psql -U chatuser -d chatdb -h localhost
```

If successful, you'll see the PostgreSQL prompt. Type `\q` to exit.

## Backend Configuration

### Environment Variables (Optional)

You can override default settings using environment variables:

**Windows (PowerShell):**
```powershell
$env:DB_USERNAME="chatuser"
$env:DB_PASSWORD="chatpass"
$env:JWT_SECRET="your-super-secret-jwt-key-minimum-256-bits-long"
$env:CORS_ALLOWED_ORIGINS="http://localhost:3000"
```

**Windows (CMD):**
```cmd
set DB_USERNAME=chatuser
set DB_PASSWORD=chatpass
set JWT_SECRET=your-super-secret-jwt-key-minimum-256-bits-long
set CORS_ALLOWED_ORIGINS=http://localhost:3000
```

**Mac/Linux:**
```bash
export DB_USERNAME=chatuser
export DB_PASSWORD=chatpass
export JWT_SECRET=your-super-secret-jwt-key-minimum-256-bits-long
export CORS_ALLOWED_ORIGINS=http://localhost:3000
```

### Configuration File

The backend uses `src/main/resources/application.yml` with these defaults:

- **Database URL**: `jdbc:postgresql://localhost:5432/chatdb`
- **Database User**: `chatuser` (or `$DB_USERNAME`)
- **Database Password**: `chatpass` (or `$DB_PASSWORD`)
- **Server Port**: `8080`
- **JWT Secret**: Set via `$JWT_SECRET` (change in production!)
- **CORS Origins**: `http://localhost:3000`

## Running the Backend

### Option 1: Using Maven (Recommended)

**Build and run:**
```bash
# Clean and build
mvn clean install

# Run the application
mvn spring-boot:run
```

**Run with a specific profile:**
```bash
# Development profile (more logging, auto-creates tables)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Production profile (less logging, validates schema)
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### Option 2: Using Maven Wrapper

If Maven is not installed, use the included wrapper:

**Windows:**
```cmd
mvnw.cmd clean install
mvnw.cmd spring-boot:run
```

**Mac/Linux:**
```bash
./mvnw clean install
./mvnw spring-boot:run
```

### Option 3: Running the JAR

**Build the JAR:**
```bash
mvn clean package
```

**Run the JAR:**
```bash
java -jar target/first-java-proj-1.0-SNAPSHOT.jar
```

**Run with profile:**
```bash
java -jar target/first-java-proj-1.0-SNAPSHOT.jar --spring.profiles.active=dev
```

### Option 4: Using IntelliJ IDEA

1. Open the project in IntelliJ IDEA
2. Wait for Maven to import dependencies
3. Find `ChatApplication.java` in `src/main/java/org/example/chat/`
4. Right-click and select "Run 'ChatApplication'"
5. Or click the green play button in the gutter

**To run with a profile in IntelliJ:**
1. Edit Run Configuration
2. Add VM options: `-Dspring.profiles.active=dev`
3. Or add Environment variables: `SPRING_PROFILES_ACTIVE=dev`

## Verifying the Backend is Running

### Check the Console Output

You should see output like:

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.0)

2026-04-30 12:00:00.000  INFO 12345 --- [main] o.e.chat.ChatApplication : Starting ChatApplication
2026-04-30 12:00:01.000  INFO 12345 --- [main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8080 (http)
2026-04-30 12:00:01.000  INFO 12345 --- [main] o.e.chat.ChatApplication : Started ChatApplication in 2.5 seconds
```

### Test the Health Endpoint

Open a browser or use curl:

```bash
curl http://localhost:8080/actuator/health
```

Or just visit: http://localhost:8080

## Testing the Backend

### Running Unit Tests

**Run all tests:**
```bash
mvn test
```

**Run specific test class:**
```bash
mvn test -Dtest=AuthenticationServiceTest
```

**Run tests with coverage:**
```bash
mvn clean test jacoco:report
```

### Testing REST API Endpoints

#### 1. Register a User

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123",
    "displayName": "Test User"
  }'
```

**Expected Response:**
```json
{
  "id": 1,
  "username": "testuser",
  "email": "test@example.com",
  "displayName": "Test User",
  "createdAt": "2026-04-30T12:00:00",
  "lastSeen": null,
  "online": false
}
```

#### 2. Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

**Expected Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "username": "testuser",
    "email": "test@example.com",
    "displayName": "Test User",
    "createdAt": "2026-04-30T12:00:00",
    "lastSeen": "2026-04-30T12:05:00",
    "online": true
  }
}
```

**Save the token for subsequent requests!**

#### 3. Create a Chat Room

```bash
# Replace YOUR_JWT_TOKEN with the token from login
curl -X POST http://localhost:8080/api/rooms \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "name": "General Chat",
    "description": "A general chat room for everyone"
  }'
```

**Expected Response:**
```json
{
  "id": 1,
  "name": "General Chat",
  "description": "A general chat room for everyone",
  "createdAt": "2026-04-30T12:10:00",
  "createdBy": {
    "id": 1,
    "username": "testuser",
    "displayName": "Test User"
  }
}
```

#### 4. List All Rooms

```bash
curl -X GET http://localhost:8080/api/rooms \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### 5. Get Room Details

```bash
curl -X GET http://localhost:8080/api/rooms/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### 6. Get Message History

```bash
# Get first page (20 messages)
curl -X GET "http://localhost:8080/api/rooms/1/messages?page=0&size=20" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Testing WebSocket/STOMP Connection

You can test WebSocket connections using a STOMP client or the frontend once it's built.

**Using a browser-based STOMP client:**

1. Install a browser extension like "Simple WebSocket Client"
2. Connect to: `ws://localhost:8080/ws`
3. Send STOMP CONNECT frame with JWT token in headers

**Or wait for the frontend to be built (Tasks 21-36)**

## Using Postman or Insomnia

### Import Collection

Create a new collection with these requests:

1. **Register User** - POST `http://localhost:8080/api/auth/register`
2. **Login** - POST `http://localhost:8080/api/auth/login`
3. **Get Current User** - GET `http://localhost:8080/api/users/me`
4. **Create Room** - POST `http://localhost:8080/api/rooms`
5. **List Rooms** - GET `http://localhost:8080/api/rooms`
6. **Get Room Details** - GET `http://localhost:8080/api/rooms/{id}`
7. **Get Message History** - GET `http://localhost:8080/api/rooms/{roomId}/messages`

### Setting Up Authorization

After login, copy the JWT token and add it to subsequent requests:

**Header:**
```
Authorization: Bearer YOUR_JWT_TOKEN
```

## Troubleshooting

### Database Connection Issues

**Error:** `Connection refused` or `FATAL: password authentication failed`

**Solutions:**
1. Verify PostgreSQL is running: `pg_isready`
2. Check database exists: `psql -U postgres -c "\l"`
3. Verify user credentials in `application.yml`
4. Check PostgreSQL is listening on port 5432: `netstat -an | grep 5432`

### Port Already in Use

**Error:** `Port 8080 is already in use`

**Solutions:**
1. Stop the process using port 8080
2. Change the port in `application.yml`: `server.port: 8081`
3. Or set environment variable: `SERVER_PORT=8081`

### JWT Token Issues

**Error:** `Invalid JWT token` or `JWT expired`

**Solutions:**
1. Ensure you're using a fresh token from login
2. Check JWT secret is set correctly
3. Token expires after 24 hours - login again

### Build Failures

**Error:** Maven build fails

**Solutions:**
1. Clean Maven cache: `mvn clean`
2. Update dependencies: `mvn dependency:resolve`
3. Check Java version: `java -version` (must be 17+)
4. Delete `.m2/repository` and rebuild

### Database Schema Issues

**Error:** `Table doesn't exist` or `Column not found`

**Solutions:**
1. Drop and recreate database:
   ```sql
   DROP DATABASE chatdb;
   CREATE DATABASE chatdb;
   ```
2. Run with dev profile to auto-create tables:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

## Logs

### Log Files

Logs are written to:
- **Console**: Real-time output
- **File**: `logs/chat-application.log`

### Log Levels

**Development:**
- Application: `DEBUG`
- SQL queries: `DEBUG`
- WebSocket: `DEBUG`

**Production:**
- Application: `INFO`
- SQL queries: `WARN`
- WebSocket: `INFO`

### Viewing Logs

**Tail the log file:**
```bash
# Windows (PowerShell)
Get-Content logs/chat-application.log -Wait -Tail 50

# Mac/Linux
tail -f logs/chat-application.log
```

## Next Steps

Once the backend is running:

1. **Test the REST API** using curl or Postman
2. **Run the frontend** (see `frontend/SETUP.md`)
3. **Test end-to-end** with both backend and frontend running
4. **Monitor logs** for any errors or issues

## Production Deployment

For production deployment:

1. Set strong JWT secret: `JWT_SECRET=your-production-secret-key`
2. Use production database credentials
3. Enable HTTPS/TLS
4. Configure proper CORS origins
5. Set up monitoring and alerting
6. Use production profile: `--spring.profiles.active=prod`
7. Consider using Docker for containerization

## Resources

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring WebSocket Documentation](https://docs.spring.io/spring-framework/reference/web/websocket.html)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [JWT.io](https://jwt.io/) - JWT token debugger

## Support

If you encounter issues:

1. Check the logs in `logs/chat-application.log`
2. Verify database connection
3. Ensure all prerequisites are installed
4. Review the troubleshooting section above
