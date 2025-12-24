# LibreLog ↔ LibreTime Media Library Sync

## ✅ Sync Status

**Yes, LibreLog syncs with LibreTime for the Media Library!**

### Automatic Sync (Periodic)
- **Frequency**: Every hour (3600 seconds)
- **Task**: `sync_media_library_from_libretime`
- **Direction**: LibreTime → LibreLog (one-way sync)
- **Status**: ✅ **ACTIVE** (Celery Beat scheduler is running)

### Manual Sync
- **Endpoint**: `POST /api/sync/tracks`
- **Description**: Manually trigger a sync of tracks from LibreTime to LibreLog
- **Parameters**:
  - `limit` (optional, default: 1000): Maximum number of tracks to sync
  - `offset` (optional, default: 0): Starting offset for pagination

## 🔄 How It Works

1. **Automatic Sync (Hourly)**:
   - Celery Beat scheduler triggers `sync_media_library_from_libretime` every hour
   - Fetches tracks from LibreTime's `/api/v2/integration/media-library` endpoint
   - Processes tracks in batches of 100
   - Creates new tracks or updates existing ones based on `libretime_id`
   - Maps LibreTime track types to LibreLog types using type mapping utility
   - Stores last sync time in settings table

2. **Manual Sync**:
   - Call the API endpoint with authentication
   - Same process as automatic sync, but triggered on-demand
   - Useful for immediate sync or testing

## 📊 Sync Details

### What Gets Synced:
- Track title, artist, album
- Track type (mapped from LibreTime to LibreLog)
- Genre
- Duration (in seconds)
- File path
- Creation and update timestamps
- LibreTime track ID (stored as `libretime_id`)

### Sync Behavior:
- **New tracks**: Created in LibreLog with `libretime_id` reference
- **Existing tracks**: Updated if they have matching `libretime_id`
- **Track types**: Automatically mapped using `type_mapping.py` utility
- **Errors**: Logged but don't stop the sync process

## 🔍 Monitoring

### Check Last Sync Time:
```bash
cd /home/jenkins/docker/librelog
docker compose exec db psql -U librelog -d librelog -c "SELECT category, key, value FROM settings WHERE category = 'sync' AND key LIKE '%libretime%' OR key LIKE '%track%';"
```

### Check Celery Beat Status:
```bash
cd /home/jenkins/docker/librelog
docker compose logs beat --tail 20
```

### Check Worker Status:
```bash
cd /home/jenkins/docker/librelog
docker compose logs worker --tail 20 | grep -i "libretime\|sync"
```

### View Scheduled Tasks:
```bash
cd /home/jenkins/docker/librelog
docker compose exec beat celery -A backend.tasks.celery inspect scheduled
```

## 🛠️ Configuration

### Sync Schedule:
Configured in `backend/tasks/celery.py`:
```python
'sync-media-library-from-libretime': {
    'task': 'backend.tasks.libretime_sync.sync_media_library_from_libretime',
    'schedule': 3600.0,  # Run hourly
}
```

### Sync Task:
Located in `backend/tasks/libretime_sync.py`

### API Client:
Located in `backend/integrations/libretime_client.py`

## 📝 API Endpoints

### Manual Sync:
```bash
POST /api/sync/tracks
Authorization: Bearer <token>
Content-Type: application/json

{
  "limit": 1000,  # optional
  "offset": 0      # optional
}
```

### Sync Status:
```bash
GET /api/sync/status
Authorization: Bearer <token>
```

Returns:
- Last track sync time
- Last playback sync time
- Total tracks count
- Connection status to LibreTime

## ✅ Current Status

- ✅ Celery Beat scheduler: **Running**
- ✅ Celery Worker: **Running**
- ✅ Sync task: **Registered and scheduled**
- ✅ LibreTime API: **Configured and accessible**
- ✅ Track type mapping: **Configured**

## 🔄 Next Sync

The next automatic sync will run:
- **When**: Within the next hour (from when Beat started)
- **Frequency**: Every hour thereafter
- **Logs**: Check `docker compose logs beat` and `docker compose logs worker`

## 🚨 Troubleshooting

### If sync isn't running:
1. Check Celery Beat is running: `docker compose ps | grep beat`
2. Check logs: `docker compose logs beat`
3. Verify Redis connection: `docker compose ps | grep redis`
4. Check worker logs: `docker compose logs worker`

### If sync fails:
1. Check LibreTime API connectivity
2. Verify API key is correct
3. Check worker logs for errors
4. Verify LibreTime integration endpoint is accessible

### Manual trigger:
If you need to sync immediately:
```bash
curl -X POST http://localhost:8000/api/sync/tracks \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json"
```


