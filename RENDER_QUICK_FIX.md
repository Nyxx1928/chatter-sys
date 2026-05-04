# 🚨 Quick Fix: Database Connection Error

## The Issue

Your app is trying to connect to `localhost:5432` instead of the Render database.

**Error in logs:**
```
Connection to localhost:5432 refused
```

## The Solution (2 Minutes)

### 1️⃣ Get Database URL

Go to Render Dashboard → **PostgreSQL Database** → Copy **"Internal Database URL"**

It looks like:
```
postgresql://chatuser:abc123@dpg-xxxxx-a/chatdb_xxxxx
```

### 2️⃣ Set DATABASE_URL

Go to Render Dashboard → **Web Service** (`chat-backend`) → **Environment** tab

Add or update:
```
Key:   DATABASE_URL
Value: postgresql://chatuser:abc123@dpg-xxxxx-a/chatdb_xxxxx
       (paste what you copied)
```

Click **"Save Changes"**

### 3️⃣ Wait for Redeploy

Render will automatically redeploy (3-5 minutes)

### 4️⃣ Verify

Check logs for:
```
✅ HikariPool-1 - Start completed
✅ Started ChatApplication
```

Test:
```bash
curl https://your-backend.onrender.com/actuator/health
```

Should return:
```json
{"status":"UP"}
```

## Why This Happened

The `render.yaml` Blueprint database linking didn't work automatically. You need to manually set the `DATABASE_URL` environment variable.

## Checklist

- [ ] Found PostgreSQL database in Render Dashboard
- [ ] Copied **Internal Database URL** (not External)
- [ ] Went to web service Environment tab
- [ ] Added/updated `DATABASE_URL` variable
- [ ] Saved changes
- [ ] Waited for redeploy
- [ ] Checked logs for success
- [ ] Tested health endpoint

## Still Not Working?

Check these environment variables are set:

- [ ] `SPRING_PROFILES_ACTIVE` = `prod`
- [ ] `DATABASE_URL` = `postgresql://...` (Internal URL)
- [ ] `JWT_SECRET` = (any value)
- [ ] `PORT` = `8080`

## Need More Help?

See detailed guide: [RENDER_DATABASE_SETUP.md](RENDER_DATABASE_SETUP.md)

---

**TL;DR:** Copy your database's Internal URL and paste it as `DATABASE_URL` in your web service's environment variables. 🎯
