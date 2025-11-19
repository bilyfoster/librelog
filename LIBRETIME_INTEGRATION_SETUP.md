# LibreTime Integration Setup Summary

## ✅ Completed Steps

### 1. LibreTime SSL Configuration
- ✅ Updated `docker-compose.yml` with proper HTTPS/TLS configuration
- ✅ Set `public_url: https://dev-studio.gayphx.com` in `config.yml`
- ✅ Added CORS origin for `https://log.gayphx.com`

### 2. LibreTime REST API Integration
- ✅ Created integration module with endpoints:
  - `POST /api/v2/integration/voice-tracks` - Receive voice tracks from LibreLog
  - `GET /api/v2/integration/media-library` - Export media library
  - `GET /api/v2/integration/track/{id}` - Get track details
  - `GET /api/v2/integration/sync-status` - Check sync status

### 3. VOT Track Type
- ✅ Added VOT (Voice Over Track) type to LibreTime database
- ✅ Updated LibreLog Track model to include VOT in type constraint
- ✅ Created type mapping utility for compatibility

### 4. LibreLog Integration
- ✅ Enhanced `libretime_client.py` with upload and sync methods
- ✅ Added voice track upload endpoint: `POST /api/voice-tracks/{id}/upload-to-libretime`
- ✅ Created periodic sync task (runs hourly)
- ✅ Updated sync router to use new integration endpoints

### 5. Database Migrations
- ✅ Added `libretime_id` field to `voice_tracks` table
- ✅ Updated track types constraint to include VOT

### 6. Container Deployment
- ✅ LibreLog containers restarted with new code
- ✅ LibreTime containers started and configured
- ✅ Database migrations run successfully

## 🔧 Required Configuration

### LibreLog Environment Variables

Add these to `/home/jenkins/docker/librelog/.env`:

```bash
# LibreTime Integration
LIBRETIME_API_URL=https://dev-studio.gayphx.com
LIBRETIME_API_KEY=xRzzSy8hwHM9WoFjlyHtoyKRSURXCc0K49WZYLN_2zk
```

**Note:** The API key above was generated for LibreTime. Make sure it matches the `api_key` in `/home/jenkins/docker/libretime/docker/config.yml`.

### LibreTime Configuration

The following is already configured in `/home/jenkins/docker/libretime/docker/config.yml`:
- ✅ `public_url: https://dev-studio.gayphx.com`
- ✅ `api_key: xRzzSy8hwHM9WoFjlyHtoyKRSURXCc0K49WZYLN_2zk`
- ✅ `secret_key: YM4fcXXhVlMDC5Elav-S3W7bis3C1AB-duKFEC6gZraHn4ihrsAtyWx-VmA4mHpCp_4`
- ✅ `allowed_cors_origins: [https://log.gayphx.com]`

## 📋 Next Steps

1. **Add environment variables to LibreLog:**
   ```bash
   cd /home/jenkins/docker/librelog
   echo "LIBRETIME_API_URL=https://dev-studio.gayphx.com" >> .env
   echo "LIBRETIME_API_KEY=xRzzSy8hwHM9WoFjlyHtoyKRSURXCc0K49WZYLN_2zk" >> .env
   docker compose restart api worker
   ```

2. **Verify LibreTime API is accessible:**
   ```bash
   curl -H "Authorization: Api-Key xRzzSy8hwHM9WoFjlyHtoyKRSURXCc0K49WZYLN_2zk" \
        https://dev-studio.gayphx.com/api/v2/integration/sync-status
   ```

3. **Test voice track upload:**
   - Record/edit a voice track in LibreLog
   - Use the upload endpoint: `POST /api/voice-tracks/{id}/upload-to-libretime`

4. **Verify media library sync:**
   - The periodic sync runs hourly automatically
   - Or trigger manually: `POST /api/sync/tracks`

## 🔍 Verification

### Check LibreTime containers:
```bash
cd /home/jenkins/docker/libretime
docker compose ps
```

### Check LibreLog containers:
```bash
cd /home/jenkins/docker/librelog
docker compose ps
```

### Verify VOT type exists:
```bash
cd /home/jenkins/docker/libretime
docker compose exec postgres psql -U libretime -d libretime -c "SELECT code, type_name FROM cc_track_types WHERE code = 'VOT';"
```

## 📝 API Endpoints

### LibreTime Integration Endpoints (require API key):
- `POST /api/v2/integration/voice-tracks` - Upload voice track
- `GET /api/v2/integration/media-library?limit=100&offset=0` - Get media library
- `GET /api/v2/integration/track/{id}` - Get track details
- `GET /api/v2/integration/sync-status` - Get sync status

### LibreLog Endpoints:
- `POST /api/voice-tracks/{id}/upload-to-libretime` - Upload voice track to LibreTime
- `POST /api/sync/tracks` - Manually sync media library from LibreTime
- `GET /api/sync/status` - Get sync status

## 🔐 Security Notes

- The API key is used for authentication between LibreLog and LibreTime
- Keep the API key secure and don't commit it to version control
- The key is stored in:
  - LibreTime: `docker/config.yml`
  - LibreLog: `.env` file (to be added)

