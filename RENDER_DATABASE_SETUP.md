# Render Database Connection Setup

## The Problem

Your application is trying to connect to `localhost:5432` instead of the Render database. This means the `DATABASE_URL` environment variable is not set correctly.

## Quick Fix (5 minutes)

### Step 1: Get Database URL

1. Go to [Render Dashboard](https://dashboard.render.com)
2. Click on your **PostgreSQL database** (should be named `chat-db` or similar)
3. You'll see connection information
4. Find **"Internal Database URL"** (looks like this):
   ```
   postgresql://chatuser:XXXXX@dpg-XXXXX-a/chatdb_XXXXX
   ```
5. Click the **copy icon** to copy it

**Important:** Use the **Internal** URL, NOT the External URL!

### Step 2: Set DATABASE_URL in Web Service

1. Go back to Dashboard
2. Click on your **web service** (`chat-backend`)
3. Click the **"Environment"** tab on the left
4. Look for `DATABASE_URL`:
   
   **If it exists but is empty:**
   - Click the edit icon
   - Paste the Internal Database URL
   - Click "Save Changes"
   
   **If it doesn't exist:**
   - Click **"Add Environment Variable"**
   - Key: `DATABASE_URL`
   - Value: Paste the Internal Database URL
   - Click "Save Changes"

### Step 3: Verify All Environment Variables

Make sure you have these set:

| Variable | Value | Status |
|----------|-------|--------|
| `SPRING_PROFILES_ACTIVE` | `prod` | ✅ Should be set |
| `DATABASE_URL` | `postgresql://user:pass@host/db` | ⚠️ **SET THIS!** |
| `JWT_SECRET` | Any random string | ✅ Can auto-generate |
| `CORS_ALLOWED_ORIGINS` | Your frontend URL or `*` | ✅ Can update later |
| `PORT` | `8080` | ✅ Should be set |

### Step 4: Redeploy

After saving environment variables:
- Render will automatically trigger a redeploy
- Wait 3-5 minutes
- Check the logs

## Verify It's Working

### Check the Logs

In your web service logs, you should see:

**Before (WRONG):**
```
Connection to localhost:5432 refused
```

**After (CORRECT):**
```
HikariPool-1 - Starting...
HikariPool-1 - Start completed.
Started ChatApplication in X.XXX seconds
```

### Test the Health Endpoint

```bash
curl https://your-backend.onrender.com/actuator/health
```

**Expected response:**
```json
{"status":"UP"}
```

## Troubleshooting

### DATABASE_URL is set but still connecting to localhost

**Cause:** The `prod` profile might not be active.

**Solution:**
1. Check `SPRING_PROFILES_ACTIVE` is set to `prod`
2. Check the logs for: `The following profiles are active: prod`

### Can't find Internal Database URL

**Steps:**
1. Go to your PostgreSQL database in Render
2. Click the **"Info"** tab
3. Scroll down to **"Connections"**
4. Look for **"Internal Database URL"**
5. It should start with `postgresql://`

### Database doesn't exist

**If you created the database manually (not via Blueprint):**

The database might have a different name. Check:
1. In your PostgreSQL service, look at the **"Info"** tab
2. Note the actual database name
3. Make sure the `DATABASE_URL` includes the correct database name

### Still getting connection errors

**Check these:**
1. Database status is "Available" (not "Creating" or "Failed")
2. Database and web service are in the **same region**
3. You're using the **Internal** URL (not External)
4. The URL format is correct: `postgresql://user:password@host:port/database`

## Understanding the Connection

### How It Works

```
1. Render sets DATABASE_URL environment variable
   ↓
2. Spring Boot starts with prod profile
   ↓
3. RenderDatabaseConfig.java reads DATABASE_URL
   ↓
4. Converts postgresql:// to jdbc:postgresql://
   ↓
5. Creates DataSource with correct connection
   ↓
6. Application connects to Render database ✅
```

### What Was Wrong

```
1. DATABASE_URL not set or empty
   ↓
2. RenderDatabaseConfig.java has no URL to parse
   ↓
3. Falls back to application.yml defaults
   ↓
4. Tries to connect to localhost:5432 ❌
   ↓
5. Connection refused (no local database)
```

## Manual Database Creation (If Needed)

If you didn't use Blueprint or need to create database manually:

### Create Database

1. In Render Dashboard, click **"New +"**
2. Select **"PostgreSQL"**
3. Configure:
   - **Name:** `chat-db`
   - **Database:** `chatdb`
   - **User:** `chatuser`
   - **Region:** Same as your web service (e.g., Oregon)
   - **Plan:** Free
4. Click **"Create Database"**
5. Wait for it to become "Available"

### Link to Web Service

1. Once database is created, copy the **Internal Database URL**
2. Go to your web service
3. Add `DATABASE_URL` environment variable with the copied URL
4. Save and redeploy

## Testing Locally

To test the database connection logic locally:

```bash
# Set environment variables
export SPRING_PROFILES_ACTIVE=prod
export DATABASE_URL="postgresql://user:pass@host:5432/database"

# Run the application
mvn spring-boot:run
```

Or with Docker:

```bash
# Build
docker build -t chat-backend .

# Run with environment variables
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATABASE_URL="postgresql://user:pass@host:5432/database" \
  chat-backend
```

## Common Mistakes

### ❌ Using External Database URL
```
postgresql://user:pass@dpg-xxxxx-a.oregon-postgres.render.com:5432/db
```
**Problem:** External URL is for connections from outside Render (slower, may have issues)

### ✅ Using Internal Database URL
```
postgresql://user:pass@dpg-xxxxx-a/db
```
**Correct:** Internal URL is for connections within Render (faster, more reliable)

### ❌ Wrong Profile
```
SPRING_PROFILES_ACTIVE=dev
```
**Problem:** `RenderDatabaseConfig` only activates with `prod` profile

### ✅ Correct Profile
```
SPRING_PROFILES_ACTIVE=prod
```
**Correct:** Activates the Render database configuration

## Next Steps

After DATABASE_URL is set and deployment succeeds:

1. ✅ Verify health endpoint works
2. ✅ Test user registration
3. ✅ Test user login
4. ✅ Deploy frontend
5. ✅ Update CORS_ALLOWED_ORIGINS with frontend URL
6. ✅ Test end-to-end

## Quick Reference

### Get Database URL
```
Dashboard → PostgreSQL Database → Info → Internal Database URL
```

### Set Environment Variable
```
Dashboard → Web Service → Environment → Add/Edit DATABASE_URL
```

### Check Logs
```
Dashboard → Web Service → Logs → Search for "Started ChatApplication"
```

### Test Health
```bash
curl https://your-backend.onrender.com/actuator/health
```

---

**Once DATABASE_URL is set correctly, your deployment should succeed!** 🎉
