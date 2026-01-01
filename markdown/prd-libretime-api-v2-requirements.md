# Product Requirements Document: LibreTime API v2 Requirements for LibreLog Integration

**Document Version:** 1.0  
**Date:** January 1, 2026  
**Author:** LibreLog Development Team  
**Status:** Draft for Review

---

## Executive Summary

This document outlines the requirements for LibreTime API v2 to support seamless integration with LibreLog, a radio traffic management system. The integration requires robust authentication mechanisms, health/status endpoints, file management capabilities, and comprehensive error handling to ensure reliable synchronization between the two systems.

---

## 1. Background and Context

### 1.1 Purpose
LibreLog needs to integrate with LibreTime to:
- Synchronize audio files between systems
- Export broadcast logs (clocks) to LibreTime
- Monitor sync status and health
- Provide administrators with visibility into integration status

### 1.2 Integration Scope
- **File Synchronization:** Upload, download, list, and query audio files
- **Log Export:** Export broadcast schedules (clocks) to LibreTime
- **Status Monitoring:** Real-time health checks and status reporting
- **Error Handling:** Comprehensive error responses for troubleshooting

---

## 2. Authentication and Token Management

### 2.1 JWT Token Generation

**Requirement:** LibreTime API v2 MUST provide a mechanism for users to generate and manage JWT tokens for API authentication.

#### 2.1.1 Token Generation Endpoint

**Endpoint:** `POST /api/v2/auth/tokens`

**Request Body:**
```json
{
  "name": "LibreLog Integration Token",
  "description": "Token for LibreLog integration",
  "expires_in": 31536000,  // Optional: seconds until expiration (1 year default)
  "scopes": ["files:read", "files:write", "logs:write"]  // Optional: token scopes
}
```

**Response (201 Created):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_id": "uuid-here",
  "name": "LibreLog Integration Token",
  "created_at": "2026-01-01T12:00:00Z",
  "expires_at": "2027-01-01T12:00:00Z",
  "scopes": ["files:read", "files:write", "logs:write"]
}
```

**Error Responses:**
- `400 Bad Request`: Invalid request body
- `401 Unauthorized`: User not authenticated
- `403 Forbidden`: User lacks permission to create tokens
- `429 Too Many Requests`: Rate limit exceeded

#### 2.1.2 Token List Endpoint

**Endpoint:** `GET /api/v2/auth/tokens`

**Response (200 OK):**
```json
{
  "tokens": [
    {
      "token_id": "uuid-here",
      "name": "LibreLog Integration Token",
      "created_at": "2026-01-01T12:00:00Z",
      "expires_at": "2027-01-01T12:00:00Z",
      "last_used_at": "2026-01-15T10:30:00Z",
      "scopes": ["files:read", "files:write", "logs:write"]
    }
  ],
  "total": 1
}
```

#### 2.1.3 Token Revocation Endpoint

**Endpoint:** `DELETE /api/v2/auth/tokens/{token_id}`

**Response:**
- `204 No Content`: Token successfully revoked
- `404 Not Found`: Token not found
- `403 Forbidden`: User lacks permission to revoke this token

#### 2.1.4 Web UI Token Management

**Requirement:** LibreTime web interface MUST provide a user-accessible section for managing API tokens.

**Location:** `Settings → API → Tokens` or `User Settings → API Tokens`

**Features:**
- List all user's API tokens
- Generate new tokens with custom names and descriptions
- View token creation date, expiration date, and last used timestamp
- Revoke tokens
- Copy token to clipboard (one-time display for security)
- Show token scopes/permissions

**Security Requirements:**
- Tokens MUST be displayed only once upon creation
- Tokens MUST be masked in the UI (show only first 8 characters + "...")
- Token revocation MUST be immediate and irreversible
- Expired tokens MUST be automatically invalidated

### 2.2 Token Authentication

**Requirement:** All API endpoints MUST accept JWT tokens in the `Authorization` header.

**Format:** `Authorization: Bearer {jwt_token}`

**Token Validation:**
- Tokens MUST be validated on every request
- Expired tokens MUST return `401 Unauthorized`
- Revoked tokens MUST return `401 Unauthorized`
- Invalid token format MUST return `401 Unauthorized`

**Error Response (401 Unauthorized):**
```json
{
  "error": "unauthorized",
  "message": "Invalid or expired token",
  "error_code": "TOKEN_INVALID"
}
```

---

## 3. Health and Status Endpoints

### 3.1 Health Check Endpoint

**Requirement:** LibreTime API v2 MUST provide a health check endpoint for monitoring system status.

**Endpoint:** `GET /api/v2/health`

**Response (200 OK):**
```json
{
  "status": "healthy",
  "version": "4.0.0",
  "timestamp": "2026-01-01T12:00:00Z",
  "components": {
    "database": {
      "status": "healthy",
      "response_time_ms": 5
    },
    "storage": {
      "status": "healthy",
      "free_space_gb": 500,
      "total_space_gb": 1000
    },
    "api": {
      "status": "healthy",
      "uptime_seconds": 86400
    }
  }
}
```

**Response (503 Service Unavailable):**
```json
{
  "status": "unhealthy",
  "version": "4.0.0",
  "timestamp": "2026-01-01T12:00:00Z",
  "components": {
    "database": {
      "status": "unhealthy",
      "error": "Connection timeout"
    }
  }
}
```

**Requirements:**
- Endpoint MUST be publicly accessible (no authentication required)
- Response MUST be cacheable (max-age: 30 seconds)
- MUST return 200 if all components are healthy
- MUST return 503 if any critical component is unhealthy

### 3.2 API Status Endpoint

**Requirement:** LibreTime API v2 MUST provide an authenticated status endpoint with detailed system information.

**Endpoint:** `GET /api/v2/status`

**Authentication:** Required (JWT token)

**Response (200 OK):**
```json
{
  "api_version": "2.0.0",
  "libretime_version": "4.0.0",
  "server_time": "2026-01-01T12:00:00Z",
  "timezone": "America/New_York",
  "features": {
    "file_upload": true,
    "file_download": true,
    "log_export": true,
    "webhooks": true
  },
  "limits": {
    "max_file_size_mb": 500,
    "max_files_per_request": 100,
    "rate_limit_per_minute": 60
  },
  "user": {
    "id": "user-uuid",
    "username": "admin",
    "permissions": ["files:read", "files:write", "logs:write"]
  }
}
```

### 3.3 Connection Test Endpoint

**Requirement:** LibreTime API v2 MUST provide an endpoint specifically for testing API connectivity and authentication.

**Endpoint:** `GET /api/v2/test/connection`

**Authentication:** Required (JWT token)

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Connection successful",
  "response_time_ms": 45,
  "server_time": "2026-01-01T12:00:00Z",
  "authenticated_user": {
    "id": "user-uuid",
    "username": "admin"
  }
}
```

**Response (401 Unauthorized):**
```json
{
  "success": false,
  "message": "Authentication failed",
  "error_code": "AUTH_FAILED"
}
```

**Requirements:**
- Endpoint MUST be lightweight (minimal processing)
- Response MUST include response time in milliseconds
- MUST validate token and return user information if authenticated

---

## 4. Error Handling and Response Standards

### 4.1 Standard Error Response Format

**Requirement:** All error responses MUST follow a consistent format.

**Standard Error Response:**
```json
{
  "error": "error_type",
  "message": "Human-readable error message",
  "error_code": "ERROR_CODE",
  "details": {
    "field": "additional error details"
  },
  "timestamp": "2026-01-01T12:00:00Z",
  "request_id": "request-uuid"
}
```

### 4.2 HTTP Status Codes

**Requirement:** LibreTime API v2 MUST use appropriate HTTP status codes.

| Status Code | Usage |
|------------|-------|
| 200 OK | Successful GET, PUT, PATCH requests |
| 201 Created | Successful POST requests creating resources |
| 204 No Content | Successful DELETE requests |
| 400 Bad Request | Invalid request body or parameters |
| 401 Unauthorized | Missing or invalid authentication |
| 403 Forbidden | Authenticated but lacks permission |
| 404 Not Found | Resource not found |
| 409 Conflict | Resource conflict (e.g., duplicate file) |
| 413 Payload Too Large | File size exceeds limit |
| 422 Unprocessable Entity | Valid format but semantic errors |
| 429 Too Many Requests | Rate limit exceeded |
| 500 Internal Server Error | Server-side error |
| 503 Service Unavailable | Service temporarily unavailable |

### 4.3 Error Codes

**Requirement:** All errors MUST include a machine-readable error code.

**Standard Error Codes:**
- `TOKEN_INVALID`: Invalid or expired JWT token
- `TOKEN_MISSING`: Authorization header missing
- `PERMISSION_DENIED`: User lacks required permission
- `RESOURCE_NOT_FOUND`: Requested resource does not exist
- `VALIDATION_ERROR`: Request validation failed
- `FILE_TOO_LARGE`: File size exceeds maximum
- `DUPLICATE_RESOURCE`: Resource already exists
- `RATE_LIMIT_EXCEEDED`: Too many requests
- `STORAGE_ERROR`: File storage operation failed
- `DATABASE_ERROR`: Database operation failed
- `INTERNAL_ERROR`: Unexpected server error

### 4.4 Rate Limiting

**Requirement:** LibreTime API v2 MUST implement rate limiting with clear headers.

**Rate Limit Headers:**
```
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 45
X-RateLimit-Reset: 1641024000
```

**Rate Limit Response (429 Too Many Requests):**
```json
{
  "error": "rate_limit_exceeded",
  "message": "Rate limit exceeded. Please try again later.",
  "error_code": "RATE_LIMIT_EXCEEDED",
  "retry_after": 60
}
```

---

## 5. File Management Endpoints

### 5.1 File Upload

**Endpoint:** `POST /api/v2/files`

**Authentication:** Required (JWT token with `files:write` scope)

**Request:**
- Content-Type: `multipart/form-data`
- Body: File binary data + metadata

**Request Fields:**
```json
{
  "file": "<binary file data>",
  "name": "audio_file.mp3",
  "description": "Morning show intro",
  "metadata": {
    "artist": "Artist Name",
    "title": "Song Title",
    "duration": 180
  }
}
```

**Response (201 Created):**
```json
{
  "id": "file-uuid",
  "name": "audio_file.mp3",
  "size": 5242880,
  "mime_type": "audio/mpeg",
  "uploaded_at": "2026-01-01T12:00:00Z",
  "url": "/api/v2/files/file-uuid"
}
```

### 5.2 File Download

**Endpoint:** `GET /api/v2/files/{file_id}`

**Authentication:** Required (JWT token with `files:read` scope)

**Response:**
- Content-Type: Appropriate MIME type for file
- Content-Disposition: `attachment; filename="audio_file.mp3"`
- Body: File binary data

**Error Responses:**
- `404 Not Found`: File does not exist
- `403 Forbidden`: User lacks permission to access file

### 5.3 File List

**Endpoint:** `GET /api/v2/files`

**Authentication:** Required (JWT token with `files:read` scope)

**Query Parameters:**
- `page`: Page number (default: 1)
- `per_page`: Items per page (default: 20, max: 100)
- `sort`: Sort field (default: "uploaded_at")
- `order`: Sort order ("asc" or "desc", default: "desc")
- `search`: Search term (searches name and description)

**Response (200 OK):**
```json
{
  "files": [
    {
      "id": "file-uuid",
      "name": "audio_file.mp3",
      "size": 5242880,
      "mime_type": "audio/mpeg",
      "uploaded_at": "2026-01-01T12:00:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "per_page": 20,
    "total": 150,
    "total_pages": 8
  }
}
```

### 5.4 File Query/Search

**Endpoint:** `GET /api/v2/files/search`

**Authentication:** Required (JWT token with `files:read` scope)

**Query Parameters:**
- `q`: Search query
- `mime_type`: Filter by MIME type
- `min_size`: Minimum file size in bytes
- `max_size`: Maximum file size in bytes
- `uploaded_after`: ISO 8601 datetime
- `uploaded_before`: ISO 8601 datetime

**Response:** Same format as File List endpoint

---

## 6. Log Export Endpoints

### 6.1 Export Log/Clock

**Endpoint:** `POST /api/v2/logs/export`

**Authentication:** Required (JWT token with `logs:write` scope)

**Request Body:**
```json
{
  "name": "Morning Show - 2026-01-01",
  "start_time": "2026-01-01T06:00:00Z",
  "end_time": "2026-01-01T10:00:00Z",
  "timezone": "America/New_York",
  "events": [
    {
      "time": "06:00:00",
      "type": "show",
      "title": "Morning Show",
      "duration": 240
    },
    {
      "time": "06:04:00",
      "type": "song",
      "title": "Song Title",
      "artist": "Artist Name",
      "duration": 180
    }
  ]
}
```

**Response (201 Created):**
```json
{
  "id": "log-uuid",
  "name": "Morning Show - 2026-01-01",
  "status": "imported",
  "created_at": "2026-01-01T12:00:00Z",
  "events_count": 45
}
```

---

## 7. API Discovery and Documentation

### 7.1 API Information Endpoint

**Requirement:** LibreTime API v2 MUST provide an endpoint that returns API information and available endpoints.

**Endpoint:** `GET /api/v2/info`

**Authentication:** Optional (provides more details if authenticated)

**Response (200 OK):**
```json
{
  "api_version": "2.0.0",
  "libretime_version": "4.0.0",
  "base_url": "https://libretime.example.org/api/v2",
  "endpoints": [
    {
      "path": "/api/v2/health",
      "method": "GET",
      "description": "Health check endpoint",
      "authentication": false
    },
    {
      "path": "/api/v2/files",
      "method": "GET",
      "description": "List files",
      "authentication": true,
      "scopes": ["files:read"]
    }
  ],
  "documentation_url": "https://libretime.example.org/api/v2/docs"
}
```

### 7.2 OpenAPI/Swagger Documentation

**Requirement:** LibreTime API v2 MUST provide OpenAPI 3.0 specification.

**Endpoint:** `GET /api/v2/docs/openapi.json`

**Response:** OpenAPI 3.0 JSON specification

**Alternative:** Swagger UI at `/api/v2/docs`

---

## 8. Webhook Support (Optional but Recommended)

### 8.1 Webhook Registration

**Endpoint:** `POST /api/v2/webhooks`

**Request Body:**
```json
{
  "url": "https://librelog.example.org/webhooks/libretime",
  "events": ["file.uploaded", "file.deleted", "log.imported"],
  "secret": "webhook-secret-token"
}
```

**Response (201 Created):**
```json
{
  "id": "webhook-uuid",
  "url": "https://librelog.example.org/webhooks/libretime",
  "events": ["file.uploaded", "file.deleted", "log.imported"],
  "created_at": "2026-01-01T12:00:00Z",
  "status": "active"
}
```

### 8.2 Webhook Events

**Supported Events:**
- `file.uploaded`: File successfully uploaded
- `file.deleted`: File deleted
- `log.imported`: Log/clock imported
- `log.updated`: Log/clock updated

---

## 9. Testing and Validation Requirements

### 9.1 Test Endpoints

**Requirement:** LibreTime API v2 SHOULD provide test endpoints for integration validation.

**Endpoint:** `GET /api/v2/test/endpoints`

**Response:**
```json
{
  "endpoints": [
    {
      "path": "/api/v2/files",
      "method": "GET",
      "status": "working",
      "response_time_ms": 45,
      "last_tested": "2026-01-01T12:00:00Z"
    }
  ]
}
```

### 9.2 Validation Endpoint

**Endpoint:** `POST /api/v2/test/validate`

**Request Body:**
```json
{
  "endpoint": "/api/v2/files",
  "method": "GET",
  "expected_status": 200
}
```

**Response:**
```json
{
  "valid": true,
  "actual_status": 200,
  "response_time_ms": 45,
  "message": "Endpoint validation successful"
}
```

---

## 10. Performance and Reliability Requirements

### 10.1 Response Time Requirements

- Health check endpoint: < 100ms
- Status endpoint: < 200ms
- File list endpoint: < 500ms (for 20 items)
- File upload endpoint: Depends on file size, but should accept files up to 500MB
- File download endpoint: Streaming support for large files

### 10.2 Availability Requirements

- API MUST be available 99.9% of the time
- Health check endpoint MUST respond even during maintenance
- Graceful degradation: Non-critical endpoints may be unavailable during maintenance

### 10.3 Scalability Requirements

- API MUST support at least 100 concurrent requests
- File upload MUST support files up to 500MB
- Pagination MUST be implemented for all list endpoints

---

## 11. Security Requirements

### 11.1 Authentication Security

- JWT tokens MUST use strong signing algorithm (HS256 or RS256)
- Tokens MUST have expiration times
- Token secrets MUST be stored securely
- Token revocation MUST be immediate

### 11.2 HTTPS Requirement

- All API endpoints MUST be accessible only via HTTPS
- HTTP requests MUST redirect to HTTPS
- TLS 1.2 or higher required

### 11.3 Input Validation

- All input MUST be validated
- File uploads MUST be validated for type and size
- SQL injection prevention
- XSS prevention in error messages

### 11.4 CORS Configuration

- CORS MUST be configurable
- Default: Same-origin only
- Allow configuration for trusted domains

---

## 12. Documentation Requirements

### 12.1 API Documentation

- Complete OpenAPI 3.0 specification
- Interactive API documentation (Swagger UI)
- Code examples for common operations
- Authentication guide
- Error handling guide

### 12.2 Integration Guide

- Step-by-step integration guide
- Example code in multiple languages (Python, JavaScript, Java)
- Common integration patterns
- Troubleshooting guide

---

## 13. Versioning and Compatibility

### 13.1 API Versioning

- API version MUST be included in URL path: `/api/v2/`
- Breaking changes MUST result in new version
- Deprecated endpoints MUST be marked and supported for at least 6 months

### 13.2 Backward Compatibility

- Non-breaking changes SHOULD be backward compatible
- New optional fields in responses are acceptable
- Removing required fields requires version bump

---

## 14. Monitoring and Observability

### 14.1 Request Logging

- All API requests SHOULD be logged
- Include: timestamp, method, path, status, response_time, user_id
- Sensitive data (tokens, passwords) MUST be masked in logs

### 14.2 Metrics

- Request count by endpoint
- Response time percentiles
- Error rate by endpoint
- Authentication success/failure rate

### 14.3 Alerts

- High error rate alerts
- Slow response time alerts
- Authentication failure spikes

---

## 15. Implementation Priority

### Phase 1 (Critical - Required for Initial Integration)
1. JWT token generation and management (Section 2)
2. Health check endpoint (Section 3.1)
3. API status endpoint (Section 3.2)
4. Connection test endpoint (Section 3.3)
5. Standard error handling (Section 4)
6. Basic file upload/download (Section 5.1, 5.2)

### Phase 2 (Important - Required for Full Integration)
7. File list and search (Section 5.3, 5.4)
8. Log export endpoint (Section 6)
9. API discovery endpoint (Section 7.1)
10. OpenAPI documentation (Section 7.2)

### Phase 3 (Nice to Have - Enhanced Features)
11. Webhook support (Section 8)
12. Test endpoints (Section 9)
13. Advanced monitoring (Section 14)

---

## 16. Acceptance Criteria

### 16.1 Token Management
- [ ] Users can generate JWT tokens via API
- [ ] Users can generate JWT tokens via web UI
- [ ] Users can list and revoke tokens
- [ ] Tokens authenticate API requests correctly
- [ ] Expired tokens are rejected
- [ ] Revoked tokens are rejected

### 16.2 Health and Status
- [ ] Health endpoint returns 200 when system is healthy
- [ ] Health endpoint returns 503 when system is unhealthy
- [ ] Status endpoint returns API version and system info
- [ ] Connection test endpoint validates authentication

### 16.3 Error Handling
- [ ] All errors follow standard format
- [ ] Appropriate HTTP status codes are used
- [ ] Error codes are machine-readable
- [ ] Rate limiting headers are included

### 16.4 File Management
- [ ] Files can be uploaded via API
- [ ] Files can be downloaded via API
- [ ] Files can be listed with pagination
- [ ] Files can be searched/queried

### 16.5 Documentation
- [ ] OpenAPI specification is available
- [ ] Interactive documentation is available
- [ ] Integration guide is provided

---

## 17. Questions and Clarifications

For questions or clarifications regarding this PRD, please contact:
- **Email:** [integration-team@librelog.example.org]
- **Issue Tracker:** [GitHub Issues URL]

---

## 18. Appendix

### 18.1 Example Integration Flow

1. User logs into LibreTime web UI
2. User navigates to Settings → API → Tokens
3. User generates new token with name "LibreLog Integration"
4. User copies token
5. User configures LibreLog integration with token
6. LibreLog tests connection using `/api/v2/test/connection`
7. LibreLog begins file synchronization

### 18.2 Example cURL Commands

**Generate Token:**
```bash
curl -X POST https://libretime.example.org/api/v2/auth/tokens \
  -H "Authorization: Bearer {user_jwt_token}" \
  -H "Content-Type: application/json" \
  -d '{"name": "LibreLog Integration Token"}'
```

**Test Connection:**
```bash
curl -X GET https://libretime.example.org/api/v2/test/connection \
  -H "Authorization: Bearer {integration_jwt_token}"
```

**Check Health:**
```bash
curl -X GET https://libretime.example.org/api/v2/health
```

**Upload File:**
```bash
curl -X POST https://libretime.example.org/api/v2/files \
  -H "Authorization: Bearer {integration_jwt_token}" \
  -F "file=@audio.mp3" \
  -F "name=audio.mp3" \
  -F "description=Morning show intro"
```

---

**Document End**

