# Task List: LibreLog-LibreTime API Integration

Based on PRD: `prd-libretime-api-integration.md`

## Relevant Files

### Database Schema
- `librelog-liquibase/src/main/resources/db/changelog/004-libretime-integration-schema.xml` - Liquibase changeset for all integration tables
- `librelog-liquibase/src/main/resources/db/changelog/db.changelog-master.xml` - Master changelog (needs update to include new changeset)

### Enums
- `librelog-api/src/main/java/com/onelpro/librelog/enums/SyncFrequency.java` - Enum for sync frequency options
- `librelog-api/src/main/java/com/onelpro/librelog/enums/SyncDirection.java` - Enum for sync direction (bidirectional, librelog_to_libretime, libretime_to_librelog)
- `librelog-api/src/main/java/com/onelpro/librelog/enums/ConflictResolution.java` - Enum for conflict resolution strategies
- `librelog-api/src/main/java/com/onelpro/librelog/enums/SyncStatus.java` - Enum for file sync status (pending, syncing, synced, failed, conflict)
- `librelog-api/src/main/java/com/onelpro/librelog/enums/SyncType.java` - Enum for sync operation types
- `librelog-api/src/main/java/com/onelpro/librelog/enums/EndpointStatus.java` - Enum for API endpoint status (working, broken, missing, unknown)
- `librelog-api/src/main/java/com/onelpro/librelog/enums/TestType.java` - Enum for API test types
- `librelog-api/src/main/java/com/onelpro/librelog/enums/TestStatus.java` - Enum for test result status (passed, failed, error, skipped)
- `librelog-api/src/test/java/com/onelpro/librelog/enums/*Test.java` - Unit tests for all new enums

### Models/Entities
- `librelog-api/src/main/java/com/onelpro/librelog/models/LibreTimeIntegrationConfig.java` - Entity for integration configuration
- `librelog-api/src/main/java/com/onelpro/librelog/models/LibreTimeApiEndpoint.java` - Entity for discovered API endpoints
- `librelog-api/src/main/java/com/onelpro/librelog/models/LibreTimeApiTestResult.java` - Entity for API test results
- `librelog-api/src/main/java/com/onelpro/librelog/models/LibreTimeFileSyncStatus.java` - Entity for file sync status tracking
- `librelog-api/src/main/java/com/onelpro/librelog/models/LibreTimeSyncHistory.java` - Entity for sync operation history

### Repositories
- `librelog-api/src/main/java/com/onelpro/librelog/repositories/LibreTimeIntegrationConfigRepository.java` - Repository for integration config
- `librelog-api/src/main/java/com/onelpro/librelog/repositories/LibreTimeApiEndpointRepository.java` - Repository for API endpoints
- `librelog-api/src/main/java/com/onelpro/librelog/repositories/LibreTimeApiTestResultRepository.java` - Repository for test results
- `librelog-api/src/main/java/com/onelpro/librelog/repositories/LibreTimeFileSyncStatusRepository.java` - Repository for file sync status
- `librelog-api/src/main/java/com/onelpro/librelog/repositories/LibreTimeSyncHistoryRepository.java` - Repository for sync history

### DTOs
- `librelog-api/src/main/java/com/onelpro/librelog/dto/LibreTimeIntegrationConfigRequestDTO.java` - Request DTO for integration config
- `librelog-api/src/main/java/com/onelpro/librelog/dto/LibreTimeIntegrationConfigResponseDTO.java` - Response DTO for integration config
- `librelog-api/src/main/java/com/onelpro/librelog/dto/ConnectionTestResponseDTO.java` - Response DTO for connection test
- `librelog-api/src/main/java/com/onelpro/librelog/dto/FileUploadRequestDTO.java` - Request DTO for file upload
- `librelog-api/src/main/java/com/onelpro/librelog/dto/FileUploadResponseDTO.java` - Response DTO for file upload
- `librelog-api/src/main/java/com/onelpro/librelog/dto/FileDownloadResponseDTO.java` - Response DTO for file download
- `librelog-api/src/main/java/com/onelpro/librelog/dto/FileListResponseDTO.java` - Response DTO for file listing
- `librelog-api/src/main/java/com/onelpro/librelog/dto/FileQueryRequestDTO.java` - Request DTO for file querying
- `librelog-api/src/main/java/com/onelpro/librelog/dto/ApiEndpointResponseDTO.java` - Response DTO for API endpoint info
- `librelog-api/src/main/java/com/onelpro/librelog/dto/ApiTestResultResponseDTO.java` - Response DTO for test results
- `librelog-api/src/main/java/com/onelpro/librelog/dto/ApiTestSummaryResponseDTO.java` - Response DTO for test summary
- `librelog-api/src/main/java/com/onelpro/librelog/dto/BugReportResponseDTO.java` - Response DTO for bug reports
- `librelog-api/src/main/java/com/onelpro/librelog/dto/SyncStatusResponseDTO.java` - Response DTO for sync status
- `librelog-api/src/main/java/com/onelpro/librelog/dto/SyncHistoryResponseDTO.java` - Response DTO for sync history
- `librelog-api/src/main/java/com/onelpro/librelog/dto/SyncStatisticsResponseDTO.java` - Response DTO for sync statistics

### Services (Interfaces)
- `librelog-api/src/main/java/com/onelpro/librelog/services/LibreTimeIntegrationConfigService.java` - Service interface for integration config management
- `librelog-api/src/main/java/com/onelpro/librelog/services/LibreTimeFileSyncService.java` - Service interface for file synchronization
- `librelog-api/src/main/java/com/onelpro/librelog/services/LibreTimeApiDiscoveryService.java` - Service interface for API endpoint discovery
- `librelog-api/src/main/java/com/onelpro/librelog/services/LibreTimeApiTestingService.java` - Service interface for API testing
- `librelog-api/src/main/java/com/onelpro/librelog/services/LibreTimeSyncStatusService.java` - Service interface for sync status tracking

### Services (Implementations)
- `librelog-api/src/main/java/com/onelpro/librelog/services/impl/LibreTimeIntegrationConfigServiceImpl.java` - Implementation for integration config
- `librelog-api/src/main/java/com/onelpro/librelog/services/impl/LibreTimeFileSyncServiceImpl.java` - Implementation for file sync
- `librelog-api/src/main/java/com/onelpro/librelog/services/impl/LibreTimeApiDiscoveryServiceImpl.java` - Implementation for API discovery
- `librelog-api/src/main/java/com/onelpro/librelog/services/impl/LibreTimeApiTestingServiceImpl.java` - Implementation for API testing
- `librelog-api/src/main/java/com/onelpro/librelog/services/impl/LibreTimeSyncStatusServiceImpl.java` - Implementation for sync status
- `librelog-api/src/main/java/com/onelpro/librelog/services/impl/LibreTimeSyncServiceImpl.java` - Existing service (needs enhancements)

### Integrations/Client
- `librelog-api/src/main/java/com/onelpro/librelog/integrations/LibreTimeClient.java` - Existing client (needs enhancements)
- `librelog-api/src/main/java/com/onelpro/librelog/integrations/LibreTimeHttpClient.java` - Enhanced HTTP client service

### Controllers
- `librelog-api/src/main/java/com/onelpro/librelog/controllers/LibreTimeIntegrationController.java` - Controller for integration management
- `librelog-api/src/main/java/com/onelpro/librelog/controllers/LibreTimeFileSyncController.java` - Controller for file sync operations
- `librelog-api/src/main/java/com/onelpro/librelog/controllers/LibreTimeApiTestingController.java` - Controller for API testing and discovery

### UI Files
- `librelog-api/src/main/resources/static/libretime-integration-settings.html` - Main settings UI page
- `librelog-api/src/main/resources/static/css/libretime-integration.css` - CSS styles for settings UI
- `librelog-api/src/main/resources/static/js/libretime-integration.js` - JavaScript for settings UI functionality

### Config
- `librelog-api/src/main/java/com/onelpro/librelog/config/LibreTimeConfig.java` - Existing config (may need updates)

### Security
- `librelog-api/src/main/java/com/onelpro/librelog/security/LibreLogPermissionEvaluator.java` - Existing permission evaluator (needs updates for new permissions)

### Utils
- `librelog-api/src/main/java/com/onelpro/librelog/utils/EncryptionUtils.java` - Utility for encrypting sensitive data (JWT tokens, webhook secrets)

### Test Files
- `librelog-api/src/test/java/com/onelpro/librelog/services/impl/LibreTimeIntegrationConfigServiceImplTest.java` - Unit tests for config service
- `librelog-api/src/test/java/com/onelpro/librelog/services/impl/LibreTimeFileSyncServiceImplTest.java` - Unit tests for file sync service
- `librelog-api/src/test/java/com/onelpro/librelog/services/impl/LibreTimeApiDiscoveryServiceImplTest.java` - Unit tests for discovery service
- `librelog-api/src/test/java/com/onelpro/librelog/services/impl/LibreTimeApiTestingServiceImplTest.java` - Unit tests for testing service
- `librelog-api/src/test/java/com/onelpro/librelog/services/impl/LibreTimeSyncStatusServiceImplTest.java` - Unit tests for sync status service
- `librelog-api/src/test/java/com/onelpro/librelog/integrations/LibreTimeClientTest.java` - Unit tests for enhanced client
- `librelog-api/src/test/java/com/onelpro/librelog/controllers/LibreTimeIntegrationControllerTest.java` - Unit tests for integration controller
- `librelog-api/src/test/java/com/onelpro/librelog/controllers/LibreTimeFileSyncControllerTest.java` - Unit tests for file sync controller
- `librelog-api/src/test/java/com/onelpro/librelog/controllers/LibreTimeApiTestingControllerTest.java` - Unit tests for API testing controller
- `librelog-api/src/test/java/com/onelpro/librelog/integration/LibreTimeIntegrationIT.java` - Integration tests for LibreTime integration

### Notes
- All service implementations must be in `services/impl/` subfolder
- All DTOs must end with "DTO" suffix
- All enums must be in `enums/` folder with unit tests
- All entities must use UUID primary keys with `@Enumerated(EnumType.STRING)` for enum fields
- All code must follow coding guidelines (Lombok, constructor injection, logging, etc.)
- Minimum 80% code coverage required for all new code

## Tasks

- [ ] 1.0 Database Schema and Enums
  - [ ] 1.1 Create enum `SyncFrequency` in `enums/` folder with values: REAL_TIME, FIVE_MINUTES, FIFTEEN_MINUTES, HOURLY, MANUAL. Include Javadoc and unit test.
  - [ ] 1.2 Create enum `SyncDirection` in `enums/` folder with values: BIDIRECTIONAL, LIBRELOG_TO_LIBRETIME, LIBRETIME_TO_LIBRELOG. Include Javadoc and unit test.
  - [ ] 1.3 Create enum `ConflictResolution` in `enums/` folder with values: LAST_WRITE_WINS, MANUAL, LIBRELOG_PRIORITY, LIBRETIME_PRIORITY. Include Javadoc and unit test.
  - [ ] 1.4 Create enum `SyncStatus` in `enums/` folder with values: PENDING, SYNCING, SYNCED, FAILED, CONFLICT. Include Javadoc and unit test.
  - [ ] 1.5 Create enum `SyncType` in `enums/` folder with values: FILE_UPLOAD, FILE_DOWNLOAD, BATCH_SYNC, LOG_EXPORT, MANUAL. Include Javadoc and unit test.
  - [ ] 1.6 Create enum `EndpointStatus` in `enums/` folder with values: WORKING, BROKEN, MISSING, UNKNOWN. Include Javadoc and unit test.
  - [ ] 1.7 Create enum `TestType` in `enums/` folder with values: CONNECTIVITY, AUTHENTICATION, CRUD, ERROR_HANDLING, EDGE_CASE. Include Javadoc and unit test.
  - [ ] 1.8 Create enum `TestStatus` in `enums/` folder with values: PASSED, FAILED, ERROR, SKIPPED. Include Javadoc and unit test.
  - [ ] 1.9 Create Liquibase changeset file `004-libretime-integration-schema.xml` with all 5 tables: `libretime_integration_config`, `libretime_api_endpoints`, `libretime_api_test_results`, `libretime_file_sync_status`, `libretime_sync_history`. Use UUID primary keys, proper foreign keys, indexes, and constraints.
  - [ ] 1.10 Update `db.changelog-master.xml` to include the new changeset `004-libretime-integration-schema.xml`.

- [ ] 2.0 Enhanced LibreTime Client and HTTP Service Layer
  - [ ] 2.1 Create `LibreTimeHttpClient.java` in `integrations/` package as a service layer for HTTP operations. Use Spring WebClient, implement constructor injection, add logging with Log4j.
  - [ ] 2.2 Implement JWT token authentication in `LibreTimeHttpClient` - add method to set/get JWT token, include in Authorization header for all requests.
  - [ ] 2.3 Implement request timeout handling (default 30 seconds, configurable) in `LibreTimeHttpClient`.
  - [ ] 2.4 Implement retry logic for transient failures (5xx errors, network errors) in `LibreTimeHttpClient` with configurable max retries (default 3).
  - [ ] 2.5 Add request/response logging in `LibreTimeHttpClient` (log URL, method, status, timing) - mask sensitive data in logs.
  - [ ] 2.6 Enhance existing `LibreTimeClient.java` to use `LibreTimeHttpClient` for HTTP operations, maintain backward compatibility.
  - [ ] 2.7 Add connection test method to `LibreTimeHttpClient` that tests API connectivity and authentication.
  - [ ] 2.8 Implement error handling in `LibreTimeHttpClient` for 4xx (client errors), 5xx (server errors), and network errors with appropriate exception types.

- [ ] 3.0 Integration Configuration Management (Models, Repositories, Services)
  - [ ] 3.1 Create entity `LibreTimeIntegrationConfig.java` in `models/` package with all required fields from PRD FR-4.1. Use Lombok annotations (@Data, @Builder, @NoArgsConstructor, @AllArgsConstructor), UUID primary key, proper JPA annotations.
  - [ ] 3.2 Create repository interface `LibreTimeIntegrationConfigRepository.java` extending JpaRepository. Add custom query methods if needed (e.g., find by sync enabled status).
  - [ ] 3.3 Create service interface `LibreTimeIntegrationConfigService.java` in `services/` package with methods: getConfig(), saveConfig(), updateConfig(), testConnection().
  - [ ] 3.4 Create service implementation `LibreTimeIntegrationConfigServiceImpl.java` in `services/impl/` package. Use constructor injection, implement all interface methods, add logging.
  - [ ] 3.5 Implement encryption for JWT token and webhook secret in config service using Spring encryption utilities or create `EncryptionUtils.java`.
  - [ ] 3.6 Create DTOs: `LibreTimeIntegrationConfigRequestDTO.java` and `LibreTimeIntegrationConfigResponseDTO.java` in `dto/` package. Use Lombok, mask sensitive fields in response DTO.
  - [ ] 3.7 Create `ConnectionTestResponseDTO.java` in `dto/` package for connection test results.
  - [ ] 3.8 Add validation to config service - validate URL format, validate enum values, validate file size limits.

- [ ] 4.0 File Synchronization Services (Upload, Download, List, Query)
  - [ ] 4.1 Create entity `LibreTimeFileSyncStatus.java` in `models/` package with all fields from PRD FR-4.4. Use proper JPA annotations, UUID primary key, enum types.
  - [ ] 4.2 Create repository `LibreTimeFileSyncStatusRepository.java` with query methods for finding by status, by cart ID, by file ID, etc.
  - [ ] 4.3 Create service interface `LibreTimeFileSyncService.java` in `services/` package with methods: uploadFile(), downloadFile(), listFiles(), queryFiles(), getSyncStatus().
  - [ ] 4.4 Create service implementation `LibreTimeFileSyncServiceImpl.java` in `services/impl/` package. Use constructor injection, implement file upload using multipart/form-data.
  - [ ] 4.5 Implement file upload in service - validate file format and size, include metadata (name, cart ID, cue points, fade settings, asset type), track upload progress, handle errors.
  - [ ] 4.6 Implement file download in service - retrieve file and metadata from LibreTime API, save to configured storage path, preserve metadata in database, track download progress.
  - [ ] 4.7 Implement file listing in service - support pagination, caching, filtering, sorting. Handle API pagination automatically.
  - [ ] 4.8 Implement file querying in service - query by metadata (asset type, content type, etc.), support filtering and sorting.
  - [ ] 4.9 Create DTOs: `FileUploadRequestDTO.java`, `FileUploadResponseDTO.java`, `FileDownloadResponseDTO.java`, `FileListResponseDTO.java`, `FileQueryRequestDTO.java` in `dto/` package.
  - [ ] 4.10 Implement sync status tracking - update `LibreTimeFileSyncStatus` entity after each sync operation, track status changes, detect conflicts using file hash comparison.

- [ ] 5.0 API Endpoint Discovery and Testing Services
  - [ ] 5.1 Create entity `LibreTimeApiEndpoint.java` in `models/` package with all fields from PRD FR-4.2. Use proper JPA annotations, UUID primary key.
  - [ ] 5.2 Create entity `LibreTimeApiTestResult.java` in `models/` package with all fields from PRD FR-4.3. Use proper JPA annotations, UUID primary key, foreign key to endpoint.
  - [ ] 5.3 Create repositories: `LibreTimeApiEndpointRepository.java` and `LibreTimeApiTestResultRepository.java` with appropriate query methods.
  - [ ] 5.4 Create service interface `LibreTimeApiDiscoveryService.java` in `services/` package with methods: discoverEndpoints(), getDiscoveredEndpoints(), updateEndpointStatus().
  - [ ] 5.5 Create service implementation `LibreTimeApiDiscoveryServiceImpl.java` in `services/impl/` package. Implement endpoint discovery by testing common REST patterns (GET /api/v2/{resource}, POST /api/v2/{resource}, etc.).
  - [ ] 5.6 Implement discovery logic - test standard CRUD endpoints for resource types (files, playlists, smart-blocks, shows, etc.), document HTTP methods, identify URL patterns.
  - [ ] 5.7 Create service interface `LibreTimeApiTestingService.java` in `services/` package with methods: testConnection(), testAuthentication(), testEndpoint(), runAllTests(), generateTestReport(), generateBugReport().
  - [ ] 5.8 Create service implementation `LibreTimeApiTestingServiceImpl.java` in `services/impl/` package. Implement testing for connectivity, authentication, CRUD operations, error scenarios, edge cases.
  - [ ] 5.9 Implement test result storage - save test results to database, categorize endpoints (working, broken, missing, unknown), record detailed test data.
  - [ ] 5.10 Implement test report generation - generate reports in JSON, Markdown, and HTML formats. Include summary statistics, detailed results, response times.
  - [ ] 5.11 Implement bug report generation - create detailed bug reports for broken endpoints with expected/actual behavior, request/response examples, steps to reproduce, severity levels.
  - [ ] 5.12 Create DTOs: `ApiEndpointResponseDTO.java`, `ApiTestResultResponseDTO.java`, `ApiTestSummaryResponseDTO.java`, `BugReportResponseDTO.java` in `dto/` package.

- [ ] 6.0 Clock Template Export Service Enhancements
  - [ ] 6.1 Review existing `LibreTimeSyncServiceImpl.java` and identify enhancement needs based on PRD requirements.
  - [ ] 6.2 Enhance `exportClock()` method to include better validation - check for missing files, invalid timing, metadata completeness.
  - [ ] 6.3 Enhance `pushClockToLibreTime()` method to handle validation errors, return detailed results (success, failures, warnings).
  - [ ] 6.4 Enhance `generateLogFromClock()` method to support date ranges, recurring schedules, conflict detection.
  - [ ] 6.5 Add validation method to check clock template before export - verify all referenced files exist in LibreTime, check timing conflicts, validate metadata.
  - [ ] 6.6 Add conflict detection for scheduled shows - detect overlapping show instances, check for scheduling conflicts with existing shows.
  - [ ] 6.7 Update `LibreTimeClient.java` to support new endpoints if needed (show instance update, delete, query by date range).

- [ ] 7.0 Sync Status and History Tracking
  - [ ] 7.1 Create entity `LibreTimeSyncHistory.java` in `models/` package with all fields from PRD FR-4.5. Use proper JPA annotations, UUID primary key.
  - [ ] 7.2 Create repository `LibreTimeSyncHistoryRepository.java` with query methods for finding by sync type, by status, by date range, by user.
  - [ ] 7.3 Create service interface `LibreTimeSyncStatusService.java` in `services/` package with methods: getSyncStatus(), getSyncHistory(), getSyncStatistics(), detectConflicts().
  - [ ] 7.4 Create service implementation `LibreTimeSyncStatusServiceImpl.java` in `services/impl/` package. Implement sync status tracking, history maintenance, conflict detection.
  - [ ] 7.5 Implement sync status tracking - track status for each file (pending, syncing, synced, failed, conflict), maintain timestamps, update status in real-time.
  - [ ] 7.6 Implement sync history - record all sync operations with type, status, item counts, timestamps, error summaries, initiated by user.
  - [ ] 7.7 Implement conflict detection - compare file hashes, detect when same file modified in both systems, flag conflicts for resolution.
  - [ ] 7.8 Implement sync statistics - calculate total files, synced count, failed count, pending count, success rate.
  - [ ] 7.9 Create DTOs: `SyncStatusResponseDTO.java`, `SyncHistoryResponseDTO.java`, `SyncStatisticsResponseDTO.java` in `dto/` package.
  - [ ] 7.10 Integrate sync status tracking into file sync service - update status after each operation, record history entries.

- [ ] 8.0 REST API Controllers
  - [ ] 8.1 Create controller `LibreTimeIntegrationController.java` in `controllers/` package. Add @RestController, @RequestMapping("/api/libretime/integration"), Swagger annotations.
  - [ ] 8.2 Implement GET endpoint to retrieve integration configuration. Add @PreAuthorize with LIBRETIME_INTEGRATION_VIEW permission, return masked sensitive data.
  - [ ] 8.3 Implement PUT endpoint to update integration configuration. Add @PreAuthorize with LIBRETIME_INTEGRATION_CONFIGURE permission, validate input, encrypt sensitive fields.
  - [ ] 8.4 Implement POST endpoint to test connection. Add @PreAuthorize with LIBRETIME_INTEGRATION_VIEW permission, return connection test results.
  - [ ] 8.5 Create controller `LibreTimeFileSyncController.java` in `controllers/` package. Add @RestController, @RequestMapping("/api/libretime/files"), Swagger annotations.
  - [ ] 8.6 Implement POST endpoint for file upload. Add @PreAuthorize with LIBRETIME_INTEGRATION_SYNC permission, accept multipart/form-data, return upload results.
  - [ ] 8.7 Implement GET endpoint for file download. Add @PreAuthorize with LIBRETIME_INTEGRATION_VIEW permission, return file and metadata.
  - [ ] 8.8 Implement GET endpoint for file listing. Add @PreAuthorize with LIBRETIME_INTEGRATION_VIEW permission, support pagination, filtering, sorting.
  - [ ] 8.9 Implement POST endpoint for file querying. Add @PreAuthorize with LIBRETIME_INTEGRATION_VIEW permission, accept query criteria, return matching files.
  - [ ] 8.10 Implement POST endpoint for manual sync trigger. Add @PreAuthorize with LIBRETIME_INTEGRATION_SYNC permission, trigger sync operation, return sync status.
  - [ ] 8.11 Create controller `LibreTimeApiTestingController.java` in `controllers/` package. Add @RestController, @RequestMapping("/api/libretime/testing"), Swagger annotations.
  - [ ] 8.12 Implement POST endpoint to discover endpoints. Add @PreAuthorize with LIBRETIME_INTEGRATION_TEST permission, trigger discovery, return discovered endpoints.
  - [ ] 8.13 Implement POST endpoint to run API tests. Add @PreAuthorize with LIBRETIME_INTEGRATION_TEST permission, run tests, return test results.
  - [ ] 8.14 Implement GET endpoint to get test results. Add @PreAuthorize with LIBRETIME_INTEGRATION_VIEW permission, return test results with filtering options.
  - [ ] 8.15 Implement GET endpoint to get test summary. Add @PreAuthorize with LIBRETIME_INTEGRATION_VIEW permission, return summary statistics.
  - [ ] 8.16 Implement GET endpoint to get bug reports. Add @PreAuthorize with LIBRETIME_INTEGRATION_EXPORT_LOGS permission, return bug reports for broken endpoints.
  - [ ] 8.17 Implement GET endpoint to export test documentation. Add @PreAuthorize with LIBRETIME_INTEGRATION_EXPORT_LOGS permission, return documentation in requested format (JSON, Markdown, HTML).
  - [ ] 8.18 Add GET endpoints for sync status and history in appropriate controllers. Add @PreAuthorize with LIBRETIME_INTEGRATION_VIEW permission.
  - [ ] 8.19 Add proper error handling to all controllers - use @ExceptionHandler or GlobalExceptionHandler, return consistent error response format.
  - [ ] 8.20 Add comprehensive Swagger/OpenAPI annotations to all endpoints - @Operation, @ApiResponse, parameter descriptions.

- [ ] 9.0 Settings UI Implementation
  - [ ] 9.1 Create HTML file `libretime-integration-settings.html` in `src/main/resources/static/` directory. Use semantic HTML5 elements, proper structure.
  - [ ] 9.2 Implement Connection Settings section - API Base URL input, JWT Token input (masked display), Test Connection button, connection status indicator. Add proper labels, ARIA attributes, keyboard navigation.
  - [ ] 9.3 Implement Synchronization Settings section - auto-sync toggle, sync frequency dropdown, sync direction radio buttons, conflict resolution dropdown, manual sync button, last sync timestamp display. Ensure accessibility.
  - [ ] 9.4 Implement File Settings section - supported formats checklist, max file size input, storage path input, metadata sync toggle. Add validation, help text.
  - [ ] 9.5 Implement Log Generation Settings section - auto-export toggle, validation level dropdown, conflict resolution dropdown, preview toggle, scheduling options. Ensure proper form structure.
  - [ ] 9.6 Implement Webhook Settings section - webhook URL input, webhook secret input (masked), enable/disable toggle, test webhook button, delivery status display. Add security considerations.
  - [ ] 9.7 Implement API Testing & Documentation section - Discover Endpoints button, Run Tests button, Test Results table/view, Export Documentation button, Bug Report display. Make interactive and accessible.
  - [ ] 9.8 Implement Sync Status & History section - current sync status dashboard, sync history table, file sync status list, error log viewer. Add pagination, filtering, sorting.
  - [ ] 9.9 Create CSS file `libretime-integration.css` in `src/main/resources/static/css/` directory. Implement responsive design (mobile, tablet, desktop), proper color contrast (WCAG 2.1 Level AA), focus indicators.
  - [ ] 9.10 Create JavaScript file `libretime-integration.js` in `src/main/resources/static/js/` directory. Implement API calls to backend endpoints, form validation, dynamic UI updates, error handling.
  - [ ] 9.11 Implement form validation in JavaScript - validate URL format, validate file size limits, validate required fields, show clear error messages.
  - [ ] 9.12 Implement API integration in JavaScript - fetch integration config, update config, test connection, trigger sync, discover endpoints, run tests, export documentation.
  - [ ] 9.13 Implement real-time status updates - poll for sync status updates, update UI dynamically, show loading states, handle errors gracefully.
  - [ ] 9.14 Ensure keyboard navigation - all interactive elements keyboard accessible, visible focus indicators, logical tab order, skip links.
  - [ ] 9.15 Ensure screen reader compatibility - proper ARIA labels, ARIA describedby for help text, ARIA invalid for errors, semantic HTML structure.
  - [ ] 9.16 Test responsive design - verify UI works on mobile (320px+), tablet (768px+), desktop (1024px+). Test on multiple browsers.

- [ ] 10.0 Role-Based Access Control and Permissions
  - [ ] 10.1 Review existing permission system in `PermissionLevel.java`, `ModuleType.java`, `ActionType.java` to understand current structure.
  - [ ] 10.2 Add new permission constants to appropriate enum/class: LIBRETIME_INTEGRATION_VIEW, LIBRETIME_INTEGRATION_CONFIGURE, LIBRETIME_INTEGRATION_TEST, LIBRETIME_INTEGRATION_SYNC, LIBRETIME_INTEGRATION_EXPORT_LOGS.
  - [ ] 10.3 Update `LibreLogPermissionEvaluator.java` to handle new LibreTime integration permissions if needed.
  - [ ] 10.4 Assign default permissions to user roles - Administrators get all permissions, Operators get VIEW and SYNC, Viewers get VIEW only. Update role initialization or configuration.
  - [ ] 10.5 Update all controller endpoints to use @PreAuthorize with appropriate permissions (already added in task 8.0, verify completeness).
  - [ ] 10.6 Update UI JavaScript to check permissions before displaying/enabling features - hide/disable features based on user permissions.
  - [ ] 10.7 Add permission checks in service layer if needed - verify permissions before performing sensitive operations.

- [ ] 11.0 Error Handling and Logging Enhancements
  - [ ] 11.1 Review existing `GlobalExceptionHandler.java` and ensure it handles all new exception types from LibreTime integration.
  - [ ] 11.2 Create custom exceptions if needed: `LibreTimeConnectionException.java`, `LibreTimeApiException.java`, `FileSyncException.java` in `exceptions/` package.
  - [ ] 11.3 Implement error handling in `LibreTimeHttpClient` - handle network errors, authentication errors (401, 403), validation errors (400), server errors (500, 502, 503) with appropriate exception types.
  - [ ] 11.4 Implement error handling in file sync service - handle upload failures, download failures, file not found, permission denied, with detailed error messages.
  - [ ] 11.5 Implement error handling in API testing service - handle test failures gracefully, capture error details, provide actionable error messages.
  - [ ] 11.6 Add comprehensive logging to all services - log API requests/responses, sync operations, errors with appropriate log levels (INFO, WARN, ERROR, DEBUG).
  - [ ] 11.7 Ensure sensitive information masking in logs - mask JWT tokens, passwords, webhook secrets in all log statements.
  - [ ] 11.8 Implement user-friendly error messages in controllers - convert technical errors to actionable messages for end users, log technical details separately.
  - [ ] 11.9 Add error response DTO if not exists - ensure all error responses follow consistent format from PRD design considerations.

- [ ] 12.0 Testing (Unit, Integration, UI)
  - [ ] 12.1 Create unit tests for all new enum classes following existing pattern (e.g., `UserStatusTest.java`). Test values(), valueOf(), ensure 80%+ coverage.
  - [ ] 12.2 Create unit tests for `LibreTimeIntegrationConfigServiceImpl` - test getConfig(), saveConfig(), updateConfig(), testConnection(), encryption/decryption. Use Mockito for dependencies, aim for 80%+ coverage.
  - [ ] 12.3 Create unit tests for `LibreTimeFileSyncServiceImpl` - test uploadFile(), downloadFile(), listFiles(), queryFiles(). Mock LibreTimeHttpClient, test error scenarios, aim for 80%+ coverage.
  - [ ] 12.4 Create unit tests for `LibreTimeApiDiscoveryServiceImpl` - test discoverEndpoints(), getDiscoveredEndpoints(). Mock HTTP client, test various endpoint patterns, aim for 80%+ coverage.
  - [ ] 12.5 Create unit tests for `LibreTimeApiTestingServiceImpl` - test testConnection(), testEndpoint(), runAllTests(), generateTestReport(), generateBugReport(). Mock HTTP client, test all test types, aim for 80%+ coverage.
  - [ ] 12.6 Create unit tests for `LibreTimeSyncStatusServiceImpl` - test getSyncStatus(), getSyncHistory(), getSyncStatistics(), detectConflicts(). Mock repositories, test various scenarios, aim for 80%+ coverage.
  - [ ] 12.7 Create unit tests for `LibreTimeHttpClient` - test JWT authentication, timeout handling, retry logic, error handling. Mock WebClient, test all scenarios, aim for 80%+ coverage.
  - [ ] 12.8 Create unit tests for all controllers - test all endpoints, permission checks, error handling. Use @WebMvcTest, mock services, test request/response DTOs, aim for 80%+ coverage.
  - [ ] 12.9 Create integration test `LibreTimeIntegrationIT.java` in `integration/` package. Use @SpringBootTest, test end-to-end workflows: configure → test connection → upload file → verify sync status. Use Testcontainers if needed for external dependencies.
  - [ ] 12.10 Create integration test for API discovery and testing - test actual discovery against test LibreTime instance (if available), or use mocks. Test full discovery and testing workflow.
  - [ ] 12.11 Test UI accessibility - use automated tools (axe DevTools, WAVE) to test WCAG 2.1 Level AA compliance. Test keyboard navigation, screen reader compatibility manually.
  - [ ] 12.12 Test UI responsive design - test on multiple screen sizes (mobile 320px, tablet 768px, desktop 1024px+), test on multiple browsers (Chrome, Firefox, Safari, Edge).
  - [ ] 12.13 Test form validation in UI - test all input validation, error message display, required field handling.
  - [ ] 12.14 Test permission-based UI visibility - test that features are hidden/disabled based on user permissions.
  - [ ] 12.15 Run full test suite with `mvn clean install` - verify all tests pass, verify JaCoCo coverage meets 80% minimum requirement, fix any failing tests or coverage gaps.

