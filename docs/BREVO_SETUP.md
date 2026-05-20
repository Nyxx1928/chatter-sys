# Brevo Email Setup Guide

## ✅ Perfect Choice!

Brevo (formerly Sendinblue) is perfect for your use case:
- ✅ **300 emails/day** on free tier
- ✅ **No domain required** - can send to ANY email immediately
- ✅ **No credit card required**
- ✅ **Easy setup** - just need API key

---

## Quick Setup (3 minutes)

### Step 1: Get Your API Key

Since you already have a Brevo account:

1. Go to [app.brevo.com/settings/keys/api](https://app.brevo.com/settings/keys/api)
2. Click **"Create a new API key"**
3. Give it a name (e.g., "Real-Time Chat")
4. Copy the API key (starts with `xkeysib-`)

### Step 2: Get Your Sender Email

1. Go to [app.brevo.com/senders](https://app.brevo.com/senders)
2. You should see your verified email address
3. If not, click **"Add a new sender"** and verify your email

### Step 3: Update Environment Variables

Update your `.env.local` file:

```env
# Brevo Configuration
BREVO_API_KEY="xkeysib-your-actual-api-key-here"
BREVO_FROM_EMAIL="nyxxlumapak@gmail.com"
BREVO_FROM_NAME="Real-Time Chat"
BREVO_ENABLED="true"

# Disable Resend
RESEND_ENABLED="false"
```

### Step 4: Test Locally

```bash
# Start the application
mvn spring-boot:run

# In another terminal, test registration
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "anyone@gmail.com",
    "password": "password123",
    "displayName": "Test User"
  }'

# Check the email inbox of anyone@gmail.com
```

### Step 5: Check Logs

Look for these log messages:

✅ **Success**:
```
INFO  o.e.c.s.BrevoEmailService - Successfully sent verification email to: anyone@gmail.com
INFO  o.e.c.s.RegistrationService - Pending registration created for username: testuser, email sent: true
```

❌ **Failure**:
```
ERROR o.e.c.s.BrevoEmailService - Brevo API error (status 401): Unauthorized
```

---

## Production Setup (Render)

### Update Render Environment Variables

Go to your Render dashboard → Your service → Environment:

```
BREVO_API_KEY=xkeysib-your-actual-api-key
BREVO_FROM_EMAIL=nyxxlumapak@gmail.com
BREVO_FROM_NAME=Real-Time Chat
BREVO_ENABLED=true
RESEND_ENABLED=false
APP_BASE_URL=https://your-backend.onrender.com
FRONTEND_BASE_URL=https://your-frontend.vercel.app
```

Then redeploy!

---

## Brevo Dashboard

### Monitor Your Emails

1. Go to [app.brevo.com/email/campaign/list](https://app.brevo.com/email/campaign/list)
2. Click **"Transactional"** tab
3. See all sent emails with delivery status

### Check Your Quota

1. Go to [app.brevo.com/account/plan](https://app.brevo.com/account/plan)
2. See how many emails you've sent today
3. Free tier: 300 emails/day

---

## Troubleshooting

### Error: "Unauthorized" (401)

**Cause**: Wrong API key

**Solution**:
1. Go to [app.brevo.com/settings/keys/api](https://app.brevo.com/settings/keys/api)
2. Create a new API key
3. Update `BREVO_API_KEY` in `.env.local`

### Error: "Sender not verified"

**Cause**: Email address not verified in Brevo

**Solution**:
1. Go to [app.brevo.com/senders](https://app.brevo.com/senders)
2. Click "Add a new sender"
3. Enter your email and verify it
4. Update `BREVO_FROM_EMAIL` to match

### Email Not Received

**Check**:
1. ✅ Spam folder
2. ✅ Brevo dashboard shows "Delivered"
3. ✅ Backend logs show "Successfully sent"
4. ✅ Correct email address

### Daily Limit Reached

**Symptoms**: Error "Daily sending limit reached"

**Solution**:
- Free tier: 300 emails/day
- Wait until tomorrow, or
- Upgrade to paid plan ($25/month for 20k emails)

---

## Brevo Free Tier Limits

| Feature | Free Tier |
|---------|-----------|
| Emails/day | 300 |
| Emails/month | ~9,000 |
| Domain required | No |
| Credit card required | No |
| Sender verification | Yes (email only) |

---

## Comparison: Brevo vs Resend

| Feature | Brevo | Resend |
|---------|-------|--------|
| Free tier | 300/day | 3,000/month |
| Domain required | No | Yes |
| Setup difficulty | Easy | Easy |
| Deliverability | Good | Excellent |
| Best for | Testing, small apps | Production apps |

---

## Email Template

The email sent looks like this:

```
Subject: Verify your email - Real-Time Chat

Welcome to Real-Time Chat!

Thank you for registering! Please verify your email address 
to complete your registration.

[Verify Email Address Button]

Or copy and paste this link into your browser:
http://localhost:8080/api/auth/verify-email?token=xxx

This link will expire in 24 hours.

If you did not register for this account, please ignore this email.
```

---

## Testing Checklist

- [ ] Brevo account created ✅ (you already have this)
- [ ] API key obtained
- [ ] Sender email verified
- [ ] `.env.local` updated with API key
- [ ] `.env.local` updated with sender email
- [ ] `BREVO_ENABLED=true` set
- [ ] `RESEND_ENABLED=false` set
- [ ] Application starts without errors
- [ ] Registration tested
- [ ] Email received
- [ ] Verification link works
- [ ] Login works after verification

---

## Next Steps

1. **Get your API key** from Brevo dashboard
2. **Update `.env.local`** with your API key and email
3. **Test locally** with any email address
4. **Update Render** environment variables
5. **Deploy and test** in production

---

## Support

### Brevo Documentation
- API Docs: [developers.brevo.com](https://developers.brevo.com)
- Support: [help.brevo.com](https://help.brevo.com)

### Application Logs
Check logs for detailed error messages:
```bash
# Local
mvn spring-boot:run

# Render
# View logs in Render dashboard
```

---

## FAQ

**Q: Can I send to any email address?**
A: Yes! Unlike Resend, Brevo doesn't require a custom domain.

**Q: Do I need to verify recipient emails?**
A: No, only your sender email needs to be verified.

**Q: What happens if I hit the daily limit?**
A: Emails will fail until the next day (resets at midnight UTC).

**Q: Can I use a custom domain?**
A: Yes, but it's optional. You can use any verified email address.

**Q: Is 300 emails/day enough?**
A: For testing and small apps, yes. For production, consider upgrading.

---

## Ready to Test!

Your project is now configured to use Brevo. Just:

1. Get your API key from Brevo
2. Update `.env.local`
3. Run `mvn spring-boot:run`
4. Test registration!

🚀 **You can now send emails to ANY address without a custom domain!**
