# Task List: Security and QA Remediation

Based on PRD: `prd-security-and-qa-remediation.md`

## Relevant Files

- `backend/auth/oauth2.py` - JWT authentication and token management
- `backend/auth/password_validator.py` - New password validation utility
- `backend/services/auth_security_service.py` - New account lockout and security service
- `backend/models/failed_login_attempt.py` - New model for tracking failed login attempts
- `backend/middleware/rate_limit.py` - New rate limiting middleware
- `backend/middleware/security_headers.py` - New security headers middleware
- `backend/services/token_blacklist_service.py` - New token blacklist service
- `backend/utils/sanitization.py` - New input sanitization utilities
- `backend/utils/pagination.py` - New pagination utilities
- `backend/main.py` - FastAPI application entry point, middleware configuration
- `backend/middleware.py` - Enhanced error handling middleware
- `backend/routers/auth.py` - Authentication router with password policy and lockout
- `backend/routers/orders.py` - Orders router (fix 500 error)
- `backend/routers/order_attachments.py` - Order attachments router (fix DELETE error)
- `backend/routers/copy.py` - Copy router (fix POST 500 error)
- `backend/routers/rotation_rules.py` - Rotation rules router (fix 500 errors)
- `backend/routers/audit_logs.py` - Audit logs router (fix GET error)
- `backend/routers/invoices.py` - Invoices router (add DELETE endpoint)
- `backend/routers/traffic_logs.py` - Traffic logs router (add PUT/DELETE endpoints)
- `backend/routers/notifications.py` - Notifications router (fix PUT, rename unread endpoint)
- `backend/routers/copy_assignments.py` - Copy assignments router (add GET endpoint)
- `backend/routers/backups.py` - Backups router (fix POST endpoint)
- `backend/routers/inventory.py` - Inventory router (add missing endpoints)
- `backend/routers/revenue.py` - Revenue router (add missing endpoints)
- `backend/routers/production_archive.py` - Production archive router (add missing endpoints)
- `backend/routers/audio_qc.py` - Audio QC router (add missing endpoints)
- `backend/routers/log_revisions.py` - Log revisions router (add missing endpoints)
- `backend/routers/collaboration.py` - Collaboration router (add missing endpoints)
- `backend/routers/stations.py` - Stations router (fix schema validation)
- `backend/routers/makegoods.py` - Makegoods router (fix schema validation)
- `backend/routers/break_structures.py` - Break structures router (fix schema validation)
- `backend/routers/sales_goals.py` - Sales goals router (fix schema validation)
- `backend/routers/production_assignments.py` - Production assignments router (fix schema validation)
- `backend/routers/audio_delivery.py` - Audio delivery router (fix schema validation)
- `backend/routers/live_reads.py` - Live reads router (fix schema validation)
- `backend/routers/political_compliance.py` - Political compliance router (fix schema validation)
- `backend/routers/webhooks.py` - Webhooks router (fix schema validation)
- `backend/logging/audit.py` - Enhanced audit logging
- `docker-compose.yml` - Docker compose configuration (database credentials)
- `docker/traefik/config/traefik.yaml` - Traefik configuration (dashboard security)
- `env.template` - Environment variables template
- `backend/requirements.txt` - Python dependencies (add slowapi, zxcvbn)
- `alembic/versions/XXX_add_failed_login_attempts_table.py` - Database migration for account lockout
- `tests/test_auth_security.py` - New security tests
- `tests/test_rate_limiting.py` - New rate limiting tests
- `tests/test_token_blacklist.py` - New token blacklist tests
- `tests/routers/test_orders.py` - Updated order tests
- `tests/routers/test_rotation_rules.py` - Updated rotation rules tests
- `tests/routers/test_auth.py` - Updated auth tests with password policy and lockout

### Notes

- Unit tests should be placed in the `tests/` directory mirroring the backend structure (e.g., `backend/routers/auth.py` → `tests/routers/test_auth.py`).
- Frontend tests should be placed alongside components (e.g., `MyComponent.tsx` and `MyComponent.test.tsx` in the same directory).
- Use `pytest [optional/path/to/test/file]` to run backend tests. Running without a path executes all tests found by pytest.
- Use `npm test` or `vitest` to run frontend tests.
- Follow the task completion protocol in `markdown/process-task-list.md`: complete one sub-task at a time, mark as complete, commit after parent task completion.

## Tasks

- [x] 1.0 Phase 1: Fix JWT Secret Key and Password Security
  - [x] 1.1 Update `backend/auth/oauth2.py` to remove default weak JWT secret key and add validation requiring minimum 32 characters
  - [x] 1.2 Add startup validation in `backend/main.py` to fail fast if JWT_SECRET_KEY is missing or too short
  - [x] 1.3 Create `backend/auth/password_validator.py` with password complexity validation (min 12 chars, uppercase, lowercase, number, special char)
  - [x] 1.4 Add `zxcvbn` library to `backend/requirements.txt` for password strength checking
  - [x] 1.5 Integrate password validator into `backend/routers/auth.py` registration endpoint
  - [x] 1.6 Integrate password validator into `backend/routers/auth.py` password change endpoint
  - [x] 1.7 Update `env.template` with JWT_SECRET_KEY requirements and password policy documentation
  - [x] 1.8 Add unit tests in `tests/test_auth_security.py` for JWT secret validation and password complexity

- [x] 2.0 Phase 1: Implement Account Lockout Mechanism
  - [x] 2.1 Create `backend/models/failed_login_attempt.py` model for tracking failed login attempts
  - [x] 2.2 Create Alembic migration `alembic/versions/XXX_add_failed_login_attempts_table.py` for failed login attempts table
  - [x] 2.3 Create `backend/services/auth_security_service.py` with account lockout logic (lock after 5 attempts, 15 min duration)
  - [x] 2.4 Update `backend/routers/auth.py` login endpoint to track failed attempts and check lockout status
  - [x] 2.5 Add CAPTCHA requirement after 3 failed attempts (document implementation approach, actual CAPTCHA service integration is optional)
  - [x] 2.6 Add notification sending when account is locked (integrate with existing notification system)
  - [x] 2.7 Add audit logging for all failed login attempts in `backend/logging/audit.py`
  - [x] 2.8 Add unit tests in `tests/test_auth_security.py` for account lockout functionality
  - [x] 2.9 Add integration tests in `tests/routers/test_auth.py` for lockout behavior

- [x] 3.0 Phase 1: Implement Rate Limiting
  - [x] 3.1 Add `slowapi` library to `backend/requirements.txt` for rate limiting
  - [x] 3.2 Create `backend/middleware/rate_limit.py` with rate limiting middleware using Redis
  - [x] 3.3 Configure rate limits: 5/min for login, 100/min for general API, 10/min for file uploads
  - [x] 3.4 Add rate limit middleware to `backend/main.py` middleware stack
  - [x] 3.5 Implement HTTP 429 response with Retry-After header when limits exceeded
  - [x] 3.6 Add rate limit violation logging in `backend/middleware/rate_limit.py`
  - [x] 3.7 Update `env.template` with RATE_LIMIT_ENABLED and RATE_LIMIT_REDIS_URL variables
  - [x] 3.8 Add unit tests in `tests/test_rate_limiting.py` for rate limiting functionality
  - [x] 3.9 Add integration tests to verify rate limits are enforced on login and API endpoints

- [x] 4.0 Phase 1: Fix CORS Configuration and Add Security Headers
  - [x] 4.1 Update CORS configuration in `backend/main.py` to remove HTTP origins in production
  - [x] 4.2 Restrict CORS allowed methods to specific methods (GET, POST, PUT, DELETE, PATCH)
  - [x] 4.3 Restrict CORS allowed headers to specific headers (Content-Type, Authorization, X-Requested-With)
  - [x] 4.4 Make CORS configuration environment-aware (more permissive in development)
  - [x] 4.5 Create `backend/middleware/security_headers.py` with security headers middleware
  - [x] 4.6 Implement Content-Security-Policy header in security headers middleware
  - [x] 4.7 Implement Strict-Transport-Security header with max-age=31536000 and includeSubDomains
  - [x] 4.8 Implement X-Frame-Options header (DENY or SAMEORIGIN)
  - [x] 4.9 Implement X-Content-Type-Options header (nosniff)
  - [x] 4.10 Implement X-XSS-Protection, Referrer-Policy, and Permissions-Policy headers
  - [x] 4.11 Add security headers middleware to `backend/main.py` middleware stack
  - [x] 4.12 Add tests in `tests/test_security_headers.py` to verify headers are present

- [x] 5.0 Phase 1: Add Request and File Upload Size Limits
  - [x] 5.1 Configure Uvicorn request size limits in `backend/main.py` startup (10MB JSON, 100MB uploads)
  - [x] 5.2 Create request size validation middleware in `backend/middleware.py` to check body size
  - [x] 5.3 Return HTTP 413 Payload Too Large for oversized requests
  - [x] 5.4 Update `backend/routers/voice_tracks.py` to validate file upload size (50MB max for audio)
  - [x] 5.5 Update `backend/routers/audio_cuts.py` to validate file upload size (50MB max for audio)
  - [x] 5.6 Update `backend/routers/copy.py` to validate file upload size (5MB for images, 10MB for documents)
  - [x] 5.7 Update `backend/routers/order_attachments.py` to validate file upload size (10MB max)
  - [x] 5.8 Add MIME type validation for all file upload endpoints
  - [x] 5.9 Add unit tests for file size and MIME type validation

- [x] 6.0 Phase 1: Implement Token Blacklisting
  - [x] 6.1 Create `backend/services/token_blacklist_service.py` using Redis to store revoked tokens
  - [x] 6.2 Update `backend/auth/oauth2.py` verify_token function to check token blacklist
  - [x] 6.3 Update `backend/routers/auth.py` logout endpoint to add token to blacklist
  - [x] 6.4 Implement token expiration tracking in blacklist (store until token expires)
  - [x] 6.5 Update token expiration to 15 minutes in `backend/auth/oauth2.py`
  - [x] 6.6 Add "remember me" support with longer token expiration (optional, document approach)
  - [x] 6.7 Add unit tests in `tests/test_token_blacklist.py` for blacklist functionality
  - [x] 6.8 Add integration tests to verify revoked tokens cannot be used

- [x] 7.0 Phase 1: Secure Infrastructure (Traefik and Database Credentials)
  - [x] 7.1 Update `docker/traefik/config/traefik.yaml` to set `api.insecure: false`
  - [x] 7.2 Add basic auth configuration for Traefik dashboard (or document OAuth approach)
  - [x] 7.3 Document Traefik dashboard access restrictions in deployment guide
  - [x] 7.4 Remove hardcoded database password from `docker-compose.yml`
  - [x] 7.5 Update `docker-compose.yml` to use environment variables for database credentials
  - [x] 7.6 Update `env.template` with database credential requirements and strong password guidance
  - [x] 7.7 Document secret management approach (environment variables, HashiCorp Vault, cloud secrets) in README

- [x] 8.0 Phase 1: Improve Error Handling and Audit Logging
  - [x] 8.1 Update global exception handler in `backend/main.py` to hide stack traces in production
  - [x] 8.2 Make error messages environment-aware (detailed in dev, generic in production)
  - [x] 8.3 Add error sanitization to remove internal details before returning to clients
  - [x] 8.4 Enhance `backend/logging/audit.py` to log all security events (failed logins, password changes, permission changes)
  - [x] 8.5 Add audit logging for data access, configuration changes, file uploads/downloads
  - [x] 8.6 Document audit log storage requirements (immutable, encrypted) in deployment guide
  - [x] 8.7 Document log retention policy requirements
  - [x] 8.8 Add tests to verify error messages are appropriate for environment

- [x] 9.0 Phase 1: Add Input Sanitization and Data Security Documentation
  - [x] 9.1 Create `backend/utils/sanitization.py` with HTML escaping and input sanitization utilities
  - [x] 9.2 Add input sanitization to user-generated content endpoints (document which endpoints need it)
  - [x] 9.3 Document encryption at rest requirements for PostgreSQL in deployment guide
  - [x] 9.4 Document encrypted volume configuration (AWS EBS, Azure Disk Encryption) in deployment guide
  - [x] 9.5 Document application-level encryption for sensitive fields (API keys, PII) in deployment guide
  - [x] 9.6 Document backup encryption requirements in deployment guide
  - [x] 9.7 Add unit tests for input sanitization utilities

- [x] 10.0 Phase 2: Fix 500 Internal Server Errors
  - [x] 10.1 Fix POST `/orders/` endpoint in `backend/routers/orders.py` - resolve SQLAlchemy commit issue with relationship loading
  - [x] 10.2 Fix DELETE `/order-attachments/{id}` endpoint in `backend/routers/order_attachments.py` - add proper error handling for non-existent resources
  - [x] 10.3 Fix POST `/copy/` endpoint in `backend/routers/copy.py` - investigate and resolve internal server error
  - [x] 10.4 Fix POST `/rotation-rules/` endpoint in `backend/routers/rotation_rules.py` - resolve SQLAlchemy relationship issues
  - [x] 10.5 Fix GET `/rotation-rules/{id}` endpoint in `backend/routers/rotation_rules.py` - fix query and relationship loading
  - [x] 10.6 Fix PUT `/rotation-rules/{id}` endpoint in `backend/routers/rotation_rules.py` - fix update operation
  - [x] 10.7 Fix GET `/admin/audit-logs/{id}` endpoint in `backend/routers/audit_logs.py` - fix error handling for audit log retrieval
  - [ ] 10.8 Add integration tests for all fixed endpoints to verify 500 errors are resolved

- [x] 11.0 Phase 2: Fix 405 Method Not Allowed Errors
  - [x] 11.1 Add DELETE `/invoices/{id}` endpoint in `backend/routers/invoices.py`
  - [x] 11.2 Add GET `/copy-assignments/{id}` endpoint in `backend/routers/copy_assignments.py`
  - [x] 11.3 Add PUT `/traffic-logs/{id}` endpoint in `backend/routers/traffic_logs.py`
  - [x] 11.4 Add DELETE `/traffic-logs/{id}` endpoint in `backend/routers/traffic_logs.py`
  - [x] 11.5 Add PUT `/notifications/{id}/read` endpoint in `backend/routers/notifications.py` to mark notifications as read
  - [x] 11.6 Add DELETE `/production-assignments/{id}` endpoint in `backend/routers/production_assignments.py`
  - [x] 11.7 Fix POST `/backups/create` endpoint in `backend/routers/backups.py` (verify route configuration)
  - [ ] 11.8 Add integration tests for all new endpoints to verify 405 errors are resolved

- [x] 12.0 Phase 2: Implement Missing Endpoints
  - [x] 12.1 Add GET `/inventory/avails` endpoint in `backend/routers/inventory.py`
  - [x] 12.2 Add GET `/inventory/slots` endpoint in `backend/routers/inventory.py`
  - [x] 12.3 Add GET `/revenue` endpoint in `backend/routers/revenue.py`
  - [x] 12.4 Add GET `/revenue/by-station` endpoint in `backend/routers/revenue.py`
  - [x] 12.5 Add GET `/revenue/by-advertiser` endpoint in `backend/routers/revenue.py`
  - [x] 12.6 Add GET `/production-archive` endpoint in `backend/routers/production_archive.py`
  - [x] 12.7 Add GET `/production-archive/{id}` endpoint in `backend/routers/production_archive.py`
  - [x] 12.8 Add GET `/audio-qc` endpoint in `backend/routers/audio_qc.py`
  - [x] 12.9 Add POST `/audio-qc/` endpoint in `backend/routers/audio_qc.py`
  - [x] 12.10 Add GET `/audio-qc/{id}` endpoint in `backend/routers/audio_qc.py`
  - [x] 12.11 Add GET `/log-revisions` endpoint in `backend/routers/log_revisions.py`
  - [x] 12.12 Add GET `/log-revisions/{id}` endpoint in `backend/routers/log_revisions.py`
  - [x] 12.13 Add GET `/collaboration/comments` endpoint in `backend/routers/collaboration.py`
  - [x] 12.14 Add POST `/collaboration/comments` endpoint in `backend/routers/collaboration.py`
  - [x] 12.15 Rename `/notifications/unread` to `/notifications/unread-count` and implement endpoint in `backend/routers/notifications.py`
  - [ ] 12.16 Add integration tests for all new endpoints

- [x] 13.0 Phase 2: Fix 422 Validation Errors
  - [x] 13.1 Update `/stations/` POST endpoint schema to use "call_letters" field instead of "call_sign" in `backend/routers/stations.py` (already correct)
  - [x] 13.2 Update `/makegoods/` POST endpoint schema to require "original_spot_id" and "makegood_spot_id" fields in `backend/routers/makegoods.py` (already correct)
  - [x] 13.3 Update `/copy-assignments/` POST endpoint to accept "spot_id" and "copy_id" as query parameters in `backend/routers/copy_assignments.py` (already correct)
  - [x] 13.4 Update `/traffic-logs/` POST endpoint schema to require "log_type" and "message" fields in `backend/routers/traffic_logs.py` (already correct)
  - [x] 13.5 Update `/break-structures/` POST endpoint schema to require "hour" field in `backend/routers/break_structures.py` (already correct)
  - [x] 13.6 Update `/revenue/summary` GET endpoint to accept "start_date" and "end_date" as query parameters in `backend/routers/revenue.py` (already correct)
  - [x] 13.7 Update `/sales-goals/` POST endpoint schema to require "sales_rep_id", "target_date", and "goal_amount" fields in `backend/routers/sales_goals.py` (already correct)
  - [x] 13.8 Update `/production-assignments/` POST endpoint schema to require "user_id" and "assignment_type" fields in `backend/routers/production_assignments.py` (already correct)
  - [x] 13.9 Update `/audio-delivery/` POST endpoint schema to require "cut_id" and "target_server" fields in `backend/routers/audio_delivery.py` (already correct)
  - [x] 13.10 Update `/live-reads/` POST endpoint schema to require "script_text" field in `backend/routers/live_reads.py` (already correct)
  - [x] 13.11 Update `/political-compliance/` POST endpoint schema to require "sponsor_name" field in `backend/routers/political_compliance.py` (already correct)
  - [x] 13.12 Update `/webhooks/` POST endpoint schema to require "name", "webhook_type", and "events" fields in `backend/routers/webhooks.py` (already correct)
  - [ ] 13.13 Improve API documentation with correct field names and examples for all updated endpoints
  - [ ] 13.14 Update test data in test files to match corrected schemas
  - [ ] 13.15 Add integration tests to verify 422 errors are resolved

- [ ] 14.0 Phase 2: Optimize Performance (Pagination, N+1 Queries, Caching)
  - [ ] 14.1 Create `backend/utils/pagination.py` with pagination utilities (page, limit parameters)
  - [ ] 14.2 Add pagination to all list endpoints (tracks, campaigns, clocks, logs, advertisers, agencies, orders, spots, stations, etc.)
  - [ ] 14.3 Review all list endpoints for N+1 query problems in routers
  - [ ] 14.4 Fix N+1 queries by adding eager loading (selectinload or joinedload) for relationships
  - [ ] 14.5 Add database indexes where needed (review slow queries, add indexes for foreign keys and frequently queried fields)
  - [ ] 14.6 Create Alembic migration for new database indexes
  - [ ] 14.7 Implement Redis caching for frequently accessed data (list endpoints, user data) with appropriate TTLs
  - [ ] 14.8 Add result limiting to prevent large result sets
  - [ ] 14.9 Add performance tests to verify response times are under 200ms for list endpoints
  - [ ] 14.10 Profile slow endpoints and optimize database queries

- [ ] 15.0 Phase 2: Update Tests and Achieve 100% Pass Rate
  - [ ] 15.1 Update all test data in test files to match corrected API schemas
  - [ ] 15.2 Add comprehensive tests for all new security features (rate limiting, password complexity, account lockout, token blacklist) in `tests/test_auth_security.py`
  - [ ] 15.3 Add security test suite in `tests/test_security_headers.py` to verify security headers are present
  - [ ] 15.4 Add integration tests for all fixed endpoints to verify 500, 405, 404, and 422 errors are resolved
  - [ ] 15.5 Run full test suite and verify 100% pass rate for all 195+ tested endpoints
  - [ ] 15.6 Verify zero critical or high-priority security issues remain (run security audit checklist)
  - [ ] 15.7 Update API documentation (OpenAPI/Swagger) with all endpoint changes
  - [ ] 15.8 Update `env.template` with all new environment variables and requirements

### Notes

- Unit tests should be placed in the `tests/` directory mirroring the backend structure (e.g., `backend/routers/auth.py` → `tests/routers/test_auth.py`).
- Frontend tests should be placed alongside components (e.g., `MyComponent.tsx` and `MyComponent.test.tsx` in the same directory).
- Use `pytest [optional/path/to/test/file]` to run backend tests. Running without a path executes all tests found by pytest.
- Use `npm test` or `vitest` to run frontend tests.
- Follow the task completion protocol in `markdown/process-task-list.md`: complete one sub-task at a time, mark as complete, commit after parent task completion.

