# LibreTime Integration Adjustments

## Overview
This document outlines the adjustments needed to properly export clock templates from LibreLog to LibreTime, considering LibreTime's Smart Blocks, automation system, and differences from WideOrbit.

## Key Differences: LibreTime vs WideOrbit

### LibreTime Architecture
- **Show Instances**: LibreTime schedules content as "show instances" with start/end times
- **Smart Blocks**: Dynamic playlists that auto-populate based on criteria (genre, artist, duration, etc.)
- **Playlists**: Collections of tracks, smart blocks, and other content
- **Cue Points**: Audio items have cueIn/cueOut for precise playback control
- **Fade Transitions**: Fade in/out durations for smooth transitions
- **Cart System**: Uses file-based carts with unique identifiers

### WideOrbit Architecture
- **Clock Templates**: Rigid 60-minute templates with fixed positions
- **Asset Types**: IM, ID, CM, PR, VT, SH categories
- **Music Categories**: S1, S2, S3 classifications
- **Show Segments**: Named segments like SH_MORNING_SEG1
- **Automation Commands**: Direct automation system commands

## Required Adjustments

### 1. Fixed Assets → LibreTime Carts/File References

**Current Issue:**
- Fixed assets only store `assetIdentifier` as a string
- No mapping to actual LibreTime cart/file system
- No cue point information

**Required Changes:**
- Add `libreTimeCartId` field to `FixedAsset` entity
- Add `cueIn` and `cueOut` fields (in milliseconds or seconds)
- Add `fadeIn` and `fadeOut` fields (in milliseconds)
- Map `assetIdentifier` to LibreTime's file/cart lookup system
- Store LibreTime file path or cart number

**Implementation:**
```java
// Add to FixedAsset entity
@Column(name = "libretime_cart_id")
private String libreTimeCartId;

@Column(name = "cue_in_ms")
private Integer cueInMs;

@Column(name = "cue_out_ms")
private Integer cueOutMs;

@Column(name = "fade_in_ms")
private Integer fadeInMs;

@Column(name = "fade_out_ms")
private Integer fadeOutMs;
```

### 2. Automation Commands → LibreTime Smart Blocks/Playlists

**Current Issue:**
- Automation commands are generic (SWITCH_TO_SATELLITE, START_RECORDING, etc.)
- No mapping to LibreTime's playlist/smart block system
- LibreTime doesn't have direct "automation commands" - uses playlists instead

**Required Changes:**
- Map automation commands to LibreTime playlists or smart blocks
- For commands like "SWITCH_TO_SATELLITE", create a special playlist reference
- Store LibreTime playlist ID or smart block ID
- Add playlist/smart block lookup service

**Implementation:**
```java
// Add to AutomationCommand entity
@Column(name = "libretime_playlist_id")
private String libreTimePlaylistId;

@Column(name = "libretime_smart_block_id")
private String libreTimeSmartBlockId;

// New enum for LibreTime command mapping
public enum LibreTimeCommandType {
    PLAYLIST,      // Reference to a playlist
    SMART_BLOCK,   // Reference to a smart block
    LIVE_INPUT,    // Switch to live input
    NETWORK_FEED,  // Switch to network feed
    EAS_ALERT      // Emergency Alert System
}
```

### 3. Breaks → LibreTime Smart Blocks with Duration

**Current Issue:**
- Breaks are just time slots with duration
- No content selection mechanism
- No mapping to LibreTime's break content system

**Required Changes:**
- Map breaks to LibreTime smart blocks or playlists
- Store smart block criteria (genre, duration, etc.)
- Add break content selection rules
- Support both static and dynamic break content

**Implementation:**
```java
// Add to BreakStructure entity
@Column(name = "libretime_smart_block_id")
private String libreTimeSmartBlockId;

@Column(name = "break_content_type") // STATIC, DYNAMIC, PLAYLIST
private String breakContentType;

@Column(name = "break_criteria_json") // JSON for smart block criteria
private String breakCriteriaJson;
```

### 4. Transition Codes → LibreTime Transitions

**Current Issue:**
- Transition codes (SEGUE, OVERLAP, HARD_START) are WideOrbit-specific
- LibreTime uses fade in/out and cue points instead

**Required Changes:**
- Map WideOrbit transitions to LibreTime fade/cue settings
- Add fade duration fields
- Add transition type mapping

**Mapping:**
- `SEGUE` → Fade out previous, fade in next (default fade times)
- `OVERLAP` → Crossfade with overlap (shorter fade out, longer fade in)
- `HARD_START` → No fade, immediate start

### 5. Asset Type Mapping

**Current Issue:**
- WideOrbit asset types (IM, ID, CM, PR, VT, SH) don't directly map to LibreTime
- LibreTime uses file types and metadata instead

**Required Changes:**
- Create mapping table: WideOrbit Asset Type → LibreTime File Type/Metadata
- Store LibreTime file type in export
- Use metadata tags for categorization

**Mapping:**
- `IM` (Imaging) → LibreTime file type: "sweeper" or "stinger", metadata tag: "imaging"
- `ID` (Legal ID) → LibreTime file type: "legal_id", metadata tag: "legal"
- `CM` (Commercial) → LibreTime file type: "commercial", metadata tag: "advertisement"
- `PR` (Promo) → LibreTime file type: "promo", metadata tag: "promotion"
- `VT` (Voice Track) → LibreTime file type: "voice_track", metadata tag: "voice"
- `SH` (Show) → LibreTime file type: "show", metadata tag: "longform"

### 6. Music Categories → LibreTime Metadata

**Current Issue:**
- Music categories (S1, S2, S3) are WideOrbit-specific
- LibreTime uses metadata tags and smart block criteria

**Required Changes:**
- Map music categories to LibreTime metadata tags
- Store as metadata in exported items
- Use for smart block criteria when applicable

### 7. Show Segment Names → LibreTime Show Structure

**Current Issue:**
- Show segment names (SH_MORNING_SEG1) are WideOrbit conventions
- LibreTime uses show instances and playlists

**Required Changes:**
- Map show segments to LibreTime show instance names
- Use segment names as part of show instance naming
- Store as metadata for reference

### 8. LibreTime API Integration Enhancements

**Current Implementation:**
- Basic show instance creation
- No smart block creation/management
- No playlist management
- No cart/file lookup

**Required Enhancements:**
- Add smart block creation API calls
- Add playlist management API calls
- Add cart/file lookup service
- Add metadata tag management
- Add show instance update/delete operations

### 9. Export Format Adjustments

**Current Export Format:**
```json
{
  "name": "Clock Template Name",
  "showInstances": [{
    "startTime": "2024-01-01T00:00:00",
    "endTime": "2024-01-01T01:00:00",
    "items": [{
      "type": "break",
      "name": "Break 1",
      "startTime": "00:15:00",
      "durationSeconds": 60
    }]
  }]
}
```

**Required Format:**
```json
{
  "name": "Clock Template Name",
  "showInstances": [{
    "startTime": "2024-01-01T00:00:00",
    "endTime": "2024-01-01T01:00:00",
    "showName": "Morning Show",
    "items": [{
      "type": "playlist",  // or "smart_block", "file"
      "name": "Break 1",
      "startTime": "00:15:00",
      "durationSeconds": 60,
      "playlistId": "123",  // or smartBlockId, fileId
      "fadeIn": "2000",     // milliseconds
      "fadeOut": "2000",
      "cueIn": "0",
      "cueOut": "60000",
      "metadata": {
        "assetType": "CM",
        "musicCategory": "S1",
        "showSegment": "SH_MORNING_SEG1"
      }
    }]
  }]
}
```

## Implementation Priority

### Phase 1: Core Mapping (High Priority)
1. Add LibreTime cart/file ID fields to FixedAsset
2. Add cue point and fade fields
3. Update export to include cue/fade information
4. Map asset types to LibreTime metadata

### Phase 2: Smart Blocks & Playlists (Medium Priority)
1. Add smart block/playlist ID fields to BreakStructure
2. Create smart block creation service
3. Map automation commands to playlists
4. Add playlist management API integration

### Phase 3: Advanced Features (Lower Priority)
1. Dynamic smart block criteria generation
2. Break content selection rules
3. Show segment mapping
4. Music category metadata integration

## Database Schema Changes

### New Columns Needed

**fixed_assets table:**
- `libretime_cart_id VARCHAR(255)`
- `cue_in_ms INTEGER`
- `cue_out_ms INTEGER`
- `fade_in_ms INTEGER`
- `fade_out_ms INTEGER`

**break_structures table:**
- `libretime_smart_block_id VARCHAR(255)`
- `break_content_type VARCHAR(50)`
- `break_criteria_json TEXT`

**automation_commands table:**
- `libretime_playlist_id VARCHAR(255)`
- `libretime_smart_block_id VARCHAR(255)`
- `libretime_command_type VARCHAR(50)`

## API Endpoints Needed

### LibreTime Client Enhancements
- `createSmartBlock(criteria)` - Create a smart block with criteria
- `getSmartBlock(id)` - Retrieve smart block details
- `createPlaylist(name, items)` - Create a playlist
- `getPlaylist(id)` - Retrieve playlist details
- `lookupCart(identifier)` - Lookup cart/file by identifier
- `getFileMetadata(fileId)` - Get file metadata including cue points

## Testing Considerations

1. **LibreTime Instance Required**: Need a test LibreTime instance for integration testing
2. **Cart/File Database**: Need access to LibreTime's file/cart database for lookups
3. **Smart Block Testing**: Test dynamic vs static smart blocks
4. **Playlist Testing**: Test playlist creation and scheduling
5. **Cue Point Testing**: Verify cue in/out accuracy
6. **Fade Testing**: Verify fade transitions work correctly

## Migration Path

1. Add new fields as nullable initially
2. Create migration script to populate LibreTime IDs from existing data where possible
3. Update export service to use new fields
4. Add UI for LibreTime-specific configuration
5. Test export with real LibreTime instance
6. Iterate based on feedback

## References

- LibreTime API Documentation: https://libretime.org/docs/
- LibreTime Smart Blocks: https://libretime.org/docs/stable-3.x/user-manual/playlists/
- LibreTime API v2: https://libretime.org/docs/api/

