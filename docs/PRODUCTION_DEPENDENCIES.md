# Production Dependencies Verification

## Overview

This document verifies that all required dependencies for the security and QA remediation features are properly installed in production.

## Python Dependencies

All dependencies are listed in `backend/requirements.txt` and installed via Docker during the build process.

### Security Dependencies

| Package | Version | Purpose | Status |
|---------|---------|---------|--------|
| `zxcvbn` | 4.4.28 | Password strength checking | ✅ Installed |
| `slowapi` | 0.1.9 | API rate limiting | ✅ Installed |
| `redis` | 5.0.1 | Token blacklist, rate limiting storage | ✅ Installed |
| `python-jose[cryptography]` | 3.3.0 | JWT token handling | ✅ Installed |
| `passlib[bcrypt]` | 1.7.4 | Password hashing | ✅ Installed |
| `cryptography` | 41.0.7 | Encryption support | ✅ Installed |

### Core Framework Dependencies

| Package | Version | Purpose | Status |
|---------|---------|---------|--------|
| `fastapi` | 0.104.1 | Web framework | ✅ Installed |
| `uvicorn[standard]` | 0.24.0 | ASGI server | ✅ Installed |
| `pydantic` | 2.5.0 | Data validation | ✅ Installed |
| `structlog` | 23.2.0 | Structured logging | ✅ Installed |

### Database Dependencies

| Package | Version | Purpose | Status |
|---------|---------|---------|--------|
| `sqlalchemy` | 2.0.23 | ORM | ✅ Installed |
| `alembic` | 1.12.1 | Database migrations | ✅ Installed |
| `psycopg2-binary` | 2.9.9 | PostgreSQL adapter | ✅ Installed |
| `asyncpg` | 0.30.0 | Async PostgreSQL driver | ✅ Installed |

### Task Queue Dependencies

| Package | Version | Purpose | Status |
|---------|---------|---------|--------|
| `celery` | 5.3.4 | Task queue | ✅ Installed |
| `redis` | 5.0.1 | Celery broker | ✅ Installed |

## System Dependencies

System dependencies are installed in the Dockerfile and are required for production:

### Required System Packages

- `gcc` - Compiler for building Python packages
- `libpq-dev` - PostgreSQL development libraries (for psycopg2)
- `ffmpeg` - Audio processing (for audio QC features)

### Installation

System dependencies are installed in `backend/Dockerfile`:

```dockerfile
RUN apt-get update && apt-get install -y \
    gcc \
    libpq-dev \
    ffmpeg \
    && rm -rf /var/lib/apt/lists/*
```

## Docker Services

Production requires the following Docker services (configured in `docker-compose.yml`):

### Required Services

1. **PostgreSQL Database** (`db`)
   - Image: `postgres:15-alpine`
   - Purpose: Primary data storage
   - Health check: Enabled

2. **Redis** (`redis`)
   - Image: `redis:7-alpine`
   - Purpose: Rate limiting, token blacklist, Celery broker
   - Health check: Enabled

3. **FastAPI Backend** (`api`)
   - Built from `backend/Dockerfile`
   - Depends on: `db`, `redis`
   - Environment variables: See `env.template`

## Environment Variables

Required environment variables for production (see `env.template`):

### Critical Security Variables

- `JWT_SECRET_KEY` - Must be at least 32 characters (validated at startup)
- `POSTGRES_PASSWORD` - Database password (not hardcoded)
- `REDIS_URL` - Redis connection URL
- `APP_ENV` - Set to `production` for production environment

### Security Configuration

- `PASSWORD_MIN_LENGTH` - Minimum password length (default: 12)
- `ACCOUNT_LOCKOUT_ENABLED` - Enable account lockout (default: true)
- `ACCOUNT_LOCKOUT_ATTEMPTS` - Failed attempts before lockout (default: 5)
- `ACCOUNT_LOCKOUT_DURATION_MINUTES` - Lockout duration (default: 15)
- `RATE_LIMIT_ENABLED` - Enable rate limiting (default: true)

## Verification Checklist

Before deploying to production, verify:

- [ ] All Python dependencies installed (`pip install -r requirements.txt`)
- [ ] System dependencies installed (gcc, libpq-dev, ffmpeg)
- [ ] PostgreSQL service running and healthy
- [ ] Redis service running and healthy
- [ ] `JWT_SECRET_KEY` set and at least 32 characters
- [ ] `POSTGRES_PASSWORD` set (not hardcoded)
- [ ] `APP_ENV=production` set
- [ ] All environment variables from `env.template` configured
- [ ] Database migrations applied (`alembic upgrade head`)
- [ ] Docker containers can communicate (network configured)

## Installation Verification

To verify all dependencies are installed correctly:

```bash
# Check Python packages
docker-compose exec api pip list | grep -E "(zxcvbn|slowapi|redis|structlog|fastapi)"

# Check system packages
docker-compose exec api dpkg -l | grep -E "(gcc|libpq|ffmpeg)"

# Verify services are running
docker-compose ps

# Check environment variables
docker-compose exec api env | grep -E "(JWT_SECRET_KEY|APP_ENV|REDIS_URL)"
```

## Troubleshooting

### Missing Dependencies

If a dependency is missing:

1. Add it to `backend/requirements.txt`
2. Rebuild the Docker image: `docker-compose build api`
3. Restart the service: `docker-compose restart api`

### System Dependencies

If a system package is missing:

1. Add it to `backend/Dockerfile` in the `apt-get install` command
2. Rebuild the Docker image: `docker-compose build api`

### Redis Connection Issues

If Redis is not accessible:

1. Verify Redis service is running: `docker-compose ps redis`
2. Check Redis URL: `docker-compose exec api env | grep REDIS_URL`
3. Test connection: `docker-compose exec api python -c "import redis; r=redis.from_url('redis://redis:6379/0'); print(r.ping())"`

## Production Build Process

The production build process ensures all dependencies are installed:

1. **Docker Build**:
   ```bash
   docker-compose build api
   ```
   - Installs system dependencies
   - Installs Python dependencies from `requirements.txt`

2. **Service Startup**:
   ```bash
   docker-compose up -d
   ```
   - Starts PostgreSQL
   - Starts Redis
   - Starts API service (which validates environment variables)

3. **Database Migration**:
   ```bash
   docker-compose exec api alembic upgrade head
   ```
   - Applies all database migrations
   - Creates required tables (including `failed_login_attempts`)

## Notes

- All dependencies are pinned to specific versions for reproducibility
- Development dependencies (pytest, etc.) are included but not required in production
- The Dockerfile uses `--no-cache-dir` to reduce image size
- System packages are cleaned up after installation (`rm -rf /var/lib/apt/lists/*`)










