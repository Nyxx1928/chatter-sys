# Test Data Cleanup Guide

## Quick Cleanup (Recommended)

I've created an admin endpoint to easily clean up test data.

### Step 1: Restart Your Application

```bash
# Stop current app (Ctrl+C)
mvn spring-boot:run
```

### Step 2: Check Current Status

```bash
curl http://localhost:8080/api/admin/test-data-status
```

**Response**:
```json
{
  "totalUsers": 3,
  "verifiedUsers": 0,
  "unverifiedUsers": 3,
  "pendingRegistrations": 1
}
```

### Step 3: Clean Up All Test Data

```bash
curl -X DELETE http://localhost:8080/api/admin/cleanup-test-data
```

**Response**:
```json
{
  "message": "Cleanup successful",
  "unverifiedUsersDeleted": 3,
  "pendingRegistrationsDeleted": 1
}
```

### Step 4: Verify Cleanup

```bash
curl http://localhost:8080/api/admin/test-data-status
```

**Response**:
```json
{
  "totalUsers": 0,
  "verifiedUsers": 0,
  "unverifiedUsers": 0,
  "pendingRegistrations": 0
}
```

### Step 5: Test Registration Again

Now you can register with those emails again:

```bash
# This should work now
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser2",
    "email": "nicslumapak@gmail.com",
    "password": "password123",
    "displayName": "Test User"
  }'
```

---

## What Gets Deleted

The cleanup endpoint deletes:
1. ✅ All **unverified users** (created by old system)
2. ✅ All **pending registrations** (not yet verified)

It does NOT delete:
- ❌ Verified users (those who clicked verification link)

---

## Manual Cleanup (Alternative)

If you prefer to use SQL directly:

### Option 1: Using psql

```bash
# Connect to your database
psql -U chatuser -d chatdb

# Delete unverified users
DELETE FROM users WHERE email_verified = false;

# Delete pending registrations
DELETE FROM pending_registrations;

# Verify
SELECT COUNT(*) FROM users;
SELECT COUNT(*) FROM pending_registrations;

# Exit
\q
```

### Option 2: Using SQL File

I've created `cleanup-test-data.sql` for you:

```bash
psql -U chatuser -d chatdb -f cleanup-test-data.sql
```

---

## Specific Email Cleanup

If you only want to delete specific emails:

```bash
# Check what exists
curl http://localhost:8080/api/admin/test-data-status
```

Then use SQL:

```sql
-- Delete specific user
DELETE FROM users WHERE email = 'nicslumapak@gmail.com';

-- Delete specific pending registration
DELETE FROM pending_registrations WHERE email = 'echolumapak@gmail.com';
```

---

## Admin Endpoint Security

### Local Development
Admin endpoints are **enabled** in your `.env.local`:
```env
ADMIN_ENABLED="true"
```

### Production (Render)
**IMPORTANT**: Admin endpoints should be **disabled** in production!

Make sure your Render environment does NOT have:
```
ADMIN_ENABLED=true
```

Or explicitly set:
```
ADMIN_ENABLED=false
```

---

## Troubleshooting

### "Admin endpoints are disabled"

**Solution**: Make sure `.env.local` has:
```env
ADMIN_ENABLED="true"
```

Then restart the application.

### "Email already exists" after cleanup

**Possible causes**:
1. Cleanup didn't run (check logs)
2. Email exists in `users` table (verified user)
3. Email exists in `pending_registrations` table

**Solution**: Check status endpoint:
```bash
curl http://localhost:8080/api/admin/test-data-status
```

---

## Your Current Situation

Based on your test attempts, you have:

1. ❌ `nicslumapak@gmail.com` - blocked (unverified user)
2. ❌ `echolumapak@gmail.com` - blocked (unverified user)
3. ✅ `shirokun0302@gmail.com` - working (pending registration)

**After cleanup**, all three emails will be available again!

---

## Complete Cleanup Steps

```bash
# 1. Restart app
mvn spring-boot:run

# 2. Check status
curl http://localhost:8080/api/admin/test-data-status

# 3. Clean up
curl -X DELETE http://localhost:8080/api/admin/cleanup-test-data

# 4. Verify
curl http://localhost:8080/api/admin/test-data-status

# 5. Test registration
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "nicslumapak@gmail.com",
    "password": "password123",
    "displayName": "Test User"
  }'
```

---

## Automatic Cleanup

The system automatically cleans up **expired** pending registrations every hour.

Pending registrations expire after **24 hours**.

So if you wait 24 hours, expired pending registrations will be automatically deleted.

But unverified users (from old system) need manual cleanup.

---

## Summary

✅ **Admin endpoint created**: `/api/admin/cleanup-test-data`  
✅ **Status endpoint created**: `/api/admin/test-data-status`  
✅ **Enabled for local dev**: `ADMIN_ENABLED=true`  
✅ **Safe for production**: Disabled by default  

**Next**: Restart app and run cleanup! 🧹
