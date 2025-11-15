# LibreTime Integration Setup

## Configuration Complete

LibreLog has been configured to connect to LibreTime using the new API endpoints we just implemented.

## Connection Details

- **LibreTime URL**: `http://api:9001/api/v2` (Docker network access)
- **API Key**: Configured in `.env` file
- **Network**: LibreLog containers are connected to the `libretime` Docker network

## Updated Endpoints

The LibreTime client has been updated to use the new endpoints:

1. **Library Export**: `/api/v2/files/library-full`
   - Replaces old `/tracks` endpoint
   - Returns complete library with all metadata

2. **Playout History**: `/api/v2/playout-history/playout-history-full`
   - Replaces old `/playback-history` endpoint
   - Requires `start` and `end` datetime parameters

3. **Schedule Injection**: `/api/v2/schedule/replace-day`
   - Replaces old `/schedule/{date}` endpoint
   - Accepts date and entries array

4. **Schedule Status**: `/api/v2/schedule/status`
   - New endpoint for checking schedule status
   - Returns conflicts and broken items

## Authentication

LibreLog uses `Api-Key` authentication format (not Bearer) as required by LibreTime.

## Testing the Connection

Once LibreLog is running, you can test the connection:

```bash
# From within LibreLog API container
docker exec librelog-api-1 curl -s "http://api:9001/api/v2/files/library-full?limit=1" \
  -H "Authorization: Api-Key 9cf5ef7496da3029693aa7516d41db24a9b0e80cfe3771313576d9dd64216919"
```

## Next Steps

1. Start LibreLog: `docker-compose up -d`
2. Verify connection in LibreLog UI
3. Test library sync
4. Test schedule generation and injection

