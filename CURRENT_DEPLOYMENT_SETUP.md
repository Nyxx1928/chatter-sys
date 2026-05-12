# Backend Deployment Setup (Current)

## Platform
- **Host:** Render
- **Plan:** Free (Singapore region)
- **Deployment Type:** Docker

## Architecture

```
GitHub push
    |
    v
Render build -> Docker image -> Web Service (:8080)
                                    |
                            start.sh (converts DATABASE_URL)
                                    |
                            Spring Boot (prod profile)
                                    |
                            Neon PostgreSQL (external)
```

## Key Files

| File | Role |
|------|------|
| `render.yaml` | Render infra-as-code: defines web service, env vars, health check path |
| `Dockerfile` | Multi-stage build: Maven compile -> JRE runtime |
| `start.sh` | Entrypoint: converts `DATABASE_URL` -> `JDBC_DATABASE_URL`, starts JAR |
| `src/main/resources/application.yml` | Spring Boot config with `default`, `dev`, `prod` profiles |
| `src/main/java/org/example/chat/security/SecurityConfig.java` | Permits `/actuator/health` without auth |

## Profiles

| Profile | When | DB URL |
|---------|------|--------|
| `default` | Local dev | `jdbc:postgresql://localhost:5432/chatdb` (hardcoded) |
| `dev` | Local dev (explicit) | Same as default, verbose logging, `create-drop` DDL |
| `prod` | Render | `${JDBC_DATABASE_URL}` (set by `start.sh`) |

## Startup Flow

1. Render runs `ENTRYPOINT ["/app/start.sh"]`
2. `start.sh` checks `DATABASE_URL` is set — exits 1 if missing
3. Parses `postgresql://user:pass@host/db?params` into `jdbc:postgresql://host/db?user=...&password=...&params`
4. Exports `JDBC_DATABASE_URL` and launches `java -jar app.jar --spring.profiles.active=prod`
5. Spring Boot reads `${JDBC_DATABASE_URL}` under `spring.datasource.url` (prod profile)
6. Health endpoint at `/actuator/health/liveness` — **DB + Mail health checks disabled** in prod so it returns `UP` even if external dependencies are cold/unreachable

## Environment Variables (Render Dashboard)

| Variable | Source | Notes |
|----------|--------|-------|
| `SPRING_PROFILES_ACTIVE=prod` | `render.yaml` | Static |
| `DATABASE_URL` | **Must be set manually** | Neon connection string; do not set placeholder values in `render.yaml` |
| `JWT_SECRET` | `render.yaml` (auto-generated) | |
| `CORS_ALLOWED_ORIGINS` | `render.yaml` | Currently `https://chatter-sys.vercel.app` |
| `PORT=8080` | `render.yaml` | Render maps external :443 to internal :8080 |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS` | `render.yaml` | Resend SMTP — values are placeholders |
| `APP_BASE_URL` | `render.yaml` | Placeholder — must be set to actual Render URL |

## Health Check

- **Path:** `/actuator/health/liveness` (Render health check)
- **Security:** Permitted without auth in `SecurityConfig.java:80`
- **Default profile:** `show-details: when-authorized`, includes DB check
- **Prod profile:** `show-details: always`, `health.db.enabled: false`, `health.mail.enabled: false`
- **Render behavior:** Waits for HTTP 200; free plan spins down after 15min idle

## Security

- CSRF disabled (JWT-based auth)
- Stateless sessions
- CORS allows `http://localhost:3000` + `CORS_ALLOWED_ORIGINS` env var value
- Public endpoints: `/api/auth/register`, `/api/auth/login`, `/api/auth/verify-email`, `/api/auth/resend-verification`, `/actuator/health`, `/actuator/info`, `/ws/**`
- All other endpoints require valid JWT

## Database (Neon PostgreSQL)

- External — not managed by Render blueprint
- Connection string format: `postgresql://user:pass@host/db?sslmode=require`
- Converted to JDBC format by `start.sh`
- HikariCP pool: min-idle=2, max=5, timeout=30s

## Deployment Steps

1. Set `DATABASE_URL` in Render dashboard (Environment tab)
2. Set actual `MAIL_PASSWORD` and `APP_BASE_URL` values if using email
3. Push to GitHub — Render auto-deploys via webhook
4. Monitor logs in Render dashboard for startup errors

## Known Issues / Recent Fixes

- **Health check timeout** — Fixed by disabling `management.health.db.enabled` + `management.health.mail.enabled` in prod and using `/actuator/health/liveness` for Render’s health check
- **`DATABASE_URL` not set** — `start.sh` exits 1 if missing; must be configured manually in Render dashboard
