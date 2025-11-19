# Track Types and LibreTime Configuration Setup

## ✅ Completed Tasks

### 1. Track Types Synchronization

All LibreLog track types have been added to LibreTime:

**LibreLog Track Types (11 total):**
- **MUS** - Music
- **ADV** - Advertisement
- **PSA** - Public Service Announcement
- **LIN** - Liner
- **INT** - Interstitial
- **PRO** - Promo
- **SHO** - Show
- **IDS** - ID/Station ID
- **COM** - Commercial
- **NEW** - News
- **VOT** - Voice Over Track

**LibreTime Track Types:**
All 11 types are now present in LibreTime's `cc_track_types` table.

### 2. LibreTime Configuration Saved to LibreLog Settings

The following LibreTime configuration has been saved to LibreLog's settings table:

| Setting Key | Value | Encrypted | Description |
|------------|-------|-----------|-------------|
| `libretime_api_url` | `https://dev-studio.gayphx.com` | No | LibreTime API base URL |
| `libretime_api_key` | `***ENCRYPTED***` | Yes | LibreTime API authentication key |
| `libretime_public_url` | `https://dev-studio.gayphx.com` | No | LibreTime public web interface URL |

**Settings Location:**
- Category: `integrations`
- Table: `settings`
- Database: `librelog`

## 📋 Verification

### Check Track Types in LibreTime:
```bash
cd /home/jenkins/docker/libretime
docker compose exec postgres psql -U libretime -d libretime -c "SELECT code, type_name FROM cc_track_types ORDER BY code;"
```

### Check LibreTime Config in LibreLog:
```bash
cd /home/jenkins/docker/librelog
docker compose exec db psql -U librelog -d librelog -c "SELECT category, key, CASE WHEN encrypted THEN '***ENCRYPTED***' ELSE value END as value FROM settings WHERE category = 'integrations' AND key LIKE 'libretime%' ORDER BY key;"
```

## 🔧 API Endpoints

### Save LibreTime Configuration:
```bash
POST /api/sync/libretime/save-config
```
Saves LibreTime configuration from environment variables to settings table.

### Get LibreTime Configuration:
```bash
GET /api/sync/libretime/config
```
Retrieves LibreTime configuration from settings (API key is masked for security).

## 📝 Scripts

### Save Configuration Script:
```bash
cd /home/jenkins/docker/librelog
docker compose exec api python -m backend.scripts.save_libretime_config
```

This script:
- Reads `LIBRETIME_API_URL`, `LIBRETIME_API_KEY`, and `LIBRETIME_PUBLIC_URL` from environment
- Saves them to the `settings` table under the `integrations` category
- Encrypts the API key automatically

## 🔄 Future Maintenance

### Adding New Track Types:

If new track types are added to LibreLog:

1. **Add to LibreLog model:**
   - Update `backend/models/track.py` CheckConstraint
   - Update `backend/integrations/type_mapping.py`

2. **Add to LibreTime:**
   ```bash
   cd /home/jenkins/docker/libretime
   docker compose exec postgres psql -U libretime -d libretime -c "INSERT INTO cc_track_types (code, type_name, description, visibility, analyze_cue_points) VALUES ('NEWCODE', 'New Type Name', 'Description', true, true);"
   ```

### Updating Configuration:

Configuration can be updated via:
1. **Environment variables** (then run save script)
2. **API endpoint** `/api/sync/libretime/save-config`
3. **Direct database update** (not recommended)

## ✅ Status

- ✅ All 11 track types exist in both systems
- ✅ LibreTime API URL saved to LibreLog settings
- ✅ LibreTime API Key saved to LibreLog settings (encrypted)
- ✅ LibreTime Public URL saved to LibreLog settings
- ✅ Type mapping utility available for conversions
- ✅ Integration endpoints ready for use

