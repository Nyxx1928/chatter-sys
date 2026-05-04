# Backend Troubleshooting Guide

## Your Current Situation

Based on the logs, your backend **DID start successfully** but then stopped. Here's what happened:

```
✅ Tomcat started on port 8080 (http) with context path ''
✅ Started ChatApplication in 36.729 seconds
```

But the server is no longer running when you try to access it.

## Why Can't You Connect to localhost:8080?

### Reason 1: Server Stopped After Starting

The most common reason is that you pressed `Ctrl+C` or closed the terminal window where Maven was running.

**Solution:** Keep the Maven process running in the terminal.

### Reason 2: Accessing the Wrong Endpoint

Even if the server is running, accessing `http://localhost:8080/` directly will give you a **403 Forbidden** error because Spring Security protects all endpoints by default.

**Solution:** Access the API endpoints instead:
- `http://localhost:8080/api/auth/register` (POST)
- `http://localhost:8080/api/auth/login` (POST)
- `http://localhost:8080/api/rooms` (GET with auth)

## Step-by-Step: Start and Test the Backend

### Step 1: Start the Backend (Keep It Running!)

Open a terminal and run:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**IMPORTANT:** 
- **DO NOT close this terminal window**
- **DO NOT press Ctrl+C**
- Leave it running in the background

You should see output ending with:
```
Started ChatApplication in XX.XXX seconds
```

### Step 2: Open a NEW Terminal Window

Open a **second** terminal window (keep the first one running!) and run the test script:

```powershell
powershell -ExecutionPolicy Bypass -File test-backend.ps1
```

This will test all the API endpoints and confirm everything is working.

### Step 3: If Test Passes

If all tests pass, your backend is working! You can now:

1. **Test with curl** (in the second terminal):
   ```powershell
   # Register a user
   curl -Method POST -Uri "http://localhost:8080/api/auth/register" `
     -ContentType "application/json" `
     -Body '{"username":"alice","email":"alice@example.com","password":"password123","displayName":"Alice"}'
   ```

2. **Start the frontend** (in a third terminal):
   ```bash
   cd frontend
   npm run dev
   ```

3. **Access the application** at http://localhost:3000

## Common Issues and Solutions

### Issue 1: "Connection Refused" or "Unable to Connect"

**Cause:** Backend is not running.

**Solution:**
1. Check if Java process is running:
   ```powershell
   Get-Process -Name java
   ```
2. If not running, start it:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

### Issue 2: "403 Forbidden" When Accessing Root

**Cause:** This is **NORMAL**! Spring Security protects the root endpoint.

**Solution:** This means the server IS running! Access the API endpoints instead:
- `/api/auth/register` - Register a user
- `/api/auth/login` - Login
- `/api/rooms` - List rooms (requires authentication)

### Issue 3: "500 Internal Server Error" on Registration

**Cause:** Database connection issue.

**Solution:**
1. Check if PostgreSQL is running:
   ```powershell
   Get-Service -Name postgresql*
   ```
2. Test database connection:
   ```bash
   psql -U chatuser -d chatdb -h localhost
   ```
3. If connection fails, check:
   - PostgreSQL service is running
   - Database `chatdb` exists
   - User `chatuser` has access
   - Password is correct (`chatpass`)

### Issue 4: Port 8080 Already in Use

**Cause:** Another application is using port 8080.

**Solution:**
1. Find what's using port 8080:
   ```powershell
   netstat -ano | findstr :8080
   ```
2. Kill the process or change the port in `application.yml`:
   ```yaml
   server:
     port: 8081
   ```

### Issue 5: Maven Build Fails

**Cause:** Dependencies not downloaded or compilation errors.

**Solution:**
1. Clean and rebuild:
   ```bash
   mvn clean install
   ```
2. If still fails, delete Maven cache:
   ```bash
   rm -r ~/.m2/repository
   mvn clean install
   ```

## Verifying the Backend is Running

### Method 1: Check the Terminal

Look for this message in the terminal where you ran `mvn spring-boot:run`:

```
Started ChatApplication in XX.XXX seconds (process running for XX.XXX)
```

### Method 2: Check Java Process

```powershell
Get-Process -Name java
```

Should show a Java process running.

### Method 3: Test with curl

```powershell
curl http://localhost:8080
```

- **403 Forbidden** = Server is running ✅
- **Connection refused** = Server is NOT running ❌

### Method 4: Run the Test Script

```powershell
powershell -ExecutionPolicy Bypass -File test-backend.ps1
```

Should pass all 5 tests.

## Understanding the 403 Forbidden Error

When you see **403 Forbidden** at `http://localhost:8080`, this is **GOOD NEWS**! It means:

1. ✅ The server is running
2. ✅ Spring Boot is working
3. ✅ Spring Security is protecting endpoints

The root endpoint (`/`) is protected by Spring Security. You need to:
- Use the API endpoints (`/api/auth/register`, `/api/auth/login`, etc.)
- Or access the frontend at `http://localhost:3000` (once it's running)

## Quick Test Commands

### Test 1: Check if Server is Running

```powershell
curl http://localhost:8080
```

**Expected:** 403 Forbidden (this is good!)

### Test 2: Register a User

```powershell
$body = @{
    username = "testuser"
    email = "test@example.com"
    password = "password123"
    displayName = "Test User"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" `
  -Method Post `
  -ContentType "application/json" `
  -Body $body
```

**Expected:** User object with ID

### Test 3: Login

```powershell
$loginBody = @{
    username = "testuser"
    password = "password123"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" `
  -Method Post `
  -ContentType "application/json" `
  -Body $loginBody

$token = $response.token
Write-Host "Token: $token"
```

**Expected:** JWT token

## Logs Location

Check the logs for detailed error messages:

```
logs/chat-application.log
```

View the last 50 lines:

```powershell
Get-Content logs/chat-application.log -Tail 50
```

## Still Having Issues?

1. **Check the logs:** `logs/chat-application.log`
2. **Verify database:** `psql -U chatuser -d chatdb`
3. **Check Java version:** `java -version` (must be 17+)
4. **Check Maven version:** `mvn -version`
5. **Restart PostgreSQL:** 
   ```powershell
   Restart-Service postgresql*
   ```

## Summary: How to Run the Backend

1. **Start PostgreSQL** (if not running)
2. **Open terminal** and run:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```
3. **Keep the terminal open** (don't close it!)
4. **Open a NEW terminal** and test:
   ```powershell
   powershell -ExecutionPolicy Bypass -File test-backend.ps1
   ```
5. **If tests pass**, backend is working!
6. **Access API** at `http://localhost:8080/api/...`
7. **Or start frontend** and access at `http://localhost:3000`

## Next Steps

Once the backend is running and tests pass:

1. ✅ Backend is working
2. 📝 Test API endpoints (see `API_TESTING.md`)
3. 🚀 Start the frontend (`cd frontend && npm run dev`)
4. 🌐 Access the application at `http://localhost:3000`
