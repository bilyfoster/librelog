# Product Requirements Document: UUID Migration

## Introduction/Overview

The LibreLog system currently uses integer-based primary keys and foreign keys throughout the database, APIs, and frontend. While functional, integer IDs present security risks as they are easily enumerable, allowing potential attackers to discover and access resources by guessing sequential IDs. Additionally, the coding guidelines specify that UUIDs should be used for primary keys to prevent walkable IDs and enumeration attacks.

This PRD outlines the complete migration of the system from integer IDs to UUIDs (Universally Unique Identifiers) across all database models, API endpoints, service layers, and frontend components. Since the system is in production development with no real data, we can perform a clean migration by wiping the database and recreating all tables with UUID primary keys, while preserving essential configuration (email settings and admin user accounts).

The goal is to enhance security, prevent enumeration attacks, align with coding guidelines, and establish a robust foundation for future development.

## Goals

1. **Security Enhancement**: Replace all integer IDs with UUIDs to prevent enumeration attacks and unauthorized resource discovery
2. **Coding Guideline Compliance**: Align with the coding guidelines requirement for UUID-based primary keys
3. **Complete Migration**: Migrate all database models, APIs, services, and frontend code to use UUIDs consistently
4. **Data Preservation**: Preserve essential configuration data (email settings, admin user accounts) during migration
5. **Clean Foundation**: Establish a clean, consistent UUID-based architecture for future development
6. **Type Safety**: Ensure proper UUID type handling throughout the stack (Python, SQLAlchemy, Pydantic, TypeScript)
7. **Documentation**: Update all relevant documentation to reflect UUID usage

## User Stories

1. **As a security-conscious developer**, I want all database entities to use UUIDs so that resource enumeration attacks are prevented
2. **As a developer**, I want consistent UUID usage across the entire stack so that I don't have to handle different ID types
3. **As a system administrator**, I want email and admin settings preserved during migration so that system configuration is not lost
4. **As a developer**, I want all API endpoints to use UUIDs so that the API is secure and consistent
5. **As a frontend developer**, I want TypeScript interfaces to use UUID types so that type checking catches ID-related errors
6. **As a developer**, I want all foreign key relationships to use UUIDs so that referential integrity is maintained with UUIDs
7. **As a developer**, I want proper UUID validation in APIs so that invalid IDs are rejected early

## Functional Requirements

### 1. Database Migration
1. The system must create a new Alembic migration that drops all existing tables
2. The system must recreate all tables with UUID primary keys using PostgreSQL's UUID type
3. The system must update all foreign key columns to reference UUID primary keys
4. The system must ensure all foreign key constraints use UUID types
5. The system must preserve database indexes on UUID columns where appropriate
6. The system must use `sqlalchemy.dialects.postgresql.UUID` for all UUID columns
7. The system must configure UUID primary keys to auto-generate using `server_default=text("gen_random_uuid()")` or similar PostgreSQL function

### 2. Model Updates
8. The system must update all SQLAlchemy models in `backend/models/` to use UUID primary keys
9. The system must update all foreign key columns in models to use UUID type
10. The system must update all `ForeignKey` references to point to UUID columns
11. The system must ensure all relationship definitions remain functional with UUID foreign keys
12. The system must update all model imports to include UUID type from `sqlalchemy.dialects.postgresql`
13. The system must update approximately 48 model files across the codebase

### 3. Schema Updates
14. The system must update all Pydantic schemas in `backend/schemas/` to use `UUID` type instead of `int` for ID fields
15. The system must update all request schemas that accept IDs to use `UUID` type
16. The system must update all response schemas that return IDs to use `UUID` type
17. The system must ensure Pydantic validates UUID format in all schemas
18. The system must update all schema imports to include `UUID` from `uuid` package

### 4. Router/API Updates
19. The system must update all FastAPI router endpoints to accept UUID parameters instead of integers
20. The system must update all path parameters that represent IDs to use UUID type
21. The system must update all query parameters that filter by ID to use UUID type
22. The system must update all request body schemas in routers to use UUID types
23. The system must update all response models in routers to use UUID types
24. The system must ensure FastAPI validates UUID format in path and query parameters
25. The system must update all router files in `backend/routers/` (approximately 50+ router files)

### 5. Service Layer Updates
26. The system must update all service methods that accept ID parameters to use UUID type
27. The system must update all service methods that return entities with IDs to handle UUIDs
28. The system must update all database queries in services to use UUID values
29. The system must update all service files in `backend/services/` (approximately 40+ service files)
30. The system must ensure service methods properly handle UUID validation and conversion

### 6. Frontend Updates
31. The system must update all TypeScript interfaces that define ID fields to use `string` type (UUIDs as strings)
32. The system must update all API client functions to send/receive UUIDs as strings
33. The system must update all form components that handle IDs to work with UUID strings
34. The system must update all URL parameters and route definitions that use IDs
35. The system must update all frontend code that constructs API requests with IDs
36. The system must ensure frontend validates UUID format where appropriate (using regex or library)
37. The system must update all components that display or manipulate entity IDs

### 7. Settings and Admin Preservation
38. The system must create a script or utility to export email settings from the `settings` table before migration
39. The system must create a script or utility to export admin user accounts (username, password hash, role) before migration
40. The system must create a script or utility to restore email settings after migration
41. The system must create a script or utility to restore admin user accounts after migration
42. The system must ensure restored admin users have valid UUID primary keys
43. The system must document the preservation and restoration process

### 8. Testing Updates
44. The system must update all unit tests to use UUID test values instead of integers
45. The system must update all integration tests to generate and use UUIDs
46. The system must update all test fixtures to create entities with UUID primary keys
47. The system must update all test assertions that check ID values to expect UUIDs
48. The system must ensure all tests pass after UUID migration
49. The system must add tests to verify UUID validation in APIs

### 9. Code Quality and Type Safety
50. The system must update all type hints throughout the codebase to use `UUID` type where appropriate
51. The system must ensure no integer ID types remain in the codebase (except for non-ID integer fields)
52. The system must update all function signatures to use UUID types for ID parameters
53. The system must ensure consistent UUID import statements (`from uuid import UUID`)

## Non-Goals (Out of Scope)

1. **Data Migration from Existing Data**: Since the database can be wiped, we will not migrate existing business data (orders, spots, tracks, etc.). Only configuration data (settings, admin users) will be preserved.
2. **Backward Compatibility**: No backward compatibility layer for integer IDs. This is a breaking change, which is acceptable since the system is in production development.
3. **API Versioning**: No API versioning strategy. All APIs will immediately use UUIDs.
4. **Performance Optimization**: While UUIDs may have different performance characteristics than integers, performance optimization is out of scope for this migration.
5. **UUID Generation Strategy Changes**: We will use standard UUID v4 (random UUIDs) generation. No custom UUID formats or strategies.
6. **Frontend UUID Libraries**: While we may use UUID validation, we will not introduce new UUID manipulation libraries unless necessary for validation.
7. **Database Index Optimization**: We will maintain existing index patterns but will not perform comprehensive index optimization for UUIDs.

## Design Considerations

### UUID Type Selection
- Use PostgreSQL's native `UUID` type for database columns
- Use Python's `uuid.UUID` type for type hints and validation
- Use Pydantic's `UUID` type for schema validation
- Represent UUIDs as strings in JSON/API responses
- Represent UUIDs as strings in TypeScript/JavaScript

### Database Migration Strategy
- Since database can be wiped, create a single comprehensive migration that:
  - Drops all existing tables (using `drop_all()` or explicit DROP statements)
  - Creates all tables fresh with UUID primary keys
  - Sets up all foreign key relationships with UUID types
  - Uses PostgreSQL's `gen_random_uuid()` for default UUID generation

### Settings Preservation
- Export settings data to JSON or SQL dump before migration
- Categorize settings by type (email, admin, general)
- Restore settings after migration with new UUID primary keys
- Ensure settings table structure supports UUID primary key

### Admin User Preservation
- Export admin user data (username, password_hash, role, permissions) before migration
- Restore admin users after migration with new UUID primary keys
- Ensure password hashes remain valid (they are independent of user ID)
- Verify admin users can log in after restoration

### Code Update Strategy
- Update models first (foundation)
- Update schemas second (data validation layer)
- Update services third (business logic layer)
- Update routers fourth (API layer)
- Update frontend last (presentation layer)
- Update tests alongside each layer

## Technical Considerations

### Dependencies
- **PostgreSQL**: Must support UUID type (PostgreSQL 8.3+)
- **SQLAlchemy**: Use `sqlalchemy.dialects.postgresql.UUID` type
- **Pydantic**: Use `pydantic.UUID4` or `UUID` type for validation
- **FastAPI**: Automatically validates UUID format in path/query parameters
- **Python**: Use `uuid` standard library module

### Migration Script Structure
- Create Alembic migration: `XXX_migrate_to_uuid_primary_keys.py`
- Migration should:
  1. Export settings and admin users (if not done separately)
  2. Drop all existing tables
  3. Create all tables with UUID primary keys
  4. Restore settings and admin users (if not done separately)

### Settings Export/Restore Script
- Create utility script: `scripts/export_settings.py` and `scripts/restore_settings.py`
- Export to JSON format for easy inspection and modification
- Include all settings categories (email, admin, general)
- Include admin user data with password hashes

### Foreign Key Updates
- All foreign key columns must change from `Integer` to `UUID`
- All `ForeignKey()` definitions must reference UUID columns
- Relationship definitions should remain unchanged (SQLAlchemy handles UUID foreign keys)

### API Parameter Validation
- FastAPI automatically validates UUID format in path parameters: `/users/{user_id:uuid}`
- FastAPI automatically validates UUID format in query parameters when type is `UUID`
- Invalid UUIDs will return 422 Unprocessable Entity with validation error

### Frontend UUID Handling
- UUIDs are represented as strings in JSON
- TypeScript interfaces should use `string` type for UUID fields
- Use regex or library for UUID validation if needed: `/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i`
- No need for UUID manipulation libraries unless validation is required

### Testing Considerations
- Generate test UUIDs using `uuid.uuid4()` in Python
- Generate test UUIDs using `crypto.randomUUID()` or library in TypeScript/JavaScript
- Update all test fixtures to create entities with UUID primary keys
- Mock UUIDs in tests should be valid UUID format

### Files to Modify
- **Models**: ~48 files in `backend/models/`
- **Schemas**: ~10+ files in `backend/schemas/`
- **Routers**: ~50+ files in `backend/routers/`
- **Services**: ~40+ files in `backend/services/`
- **Frontend**: All files that reference entity IDs
- **Tests**: All test files that create or reference entities
- **Migrations**: New migration file in `alembic/versions/`

### Potential Challenges
1. **Cascade of Changes**: Changing primary keys affects all foreign keys, requiring updates across many files
2. **Test Data**: All test fixtures and mocks need UUID updates
3. **Frontend State Management**: Any cached IDs or state management needs UUID updates
4. **URL Parameters**: All routes with ID parameters need UUID validation
5. **Search/Filter Logic**: Any code that searches or filters by ID range needs updates (UUIDs don't have ranges)

## Success Metrics

1. **Zero Integer Primary Keys**: All database tables use UUID primary keys
2. **Zero Integer Foreign Keys**: All foreign key columns use UUID type
3. **100% Schema Coverage**: All Pydantic schemas use UUID type for ID fields
4. **100% API Coverage**: All API endpoints accept/return UUIDs
5. **100% Service Coverage**: All service methods use UUID types
6. **100% Frontend Coverage**: All frontend code handles UUIDs correctly
7. **100% Test Pass Rate**: All existing tests pass with UUID implementation
8. **Settings Preserved**: Email settings and admin users are successfully restored
9. **No Integer ID References**: Codebase contains no integer ID types (except non-ID integers)
10. **Build Success**: Application builds and runs without errors
11. **API Validation**: Invalid UUIDs are properly rejected with 422 errors
12. **Admin Login**: Admin users can successfully log in after restoration

## Open Questions

1. **UUID Generation**: Should we use PostgreSQL's `gen_random_uuid()` or generate UUIDs in Python? (Recommendation: Use PostgreSQL's function for database defaults, Python's `uuid.uuid4()` for application-generated UUIDs)
2. **Settings Export Format**: JSON or SQL dump? (Recommendation: JSON for readability and easy modification)
3. **Migration Timing**: Should settings export/restore be part of the Alembic migration or separate scripts? (Recommendation: Separate scripts for flexibility and inspection)
4. **Frontend UUID Validation**: Should we add UUID validation library or use regex? (Recommendation: Use regex for simple validation, library only if complex manipulation needed)
5. **Test UUID Generation**: Should we use fixed UUIDs for tests or generate random ones? (Recommendation: Generate random UUIDs for most tests, fixed UUIDs only for specific test cases that need deterministic behavior)

## Target Audience

This PRD is written for junior to mid-level developers who will implement the UUID migration. Requirements are explicit and actionable, with clear file locations and technical specifications provided.

## Related Documents

- `markdown/coding_guidelines.md` - Coding standards requiring UUID primary keys
- `markdown/create-prd.md` - PRD creation process
- `markdown/generate-tasks.md` - Task list generation process
- SQLAlchemy UUID Documentation: https://docs.sqlalchemy.org/en/20/core/type_basics.html#sqlalchemy.types.UUID
- PostgreSQL UUID Documentation: https://www.postgresql.org/docs/current/datatype-uuid.html
- Pydantic UUID Documentation: https://docs.pydantic.dev/latest/concepts/types/#uuid-types

