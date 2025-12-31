# Product Requirements Document: LibreTime API Extensions for LibreLog Integration

## Introduction/Overview

LibreLog is a broadcast traffic management system designed to replace WideOrbit, with deep integration into LibreTime for audio scheduling and automation. Currently, LibreTime's existing API lacks the functionality required for seamless two-way synchronization, programmatic log generation, and advanced scheduling capabilities that LibreLog needs.

This PRD defines the API extensions and enhancements required in LibreTime to support:
- **Two-way file synchronization** between LibreLog and LibreTime
- **Programmatic log generation and scheduling** from clock templates
- **Real-time and batch synchronization** of audio files and metadata
- **Enhanced scheduling capabilities** beyond random playlists

The goal is to create a comprehensive API that allows LibreLog to function as a complete traffic management system while leveraging LibreTime's audio playback and automation capabilities.

## Goals

1. **Enable two-way file synchronization** between LibreLog and LibreTime, allowing audio files uploaded in either system to be accessible in both
2. **Support programmatic log generation** from LibreLog clock templates, enabling scheduled programming instead of random playlists
3. **Provide real-time and batch synchronization** capabilities for files, metadata, and scheduling data
4. **Replace/enhance the current LibreTime API** with more comprehensive functionality for external system integration
5. **Maintain backward compatibility** with existing LibreTime installations while providing enhanced capabilities
6. **Support second-level precision** in timing and scheduling operations

## User Stories

### As a LibreLog Administrator
- **US-1**: I want to upload audio files in LibreLog and have them automatically sync to LibreTime so that I can manage all content from one system
- **US-2**: I want audio files uploaded in LibreTime to be accessible in LibreLog so that I can schedule them in clock templates
- **US-3**: I want to generate a broadcast log from a clock template and push it to LibreTime so that programming is scheduled instead of random playlists
- **US-4**: I want to see real-time synchronization status of files between LibreLog and LibreTime so that I know when content is available
- **US-5**: I want to schedule programming with second-level precision so that commercial breaks and content align perfectly with clock templates

### As a LibreTime Administrator
- **US-6**: I want LibreTime to accept programmatic log generation from external systems so that I can integrate with traffic management systems
- **US-7**: I want file metadata (cue points, duration, tags) to sync bidirectionally so that both systems have complete information
- **US-8**: I want validation and conflict detection before accepting scheduled logs so that I can prevent scheduling errors

### As a Broadcast Operator
- **US-9**: I want clock templates from LibreLog to automatically create scheduled programming in LibreTime so that I don't have to manually create playlists
- **US-10**: I want to see the same audio files and metadata in both systems so that I can work in either interface

## Functional Requirements

### FR-1: File Synchronization API

#### FR-1.1: Upload File to LibreTime
- **REQ-1.1.1**: The API must accept file uploads via HTTP POST with multipart/form-data
- **REQ-1.1.2**: The API must accept metadata including:
  - File name
  - Cart number/identifier
  - Cue in point (milliseconds)
  - Cue out point (milliseconds)
  - Fade in duration (milliseconds)
  - Fade out duration (milliseconds)
  - Asset type (IM, ID, CM, PR, VT, SH)
  - Music category (S1, S2, S3)
  - Content type (MUSIC, TALK, INTERVIEW, MIXED, ADVERT)
  - Show segment name
  - Duration (seconds)
- **REQ-1.1.3**: The API must return a unique file/cart identifier upon successful upload
- **REQ-1.1.4**: The API must validate file format and size before accepting upload
- **REQ-1.1.5**: The API must support audio file formats: MP3, WAV, FLAC, OGG, AAC

#### FR-1.2: Retrieve File from LibreTime
- **REQ-1.2.1**: The API must provide an endpoint to retrieve file metadata by cart identifier
- **REQ-1.2.2**: The API must provide an endpoint to download the actual audio file
- **REQ-1.2.3**: The API must return all metadata associated with the file
- **REQ-1.2.4**: The API must support querying files by metadata criteria (asset type, content type, etc.)

#### FR-1.3: Update File Metadata
- **REQ-1.3.1**: The API must allow updating file metadata without re-uploading the file
- **REQ-1.3.2**: The API must support partial updates (only specified fields)
- **REQ-1.3.3**: The API must validate metadata updates before applying changes

#### FR-1.4: Delete File from LibreTime
- **REQ-1.4.1**: The API must provide an endpoint to delete files by cart identifier
- **REQ-1.4.2**: The API must check for file usage in scheduled shows before allowing deletion
- **REQ-1.4.3**: The API must return appropriate error messages if file is in use

#### FR-1.5: List Files
- **REQ-1.5.1**: The API must provide paginated listing of all files
- **REQ-1.5.2**: The API must support filtering by metadata fields
- **REQ-1.5.3**: The API must support sorting by various fields (name, upload date, duration, etc.)
- **REQ-1.5.4**: The API must return file count and pagination metadata

### FR-2: Log Generation and Scheduling API

#### FR-2.1: Generate Log from Clock Template
- **REQ-2.1.1**: The API must accept a clock template definition in JSON format
- **REQ-2.1.2**: The API must accept the following clock template structure:
  ```json
  {
    "name": "Template Name",
    "showName": "Show Name",
    "startDate": "2024-01-01",
    "endDate": "2024-01-31",
    "items": [
      {
        "type": "break|fixed_asset|automation_command",
        "name": "Item Name",
        "startTime": "00:15:00",
        "durationSeconds": 180,
        "cartId": "CART123",
        "playlistId": "PLAY123",
        "smartBlockId": "SMART123",
        "fadeIn": 2000,
        "fadeOut": 2000,
        "cueIn": 0,
        "cueOut": 180000,
        "metadata": {
          "assetType": "CM",
          "musicCategory": "S1",
          "contentType": "ADVERT"
        }
      }
    ]
  }
  ```
- **REQ-2.1.3**: The API must validate all referenced files, playlists, and smart blocks exist
- **REQ-2.1.4**: The API must check for timing conflicts and overlaps
- **REQ-2.1.5**: The API must return a detailed validation report before generating the log

#### FR-2.2: Create Show Instances
- **REQ-2.2.1**: The API must create show instances from clock template items
- **REQ-2.2.2**: The API must support second-level precision in start times (HH:MM:SS format)
- **REQ-2.2.3**: The API must map clock template items to LibreTime show items:
  - Fixed assets → File references with cue points
  - Breaks → Smart blocks or playlists
  - Automation commands → Appropriate LibreTime command types
- **REQ-2.2.4**: The API must preserve all metadata from clock templates in show instances

#### FR-2.3: Schedule Programming
- **REQ-2.3.1**: The API must accept date ranges for scheduling (start date, end date)
- **REQ-2.3.2**: The API must support recurring schedules (daily, weekly, etc.)
- **REQ-2.3.3**: The API must generate show instances for each occurrence in the date range
- **REQ-2.3.4**: The API must replace random playlist scheduling with clock template-based scheduling
- **REQ-2.3.5**: The API must return a summary of created show instances

#### FR-2.4: Query Scheduled Logs
- **REQ-2.4.1**: The API must provide endpoints to query scheduled shows by date range
- **REQ-2.4.2**: The API must return detailed show instance information including all items
- **REQ-2.4.3**: The API must support filtering by show name, date, or template name

#### FR-2.5: Update Scheduled Logs
- **REQ-2.5.1**: The API must allow updating show instances before they air
- **REQ-2.5.2**: The API must validate updates for conflicts and errors
- **REQ-2.5.3**: The API must prevent updates to shows that have already started or completed

#### FR-2.6: Delete Scheduled Logs
- **REQ-2.6.1**: The API must allow deletion of show instances before they air
- **REQ-2.6.2**: The API must prevent deletion of shows that have already started
- **REQ-2.6.3**: The API must support bulk deletion by date range or template name

### FR-3: Smart Block Management API

#### FR-3.1: Create Smart Block
- **REQ-3.1.1**: The API must accept smart block creation with criteria in JSON format
- **REQ-3.1.2**: The API must support criteria including:
  - Genre/tags
  - Artist
  - Duration range (min/max)
  - Content type
  - Music category
  - Asset type
- **REQ-3.1.3**: The API must return a unique smart block identifier

#### FR-3.2: Query Smart Block Contents
- **REQ-3.2.1**: The API must provide an endpoint to query what content a smart block would select
- **REQ-3.2.2**: The API must return available content matching smart block criteria
- **REQ-3.2.3**: The API must support "what-if" analysis before scheduling

#### FR-3.3: Update Smart Block
- **REQ-3.3.1**: The API must allow updating smart block criteria
- **REQ-3.3.2**: The API must validate criteria before applying updates

#### FR-3.4: List Smart Blocks
- **REQ-3.4.1**: The API must provide paginated listing of all smart blocks
- **REQ-3.4.2**: The API must return smart block criteria and metadata

### FR-4: Playlist Management API

#### FR-4.1: Create Playlist
- **REQ-4.1.1**: The API must accept playlist creation with name and description
- **REQ-4.1.2**: The API must allow adding items to playlist during creation
- **REQ-4.1.3**: The API must return a unique playlist identifier

#### FR-4.2: Add Items to Playlist
- **REQ-4.2.1**: The API must allow adding files, smart blocks, or other playlists to a playlist
- **REQ-4.2.2**: The API must support specifying item order/position
- **REQ-4.2.3**: The API must validate that referenced items exist

#### FR-4.3: Remove Items from Playlist
- **REQ-4.3.1**: The API must allow removing items from playlists by position or identifier
- **REQ-4.3.2**: The API must update playlist order after removal

#### FR-4.4: Query Playlist Contents
- **REQ-4.4.1**: The API must provide endpoints to query playlist contents
- **REQ-4.4.2**: The API must return all items in order with metadata

#### FR-4.5: Update Playlist
- **REQ-4.5.1**: The API must allow updating playlist name and description
- **REQ-4.5.2**: The API must allow reordering playlist items

### FR-5: Synchronization API

#### FR-5.1: Real-Time File Sync
- **REQ-5.1.1**: The API must support webhook notifications when files are added/updated/deleted in LibreTime
- **REQ-5.1.2**: The API must provide webhook configuration endpoints
- **REQ-5.1.3**: The API must include file metadata in webhook payloads

#### FR-5.2: Batch File Sync
- **REQ-5.2.1**: The API must support bulk file upload via batch endpoint
- **REQ-5.2.2**: The API must support bulk metadata updates
- **REQ-5.2.3**: The API must return batch operation status and results

#### FR-5.3: Sync Status
- **REQ-5.3.1**: The API must provide endpoints to query sync status between systems
- **REQ-5.3.2**: The API must track last sync timestamp per file
- **REQ-5.3.3**: The API must identify files that need synchronization

#### FR-5.4: Conflict Resolution
- **REQ-5.4.1**: The API must detect conflicts when the same file is modified in both systems
- **REQ-5.4.2**: The API must provide conflict resolution strategies (last-write-wins, manual resolution, etc.)
- **REQ-5.4.3**: The API must return conflict details for manual resolution

### FR-6: Validation and Error Handling

#### FR-6.1: Pre-Flight Validation
- **REQ-6.1.1**: The API must validate clock templates before generating logs
- **REQ-6.1.2**: The API must check for:
  - Missing files/carts
  - Invalid timing (overlaps, gaps, out-of-range)
  - Invalid metadata values
  - Missing required fields
- **REQ-6.1.3**: The API must return detailed validation errors with field-level information

#### FR-6.2: Conflict Detection
- **REQ-6.2.1**: The API must detect overlapping show instances
- **REQ-6.2.2**: The API must detect scheduling conflicts with existing shows
- **REQ-6.2.3**: The API must return conflict details with timestamps and affected shows

#### FR-6.3: Content Availability Validation
- **REQ-6.3.1**: The API must verify that all referenced files exist before scheduling
- **REQ-6.3.2**: The API must verify that smart blocks have sufficient content available
- **REQ-6.3.3**: The API must return warnings for low-content smart blocks

#### FR-6.4: Error Messages
- **REQ-6.4.1**: The API must return detailed, actionable error messages
- **REQ-6.4.2**: The API must include error codes for programmatic handling
- **REQ-6.4.3**: The API must provide suggestions for resolving errors

### FR-7: Authentication and Authorization

#### FR-7.1: API Authentication
- **REQ-7.1.1**: The API must support API key authentication
- **REQ-7.1.2**: The API must support OAuth2 token authentication
- **REQ-7.1.3**: The API must support JWT token authentication
- **REQ-7.1.4**: The API must validate authentication on all endpoints

#### FR-7.2: Authorization
- **REQ-7.2.1**: The API must support role-based access control
- **REQ-7.2.2**: The API must enforce permissions for file operations, scheduling, and administration
- **REQ-7.2.3**: The API must return appropriate error messages for unauthorized access

### FR-8: API Versioning and Compatibility

#### FR-8.1: API Versioning
- **REQ-8.1.1**: The API must support versioning via URL path (e.g., `/api/v2/`)
- **REQ-8.1.2**: The API must maintain backward compatibility with existing LibreTime API
- **REQ-8.1.3**: The API must document deprecation timelines for old versions

#### FR-8.2: Response Format
- **REQ-8.2.1**: The API must return JSON responses
- **REQ-8.2.2**: The API must follow consistent response structure:
  ```json
  {
    "success": true|false,
    "data": {},
    "errors": [],
    "warnings": [],
    "metadata": {
      "timestamp": "2024-01-01T00:00:00Z",
      "version": "2.0"
    }
  }
  ```

## Non-Goals (Out of Scope)

1. **LibreTime UI Modifications**: This PRD does not include changes to LibreTime's web interface or user experience
2. **LibreTime Core Functionality Changes**: This PRD does not modify LibreTime's core audio playback, automation, or scheduling engine
3. **Migration Tools**: This PRD does not include tools for migrating from other systems (WideOrbit, etc.)
4. **LibreTime Database Schema Changes**: This PRD assumes API-level changes only, not direct database modifications
5. **Voice Tracking Implementation Location**: **Open Question** - Should voice tracking functionality be implemented in LibreTime or LibreLog? This decision should be made based on:
   - Where voice tracks are typically created/managed
   - Integration complexity
   - User workflow preferences
   - System architecture considerations

## Design Considerations

### API Endpoint Structure

All new endpoints should follow RESTful conventions:
- `GET /api/v2/files` - List files
- `POST /api/v2/files` - Upload file
- `GET /api/v2/files/{cartId}` - Get file metadata
- `PUT /api/v2/files/{cartId}` - Update file metadata
- `DELETE /api/v2/files/{cartId}` - Delete file
- `POST /api/v2/logs/generate` - Generate log from clock template
- `POST /api/v2/logs/schedule` - Schedule programming
- `GET /api/v2/logs` - Query scheduled logs
- `PUT /api/v2/logs/{showId}` - Update scheduled log
- `DELETE /api/v2/logs/{showId}` - Delete scheduled log
- `POST /api/v2/smart-blocks` - Create smart block
- `GET /api/v2/smart-blocks` - List smart blocks
- `GET /api/v2/smart-blocks/{id}/contents` - Query smart block contents
- `POST /api/v2/playlists` - Create playlist
- `GET /api/v2/playlists/{id}` - Get playlist
- `POST /api/v2/sync/webhooks` - Configure webhooks
- `POST /api/v2/sync/batch` - Batch synchronization

### Metadata Mapping

The API must support mapping between LibreLog's WideOrbit-compatible fields and LibreTime's native fields:

| LibreLog Field | LibreTime Field | Notes |
|---------------|-----------------|-------|
| Asset Type (IM, ID, CM, PR, VT, SH) | File Type / Metadata Tags | Map to LibreTime file types and tags |
| Music Category (S1, S2, S3) | Metadata Tags | Store as tags for smart block criteria |
| Show Segment Name | Show Instance Name / Metadata | Include in show instance naming |
| Transition Code | Fade In/Out | Convert to fade durations |
| Cue In/Out | Cue Points | Direct mapping in milliseconds |

### Timing Precision

- All time values must support second-level precision (HH:MM:SS)
- Cue points and fade durations use millisecond precision
- Date/time values must be in ISO 8601 format with timezone support

## Technical Considerations

### Dependencies

- The API should be built on LibreTime's existing API framework
- Should maintain compatibility with LibreTime's current database schema
- Should leverage existing LibreTime services where possible

### Performance Requirements

- File uploads should support files up to 500MB
- API responses should return within 2 seconds for standard operations
- Batch operations should support up to 1000 items per request
- Real-time sync should have latency under 5 seconds

### Error Handling

- All endpoints must return appropriate HTTP status codes
- Error responses must include detailed error messages
- Validation errors must be returned before processing begins
- Partial failures in batch operations must be clearly reported

### Security

- All endpoints must require authentication
- File uploads must be validated for malicious content
- API keys must be securely stored and transmitted
- Rate limiting should be implemented to prevent abuse

## Success Metrics

1. **File Synchronization Success Rate**: 100% of files uploaded in LibreLog successfully sync to LibreTime
2. **File Synchronization Success Rate**: 100% of files uploaded in LibreTime are accessible in LibreLog
3. **Log Generation Success Rate**: 100% of valid clock templates successfully generate logs in LibreTime
4. **Scheduling Accuracy**: 100% of scheduled shows match clock template timing (second-level precision)
5. **API Response Time**: 95% of API requests complete within 2 seconds
6. **Sync Latency**: Real-time sync completes within 5 seconds of file change
7. **Error Rate**: Less than 1% of API requests result in errors
8. **Validation Coverage**: 100% of invalid clock templates are caught before scheduling

## Open Questions

1. **Voice Tracking Location**: Should voice tracking functionality be implemented in LibreTime or LibreLog? Considerations:
   - Voice tracks are typically created in traffic/logging systems (LibreLog)
   - Voice tracks need to be scheduled alongside other content (LibreTime)
   - Recommendation: Implement voice track creation/management in LibreLog, but ensure LibreTime API can accept and schedule voice tracks with proper metadata

2. **Automation Commands**: While not required at this stage, should the API design include hooks for future automation command support?

3. **Webhook Reliability**: How should webhook delivery failures be handled? Should there be a retry mechanism or fallback to polling?

4. **File Storage**: Should files be stored in LibreTime's existing storage system, or should there be a shared storage location accessible by both systems?

5. **Metadata Schema Evolution**: How should metadata schema changes be handled to maintain backward compatibility?

6. **Rate Limiting**: What are the appropriate rate limits for different types of operations (file uploads, log generation, queries)?

## Implementation Priority

### Phase 1: Core File Synchronization (High Priority)
- File upload/download API
- File metadata management
- Basic file listing and querying

### Phase 2: Log Generation and Scheduling (High Priority)
- Clock template validation
- Log generation from templates
- Show instance creation
- Scheduling API

### Phase 3: Smart Blocks and Playlists (Medium Priority)
- Smart block creation and querying
- Playlist management API
- Content availability checking

### Phase 4: Advanced Synchronization (Medium Priority)
- Real-time webhooks
- Batch operations
- Conflict resolution

### Phase 5: Enhanced Features (Lower Priority)
- Advanced validation
- Performance optimizations
- Extended metadata support

## References

- LibreTime API Documentation: https://libretime.org/docs/api/
- LibreTime Smart Blocks: https://libretime.org/docs/stable-3.x/user-manual/playlists/
- LibreLog Clock Template Builder PRD: `prd-clock-template-builder.md`
- LibreTime Integration Adjustments: `libretime-integration-adjustments.md`


