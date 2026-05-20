# Registration System Refactor Guide

## What Changed

### 1. **Verify-First Registration Flow**

**Before**:
```
User registers → Account created immediately → Email sent → User can't login until verified
```

**After**:
```
User registers → Pending registration created → Email sent → User clicks link → Account created → User can login
```

### 2. **Resend HTTP API Instead of SMTP**

**Before**: Used Spring Mail with SMTP (`smtp.resend.com:587`)
**After**: Uses Resend HTTP API directly via WebClient

**Benefits**:
- ✅ Better error messages
- ✅ More reliable delivery
- ✅ Easier to debug
- ✅ No SMTP configuration issues
- ✅ Better rate limiting

### 3. **New Database Table**

A new `pending_registrations` table stores registration data before email verification:

```sql
CREATE TABLE pending_registrations (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) UNIQUE NOT NULL,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    email_sent BOOLEAN NOT NULL DEFAULT FALSE
);
```

## Configuration Changes

### Environment Variables

**New (Required)**:
```env
RESEND_API_KEY="re_YourAPIKey"
RESEND_FROM_EMAIL="your-verified-email@example.com"
RESEND_ENABLED="true"
```

**Old (Still supported for backward compatibility)**:
```env
MAIL_HOST="smtp.resend.com"
MAIL_PORT="587"
MAIL_USERNAME="resend"
MAIL_PASSWORD="re_YourAPIKey"
MAIL_SMTP_AUTH="true"
MAIL_SMTP_STARTTLS="true"
```

### Local Development Setup

1. **Get your Resend API key**:
   - Go to [resend.com](https://resend.com)
   - Sign up or log in
   - Go to API Keys section
   - Copy your API key (starts with `re_`)

2. **Verify your email in Resend**:
   - Go to Domains section
   - Click "Verify Email"
   - Enter your email address
   - Check your inbox and verify

3. **Update `.env.local`**:
   ```env
   RESEND_API_KEY="re_YourActualAPIKey"
   RESEND_FROM_EMAIL="your-verified-email@gmail.com"
   RESEND_ENABLED="true"
   APP_BASE_URL="http://localhost:8080"
   ```

### Production Setup (Render)

Set these environment variables in your Render dashboard:

```env
RESEND_API_KEY=re_YourActualAPIKey
RESEND_FROM_EMAIL=your-verified-email@example.com
RESEND_ENABLED=true
APP_BASE_URL=https://your-app.onrender.com
FRONTEND_BASE_URL=https://your-app.vercel.app
```

## API Changes

### Registration Endpoint

**Before** (`POST /api/auth/register`):
```json
Response (201 Created):
{
  "id": 123,
  "username": "john",
  "email": "john@example.com",
  "displayName": "John Doe",
  "emailVerified": false,
  "verificationUrl": "http://..."  // Only if EXPOSE_VERIFICATION_LINK=true
}
```

**After** (`POST /api/auth/register`):
```json
Response (201 Created):
{
  "message": "Registration initiated. Please check your email to verify your account.",
  "emailSent": true,
  "verificationUrl": "http://...",  // Only if EXPOSE_VERIFICATION_LINK=true or email failed
  "errorMessage": null  // Only present if email failed
}
```

### Verification Endpoint

**Behavior Change**:
- Now creates the user account when verification link is clicked
- Before: Just marked existing user as verified

**Endpoint**: `GET /api/auth/verify-email?token=xxx`
- Redirects to frontend with status and message
- Creates user account on successful verification

### Resend Verification Endpoint

**Endpoint**: `POST /api/auth/resend-verification`
```json
Request:
{
  "email": "user@example.com"
}

Response (200 OK):
{
  "message": "Verification email sent"
}
```

## Migration Steps

### For Existing Deployments

1. **Update dependencies**:
   ```bash
   ./mvnw clean install
   ```

2. **Database migration**:
   - The `pending_registrations` table will be created automatically by Hibernate
   - Existing users are not affected

3. **Update environment variables**:
   - Add `RESEND_API_KEY`
   - Add `RESEND_FROM_EMAIL`
   - Add `RESEND_ENABLED=true`

4. **Verify email in Resend**:
   - Log into Resend dashboard
   - Verify the email you'll use as `RESEND_FROM_EMAIL`

5. **Deploy**:
   - Push changes
   - Restart application

### For New Deployments

Follow the "Production Setup" section above.

## Testing

### Local Testing

1. **Start the application**:
   ```bash
   ./mvnw spring-boot:run
   ```

2. **Register a new user**:
   ```bash
   curl -X POST http://localhost:8080/api/auth/register \
     -H "Content-Type: application/json" \
     -d '{
       "username": "testuser",
       "email": "your-verified-email@example.com",
       "password": "password123",
       "displayName": "Test User"
     }'
   ```

3. **Check logs**:
   ```
   INFO  o.e.c.s.ResendEmailService - Successfully sent verification email to: your-verified-email@example.com
   INFO  o.e.c.s.RegistrationService - Pending registration created for username: testuser, email sent: true
   ```

4. **Check your email** and click the verification link

5. **Verify account was created**:
   ```bash
   # Try to login
   curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{
       "username": "testuser",
       "password": "password123"
     }'
   ```

### Testing Email Failures

Set `RESEND_ENABLED=false` to test the flow when email fails:

```env
RESEND_ENABLED=false
```

The registration will still succeed, but `emailSent` will be `false` and `verificationUrl` will be included in the response.

## Troubleshooting

### Email Not Sending

**Check logs for**:
```
ERROR o.e.c.s.ResendEmailService - Resend API error (status 403): ...
```

**Common causes**:
1. ❌ Invalid API key
2. ❌ Email not verified in Resend
3. ❌ Trying to send to unverified email (without domain)
4. ❌ Rate limit exceeded

**Solutions**:
1. Verify API key is correct
2. Verify email in Resend dashboard
3. Only send to verified emails (or add a domain)
4. Check Resend dashboard for rate limits

### "Username already exists" Error

This can happen if:
1. Username exists in `users` table (already registered)
2. Username exists in `pending_registrations` table (pending verification)

**Solution**: Use a different username or wait for pending registration to expire (24 hours)

### "Email already exists" Error

Same as username - check both tables.

### Verification Link Expired

Pending registrations expire after 24 hours. User must register again.

### Account Not Created After Verification

**Check**:
1. Did verification succeed? (check frontend redirect)
2. Check backend logs for errors
3. Check database - is user in `users` table?

## Cleanup

### Expired Pending Registrations

The system automatically cleans up expired pending registrations every hour via scheduled task:

```java
@Scheduled(fixedRate = 3600000) // 1 hour
public void cleanupExpiredPendingRegistrations()
```

### Manual Cleanup

If needed, you can manually delete expired pending registrations:

```sql
DELETE FROM pending_registrations 
WHERE expiry_date < NOW();
```

## Backward Compatibility

### Existing Users

- ✅ Existing users are not affected
- ✅ They can still login normally
- ✅ Email verification for existing users still works via old flow

### SMTP Configuration

- ✅ SMTP configuration is still supported
- ✅ Old `EmailService` is still available
- ✅ New `ResendEmailService` is preferred

## Benefits of New System

### Security
- ✅ No orphaned accounts in database
- ✅ No email/username squatting
- ✅ Cleaner user table

### User Experience
- ✅ Clear feedback about email status
- ✅ Better error messages
- ✅ Can resend verification email

### Developer Experience
- ✅ Easier to debug (HTTP API vs SMTP)
- ✅ Better logging
- ✅ More reliable email delivery

### Operations
- ✅ Automatic cleanup of expired registrations
- ✅ No manual database cleanup needed
- ✅ Better monitoring via Resend dashboard

## Rollback Plan

If you need to rollback:

1. **Revert code changes**:
   ```bash
   git revert <commit-hash>
   ```

2. **Keep environment variables**:
   - Old SMTP config still works
   - Just set `RESEND_ENABLED=false`

3. **Database**:
   - `pending_registrations` table can be dropped
   - Existing users are not affected

## Support

For issues:
1. Check logs first
2. Verify Resend configuration
3. Test with verified email
4. Check Resend dashboard for delivery status

## Next Steps

1. ✅ Update frontend to handle new registration response
2. ✅ Add monitoring for email delivery
3. ✅ Consider adding email templates
4. ✅ Add rate limiting for registration
5. ✅ Add CAPTCHA for registration
