# Admin Endpoints Configuration - COMPLETE

## Summary
Successfully enabled admin endpoints for test data cleanup. The admin endpoints are now accessible without authentication when `ADMIN_ENABLED=true` in environment variables.

## Changes Made

### 1. Updated `SecurityConfig.java`
- Added `@Value("${app.admin.enabled:false}")` to inject the admin enabled flag
- Modified authorization rules to conditionally permit `/api/admin/**` endpoints when `adminEnabled` is true
- Admin endpoints are only accessible in development mode (when `ADMIN_ENABLED=true`)

### 2. Configuration Already in Place
- `.env.local` already has `ADMIN_ENABLED="true"` set
- `application.yml` already has `app.admin.enabled` configuration
- `AdminController.java` already has the cleanup endpoints implemented

## Build Status
✅ **BUILD SUCCESSFUL** - 78 source files compiled without errors

## Available Admin Endpoints

### 1. Get Test Data Status
```bash
curl http://localhost:8080/api/admin/test-data-status
```

Response:
```json
{
  "totalUsers": 5,
  "verifiedUsers": 2,
  "unverifiedUsers": 3,
  "pendingRegistrations": 1
}
```

### 2. Clean Up Test Data
```bash
curl -X DELETE http://localhost:8080/api/admin/cleanup-test-data
```

Response:
```json
{
  "message": "Cleanup successful",
  "unverifiedUsersDeleted": 3,
  "pendingRegistrationsDeleted": 1
}
```

## How to Use

### Step 1: Start the Application
Make sure your Spring Boot application is running on `http://localhost:8080`

### Step 2: Check Current Test Data
```bash
curl http://localhost:8080/api/admin/test-data-status
```

This will show you:
- Total users in the system
- Number of verified users
- Number of unverified users (these will be deleted)
- Number of pending registrations (these will be deleted)

### Step 3: Clean Up Old Test Data
```bash
curl -X DELETE http://localhost:8080/api/admin/cleanup-test-data
```

This will:
- Delete all unverified users (including `nicslumapak@gmail.com` and `echolumapak@gmail.com`)
- Delete all pending registrations
- Return the count of deleted records

### Step 4: Verify Cleanup
```bash
curl http://localhost:8080/api/admin/test-data-status
```

The unverified users count should now be 0.

## Security Notes

⚠️ **IMPORTANT**: Admin endpoints are only enabled when `ADMIN_ENABLED=true`. This is controlled by:
- Environment variable: `ADMIN_ENABLED`
- Configuration property: `app.admin.enabled`

In production, make sure `ADMIN_ENABLED` is set to `false` or not set at all (defaults to false).

## Next Steps

1. Start your Spring Boot application
2. Run the cleanup commands above to delete the old test data
3. Try registering new accounts with the previously blocked emails
4. The new registration flow will:
   - Create a `PendingRegistration` record
   - Send a verification email via Brevo
   - Only create the actual `User` account after email verification

## Troubleshooting

If you get "Admin endpoints are disabled" error:
- Check that `ADMIN_ENABLED=true` in `.env.local`
- Restart the application after changing environment variables
- Verify the application loaded the `.env.local` file (check logs for "Loading .env.local")

If you get "Authentication required" error:
- The application may not have reloaded the SecurityConfig
- Try restarting the application
- Verify the build was successful with the new SecurityConfig changes
