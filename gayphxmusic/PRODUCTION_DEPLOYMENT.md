# GayPHX Music Platform - Production Deployment Guide

**Version:** 2.1.0  
**Last Updated:** January 25, 2025

## 🚀 Production Deployment Overview

This guide covers deploying the GayPHX Music Platform to a production server with proper security, monitoring, and scalability configurations.

## 📋 Prerequisites

### Server Requirements
- **OS**: Ubuntu 20.04+ or CentOS 8+
- **RAM**: Minimum 4GB (8GB+ recommended)
- **CPU**: 2+ cores
- **Storage**: 50GB+ SSD
- **Network**: Static IP address

### Software Requirements
- Docker Engine 20.10+
- Docker Compose 2.0+
- Git
- SSL Certificate (Let's Encrypt recommended)

## 🔧 Environment Configuration

### 1. Production Environment Variables

Create a `.env.production` file:

```bash
# Database Configuration
POSTGRES_DB=gayphx_music_prod
POSTGRES_USER=gayphx_prod
POSTGRES_PASSWORD=<STRONG_PASSWORD>
POSTGRES_HOST=postgres
POSTGRES_PORT=5432

# Application Configuration
SECRET_KEY=<GENERATE_STRONG_SECRET_KEY>
ALGORITHM=HS256
ACCESS_TOKEN_EXPIRE_MINUTES=30

# Backend Configuration
BACKEND_HOST=0.0.0.0
BACKEND_PORT=8000
CORS_ORIGINS=https://yourdomain.com,https://www.yourdomain.com

# Frontend Configuration
NEXT_PUBLIC_API_URL=https://api.yourdomain.com
NEXT_PUBLIC_APP_URL=https://yourdomain.com

# File Storage (MinIO)
MINIO_ROOT_USER=gayphx_admin
MINIO_ROOT_PASSWORD=<STRONG_PASSWORD>
MINIO_BUCKET_NAME=gayphx-audio

# LibreTime Integration
LIBRETIME_API_URL=http://libretime:8080
LIBRETIME_API_KEY=<LIBRETIME_API_KEY>

# Email Configuration
SMTP_HOST=smtp.yourdomain.com
SMTP_PORT=587
SMTP_USERNAME=noreply@yourdomain.com
SMTP_PASSWORD=<EMAIL_PASSWORD>
SMTP_FROM_NAME=GayPHX Music Platform

# ISRC Configuration
ISRC_COUNTRY_CODE=US
ISRC_REGISTRANT_CODE=GPH
```

### 2. Generate Strong Secrets

```bash
# Generate SECRET_KEY
python3 -c "import secrets; print(secrets.token_urlsafe(32))"

# Generate database password
openssl rand -base64 32

# Generate MinIO password
openssl rand -base64 32
```

## 🐳 Docker Production Configuration

### 1. Production Docker Compose

Create `docker-compose.prod.yml`:

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: gayphx-postgres-prod
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./scripts/init-db.sql:/docker-entrypoint-initdb.d/init-db.sql
    restart: unless-stopped
    networks:
      - gayphx-network

  minio:
    image: minio/minio:latest
    container_name: gayphx-minio-prod
    environment:
      MINIO_ROOT_USER: ${MINIO_ROOT_USER}
      MINIO_ROOT_PASSWORD: ${MINIO_ROOT_PASSWORD}
    command: server /data --console-address ":9001"
    volumes:
      - minio_data:/data
    ports:
      - "9000:9000"
      - "9001:9001"
    restart: unless-stopped
    networks:
      - gayphx-network

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: gayphx-backend-prod
    environment:
      - DATABASE_URL=postgresql://${POSTGRES_USER}:${POSTGRES_PASSWORD}@postgres:5432/${POSTGRES_DB}
      - SECRET_KEY=${SECRET_KEY}
      - ALGORITHM=${ALGORITHM}
      - ACCESS_TOKEN_EXPIRE_MINUTES=${ACCESS_TOKEN_EXPIRE_MINUTES}
      - MINIO_ENDPOINT=minio:9000
      - MINIO_ACCESS_KEY=${MINIO_ROOT_USER}
      - MINIO_SECRET_KEY=${MINIO_ROOT_PASSWORD}
      - MINIO_BUCKET_NAME=${MINIO_BUCKET_NAME}
      - LIBRETIME_API_URL=${LIBRETIME_API_URL}
      - LIBRETIME_API_KEY=${LIBRETIME_API_KEY}
      - SMTP_HOST=${SMTP_HOST}
      - SMTP_PORT=${SMTP_PORT}
      - SMTP_USERNAME=${SMTP_USERNAME}
      - SMTP_PASSWORD=${SMTP_PASSWORD}
      - SMTP_FROM_NAME=${SMTP_FROM_NAME}
    volumes:
      - ./backend/audio_cache:/app/audio_cache
      - ./backend/uploads:/app/uploads
    depends_on:
      - postgres
      - minio
    restart: unless-stopped
    networks:
      - gayphx-network

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    container_name: gayphx-frontend-prod
    environment:
      - NODE_ENV=production
      - NEXT_PUBLIC_API_URL=${NEXT_PUBLIC_API_URL}
      - NEXT_PUBLIC_APP_URL=${NEXT_PUBLIC_APP_URL}
    depends_on:
      - backend
    restart: unless-stopped
    networks:
      - gayphx-network

  nginx:
    image: nginx:alpine
    container_name: gayphx-nginx-prod
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.prod.conf:/etc/nginx/nginx.conf
      - ./nginx/ssl:/etc/nginx/ssl
      - ./nginx/logs:/var/log/nginx
    depends_on:
      - frontend
      - backend
    restart: unless-stopped
    networks:
      - gayphx-network

volumes:
  postgres_data:
  minio_data:

networks:
  gayphx-network:
    driver: bridge
```

## 🔒 SSL/TLS Configuration

### 1. Let's Encrypt SSL Setup

```bash
# Install Certbot
sudo apt update
sudo apt install certbot

# Generate SSL certificate
sudo certbot certonly --standalone -d yourdomain.com -d www.yourdomain.com

# Copy certificates to nginx directory
sudo cp /etc/letsencrypt/live/yourdomain.com/fullchain.pem ./nginx/ssl/
sudo cp /etc/letsencrypt/live/yourdomain.com/privkey.pem ./nginx/ssl/
sudo chown -R $USER:$USER ./nginx/ssl/
```

### 2. Production Nginx Configuration

Create `nginx/nginx.prod.conf`:

```nginx
events {
    worker_connections 1024;
}

http {
    include       /etc/nginx/mime.types;
    default_type  application/octet-stream;
    
    # Logging
    log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                    '$status $body_bytes_sent "$http_referer" '
                    '"$http_user_agent" "$http_x_forwarded_for"';
    
    access_log /var/log/nginx/access.log main;
    error_log /var/log/nginx/error.log warn;
    
    # Security headers
    add_header X-Frame-Options DENY;
    add_header X-Content-Type-Options nosniff;
    add_header X-XSS-Protection "1; mode=block";
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    
    # Rate limiting
    limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;
    limit_req_zone $binary_remote_addr zone=login:10m rate=5r/m;
    
    # Upstream servers
    upstream backend {
        server backend:8000;
    }
    
    upstream frontend {
        server frontend:3000;
    }
    
    # HTTP to HTTPS redirect
    server {
        listen 80;
        server_name yourdomain.com www.yourdomain.com;
        return 301 https://$server_name$request_uri;
    }
    
    # Main HTTPS server
    server {
        listen 443 ssl http2;
        server_name yourdomain.com www.yourdomain.com;
        
        # SSL configuration
        ssl_certificate /etc/nginx/ssl/fullchain.pem;
        ssl_certificate_key /etc/nginx/ssl/privkey.pem;
        ssl_protocols TLSv1.2 TLSv1.3;
        ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512:ECDHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES256-GCM-SHA384;
        ssl_prefer_server_ciphers off;
        ssl_session_cache shared:SSL:10m;
        ssl_session_timeout 10m;
        
        # API routes
        location /api/ {
            limit_req zone=api burst=20 nodelay;
            proxy_pass http://backend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            proxy_connect_timeout 30s;
            proxy_send_timeout 30s;
            proxy_read_timeout 30s;
        }
        
        # Login rate limiting
        location /api/auth/login {
            limit_req zone=login burst=5 nodelay;
            proxy_pass http://backend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }
        
        # Frontend routes
        location / {
            proxy_pass http://frontend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }
        
        # Static files caching
        location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
            proxy_pass http://frontend;
            expires 1y;
            add_header Cache-Control "public, immutable";
        }
    }
}
```

## 🚀 Deployment Scripts

### 1. Production Deployment Script

Create `scripts/deploy-prod.sh`:

```bash
#!/bin/bash

set -e

echo "🚀 Starting GayPHX Music Platform Production Deployment"

# Check if running as root
if [[ $EUID -eq 0 ]]; then
   echo "❌ This script should not be run as root"
   exit 1
fi

# Load environment variables
if [ -f .env.production ]; then
    export $(cat .env.production | grep -v '^#' | xargs)
    echo "✅ Environment variables loaded"
else
    echo "❌ .env.production file not found"
    exit 1
fi

# Create necessary directories
echo "📁 Creating directories..."
mkdir -p nginx/logs
mkdir -p nginx/ssl
mkdir -p backend/audio_cache
mkdir -p backend/uploads

# Set proper permissions
echo "🔐 Setting permissions..."
chmod 755 nginx/logs
chmod 600 nginx/ssl/*.pem 2>/dev/null || true

# Stop existing containers
echo "🛑 Stopping existing containers..."
docker-compose -f docker-compose.prod.yml down

# Build and start services
echo "🔨 Building and starting services..."
docker-compose -f docker-compose.prod.yml up -d --build

# Wait for services to be ready
echo "⏳ Waiting for services to be ready..."
sleep 30

# Run database migrations
echo "🗄️ Running database migrations..."
docker-compose -f docker-compose.prod.yml exec backend alembic upgrade head

# Initialize MinIO bucket
echo "🪣 Initializing MinIO bucket..."
docker-compose -f docker-compose.prod.yml exec minio mc mb minio/${MINIO_BUCKET_NAME} 2>/dev/null || true

# Health check
echo "🏥 Performing health check..."
if curl -f http://localhost/api/health > /dev/null 2>&1; then
    echo "✅ Health check passed"
else
    echo "❌ Health check failed"
    exit 1
fi

echo "🎉 Deployment completed successfully!"
echo "🌐 Application is available at: https://yourdomain.com"
echo "📊 Admin panel: https://yourdomain.com/admin"
```

### 2. Backup Script

Create `scripts/backup-prod.sh`:

```bash
#!/bin/bash

set -e

BACKUP_DIR="/opt/gayphx-backups"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="gayphx_backup_${DATE}.tar.gz"

echo "💾 Starting production backup..."

# Create backup directory
mkdir -p $BACKUP_DIR

# Create database backup
echo "🗄️ Backing up database..."
docker-compose -f docker-compose.prod.yml exec -T postgres pg_dump -U $POSTGRES_USER $POSTGRES_DB > $BACKUP_DIR/database_${DATE}.sql

# Create file backup
echo "📁 Backing up files..."
tar -czf $BACKUP_DIR/$BACKUP_FILE \
    --exclude='node_modules' \
    --exclude='.git' \
    --exclude='*.log' \
    .

# Clean old backups (keep last 7 days)
echo "🧹 Cleaning old backups..."
find $BACKUP_DIR -name "*.sql" -mtime +7 -delete
find $BACKUP_DIR -name "*.tar.gz" -mtime +7 -delete

echo "✅ Backup completed: $BACKUP_DIR/$BACKUP_FILE"
```

## 📊 Monitoring & Logging

### 1. Log Rotation Configuration

Create `scripts/setup-logrotate.sh`:

```bash
#!/bin/bash

# Create logrotate configuration
sudo tee /etc/logrotate.d/gayphx > /dev/null <<EOF
/opt/gayphx/nginx/logs/*.log {
    daily
    missingok
    rotate 52
    compress
    delaycompress
    notifempty
    create 644 root root
    postrotate
        docker-compose -f /opt/gayphx/docker-compose.prod.yml restart nginx
    endscript
}
EOF

echo "✅ Log rotation configured"
```

### 2. System Monitoring

Create `scripts/monitor.sh`:

```bash
#!/bin/bash

# Check service health
check_service() {
    local service=$1
    local url=$2
    
    if curl -f $url > /dev/null 2>&1; then
        echo "✅ $service is healthy"
        return 0
    else
        echo "❌ $service is unhealthy"
        return 1
    fi
}

echo "🔍 Checking service health..."

check_service "Frontend" "http://localhost"
check_service "Backend API" "http://localhost/api/health"
check_service "Database" "http://localhost/api/admin/stats"

# Check disk space
DISK_USAGE=$(df / | awk 'NR==2 {print $5}' | sed 's/%//')
if [ $DISK_USAGE -gt 80 ]; then
    echo "⚠️ Disk usage is high: ${DISK_USAGE}%"
else
    echo "✅ Disk usage is normal: ${DISK_USAGE}%"
fi

# Check memory usage
MEMORY_USAGE=$(free | awk 'NR==2{printf "%.0f", $3*100/$2}')
if [ $MEMORY_USAGE -gt 80 ]; then
    echo "⚠️ Memory usage is high: ${MEMORY_USAGE}%"
else
    echo "✅ Memory usage is normal: ${MEMORY_USAGE}%"
fi
```

## 🔄 Maintenance Tasks

### 1. Update Script

Create `scripts/update-prod.sh`:

```bash
#!/bin/bash

set -e

echo "🔄 Starting production update..."

# Backup before update
./scripts/backup-prod.sh

# Pull latest changes
git pull origin main

# Rebuild and restart services
docker-compose -f docker-compose.prod.yml down
docker-compose -f docker-compose.prod.yml up -d --build

# Run migrations
docker-compose -f docker-compose.prod.yml exec backend alembic upgrade head

echo "✅ Update completed successfully!"
```

### 2. SSL Renewal

Create `scripts/renew-ssl.sh`:

```bash
#!/bin/bash

echo "🔒 Renewing SSL certificate..."

# Stop nginx
docker-compose -f docker-compose.prod.yml stop nginx

# Renew certificate
sudo certbot renew --standalone

# Copy new certificates
sudo cp /etc/letsencrypt/live/yourdomain.com/fullchain.pem ./nginx/ssl/
sudo cp /etc/letsencrypt/live/yourdomain.com/privkey.pem ./nginx/ssl/
sudo chown -R $USER:$USER ./nginx/ssl/

# Restart nginx
docker-compose -f docker-compose.prod.yml start nginx

echo "✅ SSL certificate renewed successfully!"
```

## 📋 Deployment Checklist

### Pre-Deployment
- [ ] Server meets minimum requirements
- [ ] Domain name configured and pointing to server
- [ ] SSL certificate obtained
- [ ] Environment variables configured
- [ ] Database credentials secured
- [ ] Backup strategy implemented

### Deployment Steps
- [ ] Clone repository to production server
- [ ] Configure environment variables
- [ ] Set up SSL certificates
- [ ] Run deployment script
- [ ] Verify all services are running
- [ ] Test application functionality
- [ ] Configure monitoring and logging
- [ ] Set up automated backups

### Post-Deployment
- [ ] Monitor application logs
- [ ] Verify SSL certificate auto-renewal
- [ ] Test backup and restore procedures
- [ ] Document any custom configurations
- [ ] Train team on maintenance procedures

## 🆘 Troubleshooting

### Common Issues

1. **Services won't start**
   ```bash
   # Check logs
   docker-compose -f docker-compose.prod.yml logs
   
   # Check resource usage
   docker stats
   ```

2. **Database connection issues**
   ```bash
   # Check database status
   docker-compose -f docker-compose.prod.yml exec postgres pg_isready
   
   # Check database logs
   docker-compose -f docker-compose.prod.yml logs postgres
   ```

3. **SSL certificate issues**
   ```bash
   # Test SSL configuration
   openssl s_client -connect yourdomain.com:443
   
   # Check certificate expiry
   openssl x509 -in nginx/ssl/fullchain.pem -text -noout | grep "Not After"
   ```

## 📞 Support

For production support and maintenance:
- **Documentation**: See `README.md` and `SYSTEM_STATUS.md`
- **Logs**: Check `nginx/logs/` and Docker container logs
- **Monitoring**: Use `scripts/monitor.sh` for health checks
- **Backups**: Automated daily backups in `/opt/gayphx-backups/`

---

**⚠️ Important**: Always test deployment procedures in a staging environment before applying to production.
