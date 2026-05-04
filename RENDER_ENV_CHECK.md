# 🔍 Render Environment Variables Checklist

## Critical: You MUST Set These in Render

Go to: **Render Dashboard → Your Web Service → Environment Tab**

### Required Environment Variables

Copy this checklist and verify each one:

```
□ SPRING_PROFILES_ACTIVE
  Value: prod
  
□ DATABASE_URL
  Value: postgresql://chatuser:XXXXX@dpg-XXXXX-a/chatdb_XXXXX
  (Copy from your PostgreSQL database's "Internal Database URL")
  
□ JWT_SECRET
  Value: (any random string, or let Render generate)
  Example: openssl rand -base64 32
  
□ PORT
  Value: 8080
```

### Optional (Can Set Later)

```
□ CORS_ALLOWED_ORIGINS
  Value: * (for testing) or https://your-frontend.onrender.com
```

## How to Get DATABASE_URL

### Step-by-Step:

1. In Render Dashboard, click **"PostgreSQL"** in the left sidebar
2. Click on your database (e.g., `chat-db`)
3. Look for **"Internal Database URL"** section
4. Click the **copy icon** next to the URL
5. It should look like:
   ```
   postgresql://chatuser:abc123xyz@dpg-d7s93d77f7vs73dgq990-a/chatdb_56sp
   ```

### Important Notes:

- ✅ Use **Internal** URL (shorter, no `.render.com`)
- ❌ Don't use **External** URL (longer, has `.render.com`)
- ✅ Should start with `postgresql://` (NOT `jdbc:postgresql://`)
- ✅ Should NOT have a port number (like `:5432`)

## How to Set Environment Variables

### In Render Dashboard:

1. Go to your **web service** (not database)
2. Click **"Environment"** in the left sidebar
3. For each variable:
   - Click **"Add Environment Variable"** (or edit existing)
   - Enter **Key** (e.g., `DATABASE_URL`)
   - Enter **Value** (paste the URL)
   - Click **"Save Changes"**

### After Saving:

- Render will automatically trigger a redeploy
- Wait 5-7 minutes for the build and deployment
- Check the logs for success messages

## Verification

### In Render Logs (After Deploy):

Look for these messages:

```
✅ Good Signs:
🚀 Starting Chat Application...
✅ DATABASE_URL found, converting format...
✅ Converted to JDBC format
📊 Database host: dpg-xxxxx-a
🔧 Active profile: prod
🎯 Starting Spring Boot application...
HikariPool-1 - Starting...
HikariPool-1 - Start completed.
Started ChatApplication in X.XXX seconds

❌ Bad Signs:
⚠️  DATABASE_URL not set
Connection to localhost:5432 refused
Active profile: default (should be "prod")
```

### Test Endpoints:

```bash
# Health check
curl https://your-backend.onrender.com/actuator/health

# Should return:
{"status":"UP"}
```

## Common Mistakes

### ❌ Mistake 1: Using External Database URL

**Wrong:**
```
postgresql://chatuser:pass@dpg-xxxxx-a.oregon-postgres.render.com:5432/chatdb
```

**Right:**
```
postgresql://chatuser:pass@dpg-xxxxx-a/chatdb
```

### ❌ Mistake 2: Adding jdbc: prefix

**Wrong:**
```
jdbc:postgresql://chatuser:pass@dpg-xxxxx-a/chatdb
```

**Right:**
```
postgresql://chatuser:pass@dpg-xxxxx-a/chatdb
```

The startup script will add `jdbc:` automatically!

### ❌ Mistake 3: Setting in Database Instead of Web Service

**Wrong:** Setting `DATABASE_URL` in the PostgreSQL database settings

**Right:** Setting `DATABASE_URL` in the web service's Environment tab

### ❌ Mistake 4: Not Setting SPRING_PROFILES_ACTIVE

**Wrong:** Leaving it empty or set to `dev`

**Right:** Set to `prod`

## Screenshot Checklist

If you need help, take screenshots of:

1. **PostgreSQL Database → Info Tab**
   - Shows the Internal Database URL

2. **Web Service → Environment Tab**
   - Shows all environment variables

3. **Web Service → Logs Tab**
   - Shows the startup messages

## Quick Test

After setting environment variables, you can test immediately:

```bash
# Replace with your actual backend URL
BACKEND_URL="https://your-backend.onrender.com"

# Test health (should return {"status":"UP"})
curl $BACKEND_URL/actuator/health

# Test registration (should create user)
curl -X POST $BACKEND_URL/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"password123"}'

# Test login (should return JWT token)
curl -X POST $BACKEND_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}'
```

## Summary

**The #1 reason for "Connection to localhost:5432" error:**

→ `DATABASE_URL` is NOT set in the web service's environment variables!

**The fix:**

1. Get Internal Database URL from PostgreSQL service
2. Set it as `DATABASE_URL` in web service
3. Make sure `SPRING_PROFILES_ACTIVE=prod`
4. Save and wait for redeploy

---

**Double-check your Environment tab right now!** Is `DATABASE_URL` actually set with a value? 🔍
