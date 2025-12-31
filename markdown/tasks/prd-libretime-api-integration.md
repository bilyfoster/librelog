# Product Requirements Document: LibreLog-LibreTime API Integration

## Introduction/Overview

LibreLog is a broadcast traffic management system that integrates with LibreTime for audio scheduling and automation. This PRD defines the implementation of a comprehensive API integration between LibreLog and LibreTime via the LibreTime API v2 endpoint (`https://studio-qa.gayphx.com/api/v2/`).

The integration will enable:
- **Two-way file synchronization** between LibreLog and LibreTime (upload/download audio files)
- **Clock template export** to generate scheduled programming in LibreTime
- **Real-time status monitoring** of synchronization operations
- **Comprehensive API testing and documentation** to identify working and broken endpoints

A user-friendly settings UI will allow administrators and authorized users to configure the integration, monitor sync status, and manage API connections. All API endpoints will be thoroughly tested and documented, with broken or missing functionality clearly identified for the LibreTime development team.

## Goals

1. **Implement two-way file synchronization** between LibreLog and LibreTime, with file upload/download as the highest priority feature
2. **Create a comprehensive settings UI** for configuring and managing the LibreTime API integration
3. **Discover and test all available LibreTime API endpoints** to understand current capabilities
4. **Document API endpoint status** (working, broken, missing) with detailed test results and error reports
5. **Implement clock template export** to generate scheduled programming in LibreTime
6. **Provide real-time sync status monitoring** with error reporting and conflict detection
7. **Support role-based access control** for integration settings and operations
8. **Ensure secure authentication** using JWT token-based authentication with the LibreTime API

## User Stories

### As a LibreLog Administrator
- **US-1**: I want to configure LibreTime API connection settings (URL, JWT token) so that LibreLog can communicate with LibreTime
- **US-2**: I want to see a list of all discovered LibreTime API endpoints with their status (working/broken/missing) so that I know what functionality is available
- **US-3**: I want to upload audio files in LibreLog and have them automatically sync to LibreTime so that I can manage all content from one system
- **US-4**: I want to download audio files from LibreTime into LibreLog so that I can schedule them in clock templates
- **US-5**: I want to see real-time synchronization status of files between LibreLog and LibreTime so that I know when content is available
- **US-6**: I want to configure sync preferences (auto-sync, manual sync, sync frequency) so that I can control when synchronization occurs
- **US-7**: I want to export clock templates to LibreTime to generate scheduled programming so that shows are automatically scheduled
- **US-8**: I want to see detailed error reports when API calls fail so that I can troubleshoot issues or report them to the LibreTime team

### As a LibreLog Operator
- **US-9**: I want to manually trigger file synchronization when needed so that I can ensure content is up-to-date
- **US-10**: I want to see sync logs and history so that I can track what files have been synchronized
- **US-11**: I want to view API test results so that I understand which LibreTime features are working

### As a System Administrator
- **US-12**: I want to control who can access integration settings through role-based permissions so that only authorized users can modify API configuration
- **US-13**: I want to configure webhook endpoints for real-time sync notifications so that changes in LibreTime are immediately reflected in LibreLog
- **US-14**: I want to export API documentation and test results so that I can provide detailed reports to the LibreTime development team

## Functional Requirements

### FR-1: Settings UI Implementation

#### FR-1.1: API Connection Configuration
- **REQ-1.1.1**: The UI must provide a form to configure LibreTime API base URL (default: `https://studio-qa.gayphx.com/api/v2/`)
- **REQ-1.1.2**: The UI must provide a secure field to enter and store JWT authentication token
- **REQ-1.1.3**: The UI must provide a "Test Connection" button to verify API connectivity and authentication
- **REQ-1.1.4**: The UI must display connection status (connected, disconnected, error) with visual indicators
- **REQ-1.1.5**: The UI must validate URL format before saving configuration
- **REQ-1.1.6**: The UI must mask JWT token in display (show only last 4 characters) for security
- **REQ-1.1.7**: The UI must provide a "Refresh Token" option to update authentication credentials

#### FR-1.2: Sync Preferences Configuration
- **REQ-1.2.1**: The UI must provide options to enable/disable automatic file synchronization
- **REQ-1.2.2**: The UI must provide sync frequency settings (real-time, every 5 minutes, every 15 minutes, hourly, manual only)
- **REQ-1.2.3**: The UI must provide options to configure sync direction:
  - Bidirectional (LibreLog ↔ LibreTime)
  - LibreLog → LibreTime only
  - LibreTime → LibreLog only
- **REQ-1.2.4**: The UI must provide a "Sync Now" button for manual synchronization
- **REQ-1.2.5**: The UI must display last sync timestamp and next scheduled sync time
- **REQ-1.2.6**: The UI must provide conflict resolution settings (last-write-wins, manual resolution, LibreLog priority, LibreTime priority)

#### FR-1.3: File Upload/Download Settings
- **REQ-1.3.1**: The UI must provide configuration for supported file formats (MP3, WAV, FLAC, OGG, AAC)
- **REQ-1.3.2**: The UI must provide maximum file size limit configuration (default: 500MB)
- **REQ-1.3.3**: The UI must provide storage path configuration for downloaded files
- **REQ-1.3.4**: The UI must provide options to automatically sync metadata (cue points, duration, tags) with files
- **REQ-1.3.5**: The UI must provide options to preserve file organization (folders, categories) during sync

#### FR-1.4: Log Generation Settings
- **REQ-1.4.1**: The UI must provide options to enable/disable automatic log generation from clock templates
- **REQ-1.4.2**: The UI must provide validation rule configuration (strict, moderate, lenient)
- **REQ-1.4.3**: The UI must provide conflict resolution settings for scheduled shows (replace, skip, merge)
- **REQ-1.4.4**: The UI must provide options to preview log generation before applying to LibreTime
- **REQ-1.4.5**: The UI must provide scheduling options (immediate, scheduled time, batch)

#### FR-1.5: Webhook Configuration
- **REQ-1.5.1**: The UI must provide webhook endpoint URL configuration for receiving LibreTime notifications
- **REQ-1.5.2**: The UI must provide webhook secret/token configuration for security
- **REQ-1.5.3**: The UI must provide options to enable/disable webhook notifications
- **REQ-1.5.4**: The UI must display webhook delivery status and last received notification timestamp
- **REQ-1.5.5**: The UI must provide webhook test functionality to verify endpoint is working

#### FR-1.6: UI Design Requirements
- **REQ-1.6.1**: The UI must be fully responsive and work on mobile, tablet, and desktop devices
- **REQ-1.6.2**: The UI must meet WCAG 2.1 Level AA accessibility standards
- **REQ-1.6.3**: The UI must use semantic HTML5 elements and proper ARIA attributes
- **REQ-1.6.4**: The UI must be keyboard navigable with visible focus indicators
- **REQ-1.6.5**: The UI must provide clear visual feedback for all user actions (loading states, success, errors)
- **REQ-1.6.6**: The UI must organize settings into logical sections with clear headings
- **REQ-1.6.7**: The UI must provide help text and tooltips for complex settings

### FR-2: API Client Service Implementation

#### FR-2.1: HTTP Client Service
- **REQ-2.1.1**: The service must implement HTTP client for making requests to LibreTime API
- **REQ-2.1.2**: The service must include JWT token authentication in all API requests (Authorization header)
- **REQ-2.1.3**: The service must handle HTTP errors (4xx, 5xx) with appropriate error messages
- **REQ-2.1.4**: The service must implement request timeout handling (default: 30 seconds)
- **REQ-2.1.5**: The service must implement retry logic for transient failures (network errors, 5xx errors)
- **REQ-2.1.6**: The service must log all API requests and responses for debugging
- **REQ-2.1.7**: The service must validate API base URL format before making requests

#### FR-2.2: File Upload Service
- **REQ-2.2.1**: The service must implement file upload to LibreTime API using multipart/form-data
- **REQ-2.2.2**: The service must include file metadata in upload request (name, cart ID, cue points, fade settings, asset type, etc.)
- **REQ-2.2.3**: The service must validate file format and size before upload
- **REQ-2.2.4**: The service must track upload progress and provide status updates
- **REQ-2.2.5**: The service must handle upload failures and provide detailed error messages
- **REQ-2.2.6**: The service must store LibreTime cart ID after successful upload for future reference

#### FR-2.3: File Download Service
- **REQ-2.3.1**: The service must implement file download from LibreTime API
- **REQ-2.3.2**: The service must retrieve file metadata along with file download
- **REQ-2.3.3**: The service must save downloaded files to configured storage path
- **REQ-2.3.4**: The service must preserve file metadata in LibreLog database
- **REQ-2.3.5**: The service must handle download failures and provide detailed error messages
- **REQ-2.3.6**: The service must track download progress for large files

#### FR-2.4: File List and Query Service
- **REQ-2.4.1**: The service must implement file listing from LibreTime API with pagination support
- **REQ-2.4.2**: The service must implement file querying by metadata (asset type, content type, etc.)
- **REQ-2.4.3**: The service must cache file list results for performance
- **REQ-2.4.4**: The service must support filtering and sorting of file lists
- **REQ-2.4.5**: The service must handle API pagination automatically

#### FR-2.5: Clock Template Export Service
- **REQ-2.5.1**: The service must convert LibreLog clock templates to LibreTime log format
- **REQ-2.5.2**: The service must validate clock template before export (check for missing files, invalid timing, etc.)
- **REQ-2.5.3**: The service must map LibreLog asset types to LibreTime metadata
- **REQ-2.5.4**: The service must create show instances in LibreTime via API
- **REQ-2.5.5**: The service must handle scheduling conflicts and provide resolution options
- **REQ-2.5.6**: The service must return detailed export results (success, failures, warnings)

#### FR-2.6: Sync Status Service
- **REQ-2.6.1**: The service must track sync status for each file (pending, syncing, synced, failed)
- **REQ-2.6.2**: The service must maintain sync history with timestamps
- **REQ-2.6.3**: The service must detect sync conflicts (same file modified in both systems)
- **REQ-2.6.4**: The service must provide sync statistics (total files, synced, failed, pending)
- **REQ-2.6.5**: The service must implement real-time status updates via polling or webhooks

### FR-3: API Endpoint Discovery and Testing

#### FR-3.1: Endpoint Discovery Service
- **REQ-3.1.1**: The service must attempt to discover available LibreTime API endpoints by testing common REST patterns
- **REQ-3.1.2**: The service must test standard CRUD endpoints for each resource type (files, playlists, smart blocks, shows, etc.)
- **REQ-3.1.3**: The service must document discovered endpoints with HTTP methods (GET, POST, PUT, DELETE, PATCH)
- **REQ-3.1.4**: The service must identify endpoint URL patterns and parameters
- **REQ-3.1.5**: The service must store discovered endpoint information in database for reference

#### FR-3.2: Endpoint Testing Service
- **REQ-3.2.1**: The service must test basic connectivity to LibreTime API (health check, ping endpoint)
- **REQ-3.2.2**: The service must test authentication with JWT token
- **REQ-3.2.3**: The service must test each discovered endpoint with appropriate HTTP methods
- **REQ-3.2.4**: The service must test CRUD operations for each resource type:
  - Create (POST) operations
  - Read (GET) operations
  - Update (PUT/PATCH) operations
  - Delete (DELETE) operations
- **REQ-3.2.5**: The service must test error scenarios (invalid data, missing resources, unauthorized access)
- **REQ-3.2.6**: The service must test edge cases (empty lists, large payloads, special characters)
- **REQ-3.2.7**: The service must measure response times for each endpoint
- **REQ-3.2.8**: The service must test pagination where applicable

#### FR-3.3: Test Result Documentation
- **REQ-3.3.1**: The service must categorize each endpoint as: Working, Broken, Missing, or Unknown
- **REQ-3.3.2**: The service must record detailed test results including:
  - Endpoint URL and HTTP method
  - Request payload (if applicable)
  - Response status code
  - Response body (success and error cases)
  - Response time
  - Error messages (if any)
  - Test timestamp
- **REQ-3.3.3**: The service must generate test result reports in multiple formats (JSON, Markdown, HTML)
- **REQ-3.3.4**: The service must provide summary statistics (total endpoints, working count, broken count, missing count)
- **REQ-3.3.5**: The service must export test results for sharing with LibreTime development team

#### FR-3.4: Bug Report Generation
- **REQ-3.4.1**: The service must generate detailed bug reports for broken endpoints
- **REQ-3.4.2**: Each bug report must include:
  - Endpoint URL and HTTP method
  - Expected behavior
  - Actual behavior
  - Request example (with sample data)
  - Response example (error response)
  - Error message and stack trace (if available)
  - Steps to reproduce
  - Environment information (API version, LibreTime version if available)
- **REQ-3.4.3**: The service must generate bug reports in formats suitable for issue tracking systems (GitHub issues, JIRA, etc.)
- **REQ-3.4.4**: The service must prioritize bugs by severity (critical, high, medium, low)

### FR-4: Database Schema

#### FR-4.1: Integration Configuration Table
- **REQ-4.1.1**: Create `libretime_integration_config` table to store API connection settings
- **REQ-4.1.2**: Fields must include:
  - `id` (UUID, primary key)
  - `api_base_url` (VARCHAR, not null)
  - `jwt_token` (VARCHAR, encrypted)
  - `sync_enabled` (BOOLEAN, default false)
  - `sync_frequency` (VARCHAR, enum: real-time, 5min, 15min, hourly, manual)
  - `sync_direction` (VARCHAR, enum: bidirectional, librelog_to_libretime, libretime_to_librelog)
  - `conflict_resolution` (VARCHAR, enum: last_write_wins, manual, librelog_priority, libretime_priority)
  - `webhook_url` (VARCHAR, nullable)
  - `webhook_secret` (VARCHAR, encrypted, nullable)
  - `webhook_enabled` (BOOLEAN, default false)
  - `max_file_size_mb` (INTEGER, default 500)
  - `supported_formats` (TEXT, JSON array)
  - `created_at` (TIMESTAMP)
  - `updated_at` (TIMESTAMP)
  - `created_by` (UUID, foreign key to users)
  - `updated_by` (UUID, foreign key to users)

#### FR-4.2: API Endpoint Registry Table
- **REQ-4.2.1**: Create `libretime_api_endpoints` table to store discovered endpoint information
- **REQ-4.2.2**: Fields must include:
  - `id` (UUID, primary key)
  - `endpoint_path` (VARCHAR, not null)
  - `http_method` (VARCHAR, not null, enum: GET, POST, PUT, DELETE, PATCH)
  - `resource_type` (VARCHAR, e.g., files, playlists, smart-blocks, shows)
  - `status` (VARCHAR, enum: working, broken, missing, unknown)
  - `last_tested_at` (TIMESTAMP)
  - `response_time_ms` (INTEGER, nullable)
  - `requires_authentication` (BOOLEAN, default true)
  - `description` (TEXT, nullable)
  - `documentation_url` (VARCHAR, nullable)
  - `created_at` (TIMESTAMP)
  - `updated_at` (TIMESTAMP)

#### FR-4.3: API Test Results Table
- **REQ-4.3.1**: Create `libretime_api_test_results` table to store detailed test results
- **REQ-4.3.2**: Fields must include:
  - `id` (UUID, primary key)
  - `endpoint_id` (UUID, foreign key to libretime_api_endpoints)
  - `test_type` (VARCHAR, enum: connectivity, authentication, crud, error_handling, edge_case)
  - `status` (VARCHAR, enum: passed, failed, error, skipped)
  - `request_payload` (TEXT, JSON, nullable)
  - `response_status_code` (INTEGER, nullable)
  - `response_body` (TEXT, JSON, nullable)
  - `error_message` (TEXT, nullable)
  - `response_time_ms` (INTEGER, nullable)
  - `test_timestamp` (TIMESTAMP)
  - `notes` (TEXT, nullable)

#### FR-4.4: File Sync Status Table
- **REQ-4.4.1**: Create `libretime_file_sync_status` table to track file synchronization
- **REQ-4.4.2**: Fields must include:
  - `id` (UUID, primary key)
  - `librelog_file_id` (UUID, foreign key to files/assets, nullable)
  - `libretime_cart_id` (VARCHAR, nullable)
  - `file_name` (VARCHAR, not null)
  - `sync_direction` (VARCHAR, enum: upload, download, bidirectional)
  - `sync_status` (VARCHAR, enum: pending, syncing, synced, failed, conflict)
  - `last_sync_at` (TIMESTAMP, nullable)
  - `sync_error` (TEXT, nullable)
  - `file_size_bytes` (BIGINT, nullable)
  - `file_hash` (VARCHAR, nullable, for conflict detection)
  - `metadata_synced` (BOOLEAN, default false)
  - `created_at` (TIMESTAMP)
  - `updated_at` (TIMESTAMP)

#### FR-4.5: Sync History Table
- **REQ-4.5.1**: Create `libretime_sync_history` table to maintain sync operation history
- **REQ-4.5.2**: Fields must include:
  - `id` (UUID, primary key)
  - `sync_type` (VARCHAR, enum: file_upload, file_download, batch_sync, log_export, manual)
  - `status` (VARCHAR, enum: started, completed, failed, cancelled)
  - `items_total` (INTEGER, default 0)
  - `items_succeeded` (INTEGER, default 0)
  - `items_failed` (INTEGER, default 0)
  - `started_at` (TIMESTAMP)
  - `completed_at` (TIMESTAMP, nullable)
  - `error_summary` (TEXT, nullable)
  - `initiated_by` (UUID, foreign key to users)
  - `details` (TEXT, JSON, nullable)

### FR-5: Role-Based Access Control

#### FR-5.1: Permission Management
- **REQ-5.1.1**: Create permissions for integration management:
  - `LIBRETIME_INTEGRATION_VIEW` - View integration settings and status
  - `LIBRETIME_INTEGRATION_CONFIGURE` - Modify integration settings
  - `LIBRETIME_INTEGRATION_TEST` - Run API tests and discovery
  - `LIBRETIME_INTEGRATION_SYNC` - Trigger manual synchronization
  - `LIBRETIME_INTEGRATION_EXPORT_LOGS` - Export test results and documentation
- **REQ-5.1.2**: Assign default permissions to user roles:
  - Administrators: All permissions
  - Operators: VIEW, SYNC permissions
  - Viewers: VIEW permission only
- **REQ-5.1.3**: The UI must check permissions before displaying or enabling features
- **REQ-5.1.4**: The API must enforce permissions on all integration endpoints

#### FR-5.2: Access Control Implementation
- **REQ-5.2.1**: Integration settings page must be accessible only to users with VIEW permission
- **REQ-5.2.2**: Settings modification must require CONFIGURE permission
- **REQ-5.2.3**: API testing features must require TEST permission
- **REQ-5.2.4**: Manual sync operations must require SYNC permission
- **REQ-5.2.5**: Export functionality must require appropriate permissions

### FR-6: Error Handling and Logging

#### FR-6.1: Error Handling
- **REQ-6.1.1**: All API calls must handle network errors gracefully
- **REQ-6.1.2**: All API calls must handle authentication errors (401, 403) with clear messages
- **REQ-6.1.3**: All API calls must handle validation errors (400) and display field-level errors
- **REQ-6.1.4**: All API calls must handle server errors (500, 502, 503) with retry logic
- **REQ-6.1.5**: Error messages must be user-friendly and actionable
- **REQ-6.1.6**: Technical error details must be logged for debugging but not exposed to end users

#### FR-6.2: Logging
- **REQ-6.2.1**: All API requests must be logged with request details (URL, method, payload)
- **REQ-6.2.2**: All API responses must be logged with response details (status, body, timing)
- **REQ-6.2.3**: Sync operations must be logged with start/end times and results
- **REQ-6.2.4**: Errors must be logged with stack traces and context
- **REQ-6.2.5**: Logs must use appropriate log levels (INFO, WARN, ERROR, DEBUG)
- **REQ-6.2.6**: Sensitive information (JWT tokens, passwords) must be masked in logs

### FR-7: Testing Requirements

#### FR-7.1: Unit Tests
- **REQ-7.1.1**: All service classes must have unit tests with minimum 80% code coverage
- **REQ-7.1.2**: API client service must be tested with mocked HTTP responses
- **REQ-7.1.3**: File upload/download services must be tested with mock file operations
- **REQ-7.1.4**: Clock template export service must be tested with sample templates
- **REQ-7.1.5**: Error handling must be tested for all error scenarios

#### FR-7.2: Integration Tests
- **REQ-7.2.1**: Integration tests must test actual API calls to LibreTime (if test instance available)
- **REQ-7.2.2**: Integration tests must use Testcontainers or similar for external dependencies
- **REQ-7.2.3**: Integration tests must test end-to-end workflows (configure → sync → verify)
- **REQ-7.2.4**: Integration tests must clean up test data after execution

#### FR-7.3: UI Tests
- **REQ-7.3.1**: Settings UI must be tested for accessibility (keyboard navigation, screen readers)
- **REQ-7.3.2**: Settings UI must be tested for responsive design (mobile, tablet, desktop)
- **REQ-7.3.3**: Form validation must be tested for all input fields
- **REQ-7.3.4**: Permission-based UI visibility must be tested

## Non-Goals (Out of Scope)

1. **LibreTime API Modifications**: This PRD does not include modifying the LibreTime API itself - only consuming and testing it
2. **LibreTime UI Changes**: This PRD does not include changes to LibreTime's web interface
3. **File Storage Migration**: This PRD does not include migrating existing files between systems (only new sync operations)
4. **Legacy System Migration**: This PRD does not include migration tools from WideOrbit or other systems
5. **Voice Tracking Implementation**: Voice tracking functionality is out of scope for this integration (may be addressed in future PRD)
6. **Real-time Audio Streaming**: This PRD does not include audio streaming capabilities
7. **Multi-Instance LibreTime Support**: This PRD assumes integration with a single LibreTime instance (multi-instance support may be future enhancement)

## Design Considerations

### Settings UI Layout

The settings UI should be organized into the following sections:

1. **Connection Settings** (Primary Section)
   - API Base URL input
   - JWT Token input (masked)
   - Test Connection button
   - Connection status indicator

2. **Synchronization Settings**
   - Enable/disable auto-sync toggle
   - Sync frequency dropdown
   - Sync direction radio buttons
   - Conflict resolution dropdown
   - Manual sync button

3. **File Settings**
   - Supported formats checklist
   - Max file size input
   - Storage path input
   - Metadata sync toggle

4. **Log Generation Settings**
   - Enable/disable auto-export toggle
   - Validation level dropdown
   - Conflict resolution for shows
   - Preview before export toggle

5. **Webhook Settings**
   - Webhook URL input
   - Webhook secret input
   - Enable/disable toggle
   - Test webhook button
   - Delivery status display

6. **API Testing & Documentation** (Secondary Section)
   - Discover Endpoints button
   - Run Tests button
   - Test Results table/view
   - Export Documentation button
   - Bug Report generation

7. **Sync Status & History** (Secondary Section)
   - Current sync status dashboard
   - Sync history table
   - File sync status list
   - Error log viewer

### API Client Architecture

The API client should follow a layered architecture:

```
Controller Layer (REST API)
    ↓
Service Layer (Business Logic)
    ↓
Client Layer (HTTP Client, API-specific logic)
    ↓
HTTP Client (Spring RestTemplate/WebClient)
```

### Error Response Format

All API error responses should follow a consistent format:

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "User-friendly error message",
    "details": "Technical details (for logging)",
    "timestamp": "2024-01-01T00:00:00Z"
  }
}
```

## Technical Considerations

### Technology Stack
- **Backend**: Spring Boot (Java) - following coding guidelines
- **Frontend**: HTML/CSS/JavaScript (or framework if already in use)
- **Database**: PostgreSQL (via existing Liquibase migrations)
- **HTTP Client**: Spring WebClient or RestTemplate
- **Authentication**: JWT token in Authorization header
- **Encryption**: Use Spring's encryption utilities for sensitive data storage

### Dependencies
- Spring Web (for REST API and HTTP client)
- Spring Security (for role-based access control)
- Jackson (for JSON processing)
- Liquibase (for database migrations)
- Log4j (for logging)
- JUnit 5 and Mockito (for testing)
- JaCoCo (for code coverage)

### Performance Requirements
- API connection test should complete within 5 seconds
- File upload should support files up to 500MB
- File list queries should return results within 2 seconds
- Sync operations should process files in batches (100 files per batch)
- UI should be responsive with loading indicators for long operations

### Security Considerations
- JWT tokens must be encrypted at rest in database
- API credentials must never be logged in plain text
- All API communications must use HTTPS
- Webhook endpoints must validate webhook secret
- Role-based access must be enforced on all endpoints

### API Endpoint Testing Strategy

1. **Discovery Phase**: Test common REST patterns to discover available endpoints
2. **Documentation Phase**: Document discovered endpoints with request/response examples
3. **Functional Testing Phase**: Test each endpoint with valid data
4. **Error Testing Phase**: Test error scenarios (invalid data, missing resources)
5. **Performance Testing Phase**: Measure response times and identify slow endpoints
6. **Documentation Generation Phase**: Generate comprehensive test reports

## Success Metrics

1. **API Connectivity**: 100% success rate for API connection tests when credentials are valid
2. **File Upload Success Rate**: 95% of file uploads complete successfully
3. **File Download Success Rate**: 95% of file downloads complete successfully
4. **Endpoint Discovery**: 90% of expected endpoints are discovered and tested
5. **Test Coverage**: 100% of discovered endpoints have test results documented
6. **UI Responsiveness**: All UI operations complete within 2 seconds (excluding file transfers)
7. **Error Handling**: 100% of API errors are caught and logged with actionable messages
8. **Code Coverage**: Minimum 80% code coverage for all new code (verified via JaCoCo)
9. **Accessibility**: Settings UI passes WCAG 2.1 Level AA compliance tests
10. **Documentation Quality**: 100% of broken endpoints have detailed bug reports generated

## Open Questions

1. **LibreTime API Version**: What version of LibreTime API v2 is running at `https://studio-qa.gayphx.com/api/v2/`? Is there API documentation available?

2. **JWT Token Management**: How are JWT tokens obtained? Is there a token refresh endpoint? What is the token expiration time?

3. **API Rate Limiting**: Are there rate limits on the LibreTime API? What are the limits for different operations?

4. **File Storage Location**: Where are files stored in LibreTime? Is there a shared storage location, or do files need to be transferred?

5. **Webhook Support**: Does LibreTime support webhooks for real-time notifications? What events can trigger webhooks?

6. **API Endpoint Documentation**: Is there existing API documentation for LibreTime v2 that we can reference, or do we need to discover everything?

7. **Test Environment**: Is `https://studio-qa.gayphx.com/api/v2/` a test/QA environment? Is there a production environment we should also test against?

8. **LibreTime Version**: What version of LibreTime is running? Are there version-specific API differences we need to account for?

9. **Error Response Format**: What format do LibreTime API errors follow? Is it consistent across all endpoints?

10. **Authentication Scope**: Do different API endpoints require different permission levels? Are there read-only vs read-write tokens?

## Implementation Priority

### Phase 1: Foundation (High Priority)
1. Database schema creation (Liquibase changesets)
2. API client service implementation (HTTP client, JWT authentication)
3. Basic connection testing functionality
4. Settings UI - Connection Settings section
5. Role-based access control implementation

### Phase 2: File Synchronization (Highest Priority - Per User Requirement)
1. File upload service implementation
2. File download service implementation
3. File list/query service implementation
4. Sync status tracking
5. Settings UI - File Settings and Sync Settings sections
6. Unit and integration tests for file operations

### Phase 3: API Discovery and Testing (High Priority)
1. Endpoint discovery service
2. Endpoint testing service
3. Test result storage and retrieval
4. Bug report generation
5. Settings UI - API Testing & Documentation section
6. Test result export functionality

### Phase 4: Clock Template Export (Medium Priority)
1. Clock template to LibreTime log conversion
2. Show instance creation service
3. Validation and conflict detection
4. Settings UI - Log Generation Settings section
5. Export preview functionality

### Phase 5: Advanced Features (Medium Priority)
1. Webhook implementation
2. Real-time sync status updates
3. Batch synchronization
4. Conflict resolution UI
5. Sync history and reporting

### Phase 6: Polish and Documentation (Lower Priority)
1. UI/UX improvements
2. Comprehensive error handling
3. Performance optimizations
4. Documentation generation
5. Final testing and bug fixes

## References

- LibreTime API Documentation: https://libretime.org/docs/api/
- LibreTime Integration Adjustments: `libretime-integration-adjustments.md`
- LibreTime API Extensions PRD: `prd-libretime-api-extensions.md`
- Coding Guidelines: `coding_guidelines.md`
- LibreLog Clock Template Builder PRD: `prd-clock-template-builder.md`

