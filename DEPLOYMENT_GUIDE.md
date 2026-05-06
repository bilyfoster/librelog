# LibreLog Deployment Guide

## Current Status

**⚠️ Issue**: The application JAR is built but **NOT RUNNING**.

```
✅ JAR Built:        librelog-api/target/librelog-api-0.1.9.jar
✅ Version:          0.1.9
✅ Quick-Start:      INCLUDED
❌ Status:           NOT RUNNING (process not found)
```

---

## Deployment Options

### Option 1: Direct Java Deployment (Quick)

```bash
# 1. Run the deployment script
./deploy.sh

# 2. Or manually:
cd librelog-api
java -jar target/librelog-api-0.1.9.jar

# 3. Access the app
open http://localhost:8080
```

### Option 2: Docker Deployment (Production)

```bash
# Build and deploy with Docker
./deploy-docker.sh

# Or manually:
docker-compose build api --no-cache
docker-compose down
docker-compose up -d
```

---

## Verification

After deployment, verify the new features:

```bash
# Check deployment status
./check-deployment.sh

# Expected output:
# ✅ LibreLog is RUNNING (PID: xxxxx)
# ✅ Quick-Start module: INCLUDED
# ✅ Application responding on http://localhost:8080
```

### Browser Verification

1. Open: http://localhost:8080 (or your domain)
2. Login with admin credentials
3. Go to **Clock Templates** → Click **"Build"** on any clock
4. Look for **"🕐 Quick Start"** button (replaces old "📚 Start Tour")

**If you see "🕐 Quick Start" button → v0.1.9 is deployed correctly**

---

## Troubleshooting

### "Still see old version"

**Cause**: Browser caching static assets

**Fix**: Hard refresh
```bash
# Chrome/Edge: Ctrl+Shift+R (Windows) or Cmd+Shift+R (Mac)
# Or open DevTools (F12) → Network tab → Check "Disable cache" → Refresh
```

### "Port 8080 already in use"

```bash
# Find and kill old process
sudo lsof -ti:8080 | xargs kill -9

# Or use different port
java -jar librelog-api/target/librelog-api-0.1.9.jar --server.port=8081
```

### "Database connection failed"

```bash
# Start PostgreSQL
docker-compose up -d db

# Or check existing DB
pg_isready -h localhost -p 5432
```

### "Permission denied on deploy.sh"

```bash
chmod +x deploy.sh check-deployment.sh deploy-docker.sh
```

---

## What's New in v0.1.9

### 🕐 Clock Builder Quick-Start
- **Before**: 14-step blocking tour
- **After**: 1-click templates (Music Hour, Talk Hour, Morning Drive, Minimal)

### 📋 Order → Campaign Workflow
- **Before**: No link between orders and campaigns
- **After**: "Create Campaign" button on orders with auto-population

### 🎧 Spot Creation UI
- **Before**: "UI in progress" placeholder
- **After**: Full modal with campaign linking, scheduling, audio upload

---

## Files Changed

| File | Change |
|------|--------|
| `librelog-api/target/librelog-api-0.1.9.jar` | New build with all features |
| `librelog-api/src/main/resources/static/dashboard.html` | Updated UI buttons, Spot modal, Campaign modal |
| `librelog-api/src/main/resources/static/js/clock-builder-quickstart.js` | **NEW**: Template system |
| Database migration `028-add-order-id-to-campaigns.xml` | Links orders to campaigns |

---

## Quick Test

```bash
# 1. Check status
./check-deployment.sh

# 2. If not running, start it
./deploy.sh

# 3. Wait 10 seconds, then verify
./check-deployment.sh

# 4. Open browser
# Look for "🕐 Quick Start" button in Clock Builder
```

---

## Support

If deployment fails:

1. Check logs: `tail -f librelog.log`
2. Verify build: `ls -la librelog-api/target/*.jar`
3. Check ports: `netstat -tlnp | grep 8080`
4. Database: `docker-compose ps`
