# Deploying Backend to Render

This guide walks you through deploying the Real-Time Chat System backend to Render.

## Prerequisites

1. **Render Account**: Sign up at [render.com](https://render.com)
2. **GitHub Repository**: Your code should be in a GitHub repository
3. **Git**: Ensure your latest changes are committed and pushed

## Deployment Options

### Option 1: Using render.yaml (Recommended - Infrastructure as Code)

This method uses the `render.yaml` file to define your infrastructure.

#### Step 1: Prepare Your Repository

Ensure these files are in your repository root:
- `render.yaml` (already created)
- `Dockerfile` (already exists)
- `pom.xml` (already exists)

#### Step 2: Connect to Render

1. Go to [Render Dashboard](https://dashboard.render.com)
2. Click **"New +"** → **"Blueprint"**
3. Connect your GitHub repository
4. Render will automatically detect the `render.yaml` file

#### Step 3: Configure Environment Variables

Render will create the services defined in `render.yaml`. You'll need to update:

1. **JWT_SECRET**: Render will auto-generate this, or you can set a custom value
2. **CORS_ALLOWED_ORIGINS**: Update with your frontend URL once deployed

#### Step 4: Deploy

1. Click **"Apply"** to create the services
2. Render will:
   - Create a PostgreSQL database
   - Build your Docker image
   - Deploy the backend service
   - Set up environment variables automatically

#### Step 5: Get Your Backend URL

Once deployed, your backend will be available at:
```
https://chat-backend.onrender.com
```

### Option 2: Manual Deployment (Step-by-Step)

If you prefer manual setup:

#### Step 1: Create PostgreSQL Database

1. In Render Dashboard, click **"New +"** → **"PostgreSQL"**
2. Configure:
   - **Name**: `chat-db`
   - **Database**: `chatdb`
   - **User**: `chatuser`
   - **Region**: Choose closest to your users
   - **Plan**: Free (or paid for production)
3. Click **"Create Database"**
4. Save the connection details (Internal Database URL)

#### Step 2: Create Web Service

1. Click **"New +"** → **"Web Service"**
2. Connect your GitHub repository
3. Configure:
   - **Name**: `chat-backend`
   - **Region**: Same as database
   - **Branch**: `main` (or your default branch)
   - **Root Directory**: Leave empty (or specify if backend is in subdirectory)
   - **Environment**: `Docker`
   - **Plan**: Free (or paid for production)

#### Step 3: Configure Build Settings

Render will auto-detect your Dockerfile. If not:
- **Dockerfile Path**: `./Dockerfile`

#### Step 4: Add Environment Variables

In the "Environment" section, add:

| Key | Value |
|-----|-------|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DATABASE_URL` | Copy from PostgreSQL service (Internal Database URL) |
| `DB_USERNAME` | `chatuser` (from database) |
| `DB_PASSWORD` | Copy from PostgreSQL service |
| `JWT_SECRET` | Generate using: `openssl rand -base64 32` |
| `CORS_ALLOWED_ORIGINS` | `https://your-frontend.onrender.com` |
| `PORT` | `8080` |

**Important**: Use the **Internal Database URL** from your Render PostgreSQL service, not localhost.

#### Step 5: Configure Health Check

- **Health Check Path**: `/actuator/health`

#### Step 6: Deploy

1. Click **"Create Web Service"**
2. Render will:
   - Clone your repository
   - Build the Docker image
   - Start the service
3. Monitor the logs for any errors

## Database Connection Configuration

Render provides the database URL in this format:
```
postgresql://user:password@host:port/database
```

Your `application.yml` needs to parse this. Update it to support Render's `DATABASE_URL`:

### Option A: Use DATABASE_URL directly

Add this to your `application.yml`:

```yaml
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/chatdb}
```

**Note**: Render's `DATABASE_URL` starts with `postgresql://`, but Spring Boot expects `jdbc:postgresql://`. You may need to add `jdbc:` prefix.

### Option B: Parse DATABASE_URL in code

Create a configuration class to parse Render's DATABASE_URL format.

## Post-Deployment Configuration

### 1. Update CORS Origins

Once your frontend is deployed, update the `CORS_ALLOWED_ORIGINS` environment variable:

1. Go to your backend service in Render
2. Navigate to **"Environment"**
3. Update `CORS_ALLOWED_ORIGINS` with your frontend URL
4. Save (service will auto-redeploy)

### 2. Set Up Custom Domain (Optional)

1. In your service settings, go to **"Settings"** → **"Custom Domain"**
2. Add your domain (e.g., `api.yourdomain.com`)
3. Update DNS records as instructed
4. Render provides free SSL certificates

### 3. Enable Health Checks

Render automatically monitors your health check endpoint. Ensure Spring Boot Actuator is enabled:

Add to `pom.xml` if not already present:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### 4. Configure Logging

Render captures stdout/stderr. Your application logs will appear in the Render dashboard under **"Logs"**.

## Important Render-Specific Notes

### Free Tier Limitations

- **Spin Down**: Free services spin down after 15 minutes of inactivity
- **Spin Up**: Takes 30-60 seconds to wake up on first request
- **Database**: 90-day expiration on free PostgreSQL databases
- **Build Minutes**: Limited build minutes per month

### Database URL Format

Render provides `DATABASE_URL` in this format:
```
postgresql://user:password@host:port/database
```

Spring Boot expects:
```
jdbc:postgresql://host:port/database
```

You'll need to handle this conversion.

### Port Configuration

Render automatically assigns a port and sets the `PORT` environment variable. Your application should listen on this port:

```yaml
server:
  port: ${PORT:8080}
```

This is already configured in your `application.yml`.

## Troubleshooting

### Build Fails

**Issue**: Maven build fails
**Solution**: 
- Check build logs in Render dashboard
- Ensure `pom.xml` is valid
- Try building locally: `mvn clean package`

### Database Connection Fails

**Issue**: Application can't connect to database
**Solution**:
- Verify `DATABASE_URL` is set correctly
- Use **Internal Database URL** from Render PostgreSQL service
- Check database credentials
- Ensure database is in the same region

### Application Won't Start

**Issue**: Service shows as "Deploy failed"
**Solution**:
- Check logs in Render dashboard
- Verify all environment variables are set
- Ensure `PORT` environment variable is used
- Check health check endpoint is accessible

### WebSocket Connection Issues

**Issue**: WebSocket connections fail
**Solution**:
- Render supports WebSockets on all plans
- Ensure CORS is configured correctly
- Check that frontend is using `wss://` (not `ws://`)
- Verify WebSocket endpoint path

### Health Check Fails

**Issue**: Service keeps restarting
**Solution**:
- Verify `/actuator/health` endpoint works
- Check Spring Boot Actuator is included
- Ensure application starts successfully
- Review startup logs

## Monitoring and Maintenance

### View Logs

1. Go to your service in Render dashboard
2. Click **"Logs"** tab
3. View real-time logs or search historical logs

### Monitor Performance

1. Go to **"Metrics"** tab
2. View:
   - CPU usage
   - Memory usage
   - Request count
   - Response times

### Database Backups

Render automatically backs up PostgreSQL databases:
- **Free tier**: Daily backups, 7-day retention
- **Paid tiers**: More frequent backups, longer retention

### Scaling

To scale your application:
1. Go to **"Settings"**
2. Change **"Instance Type"** to a larger plan
3. Enable **"Auto-Scaling"** (paid plans only)

## Environment Variables Reference

| Variable | Description | Example |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Spring profile to use | `prod` |
| `DATABASE_URL` | PostgreSQL connection string | `postgresql://user:pass@host:5432/db` |
| `DB_USERNAME` | Database username | `chatuser` |
| `DB_PASSWORD` | Database password | `generated_password` |
| `JWT_SECRET` | Secret key for JWT tokens | `base64_encoded_secret` |
| `CORS_ALLOWED_ORIGINS` | Allowed CORS origins | `https://frontend.onrender.com` |
| `PORT` | Port to listen on | `8080` |

## Next Steps

After deploying the backend:

1. **Test the API**: Use curl or Postman to test endpoints
   ```bash
   curl https://chat-backend.onrender.com/actuator/health
   ```

2. **Deploy Frontend**: Deploy your Next.js frontend to Render or Vercel

3. **Update Frontend Config**: Point frontend to your backend URL

4. **Test WebSocket**: Verify WebSocket connections work

5. **Set Up Monitoring**: Configure alerts for downtime

6. **Custom Domain**: Add custom domain for production

## Cost Optimization

### Free Tier Strategy

- Use free tier for development/testing
- Upgrade to paid tier for production
- Keep database in same region as backend
- Monitor usage to avoid overages

### Paid Tier Benefits

- No spin down
- More resources (CPU/RAM)
- Auto-scaling
- Better support
- Longer database retention

## Security Best Practices

1. **JWT Secret**: Use a strong, randomly generated secret
2. **Database Password**: Use Render's generated passwords
3. **CORS**: Only allow your frontend domain
4. **HTTPS**: Render provides free SSL (always use HTTPS)
5. **Environment Variables**: Never commit secrets to Git
6. **Database Access**: Use internal URLs, not public

## Support and Resources

- **Render Docs**: https://render.com/docs
- **Render Community**: https://community.render.com
- **Spring Boot on Render**: https://render.com/docs/deploy-spring-boot
- **Status Page**: https://status.render.com

## Deployment Checklist

- [ ] Code pushed to GitHub
- [ ] `render.yaml` configured
- [ ] PostgreSQL database created
- [ ] Environment variables set
- [ ] Health check endpoint working
- [ ] CORS configured
- [ ] JWT secret generated
- [ ] Build successful
- [ ] Application starts without errors
- [ ] Database connection working
- [ ] API endpoints responding
- [ ] WebSocket connections working
- [ ] Frontend updated with backend URL
- [ ] End-to-end testing completed

## Quick Deploy Commands

If you need to trigger a manual deploy:

```bash
# Commit and push changes
git add .
git commit -m "Deploy to Render"
git push origin main

# Render will auto-deploy on push
```

Or use Render's manual deploy:
1. Go to service in dashboard
2. Click **"Manual Deploy"** → **"Deploy latest commit"**

---

**Congratulations!** Your backend should now be deployed on Render. 🎉

For frontend deployment, see `frontend/README.md` or create a similar guide for Vercel/Render.
