# Render Deployment Checklist

Use this checklist to ensure a smooth deployment to Render.

## Pre-Deployment

- [ ] Code is committed to Git
- [ ] All tests pass locally (`mvn test`)
- [ ] Application builds successfully (`mvn clean package`)
- [ ] GitHub repository is created and code is pushed
- [ ] `render.yaml` file is in repository root
- [ ] `Dockerfile` is in repository root
- [ ] Spring Boot Actuator is added to `pom.xml`

## Render Setup

- [ ] Render account created at [render.com](https://render.com)
- [ ] GitHub account connected to Render
- [ ] Repository access granted to Render

## Deployment Steps

### Option 1: Blueprint (Recommended)

- [ ] Go to Render Dashboard
- [ ] Click "New +" → "Blueprint"
- [ ] Select your GitHub repository
- [ ] Render detects `render.yaml`
- [ ] Review services to be created:
  - [ ] PostgreSQL database (`chat-db`)
  - [ ] Web service (`chat-backend`)
- [ ] Click "Apply" to create services

### Option 2: Manual Setup

- [ ] Create PostgreSQL database
  - [ ] Name: `chat-db`
  - [ ] Database: `chatdb`
  - [ ] User: `chatuser`
  - [ ] Save connection details
- [ ] Create Web Service
  - [ ] Connect GitHub repository
  - [ ] Environment: Docker
  - [ ] Dockerfile path: `./Dockerfile`
  - [ ] Health check: `/actuator/health`
- [ ] Add environment variables (see below)

## Environment Variables

Configure these in Render dashboard:

- [ ] `SPRING_PROFILES_ACTIVE` = `prod`
- [ ] `DATABASE_URL` = (from PostgreSQL service - Internal URL)
- [ ] `DB_USERNAME` = `chatuser`
- [ ] `DB_PASSWORD` = (from PostgreSQL service)
- [ ] `JWT_SECRET` = (generate with: `openssl rand -base64 32`)
- [ ] `CORS_ALLOWED_ORIGINS` = `https://your-frontend.onrender.com`
- [ ] `PORT` = `8080`

## Post-Deployment

- [ ] Build completes successfully
- [ ] Service starts without errors
- [ ] Health check passes: `https://your-backend.onrender.com/actuator/health`
- [ ] Test API endpoints:
  - [ ] `GET /actuator/health` returns 200
  - [ ] `POST /api/auth/register` works
  - [ ] `POST /api/auth/login` works
- [ ] Database connection works
- [ ] WebSocket endpoint accessible

## Frontend Integration

- [ ] Update frontend `NEXT_PUBLIC_API_URL` with backend URL
- [ ] Update frontend `NEXT_PUBLIC_WS_URL` with backend WebSocket URL
- [ ] Update backend `CORS_ALLOWED_ORIGINS` with frontend URL
- [ ] Test end-to-end:
  - [ ] User registration
  - [ ] User login
  - [ ] Send message
  - [ ] Receive message
  - [ ] WebSocket connection

## Optional Enhancements

- [ ] Set up custom domain
- [ ] Configure SSL certificate (automatic with Render)
- [ ] Set up monitoring/alerts
- [ ] Configure auto-deploy on push
- [ ] Set up staging environment
- [ ] Enable database backups
- [ ] Review and optimize resource usage

## Troubleshooting

If deployment fails, check:

- [ ] Build logs in Render dashboard
- [ ] Application logs in Render dashboard
- [ ] Environment variables are set correctly
- [ ] Database URL format is correct
- [ ] Health check endpoint is accessible
- [ ] Port configuration is correct
- [ ] CORS configuration allows frontend domain

## Useful Commands

### Generate JWT Secret
```bash
openssl rand -base64 32
```

### Test Health Endpoint
```bash
curl https://your-backend.onrender.com/actuator/health
```

### Test API Endpoint
```bash
curl -X POST https://your-backend.onrender.com/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"password123"}'
```

### View Logs
Go to Render Dashboard → Your Service → Logs

## Support Resources

- [ ] Read `RENDER_DEPLOYMENT.md` for detailed instructions
- [ ] Check Render documentation: https://render.com/docs
- [ ] Visit Render community: https://community.render.com
- [ ] Review Spring Boot on Render guide

## Notes

**Free Tier Limitations:**
- Services spin down after 15 minutes of inactivity
- First request after spin down takes 30-60 seconds
- Database expires after 90 days
- Limited build minutes per month

**Production Recommendations:**
- Upgrade to paid tier for production use
- Enable auto-scaling
- Set up monitoring and alerts
- Configure database backups
- Use custom domain with SSL

---

**Status:** ⬜ Not Started | 🟡 In Progress | ✅ Complete

**Deployment Date:** _______________

**Backend URL:** _______________

**Database URL:** _______________

**Notes:**
