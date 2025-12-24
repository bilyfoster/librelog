# Voice Track Selection and Sync Workflow

## Overview

When a user selects a voice track for a spot/break in the log, the system performs several operations to ensure everything stays in sync across the database, file system, and LibreTime.

## Workflow Steps

### 1. User Action
- User views a daily log
- User selects a voice track for a specific break/spot (e.g., "A break" at 14:00)

### 2. API Call
The frontend calls:
```
PUT /api/logs/{log_id}/voice-slots/{slot_id}/link-voice-track
Body: { "voice_track_id": 123 }
```

### 3. Backend Processing (Automatic)

When the endpoint is called, the following happens **automatically**:

#### Step 3.1: Link Voice Track to Slot
- Links the voice track to the `VoiceTrackSlot` record
- Updates slot status to "recorded"
- Sets `slot.voice_track_id = voice_track_id`

#### Step 3.2: Set Standardized Name
- Gets the slot's `standardized_name` (format: `HH-00_BreakX`, e.g., `14-00_BreakA`)
- Updates the voice track with:
  - `voice_track.standardized_name = slot.standardized_name`
  - `voice_track.recorded_date = current timestamp`
  - `voice_track.break_id = slot_id`

#### Step 3.3: Rename File (if needed)
- Checks if the current filename matches the standardized name
- If different, renames the physical file:
  - Old: `20251123_143022_break123_take1.mp3`
  - New: `14-00_BreakA.mp3`
- Updates `voice_track.file_url` to reflect the new filename

#### Step 3.4: Sync to Log JSON Data
- Finds the corresponding VOT element in the log's `json_data`
- Updates the element with:
  ```json
  {
    "title": "Voice Track Name",
    "artist": "Voice Over",
    "file_path": "/api/voice/14-00_BreakA.mp3/file",
    "libretime_id": "12345",
    "voice_track_id": 123,
    "pending": false,
    "standardized_name": "14-00_BreakA",
    "fallback_used": false
  }
  ```
- Updates `log.updated_at` timestamp

#### Step 3.5: Database Commit
- All changes are committed to the database
- The voice track is now fully linked and synced

### 4. Result

After the workflow completes:

✅ **Voice Track**:
- Has `standardized_name` set (e.g., `14-00_BreakA`)
- File is renamed to match standardized name
- Linked to the slot via `break_id`

✅ **Slot**:
- Has `voice_track_id` set
- Status is "recorded"

✅ **Log JSON Data**:
- VOT element has voice track information
- Element shows as not pending
- File path points to renamed file

✅ **Viewing the Log**:
- Voice track appears linked in the log view
- File name matches what LibreTime expects

## Uploading to LibreTime

When ready to send to LibreTime:

1. Call `POST /api/voice-tracks/{voice_track_id}/upload-to-libretime`
2. The system:
   - Uses the **standardized filename** (e.g., `14-00_BreakA.mp3`) when uploading
   - Updates `voice_track.libretime_id` with the LibreTime file ID
   - Updates `voice_track.status` to `READY`
   - The log element's `libretime_id` is also updated

## Alternative Workflows

### Recording Directly for a Break
- Use `POST /api/voice/breaks/{break_id}/takes`
- Automatically links to slot and sets standardized name
- File is saved with standardized name from the start

### Uploading Separately Recorded Track
- Use `POST /api/voice-tracks/upload` with `slot_id` parameter
- Automatically links to slot and renames file
- Same sync workflow applies

### Selecting a Take
- Use `PUT /api/voice/takes/{take_id}/select`
- Marks take as final and links to slot
- Syncs to log element

## Key Points

1. **Standardized Naming**: All voice tracks get a standardized name format (`HH-00_BreakX`) that matches what LibreTime expects in the log.

2. **Automatic Sync**: When a voice track is linked to a slot, it automatically:
   - Gets renamed to match standardized name
   - Syncs to the log's JSON data
   - Updates all related records

3. **LibreTime Compatibility**: The standardized filename ensures LibreTime can match the voice track to the log entry when the log is sent.

4. **Non-Blocking**: File operations and log syncs are designed to not block the API response, ensuring fast user experience.

## API Endpoints

### Link Voice Track to Slot
```
PUT /api/logs/{log_id}/voice-slots/{slot_id}/link-voice-track
Body: { "voice_track_id": <int> }
```

### Upload Voice Track (with slot linking)
```
POST /api/voice-tracks/upload
Form Data:
  - file: <audio file>
  - slot_id: <optional slot_id>
```

### Record Take for Break
```
POST /api/voice/breaks/{break_id}/takes
Form Data:
  - file: <audio file>
```

### Select Take
```
PUT /api/voice/takes/{take_id}/select
```

### Upload to LibreTime
```
POST /api/voice-tracks/{voice_track_id}/upload-to-libretime
```

