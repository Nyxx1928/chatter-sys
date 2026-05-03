# Deployment Documentation

This document provides comprehensive instructions for deploying the Real-Time Chat System to production environments.

## Table of Contents

1. [System Requirements](#system-requirements)
2. [Environment Configuration](#environment-configuration)
3. [Backend Deployment](#backend-deployment)
4. [Frontend Deployment](#frontend-deployment)
5. [Database Setup](#database-setup)
6. [Security Configuration](#security-configuration)
7. [Monitoring and Logging](#monitoring-and-logging)
8. [Troubleshooting](#troubleshooting)

## System Requirements

### Backend Requirements
- **Java**: OpenJDK 21 or later
- **Memory**: Minimum 512MB RAM, recommended 1GB+
- **CPU**: 1 core minimum, 2+ cores recommended
- **Disk**: 500MB for application + logs
- **Database**: PostgreSQL 13 or later

### Frontend Requirements
- **Node.js**: 18.x or later
- **Memory**: 256MB RAM minimum
- **Disk**: 200MB for build artifacts
- **Web Server**: Nginx, Apache, or Node.js server

### Network Requirements
- **Backend Port**: 8080 (configurable)
- **Frontend Port**: 3000 (development), 80/443 (production)
- **WebSocket**: Port 8080 (same as backend)
- **Database Port**: 5432 (PostgreSQL default)

## Environment Configuration

### Backend Environment Variables

Create a `.env` file or set environment variables:

```bash
# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=chatdb
DB_USERNAME=chatuser
DB_PASSWORD=your_secure_password

# JWT Configuration
JWT_SECRET=your_very_long_and_secure_secret_key_here_at_least_256_bits
JWT_EXPIRATION=86400000  # 24 hours in milliseconds

# Server Configuration
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=prod

# CORS Configuration
CORS_ALLOWED_ORIGINS=https://your-frontend-domain.com

# Logging Configuration
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_ORG_EXAMPLE_CHAT=INFO
LOGGING_FILE_PATH=./logs
```

### Frontend Environment Variables

Create a `.env.production` file:

```bash
# API Configuration
NEXT_PUBLIC_API_URL=https://your-backend-domain.com
NEXT_PUBLIC_WS_URL=https://your-backend-domain.com/ws

# Application Configuration
NEXT_PUBLIC_APP_NAME=Real-Time Chat
NEXT_PUBLIC_MAX_MESSAGE_LENGTH=2000
```

## Backend Deployment

### Option 1: JAR Deployment (Recommended)

#### 1. Build the Application

```bash
# Navigate to project root
cd /path/to/realtime-chat-system

# Build with Maven
mvn clean package -DskipTests

# Or build with tests
mvn clean package
```

**Output:** `target/chat-application-0.0.1-SNAPSHOT.jar`

#### 2. Transfer to Server

```bash
# Using SCP
scp target/chat-application-0.0.1-SNAPSHOT.jar user@server:/opt/chat-app/

# Or using rsync
rsync -avz target/chat-application-0.0.1-SNAPSHOT.jar user@server:/opt/chat-app/
```

#### 3. Create Systemd Service

Create `/etc/systemd/system/chat-app.service`:

```ini
[Unit]
Description=Real-Time Chat Application
After=network.target postgresql.service

[Service]
Type=simple
User=chatapp
Group=chatapp
WorkingDirectory=/opt/chat-app
ExecStart=/usr/bin/java -jar /opt/chat-app/chat-application-0.0.1-SNAPSHOT.jar
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=chat-app

# Environment variables
Environment="SPRING_PROFILES_ACTIVE=prod"
Environment="DB_HOST=localhost"
Environment="DB_PORT=5432"
Environment="DB_NAME=chatdb"
Environment="DB_USERNAME=chatuser"
Environment="DB_PASSWORD=your_secure_password"
Environment="JWT_SECRET=your_very_long_and_secure_secret_key"
Environment="CORS_ALLOWED_ORIGINS=https://your-frontend-domain.com"

# Resource limits
LimitNOFILE=65536
MemoryLimit=1G

[Install]
WantedBy=multi-user.target
```

#### 4. Start the Service

```bash
# Create user for running the application
sudo useradd -r -s /bin/false chatapp
sudo chown -R chatapp:chatapp /opt/chat-app

# Reload systemd
sudo systemctl daemon-reload

# Enable service to start on boot
sudo systemctl enable chat-app

# Start the service
sudo systemctl start chat-app

# Check status
sudo systemctl status chat-app

# View logs
sudo journalctl -u chat-app -f
```

### Option 2: Docker Deployment

#### 1. Build Docker Image

```bash
# Build backend image
docker build -t chat-backend:latest .

# Or using docker-compose
docker-compose build backend
```

#### 2. Run with Docker Compose

Create `docker-compose.yml`:

```yaml
version: '3.8'

services:
  database:
    image: postgres:15-alpine
    container_name: chat-db
    environment:
      POSTGRES_DB: chatdb
      POSTGRES_USER: chatuser
      POSTGRES_PASSWORD: your_secure_password
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    networks:
      - chat-network
    restart: unless-stopped

  backend:
    image: chat-backend:latest
    container_name: chat-backend
    depends_on:
      - database
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_HOST: database
      DB_PORT: 5432
      DB_NAME: chatdb
      DB_USERNAME: chatuser
      DB_PASSWORD: your_secure_password
      JWT_SECRET: your_very_long_and_secure_secret_key
      CORS_ALLOWED_ORIGINS: https://your-frontend-domain.com
    ports:
      - "8080:8080"
    networks:
      - chat-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  frontend:
    image: chat-frontend:latest
    container_name: chat-frontend
    depends_on:
      - backend
    environment:
      NEXT_PUBLIC_API_URL: https://your-backend-domain.com
      NEXT_PUBLIC_WS_URL: https://your-backend-domain.com/ws
    ports:
      - "3000:3000"
    networks:
      - chat-network
    restart: unless-stopped

networks:
  chat-network:
    driver: bridge

volumes:
  postgres_data:
```

#### 3. Start Services

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

## Frontend Deployment

### Option 1: Static Export (Recommended for CDN)

#### 1. Build for Production

```bash
# Navigate to frontend directory
cd frontend

# Install dependencies
npm install

# Build for production
npm run build

# Export static files (if using static export)
npm run export
```

**Output:** `frontend/out/` or `frontend/.next/` directory

#### 2. Deploy to Nginx

Install and configure Nginx:

```bash
# Install Nginx
sudo apt-get update
sudo apt-get install nginx
```

Create Nginx configuration `/etc/nginx/sites-available/chat-frontend`:

```nginx
server {
    listen 80;
    server_name your-frontend-domain.com;

    # Redirect HTTP to HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name your-frontend-domain.com;

    # SSL Configuration
    ssl_certificate /etc/letsencrypt/live/your-frontend-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-frontend-domain.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    # Root directory
    root /var/www/chat-frontend;
    index index.html;

    # Gzip compression
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "no-referrer-when-downgrade" always;

    # Next.js static files
    location /_next/static/ {
        alias /var/www/chat-frontend/.next/static/;
        expires 1y;
        access_log off;
        add_header Cache-Control "public, immutable";
    }

    # Static assets
    location /static/ {
        alias /var/www/chat-frontend/public/;
        expires 1y;
        access_log off;
        add_header Cache-Control "public, immutable";
    }

    # API proxy (optional, if backend on different domain)
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # WebSocket proxy
    location /ws {
        proxy_pass http://localhost:8080/ws;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 86400;
    }

    # Next.js pages
    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

Enable the site:

```bash
# Create symbolic link
sudo ln -s /etc/nginx/sites-available/chat-frontend /etc/nginx/sites-enabled/

# Test configuration
sudo nginx -t

# Reload Nginx
sudo systemctl reload nginx
```

#### 3. Deploy Files

```bash
# Copy build files to web root
sudo mkdir -p /var/www/chat-frontend
sudo cp -r frontend/out/* /var/www/chat-frontend/
# Or for Next.js standalone
sudo cp -r frontend/.next /var/www/chat-frontend/

# Set permissions
sudo chown -R www-data:www-data /var/www/chat-frontend
```

### Option 2: Node.js Server

#### 1. Build and Start

```bash
# Build for production
npm run build

# Start production server
npm run start
```

#### 2. Use PM2 for Process Management

```bash
# Install PM2 globally
npm install -g pm2

# Start application
pm2 start npm --name "chat-frontend" -- start

# Save PM2 configuration
pm2 save

# Setup PM2 to start on boot
pm2 startup

# Monitor
pm2 monit

# View logs
pm2 logs chat-frontend
```

## Database Setup

### PostgreSQL Installation

```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install postgresql postgresql-contrib

# Start PostgreSQL
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

### Database Configuration

```bash
# Switch to postgres user
sudo -u postgres psql

# Create database and user
CREATE DATABASE chatdb;
CREATE USER chatuser WITH ENCRYPTED PASSWORD 'your_secure_password';
GRANT ALL PRIVILEGES ON DATABASE chatdb TO chatuser;

# Exit psql
\q
```

### Database Migrations

The application uses Hibernate with `ddl-auto: update` for development. For production:

1. **Option 1**: Use Flyway or Liquibase for versioned migrations
2. **Option 2**: Generate schema manually:

```bash
# Export schema from development
pg_dump -U chatuser -s chatdb > schema.sql

# Import to production
psql -U chatuser -d chatdb < schema.sql
```

### Database Backup

```bash
# Create backup script
cat > /opt/scripts/backup-chatdb.sh << 'EOF'
#!/bin/bash
BACKUP_DIR="/var/backups/chatdb"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
mkdir -p $BACKUP_DIR
pg_dump -U chatuser chatdb | gzip > $BACKUP_DIR/chatdb_$TIMESTAMP.sql.gz
# Keep only last 7 days
find $BACKUP_DIR -name "chatdb_*.sql.gz" -mtime +7 -delete
EOF

# Make executable
chmod +x /opt/scripts/backup-chatdb.sh

# Add to crontab (daily at 2 AM)
echo "0 2 * * * /opt/scripts/backup-chatdb.sh" | crontab -
```

## Security Configuration

### SSL/TLS Certificates

#### Using Let's Encrypt (Recommended)

```bash
# Install Certbot
sudo apt-get install certbot python3-certbot-nginx

# Obtain certificate
sudo certbot --nginx -d your-frontend-domain.com -d your-backend-domain.com

# Auto-renewal is configured automatically
# Test renewal
sudo certbot renew --dry-run
```

### Firewall Configuration

```bash
# Using UFW (Ubuntu)
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw enable

# Block direct access to backend (if using reverse proxy)
sudo ufw deny 8080/tcp
```

### Application Security

#### 1. JWT Secret

Generate a strong JWT secret:

```bash
# Generate 256-bit secret
openssl rand -base64 32
```

Use this value for `JWT_SECRET` environment variable.

#### 2. Database Password

Use a strong database password:

```bash
# Generate strong password
openssl rand -base64 24
```

#### 3. CORS Configuration

Update `application.yml` or environment variables:

```yaml
cors:
  allowed-origins: https://your-frontend-domain.com
  allowed-methods: GET,POST,PUT,DELETE,OPTIONS
  allowed-headers: Authorization,Content-Type
  max-age: 3600
```

#### 4. Rate Limiting (Optional)

Add rate limiting to prevent abuse:

```java
// Add to pom.xml
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.0.1</version>
</dependency>
```

## Monitoring and Logging

### Application Logging

Configure logging in `application-prod.yml`:

```yaml
logging:
  level:
    root: INFO
    org.example.chat: INFO
  file:
    name: /var/log/chat-app/application.log
    max-size: 10MB
    max-history: 30
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### Log Rotation

Create `/etc/logrotate.d/chat-app`:

```
/var/log/chat-app/*.log {
    daily
    rotate 30
    compress
    delaycompress
    notifempty
    create 0640 chatapp chatapp
    sharedscripts
    postrotate
        systemctl reload chat-app > /dev/null 2>&1 || true
    endscript
}
```

### Health Checks

Add Spring Boot Actuator for health monitoring:

```xml
<!-- Add to pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Configure in `application-prod.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized
```

Access health endpoint: `https://your-backend-domain.com/actuator/health`

### Monitoring Tools (Optional)

- **Prometheus + Grafana**: Metrics and dashboards
- **ELK Stack**: Centralized logging
- **Sentry**: Error tracking
- **Uptime Robot**: Uptime monitoring

## Troubleshooting

### Backend Issues

#### Application Won't Start

```bash
# Check logs
sudo journalctl -u chat-app -n 100

# Common issues:
# 1. Port already in use
sudo lsof -i :8080

# 2. Database connection failed
sudo systemctl status postgresql
psql -U chatuser -d chatdb -c "SELECT 1"

# 3. Permission issues
sudo chown -R chatapp:chatapp /opt/chat-app
```

#### WebSocket Connection Fails

```bash
# Check CORS configuration
# Verify allowed origins include frontend domain

# Check firewall
sudo ufw status

# Test WebSocket connection
wscat -c ws://localhost:8080/ws
```

### Frontend Issues

#### Build Fails

```bash
# Clear cache and rebuild
rm -rf .next node_modules
npm install
npm run build
```

#### Static Files Not Loading

```bash
# Check Nginx configuration
sudo nginx -t

# Check file permissions
ls -la /var/www/chat-frontend

# Check Nginx error log
sudo tail -f /var/log/nginx/error.log
```

### Database Issues

#### Connection Pool Exhausted

```yaml
# Increase pool size in application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
```

#### Slow Queries

```sql
-- Enable query logging
ALTER DATABASE chatdb SET log_statement = 'all';
ALTER DATABASE chatdb SET log_duration = on;

-- Check slow queries
SELECT * FROM pg_stat_statements ORDER BY total_time DESC LIMIT 10;
```

## Performance Optimization

### Backend Optimization

1. **Enable Connection Pooling**: Already configured with HikariCP
2. **Add Database Indexes**: Ensure indexes on frequently queried columns
3. **Enable Caching**: Add Redis for session/data caching
4. **Optimize Queries**: Use pagination, avoid N+1 queries

### Frontend Optimization

1. **Enable Compression**: Gzip/Brotli in Nginx
2. **CDN**: Use CDN for static assets
3. **Code Splitting**: Next.js does this automatically
4. **Image Optimization**: Use Next.js Image component

### Database Optimization

```sql
-- Add indexes for common queries
CREATE INDEX idx_messages_room_timestamp ON messages(chat_room_id, timestamp DESC);
CREATE INDEX idx_room_memberships_user ON room_memberships(user_id);
CREATE INDEX idx_room_memberships_room ON room_memberships(chat_room_id);

-- Analyze tables
ANALYZE messages;
ANALYZE chat_rooms;
ANALYZE users;
```

## Scaling Considerations

For scaling beyond 20 concurrent users:

1. **Load Balancing**: Use Nginx or HAProxy for multiple backend instances
2. **Session Stickiness**: Required for WebSocket connections
3. **Redis**: For distributed session storage
4. **Message Queue**: RabbitMQ or Kafka for message broadcasting
5. **Database Replication**: PostgreSQL read replicas
6. **Horizontal Scaling**: Multiple backend instances with shared database

## Rollback Procedure

If deployment fails:

```bash
# Backend rollback
sudo systemctl stop chat-app
sudo cp /opt/chat-app/chat-application-previous.jar /opt/chat-app/chat-application-0.0.1-SNAPSHOT.jar
sudo systemctl start chat-app

# Frontend rollback
sudo rm -rf /var/www/chat-frontend/*
sudo cp -r /var/www/chat-frontend-backup/* /var/www/chat-frontend/
sudo systemctl reload nginx

# Database rollback
psql -U chatuser -d chatdb < /var/backups/chatdb/chatdb_TIMESTAMP.sql
```

## Support and Maintenance

### Regular Maintenance Tasks

- **Daily**: Check application logs for errors
- **Weekly**: Review database performance, check disk space
- **Monthly**: Update dependencies, security patches
- **Quarterly**: Review and optimize database queries

### Update Procedure

1. Backup database
2. Test updates in staging environment
3. Schedule maintenance window
4. Deploy updates
5. Verify functionality
6. Monitor for issues

## Conclusion

This deployment guide covers the essential steps for deploying the Real-Time Chat System to production. Always test deployments in a staging environment before production, and maintain regular backups.

For additional support, refer to:
- Spring Boot Documentation: https://spring.io/projects/spring-boot
- Next.js Documentation: https://nextjs.org/docs
- PostgreSQL Documentation: https://www.postgresql.org/docs/
