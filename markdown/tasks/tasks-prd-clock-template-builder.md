# Task List: Professional Clock Template Builder

Based on PRD: `prd-clock-template-builder.md`

## Relevant Files

### Database Migrations
- `librelog-api/src/main/resources/db/changelog/006-enhance-break-structures.xml` - Enhance break_structures table with avail_type, timing_type, transition_code fields
- `librelog-api/src/main/resources/db/changelog/007-create-fixed-assets-table.xml` - Create fixed_assets table for static carts
- `librelog-api/src/main/resources/db/changelog/008-create-automation-commands-table.xml` - Create automation_commands table
- `librelog-api/src/main/resources/db/changelog/009-create-avail-types-table.xml` - Create avail_types table/enum
- `librelog-api/src/main/resources/db/changelog/010-create-daypart-assignments-table.xml` - Create daypart_assignments table for template hierarchy
- `librelog-api/src/main/resources/db/changelog/011-create-grids-table.xml` - Create grids table for weekly schedules
- `librelog-api/src/main/resources/db/changelog/db.changelog-master.xml` - Master changelog (update to include new changesets)

### Backend - Enums
- `librelog-api/src/main/java/com/onelpro/librelog/enums/TimingType.java` - Enum for Hard/Soft start timing
- `librelog-api/src/main/java/com/onelpro/librelog/enums/TransitionCode.java` - Enum for Segue, Overlap, Hard Start
- `librelog-api/src/main/java/com/onelpro/librelog/enums/AvailType.java` - Enum for avail type categories (or entity if configurable)
- `librelog-api/src/main/java/com/onelpro/librelog/enums/AutomationCommandType.java` - Enum for automation command types
- `librelog-api/src/main/java/com/onelpro/librelog/enums/AssetType.java` - Enum for WideOrbit asset types (IM, ID, CM, PR, VT, SH)
- `librelog-api/src/main/java/com/onelpro/librelog/enums/MusicCategory.java` - Enum for WideOrbit music categories (S1, S2, S3)

### Backend - Models/Entities
- `librelog-api/src/main/java/com/onelpro/librelog/models/ClockTemplate.java` - Clock template entity (exists, needs enhancement)
- `librelog-api/src/main/java/com/onelpro/librelog/models/BreakStructure.java` - Break structure entity (exists, needs enhancement)
- `librelog-api/src/main/java/com/onelpro/librelog/models/FixedAsset.java` - Fixed asset entity for static carts
- `librelog-api/src/main/java/com/onelpro/librelog/models/AutomationCommand.java` - Automation command entity
- `librelog-api/src/main/java/com/onelpro/librelog/models/AvailType.java` - Avail type entity (if configurable, otherwise use enum)
- `librelog-api/src/main/java/com/onelpro/librelog/models/DaypartAssignment.java` - Daypart assignment entity for template hierarchy
- `librelog-api/src/main/java/com/onelpro/librelog/models/Grid.java` - Grid entity for weekly schedules

### Backend - Repositories
- `librelog-api/src/main/java/com/onelpro/librelog/repositories/BreakStructureRepository.java` - Break structure repository (exists, may need additional methods)
- `librelog-api/src/main/java/com/onelpro/librelog/repositories/FixedAssetRepository.java` - Fixed asset repository
- `librelog-api/src/main/java/com/onelpro/librelog/repositories/AutomationCommandRepository.java` - Automation command repository
- `librelog-api/src/main/java/com/onelpro/librelog/repositories/AvailTypeRepository.java` - Avail type repository (if entity-based)
- `librelog-api/src/main/java/com/onelpro/librelog/repositories/DaypartAssignmentRepository.java` - Daypart assignment repository
- `librelog-api/src/main/java/com/onelpro/librelog/repositories/GridRepository.java` - Grid repository

### Backend - DTOs
- `librelog-api/src/main/java/com/onelpro/librelog/dto/BreakStructureRequestDTO.java` - Break structure creation/update request
- `librelog-api/src/main/java/com/onelpro/librelog/dto/BreakStructureResponseDTO.java` - Break structure response
- `librelog-api/src/main/java/com/onelpro/librelog/dto/FixedAssetRequestDTO.java` - Fixed asset request
- `librelog-api/src/main/java/com/onelpro/librelog/dto/FixedAssetResponseDTO.java` - Fixed asset response
- `librelog-api/src/main/java/com/onelpro/librelog/dto/AutomationCommandRequestDTO.java` - Automation command request
- `librelog-api/src/main/java/com/onelpro/librelog/dto/AutomationCommandResponseDTO.java` - Automation command response
- `librelog-api/src/main/java/com/onelpro/librelog/dto/ClockTemplateWithBreaksDTO.java` - Combined clock template with all elements
- `librelog-api/src/main/java/com/onelpro/librelog/dto/ClockValidationResultDTO.java` - Validation result response
- `librelog-api/src/main/java/com/onelpro/librelog/dto/RevenueAnalysisDTO.java` - Revenue impact analysis response
- `librelog-api/src/main/java/com/onelpro/librelog/dto/DaypartAssignmentRequestDTO.java` - Daypart assignment request
- `librelog-api/src/main/java/com/onelpro/librelog/dto/GridRequestDTO.java` - Grid request
- `librelog-api/src/main/java/com/onelpro/librelog/dto/LibreTimeExportDTO.java` - LibreTime export format

### Backend - Services
- `librelog-api/src/main/java/com/onelpro/librelog/services/BreakStructureService.java` - Break structure service interface
- `librelog-api/src/main/java/com/onelpro/librelog/services/impl/BreakStructureServiceImpl.java` - Break structure service implementation
- `librelog-api/src/main/java/com/onelpro/librelog/services/FixedAssetService.java` - Fixed asset service interface
- `librelog-api/src/main/java/com/onelpro/librelog/services/impl/FixedAssetServiceImpl.java` - Fixed asset service implementation
- `librelog-api/src/main/java/com/onelpro/librelog/services/AutomationCommandService.java` - Automation command service interface
- `librelog-api/src/main/java/com/onelpro/librelog/services/impl/AutomationCommandServiceImpl.java` - Automation command service implementation
- `librelog-api/src/main/java/com/onelpro/librelog/services/ClockBuilderService.java` - Clock builder service interface
- `librelog-api/src/main/java/com/onelpro/librelog/services/impl/ClockBuilderServiceImpl.java` - Clock builder service implementation
- `librelog-api/src/main/java/com/onelpro/librelog/services/ClockValidationService.java` - Clock validation service interface
- `librelog-api/src/main/java/com/onelpro/librelog/services/impl/ClockValidationServiceImpl.java` - Clock validation service implementation
- `librelog-api/src/main/java/com/onelpro/librelog/services/RevenueAnalysisService.java` - Revenue analysis service interface
- `librelog-api/src/main/java/com/onelpro/librelog/services/impl/RevenueAnalysisServiceImpl.java` - Revenue analysis service implementation
- `librelog-api/src/main/java/com/onelpro/librelog/services/LibreTimeSyncService.java` - LibreTime sync service interface
- `librelog-api/src/main/java/com/onelpro/librelog/services/impl/LibreTimeSyncServiceImpl.java` - LibreTime sync service implementation
- `librelog-api/src/main/java/com/onelpro/librelog/services/ClockService.java` - Clock service interface (exists, needs enhancement)
- `librelog-api/src/main/java/com/onelpro/librelog/services/impl/ClockServiceImpl.java` - Clock service implementation (exists, needs enhancement)

### Backend - Controllers
- `librelog-api/src/main/java/com/onelpro/librelog/controllers/BreakStructureController.java` - Break structure REST controller
- `librelog-api/src/main/java/com/onelpro/librelog/controllers/FixedAssetController.java` - Fixed asset REST controller
- `librelog-api/src/main/java/com/onelpro/librelog/controllers/AutomationCommandController.java` - Automation command REST controller
- `librelog-api/src/main/java/com/onelpro/librelog/controllers/ClockController.java` - Clock controller (exists, needs enhancement)
- `librelog-api/src/main/java/com/onelpro/librelog/controllers/ClockBuilderController.java` - Clock builder controller for structure management
- `librelog-api/src/main/java/com/onelpro/librelog/controllers/GridController.java` - Grid controller for weekly schedules

### Backend - Integration
- `librelog-api/src/main/java/com/onelpro/librelog/integrations/LibreTimeClient.java` - LibreTime API client
- `librelog-api/src/main/java/com/onelpro/librelog/integrations/LibreTimeConfig.java` - LibreTime configuration

### Frontend - UI Components
- `librelog-api/src/main/resources/static/dashboard.html` - Main dashboard (exists, needs clock builder UI)
- `librelog-api/src/main/resources/static/css/clock-builder.css` - Clock builder styles
- `librelog-api/src/main/resources/static/js/clock-builder.js` - Clock builder JavaScript

### Tests
- `librelog-api/src/test/java/com/onelpro/librelog/enums/TimingTypeTest.java` - Timing type enum tests
- `librelog-api/src/test/java/com/onelpro/librelog/enums/TransitionCodeTest.java` - Transition code enum tests
- `librelog-api/src/test/java/com/onelpro/librelog/models/BreakStructureTest.java` - Break structure entity tests
- `librelog-api/src/test/java/com/onelpro/librelog/models/FixedAssetTest.java` - Fixed asset entity tests
- `librelog-api/src/test/java/com/onelpro/librelog/models/AutomationCommandTest.java` - Automation command entity tests
- `librelog-api/src/test/java/com/onelpro/librelog/services/impl/BreakStructureServiceImplTest.java` - Break structure service tests
- `librelog-api/src/test/java/com/onelpro/librelog/services/impl/FixedAssetServiceImplTest.java` - Fixed asset service tests
- `librelog-api/src/test/java/com/onelpro/librelog/services/impl/AutomationCommandServiceImplTest.java` - Automation command service tests
- `librelog-api/src/test/java/com/onelpro/librelog/services/impl/ClockBuilderServiceImplTest.java` - Clock builder service tests
- `librelog-api/src/test/java/com/onelpro/librelog/services/impl/ClockValidationServiceImplTest.java` - Clock validation service tests
- `librelog-api/src/test/java/com/onelpro/librelog/services/impl/RevenueAnalysisServiceImplTest.java` - Revenue analysis service tests
- `librelog-api/src/test/java/com/onelpro/librelog/controllers/BreakStructureControllerTest.java` - Break structure controller tests
- `librelog-api/src/test/java/com/onelpro/librelog/controllers/FixedAssetControllerTest.java` - Fixed asset controller tests
- `librelog-api/src/test/java/com/onelpro/librelog/controllers/AutomationCommandControllerTest.java` - Automation command controller tests
- `librelog-api/src/test/java/com/onelpro/librelog/integrations/LibreTimeClientTest.java` - LibreTime client tests

### Notes

- Unit tests should be placed alongside the code files they are testing
- Use `mvn test` to run all tests
- Use `mvn jacoco:report` to generate coverage reports
- All new code must meet 80% minimum coverage requirement
- Follow Spring Boot best practices: constructor injection, DTOs for API, UUID primary keys
- All web pages must be mobile responsive and meet WCAG 2.1 Level AA accessibility standards
- Clock templates represent 60-minute broadcast structures with commercial breaks, fixed assets, and automation commands
- Integration with LibreTime requires API authentication and proper data format conversion

## Tasks

- [x] 1.0 Enhance Database Schema for Clock Template Elements
  - [x] 1.1 Create Liquibase changeset `006-enhance-break-structures.xml` to add `avail_type`, `timing_type`, and `transition_code` columns to `break_structures` table
  - [x] 1.2 Create Liquibase changeset `007-create-fixed-assets-table.xml` to create `fixed_assets` table with columns: id (UUID), clock_template_id (UUID FK), name (VARCHAR), asset_type (VARCHAR), start_time (TIME), asset_identifier (VARCHAR), timing_type (VARCHAR), created_at, updated_at
  - [x] 1.3 Create Liquibase changeset `008-create-automation-commands-table.xml` to create `automation_commands` table with columns: id (UUID), clock_template_id (UUID FK), command_type (VARCHAR), trigger_time (TIME), priority (VARCHAR), parameters (JSONB), created_at, updated_at
  - [x] 1.4 Create Liquibase changeset `009-create-avail-types-table.xml` to create `avail_types` table with columns: id (UUID), name (VARCHAR UNIQUE), description (TEXT), is_active (BOOLEAN), created_at, updated_at
  - [x] 1.5 Create Liquibase changeset `010-create-daypart-assignments-table.xml` to create `daypart_assignments` table with columns: id (UUID), daypart_id (UUID FK), clock_template_id (UUID FK), created_at, updated_at
  - [x] 1.6 Create Liquibase changeset `011-create-grids-table.xml` to create `grids` table with columns: id (UUID), name (VARCHAR), channel_id (UUID FK), description (TEXT), is_active (BOOLEAN), created_at, updated_at
  - [x] 1.7 Create Liquibase changeset `012-create-grid-daypart-mappings-table.xml` to create `grid_daypart_mappings` table for weekly schedule assignments
  - [x] 1.8 Add appropriate indexes on foreign keys and frequently queried columns
  - [x] 1.9 Update `db.changelog-master.xml` to include all new changesets in order
  - [x] 1.10 Verify all changesets are idempotent and can be run multiple times safely

- [x] 2.0 Create Backend Entities and Enums for New Element Types
  - [x] 2.1 Create `TimingType` enum in `enums` package with values: HARD_START, SOFT_START
  - [x] 2.2 Create `TransitionCode` enum in `enums` package with values: SEGUE, OVERLAP, HARD_START
  - [x] 2.3 Create `AutomationCommandType` enum in `enums` package with values: SWITCH_TO_SATELLITE, START_RECORDING, ENABLE_LIVE_MIX, etc.
  - [x] 2.4 Create `AssetType` enum in `enums` package with WideOrbit values: IM (Imaging), ID (Legal ID), CM (Commercials), PR (Promos), VT (Voice Tracks), SH (Show/Longform)
  - [x] 2.5 Create `MusicCategory` enum in `enums` package with WideOrbit values: S1 (Power/Current), S2 (Secondary), S3 (New/Discovery)
  - [x] 2.6 Create `AvailType` entity in `models` package with JPA annotations, UUID primary key, Lombok annotations (@Data, @Builder, @NoArgsConstructor, @AllArgsConstructor)
  - [x] 2.7 Create `FixedAsset` entity in `models` package with relationship to ClockTemplate, fields for name, asset_type (enum), start_time, asset_identifier, timing_type (enum), and timestamps
  - [x] 2.8 Create `AutomationCommand` entity in `models` package with relationship to ClockTemplate, fields for command_type (enum), trigger_time, priority, parameters (JSONB), and timestamps
  - [x] 2.9 Create `DaypartAssignment` entity in `models` package with relationships to Daypart and ClockTemplate
  - [x] 2.10 Create `Grid` entity in `models` package with relationship to Channel, fields for name, description, is_active, and timestamps
  - [x] 2.11 Create `GridDaypartMapping` entity for weekly schedule assignments
  - [x] 2.12 Update `BreakStructure` entity to include new fields: avail_type (relationship or enum), timing_type (enum), transition_code (enum)
  - [x] 2.13 Write unit tests for all new enum classes (80%+ coverage)
  - [x] 2.14 Verify all entities use `@Enumerated(EnumType.STRING)` for enum fields

- [x] 3.0 Implement Break Structure Service and Controller
  - [x] 3.1 Create `BreakStructureRequestDTO` with fields: name, startTime, durationSeconds, isFloating, availTypeId, timingType, transitionCode, clockTemplateId
  - [x] 3.2 Create `BreakStructureResponseDTO` with all break structure fields plus related data (avail type name, etc.)
  - [x] 3.3 Create `BreakStructureService` interface in `services` package with methods: create, getById, getByClockTemplateId, update, delete
  - [x] 3.4 Create `BreakStructureServiceImpl` in `services.impl` package implementing the interface with constructor injection, transaction management, and proper error handling
  - [x] 3.5 Create `BreakStructureRepository` interface extending JpaRepository with custom query methods (findByClockTemplateId, etc.)
  - [x] 3.6 Create `BreakStructureController` in `controllers` package with REST endpoints: POST /api/breaks, GET /api/breaks/{id}, GET /api/breaks/clock-templates/{clockId}, PUT /api/breaks/{id}, DELETE /api/breaks/{id}
  - [x] 3.7 Add Swagger/OpenAPI annotations to all controller endpoints
  - [x] 3.8 Implement proper validation in service layer (check clock template exists, validate timing, etc.)
  - [x] 3.9 Write unit tests for `BreakStructureServiceImpl` (80%+ coverage)
  - [ ] 3.10 Write integration tests for `BreakStructureController` endpoints

- [x] 4.0 Create Fixed Assets and Automation Commands Infrastructure
  - [x] 4.1 Create `FixedAssetRequestDTO` and `FixedAssetResponseDTO` with all required fields
  - [x] 4.2 Create `FixedAssetService` interface and `FixedAssetServiceImpl` with CRUD operations
  - [x] 4.3 Create `FixedAssetRepository` interface extending JpaRepository
  - [x] 4.4 Create `FixedAssetController` with REST endpoints for CRUD operations
  - [x] 4.5 Create `AutomationCommandRequestDTO` and `AutomationCommandResponseDTO` with all required fields including parameters (Map or JSON)
  - [x] 4.6 Create `AutomationCommandService` interface and `AutomationCommandServiceImpl` with CRUD operations
  - [x] 4.7 Create `AutomationCommandRepository` interface extending JpaRepository
  - [x] 4.8 Create `AutomationCommandController` with REST endpoints for CRUD operations
  - [x] 4.9 Add Swagger/OpenAPI annotations to all new controller endpoints
  - [x] 4.10 Write unit tests for both service implementations (80%+ coverage)
  - [ ] 4.11 Write integration tests for both controllers

- [x] 5.0 Implement Avail Types and Timing Rules
  - [x] 5.1 Create `AvailTypeRepository` interface extending JpaRepository
  - [x] 5.2 Create `AvailTypeService` interface and `AvailTypeServiceImpl` for managing avail types (if entity-based)
  - [x] 5.3 Create `AvailTypeController` with REST endpoints for CRUD operations (if entity-based)
  - [x] 5.4 Implement default avail types seeding (Weather Sponsor Only, Sports Content Only, General, Premium)
  - [x] 5.5 Update `BreakStructureService` to validate and assign avail types
  - [x] 5.6 Implement timing type validation logic (Hard Start must have exact time, Soft Start can be approximate)
  - [x] 5.7 Add timing type defaults (Fixed Assets default to Hard Start, Breaks default to Soft Start)
  - [x] 5.8 Write unit tests for avail type and timing rule logic (80%+ coverage)

- [x] 6.0 Build Clock Validation and Conflict Detection Service
  - [x] 6.1 Create `ClockValidationService` interface with methods: validateClock, detectConflicts, checkOverlaps, validateTiming
  - [x] 6.2 Create `ClockValidationResultDTO` with fields: isValid, errors (List), warnings (List), conflictDetails
  - [x] 6.3 Create `ClockValidationServiceImpl` implementing validation logic:
    - Detect overlapping commercial breaks
    - Detect overlapping fixed assets with breaks
    - Detect high-priority automation commands within 30 seconds
    - Validate total break time doesn't exceed limits (e.g., >18 minutes/hour)
    - Check all elements fit within 60-minute hour
  - [x] 6.4 Implement real-time validation that can be called during clock building
  - [x] 6.5 Create validation error/warning message formatting with actionable descriptions
  - [x] 6.6 Add endpoint in `ClockController`: POST /api/clocks/{id}/validate
  - [x] 6.7 Write comprehensive unit tests for all validation scenarios (80%+ coverage)
  - [ ] 6.8 Write integration tests for validation endpoint

- [ ] 7.0 Create Clock Builder Service with Structure Management
  - [ ] 7.1 Create `ClockBuilderService` interface with methods: getClockStructure, addBreak, updateBreak, removeBreak, addFixedAsset, updateFixedAsset, removeFixedAsset, addAutomationCommand, updateAutomationCommand, removeAutomationCommand
  - [ ] 7.2 Create `ClockTemplateWithBreaksDTO` containing clock template plus all breaks, fixed assets, and automation commands
  - [ ] 7.3 Create `ClockBuilderServiceImpl` implementing structure management with proper transaction handling
  - [ ] 7.4 Implement `getClockStructure` method that loads clock template with all related elements
  - [ ] 7.5 Implement methods to add/update/remove breaks, fixed assets, and automation commands
  - [ ] 7.6 Integrate validation service calls when elements are added/modified
  - [ ] 7.7 Create `ClockBuilderController` with endpoints:
    - GET /api/clock-templates/{id}/structure - Get full clock structure
    - POST /api/clock-templates/{id}/breaks - Add break (delegate to BreakStructureController)
    - POST /api/clock-templates/{id}/fixed-assets - Add fixed asset
    - POST /api/clock-templates/{id}/automation-commands - Add automation command
  - [ ] 7.8 Add Swagger/OpenAPI annotations to all endpoints
  - [ ] 7.9 Write unit tests for `ClockBuilderServiceImpl` (80%+ coverage)
  - [ ] 7.10 Write integration tests for structure management endpoints

- [ ] 8.0 Implement Revenue Analysis Service
  - [ ] 8.1 Create `RevenueAnalysisService` interface with method: calculateRevenueImpact(clockTemplateId)
  - [ ] 8.2 Create `RevenueAnalysisDTO` with fields: totalInventoryMinutes, potentialRevenue, revenueByBreak, revenueByAvailType, warnings
  - [ ] 8.3 Create `RevenueAnalysisServiceImpl` implementing revenue calculation logic:
    - Calculate total commercial inventory in minutes
    - Apply current spot rates (if available) or use default rates
    - Calculate potential revenue per break
    - Break down by avail type if configured
    - Generate revenue impact messages for changes
  - [ ] 8.4 Add endpoint in `ClockBuilderController`: GET /api/clock-templates/{id}/revenue-analysis
  - [ ] 8.5 Implement revenue change calculation when breaks are added/removed/resized
  - [ ] 8.6 Add Swagger/OpenAPI annotations
  - [ ] 8.7 Write unit tests for revenue calculation logic (80%+ coverage)
  - [ ] 8.8 Write integration tests for revenue analysis endpoint

- [ ] 9.0 Build Visual Clock Builder UI (Timeline and Drag-and-Drop)
  - [ ] 9.1 Create `clock-builder.css` with styles for timeline visualization, element blocks, drag-and-drop feedback
  - [ ] 9.2 Create `clock-builder.js` with JavaScript functions for timeline rendering and interaction
  - [ ] 9.3 Implement timeline visualization in `dashboard.html`:
    - Horizontal timeline representing 0-60 minutes
    - Time markers every 5 minutes
    - Color-coded element blocks (breaks=red/orange, fixed assets=blue, commands=yellow, content=gray/green)
  - [ ] 9.4 Implement drag-and-drop functionality using interact.js or similar library:
    - Make break blocks draggable
    - Show visual feedback during drag (ghost element, drop zones)
    - Implement snap-to-grid for common break times (:00, :15, :30, :45)
    - Display tooltips showing exact time and duration while dragging
  - [ ] 9.5 Implement break resizing by dragging edges
  - [ ] 9.6 Create properties panel (slide-out from right) for editing element properties
  - [ ] 9.7 Implement real-time timeline updates when properties are edited
  - [ ] 9.8 Add list/table view as alternative to timeline view with synchronized updates
  - [ ] 9.9 Implement validation panel showing real-time warnings/errors
  - [ ] 9.10 Implement revenue analysis panel showing revenue impact
  - [ ] 9.11 Add "Add Break", "Add Fixed Asset", "Add Automation Command" buttons
  - [ ] 9.12 Implement save functionality that sends clock structure to backend
  - [ ] 9.13 Ensure UI is mobile responsive (horizontal scroll for timeline on mobile, collapsible panels)
  - [ ] 9.14 Ensure UI meets WCAG 2.1 Level AA accessibility (keyboard navigation, ARIA labels, color contrast)
  - [ ] 9.15 Test drag-and-drop on touch devices (tablets)

- [ ] 10.0 Implement Template Management Features (Clone, Library, Daypart Assignments)
  - [ ] 10.1 Enhance `ClockService` with `cloneClockTemplate` method that creates a copy with all elements
  - [ ] 10.2 Add endpoint in `ClockController`: POST /api/clock-templates/{id}/clone
  - [ ] 10.3 Create `DaypartAssignmentService` interface and implementation for managing daypart-to-clock assignments
  - [ ] 10.4 Create `DaypartAssignmentController` with endpoints for assigning clocks to dayparts
  - [ ] 10.5 Create `GridService` interface and implementation for managing weekly grids
  - [ ] 10.6 Create `GridController` with endpoints for CRUD operations on grids
  - [ ] 10.7 Implement grid daypart mapping functionality (assign dayparts to specific days of week)
  - [ ] 10.8 Enhance clock template list view in UI to show template library with filters (by channel, active status, search)
  - [ ] 10.9 Add "Clone" button in clock template UI that calls clone endpoint
  - [ ] 10.10 Display template usage information (which dayparts/grids use each template)
  - [ ] 10.11 Write unit tests for clone functionality (80%+ coverage)
  - [ ] 10.12 Write integration tests for template management endpoints

- [ ] 11.0 Create LibreTime Integration and Export
  - [ ] 11.1 Research LibreTime API documentation for clock format and endpoints
  - [ ] 11.2 Create `LibreTimeConfig` class for LibreTime connection configuration (API key, base URL)
  - [ ] 11.3 Create `LibreTimeClient` class in `integrations` package using Spring WebClient:
    - Implement authentication (API key)
    - Implement clock export endpoint
    - Implement log push endpoint
    - Handle errors and retries
  - [ ] 11.4 Create `LibreTimeSyncService` interface with methods: exportClock, pushLog, generateLogFromClock
  - [ ] 11.5 Create `LibreTimeSyncServiceImpl` implementing conversion from internal clock format to LibreTime format
  - [ ] 11.6 Create `LibreTimeExportDTO` matching LibreTime's expected format
  - [ ] 11.7 Implement log generation from clock templates (daily logs)
  - [ ] 11.8 Add endpoint in `ClockBuilderController`: POST /api/clock-templates/{id}/export/libretime
  - [ ] 11.9 Add Swagger/OpenAPI annotations
  - [ ] 11.10 Write unit tests for LibreTime client and sync service (80%+ coverage)
  - [ ] 11.11 Write integration tests with mocked LibreTime API

- [ ] 12.0 Add WideOrbit Compatibility Features
  - [ ] 12.1 Implement WideOrbit track category support in clock elements (S1, S2, S3)
  - [ ] 12.2 Implement WideOrbit asset type assignment to fixed assets and breaks
  - [ ] 12.3 Implement transition code assignment (Segue, Overlap, Hard Start) to breaks and elements
  - [ ] 12.4 Implement show segment naming convention support (SH_MORNING_SEG1, etc.)
  - [ ] 12.5 Create WideOrbit export format converter
  - [ ] 12.6 Add endpoint: POST /api/clock-templates/{id}/export/wideorbit
  - [ ] 12.7 Ensure all WideOrbit-compatible metadata is preserved during export
  - [ ] 12.8 Update UI to show WideOrbit asset types and categories in element properties
  - [ ] 12.9 Write unit tests for WideOrbit export format conversion (80%+ coverage)

- [ ] 13.0 Implement Testing and Quality Assurance
  - [ ] 13.1 Write unit tests for all enum classes (TimingType, TransitionCode, etc.) - verify 80%+ coverage
  - [ ] 13.2 Write unit tests for all service implementations - verify 80%+ coverage
  - [ ] 13.3 Write unit tests for all controller classes - verify 80%+ coverage
  - [ ] 13.4 Write repository tests using @DataJpaTest for all repositories
  - [ ] 13.5 Write integration tests for critical workflows (create clock with breaks, validate, export)
  - [ ] 13.6 Write integration tests for LibreTime export functionality
  - [ ] 13.7 Run full test suite and verify all tests pass (`mvn clean test`)
  - [ ] 13.8 Generate JaCoCo coverage report and verify 80%+ coverage (`mvn jacoco:report`)
  - [ ] 13.9 Fix any coverage gaps to meet 80% threshold
  - [ ] 13.10 Verify build passes with coverage enforcement (`mvn clean install`)
  - [ ] 13.11 Perform manual testing of clock builder UI (create, edit, drag-and-drop, validate, export)
  - [ ] 13.12 Test mobile responsiveness on multiple devices
  - [ ] 13.13 Test accessibility with screen readers and keyboard navigation
  - [ ] 13.14 Verify color contrast ratios meet WCAG 2.1 Level AA requirements

