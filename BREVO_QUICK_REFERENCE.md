# Brevo Quick Reference Card

## 🚀 3-Minute Setup

### 1. Get API Key
```
https://app.brevo.com/settings/keys/api
→ Create new API key
→ Copy it (starts with xkeysib-)
```

### 2. Update .env.local
```env
BREVO_API_KEY="xkeysib-your-key-here"
BREVO_FROM_EMAIL="nyxxlumapak@gmail.com"
BREVO_ENABLED="true"
```

### 3. Test
```bash
mvn spring-boot:run

# In another terminal:
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","email":"any@email.com","password":"password123","displayName":"Test"}'
```

---

## 📊 Brevo Dashboard Links

| What | URL |
|------|-----|
| **API Keys** | [app.brevo.com/settings/keys/api](https://app.brevo.com/settings/keys/api) |
| **Senders** | [app.brevo.com/senders](https://app.brevo.com/senders) |
| **Sent Emails** | [app.brevo.com/email/campaign/list](https://app.brevo.com/email/campaign/list) |
| **Quota** | [app.brevo.com/account/plan](https://app.brevo.com/account/plan) |

---

## 🔧 Environment Variables

### Local (.env.local)
```env
BREVO_API_KEY="xkeysib-..."
BREVO_FROM_EMAIL="your@email.com"
BREVO_FROM_NAME="Real-Time Chat"
BREVO_ENABLED="true"
RESEND_ENABLED="false"
```

### Production (Render)
```
BREVO_API_KEY=xkeysib-...
BREVO_FROM_EMAIL=your@email.com
BREVO_FROM_NAME=Real-Time Chat
BREVO_ENABLED=true
RESEND_ENABLED=false
APP_BASE_URL=https://your-backend.onrender.com
FRONTEND_BASE_URL=https://your-frontend.vercel.app
```

---

## ✅ Success Logs
```
INFO  o.e.c.s.BrevoEmailService - Brevo email service initialized
INFO  o.e.c.s.BrevoEmailService - Successfully sent verification email to: user@example.com
INFO  o.e.c.s.RegistrationService - Pending registration created, email sent: true
```

---

## ❌ Common Errors

| Error | Solution |
|-------|----------|
| "API key not configured" | Add `BREVO_API_KEY` to `.env.local` |
| "Unauthorized (401)" | Wrong API key - get new one |
| "Sender not verified" | Verify email at [app.brevo.com/senders](https://app.brevo.com/senders) |
| "Daily limit reached" | Wait until tomorrow (300/day limit) |

---

## 📈 Free Tier Limits

- **300 emails/day**
- **~9,000 emails/month**
- **No domain required**
- **No credit card required**

---

## 🎯 Quick Test

```bash
# 1. Start app
mvn spring-boot:run

# 2. Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "anyone@gmail.com",
    "password": "password123",
    "displayName": "Test User"
  }'

# 3. Check email
# 4. Click verification link
# 5. Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

---

## 📚 Documentation

- **BREVO_SETUP.md** - Full setup guide
- **BREVO_INTEGRATION_COMPLETE.md** - What was done
- **EMAIL_PROVIDER_ALTERNATIVES.md** - Provider comparison

---

## 🆘 Need Help?

1. Check `BREVO_SETUP.md` for detailed troubleshooting
2. Check Brevo dashboard for email delivery status
3. Check application logs for errors

---

## ✨ What You Get

✅ Send to ANY email (no domain needed)  
✅ 300 emails/day free  
✅ Professional HTML emails  
✅ Easy monitoring  
✅ No credit card required  

**Perfect for your use case!** 🎉
