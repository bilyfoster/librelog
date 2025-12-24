# Product Requirements Document: Users and Roles Backend REST API

## Introduction/Overview

This PRD defines the backend REST API for user management and role-based access control in the music system. The API will handle user registration, authentication, profile management, role assignment, and administrative user management functions. This is the foundational component that will enable all future features requiring user authentication and authorization.

The system will use JWT-based authentication and implement role-based access control (RBAC) with four primary roles: User, Artist, Label, and Administrator. All endpoints will be secured with proper authorization checks, and the API will maintain audit trails for security-sensitive operations.

## Goals

1. **Secure User Authentication**: Provide secure user registration and login with JWT-based authentication
2. **User Profile Management**: Enable users to view and update their profile information
3. **Role-Based Access Control**: Implement a robust RBAC system with four roles (User, Artist, Label, Administrator)
4. **Administrative User Management**: Provide administrators with tools to manage user accounts and roles
5. **Security and Audit**: Maintain comprehensive audit trails and enforce security best practices
6. **Performance**: Achieve API response times under 200ms for all user management endpoints

## User Stories

### US-1: User Registration
As a potential user, I want to register with my email address so that I can access the platform and use music-related features.

### US-2: User Authentication
As a registered user, I want to log in with my email and password so that I can access my account and use platform features.

### US-3: Profile Management
As a user, I want to view and edit my profile information so that my account details are accurate and current.

### US-4: Password Management
As a user, I want to change my password and reset it if forgotten so that I can maintain account security.

### US-5: Role Assignment (Admin)
As a system administrator, I want to assign Artist, Label, or other roles to users so that they can access role-specific features and permissions.

### US-6: Artist Profile Management
As an artist, I want to edit my artist profile so that my information is accurate and up-to-date.

### US-7: Song Submission and Rights Management
As an artist, I want to submit songs and manage their rights so that I can control my music content and distribution.

### US-8: Label Artist Management
As a label, I want to manage artist profiles and their songs so that I can manage my roster and their content.

### US-9: User Account Management (Admin)
As a system administrator, I want to view and manage all user accounts so that I can provide support and maintain system security.

### US-10: Account Status Management
As a system administrator, I want to suspend or activate user accounts so that I can maintain system integrity.

## Functional Requirements

### Authentication & Registration

1. The system must provide a POST endpoint `/api/auth/register` for user registration
2. Registration must require: email (unique), password, and name
3. Email addresses must be validated for format and uniqueness
4. Passwords must meet minimum complexity requirements (minimum 8 characters, at least one uppercase, one lowercase, one number, one special character)
5. Passwords must be hashed using a secure algorithm (BCrypt) before storage
6. The system must send an email verification link upon successful registration
7. User accounts must remain inactive until email verification is completed
8. The system must provide a POST endpoint `/api/auth/login` for user authentication
9. Login must accept email and password and return a JWT token upon successful authentication
10. JWT tokens must include user ID, email, and roles in the claims
11. JWT tokens must have a configurable expiration time (default: 24 hours)
12. The system must provide a POST endpoint `/api/auth/refresh` for token refresh
13. The system must provide a POST endpoint `/api/auth/logout` for token invalidation
14. Failed login attempts must be logged and tracked
15. The system must implement account lockout after 5 failed login attempts within 15 minutes
16. The system must provide a POST endpoint `/api/auth/forgot-password` to initiate password reset
17. The system must provide a POST endpoint `/api/auth/reset-password` to complete password reset with token validation
18. Password reset tokens must expire after 1 hour

### User Profile Management

19. The system must provide a GET endpoint `/api/users/me` to retrieve the authenticated user's profile
20. The system must provide a PUT endpoint `/api/users/me` to update the authenticated user's profile
21. Users must be able to update: name, email, and contact information
22. Email changes must require verification via email confirmation
23. The system must provide a POST endpoint `/api/users/me/profile-picture` for profile picture upload
24. Profile pictures must be validated for file type (JPEG, PNG) and size (max 5MB)
25. The system must provide a DELETE endpoint `/api/users/me/profile-picture` to remove profile picture
26. The system must provide a PUT endpoint `/api/users/me/password` to change password
27. Password changes must require current password verification
28. The system must provide a DELETE endpoint `/api/users/me` for account deletion/deactivation
29. Account deletion must be soft-delete (mark as deleted, retain data for audit)
30. The system must provide a GET endpoint `/api/users/me/privacy-settings` to retrieve privacy settings
31. The system must provide a PUT endpoint `/api/users/me/privacy-settings` to update privacy settings

### Artist Profile Management

32. The system must provide a GET endpoint `/api/artists/me` to retrieve the authenticated artist's profile
33. The system must provide a PUT endpoint `/api/artists/me` to update the authenticated artist's profile
34. Artists must be able to update: artist name, bio, genre, social media links, contact information
35. The system must provide a POST endpoint `/api/artists/me/profile-image` for artist profile image upload
36. Profile images must be validated for file type (JPEG, PNG) and size (max 10MB)
37. The system must provide a DELETE endpoint `/api/artists/me/profile-image` to remove profile image
38. Only users with ARTIST role can access artist profile endpoints

### Song Submission and Rights Management

39. The system must provide a POST endpoint `/api/artists/me/songs` for artists to submit songs
40. Song submission must require: title, audio file, genre, and release date
41. Song submission may optionally include: album, track number, composer, publisher, copyright, lyrics, comments, BPM, key, ISRC, UPC
42. The system must extract MP3 metadata (title, artist, album, genre, year, track number, duration, bitrate, sample rate, channels, composer, publisher, copyright, comments, BPM, key) from uploaded audio files when available
43. Audio files must be validated for file type (MP3, WAV, FLAC) and size (max 100MB)
44. The system must provide a GET endpoint `/api/artists/me/songs` to list all songs by the authenticated artist
45. The system must provide a GET endpoint `/api/artists/me/songs/{songId}` to retrieve song details including all metadata
46. The system must provide a PUT endpoint `/api/artists/me/songs/{songId}` to update song information and metadata
47. The system must provide a POST endpoint `/api/artists/me/songs/{songId}/album-art` for album art upload
48. Album art must be validated for file type (JPEG, PNG) and size (max 5MB)
49. The system must provide a DELETE endpoint `/api/artists/me/songs/{songId}/album-art` to remove album art
50. The system must provide a DELETE endpoint `/api/artists/me/songs/{songId}` to delete songs
51. The system must provide a POST endpoint `/api/artists/me/songs/{songId}/rights` to manage song rights
52. Song rights must include: radio/streaming rights, podcast rights, commercial rights, available for mix shows
53. The system must provide a GET endpoint `/api/artists/me/songs/{songId}/rights` to retrieve song rights
54. The system must provide a PUT endpoint `/api/artists/me/songs/{songId}/rights` to update song rights
55. Only users with ARTIST role can submit and manage their own songs
56. All song submissions and rights changes must be logged in audit trail

### Label Profile Management

52. The system must provide a GET endpoint `/api/labels/me` to retrieve the authenticated label's profile
53. The system must provide a PUT endpoint `/api/labels/me` to update the authenticated label's profile
54. Labels must be able to update: label name, description, logo, website URL, contact information, address, social media links, founded year
55. The system must provide a POST endpoint `/api/labels/me/logo` for label logo upload
56. Logos must be validated for file type (JPEG, PNG) and size (max 10MB)
57. The system must provide a DELETE endpoint `/api/labels/me/logo` to remove logo
58. Only users with LABEL role can access label profile endpoints

### Label Artist and Song Management

63. The system must provide a GET endpoint `/api/labels/me/artists` to list all artists managed by the authenticated label
64. The system must provide a GET endpoint `/api/labels/me/artists/{artistId}` to view artist details
65. The system must provide a PUT endpoint `/api/labels/me/artists/{artistId}` to update artist profiles
66. The system must provide a GET endpoint `/api/labels/me/artists/{artistId}/songs` to list all songs for an artist
67. The system must provide a POST endpoint `/api/labels/me/artists/{artistId}/songs` to add songs to an artist
68. The system must provide a PUT endpoint `/api/labels/me/artists/{artistId}/songs/{songId}` to update artist songs and metadata
69. The system must provide a POST endpoint `/api/labels/me/artists/{artistId}/songs/{songId}/album-art` for album art upload
70. The system must provide a DELETE endpoint `/api/labels/me/artists/{artistId}/songs/{songId}` to remove songs
71. The system must provide a POST endpoint `/api/labels/me/artists/{artistId}/songs/{songId}/rights` to manage song rights
72. The system must provide a GET endpoint `/api/labels/me/artists/{artistId}/songs/{songId}/rights` to retrieve song rights
73. The system must provide a PUT endpoint `/api/labels/me/artists/{artistId}/songs/{songId}/rights` to update song rights
74. Only users with LABEL role can manage artists and their songs
75. Labels can only manage artists that are assigned to them
76. All label management actions must be logged in audit trail

### Role Management

72. The system must define four roles: USER, ARTIST, LABEL, ADMINISTRATOR
73. All users must have at least the USER role by default
74. The system must provide a GET endpoint `/api/admin/users/{userId}/roles` to retrieve user roles (admin only)
75. The system must provide a PUT endpoint `/api/admin/users/{userId}/roles` to assign roles to users (admin only)
76. Administrators can assign any role (ARTIST, LABEL, ADMINISTRATOR) to users
77. Users can have multiple roles simultaneously (e.g., a user can be both USER and ARTIST)
78. Only administrators can perform role assignment operations
79. The system must send email notification to user when their role is changed
80. All role changes must be logged in audit trail with: user ID, admin ID, timestamp, old roles, new roles
81. Users must retain their basic account information after role change
82. The system must enforce role-based access control on all endpoints
83. The system must provide a POST endpoint `/api/admin/labels/{labelId}/artists/{artistId}` to assign artists to labels (admin only)
84. The system must provide a DELETE endpoint `/api/admin/labels/{labelId}/artists/{artistId}` to remove artists from labels (admin only)

### Administrative User Management

85. The system must provide a GET endpoint `/api/admin/users` to list all users (admin only)
86. The endpoint must support pagination with page number and page size parameters
87. The endpoint must support filtering by: email, name, role, account status
88. The endpoint must support sorting by: created date, email, name
89. The system must provide a GET endpoint `/api/admin/users/{userId}` to view user details (admin only)
90. The system must provide a PUT endpoint `/api/admin/users/{userId}/suspend` to suspend user accounts (admin only)
91. The system must provide a PUT endpoint `/api/admin/users/{userId}/activate` to activate user accounts (admin only)
92. The system must provide a POST endpoint `/api/admin/users/{userId}/reset-password` to reset user passwords (admin only)
93. Admin password resets must generate a temporary password and send it via email
94. The system must provide a GET endpoint `/api/admin/users/{userId}/activity` to view user activity logs (admin only)
95. The system must provide a POST endpoint `/api/admin/users/bulk-suspend` for bulk account suspension (admin only)
96. The system must provide a POST endpoint `/api/admin/users/bulk-activate` for bulk account activation (admin only)
97. The system must provide a GET endpoint `/api/admin/users/export` to export user data in CSV format (admin only)
98. All administrative actions must be logged in audit trail

### Permissions and Access Control

99. The system must implement permission checks on all endpoints based on user roles
100. USER role permissions: view own profile, update own profile
101. ARTIST role permissions: all USER permissions plus edit own artist profile, submit songs, manage song rights, delete own songs
102. LABEL role permissions: all USER permissions plus create/manage label profile, manage assigned artist profiles, add songs to assigned artists, update artist songs, manage song rights for assigned artists
103. ADMINISTRATOR role permissions: all USER permissions plus manage all users, manage roles, assign artists to labels, access admin endpoints
104. Artists can only manage their own profiles and songs
105. Labels can only manage artists that are assigned to them
106. Users with multiple roles have the union of all their role permissions
107. The system must return 403 Forbidden for unauthorized access attempts
108. The system must log all unauthorized access attempts

### Data Validation and Error Handling

109. All input data must be validated using Pydantic models and field validators
110. The system must return 400 Bad Request with validation error details for invalid input
111. The system must return 401 Unauthorized for unauthenticated requests
112. The system must return 404 Not Found for non-existent resources
113. The system must return 409 Conflict for duplicate email addresses
114. The system must return 403 Forbidden when artists try to access other artists' content
115. The system must return 403 Forbidden when labels try to manage artists not assigned to them
116. Error responses must follow a consistent format with error code, message, and timestamp
117. All errors must be logged with appropriate log levels

### Audit and Logging

118. The system must log all authentication attempts (success and failure)
119. The system must log all role changes with full details
120. The system must log all administrative actions (account suspension, activation, password resets)
121. The system must log all song submissions, updates, and deletions
122. The system must log all song rights changes
123. The system must log all label profile updates
124. The system must log all label management actions (artist assignments, song management)
125. Audit logs must include: user ID, action type, timestamp, IP address, request details
126. The system must provide a GET endpoint `/api/admin/audit-logs` to retrieve audit logs (admin only)
127. Audit logs must support filtering by user ID, action type, and date range

### Performance Requirements

128. All API endpoints must respond within 200ms under normal load conditions
129. Authentication endpoints must handle at least 100 requests per second
130. User profile endpoints must handle at least 50 requests per second
131. Song upload endpoints must handle file uploads up to 100MB
132. Database queries must be optimized with proper indexes
133. The system must implement caching for frequently accessed user data (optional, future enhancement)

## Non-Goals (Out of Scope)

1. **Frontend Implementation**: This PRD covers only the backend REST API. Frontend UI implementation is out of scope.
2. **Email Service Implementation**: Email sending functionality will use an external email service. The API will trigger email notifications, but email service configuration is out of scope.
3. **Mobile App Backend**: This API is designed for web application use. Mobile-specific endpoints or optimizations are out of scope.
4. **Music Distribution**: Integration with music distribution platforms (Spotify, Apple Music, etc.) is deferred to a later phase.
5. **Audio Processing**: Audio file processing, transcoding, and format conversion are out of scope.
6. **Real-time Features**: WebSocket support, real-time notifications, and live updates are not needed initially.
7. **Reporting and Analytics**: Advanced reporting APIs and analytics are deferred to a later phase.
8. **Social Authentication**: OAuth2 integration with social providers (Google, Facebook) is out of scope.
9. **Multi-factor Authentication**: 2FA/MFA is not included in this phase.
10. **API Versioning**: Single version API without versioning strategy for this initial release.

## Design Considerations

### API Structure
- Base path: `/api`
- Authentication endpoints: `/api/auth/*`
- User endpoints: `/api/users/*`
- Artist endpoints: `/api/artists/*`
- Label endpoints: `/api/labels/*`
- Admin endpoints: `/api/admin/*`
- All endpoints must follow RESTful conventions

### Request/Response Format
- Content-Type: `application/json`
- All requests and responses must use JSON format
- Date/time fields must use ISO 8601 format
- UUIDs must be used for all resource identifiers

### Security Headers
- All responses must include security headers (CORS, X-Content-Type-Options, etc.)
- JWT tokens must be sent in Authorization header: `Authorization: Bearer <token>`

### Database Design
- All tables must use UUID primary keys
- Audit fields required: `created_at`, `updated_at`, `created_by`, `updated_by`
- Soft delete support for user accounts
- Proper indexes on email, user_id, and role fields

## Technical Considerations

### Technology Stack
- **Framework**: FastAPI (Python)
- **Security**: OAuth2 with JWT (python-jose)
- **Database**: PostgreSQL
- **ORM**: SQLAlchemy 2.0 (async)
- **Migration**: Alembic for database schema management
- **Validation**: Pydantic models
- **Documentation**: FastAPI automatic OpenAPI/Swagger

### Dependencies
- FastAPI for web framework and API routing
- python-jose for JWT token generation and validation
- passlib[bcrypt] for password hashing
- SQLAlchemy for database access (async)
- Alembic for database migrations
- Pydantic for data validation and serialization

### Integration Points
- External email service (to be configured separately)
- Database connection (PostgreSQL)
- File storage service for audio files and images (S3, local filesystem, etc.)
- Future: Music distribution platforms
- Future: Payment processing module

### Security Requirements
- All passwords must be hashed using BCrypt (via passlib)
- JWT tokens must use RS256 or HS256 algorithm (via python-jose)
- All endpoints (except registration, login, password reset) must require authentication via OAuth2PasswordBearer
- HTTPS must be enforced in production
- SQL injection prevention through SQLAlchemy parameterized queries
- XSS prevention through Pydantic input validation and proper output encoding

### Database Schema
- `users` table: id (UUID), email, password_hash, name, email_verified, account_status, created_at, updated_at
- `roles` table: id (UUID), name, description
- `user_roles` table: user_id (UUID), role_id (UUID), assigned_at, assigned_by
- `artist_profiles` table: id (UUID), user_id (UUID), artist_name, bio, genre, profile_image_url, social_media_links (JSON), created_at, updated_at
- `label_profiles` table: id (UUID), user_id (UUID), label_name, description, logo_url, website_url, contact_email, contact_phone, address, city, state, country, postal_code, social_media_links (JSON), founded_year, created_at, updated_at
- `songs` table: id (UUID), artist_id (UUID), title, album, audio_file_url, genre, year, track_number, duration (seconds), bitrate (kbps), sample_rate (Hz), channels, file_size (bytes), album_art_url, composer, publisher, copyright, lyrics, comments, bpm (beats per minute), key, isrc (International Standard Recording Code), upc (Universal Product Code), created_at, updated_at
- `song_rights` table: id (UUID), song_id (UUID), radio_streaming_rights (boolean), podcast_rights (boolean), commercial_rights (boolean), mix_show_rights (boolean), created_at, updated_at
- `label_artists` table: id (UUID), label_id (UUID), artist_id (UUID), assigned_at, assigned_by
- `audit_logs` table: id (UUID), user_id (UUID), action_type, action_details, ip_address, timestamp

## Success Metrics

1. **Performance**: 95% of API requests respond within 200ms
2. **Reliability**: 99.9% uptime for authentication endpoints
3. **Security**: Zero successful unauthorized access attempts
4. **User Experience**: Successful registration rate > 95%
5. **Adoption**: Support for 1,000+ concurrent authenticated users
6. **Error Rate**: API error rate < 1% (excluding client errors like 400, 401, 403)

## Open Questions

1. **Email Service Provider**: Which email service will be integrated? (SendGrid, AWS SES, etc.)
2. **JWT Token Expiration**: Should refresh tokens have longer expiration? What should be the refresh token expiration time?
3. **Account Lockout Duration**: How long should accounts remain locked after failed login attempts? (Suggested: 30 minutes)
4. **Profile Picture Storage**: Where should profile pictures be stored? (Local filesystem, S3, etc.)
5. **Audit Log Retention**: How long should audit logs be retained? (Suggested: 1 year)
6. **Password History**: Should the system prevent password reuse? If yes, how many previous passwords?
7. **Session Management**: Should the system support multiple concurrent sessions per user? Should there be a limit?
8. **Bulk Operations Limit**: What is the maximum number of users that can be processed in a single bulk operation?
9. **Export Format**: Should user data export support formats other than CSV? (JSON, Excel)

