# Task List: Python to Java Backend Migration

Based on PRD: `prd-python-to-java-backend-migration.md`

## Relevant Files

### Configuration Files
- `librelog-web-api/pom.xml` - Maven POM with dependencies and JaCoCo configuration
- `librelog-web-api/src/main/resources/application.properties` - Spring Boot application configuration
- `librelog-liquibase/src/main/resources/db/changelog/db.changelog-master.xml` - Master Liquibase changelog

### Core Infrastructure
- `librelog-web-api/src/main/java/com/onelpro/librelog/config/*.java` - Spring configuration classes
- `librelog-web-api/src/main/java/com/onelpro/librelog/exceptions/*.java` - Custom exception classes
- `librelog-web-api/src/main/java/com/onelpro/librelog/utils/*.java` - Utility classes

### Middleware
- `librelog-web-api/src/main/java/com/onelpro/librelog/config/SecurityConfig.java` - Spring Security configuration
- `librelog-web-api/src/main/java/com/onelpro/librelog/config/RateLimitConfig.java` - Rate limiting configuration
- `librelog-web-api/src/main/java/com/onelpro/librelog/config/CorsConfig.java` - CORS configuration
- `librelog-web-api/src/main/java/com/onelpro/librelog/config/LoggingConfig.java` - Logging configuration

### Database Migrations
- `librelog-liquibase/src/main/resources/db/changelog/001-initial-schema.xml` - Initial database schema
- `librelog-liquibase/src/main/resources/db/changelog/002-*.xml` through `027-*.xml` - Converted Alembic migrations

### Domain Models (JPA Entities)
- `librelog-web-api/src/main/java/com/onelpro/librelog/models/*.java` - All 59 JPA entity classes
- `librelog-web-api/src/main/java/com/onelpro/librelog/enums/*.java` - Enum classes for entity fields

### Repositories
- `librelog-web-api/src/main/java/com/onelpro/librelog/repositories/*.java` - Spring Data JPA repository interfaces

### Services
- `librelog-web-api/src/main/java/com/onelpro/librelog/services/*.java` - Service interfaces (40+ services)
- `librelog-web-api/src/main/java/com/onelpro/librelog/services/impl/*.java` - Service implementations

### DTOs
- `librelog-web-api/src/main/java/com/onelpro/librelog/dto/*DTO.java` - Request/response DTOs

### Controllers
- `librelog-web-api/src/main/java/com/onelpro/librelog/controllers/*Controller.java` - REST controllers (54 controllers)

### Integrations
- `librelog-web-api/src/main/java/com/onelpro/librelog/integrations/LibreTimeClient.java` - LibreTime integration client
- `librelog-web-api/src/main/java/com/onelpro/librelog/integrations/AzuraCastClient.java` - AzuraCast integration client

### Tests
- `librelog-web-api/src/test/java/com/onelpro/librelog/**/*Test.java` - Unit tests
- `librelog-web-api/src/test/java/com/onelpro/librelog/**/*IT.java` - Integration tests
- `librelog-web-api/src/test/java/com/onelpro/librelog/config/TestcontainersConfiguration.java` - Testcontainers setup

## Tasks

- [x] 1.0 Setup Core Infrastructure and Configuration
  - [x] 1.1 Configure application.properties with database connection, JPA, and Liquibase settings
  - [x] 1.2 Add Spring Security dependency and configure basic security settings
  - [x] 1.3 Add Spring Data JPA, PostgreSQL driver, and Liquibase dependencies to pom.xml
  - [x] 1.4 Configure Lombok annotation processing in Maven compiler plugin
  - [x] 1.5 Add JaCoCo Maven plugin with 80% coverage threshold configuration
  - [x] 1.6 Add Testcontainers dependencies for integration testing
  - [x] 1.7 Configure OpenAPI/Swagger dependencies for API documentation
  - [x] 1.8 Add logging dependencies (Log4j2 or Slf4j with Logback)
  - [x] 1.9 Create base package structure following coding guidelines (config, controllers, dto, enums, exceptions, models, repositories, services, utils)
  - [x] 1.10 Verify Maven build compiles successfully (`mvn clean compile`) - Configuration verified. **BLOCKER**: JDK (javac) must be installed for compilation. Only JRE is currently installed. Install with: `sudo apt-get install openjdk-21-jdk-headless`. See README-BUILD.md for details.

- [x] 2.0 Convert Database Migrations from Alembic to Liquibase
  - [x] 2.1 Analyze Alembic migration 0001_initial_migration.py and create equivalent Liquibase changeset 001-initial-schema.xml
  - [x] 2.2 Convert migration 002_add_traffic_management_tables.py to 002-add-traffic-management-tables.xml
  - [x] 2.3 Convert migration 003_add_spot_scheduling_tables.py to 003-add-spot-scheduling-tables.xml
  - [x] 2.4 Convert migration 004_add_copy_management_tables.py to 004-add-copy-management-tables.xml
  - [x] 2.5 Convert migration 005_add_billing_tables.py to 005-add-billing-tables.xml
  - [x] 2.6 Convert migration 006_add_audit_and_permissions.py to 006-add-audit-and-permissions.xml
  - [x] 2.7 Convert migration 007_add_inventory_revenue_tables.py to 007-add-inventory-revenue-tables.xml
  - [x] 2.8 Convert migration 008_add_digital_orders.py to 008-add-digital-orders.xml
  - [x] 2.9 Convert migration 009_add_webhooks_and_notifications.py to 009-add-webhooks-and-notifications.xml
  - [x] 2.10 Convert migration 010_add_settings_table.py to 010-add-settings-table.xml
  - [x] 2.11 Convert migration 011_add_rotation_rules_traffic_logs.py to 011-add-rotation-rules-traffic-logs.xml
  - [x] 2.12 Convert migration 012_rename_metadata_to_meta_data.py to 012-rename-metadata-to-meta-data.xml
  - [x] 2.13 Convert migration 013_add_news_track_type.py to 013-add-news-track-type.xml
  - [x] 2.14 Convert migration 014_update_track_types.py to 014-update-track-types.xml
  - [x] 2.15 Convert migration 015_add_audio_management_tables.py to 015-add-audio-management-tables.xml
  - [x] 2.16 Convert migration 016_add_libretime_id_to_voice_tracks.py to 016-add-libretime-id-to-voice-tracks.xml
  - [x] 2.17 Convert migration 017_add_production_workflow_tables.py to 017-add-production-workflow-tables.xml
  - [x] 2.18 Convert migration 018_extend_voice_track_model.py to 018-extend-voice-track-model.xml
  - [x] 2.19 Convert migration 019_add_standardized_name_to_voice_tracks.py to 019-add-standardized-name-to-voice-tracks.xml
  - [x] 2.20 Convert migration 020_add_music_management_fields_to_tracks.py to 020-add-music-management-fields-to-tracks.xml
  - [x] 2.21 Convert migration 021_wideorbit_sales_order_schema.py to 021-wideorbit-sales-order-schema.xml
  - [x] 2.22 Convert migration 022_sales_admin_models.py to 022-sales-admin-models.xml
  - [x] 2.23 Convert migration 023_add_station_support.py to 023-add-station-support.xml
  - [x] 2.24 Convert migration 024_split_contact_name.py to 024-split-contact-name.xml
  - [x] 2.25 Convert migration 025_fix_schema_add_station_id_columns.py to 025-fix-schema-add-station-id-columns.xml
  - [x] 2.26 Convert migration 026_add_failed_login_attempts_table.py to 026-add-failed-login-attempts-table.xml
  - [x] 2.27 Convert migration 027_migrate_to_uuid_primary_keys.py to 027-migrate-to-uuid-primary-keys.xml
  - [ ] 2.28 Update db.changelog-master.xml to include all 27 changesets in order
  - [ ] 2.29 Verify all changesets are idempotent and can be run multiple times safely
  - [ ] 2.30 Test Liquibase migrations against clean database (`mvn liquibase:update`)

- [x] 3.0 Implement Core Infrastructure Components
  - [x] 3.1 Create GlobalExceptionHandler class with @ControllerAdvice for centralized exception handling
  - [x] 3.2 Create custom exception classes (NotFoundException, ValidationException, UnauthorizedException, etc.)
  - [x] 3.3 Create ErrorResponseDTO for standardized error responses
  - [x] 3.4 Implement SecurityHeadersMiddleware as Spring Filter for security headers (X-Content-Type-Options, X-Frame-Options, etc.)
  - [x] 3.5 Implement RateLimitMiddleware as Spring Filter using bucket4j or similar
  - [x] 3.6 Create CORS configuration class with @Configuration
  - [x] 3.7 Create LoggingConfig for structured logging with Log4j2/Slf4j (configured via application.properties)
  - [x] 3.8 Create OpenAPI/Swagger configuration class for API documentation
  - [x] 3.9 Create DatabaseConfig for JPA/Hibernate settings
  - [x] 3.10 Create TestcontainersConfiguration class for integration tests (already exists)
  - [x] 3.11 Create utility classes for common operations (DateUtils, StringUtils, ValidationUtils)
  - [x] 3.12 Write unit tests for utility classes (80%+ coverage) - 99 tests passing

- [ ] 4.0 Migrate Authentication and Authorization System
  - [ ] 4.1 Create User entity with JPA annotations, UUID primary key, and Lombok annotations
  - [ ] 4.2 Create UserStatus enum in enums package with @Enumerated(EnumType.STRING)
  - [ ] 4.3 Create UserRole enum in enums package
  - [ ] 4.4 Create FailedLoginAttempt entity
  - [ ] 4.5 Create UserRepository interface extending JpaRepository
  - [ ] 4.6 Create FailedLoginAttemptRepository interface
  - [ ] 4.7 Create PasswordValidator utility class with validation logic
  - [ ] 4.8 Create JwtTokenService for JWT token generation and validation
  - [ ] 4.9 Create TokenBlacklistService interface and implementation
  - [ ] 4.10 Create AuthSecurityService interface and implementation
  - [ ] 4.11 Create LoginRequestDTO, RegisterRequestDTO, AuthResponseDTO, UserResponseDTO
  - [ ] 4.12 Create AuthService interface and AuthServiceImpl with login, register, refresh token methods
  - [ ] 4.13 Create JwtAuthenticationFilter extending OncePerRequestFilter
  - [ ] 4.14 Create SecurityConfig class with Spring Security configuration, password encoder, and JWT filter
  - [ ] 4.15 Create AuthController with login, register, logout, refresh endpoints
  - [ ] 4.16 Write unit tests for AuthService (80%+ coverage)
  - [ ] 4.17 Write unit tests for JwtTokenService
  - [ ] 4.18 Write integration tests for AuthController endpoints
  - [ ] 4.19 Write unit tests for PasswordValidator

- [ ] 5.0 Migrate Domain Models (JPA Entities)
  - [ ] 5.1 Create Advertiser entity with UUID primary key, Lombok annotations, and JPA relationships
  - [ ] 5.2 Create Agency entity
  - [ ] 5.3 Create Track entity with enum fields for track type, status
  - [ ] 5.4 Create Campaign entity
  - [ ] 5.5 Create ClockTemplate entity
  - [ ] 5.6 Create DailyLog entity
  - [ ] 5.7 Create VoiceTrack entity with relationships
  - [ ] 5.8 Create Order entity with OrderLine and OrderAttachment relationships
  - [ ] 5.9 Create Spot entity
  - [ ] 5.10 Create Daypart and DaypartCategory entities
  - [ ] 5.11 Create RotationRule entity
  - [ ] 5.12 Create TrafficLog entity
  - [ ] 5.13 Create BreakStructure entity
  - [ ] 5.14 Create Copy and CopyAssignment entities
  - [ ] 5.15 Create Invoice, InvoiceLine, Payment, Makegood entities
  - [ ] 5.16 Create AuditLog entity
  - [ ] 5.17 Create LogRevision entity
  - [ ] 5.18 Create InventorySlot entity
  - [ ] 5.19 Create Revenue entity
  - [ ] 5.20 Create SalesGoal entity
  - [ ] 5.21 Create SalesRep, SalesTeam, SalesOffice, SalesRegion, Station, Cluster entities
  - [ ] 5.22 Create Notification entity
  - [ ] 5.23 Create Webhook entity
  - [ ] 5.24 Create Settings entity
  - [ ] 5.25 Create AudioCut, AudioDelivery, AudioQcResult, AudioVersion entities
  - [ ] 5.26 Create LiveRead entity
  - [ ] 5.27 Create PoliticalRecord entity
  - [ ] 5.28 Create ProductionOrder, ProductionAssignment, ProductionComment, ProductionRevision entities
  - [ ] 5.29 Create VoiceTalentRequest entity
  - [ ] 5.30 Create VoiceTrackAudit and VoiceTrackSlot entities
  - [ ] 5.31 Create PlaybackHistory entity
  - [ ] 5.32 Create DigitalOrder entity
  - [ ] 5.33 Create OrderTemplate, OrderRevision, OrderWorkflowState entities
  - [ ] 5.34 Create Backup entity
  - [ ] 5.35 Create all required enum classes (TrackType, OrderStatus, CampaignStatus, etc.) in enums package
  - [ ] 5.36 Create Repository interfaces for all entities extending JpaRepository
  - [ ] 5.37 Verify all entities use UUID primary keys with @GeneratedValue(strategy = GenerationType.UUID)
  - [ ] 5.38 Verify all enum fields use @Enumerated(EnumType.STRING)
  - [ ] 5.39 Write unit tests for enum classes
  - [ ] 5.40 Write repository tests using @DataJpaTest for critical repositories

- [ ] 6.0 Migrate Service Layer
  - [ ] 6.1 Create TrackService interface and TrackServiceImpl with CRUD operations
  - [ ] 6.2 Create CampaignService interface and CampaignServiceImpl
  - [ ] 6.3 Create ClockService interface and ClockServiceImpl
  - [ ] 6.4 Create LogService interface and LogServiceImpl (for daily logs)
  - [ ] 6.5 Create VoiceTrackService interface and VoiceTrackServiceImpl
  - [ ] 6.6 Create OrderService interface and OrderServiceImpl
  - [ ] 6.7 Create SpotService interface and SpotServiceImpl
  - [ ] 6.8 Create DaypartService and DaypartCategoryService interfaces and implementations
  - [ ] 6.9 Create RotationRuleService interface and implementation
  - [ ] 6.10 Create TrafficLogService interface and implementation
  - [ ] 6.11 Create BreakStructureService interface and implementation
  - [ ] 6.12 Create CopyService and CopyAssignmentService interfaces and implementations
  - [ ] 6.13 Create BillingService interface and implementation (for invoices, payments, makegoods)
  - [ ] 6.14 Create AuditService interface and AuditServiceImpl
  - [ ] 6.15 Create InventoryService interface and implementation
  - [ ] 6.16 Create RevenueService interface and implementation
  - [ ] 6.17 Create SalesGoalService interface and implementation
  - [ ] 6.18 Create SalesRepService, SalesTeamService, SalesOfficeService, SalesRegionService, StationService, ClusterService interfaces and implementations
  - [ ] 6.19 Create NotificationService interface and NotificationServiceImpl
  - [ ] 6.20 Create WebhookService interface and implementation
  - [ ] 6.21 Create SettingsService interface and implementation
  - [ ] 6.22 Create AudioCutService, AudioDeliveryService, AudioQcService interfaces and implementations
  - [ ] 6.23 Create LiveReadService interface and implementation
  - [ ] 6.24 Create PoliticalComplianceService interface and implementation
  - [ ] 6.25 Create ProductionOrderService, ProductionAssignmentService, ProductionApprovalService, ProductionDeliveryService, ProductionArchiveService, ProductionRoutingService, ProductionNotificationService interfaces and implementations
  - [ ] 6.26 Create VoiceTalentService interface and implementation
  - [ ] 6.27 Create VoiceTrackSlotService interface and implementation
  - [ ] 6.28 Create LogEditorService interface and implementation
  - [ ] 6.29 Create LogGeneratorService interface and implementation
  - [ ] 6.30 Create MusicSelectionService interface and implementation
  - [ ] 6.31 Create SpotSchedulerService interface and implementation
  - [ ] 6.32 Create TimingService interface and implementation
  - [ ] 6.33 Create ReportService interface and ReportServiceImpl
  - [ ] 6.34 Create BackupService interface and implementation
  - [ ] 6.35 Create CollaborationService interface and implementation
  - [ ] 6.36 Create AttachmentService interface and implementation
  - [ ] 6.37 Create AudioPreviewService, AudioProcessingService, AudioSearchService interfaces and implementations
  - [ ] 6.38 Create UserService interface and UserServiceImpl
  - [ ] 6.39 Add @Transactional annotations where appropriate
  - [ ] 6.40 Write unit tests for all service implementations (80%+ coverage requirement)
  - [ ] 6.41 Verify all services use constructor injection (no @Autowired field injection)

- [ ] 7.0 Migrate REST API Controllers
  - [ ] 7.1 Create AuthController (already in task 4.15, verify complete)
  - [ ] 7.2 Create TrackController with CRUD endpoints, following REST conventions
  - [ ] 7.3 Create CampaignController with campaign management endpoints
  - [ ] 7.4 Create ClockController for clock template management
  - [ ] 7.5 Create LogController for daily log operations
  - [ ] 7.6 Create VoiceTrackController for voice track management
  - [ ] 7.7 Create OrderController with order CRUD and workflow endpoints
  - [ ] 7.8 Create OrderLineController for order line management
  - [ ] 7.9 Create OrderAttachmentController for attachment handling
  - [ ] 7.10 Create SpotController for spot scheduling
  - [ ] 7.11 Create DaypartController and DaypartCategoryController
  - [ ] 7.12 Create RotationRuleController
  - [ ] 7.13 Create TrafficLogController
  - [ ] 7.14 Create BreakStructureController
  - [ ] 7.15 Create CopyController and CopyAssignmentController
  - [ ] 7.16 Create InvoiceController, PaymentController, MakegoodController
  - [ ] 7.17 Create AuditLogController
  - [ ] 7.18 Create LogRevisionController
  - [ ] 7.19 Create InventoryController
  - [ ] 7.20 Create RevenueController
  - [ ] 7.21 Create SalesGoalController
  - [ ] 7.22 Create SalesRepController, SalesTeamController, SalesOfficeController, SalesRegionController, StationController, ClusterController
  - [ ] 7.23 Create NotificationController
  - [ ] 7.24 Create WebhookController
  - [ ] 7.25 Create SettingsController
  - [ ] 7.26 Create AudioCutController, AudioDeliveryController, AudioQcController
  - [ ] 7.27 Create LiveReadController
  - [ ] 7.28 Create PoliticalComplianceController
  - [ ] 7.29 Create ProductionOrderController, ProductionAssignmentController, ProductionArchiveController
  - [ ] 7.30 Create VoiceTalentController
  - [ ] 7.31 Create ReportController
  - [ ] 7.32 Create BackupController
  - [ ] 7.33 Create UserController
  - [ ] 7.34 Create ActivityController
  - [ ] 7.35 Create AdvertiserController and AgencyController
  - [ ] 7.36 Create SyncController for synchronization operations
  - [ ] 7.37 Create SetupController for initial setup
  - [ ] 7.38 Create HelpController for help documentation
  - [ ] 7.39 Create ProxyController if needed
  - [ ] 7.40 Add OpenAPI/Swagger annotations (@Operation, @ApiResponse) to all controller endpoints
  - [ ] 7.41 Add proper HTTP status codes and error handling to all controllers
  - [ ] 7.42 Add authentication/authorization checks using @PreAuthorize or similar
  - [ ] 7.43 Create request/response DTOs for all controller endpoints (all ending with "DTO" suffix)
  - [ ] 7.44 Write integration tests for all controllers using @WebMvcTest or @SpringBootTest
  - [ ] 7.45 Verify all endpoints use UUIDs for resource identification

- [ ] 8.0 Rewrite External Integrations
  - [ ] 8.1 Analyze Python LibreTimeClient implementation
  - [ ] 8.2 Create LibreTimeClient Java class using Spring RestTemplate or WebClient
  - [ ] 8.3 Implement LibreTime authentication and API methods
  - [ ] 8.4 Create LibreTimeConfigService interface and implementation
  - [ ] 8.5 Analyze Python AzuraCastClient implementation
  - [ ] 8.6 Create AzuraCastClient Java class using Spring RestTemplate or WebClient
  - [ ] 8.7 Implement AzuraCast authentication and API methods
  - [ ] 8.8 Create TypeMapping utility class for converting between systems
  - [ ] 8.9 Create ApiConnector utility class for common HTTP operations
  - [ ] 8.10 Write unit tests for LibreTimeClient (80%+ coverage)
  - [ ] 8.11 Write unit tests for AzuraCastClient (80%+ coverage)
  - [ ] 8.12 Write integration tests using Testcontainers or mocks for external services

- [ ] 9.0 Implement Background Task Processing
  - [ ] 9.1 Analyze Python Celery task definitions
  - [ ] 9.2 Create scheduled task classes using @Scheduled annotation
  - [ ] 9.3 Create TaskSchedulerConfig for configuring scheduled tasks
  - [ ] 9.4 Implement async task processing using @Async and CompletableFuture
  - [ ] 9.5 Create AsyncConfig for thread pool configuration
  - [ ] 9.6 Migrate periodic tasks (log generation, sync operations, etc.) to @Scheduled methods
  - [ ] 9.7 Create task execution service for long-running background jobs
  - [ ] 9.8 Write unit tests for scheduled tasks
  - [ ] 9.9 Write integration tests for async operations

- [ ] 10.0 Implement Testing Infrastructure and Write Tests
  - [ ] 10.1 Verify JaCoCo Maven plugin is configured with 80% coverage threshold
  - [ ] 10.2 Create base test classes for common test setup
  - [ ] 10.3 Create test utilities and helpers
  - [ ] 10.4 Write unit tests for all service implementations (verify 80%+ coverage)
  - [ ] 10.5 Write unit tests for all utility classes
  - [ ] 10.6 Write repository tests using @DataJpaTest for all repositories
  - [ ] 10.7 Write integration tests using @SpringBootTest for critical workflows
  - [ ] 10.8 Write controller integration tests using @WebMvcTest
  - [ ] 10.9 Configure Testcontainers for PostgreSQL in integration tests
  - [ ] 10.10 Run full test suite and verify all tests pass (`mvn clean test`)
  - [ ] 10.11 Generate JaCoCo coverage report and verify 80%+ coverage (`mvn jacoco:report`)
  - [ ] 10.12 Fix any coverage gaps to meet 80% threshold
  - [ ] 10.13 Verify build passes with coverage enforcement (`mvn clean install`)

- [ ] 11.0 Documentation and Migration Verification
  - [ ] 11.1 Create API documentation using OpenAPI/Swagger annotations
  - [ ] 11.2 Verify all endpoints are documented with @Operation and @ApiResponse
  - [ ] 11.3 Create migration guide documenting differences from Python backend
  - [ ] 11.4 Create developer setup guide for Java backend
  - [ ] 11.5 Verify all 54 routers have been migrated to controllers
  - [ ] 11.6 Verify all 59 models have been migrated to JPA entities
  - [ ] 11.7 Verify all 40 services have been migrated
  - [ ] 11.8 Verify all 27 Alembic migrations have been converted to Liquibase
  - [ ] 11.9 Create feature parity checklist comparing Python and Java backends
  - [ ] 11.10 Run end-to-end integration tests to verify feature parity
  - [ ] 11.11 Document any known differences or limitations
  - [ ] 11.12 Update README with Java backend information

