# Database Issue Fix

## Problem Identified

Your backend is running, but registration fails with:

```
ERROR: relation "users" does not exist
```

This means the database tables weren't created. The issue is **PostgreSQL permissions** - the `chatuser` doesn't have permission to create tables in the `public` schema.

## Solution

### Option 1: Run the Fix Script (Easiest)

```powershell
powershell -ExecutionPolicy Bypass -File fix-database-permissions.ps1
```

This will:
1. Grant schema permissions to `chatuser`
2. Set default privileges for future tables
3. Verify the permissions

### Option 2: Manual Fix

Run these commands in PostgreSQL:

```bash
# Connect as postgres user
psql -U postgres -h localhost -d chatdb

# Run these SQL commands:
GRANT ALL ON SCHEMA public TO chatuser;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO chatuser;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO chatuser;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO chatuser;

# Exit
\q
```

## After Fixing Permissions

### Step 1: Restart the Backend

1. Stop the current backend (Ctrl+C in the terminal where it's running)
2. Start it again:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

### Step 2: Verify Tables Were Created

The backend should create the tables automatically. Check the logs for:

```
Hibernate: create table users (...)
Hibernate: create table chat_rooms (...)
Hibernate: create table messages (...)
Hibernate: create table room_memberships (...)
```

Or verify in PostgreSQL:

```bash
psql -U chatuser -d chatdb -h localhost -c "\dt"
```

You should see:
```
 Schema |       Name        | Type  |  Owner   
--------+-------------------+-------+----------
 public | chat_rooms        | table | chatuser
 public | messages          | table | chatuser
 public | room_memberships  | table | chatuser
 public | users             | table | chatuser
```

### Step 3: Test Again

```powershell
powershell -ExecutionPolicy Bypass -File test-backend.ps1
```

All tests should pass now! ✅

## Why This Happened

PostgreSQL 15+ changed the default permissions for the `public` schema. By default, new users don't have permission to create tables in the `public` schema.

The fix grants:
1. **Schema permissions** - Allows `chatuser` to create objects in the `public` schema
2. **Default privileges** - Ensures future tables are accessible to `chatuser`
3. **Table privileges** - Grants access to any existing tables
4. **Sequence privileges** - Grants access to auto-increment sequences

## Troubleshooting

### Issue: "Permission denied for schema public"

**Solution:** Run the fix script or manual commands above.

### Issue: Tables still not created after restart

**Check the logs:**
```powershell
Get-Content logs/chat-application.log -Tail 100 | Select-String "Hibernate"
```

Look for `create table` statements. If you don't see them, check:

1. **Profile is set to dev:**
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

2. **application.yml has correct settings:**
   ```yaml
   spring:
     jpa:
       hibernate:
         ddl-auto: create-drop  # Should be create-drop for dev profile
   ```

### Issue: "FATAL: password authentication failed"

**Solution:** Reset the password:

```bash
psql -U postgres -h localhost -c "ALTER USER chatuser WITH PASSWORD 'chatpass';"
```

### Issue: "Connection refused"

**Solution:** Start PostgreSQL:

```powershell
# Check status
Get-Service -Name postgresql*

# Start if stopped
Start-Service postgresql*
```

## Alternative: Use H2 Database (For Testing Only)

If you want to skip PostgreSQL setup for now, you can use H2 (in-memory database):

1. **Add H2 dependency to pom.xml:**
   ```xml
   <dependency>
       <groupId>com.h2database</groupId>
       <artifactId>h2</artifactId>
       <scope>runtime</scope>
   </dependency>
   ```

2. **Update application.yml:**
   ```yaml
   spring:
     datasource:
       url: jdbc:h2:mem:chatdb
       driver-class-name: org.h2.Driver
       username: sa
       password:
     jpa:
       hibernate:
         ddl-auto: create-drop
   ```

3. **Restart backend**

**Note:** H2 is in-memory only - data is lost when you stop the server. Use PostgreSQL for production.

## Summary

1. ✅ **Run fix script:** `powershell -ExecutionPolicy Bypass -File fix-database-permissions.ps1`
2. ✅ **Restart backend:** `mvn spring-boot:run -Dspring-boot.run.profiles=dev`
3. ✅ **Verify tables:** `psql -U chatuser -d chatdb -c "\dt"`
4. ✅ **Test API:** `powershell -ExecutionPolicy Bypass -File test-backend.ps1`

Your backend should now work perfectly!
