# Render Deployment Troubleshooting

## Common Issues and Solutions

### Issue 1: Database Connection Failed - Invalid Port

**Error Message:**
```
JDBC URL port: -1 not valid (1:65535)
Driver org.postgresql.Driver claims to not accept jdbcUrl
```

**Cause:** Render's `DATABASE_URL` format is not being parsed correctly.

**Solution:** 
The `RenderDatabaseConfig.java` class handles this conversion. Make sure:
1. The `prod` profile is active (`SPRING_PROFILES_ACTIVE=prod`)
2. The `DATABASE_URL` environment variable is set correctly
3. The config class is in the correct package

**Verify in Render Dashboard:**
- Go to your web service
- Check Environment tab
- Ensure `DATABASE_URL` is set (should be auto-populated from database)
- Format should be: `postgresql://user:password@host:port/database`

### Issue 2: Build Fails

**Error Message:**
```
Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin
```

**Solution:**
1. Check Java version in Dockerfile (should be 17)
2. Verify `pom.xml` is valid
3. Try building locally: `mvn clean package`

### Issue 3: Health Check Fails

**Error Message:**
```
Health check failed
```

**Solution:**
1. Wait 2-3 minutes for first deployment
2. Check application logs for startup errors
3. Verify `/actuator/health` endpoint is accessible
4. Ensure Spring Boot Actuator is in `pom.xml`

### Issue 4: Application Won't Start

**Error Message:**
```
Application run failed
```

**Solution:**
1. Check all environment variables are set
2. Verify database is in same region as web service
3. Check logs for specific error messages
4. Ensure `PORT` environment variable is set

### Issue 5: WebSocket Connection Fails

**Error Message:**
```
WebSocket connection failed
```

**Solution:**
1. Use `wss://` (not `ws://`) for HTTPS
2. Check CORS configuration includes frontend domain
3. Verify WebSocket endpoint path is correct
4. Test with wscat: `wscat -c wss://your-backend.onrender.com/ws`

## Debugging Steps

### 1. Check Render Logs

```bash
# In Render Dashboard:
1. Go to your service
2. Click "Logs" tab
3. Look for error messages
4. Search for "ERROR" or "WARN"
```

### 2. Verify Environment Variables

```bash
# In Render Dashboard:
1. Go to your service
2. Click "Environment" tab
3. Verify all required variables are set:
   - SPRING_PROFILES_ACTIVE=prod
   - DATABASE_URL=(auto from database)
   - JWT_SECRET=(auto-generated)
   - CORS_ALLOWED_ORIGINS=(your frontend URL)
   - PORT=8080
```

### 3. Test Database Connection

```bash
# In Render Dashboard:
1. Go to your PostgreSQL database
2. Click "Connect" → "External Connection"
3. Use psql to test connection:
   psql -h <host> -U <user> -d <database>
```

### 4. Check Build Logs

```bash
# In Render Dashboard:
1. Go to your service
2. Click "Events" tab
3. Find latest deploy
4. Click "View Logs"
5. Check for build errors
```

### 5. Verify Health Endpoint

```bash
# After deployment:
curl https://your-backend.onrender.com/actuator/health

# Should return:
{"status":"UP"}
```

## Environment Variable Issues

### DATABASE_URL Not Set

**Symptom:** Application can't connect to database

**Solution:**
1. Go to Render Dashboard
2. Navigate to your web service
3. Go to "Environment" tab
4. Add `DATABASE_URL`:
   - Click "Add Environment Variable"
   - Key: `DATABASE_URL`
   - Value: Copy from your PostgreSQL service (Internal Database URL)
   - Save

### JWT_SECRET Not Set

**Symptom:** Authentication fails

**Solution:**
1. Generate a secret: `openssl rand -base64 32`
2. Add to Render environment variables:
   - Key: `JWT_SECRET`
   - Value: (paste generated secret)
   - Save

### CORS_ALLOWED_ORIGINS Wrong

**Symptom:** Frontend can't connect to backend

**Solution:**
1. Update environment variable:
   - Key: `CORS_ALLOWED_ORIGINS`
   - Value: `https://your-frontend.onrender.com`
   - Save
2. Service will auto-redeploy

## Database Issues

### Connection Timeout

**Symptom:** Database connection times out

**Solution:**
1. Ensure database and web service are in same region
2. Use Internal Database URL (not External)
3. Check database is running (should show "Available")
4. Verify connection string format

### Database Not Found

**Symptom:** Database does not exist

**Solution:**
1. Verify database name matches in:
   - PostgreSQL service
   - `DATABASE_URL`
   - Application configuration
2. Check database was created successfully
3. Try connecting manually with psql

### Authentication Failed

**Symptom:** Database authentication failed

**Solution:**
1. Verify username and password in `DATABASE_URL`
2. Check database user has correct permissions
3. Try resetting database password in Render dashboard

## Build Issues

### Maven Build Fails

**Symptom:** Build fails during Maven package

**Solution:**
1. Check `pom.xml` is valid XML
2. Verify all dependencies are available
3. Try building locally: `mvn clean package -DskipTests`
4. Check Java version matches (17)

### Docker Build Fails

**Symptom:** Docker image build fails

**Solution:**
1. Verify `Dockerfile` syntax
2. Check base image is available
3. Ensure all COPY paths are correct
4. Try building locally: `docker build -t test .`

### Out of Memory

**Symptom:** Build runs out of memory

**Solution:**
1. Upgrade to paid tier (more resources)
2. Reduce build parallelism
3. Add Maven options: `MAVEN_OPTS=-Xmx512m`

## Runtime Issues

### Application Crashes on Startup

**Symptom:** Application starts then immediately crashes

**Solution:**
1. Check logs for stack trace
2. Verify all required beans can be created
3. Check database connection
4. Ensure all environment variables are set

### Port Binding Error

**Symptom:** Port already in use

**Solution:**
1. Ensure `PORT` environment variable is set
2. Verify application uses `${PORT:8080}` in config
3. Don't hardcode port 8080

### Memory Issues

**Symptom:** Out of memory errors

**Solution:**
1. Upgrade to larger instance type
2. Optimize JVM settings
3. Add: `JAVA_OPTS=-Xmx512m -Xms256m`
4. Review memory usage in metrics

## Deployment Issues

### Deploy Stuck

**Symptom:** Deployment hangs

**Solution:**
1. Wait 10 minutes (first deploy can be slow)
2. Check build logs for progress
3. Cancel and retry deploy
4. Contact Render support if persists

### Health Check Never Passes

**Symptom:** Service keeps restarting

**Solution:**
1. Verify `/actuator/health` endpoint works
2. Check application starts successfully
3. Increase health check timeout
4. Review startup logs

### Zero Downtime Deploy Fails

**Symptom:** Old version keeps running

**Solution:**
1. Check new version passes health check
2. Verify no errors in new version logs
3. Manually stop old version if needed

## Performance Issues

### Slow Response Times

**Symptom:** API responses are slow

**Solution:**
1. Check database query performance
2. Add database indexes
3. Enable connection pooling (already configured)
4. Upgrade to larger instance
5. Review metrics in Render dashboard

### High CPU Usage

**Symptom:** CPU at 100%

**Solution:**
1. Review application logs for errors
2. Check for infinite loops
3. Optimize database queries
4. Upgrade instance type

### Database Connection Pool Exhausted

**Symptom:** No available connections

**Solution:**
1. Increase pool size in `application.yml`:
   ```yaml
   spring:
     datasource:
       hikari:
         maximum-pool-size: 20
   ```
2. Check for connection leaks
3. Ensure connections are closed properly

## Getting Help

### Render Support

1. **Documentation:** https://render.com/docs
2. **Community:** https://community.render.com
3. **Support:** support@render.com
4. **Status:** https://status.render.com

### Application Logs

```bash
# View recent logs
1. Render Dashboard → Your Service → Logs
2. Use search to filter
3. Download logs for detailed analysis
```

### Database Logs

```bash
# View database logs
1. Render Dashboard → Your Database → Logs
2. Check for connection errors
3. Review slow queries
```

## Quick Fixes

### Restart Service

```bash
# In Render Dashboard:
1. Go to your service
2. Click "Manual Deploy" → "Clear build cache & deploy"
```

### Redeploy

```bash
# Push to GitHub:
git commit --allow-empty -m "Trigger redeploy"
git push origin main
```

### Check Service Status

```bash
# Test health endpoint:
curl https://your-backend.onrender.com/actuator/health

# Test API endpoint:
curl https://your-backend.onrender.com/api/auth/login
```

## Prevention

### Before Deploying

- [ ] Test locally: `mvn clean package`
- [ ] Run tests: `mvn test`
- [ ] Build Docker image: `docker build -t test .`
- [ ] Test Docker container: `docker run -p 8080:8080 test`
- [ ] Verify environment variables
- [ ] Check database connection string

### After Deploying

- [ ] Monitor logs for errors
- [ ] Test health endpoint
- [ ] Test API endpoints
- [ ] Test WebSocket connection
- [ ] Verify database connection
- [ ] Check metrics in dashboard

### Regular Maintenance

- [ ] Review logs weekly
- [ ] Monitor resource usage
- [ ] Update dependencies monthly
- [ ] Backup database regularly
- [ ] Test disaster recovery

## Common Error Messages

| Error | Cause | Solution |
|-------|-------|----------|
| `Port already in use` | Port hardcoded | Use `${PORT:8080}` |
| `Connection refused` | Database not accessible | Check DATABASE_URL |
| `Authentication failed` | Wrong credentials | Verify username/password |
| `Health check failed` | App not starting | Check logs |
| `Out of memory` | Insufficient resources | Upgrade instance |
| `Build timeout` | Build too slow | Optimize build |

---

**Still having issues?** Check the detailed deployment guide in [RENDER_DEPLOYMENT.md](RENDER_DEPLOYMENT.md) or contact Render support.
