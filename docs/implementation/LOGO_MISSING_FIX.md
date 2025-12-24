# Logo Missing - Issue and Fix

**Date:** 2025-11-23  
**Issue:** Logo is missing on login screen

## Problem

The logo is not displaying on the login screen even though:
- ✅ API is online and working
- ✅ Branding endpoint returns 200 OK
- ✅ Database has a logo_url setting: `/api/settings/branding/logo/logo_20251119_111540.png`

## Root Cause

The logo file itself is missing from the filesystem:
- Database setting exists: `logo_url = /api/settings/branding/logo/logo_20251119_111540.png`
- File should be at: `/var/lib/librelog/logos/logo_20251119_111540.png` or `/tmp/librelog/logos/logo_20251119_111540.png`
- **File does not exist** in either location

The branding endpoint (`/api/settings/branding/public`) checks if the logo file exists, and if not, it returns an empty `logo_url`. This is by design to prevent broken image links.

## Why the Logo Was Lost

The logo directory (`/var/lib/librelog/logos` or `/tmp/librelog/logos`) was not persisted in a Docker volume. When containers are recreated, files in these directories are lost.

## Fix Applied

### 1. Added Logo Volume to docker-compose.yml

Added a persistent volume for logos:

```yaml
volumes:
  - librelog_logos:/var/lib/librelog/logos
```

This ensures logos persist across container recreations.

### 2. Restart Required

To apply the fix:

```bash
docker-compose down
docker-compose up -d
```

## Solution: Re-upload Logo

Since the logo file is missing, you need to re-upload it:

1. **Log in to LibreLog**
2. **Go to Settings** → **Branding**
3. **Upload a new logo** using the logo upload feature
4. The logo will be saved to the persistent volume and will survive container restarts

## Verification

After re-uploading, verify:

1. Check branding endpoint:
   ```bash
   curl http://api:8000/api/settings/branding/public
   ```
   Should return a `logo_url` with a valid path.

2. Check logo file exists:
   ```bash
   docker exec librelog-api-1 ls -la /var/lib/librelog/logos/
   ```

3. Check logo is accessible:
   ```bash
   curl http://api:8000/api/settings/branding/logo/<filename>
   ```
   Should return the image file.

## Prevention

The persistent volume (`librelog_logos`) has been added to `docker-compose.yml` to prevent this issue in the future. Logos will now persist across container recreations.

## Current Status

- ✅ Volume configuration added
- ⏳ **TODO**: Restart containers to apply volume
- ⏳ **TODO**: Re-upload logo via Settings page
- ⏳ **TODO**: Verify logo displays on login screen

