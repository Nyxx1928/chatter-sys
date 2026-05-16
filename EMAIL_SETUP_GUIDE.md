# Email Verification Setup Guide

## Quick Start

### 1. Get Resend API Key

1. Go to [resend.com](https://resend.com) and sign up
2. Navigate to **API Keys** in the dashboard
3. Click **Create API Key**
4. Copy the key (starts with `re_`)

### 2. Verify Your Email

**Without a custom domain**, you can only send emails to verified addresses:

1. In Resend dashboard, go to **Domains**
2. Click **Verify Email Address**
3. Enter your email (e.g., `nyxxlumapak@gmail.com`)
4. Check your inbox and click the verification link
5. Wait for confirmation in Resend dashboard

### 3. Update Environment Variables

#### Local Development (`.env.local`)

```env
# Resend Configuration
RESEND_API_KEY="re_YourActualAPIKey"
RESEND_FROM_EMAIL="nyxxlumapak@gmail.com"  # Your verified email
RESEND_ENABLED="true"

# Application URLs
APP_BASE_URL="http://localhost:8080"
FRONTEND_BASE_URL="http://localhost:3000"
```

#### Production (Render Dashboard)

Set these environment variables in your Render service:

```
RESEND_API_KEY=re_YourActualAPIKey
RESEND_FROM_EMAIL=nyxxlumapak@gmail.com
RESEND_ENABLED=true
APP_BASE_URL=https://your-backend.onrender.com
FRONTEND_BASE_URL=https://your-frontend.vercel.app
```

### 4. Test Locally

```bash
# Start the application
./mvnw spring-boot:run

# Register with YOUR verified email
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "nyxxlumapak@gmail.com",
    "password": "password123",
    "displayName": "Test User"
  }'

# Check your email for verification link
```

## Important Limitations

### Without a Custom Domain

❌ **You can ONLY send emails to**:
- Your own verified email address
- Email addresses you manually verify in Resend

✅ **To send to ANY email address**:
- Add a custom domain to Resend
- Verify the domain with DNS records
- Use `from@yourdomain.com` as `RESEND_FROM_EMAIL`

### With a Custom Domain

✅ You can send to any email address
✅ Better deliverability
✅ Professional appearance

## Adding a Custom Domain (Optional)

### 1. Purchase a Domain

Buy a domain from:
- Namecheap
- GoDaddy
- Google Domains
- Cloudflare

### 2. Add Domain to Resend

1. In Resend dashboard, go to **Domains**
2. Click **Add Domain**
3. Enter your domain (e.g., `yourdomain.com`)
4. Copy the DNS records provided

### 3. Configure DNS

Add these records to your domain's DNS settings:

```
Type: TXT
Name: @
Value: [Resend verification code]

Type: MX
Name: @
Value: feedback-smtp.us-east-1.amazonses.com
Priority: 10

Type: TXT
Name: _dmarc
Value: v=DMARC1; p=none

Type: TXT
Name: resend._domainkey
Value: [Resend DKIM key]
```

### 4. Verify Domain

1. Wait for DNS propagation (5-30 minutes)
2. Click **Verify** in Resend dashboard
3. Once verified, update `RESEND_FROM_EMAIL`:

```env
RESEND_FROM_EMAIL="noreply@yourdomain.com"
```

## Testing

### Test Email Sending

```bash
# Register a test user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "your-verified-email@example.com",
    "password": "password123",
    "displayName": "Test User"
  }'

# Expected response:
{
  "message": "Registration initiated. Please check your email to verify your account.",
  "emailSent": true,
  "verificationUrl": "http://localhost:8080/api/auth/verify-email?token=...",
  "errorMessage": null
}
```

### Check Logs

Look for these log messages:

✅ **Success**:
```
INFO  o.e.c.s.ResendEmailService - Successfully sent verification email to: user@example.com
INFO  o.e.c.s.RegistrationService - Pending registration created for username: testuser, email sent: true
```

❌ **Failure**:
```
ERROR o.e.c.s.ResendEmailService - Resend API error (status 403): Forbidden
ERROR o.e.c.s.ResendEmailService - Failed to send email to user@example.com via Resend API
```

### Check Resend Dashboard

1. Go to **Emails** in Resend dashboard
2. See all sent emails
3. Check delivery status
4. View email content

## Troubleshooting

### Error: "Email API error: 403 Forbidden"

**Cause**: Trying to send to unverified email without a domain

**Solution**:
1. Verify the recipient email in Resend dashboard, OR
2. Add a custom domain

### Error: "Email service not configured"

**Cause**: Missing `RESEND_API_KEY` or `RESEND_FROM_EMAIL`

**Solution**: Set both environment variables

### Error: "Invalid API key"

**Cause**: Wrong or expired API key

**Solution**:
1. Go to Resend dashboard
2. Generate a new API key
3. Update `RESEND_API_KEY`

### Email Not Received

**Check**:
1. ✅ Spam folder
2. ✅ Email address is verified in Resend
3. ✅ Resend dashboard shows email as "Delivered"
4. ✅ Backend logs show "Successfully sent"

**Common causes**:
- Email in spam folder
- Email provider blocking
- Wrong email address
- Resend rate limit exceeded

### "Username already exists"

**Cause**: Username is taken (in `users` or `pending_registrations`)

**Solution**: Use a different username

### "Email already exists"

**Cause**: Email is taken (in `users` or `pending_registrations`)

**Solution**:
1. Use a different email, OR
2. Wait 24 hours for pending registration to expire, OR
3. Manually delete from `pending_registrations` table

## Production Deployment

### Render Environment Variables

Set in Render dashboard → Environment:

```
RESEND_API_KEY=re_YourProductionAPIKey
RESEND_FROM_EMAIL=noreply@yourdomain.com
RESEND_ENABLED=true
APP_BASE_URL=https://your-backend.onrender.com
FRONTEND_BASE_URL=https://your-frontend.vercel.app
```

### Vercel Environment Variables

Update frontend `.env.local`:

```env
NEXT_PUBLIC_API_BASE_URL=https://your-backend.onrender.com
```

### Database

The `pending_registrations` table will be created automatically by Hibernate on first startup.

### Monitoring

1. **Resend Dashboard**: Monitor email delivery
2. **Backend Logs**: Check for errors
3. **Database**: Monitor pending registrations

```sql
-- Check pending registrations
SELECT username, email, created_at, email_sent, expiry_date 
FROM pending_registrations 
ORDER BY created_at DESC;

-- Check expired registrations
SELECT COUNT(*) 
FROM pending_registrations 
WHERE expiry_date < NOW();
```

## Email Templates

The current email template is HTML-based with:
- ✅ Responsive design
- ✅ Clear call-to-action button
- ✅ Plain text fallback
- ✅ Professional styling

To customize, edit `ResendEmailService.buildVerificationEmailHtml()`.

## Rate Limits

### Resend Free Tier

- 100 emails/day
- 3,000 emails/month

### Resend Paid Plans

- Pro: $20/month for 50,000 emails
- Business: Custom pricing

### Handling Rate Limits

If you hit rate limits:
1. Upgrade Resend plan
2. Implement email queuing
3. Add rate limiting to registration endpoint

## Security Best Practices

### Current Implementation

✅ Tokens are UUID (cryptographically random)
✅ Tokens expire after 24 hours
✅ Tokens are single-use
✅ Passwords are hashed before storage
✅ Email verification required before account creation

### Additional Recommendations

1. **Add CAPTCHA** to registration endpoint
2. **Rate limit** registration attempts
3. **Monitor** for abuse patterns
4. **Log** all registration attempts
5. **Alert** on suspicious activity

## Maintenance

### Automatic Cleanup

Expired pending registrations are automatically deleted every hour:

```java
@Scheduled(fixedRate = 3600000) // 1 hour
public void cleanupExpiredPendingRegistrations()
```

### Manual Cleanup

If needed:

```sql
-- Delete expired pending registrations
DELETE FROM pending_registrations 
WHERE expiry_date < NOW();

-- Delete old pending registrations (older than 7 days)
DELETE FROM pending_registrations 
WHERE created_at < NOW() - INTERVAL '7 days';
```

## Cost Estimation

### Resend Costs

**Free Tier**:
- 100 emails/day = $0
- 3,000 emails/month = $0

**Pro Tier** ($20/month):
- 50,000 emails/month
- $0.40 per 1,000 additional emails

**Example**:
- 100 registrations/day = 3,000 emails/month = **FREE**
- 1,000 registrations/day = 30,000 emails/month = **FREE** (under 50k)
- 5,000 registrations/day = 150,000 emails/month = **$20 + $40 = $60/month**

### Domain Costs

- Domain registration: $10-15/year
- DNS hosting: Usually free with domain

## Support

### Resend Support

- Documentation: [resend.com/docs](https://resend.com/docs)
- Support: support@resend.com
- Status: [status.resend.com](https://status.resend.com)

### Application Logs

Check logs for detailed error messages:

```bash
# Local
./mvnw spring-boot:run

# Render
# View logs in Render dashboard
```

## Next Steps

1. ✅ Set up Resend account
2. ✅ Verify your email
3. ✅ Update environment variables
4. ✅ Test locally
5. ✅ Deploy to production
6. ⬜ (Optional) Add custom domain
7. ⬜ (Optional) Customize email template
8. ⬜ (Optional) Add CAPTCHA
9. ⬜ (Optional) Add rate limiting

## FAQ

**Q: Can I use a different email provider?**
A: Yes, but you'll need to implement a new email service. Resend is recommended for its simplicity and reliability.

**Q: Do I need a custom domain?**
A: No, but without one you can only send to verified emails. For production, a domain is recommended.

**Q: What happens to existing users?**
A: Existing users are not affected. They can still login normally.

**Q: Can I disable email verification?**
A: Not recommended for production. For testing, you can set `RESEND_ENABLED=false` and use the `verificationUrl` from the response.

**Q: How do I test without sending real emails?**
A: Set `RESEND_ENABLED=false` and use the `verificationUrl` from the registration response.

**Q: What if email sending fails?**
A: The registration still succeeds, but `emailSent` will be `false`. The user can request a resend via `/api/auth/resend-verification`.
