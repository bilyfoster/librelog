# Product Requirements Document: Enterprise User Management System

## Introduction/Overview

The User Management System is the **access control foundation** of the broadcast ERP, enabling complex multi-level permissions that match or exceed WideOrbit's capabilities. Unlike simple role-based systems, broadcast ERP requires users to have different permission levels across different stations, markets, and clusters—a single user might need full access to three radio stations in one market, view-only access to a TV station in another, and no access to the Digital/Streaming division.

**Problem Statement:** The current user management system only supports basic role assignment (ADMIN, TRAFFIC_MANAGER, SALES_REP, etc.) at a global level. Users cannot be assigned to specific stations, markets, or clusters, and there is no granular permission system, audit trail, or enterprise security features. This prevents the system from handling complex broadcast organizational structures where users need property-based scoping and different permission levels across different stations.

**Goal:** Build a comprehensive, enterprise-grade user management system that supports multi-level access control (property-based scoping, market/cluster overlays), role-based access control with custom role builder, enterprise identity integration, comprehensive audit trails, and advanced features like impersonation and session management—all while maintaining compatibility with existing authentication and providing a foundation for SSO/SAML integration in future phases.

## Goals

1. Enable property-based access control where users can have different permission levels across different stations
2. Support market and cluster-level permission inheritance for efficient multi-station management
3. Implement granular RBAC with module-level and action-level permissions (View/Create/Edit/Delete)
4. Provide a custom role builder UI that allows cloning and modifying roles with specific permission checkboxes
5. Create comprehensive audit trails for all user actions (logins, data changes, permission changes)
6. Implement real-time session management dashboard showing active users and their current activities
7. Support bulk user import via CSV/Excel upload with UI
8. Enable admin impersonation mode for troubleshooting user access issues
9. Maintain traditional username/password authentication while preparing architecture for future SSO/SAML integration
10. Ensure all features meet enterprise security standards and WCAG 2.1 Level AA accessibility requirements

## User Stories

1. **As a System Administrator**, I want to assign a user to specific stations with different permission levels, so that a regional Traffic Director can have full access to three radio stations in one market but only view-only access to a TV station in another market.

2. **As a System Administrator**, I want to assign permissions at the market level, so that a Market Controller automatically inherits permissions for all stations within that geographic cluster without manual assignment to each station.

3. **As a System Administrator**, I want to create custom roles by cloning existing roles and modifying specific permissions, so that I can create role variations like "Sales Rep - No Rate Override" without starting from scratch.

4. **As a System Administrator**, I want to see a complete audit trail of who moved a spot on the log, when they did it, and what the previous value was, so that I can resolve billing disputes and track makegoods.

5. **As a System Administrator**, I want to impersonate a user to troubleshoot why they can't see a specific avail or why a report is failing, so that I can quickly diagnose access issues without asking the user to reproduce the problem.

6. **As a System Administrator**, I want to see a real-time dashboard of who is currently logged in and which station log they are editing, so that I can prevent "Locked Record" conflicts and monitor system usage.

7. **As a System Administrator**, I want to bulk import users from a CSV/Excel file, so that I can quickly onboard a new station acquisition with multiple users at once.

8. **As a Traffic Manager**, I want my permissions to be automatically filtered to only show stations I have access to, so that I don't see or accidentally modify data from stations outside my responsibility.

9. **As a Sales Rep**, I want to create orders but not approve them or modify rates below a certain floor, so that I can perform my job functions while maintaining financial controls.

10. **As a Finance/Billing user**, I want to generate invoices and manage A/R, but not edit logs or move spots, so that I have access to financial data without interfering with traffic operations.

11. **As a Continuity/Production user**, I want to only handle Material Instructions (associating commercial audio/video files with spots), so that I can perform my specialized function without access to other modules.

12. **As a System Administrator**, I want to see all login attempts, successful and failed, in the audit trail, so that I can monitor for security issues and unauthorized access attempts.

13. **As a System Administrator**, I want to see when permissions are changed for a user, including who made the change and what the previous permissions were, so that I can track access modifications for compliance.

14. **As a user**, I want to see my own active sessions and be able to terminate sessions from other devices, so that I can maintain account security if I suspect unauthorized access.

## Functional Requirements

### 1. Multi-Level Access Control (Property-Based Scoping)

#### 1.1 Station-Level Permissions
- **FR-1.1.1:** The system must allow administrators to assign users to specific stations (properties).
- **FR-1.1.2:** Each user-station assignment must support different permission levels (Full Access, View Only, Custom Permissions).
- **FR-1.1.3:** The system must support assigning multiple stations to a single user with different permission levels per station.
- **FR-1.1.4:** When a user accesses the system, the system must automatically filter all data (orders, logs, reports) to only show stations the user has access to.
- **FR-1.1.5:** The system must prevent users from accessing or modifying data for stations they are not assigned to, even if they attempt to do so via direct API calls.
- **FR-1.1.6:** The system must provide a UI for administrators to view and manage user-station assignments.
- **FR-1.1.7:** The system must display which stations a user has access to in the user's profile view.

#### 1.2 Market-Level Permissions (Phase 2)
- **FR-1.2.1:** The system must allow administrators to assign users to markets with permission levels.
- **FR-1.2.2:** Market-level permissions must automatically inherit to all stations within that market.
- **FR-1.2.3:** The system must allow market-level permissions to be overridden at the station level if needed.
- **FR-1.2.4:** The system must provide a UI for administrators to view and manage user-market assignments.

#### 1.3 Cluster-Level Permissions (Phase 2)
- **FR-1.3.1:** The system must allow administrators to assign users to clusters with permission levels.
- **FR-1.3.2:** Cluster-level permissions must automatically inherit to all stations within that cluster.
- **FR-1.3.3:** The system must allow cluster-level permissions to be overridden at the station or market level if needed.
- **FR-1.3.4:** The system must provide a UI for administrators to view and manage user-cluster assignments.

#### 1.4 Tenant-Level Admin
- **FR-1.4.1:** The system must support a "Super Admin" role that has access to all stations, markets, and clusters regardless of assignments.
- **FR-1.4.2:** Super Admin must be able to manage global settings, rate card templates, and system-wide configurations.
- **FR-1.4.3:** Super Admin must be able to view and manage all users, regardless of their station assignments.
- **FR-1.4.4:** The system must clearly distinguish Super Admin users from regular administrators in the UI.

### 2. Role-Based Access Control (RBAC)

#### 2.1 Standard Broadcast Roles
- **FR-2.1.1:** The system must support predefined roles aligned with broadcast workflows:
  - **Sales Rep:** Can create orders but cannot approve them or modify rates below a certain floor.
  - **Traffic Manager:** Can edit logs, move spots, and finalize the "Daily Log."
  - **Finance/Billing:** Can generate invoices, manage A/R, and run reconciliation.
  - **Continuity/Production:** Only handles "Material Instructions" (associating commercial audio/video files with spots).
  - **Programming:** Responsible for content scheduling and program management.
  - **Operations:** Responsible for day-to-day operational tasks and monitoring.
  - **Admin:** System administrator with full access to assigned stations.
  - **Super Admin:** System-wide administrator with access to all stations and global settings.
- **FR-2.1.2:** Each role must have predefined module-level and action-level permissions.
- **FR-2.1.3:** The system must allow users to have different roles for different stations (e.g., Traffic Manager on Station A, Sales Rep on Station B).

#### 2.2 Module-Level Permissions
- **FR-2.2.1:** The system must support module-level permissions for the following modules:
  - Orders (Order Entry, Order Management)
  - Logs (Log Editing, Log Viewing, Log Finalization)
  - Inventory (Avail Management, Rate Card Management)
  - Billing (Invoicing, A/R Management, Reconciliation)
  - Reports (Report Generation, Report Viewing)
  - Material Instructions (Asset Association, File Management)
  - Clock Templates (Template Creation, Template Editing)
  - User Management (User Creation, User Editing, Permission Management)
  - System Settings (Global Configuration, Rate Card Templates)
- **FR-2.2.2:** The system must allow administrators to grant or deny access to entire modules per user-station assignment.
- **FR-2.2.3:** The system must hide modules from the UI that the user does not have access to.

#### 2.3 Action-Level Permissions
- **FR-2.3.1:** For each module, the system must support action-level permissions:
  - **View:** Can view data but cannot modify
  - **Create:** Can create new records
  - **Edit:** Can modify existing records
  - **Delete:** Can delete records
- **FR-2.3.2:** The system must allow administrators to grant specific action permissions independently (e.g., View + Create but not Edit or Delete).
- **FR-2.3.3:** The system must enforce action-level permissions at the API level, not just the UI level.
- **FR-2.3.4:** The system must provide clear error messages when users attempt actions they do not have permission for.

#### 2.4 Custom Role Builder
- **FR-2.4.1:** The system must provide a UI for administrators to create custom roles.
- **FR-2.4.2:** The system must allow administrators to clone existing roles as a starting point for custom roles.
- **FR-2.4.3:** The custom role builder must display all modules and actions as checkboxes that can be enabled/disabled.
- **FR-2.4.4:** The system must allow administrators to name custom roles with descriptive names.
- **FR-2.4.5:** The system must validate that custom roles have at least one permission enabled.
- **FR-2.4.6:** The system must allow administrators to edit custom roles after creation.
- **FR-2.4.7:** The system must prevent deletion of custom roles that are currently assigned to users.
- **FR-2.4.8:** The system must show which users are assigned to each custom role in the role management UI.

#### 2.5 Permission Inheritance
- **FR-2.5.1:** When a user is assigned to a market or cluster, the system must apply the assigned role's permissions to all stations within that market/cluster.
- **FR-2.5.2:** The system must allow station-level permission overrides that take precedence over market/cluster-level permissions.
- **FR-2.5.3:** The system must clearly display the effective permissions for a user at each station, showing whether they come from station, market, cluster, or tenant-level assignment.

### 3. Enterprise Identity & Security

#### 3.1 Traditional Authentication (Current Phase)
- **FR-3.1.1:** The system must maintain support for traditional username/password authentication.
- **FR-3.1.2:** The system must enforce password complexity requirements (minimum length, special characters, etc.).
- **FR-3.1.3:** The system must support password reset functionality via email.
- **FR-3.1.4:** The system must support account lockout after multiple failed login attempts.
- **FR-3.1.5:** The system must log all login attempts (successful and failed) in the audit trail.

#### 3.2 SSO/SAML Integration (Future Phase - Architecture Preparation)
- **FR-3.2.1:** The system architecture must be designed to support SSO/SAML 2.0 integration in a future phase.
- **FR-3.2.2:** The authentication layer must be abstracted to allow multiple authentication providers.
- **FR-3.2.3:** The system must maintain user records that can be linked to external identity providers.
- **FR-3.2.4:** The system must support federated permissions where users in specific Active Directory groups automatically receive roles in the system (future implementation).

#### 3.3 Security Best Practices
- **FR-3.3.1:** The system must use UUIDs for all user identifiers to prevent enumeration attacks.
- **FR-3.3.2:** The system must never expose internal system details in error messages.
- **FR-3.3.3:** The system must use secure password hashing (BCrypt or equivalent) and never store plaintext passwords.
- **FR-3.3.4:** The system must implement CSRF protection for all state-changing operations.
- **FR-3.3.5:** The system must use HTTPS for all authentication and user management operations.

### 4. Audit Trails (History Log)

#### 4.1 Comprehensive Action Logging
- **FR-4.1.1:** The system must log all user actions including:
  - Login attempts (successful and failed)
  - Logout events
  - Data creation (orders, logs, users, etc.)
  - Data modification (edits, updates, moves)
  - Data deletion
  - Permission changes
  - Role assignments
  - Station assignments
- **FR-4.1.2:** Each audit log entry must include:
  - User ID and username
  - Timestamp (with timezone)
  - Action type (CREATE, UPDATE, DELETE, LOGIN, LOGOUT, PERMISSION_CHANGE, etc.)
  - Resource type (Order, Log, User, Permission, etc.)
  - Resource ID
  - Previous value (for updates)
  - New value (for updates and creates)
  - IP address
  - User agent (browser/client information)
- **FR-4.1.3:** The system must store audit logs in a separate, immutable table that cannot be modified or deleted by users.
- **FR-4.1.4:** The system must retain audit logs for a minimum of 7 years for compliance purposes.

#### 4.2 Broadcast-Specific Audit Requirements
- **FR-4.2.1:** When a user moves a spot on a log, the system must log:
  - Who moved the spot
  - When the move occurred
  - What the previous position/time was
  - What the new position/time is
  - The spot ID and order ID
- **FR-4.2.2:** When a user edits a log after finalization, the system must log:
  - Who made the edit
  - When the edit occurred
  - What field was changed
  - Previous value
  - New value
- **FR-4.2.3:** When a user approves or rejects an order, the system must log:
  - Who made the decision
  - When the decision was made
  - The order ID
  - The previous status
  - The new status
  - Any comments or notes

#### 4.3 Audit Trail UI
- **FR-4.3.1:** The system must provide a UI for administrators to view audit logs.
- **FR-4.3.2:** The audit log UI must support filtering by:
  - User
  - Date range
  - Action type
  - Resource type
  - Station
- **FR-4.3.3:** The system must allow administrators to export audit logs to CSV/Excel format.
- **FR-4.3.4:** The system must display audit logs in a readable, chronological format.
- **FR-4.3.5:** The system must highlight critical actions (permission changes, log edits after finalization) in the audit log UI.

### 5. Advanced Features

#### 5.1 Impersonation Mode
- **FR-5.1.1:** The system must allow Super Admin and Admin users to impersonate other users.
- **FR-5.1.2:** When impersonating, the system must clearly display in the UI that the admin is viewing as another user.
- **FR-5.1.3:** The system must log all actions performed during impersonation with both the impersonated user and the admin user who initiated impersonation.
- **FR-5.1.4:** The system must allow admins to stop impersonation and return to their own account at any time.
- **FR-5.1.5:** The system must prevent users from impersonating users with higher permission levels (e.g., Admin cannot impersonate Super Admin).
- **FR-5.1.6:** The system must require explicit confirmation before starting impersonation to prevent accidental activation.

#### 5.2 Session Management
- **FR-5.2.1:** The system must track all active user sessions including:
  - User ID and username
  - Login timestamp
  - Last activity timestamp
  - IP address
  - User agent (browser/client)
  - Current station/log being edited (if applicable)
- **FR-5.2.2:** The system must provide a real-time dashboard for administrators showing:
  - All currently logged-in users
  - Which station log each user is currently editing (to prevent "Locked Record" conflicts)
  - Session duration
  - Last activity time
- **FR-5.2.3:** The system must allow administrators to terminate user sessions from the dashboard.
- **FR-5.2.4:** The system must allow users to view their own active sessions and terminate sessions from other devices.
- **FR-5.2.5:** The system must support configurable session timeout (e.g., 30 minutes of inactivity = automatic logout).
- **FR-5.2.6:** The system must prevent multiple simultaneous sessions for the same user if configured (optional security setting).

#### 5.3 Bulk User Import
- **FR-5.3.1:** The system must provide a UI for administrators to upload CSV/Excel files containing user data.
- **FR-5.3.2:** The bulk import must support the following fields:
  - Email (required)
  - Password (optional, can be auto-generated)
  - Role (required)
  - Status (required)
  - Station assignments (can be multiple)
  - Permission levels per station
- **FR-5.3.3:** The system must validate imported data before creating users and display validation errors.
- **FR-5.3.4:** The system must provide a preview of imported users before final confirmation.
- **FR-5.3.5:** The system must support partial imports (create valid users, skip invalid ones) with a detailed report.
- **FR-5.3.6:** The system must generate a report after import showing:
  - Number of users successfully created
  - Number of users skipped (with reasons)
  - Any errors encountered
- **FR-5.3.7:** The system must provide a template CSV/Excel file that administrators can download and fill in.

### 6. User Management UI/UX

#### 6.1 User List and Management
- **FR-6.1.1:** The system must provide a UI listing all users with filtering and search capabilities.
- **FR-6.1.2:** The user list must display:
  - Email
  - Role
  - Status
  - Assigned stations
  - Last login
  - Created date
- **FR-6.1.3:** The system must allow administrators to create, edit, and delete users from the UI.
- **FR-6.1.4:** The system must provide a user detail view showing:
  - Basic information (email, role, status)
  - Station assignments with permission levels
  - Active sessions
  - Recent audit log entries
- **FR-6.1.5:** The system must support bulk operations on users (e.g., bulk status change, bulk station assignment).

#### 6.2 Station Assignment UI
- **FR-6.2.1:** The system must provide a UI for assigning users to stations with permission levels.
- **FR-6.2.2:** The UI must display all available stations in a selectable list.
- **FR-6.2.3:** The UI must allow administrators to assign multiple stations to a user in a single operation.
- **FR-6.2.4:** The UI must clearly show which stations a user is currently assigned to.
- **FR-6.2.5:** The UI must support removing station assignments.

#### 6.3 Role Management UI
- **FR-6.3.1:** The system must provide a UI for managing roles (predefined and custom).
- **FR-6.3.2:** The role management UI must display all available roles with their permission sets.
- **FR-6.3.3:** The custom role builder UI must provide a visual interface with checkboxes for all modules and actions.
- **FR-6.3.4:** The role builder must show a summary of selected permissions before saving.
- **FR-6.3.5:** The system must prevent deletion of roles that are assigned to users.

#### 6.4 Mobile Responsiveness and Accessibility
- **FR-6.4.1:** All user management UIs must be fully responsive and work on mobile devices, tablets, and desktops.
- **FR-6.4.2:** All UIs must meet WCAG 2.1 Level AA accessibility standards.
- **FR-6.4.3:** All forms must have proper labels and ARIA attributes for screen readers.
- **FR-6.4.4:** All interactive elements must be keyboard accessible.
- **FR-6.4.5:** Color contrast must meet minimum 4.5:1 ratio for text.

### 7. API and Integration

#### 7.1 REST API Endpoints
- **FR-7.1.1:** The system must provide REST API endpoints for all user management operations:
  - Create user
  - Get user by ID
  - Get all users (with filtering)
  - Update user
  - Delete user
  - Assign user to station
  - Remove user from station
  - Create custom role
  - Get all roles
  - Update role
  - Delete role
  - Get audit logs (with filtering)
  - Get active sessions
  - Terminate session
  - Start impersonation
  - Stop impersonation
  - Bulk import users
- **FR-7.1.2:** All API endpoints must use UUIDs for resource identification.
- **FR-7.1.3:** All API endpoints must be documented with Swagger/OpenAPI annotations.
- **FR-7.1.4:** All API endpoints must enforce permission checks at the service layer.

#### 7.2 Permission Enforcement
- **FR-7.2.1:** The system must enforce permissions at the API level, not just the UI level.
- **FR-7.2.2:** API calls must automatically filter results based on the user's station assignments.
- **FR-7.2.3:** API calls that attempt to access or modify data for unauthorized stations must return 403 Forbidden.
- **FR-7.2.4:** The system must provide clear error messages when permissions are insufficient.

### 8. Database Schema

#### 8.1 User-Station Assignment Table
- **FR-8.1.1:** The system must create a `user_station_assignments` table with:
  - User ID (foreign key to users table)
  - Station ID (foreign key to stations table)
  - Permission level (enum: FULL_ACCESS, VIEW_ONLY, CUSTOM)
  - Custom permissions JSON (for CUSTOM permission level)
  - Created timestamp
  - Updated timestamp
- **FR-8.1.2:** The table must have a unique constraint on (user_id, station_id) to prevent duplicate assignments.

#### 8.2 User-Market Assignment Table (Phase 2)
- **FR-8.2.1:** The system must create a `user_market_assignments` table with:
  - User ID (foreign key to users table)
  - Market ID (foreign key to markets table)
  - Permission level
  - Custom permissions JSON
  - Created timestamp
  - Updated timestamp

#### 8.3 User-Cluster Assignment Table (Phase 2)
- **FR-8.3.1:** The system must create a `user_cluster_assignments` table with:
  - User ID (foreign key to users table)
  - Cluster ID (foreign key to clusters table)
  - Permission level
  - Custom permissions JSON
  - Created timestamp
  - Updated timestamp

#### 8.4 Custom Roles Table
- **FR-8.4.1:** The system must create a `custom_roles` table with:
  - ID (UUID primary key)
  - Name (unique)
  - Description
  - Permissions JSON (storing module and action permissions)
  - Created timestamp
  - Updated timestamp
  - Created by user ID

#### 8.5 Audit Logs Table
- **FR-8.5.1:** The system must create an `audit_logs` table with:
  - ID (UUID primary key)
  - User ID (foreign key to users table, nullable for system actions)
  - Impersonated user ID (nullable, for impersonation actions)
  - Action type (enum)
  - Resource type (string)
  - Resource ID (UUID, nullable)
  - Previous value (JSON, nullable)
  - New value (JSON, nullable)
  - IP address
  - User agent
  - Station ID (nullable, for station-specific actions)
  - Timestamp
- **FR-8.5.2:** The table must be optimized for read operations with indexes on user_id, timestamp, action_type, and resource_type.

#### 8.6 User Sessions Table
- **FR-8.6.1:** The system must create a `user_sessions` table with:
  - ID (UUID primary key)
  - User ID (foreign key to users table)
  - Session token (hashed)
  - Login timestamp
  - Last activity timestamp
  - IP address
  - User agent
  - Current station ID (nullable, for tracking which log is being edited)
  - Current resource ID (nullable, for tracking locked records)
  - Is active (boolean)
  - Expires at timestamp

### 9. Testing Requirements

#### 9.1 Unit Tests
- **FR-9.1.1:** All service methods must have unit tests with minimum 80% code coverage.
- **FR-9.1.2:** Permission enforcement logic must be thoroughly tested with various permission combinations.
- **FR-9.1.3:** Audit logging must be tested to ensure all actions are properly logged.

#### 9.2 Integration Tests
- **FR-9.2.1:** Integration tests must verify that users can only access data for their assigned stations.
- **FR-9.2.2:** Integration tests must verify that permission changes are properly enforced.
- **FR-9.2.3:** Integration tests must verify that audit logs are created for all actions.

#### 9.3 Security Testing
- **FR-9.3.1:** Security tests must verify that users cannot access unauthorized stations via direct API calls.
- **FR-9.3.2:** Security tests must verify that permission checks cannot be bypassed.
- **FR-9.3.3:** Security tests must verify that audit logs cannot be modified or deleted by users.

## Non-Goals (Out of Scope)

1. **SSO/SAML Implementation:** While the architecture will be prepared for SSO/SAML integration, the actual implementation of SSO/SAML 2.0 authentication is out of scope for this phase. This will be implemented in a future phase.

2. **IP Whitelisting:** IP-based access restrictions for log editing or other operations are out of scope for this phase. This may be added in Phase 2.

3. **Temporary Access Delegation:** The ability for users to delegate their approval rights to another user for a specific date range (e.g., vacation coverage) is out of scope for this phase.

4. **Market/Cluster-Level Permissions:** While the database schema and architecture will support market and cluster-level permissions, the full implementation of market/cluster permission inheritance is deferred to Phase 2. Station-level permissions are the priority for Phase 1.

5. **Two-Factor Authentication (2FA):** While a security best practice, 2FA implementation is out of scope for this phase.

6. **Password Expiration Policies:** Automatic password expiration and forced password changes are out of scope for this phase, though password complexity requirements are included.

7. **User Self-Service:** Users cannot modify their own permissions, station assignments, or roles. All user management must be performed by administrators.

8. **Advanced Reporting:** While audit logs can be exported, advanced analytics and reporting on user activity patterns are out of scope for this phase.

## Design Considerations

### UI/UX Design
- The user management interface should follow the existing design system and patterns used in the application.
- The custom role builder should use a clear, visual checkbox interface that is intuitive for administrators.
- The session management dashboard should provide real-time updates without requiring page refresh (consider WebSocket or Server-Sent Events).
- All forms should provide clear validation feedback and error messages.
- The bulk import UI should provide clear progress indicators and detailed error reporting.

### Performance Considerations
- Audit log queries must be optimized with proper indexing to handle large volumes of log entries efficiently.
- Session management queries should be optimized to support real-time dashboard updates without performance degradation.
- User-station permission lookups should be cached to avoid database queries on every request.
- Bulk user import should support asynchronous processing for large files to avoid timeout issues.

### Security Considerations
- All permission checks must be performed at the service layer, not just the controller layer, to prevent bypass via direct API calls.
- Audit logs must be stored in a separate database or table with restricted access to prevent tampering.
- Session tokens must be securely hashed and stored.
- Impersonation actions must be clearly logged and auditable.

## Technical Considerations

### Integration with Existing Systems
- The user management system must integrate with the existing authentication system (Spring Security).
- The system must work with the existing Station, Cluster, and Market models.
- Permission checks must be integrated into all existing controllers and services that access station-specific data.
- The audit logging system must be integrated into all existing services that perform data modifications.

### Database Migrations
- All database schema changes must be implemented using Liquibase changesets.
- Changesets must be idempotent and safe to run multiple times.
- New changesets must be added to the master changelog.
- Existing changesets must never be modified once applied to production.

### API Design
- All REST endpoints must follow RESTful conventions.
- All endpoints must use UUIDs for resource identification.
- All endpoints must return appropriate HTTP status codes.
- All endpoints must be documented with Swagger/OpenAPI annotations.

### Code Structure
- Service interfaces must be in the `services` package root.
- Service implementations must be in the `services.impl` package.
- All DTOs must end with "DTO" suffix.
- All enums must be in the `enums` package.
- All models must use Lombok annotations (`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`).

## Success Metrics

1. **Permission Enforcement Accuracy:** 100% of API calls correctly filter data based on user station assignments with zero unauthorized access incidents.

2. **Audit Trail Completeness:** 100% of user actions (logins, data changes, permission changes) are logged in the audit trail.

3. **User Management Efficiency:** Administrators can assign a user to 10 stations with different permission levels in under 2 minutes.

4. **Bulk Import Success Rate:** Bulk user import successfully processes 95% of valid user records on first attempt.

5. **Session Management:** Real-time session dashboard accurately reflects active users and their current activities with less than 5 seconds latency.

6. **Role Builder Adoption:** At least 3 custom roles are created and actively used within the first month of deployment.

7. **Impersonation Usage:** Impersonation feature is used to resolve at least 5 user access issues per month, reducing support ticket resolution time by 50%.

8. **Code Coverage:** All new code meets the 80% minimum code coverage requirement as verified by JaCoCo.

9. **Accessibility Compliance:** All user management UIs pass WCAG 2.1 Level AA accessibility audits.

10. **Mobile Usability:** User management features are fully functional on mobile devices with user satisfaction score of 4.0/5.0 or higher.

## Open Questions

1. **Permission Conflict Resolution:** If a user has conflicting permissions from multiple sources (e.g., station-level "View Only" but market-level "Full Access"), what is the precedence order? (Recommendation: Station > Market > Cluster > Tenant, with explicit overrides taking highest precedence)

2. **Audit Log Retention:** While 7 years is specified for compliance, should there be an automated archival process for older logs? (Recommendation: Implement log archival to cold storage after 1 year, maintain 7 years total)

3. **Custom Role Limits:** Should there be a maximum number of custom roles that can be created? (Recommendation: No hard limit, but monitor performance impact)

4. **Session Timeout Configuration:** Should session timeout be configurable per user role, or globally? (Recommendation: Global with optional per-role overrides)

5. **Bulk Import File Size Limits:** What is the maximum file size for bulk user import? (Recommendation: 10MB, with asynchronous processing for larger files)

6. **Impersonation Timeout:** Should impersonation sessions have a maximum duration? (Recommendation: Yes, 2 hours with warning at 1.5 hours)

7. **Audit Log Query Performance:** For systems with millions of audit log entries, should there be pagination limits or query timeouts? (Recommendation: Implement pagination with max 1000 records per page, query timeout of 30 seconds)

8. **Permission Caching Strategy:** How long should permission lookups be cached? (Recommendation: 5 minutes with cache invalidation on permission changes)

