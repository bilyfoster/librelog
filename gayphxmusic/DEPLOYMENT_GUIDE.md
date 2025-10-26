# GayPHX Music Platform - Deployment Guide

**Version:** 2.0.0  
**Last Updated:** October 25, 2025

## 🚀 Production Deployment Checklist

### ✅ Pre-Deployment Requirements

#### **1. Environment Setup**
- [ ] **Production Server** - Ubuntu 20.04+ or similar
- [ ] **Docker & Docker Compose** - Latest stable version
- [ ] **Domain Name** - Configured with SSL certificate
- [ ] **Email Service** - Production SMTP server configured
- [ ] **Database** - PostgreSQL 15+ (production instance)
- [ ] **Storage** - S3-compatible storage (AWS S3, MinIO, etc.)

#### **2. Security Configuration**
- [ ] **Strong Passwords** - All default passwords changed
- [ ] **JWT Secret** - Cryptographically secure random string
- [ ] **Database Password** - Strong, unique password
- [ ] **Storage Keys** - Secure access/secret keys
- [ ] **SSL Certificate** - Valid SSL certificate installed
- [ ] **Firewall** - Only necessary ports open (80, 443, 22)

#### **3. LibreTime Integration**
- [ ] **LibreTime Server** - Radio automation system running
- [ ] **API Key** - Valid LibreTime API key obtained
- [ ] **Network Access** - Backend can reach LibreTime server
- [ ] **API Endpoints** - LibreTime API endpoints verified

### 🔧 Environment Variables

Create a `.env` file with the following production values:

```bash
# Database Configuration
POSTGRES_PASSWORD=your_secure_production_password
POSTGRES_DB=gayphx_music
POSTGRES_USER=gayphx

# MinIO/S3 Storage
MINIO_ACCESS_KEY=your_production_access_key
MINIO_SECRET_KEY=your_production_secret_key
MINIO_BUCKET=gayphx-music-prod

# Email Configuration
SMTP_URL=smtp://user:pass@smtp.yourdomain.com:587
SMTP_FROM_EMAIL=music@gayphx.com
SMTP_FROM_NAME=GayPHX Music

# JWT Security
JWT_SECRET=your_super_secure_jwt_secret_key_here
JWT_ALGORITHM=HS256
JWT_EXPIRE_MINUTES=1440

# ISRC Configuration
ISRC_COUNTRY=US
ISRC_REGISTRANT=GPHX
ISRC_KEY=ABC

# LibreTime Integration
LIBRETIME_URL=https://your-libretime-server.com
LIBRETIME_API_KEY=your_libretime_api_key

# Application URLs
FRONTEND_URL=https://music.gayphx.com
BACKEND_URL=https://api.gayphx.com

# Security
CORS_ORIGINS=https://music.gayphx.com,https://www.gayphx.com
ALLOWED_HOSTS=music.gayphx.com,api.gayphx.com

# Production Settings
NODE_ENV=production
PYTHON_ENV=production
DEBUG=false
```

### 🐳 Docker Configuration

#### **Production docker-compose.yml**
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER}"]
      interval: 30s
      timeout: 10s
      retries: 3

  backend:
    build: ./backend
    environment:
      - DATABASE_URL=postgresql://${POSTGRES_USER}:${POSTGRES_PASSWORD}@postgres:5432/${POSTGRES_DB}
      - JWT_SECRET=${JWT_SECRET}
      - MINIO_ACCESS_KEY=${MINIO_ACCESS_KEY}
      - MINIO_SECRET_KEY=${MINIO_SECRET_KEY}
      - SMTP_URL=${SMTP_URL}
      - ISRC_COUNTRY=${ISRC_COUNTRY}
      - ISRC_REGISTRANT=${ISRC_REGISTRANT}
      - ISRC_KEY=${ISRC_KEY}
      - LIBRETIME_URL=${LIBRETIME_URL}
      - LIBRETIME_API_KEY=${LIBRETIME_API_KEY}
    depends_on:
      postgres:
        condition: service_healthy
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8000/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  frontend:
    build: ./frontend
    environment:
      - NODE_ENV=production
      - NEXT_PUBLIC_API_URL=${BACKEND_URL}
    depends_on:
      - backend
    restart: unless-stopped

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf
      - ./nginx/ssl:/etc/nginx/ssl
    depends_on:
      - frontend
      - backend
    restart: unless-stopped

volumes:
  postgres_data:
```

### 🗄️ Database Setup

#### **1. Run Migrations**
```bash
# Start services
docker compose up -d postgres

# Wait for database to be ready
sleep 30

# Run migrations
docker compose exec backend alembic upgrade head

# Create initial admin user
docker compose exec backend python scripts/seed-admin.py
```

#### **2. Verify Database**
```bash
# Check database connection
docker compose exec postgres psql -U gayphx -d gayphx_music -c "\dt"

# Verify tables exist
docker compose exec postgres psql -U gayphx -d gayphx_music -c "
SELECT table_name FROM information_schema.tables 
WHERE table_schema = 'public' 
ORDER BY table_name;
"
```

### 🔐 Security Hardening

#### **1. Nginx Configuration**
```nginx
server {
    listen 80;
    server_name music.gayphx.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name music.gayphx.com;

    ssl_certificate /etc/nginx/ssl/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/key.pem;
    
    # Security headers
    add_header X-Frame-Options DENY;
    add_header X-Content-Type-Options nosniff;
    add_header X-XSS-Protection "1; mode=block";
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains";

    # Rate limiting
    limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;
    limit_req_zone $binary_remote_addr zone=login:10m rate=1r/s;

    # Frontend
    location / {
        proxy_pass http://frontend:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # API
    location /api/ {
        limit_req zone=api burst=20 nodelay;
        proxy_pass http://backend:8000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Admin login rate limiting
    location /api/admin/login {
        limit_req zone=login burst=5 nodelay;
        proxy_pass http://backend:8000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

#### **2. Firewall Configuration**
```bash
# UFW Firewall
sudo ufw enable
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw deny 8000/tcp   # Block direct backend access
sudo ufw deny 3000/tcp   # Block direct frontend access
```

### 📊 Monitoring & Logging

#### **1. Health Checks**
```bash
# Backend health
curl -f https://api.gayphx.com/health

# Frontend health
curl -f https://music.gayphx.com/

# Database health
docker compose exec postgres pg_isready -U gayphx
```

#### **2. Log Monitoring**
```bash
# View all logs
docker compose logs -f

# View specific service logs
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f postgres

# Monitor error logs
docker compose logs -f backend | grep ERROR
```

#### **3. System Monitoring**
```bash
# Check disk space
df -h

# Check memory usage
free -h

# Check running containers
docker ps

# Check container health
docker compose ps
```

### 🔄 Backup Strategy

#### **1. Database Backups**
```bash
# Create backup script
cat > backup_db.sh << 'EOF'
#!/bin/bash
BACKUP_DIR="/backups/gayphx"
DATE=$(date +%Y%m%d_%H%M%S)
mkdir -p $BACKUP_DIR

# Create database backup
docker compose exec postgres pg_dump -U gayphx gayphx_music > $BACKUP_DIR/db_backup_$DATE.sql

# Compress backup
gzip $BACKUP_DIR/db_backup_$DATE.sql

# Keep only last 30 days
find $BACKUP_DIR -name "db_backup_*.sql.gz" -mtime +30 -delete
EOF

chmod +x backup_db.sh

# Add to crontab (daily at 2 AM)
echo "0 2 * * * /path/to/backup_db.sh" | crontab -
```

#### **2. File Storage Backups**
```bash
# Backup MinIO data
docker compose exec minio mc mirror /data /backup/gayphx-music-$(date +%Y%m%d)
```

### 🚀 Deployment Steps

#### **1. Initial Deployment**
```bash
# Clone repository
git clone <repository-url>
cd gayphxmusic

# Create production environment file
cp .env.example .env
# Edit .env with production values

# Start services
docker compose up -d

# Wait for services to be ready
sleep 60

# Run database migrations
docker compose exec backend alembic upgrade head

# Create admin user
docker compose exec backend python scripts/seed-admin.py

# Verify deployment
curl -f https://music.gayphx.com/
curl -f https://api.gayphx.com/health
```

#### **2. Update Deployment**
```bash
# Pull latest changes
git pull origin main

# Rebuild and restart services
docker compose up -d --build

# Run any new migrations
docker compose exec backend alembic upgrade head

# Verify update
curl -f https://music.gayphx.com/
```

### 🔧 Post-Deployment Configuration

#### **1. LibreTime Integration**
1. Access admin dashboard: https://music.gayphx.com/admin
2. Go to LibreTime Configuration
3. Enter LibreTime URL and API key
4. Test connection and save configuration

#### **2. ISRC Key Configuration**
1. Go to ISRC Key Management
2. Enter your 3-character ISRC registration key
3. Save configuration

#### **3. Email Configuration**
1. Test email delivery
2. Verify magic link authentication works
3. Check notification emails

### 📈 Performance Optimization

#### **1. Database Optimization**
```sql
-- Add indexes for better performance
CREATE INDEX CONCURRENTLY idx_submissions_artist_status ON submissions(artist_id, status);
CREATE INDEX CONCURRENTLY idx_play_logs_played_at ON play_logs(played_at);
CREATE INDEX CONCURRENTLY idx_artists_email ON artists(email);
```

#### **2. Caching**
```bash
# Enable Redis for caching (optional)
# Add Redis service to docker-compose.yml
# Configure backend to use Redis for session storage
```

### 🚨 Troubleshooting

#### **Common Issues**

**1. Database Connection Failed**
```bash
# Check database logs
docker compose logs postgres

# Check database status
docker compose exec postgres pg_isready -U gayphx

# Restart database
docker compose restart postgres
```

**2. LibreTime Integration Issues**
```bash
# Test LibreTime connection
curl -X POST https://api.gayphx.com/api/plays/test-libretime-connection \
  -H "Authorization: Bearer <admin_token>"

# Check LibreTime configuration
curl -X GET https://api.gayphx.com/api/plays/libretime-config \
  -H "Authorization: Bearer <admin_token>"
```

**3. File Upload Issues**
```bash
# Check MinIO status
docker compose logs minio

# Check storage permissions
docker compose exec minio mc ls /data
```

### ✅ Production Checklist

- [ ] **Environment Variables** - All production values set
- [ ] **SSL Certificate** - Valid certificate installed
- [ ] **Database** - Migrations run, admin user created
- [ ] **Storage** - File storage configured and tested
- [ ] **Email** - SMTP server configured and tested
- [ ] **LibreTime** - Integration configured and tested
- [ ] **ISRC Key** - Registration key configured
- [ ] **Monitoring** - Health checks and logging configured
- [ ] **Backups** - Database and file backups scheduled
- [ ] **Security** - Firewall and security headers configured
- [ ] **Performance** - Database indexes and caching configured
- [ ] **Testing** - All endpoints tested and working

### 🎉 Go Live!

Once all checklist items are complete:

1. **Update DNS** - Point domain to production server
2. **Test Everything** - Verify all features work
3. **Monitor Closely** - Watch logs and performance
4. **User Training** - Train admins on new features
5. **Documentation** - Update user guides

**Your GayPHX Music Platform is now live! 🎵🌈**

---

**For support or questions:**
- **Documentation**: See README.md and API_DOCUMENTATION.md
- **System Status**: Check SYSTEM_STATUS.md
- **Feature Updates**: See FEATURE_UPDATES.md

