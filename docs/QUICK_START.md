# Quick Start - Email Verification Setup

## 🚀 5-Minute Setup

### Step 1: Get Resend API Key (2 minutes)

1. Go to **[resend.com](https://resend.com)** → Sign up
2. Click **API Keys** → **Create API Key**
3. Copy the key (starts with `re_`)

### Step 2: Verify Your Email (1 minute)

1. In Resend dashboard → **Domains**
2. Click **Verify Email Address**
3. Enter: `nyxxlumapak@gmail.com`
4. Check inbox → Click verification link

### Step 3: Update `.env.local` (1 minute)

Replace these lines in `.env.local`:

```env
RESEND_API_KEY="re_PasteYourAPIKeyHere"
RESEND_FROM_EMAIL="nyxxlumapak@gmail.com"
RESEND_ENABLED="true"
```

### Step 4: Test (1 minute)

```bash
# Start app
mvn spring-boot:run

# Register (in another terminal)
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "nyxxlumapak@gmail.com",
    "password": "password123",
    "displayName": "Test User"
  }'

# Check your email!
```

## ✅ Expected Result

You should see:
1. ✅ Response: `"emailSent": true`
2. ✅ Email in your inbox (check spam if not there)
3. ✅ Click link → Account created
4. ✅ Can login

## 🔧 Production Setup

### Render Environment Variables

Add these in Render dashboard:

```
RESEND_API_KEY=re_YourAPIKey
RESEND_FROM_EMAIL=nyxxlumapak@gmail.com
RESEND_ENABLED=true
APP_BASE_URL=https://your-backend.onrender.com
FRONTEND_BASE_URL=https://your-frontend.vercel.app
```

Then deploy!

## ⚠️ Important

**Without a custom domain**, you can ONLY send emails to `nyxxlumapak@gmail.com` (your verified email).

**For production**: Add a custom domain in Resend to send to any email.

## 📚 Full Documentation

- `EMAIL_SETUP_GUIDE.md` - Complete setup guide
- `REGISTRATION_REFACTOR_GUIDE.md` - Technical details
- `REFACTOR_SUMMARY.md` - What changed

## 🆘 Troubleshooting

**Email not received?**
1. Check spam folder
2. Verify email is verified in Resend dashboard
3. Check logs: `INFO o.e.c.s.ResendEmailService - Successfully sent`

**"Email API error: 403"?**
- Email not verified in Resend → Go verify it

**"Email service not configured"?**
- Missing `RESEND_API_KEY` → Add it to `.env.local`

## 🎉 That's It!

You now have a working email verification system!
