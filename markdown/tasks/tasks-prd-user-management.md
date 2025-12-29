# Task List: Enterprise User Management System

Based on PRD: `prd-user-management.md`

## Relevant Files

### Database Migrations
- `librelog-api/src/main/resources/db/changelog/013-create-user-station-assignments-table.xml` - Create user_station_assignments table for property-based access control
- `librelog-api/src/main/resources/db/changelog/014-create-custom-roles-table.xml` - Create custom_roles table for user-defined roles
- `librelog-api/src/main/resources/db/changelog/015-create-audit-logs-table.xml` - Create audit_logs table for comprehensive action logging
- `librelog-api/src/main/resources/db/changelog/016-create-user-sessions-table.xml` - Create user_sessions table for session management
- `librelog-api/src/main/resources/db/changelog/017-create-user-market-assignments-table.xml` - Create user_market_assignments table (Phase 2 preparation)
- `librelog-api/src/main/resources/db/changelog/018-create-user-cluster-assignments-table.xml` - Create user_cluster_assignments table (Phase 2 preparation)
- `librelog-api/src/main/resources/db/changelog/db.changelog-master.xml` - Master changelog (update to include new changesets)

### Backend - Enums
- `librelog-api/src/main/java/com/onelpro/librelog/enums/PermissionLevel.java` - Enum for permission levels (FULL_ACCESS, VIEW_ONLY, CUSTOM)
- `librelog-api/src/main/java/com/onelpro/librelog/enums/ActionType.java` - Enum for action-level permissions (VIEW, CREATE, EDIT, DELETE)
- `librelog-api/src/main/java/com/onelpro/librelog/enums/ModuleType.java` - Enum for module-level permissions (ORDERS, LOGS, INVENTORY, BILLING, REPORTS, MATERIAL_INSTRUCTIONS, CLOCK_TEMPLATES, USER_MANAGEMENT, SYSTEM_SETTINGS)
- `librelog-api/src/main/java/com/onelpro/librelog/enums/AuditActionType.java` - Enum for audit log action types (CREATE, UPDATE, DELETE, LOGIN, LOGOUT, PERMISSION_CHANGE, STATION_ASSIGNMENT, ROLE_ASSIGNMENT, IMPERSONATION_START, IMPERSONATION_END)
- `librelog-api/src/main/java/com/onelpro/librelog/enums/ResourceType.java` - Enum for resource types in audit logs (ORDER, LOG, USER, PERMISSION, STATION, ROLE, etc.)

### Backend - Models/Entities
- `librelog-api/src/main/java/com/onelpro/librelog/models/UserStationAssignment.java` - Entity for user-station assignments with permission levels
- `librelog-api/src/main/java/com/onelpro/librelog/models/CustomRole.java` - Entity for custom roles with JSON permissions storage
- `librelog-api/src/main/java/com/onelpro/librelog/models/AuditLog.java` - Entity for audit trail entries
- `librelog-api/src/main/java/com/onelpro/librelog/models/UserSession.java` - Entity for tracking active user sessions
- `librelog-api/src/main/java/com/onelpro/librelog/models/UserMarketAssignment.java` - Entity for user-market assignments (Phase 2)
- `librelog-api/src/main/java/com/onelpro/librelog/models/UserClusterAssignment.java` - Entity for user-cluster assignments (Phase 2)
- `librelog-api/src/main/java/com/onelpro/librelog/models/User.java` - Existing user entity (needs enhancement for relationships)

### Backend - Repositories
- `librelog-api/src/main/java/com/onelpro/librelog/repositories/UserStationAssignmentRepository.java` - Repository for user-station assignments
- `librelog-api/src/main/java/com/onelpro/librelog/repositories/CustomRoleRepository.java` - Repository for custom roles
- `librelog-api/src/main/java/com/onelpro/librelog/repositories/AuditLogRepository.java` - Repository for audit logs with custom query methods
- `librelog-api/src/main/java/com/onelpro/librelog/repositories/UserSessionRepository.java` - Repository for user sessions
- `librelog-api/src/main/java/com/onelpro/librelog/repositories/UserMarketAssignmentRepository.java` - Repository for user-market assignments (Phase 2)
- `librelog-api/src/main/java/com/onelpro/librelog/repositories/UserClusterAssignmentRepository.java` - Repository for user-cluster assignments (Phase 2)
- `librelog-api/src/main/java/com/onelpro/librelog/repositories/UserRepository.java` - Existing repository (needs enhancement)

### Backend - DTOs
- `librelog-api/src/main/java/com/onelpro/librelog/dto/UserStationAssignmentRequestDTO.java` - Request DTO for assigning users to stations
- `librelog-api/src/main/java/com/onelpro/librelog/dto/UserStationAssignmentResponseDTO.java` - Response DTO for user-station assignments
- `librelog-api/src/main/java/com/onelpro/librelog/dto/CustomRoleRequestDTO.java` - Request DTO for creating/updating custom roles
- `librelog-api/src/main/java/com/onelpro/librelog/dto/CustomRoleResponseDTO.java` - Response DTO for custom roles
- `librelog-api/src/main/java/com/onelpro/librelog/dto/AuditLogResponseDTO.java` - Response DTO for audit log entries
- `librelog-api/src/main/java/com/onelpro/librelog/dto/AuditLogFilterDTO.java` - Filter DTO for audit log queries
- `librelog-api/src/main/java/com/onelpro/librelog/dto/UserSessionResponseDTO.java` - Response DTO for user sessions
- `librelog-api/src/main/java/com/onelpro/librelog/dto/ImpersonationRequestDTO.java` - Request DTO for starting impersonation
- `librelog-api/src/main/java/com/onelpro/librelog/dto/BulkUserImportRequestDTO.java` - Request DTO for bulk user import
- `librelog-api/src/main/java/com/onelpro/librelog/dto/BulkUserImportResponseDTO.java` - Response DTO for bulk import results
- `librelog-api/src/main/java/com/onelpro/librelog/dto/PermissionCheckDTO.java` - DTO for permission checking
- `librelog-api/src/main/java/com/onelpro/librelog/dto/UserDetailResponseDTO.java` - Enhanced user response with station assignments and sessions
- `librelog-api/src/main/java/com/onelpro/librelog/dto/UserRequestDTO.java` - Existing DTO (needs enhancement)
- `librelog-api/src/main/java/com/onelpro/librelog/dto/UserResponseDTO.java` - Existing DTO (needs enhancement)

### Backend - Services
- `librelog-api/src/main/java/com/onelpro/librelog/services/PermissionService.java` - Service interface for permission checking
- `librelog-api/src/main/java/com/onelpro/librelog/services/impl/PermissionServiceImpl.java` - Permission service implementation
- `librelog-api/src/main/java/com/onelpro/librelog/services/AuditService.java` - Service interface for audit logging
- `librelog-api/src/main/java/com/onelpro/librelog/services/impl/AuditServiceImpl.java` - Audit service implementation
- `librelog-api/src/main/java/com/onelpro/librelog/services/SessionService.java` - Service interface for session management
- `librelog-api/src/main/java/com/onelpro/librelog/services/impl/SessionServiceImpl.java` - Session service implementation
- `librelog-api/src/main/java/com/onelpro/librelog/services/UserStationAssignmentService.java` - Service interface for user-station assignments
- `librelog-api/src/main/java/com/onelpro/librelog/services/impl/UserStationAssignmentServiceImpl.java` - User-station assignment service implementation
- `librelog-api/src/main/java/com/onelpro/librelog/services/CustomRoleService.java` - Service interface for custom roles
- `librelog-api/src/main/java/com/onelpro/librelog/services/impl/CustomRoleServiceImpl.java` - Custom role service implementation
- `librelog-api/src/main/java/com/onelpro/librelog/services/ImpersonationService.java` - Service interface for impersonation
- `librelog-api/src/main/java/com/onelpro/librelog/services/impl/ImpersonationServiceImpl.java` - Impersonation service implementation
- `librelog-api/src/main/java/com/onelpro/librelog/services/BulkUserImportService.java` - Service interface for bulk user import
- `librelog-api/src/main/java/com/onelpro/librelog/services/impl/BulkUserImportServiceImpl.java` - Bulk user import service implementation
- `librelog-api/src/main/java/com/onelpro/librelog/services/UserService.java` - Existing service (needs enhancement)
- `librelog-api/src/main/java/com/onelpro/librelog/services/impl/UserServiceImpl.java` - Existing service implementation (needs enhancement)

### Backend - Controllers
- `librelog-api/src/main/java/com/onelpro/librelog/controllers/UserStationAssignmentController.java` - REST controller for user-station assignments
- `librelog-api/src/main/java/com/onelpro/librelog/controllers/CustomRoleController.java` - REST controller for custom roles
- `librelog-api/src/main/java/com/onelpro/librelog/controllers/AuditLogController.java` - REST controller for audit logs
- `librelog-api/src/main/java/com/onelpro/librelog/controllers/SessionController.java` - REST controller for session management
- `librelog-api/src/main/java/com/onelpro/librelog/controllers/ImpersonationController.java` - REST controller for impersonation
- `librelog-api/src/main/java/com/onelpro/librelog/controllers/BulkUserImportController.java` - REST controller for bulk user import
- `librelog-api/src/main/java/com/onelpro/librelog/controllers/UserController.java` - Existing controller (needs enhancement)

### Backend - Security and Configuration
- `librelog-api/src/main/java/com/onelpro/librelog/config/SecurityConfig.java` - Existing security config (needs enhancement for permission-based access)
- `librelog-api/src/main/java/com/onelpro/librelog/config/PermissionAspect.java` - Aspect for automatic permission checking
- `librelog-api/src/main/java/com/onelpro/librelog/utils/PermissionChecker.java` - Utility class for permission validation
- `librelog-api/src/main/java/com/onelpro/librelog/utils/StationFilter.java` - Utility for filtering data by user's station assignments

### Frontend - UI Components
- `librelog-api/src/main/resources/static/user-management.html` - Main user management page
- `librelog-api/src/main/resources/static/css/user-management.css` - Styles for user management UI
- `librelog-api/src/main/resources/static/js/user-management.js` - JavaScript for user management functionality
- `librelog-api/src/main/resources/static/js/role-builder.js` - JavaScript for custom role builder UI
- `librelog-api/src/main/resources/static/js/audit-log-viewer.js` - JavaScript for audit log viewer
- `librelog-api/src/main/resources/static/js/session-dashboard.js` - JavaScript for session management dashboard
- `librelog-api/src/main/resources/static/js/bulk-import.js` - JavaScript for bulk user import UI

### Tests
- `librelog-api/src/test/java/com/onelpro/librelog/enums/PermissionLevelTest.java` - Permission level enum tests
- `librelog-api/src/test/java/com/onelpro/librelog/enums/ActionTypeTest.java` - Action type enum tests
- `librelog-api/src/test/java/com/onelpro/librelog/enums/ModuleTypeTest.java` - Module type enum tests
- `librelog-api/src/test/java/com/onelpro/librelog/enums/AuditActionTypeTest.java` - Audit action type enum tests
- `librelog-api/src/test/java/com/onelpro/librelog/models/UserStationAssignmentTest.java` - User station assignment entity tests
- `librelog-api/src/test/java/com/onelpro/librelog/models/CustomRoleTest.java` - Custom role entity tests
- `librelog-api/src/test/java/com/onelpro/librelog/models/AuditLogTest.java` - Audit log entity tests
- `librelog-api/src/test/java/com/onelpro/librelog/models/UserSessionTest.java` - User session entity tests
- `librelog-api/src/test/java/com/onelpro/librelog/services/impl/PermissionServiceImplTest.java` - Permission service tests
- `librelog-api/src/test/java/com/onelpro/librelog/services/impl/AuditServiceImplTest.java` - Audit service tests
- `librelog-api/src/test/java/com/onelpro/librelog/services/impl/SessionServiceImplTest.java` - Session service tests
- `librelog-api/src/test/java/com/onelpro/librelog/services/impl/UserStationAssignmentServiceImplTest.java` - User station assignment service tests
- `librelog-api/src/test/java/com/onelpro/librelog/services/impl/CustomRoleServiceImplTest.java` - Custom role service tests
- `librelog-api/src/test/java/com/onelpro/librelog/services/impl/ImpersonationServiceImplTest.java` - Impersonation service tests
- `librelog-api/src/test/java/com/onelpro/librelog/services/impl/BulkUserImportServiceImplTest.java` - Bulk user import service tests
- `librelog-api/src/test/java/com/onelpro/librelog/controllers/UserStationAssignmentControllerTest.java` - User station assignment controller tests
- `librelog-api/src/test/java/com/onelpro/librelog/controllers/CustomRoleControllerTest.java` - Custom role controller tests
- `librelog-api/src/test/java/com/onelpro/librelog/controllers/AuditLogControllerTest.java` - Audit log controller tests
- `librelog-api/src/test/java/com/onelpro/librelog/controllers/SessionControllerTest.java` - Session controller tests
- `librelog-api/src/test/java/com/onelpro/librelog/controllers/ImpersonationControllerTest.java` - Impersonation controller tests
- `librelog-api/src/test/java/com/onelpro/librelog/controllers/BulkUserImportControllerTest.java` - Bulk user import controller tests
- `librelog-api/src/test/java/com/onelpro/librelog/integration/UserManagementIT.java` - Integration tests for user management workflows

### Notes

- Unit tests should be placed alongside the code files they are testing
- Use `mvn test` to run all tests
- Use `mvn jacoco:report` to generate coverage reports
- All new code must meet 80% minimum coverage requirement
- Follow Spring Boot best practices: constructor injection, DTOs for API, UUID primary keys
- All web pages must be mobile responsive and meet WCAG 2.1 Level AA accessibility standards
- Permission checks must be enforced at the service layer, not just the controller layer
- Audit logs must be immutable and stored in a separate table with restricted access
- All database changes must use Liquibase changesets (never modify existing changesets)
- Service interfaces must be in `services` package, implementations in `services.impl` package
- All DTOs must end with "DTO" suffix
- All enums must be in the `enums` package with their own class files

## Tasks

- [x] 1.0 Database Schema and Migrations
  - [x] 1.1 Create Liquibase changeset `013-create-user-station-assignments-table.xml` to create `user_station_assignments` table with columns: id (UUID PK), user_id (UUID FK to users), station_id (UUID FK to stations), permission_level (VARCHAR enum), custom_permissions (JSONB), created_at (TIMESTAMP), updated_at (TIMESTAMP), with unique constraint on (user_id, station_id)
  - [x] 1.2 Create Liquibase changeset `014-create-custom-roles-table.xml` to create `custom_roles` table with columns: id (UUID PK), name (VARCHAR UNIQUE), description (TEXT), permissions (JSONB), created_by_user_id (UUID FK to users), created_at (TIMESTAMP), updated_at (TIMESTAMP)
  - [x] 1.3 Create Liquibase changeset `015-create-audit-logs-table.xml` to create `audit_logs` table with columns: id (UUID PK), user_id (UUID FK to users, nullable), impersonated_user_id (UUID FK to users, nullable), action_type (VARCHAR enum), resource_type (VARCHAR), resource_id (UUID, nullable), previous_value (JSONB, nullable), new_value (JSONB, nullable), ip_address (VARCHAR), user_agent (VARCHAR), station_id (UUID FK to stations, nullable), timestamp (TIMESTAMP WITH TIME ZONE)
  - [x] 1.4 Create Liquibase changeset `016-create-user-sessions-table.xml` to create `user_sessions` table with columns: id (UUID PK), user_id (UUID FK to users), session_token (VARCHAR hashed), login_timestamp (TIMESTAMP), last_activity_timestamp (TIMESTAMP), ip_address (VARCHAR), user_agent (VARCHAR), current_station_id (UUID FK to stations, nullable), current_resource_id (UUID, nullable), is_active (BOOLEAN), expires_at (TIMESTAMP)
  - [x] 1.5 Create Liquibase changeset `017-create-user-market-assignments-table.xml` to create `user_market_assignments` table (Phase 2 preparation) with columns: id (UUID PK), user_id (UUID FK to users), market_id (UUID FK to markets), permission_level (VARCHAR enum), custom_permissions (JSONB), created_at (TIMESTAMP), updated_at (TIMESTAMP), with unique constraint on (user_id, market_id)
  - [x] 1.6 Create Liquibase changeset `018-create-user-cluster-assignments-table.xml` to create `user_cluster_assignments` table (Phase 2 preparation) with columns: id (UUID PK), user_id (UUID FK to users), cluster_id (UUID FK to clusters), permission_level (VARCHAR enum), custom_permissions (JSONB), created_at (TIMESTAMP), updated_at (TIMESTAMP), with unique constraint on (user_id, cluster_id)
  - [x] 1.7 Add appropriate indexes on foreign keys and frequently queried columns (user_id, station_id, timestamp, action_type, resource_type) for performance optimization
  - [x] 1.8 Update `db.changelog-master.xml` to include all new changesets (013-018) in order
  - [x] 1.9 Verify all changesets are idempotent and can be run multiple times safely
  - [x] 1.10 Test database migrations on a clean database to ensure they execute correctly

- [x] 2.0 Enums and Constants
  - [x] 2.1 Create `PermissionLevel` enum in `enums` package with values: FULL_ACCESS, VIEW_ONLY, CUSTOM, with Javadoc comments
  - [x] 2.2 Create `ActionType` enum in `enums` package with values: VIEW, CREATE, EDIT, DELETE, with Javadoc comments
  - [x] 2.3 Create `ModuleType` enum in `enums` package with values: ORDERS, LOGS, INVENTORY, BILLING, REPORTS, MATERIAL_INSTRUCTIONS, CLOCK_TEMPLATES, USER_MANAGEMENT, SYSTEM_SETTINGS, with Javadoc comments
  - [x] 2.4 Create `AuditActionType` enum in `enums` package with values: CREATE, UPDATE, DELETE, LOGIN, LOGOUT, PERMISSION_CHANGE, STATION_ASSIGNMENT, ROLE_ASSIGNMENT, IMPERSONATION_START, IMPERSONATION_END, SESSION_TERMINATED, with Javadoc comments
  - [x] 2.5 Create `ResourceType` enum in `enums` package with values: ORDER, LOG, USER, PERMISSION, STATION, ROLE, CUSTOM_ROLE, SESSION, with Javadoc comments
  - [x] 2.6 Write unit tests for all enum classes verifying values() and valueOf() methods work correctly (80%+ coverage)
  - [x] 2.7 Verify all enums follow naming conventions (PascalCase for enum name, UPPER_SNAKE_CASE for constants)

- [x] 3.0 Entity Models
  - [x] 3.1 Create `UserStationAssignment` entity in `models` package with JPA annotations, UUID primary key, Lombok annotations (@Data, @Builder, @NoArgsConstructor, @AllArgsConstructor), relationships to User and Station, fields for permission_level (enum), custom_permissions (JSONB), and timestamps
  - [x] 3.2 Create `CustomRole` entity in `models` package with JPA annotations, UUID primary key, Lombok annotations, fields for name (unique), description, permissions (JSONB), created_by_user_id (FK), and timestamps
  - [x] 3.3 Create `AuditLog` entity in `models` package with JPA annotations, UUID primary key, Lombok annotations, relationships to User (user_id and impersonated_user_id, both nullable), Station (nullable), fields for action_type (enum), resource_type, resource_id (UUID nullable), previous_value (JSONB nullable), new_value (JSONB nullable), ip_address, user_agent, and timestamp
  - [x] 3.4 Create `UserSession` entity in `models` package with JPA annotations, UUID primary key, Lombok annotations, relationship to User, fields for session_token (hashed), login_timestamp, last_activity_timestamp, ip_address, user_agent, current_station_id (FK nullable), current_resource_id (UUID nullable), is_active (boolean), expires_at
  - [x] 3.5 Create `UserMarketAssignment` entity in `models` package (Phase 2 preparation) with similar structure to UserStationAssignment but for markets
  - [x] 3.6 Create `UserClusterAssignment` entity in `models` package (Phase 2 preparation) with similar structure to UserStationAssignment but for clusters
  - [x] 3.7 Update `User` entity to add @OneToMany relationships to UserStationAssignment, UserSession, and audit logs (as creator)
  - [x] 3.8 Verify all entities use `@Enumerated(EnumType.STRING)` for enum fields
  - [x] 3.9 Verify all entities use UUID for primary keys with `@GeneratedValue(strategy = GenerationType.UUID)`
  - [x] 3.10 Write unit tests for entity classes verifying Lombok annotations work correctly

- [x] 4.0 Repository Interfaces
  - [x] 4.1 Create `UserStationAssignmentRepository` interface extending JpaRepository with custom query methods: findByUserId, findByStationId, findByUserIdAndStationId, findByUserIdIn
  - [x] 4.2 Create `CustomRoleRepository` interface extending JpaRepository with custom query methods: findByName, findByCreatedByUserId, existsByName
  - [x] 4.3 Create `AuditLogRepository` interface extending JpaRepository with custom query methods: findByUserId, findByActionType, findByResourceType, findByTimestampBetween, findByStationId, with pagination support
  - [x] 4.4 Create `UserSessionRepository` interface extending JpaRepository with custom query methods: findByUserId, findByIsActiveTrue, findByUserIdAndIsActiveTrue, findByExpiresAtBefore, deleteByExpiresAtBefore
  - [x] 4.5 Create `UserMarketAssignmentRepository` interface (Phase 2 preparation) with similar methods to UserStationAssignmentRepository
  - [x] 4.6 Create `UserClusterAssignmentRepository` interface (Phase 2 preparation) with similar methods to UserStationAssignmentRepository
  - [x] 4.7 Enhance `UserRepository` with additional query methods if needed: findByEmail, existsByEmail (verify these exist)
  - [x] 4.8 Write repository tests using @DataJpaTest for all new repositories

- [x] 5.0 DTOs (Data Transfer Objects)
  - [x] 5.1 Create `UserStationAssignmentRequestDTO` with fields: userId (UUID), stationId (UUID), permissionLevel (enum), customPermissions (Map<String, Boolean> or JSON)
  - [x] 5.2 Create `UserStationAssignmentResponseDTO` with fields: id, userId, userEmail, stationId, stationName, permissionLevel, customPermissions, createdAt, updatedAt
  - [x] 5.3 Create `CustomRoleRequestDTO` with fields: name (String), description (String), permissions (Map<ModuleType, Set<ActionType>> or JSON)
  - [x] 5.4 Create `CustomRoleResponseDTO` with fields: id, name, description, permissions, createdByUserId, createdByUserEmail, createdAt, updatedAt, assignedUserCount
  - [x] 5.5 Create `AuditLogResponseDTO` with fields: id, userId, userEmail, impersonatedUserId, impersonatedUserEmail, actionType, resourceType, resourceId, previousValue, newValue, ipAddress, userAgent, stationId, stationName, timestamp
  - [x] 5.6 Create `AuditLogFilterDTO` with fields: userId (UUID optional), actionType (enum optional), resourceType (enum optional), stationId (UUID optional), startDate (LocalDateTime optional), endDate (LocalDateTime optional), page (int), size (int)
  - [x] 5.7 Create `UserSessionResponseDTO` with fields: id, userId, userEmail, loginTimestamp, lastActivityTimestamp, ipAddress, userAgent, currentStationId, currentStationName, currentResourceId, isActive, expiresAt, sessionDuration
  - [x] 5.8 Create `ImpersonationRequestDTO` with fields: targetUserId (UUID)
  - [x] 5.9 Create `BulkUserImportRequestDTO` with fields: file (MultipartFile), validateOnly (boolean optional)
  - [x] 5.10 Create `BulkUserImportResponseDTO` with fields: totalRecords, successfulCount, failedCount, errors (List<ImportError>), importedUsers (List<UserResponseDTO>)
  - [x] 5.11 Create `PermissionCheckDTO` with fields: userId (UUID), stationId (UUID optional), moduleType (enum), actionType (enum), result (boolean)
  - [x] 5.12 Create `UserDetailResponseDTO` extending UserResponseDTO with additional fields: stationAssignments (List<UserStationAssignmentResponseDTO>), activeSessions (List<UserSessionResponseDTO>), recentAuditLogs (List<AuditLogResponseDTO>)
  - [x] 5.13 Enhance `UserRequestDTO` to optionally include station assignments
  - [x] 5.14 Enhance `UserResponseDTO` to optionally include station assignments summary
  - [x] 5.15 Verify all DTOs use Lombok annotations (@Data, @Builder, @NoArgsConstructor, @AllArgsConstructor) and end with "DTO" suffix
  - [x] 5.16 Add validation annotations (@NotNull, @NotBlank, @Valid) where appropriate

- [ ] 6.0 Permission System Core
  - [ ] 6.1 Create `PermissionService` interface in `services` package with methods: hasPermission(userId, stationId, moduleType, actionType), getUserStations(userId), canAccessStation(userId, stationId), getEffectivePermissions(userId, stationId)
  - [ ] 6.2 Create `PermissionServiceImpl` in `services.impl` package implementing the interface with constructor injection, caching for permission lookups, and proper error handling
  - [ ] 6.3 Implement `hasPermission` method that checks user-station assignments, role permissions, and custom permissions
  - [ ] 6.4 Implement `getUserStations` method that returns all stations a user has access to
  - [ ] 6.5 Implement `canAccessStation` method that verifies if a user can access a specific station
  - [ ] 6.6 Implement `getEffectivePermissions` method that returns the effective permissions for a user at a station (considering role, custom role, and station-specific overrides)
  - [ ] 6.7 Create `PermissionChecker` utility class with static methods for common permission checks
  - [ ] 6.8 Create `StationFilter` utility class for filtering collections by user's station assignments
  - [ ] 6.9 Implement permission caching strategy (5-minute cache with invalidation on permission changes)
  - [ ] 6.10 Create `PermissionAspect` aspect class for automatic permission checking on service methods (optional, for future use)
  - [ ] 6.11 Write comprehensive unit tests for `PermissionServiceImpl` covering all permission scenarios (80%+ coverage)
  - [ ] 6.12 Write integration tests for permission enforcement across different user-station combinations

- [ ] 7.0 Audit Trail System
  - [ ] 7.1 Create `AuditService` interface in `services` package with methods: logAction(actionType, resourceType, resourceId, previousValue, newValue, userId, stationId), logLogin(userId, ipAddress, userAgent), logLogout(userId), logPermissionChange(userId, targetUserId, previousPermissions, newPermissions), getAuditLogs(filter), exportAuditLogs(filter)
  - [ ] 7.2 Create `AuditServiceImpl` in `services.impl` package implementing the interface with constructor injection, transaction management, and proper error handling
  - [ ] 7.3 Implement `logAction` method that creates audit log entries with all required fields
  - [ ] 7.4 Implement `logLogin` and `logLogout` methods for authentication events
  - [ ] 7.5 Implement `logPermissionChange` method for tracking permission modifications
  - [ ] 7.6 Implement `getAuditLogs` method with filtering, pagination, and sorting support
  - [ ] 7.7 Implement `exportAuditLogs` method that generates CSV/Excel export
  - [ ] 7.8 Create audit logging interceptor/aspect to automatically log data changes in service methods (optional, for future use)
  - [ ] 7.9 Integrate audit logging into existing services (UserService, OrderService, etc.) for CREATE, UPDATE, DELETE operations
  - [ ] 7.10 Implement broadcast-specific audit logging for spot moves, log edits, and order approvals
  - [ ] 7.11 Write comprehensive unit tests for `AuditServiceImpl` (80%+ coverage)
  - [ ] 7.12 Write integration tests for audit logging across different scenarios

- [ ] 8.0 Session Management
  - [ ] 8.1 Create `SessionService` interface in `services` package with methods: createSession(userId, ipAddress, userAgent), updateLastActivity(sessionId), terminateSession(sessionId), terminateAllUserSessions(userId), getActiveSessions(), getUserSessions(userId), updateCurrentResource(sessionId, stationId, resourceId), getSession(sessionId)
  - [ ] 8.2 Create `SessionServiceImpl` in `services.impl` package implementing the interface with constructor injection, session token generation/hashing, and proper error handling
  - [ ] 8.3 Implement `createSession` method that creates a new session with hashed token and expiration
  - [ ] 8.4 Implement `updateLastActivity` method that updates the last activity timestamp
  - [ ] 8.5 Implement `terminateSession` and `terminateAllUserSessions` methods
  - [ ] 8.6 Implement `getActiveSessions` method that returns all currently active sessions
  - [ ] 8.7 Implement `getUserSessions` method that returns all sessions for a user
  - [ ] 8.8 Implement `updateCurrentResource` method for tracking which station/log a user is editing
  - [ ] 8.9 Create scheduled task to clean up expired sessions (run every hour)
  - [ ] 8.10 Integrate session management into authentication flow (create session on login, terminate on logout)
  - [ ] 8.11 Implement session timeout logic (30 minutes of inactivity = automatic expiration)
  - [ ] 8.12 Write comprehensive unit tests for `SessionServiceImpl` (80%+ coverage)
  - [ ] 8.13 Write integration tests for session management workflows

- [ ] 9.0 Service Layer Implementation
  - [ ] 9.1 Create `UserStationAssignmentService` interface in `services` package with methods: assignUserToStation(request), removeUserFromStation(userId, stationId), getUserStationAssignments(userId), getStationUserAssignments(stationId), updatePermissionLevel(userId, stationId, permissionLevel, customPermissions)
  - [ ] 9.2 Create `UserStationAssignmentServiceImpl` in `services.impl` package implementing the interface with constructor injection, transaction management, audit logging, and proper error handling
  - [ ] 9.3 Create `CustomRoleService` interface in `services` package with methods: createRole(request), updateRole(roleId, request), deleteRole(roleId), getRoleById(roleId), getAllRoles(), cloneRole(roleId, newName), getRolesAssignedToUser(userId)
  - [ ] 9.4 Create `CustomRoleServiceImpl` in `services.impl` package implementing the interface with constructor injection, transaction management, validation (prevent deletion if assigned to users), audit logging, and proper error handling
  - [ ] 9.5 Create `ImpersonationService` interface in `services` package with methods: startImpersonation(adminUserId, targetUserId), stopImpersonation(adminUserId), isImpersonating(userId), getImpersonatedUser(userId)
  - [ ] 9.6 Create `ImpersonationServiceImpl` in `services.impl` package implementing the interface with constructor injection, validation (prevent impersonating higher-level users), audit logging, and proper error handling
  - [ ] 9.7 Create `BulkUserImportService` interface in `services` package with methods: importUsers(file, validateOnly), validateUserData(rows), generateImportTemplate()
  - [ ] 9.8 Create `BulkUserImportServiceImpl` in `services.impl` package implementing the interface with constructor injection, CSV/Excel parsing, validation, partial import support, and detailed error reporting
  - [ ] 9.9 Enhance `UserService` interface with methods: getUserDetail(userId), getUserWithAssignments(userId), updateUserWithStations(userId, request, stationAssignments)
  - [ ] 9.10 Enhance `UserServiceImpl` to integrate with permission system, audit logging, and station assignments
  - [ ] 9.11 Integrate permission checks into all service methods that access station-specific data
  - [ ] 9.12 Integrate audit logging into all service methods that modify data
  - [ ] 9.13 Write comprehensive unit tests for all new service implementations (80%+ coverage)
  - [ ] 9.14 Write integration tests for service workflows

- [ ] 10.0 REST API Controllers
  - [ ] 10.1 Create `UserStationAssignmentController` in `controllers` package with REST endpoints: POST /api/user-station-assignments (assign user to station), DELETE /api/user-station-assignments/{id} (remove assignment), GET /api/user-station-assignments/user/{userId} (get user's assignments), GET /api/user-station-assignments/station/{stationId} (get station's users), PUT /api/user-station-assignments/{id} (update permission level)
  - [ ] 10.2 Create `CustomRoleController` in `controllers` package with REST endpoints: POST /api/custom-roles (create role), GET /api/custom-roles/{id} (get role), GET /api/custom-roles (get all roles), PUT /api/custom-roles/{id} (update role), DELETE /api/custom-roles/{id} (delete role), POST /api/custom-roles/{id}/clone (clone role)
  - [ ] 10.3 Create `AuditLogController` in `controllers` package with REST endpoints: GET /api/audit-logs (get audit logs with filtering), GET /api/audit-logs/{id} (get audit log by ID), GET /api/audit-logs/export (export audit logs to CSV/Excel)
  - [ ] 10.4 Create `SessionController` in `controllers` package with REST endpoints: GET /api/sessions (get all active sessions), GET /api/sessions/user/{userId} (get user's sessions), DELETE /api/sessions/{id} (terminate session), DELETE /api/sessions/user/{userId} (terminate all user's sessions), PUT /api/sessions/{id}/resource (update current resource)
  - [ ] 10.5 Create `ImpersonationController` in `controllers` package with REST endpoints: POST /api/impersonation/start (start impersonation), POST /api/impersonation/stop (stop impersonation), GET /api/impersonation/status (check if impersonating)
  - [ ] 10.6 Create `BulkUserImportController` in `controllers` package with REST endpoints: POST /api/users/bulk-import (import users from file), GET /api/users/bulk-import/template (download template file)
  - [ ] 10.7 Enhance `UserController` with new endpoints: GET /api/users/{id}/detail (get user detail with assignments and sessions), GET /api/users/{id}/assignments (get user's station assignments), PUT /api/users/{id}/assignments (update user's station assignments)
  - [ ] 10.8 Add Swagger/OpenAPI annotations (@Operation, @ApiResponse, @Tag) to all controller endpoints
  - [ ] 10.9 Add proper HTTP status codes and error handling to all endpoints
  - [ ] 10.10 Add @PreAuthorize annotations to enforce access control at controller level
  - [ ] 10.11 Write comprehensive unit tests for all controllers (80%+ coverage)
  - [ ] 10.12 Write integration tests for all REST endpoints

- [ ] 11.0 Security Integration and Permission Enforcement
  - [ ] 11.1 Enhance `SecurityConfig` to integrate with permission system for method-level security
  - [ ] 11.2 Create custom `PermissionEvaluator` for Spring Security to check permissions based on user-station assignments
  - [ ] 11.3 Update `JwtAuthenticationFilter` to include user's station assignments in security context
  - [ ] 11.4 Create `StationFilterInterceptor` or aspect to automatically filter API responses based on user's station assignments
  - [ ] 11.5 Integrate permission checks into existing controllers (OrderController, LogController, etc.) to filter data by station
  - [ ] 11.6 Update service methods in existing services to check permissions before allowing operations
  - [ ] 11.7 Implement automatic data filtering in repository queries based on user's station assignments (using @Query annotations or custom repository methods)
  - [ ] 11.8 Add permission checks to all endpoints that access station-specific data
  - [ ] 11.9 Test permission enforcement with various user-station combinations
  - [ ] 11.10 Verify that users cannot access data for stations they are not assigned to, even via direct API calls
  - [ ] 11.11 Write integration tests for permission enforcement across all endpoints

- [ ] 12.0 Bulk User Import
  - [ ] 12.1 Implement CSV parsing in `BulkUserImportServiceImpl` using Apache Commons CSV or similar library
  - [ ] 12.2 Implement Excel parsing in `BulkUserImportServiceImpl` using Apache POI or similar library
  - [ ] 12.3 Create CSV/Excel template file with headers: email, password (optional), role, status, station_assignments (comma-separated station IDs), permission_levels (JSON or comma-separated)
  - [ ] 12.4 Implement validation logic for imported user data (email format, role validity, station existence, etc.)
  - [ ] 12.5 Implement preview functionality that validates data and shows what will be imported before confirmation
  - [ ] 12.6 Implement partial import logic that creates valid users and skips invalid ones with detailed error reporting
  - [ ] 12.7 Implement password auto-generation for users without passwords in import file
  - [ ] 12.8 Create endpoint to download template CSV/Excel file
  - [ ] 12.9 Implement import result reporting with success count, failure count, and detailed error messages
  - [ ] 12.10 Add file size limits and validation (max 10MB)
  - [ ] 12.11 Write comprehensive unit tests for bulk import service (80%+ coverage)
  - [ ] 12.12 Write integration tests for bulk import endpoint with sample CSV/Excel files

- [ ] 13.0 User Management UI Components
  - [ ] 13.1 Create `user-management.html` page with responsive layout, following existing dashboard.html patterns
  - [ ] 13.2 Implement user list view with table showing: email, role, status, assigned stations, last login, created date, with filtering and search
  - [ ] 13.3 Implement user detail view/modal showing: basic info, station assignments with permission levels, active sessions, recent audit logs
  - [ ] 13.4 Implement user creation/edit form with fields: email, password, role, status, and station assignment section
  - [ ] 13.5 Implement station assignment UI with multi-select for stations and permission level dropdown per station
  - [ ] 13.6 Create `role-builder.js` for custom role builder UI with checkbox grid (modules x actions) and permission summary
  - [ ] 13.7 Implement role management page showing: predefined roles, custom roles, with ability to create, edit, clone, and delete roles
  - [ ] 13.8 Create `audit-log-viewer.js` for audit log viewer with filtering (user, date range, action type, resource type, station) and export functionality
  - [ ] 13.9 Create `session-dashboard.js` for real-time session dashboard showing: active users, current station/log being edited, session duration, with ability to terminate sessions
  - [ ] 13.10 Implement impersonation UI with "View as User" button, clear impersonation indicator, and "Stop Impersonation" button
  - [ ] 13.11 Create `bulk-import.js` for bulk user import UI with: file upload, preview table, validation errors display, import progress, and result report
  - [ ] 13.12 Create `user-management.css` with responsive styles, mobile-friendly layouts, and WCAG 2.1 Level AA compliant color contrast
  - [ ] 13.13 Ensure all forms have proper labels, ARIA attributes, and keyboard navigation support
  - [ ] 13.14 Implement real-time updates for session dashboard (using polling or WebSocket if available)
  - [ ] 13.15 Test UI on multiple devices (mobile, tablet, desktop) and browsers
  - [ ] 13.16 Test accessibility with screen readers and keyboard-only navigation
  - [ ] 13.17 Verify color contrast ratios meet WCAG 2.1 Level AA requirements (4.5:1 for normal text, 3:1 for large text)

- [ ] 14.0 Testing and Code Coverage
  - [ ] 14.1 Write unit tests for all enum classes (PermissionLevel, ActionType, ModuleType, AuditActionType, ResourceType) - verify 80%+ coverage
  - [ ] 14.2 Write unit tests for all entity classes (UserStationAssignment, CustomRole, AuditLog, UserSession) - verify 80%+ coverage
  - [ ] 14.3 Write unit tests for all service implementations (PermissionServiceImpl, AuditServiceImpl, SessionServiceImpl, UserStationAssignmentServiceImpl, CustomRoleServiceImpl, ImpersonationServiceImpl, BulkUserImportServiceImpl) - verify 80%+ coverage
  - [ ] 14.4 Write unit tests for all controller classes - verify 80%+ coverage
  - [ ] 14.5 Write repository tests using @DataJpaTest for all new repositories
  - [ ] 14.6 Write integration tests for critical workflows: user creation with station assignment, permission checking, audit logging, session management, impersonation, bulk import
  - [ ] 14.7 Write integration tests for permission enforcement across different user-station combinations
  - [ ] 14.8 Write integration tests for audit trail completeness (verify all actions are logged)
  - [ ] 14.9 Run full test suite and verify all tests pass (`mvn clean test`)
  - [ ] 14.10 Generate JaCoCo coverage report and verify 80%+ coverage (`mvn jacoco:report`)
  - [ ] 14.11 Fix any coverage gaps to meet 80% threshold
  - [ ] 14.12 Verify build passes with coverage enforcement (`mvn clean install`)
  - [ ] 14.13 Perform manual testing of user management UI (create user, assign stations, create custom role, view audit logs, manage sessions, impersonate, bulk import)
  - [ ] 14.14 Test mobile responsiveness on multiple devices
  - [ ] 14.15 Test accessibility with screen readers and keyboard navigation
  - [ ] 14.16 Verify color contrast ratios meet WCAG 2.1 Level AA requirements
  - [ ] 14.17 Test permission enforcement by attempting unauthorized access to stations
  - [ ] 14.18 Test audit trail by performing various actions and verifying they are logged correctly
