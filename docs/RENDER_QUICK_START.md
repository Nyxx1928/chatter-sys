# Render Quick Start Guide

Deploy your backend to Render in 5 minutes! ⚡

## Prerequisites

- ✅ GitHub account
- ✅ Code pushed to GitHub repository
- ✅ Render account (sign up at [render.com](https://render.com))

## Step 1: Prepare Your Repository

Ensure these files are in your repository root:
```
✓ render.yaml          (Infrastructure config)
✓ Dockerfile           (Container config)
✓ pom.xml             (Maven config)
✓ src/                (Source code)
```

## Step 2: Push to GitHub

```bash
git add .
git commit -m "Prepare for Render deployment"
git push origin main
```

Or use the helper script:
```bash
# Linux/Mac
./deploy-render.sh

# Windows PowerShell
.\deploy-render.ps1
```

## Step 3: Deploy on Render

### Using Blueprint (Recommended - 2 clicks!)

1. Go to [Render Dashboard](https://dashboard.render.com)
2. Click **"New +"** → **"Blueprint"**
3. Select your GitHub repository
4. Click **"Apply"**

That's it! Render will:
- ✅ Create PostgreSQL database
- ✅ Build Docker image
- ✅ Deploy backend service
- ✅ Set up environment variables
- ✅ Configure health checks

### Manual Setup (More Control)

1. **Create Database:**
   - New + → PostgreSQL
   - Name: `chat-db`
   - Click "Create Database"

2. **Create Web Service:**
   - New + → Web Service
   - Connect GitHub repo
   - Environment: Docker
   - Add environment variables (see below)

## Step 4: Configure Environment Variables

After deployment, update these in Render dashboard:

| Variable | Value | Where to Get |
|----------|-------|--------------|
| `JWT_SECRET` | Generate new | `openssl rand -base64 32` |
| `CORS_ALLOWED_ORIGINS` | Your frontend URL | After frontend deploy |

Other variables are auto-configured by `render.yaml`.

## Step 5: Test Your Deployment

```bash
# Test health endpoint
curl https://your-backend.onrender.com/actuator/health

# Should return:
# {"status":"UP"}
```

## Your Backend URLs

After deployment, you'll get:

- **API Base URL:** `https://chat-backend.onrender.com`
- **Health Check:** `https://chat-backend.onrender.com/actuator/health`
- **WebSocket:** `wss://chat-backend.onrender.com/ws`

## Next Steps

1. ✅ Backend deployed
2. 📝 Copy backend URL
3. 🎨 Deploy frontend (update with backend URL)
4. 🔄 Update backend CORS with frontend URL
5. 🧪 Test end-to-end

## Common Issues

### Build Fails
- Check logs in Render dashboard
- Verify `pom.xml` is valid
- Ensure Java 17 is specified

### Database Connection Fails
- Use Internal Database URL (not External)
- Check DATABASE_URL is set
- Verify database is in same region

### Health Check Fails
- Wait 2-3 minutes for first deploy
- Check application logs
- Verify `/actuator/health` endpoint

### WebSocket Issues
- Ensure frontend uses `wss://` (not `ws://`)
- Check CORS configuration
- Verify WebSocket endpoint path

## Free Tier Notes

⚠️ **Important:** Free tier services spin down after 15 minutes of inactivity.

- First request after spin down: 30-60 seconds
- Database: 90-day expiration
- Build minutes: Limited per month

For production, upgrade to paid tier ($7/month).

## Useful Commands

### Generate JWT Secret
```bash
openssl rand -base64 32
```

### Test Registration
```bash
curl -X POST https://your-backend.onrender.com/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123"
  }'
```

### Test Login
```bash
curl -X POST https://your-backend.onrender.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

## Need More Help?

- 📖 **Detailed Guide:** [RENDER_DEPLOYMENT.md](RENDER_DEPLOYMENT.md)
- ✅ **Checklist:** [RENDER_CHECKLIST.md](RENDER_CHECKLIST.md)
- 🌐 **Render Docs:** https://render.com/docs
- 💬 **Community:** https://community.render.com

## Deployment Timeline

| Step | Time |
|------|------|
| Push to GitHub | 1 min |
| Connect to Render | 1 min |
| Build & Deploy | 3-5 min |
| **Total** | **~5-7 min** |

---

**Ready to deploy?** Follow the steps above or run `./deploy-render.sh` to get started! 🚀
