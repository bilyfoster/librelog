# Task List: Fresh Start - Clean Spring Boot Foundation

Based on Plan: `fresh_start_-_clean_spring_boot_foundation_01a0cba1.plan.md`

## Relevant Files

### Configuration Files
- `pom.xml` - Parent Maven POM with module definitions
- `librelog-api/pom.xml` - Main Spring Boot application module POM
- `librelog-liquibase/pom.xml` - Liquibase database migration module POM
- `librelog-api/src/main/resources/application.properties` - Spring Boot application configuration
- `librelog-liquibase/src/main/resources/db/changelog/db.changelog-master.xml` - Master Liquibase changelog

### Core Infrastructure
- `librelog-api/src/main/java/com/onelpro/librelog/config/*.java` - Spring configuration classes
- `librelog-api/src/main/java/com/onelpro/librelog/exceptions/*.java` - Custom exception classes
- `librelog-api/src/main/java/com/onelpro/librelog/utils/*.java` - Utility classes

### Domain Models (JPA Entities)
- `librelog-api/src/main/java/com/onelpro/librelog/models/*.java` - JPA entity classes
- `librelog-api/src/main/java/com/onelpro/librelog/enums/*.java` - Enum classes

### Repositories
- `librelog-api/src/main/java/com/onelpro/librelog/repositories/*.java` - Spring Data JPA repository interfaces

### Services
- `librelog-api/src/main/java/com/onelpro/librelog/services/*.java` - Service interfaces
- `librelog-api/src/main/java/com/onelpro/librelog/services/impl/*.java` - Service implementations

### DTOs
- `librelog-api/src/main/java/com/onelpro/librelog/dto/*DTO.java` - Request/response DTOs

### Controllers
- `librelog-api/src/main/java/com/onelpro/librelog/controllers/*Controller.java` - REST controllers

### Tests
- `librelog-api/src/test/java/com/onelpro/librelog/**/*Test.java` - Unit tests
- `librelog-api/src/test/java/com/onelpro/librelog/**/*IT.java` - Integration tests

## Tasks

- [x] 0.0 Archive Current Codebase and Prepare for Fresh Start
  - [x] 0.1 Create archive directory structure to preserve existing code
  - [x] 0.2 Move librelog-web-api directory to archive/librelog-web-api-old (current module is named **librelog-api**)
  - [x] 0.3 Move librelog-liquibase directory to archive/librelog-liquibase-old
  - [x] 0.4 Clean up target directories and build artifacts
  - [x] 0.5 Document any valuable configurations or learnings from archived code

- [x] 1.0 Create Clean Multi-Module Maven Project Structure
  - [x] 1.1 Create new librelog-api module directory structure
  - [x] 1.2 Create package structure: config, controllers, dto, enums, exceptions, models, repositories, services/impl, utils
  - [x] 1.3 Create librelog-liquibase module directory structure (resources-only)
  - [x] 1.4 Create test directory structure mirroring main package structure
  - [x] 1.5 Create src/main/resources directory for application.properties
  - [x] 1.6 Create src/main/resources/static directory for frontend assets (if needed)

- [x] 2.0 Configure Maven POMs and Build System
  - [x] 2.1 Update parent pom.xml with Spring Boot 4.0.0, Java 21, module definitions
  - [x] 2.2 Create librelog-api/pom.xml with Spring Boot dependencies (Web, JPA, Security, Liquibase, Validation)
  - [x] 2.3 Create librelog-liquibase/pom.xml as resources-only module (no Java classes)
  - [x] 2.4 Configure JaCoCo Maven plugin with 80% coverage threshold in librelog-api/pom.xml
  - [x] 2.5 Add Lombok dependency and annotation processor configuration
  - [x] 2.6 Add Testcontainers dependencies for PostgreSQL integration tests
  - [x] 2.7 Add OpenAPI/Swagger dependencies (springdoc-openapi-starter-webmvc-ui)
  - [x] 2.8 Add Log4j2/Slf4j logging dependencies
  - [x] 2.9 Add JWT dependencies (jjwt-api, jjwt-impl, jjwt-jackson)
  - [x] 2.10 Add rate limiting dependency (bucket4j-core)
  - [x] 2.11 Verify Maven build succeeds (`mvn clean compile`)

- [x] 3.0 Setup Database Foundation with Liquibase
  - [x] 3.1 Create librelog-liquibase/src/main/resources/db/changelog directory
  - [x] 3.2 Create db.changelog-master.xml with initial structure
  - [x] 3.3 Create 001-initial-schema.xml changeset with users table (UUID primary key)
  - [ ] 3.4 Create 002-create-stations-table.xml changeset
  - [x] 3.5 Configure application.properties with PostgreSQL connection settings
  - [x] 3.6 Configure Liquibase in application.properties (enabled, change-log location)
  - [ ] 3.7 Create TestcontainersConfiguration class for integration tests
  - [x] 3.8 Verify Liquibase changesets are idempotent
  - [ ] 3.9 Test database migration against clean PostgreSQL database

- [x] 4.0 Implement Core Infrastructure Components
  - [x] 4.1 Create GlobalExceptionHandler class with @ControllerAdvice
  - [x] 4.2 Create custom exception classes: NotFoundException, ValidationException, UnauthorizedException, BadRequestException
  - [x] 4.3 Create ErrorResponseDTO for standardized error responses
  - [x] 4.4 Create SecurityConfig class with Spring Security configuration
  - [x] 4.5 Create CORS configuration class with @Configuration
  - [ ] 4.6 Create LoggingConfig for structured logging (Log4j2/Slf4j)
  - [x] 4.7 Create OpenAPI/Swagger configuration class
  - [ ] 4.8 Create RateLimitConfig for rate limiting configuration
  - [ ] 4.9 Create DatabaseConfig for JPA/Hibernate settings
  - [x] 4.10 Create utility classes: PasswordValidator (DateUtils, StringUtils, ValidationUtils can be added as needed)
  - [x] 4.11 Write unit tests for utility classes (80%+ coverage requirement)

- [x] 5.0 Implement Basic Authentication System
  - [x] 5.1 Create UserStatus enum in enums package with Javadoc (ACTIVE, INACTIVE, BANNED)
  - [x] 5.2 Create UserRole enum in enums package with Javadoc (ADMIN, USER, etc.)
  - [x] 5.3 Create User entity with UUID primary key, Lombok annotations (@Data, @Builder, @NoArgsConstructor, @AllArgsConstructor)
  - [x] 5.4 Add @Enumerated(EnumType.STRING) to UserStatus and UserRole fields in User entity
  - [x] 5.5 Create UserRepository interface extending JpaRepository<User, UUID>
  - [x] 5.6 Create PasswordValidator utility class with validation logic
  - [x] 5.7 Create JwtService for JWT token generation and validation
  - [x] 5.8 Create AuthService interface in services package
  - [x] 5.9 Create AuthServiceImpl in services.impl package with login, register methods
  - [x] 5.10 Create LoginRequestDTO, RegisterRequestDTO, AuthResponseDTO, UserResponseDTO (all ending with "DTO")
  - [x] 5.11 Create JwtAuthenticationFilter extending OncePerRequestFilter
  - [x] 5.12 Create SecurityConfig with JWT filter and password encoder
  - [x] 5.13 Create AuthController with login, register endpoints
  - [x] 5.14 Add OpenAPI/Swagger annotations to AuthController endpoints
  - [x] 5.15 Write unit tests for AuthService (80%+ coverage)
  - [x] 5.16 Write unit tests for JwtService (80%+ coverage)
  - [x] 5.17 Write unit tests for PasswordValidator (80%+ coverage)
  - [x] 5.18 Write unit tests for UserStatus and UserRole enums
  - [ ] 5.19 Write integration tests for AuthController endpoints using @SpringBootTest
  - [x] 5.20 Verify all endpoints use UUIDs for resource identification

- [ ] 6.0 Verify Clean Build and Foundation
  - [ ] 6.1 Run `mvn clean install` and verify build succeeds
  - [ ] 6.2 Verify all tests pass (100% pass rate)
  - [ ] 6.3 Generate JaCoCo coverage report and verify 80%+ coverage
  - [ ] 6.4 Verify application starts without errors (`mvn spring-boot:run`)
  - [ ] 6.5 Test basic authentication endpoints (login, register) via Swagger UI or Postman
  - [ ] 6.6 Verify database migrations run successfully
  - [ ] 6.7 Verify all code follows coding guidelines (no wildcard imports, proper logging, constructor injection)
  - [ ] 6.8 Document any issues or deviations from plan

- [ ] 7.0 Phase 1.1: Implement Station & Channel Configuration
  - [x] 7.1 Create Station entity with UUID primary key, Lombok annotations, JPA relationships
  - [x] 7.2 Create Channel entity with UUID primary key, relationship to Station
  - [x] 7.3 Create ClockTemplate entity (24-hour broadcast structure)
  - [x] 7.4 Create BreakStructure entity (commercial break definitions)
  - [x] 7.5 Create Daypart entity (morning drive, afternoon drive, etc.)
  - [x] 7.6 Create DaypartCategory entity
  - [x] 7.7 Create StationRepository, ChannelRepository, ClockTemplateRepository, BreakStructureRepository, DaypartRepository, DaypartCategoryRepository
  - [x] 7.8 Create StationService interface in services package
  - [x] 7.9 Create StationServiceImpl in services.impl package with CRUD operations
  - [ ] 7.10 Create ClockService interface and ClockServiceImpl
  - [ ] 7.11 Create DaypartService interface and DaypartServiceImpl
  - [x] 7.12 Create StationRequestDTO, StationResponseDTO (ClockTemplateRequestDTO, ClockTemplateResponseDTO, DaypartRequestDTO, DaypartResponseDTO pending)
  - [x] 7.13 Create StationController with CRUD endpoints
  - [ ] 7.14 Create ClockController for clock template management
  - [ ] 7.15 Create DaypartController for daypart management
  - [x] 7.16 Add OpenAPI/Swagger annotations to all controller endpoints
  - [x] 7.17 Create Liquibase changesets for all new tables (UUID primary keys, @Enumerated(EnumType.STRING) for enums)
  - [x] 7.18 Write unit tests for StationService implementation (80%+ coverage)
  - [ ] 7.19 Write integration tests for all controllers
  - [ ] 7.20 Write repository tests using @DataJpaTest

- [ ] 8.0 Phase 1.2: Implement Order Management System (OMS)
  - [ ] 8.1 Create OrderStatus enum in enums package with Javadoc
  - [ ] 8.2 Create Order entity with UUID primary key, Lombok annotations, JPA relationships
  - [ ] 8.3 Create OrderLine entity (spot requirements) with relationship to Order
  - [ ] 8.4 Create OrderAttachment entity with relationship to Order
  - [ ] 8.5 Create OrderTemplate entity
  - [ ] 8.6 Create OrderRevision entity
  - [ ] 8.7 Create OrderWorkflowState entity
  - [ ] 8.8 Create OrderRepository, OrderLineRepository, OrderAttachmentRepository, OrderTemplateRepository
  - [ ] 8.9 Create OrderService interface in services package
  - [ ] 8.10 Create OrderServiceImpl in services.impl package with CRUD and workflow methods
  - [ ] 8.11 Implement complex date-range logic (Standard vs Broadcast calendars)
  - [ ] 8.12 Create OrderRequestDTO, OrderResponseDTO, OrderLineRequestDTO, OrderLineResponseDTO
  - [ ] 8.13 Create OrderController with CRUD and workflow endpoints
  - [ ] 8.14 Create OrderLineController for order line management
  - [ ] 8.15 Create OrderAttachmentController for attachment handling
  - [ ] 8.16 Add OpenAPI/Swagger annotations to all controller endpoints
  - [ ] 8.17 Create Liquibase changesets for all new tables
  - [ ] 8.18 Write unit tests for OrderService including date-range logic (80%+ coverage)
  - [ ] 8.19 Write integration tests for OrderController endpoints
  - [ ] 8.20 Write repository tests using @DataJpaTest

- [ ] 9.0 Phase 1.3: Implement Advertiser/Agency CRM
  - [ ] 9.1 Create Advertiser entity with UUID primary key, Lombok annotations, JPA relationships
  - [ ] 9.2 Create Agency entity with UUID primary key, relationship to Advertiser
  - [ ] 9.3 Create Contact entity with split name fields (firstName, lastName)
  - [ ] 9.4 Create SalesRep entity
  - [ ] 9.5 Create SalesTeam entity
  - [ ] 9.6 Create SalesOffice entity
  - [ ] 9.7 Create SalesRegion entity
  - [ ] 9.8 Create Cluster entity
  - [ ] 9.9 Create AdvertiserRepository, AgencyRepository, ContactRepository, SalesRepRepository, SalesTeamRepository, SalesOfficeRepository, SalesRegionRepository, ClusterRepository
  - [ ] 9.10 Create AdvertiserService interface in services package
  - [ ] 9.11 Create AdvertiserServiceImpl in services.impl package with CRUD operations
  - [ ] 9.12 Create AgencyService interface and AgencyServiceImpl
  - [ ] 9.13 Create SalesRepService interface and SalesRepServiceImpl
  - [ ] 9.14 Implement credit limit tracking in AdvertiserService
  - [ ] 9.15 Implement payment terms management
  - [ ] 9.16 Create AdvertiserRequestDTO, AdvertiserResponseDTO, AgencyRequestDTO, AgencyResponseDTO, SalesRepRequestDTO, SalesRepResponseDTO
  - [ ] 9.17 Create AdvertiserController with CRUD endpoints
  - [ ] 9.18 Create AgencyController with CRUD endpoints
  - [ ] 9.19 Create SalesRepController with CRUD endpoints
  - [ ] 9.20 Add OpenAPI/Swagger annotations to all controller endpoints
  - [ ] 9.21 Create Liquibase changesets for all new tables
  - [ ] 9.22 Write unit tests for all service implementations (80%+ coverage)
  - [ ] 9.23 Write integration tests for all controllers
  - [ ] 9.24 Write repository tests using @DataJpaTest

- [ ] 10.0 Phase 2.1: Implement Log Manager
  - [ ] 10.1 Create DailyLog entity with UUID primary key, Lombok annotations
  - [ ] 10.2 Create LogRevision entity for audit trail
  - [ ] 10.3 Create TrafficLog entity
  - [ ] 10.4 Create DailyLogRepository, LogRevisionRepository, TrafficLogRepository
  - [ ] 10.5 Create LogService interface in services package
  - [ ] 10.6 Create LogServiceImpl in services.impl package for daily log operations
  - [ ] 10.7 Create LogEditorService interface and LogEditorServiceImpl
  - [ ] 10.8 Create LogGeneratorService interface and LogGeneratorServiceImpl
  - [ ] 10.9 Implement real-time time remaining calculations in LogEditorService
  - [ ] 10.10 Create DailyLogRequestDTO, DailyLogResponseDTO, TrafficLogRequestDTO, TrafficLogResponseDTO
  - [ ] 10.11 Create LogController with visual log management endpoints
  - [ ] 10.12 Add OpenAPI/Swagger annotations to LogController endpoints
  - [ ] 10.13 Create Liquibase changesets for all new tables
  - [ ] 10.14 Write unit tests for LogService, LogEditorService, LogGeneratorService (80%+ coverage)
  - [ ] 10.15 Write integration tests for LogController
  - [ ] 10.16 Write repository tests using @DataJpaTest

- [ ] 11.0 Phase 2.2: Implement Placement Logic (The "Placer")
  - [ ] 11.1 Create Spot entity with UUID primary key, Lombok annotations, relationships to Order, DailyLog, BreakStructure
  - [ ] 11.2 Create SpotStatus enum in enums package with Javadoc
  - [ ] 11.3 Create SpotRepository
  - [ ] 11.4 Create SpotSchedulerService interface in services package
  - [ ] 11.5 Create SpotSchedulerServiceImpl in services.impl package
  - [ ] 11.6 Implement priority-based placement algorithm (Premium bumps Standby)
  - [ ] 11.7 Implement conflict validation (e.g., 5 minutes of ads in 3-minute break)
  - [ ] 11.8 Implement time slot optimization
  - [ ] 11.9 Configure async processing for large schedules using @Async
  - [ ] 11.10 Ensure all placement operations are transactional with rollback capability
  - [ ] 11.11 Create SpotRequestDTO, SpotResponseDTO
  - [ ] 11.12 Create SpotController with spot scheduling endpoints
  - [ ] 11.13 Add OpenAPI/Swagger annotations to SpotController endpoints
  - [ ] 11.14 Create Liquibase changeset for spots table
  - [ ] 11.15 Write unit tests for SpotSchedulerService including algorithm logic (80%+ coverage)
  - [ ] 11.16 Write integration tests for SpotController
  - [ ] 11.17 Write repository tests using @DataJpaTest

- [ ] 12.0 Phase 2.3: Implement Separation Engine
  - [ ] 12.1 Create RotationRule entity with UUID primary key, Lombok annotations
  - [ ] 12.2 Create ProductCategory entity for competitive separation
  - [ ] 12.3 Create SeparationRule entity
  - [ ] 12.4 Create RotationRuleRepository, ProductCategoryRepository, SeparationRuleRepository
  - [ ] 12.5 Create RotationRuleService interface in services package
  - [ ] 12.6 Create RotationRuleServiceImpl in services.impl package
  - [ ] 12.7 Implement high-performance category lookups using indexed database queries
  - [ ] 12.8 Implement separation validation during placement
  - [ ] 12.9 Implement separation validation during manual edits
  - [ ] 12.10 Create RotationRuleRequestDTO, RotationRuleResponseDTO, ProductCategoryRequestDTO, ProductCategoryResponseDTO
  - [ ] 12.11 Create RotationRuleController with CRUD endpoints
  - [ ] 12.12 Add OpenAPI/Swagger annotations to RotationRuleController endpoints
  - [ ] 12.13 Create Liquibase changesets for all new tables with proper indexes
  - [ ] 12.14 Write unit tests for RotationRuleService including separation logic (80%+ coverage)
  - [ ] 12.15 Write integration tests for RotationRuleController
  - [ ] 12.16 Write repository tests using @DataJpaTest

- [ ] 13.0 Phase 3.1: Implement Log Export
  - [ ] 13.1 Create ExportService interface in services package
  - [ ] 13.2 Create ExportServiceImpl in services.impl package
  - [ ] 13.3 Implement BXF XML format generation
  - [ ] 13.4 Implement flat-file format generation
  - [ ] 13.5 Implement scheduled export functionality using @Scheduled
  - [ ] 13.6 Create export history tracking (entity or audit log)
  - [ ] 13.7 Implement file validation before transmission
  - [ ] 13.8 Configure async export generation using @Async
  - [ ] 13.9 Create ExportRequestDTO, ExportResponseDTO
  - [ ] 13.10 Create ExportController with export endpoints
  - [ ] 13.11 Add OpenAPI/Swagger annotations to ExportController endpoints
  - [ ] 13.12 Create Liquibase changeset if needed for export tracking
  - [ ] 13.13 Write unit tests for ExportService including format generation (80%+ coverage)
  - [ ] 13.14 Write integration tests for ExportController

- [ ] 14.0 Phase 3.2: Implement As-Run Reconciliation
  - [ ] 14.1 Create PlaybackHistory entity with UUID primary key, Lombok annotations
  - [ ] 14.2 Create Makegood entity with UUID primary key
  - [ ] 14.3 Create MakegoodStatus enum in enums package with Javadoc
  - [ ] 14.4 Create PlaybackHistoryRepository, MakegoodRepository
  - [ ] 14.5 Create ReconciliationService interface in services package
  - [ ] 14.6 Create ReconciliationServiceImpl in services.impl package
  - [ ] 14.7 Implement as-run file import functionality
  - [ ] 14.8 Implement log ID to timestamp matching algorithm
  - [ ] 14.9 Implement discrepancy detection logic
  - [ ] 14.10 Implement makegood handling (spots that failed to air)
  - [ ] 14.11 Implement spot status updates based on reconciliation results
  - [ ] 14.12 Create ReconciliationRequestDTO, ReconciliationResponseDTO, MakegoodRequestDTO, MakegoodResponseDTO
  - [ ] 14.13 Create ReconciliationController with reconciliation endpoints
  - [ ] 14.14 Add OpenAPI/Swagger annotations to ReconciliationController endpoints
  - [ ] 14.15 Create Liquibase changesets for playback_history and makegoods tables
  - [ ] 14.16 Write unit tests for ReconciliationService including matching logic (80%+ coverage)
  - [ ] 14.17 Write integration tests for ReconciliationController
  - [ ] 14.18 Write repository tests using @DataJpaTest

- [ ] 15.0 Phase 3.3: Implement External System Integration
  - [ ] 15.1 Create LibreTimeClient class using Spring WebClient
  - [ ] 15.2 Implement LibreTime authentication and API methods
  - [ ] 15.3 Create AzuraCastClient class using Spring WebClient
  - [ ] 15.4 Implement AzuraCast authentication and API methods
  - [ ] 15.5 Create IntegrationService interface in services package
  - [ ] 15.6 Create IntegrationServiceImpl in services.impl package
  - [ ] 15.7 Implement webhook support for external events
  - [ ] 15.8 Configure retry logic and timeout configuration for external API calls
  - [ ] 15.9 Implement error handling and logging for integration failures
  - [ ] 15.10 Create IntegrationRequestDTO, IntegrationResponseDTO
  - [ ] 15.11 Create IntegrationController with integration endpoints
  - [ ] 15.12 Add OpenAPI/Swagger annotations to IntegrationController endpoints
  - [ ] 15.13 Write unit tests for LibreTimeClient and AzuraCastClient (80%+ coverage) using mocks
  - [ ] 15.14 Write integration tests for IntegrationController with mocked external services
  - [ ] 15.15 Write unit tests for IntegrationService (80%+ coverage)

- [ ] 16.0 Phase 4.1: Implement Invoicing System
  - [ ] 16.1 Create InvoiceStatus enum in enums package with Javadoc
  - [ ] 16.2 Create Invoice entity with UUID primary key, Lombok annotations, BigDecimal for amounts
  - [ ] 16.3 Create InvoiceLine entity with relationship to Invoice, BigDecimal for amounts
  - [ ] 16.4 Create InvoiceRepository, InvoiceLineRepository
  - [ ] 16.5 Create BillingService interface in services package
  - [ ] 16.6 Create BillingServiceImpl in services.impl package
  - [ ] 16.7 Implement invoice generation based only on confirmed airings (from reconciliation)
  - [ ] 16.8 Implement rate calculations based on contract terms and actual airings
  - [ ] 16.9 Implement invoice revisions and adjustments
  - [ ] 16.10 Implement PDF export for invoices
  - [ ] 16.11 Implement CSV export for invoices
  - [ ] 16.12 Ensure all currency calculations use BigDecimal for precision
  - [ ] 16.13 Create InvoiceRequestDTO, InvoiceResponseDTO, InvoiceLineRequestDTO, InvoiceLineResponseDTO
  - [ ] 16.14 Create InvoiceController with CRUD endpoints
  - [ ] 16.15 Add OpenAPI/Swagger annotations to InvoiceController endpoints
  - [ ] 16.16 Create Liquibase changesets for invoices and invoice_lines tables
  - [ ] 16.17 Write unit tests for BillingService including calculation logic (80%+ coverage)
  - [ ] 16.18 Write integration tests for InvoiceController
  - [ ] 16.19 Write repository tests using @DataJpaTest

- [ ] 17.0 Phase 4.2: Implement Affidavits
  - [ ] 17.1 Create Affidavit entity with UUID primary key, Lombok annotations (or extend existing audit structure)
  - [ ] 17.2 Create AffidavitRepository
  - [ ] 17.3 Create AffidavitService interface in services package
  - [ ] 17.4 Create AffidavitServiceImpl in services.impl package
  - [ ] 17.5 Implement legal affidavit generation with timestamp, station, spot details
  - [ ] 17.6 Implement affidavit revisions for corrections
  - [ ] 17.7 Create AffidavitRequestDTO, AffidavitResponseDTO
  - [ ] 17.8 Create AffidavitController with CRUD endpoints
  - [ ] 17.9 Add OpenAPI/Swagger annotations to AffidavitController endpoints
  - [ ] 17.10 Create Liquibase changeset for affidavits table if separate entity
  - [ ] 17.11 Write unit tests for AffidavitService (80%+ coverage)
  - [ ] 17.12 Write integration tests for AffidavitController
  - [ ] 17.13 Write repository tests using @DataJpaTest

- [ ] 18.0 Phase 4.3: Implement Accounts Receivable (A/R)
  - [ ] 18.1 Create PaymentStatus enum in enums package with Javadoc
  - [ ] 18.2 Create Payment entity with UUID primary key, Lombok annotations, BigDecimal for amounts
  - [ ] 18.3 Create PaymentRepository
  - [ ] 18.4 Create PaymentService interface in services package
  - [ ] 18.5 Create PaymentServiceImpl in services.impl package
  - [ ] 18.6 Implement payment tracking and recording
  - [ ] 18.7 Implement aging reports calculation
  - [ ] 18.8 Implement partial payment support
  - [ ] 18.9 Implement payment allocation to invoices
  - [ ] 18.10 Ensure all currency calculations use BigDecimal for precision
  - [ ] 18.11 Create PaymentRequestDTO, PaymentResponseDTO
  - [ ] 18.12 Create PaymentController with CRUD endpoints
  - [ ] 18.13 Add OpenAPI/Swagger annotations to PaymentController endpoints
  - [ ] 18.14 Create Liquibase changeset for payments table
  - [ ] 18.15 Write unit tests for PaymentService including aging calculations (80%+ coverage)
  - [ ] 18.16 Write integration tests for PaymentController
  - [ ] 18.17 Write repository tests using @DataJpaTest

- [ ] 19.0 Phase 4.4: Implement Revenue Analytics
  - [ ] 19.1 Create Revenue entity with UUID primary key, Lombok annotations, BigDecimal for amounts
  - [ ] 19.2 Create SalesGoal entity with UUID primary key, Lombok annotations
  - [ ] 19.3 Create RevenueRepository, SalesGoalRepository
  - [ ] 19.4 Create RevenueService interface in services package
  - [ ] 19.5 Create RevenueServiceImpl in services.impl package
  - [ ] 19.6 Implement revenue tracking by station, sales rep, advertiser
  - [ ] 19.7 Implement sales goal comparison logic
  - [ ] 19.8 Implement inventory utilization reports
  - [ ] 19.9 Implement revenue forecasting calculations
  - [ ] 19.10 Optimize analytics queries for large datasets (proper indexes, query optimization)
  - [ ] 19.11 Create ReportService interface and ReportServiceImpl
  - [ ] 19.12 Create RevenueRequestDTO, RevenueResponseDTO, SalesGoalRequestDTO, SalesGoalResponseDTO, ReportRequestDTO, ReportResponseDTO
  - [ ] 19.13 Create RevenueController with revenue tracking endpoints
  - [ ] 19.14 Create ReportController with reporting endpoints
  - [ ] 19.15 Add OpenAPI/Swagger annotations to all controller endpoints
  - [ ] 19.16 Create Liquibase changesets for revenue and sales_goals tables
  - [ ] 19.17 Write unit tests for RevenueService and ReportService (80%+ coverage)
  - [ ] 19.18 Write integration tests for RevenueController and ReportController
  - [ ] 19.19 Write repository tests using @DataJpaTest

