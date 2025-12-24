# LibreLog API Testing Guide

## Overview

This guide provides comprehensive instructions for testing all LibreLog API endpoints, including tokenized (JWT Bearer) requests.

## Prerequisites

1. LibreLog system running and accessible
2. Docker containers running (if using container networking)
3. Test user account created (default: admin/admin123)
4. LibreTime integration configured (for integration tests)

## Authentication

### Getting a JWT Token

All API endpoints (except health checks) require JWT Bearer token authentication.

**Request:**
```bash
POST /auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

**Response:**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "bearer"
}
```

### Using the Token

Include the token in all subsequent requests:

```bash
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Token Refresh

Tokens expire after a set period. Refresh using:

```bash
POST /auth/refresh
Authorization: Bearer <token>
```

## Test Scripts

### 1. Complete Endpoint Test

**Script:** `test_all_endpoints.py`

Tests all API endpoints systematically:

```bash
# From host (if port exposed)
export LIBRELOG_API_URL=http://localhost:8000
export TEST_USERNAME=admin
export TEST_PASSWORD=admin123
python3 test_all_endpoints.py

# From API container
docker-compose exec api python3 /app/test_all_endpoints.py
```

**Output:** `api_test_results.json`

### 2. Complete Workflow Test

**Script:** `test_complete_workflow.py`

Tests end-to-end workflow from order to billing:

```bash
export LIBRELOG_API_URL=http://api:8000
export TEST_USERNAME=admin
export TEST_PASSWORD=admin123
python3 test_complete_workflow.py
```

**Output:** `complete_workflow_test_results.json`

## API Endpoint Categories

### Authentication Endpoints

| Method | Path | Auth Required | Description |
|--------|------|---------------|-------------|
| POST | `/auth/login` | No | Login and get JWT token |
| GET | `/auth/profile` | Yes | Get user profile |
| PUT | `/auth/profile` | Yes | Update user profile |
| GET | `/auth/me` | Yes | Get current user info |
| POST | `/auth/refresh` | Yes | Refresh JWT token |
| POST | `/auth/logout` | Yes | Logout (invalidate token) |

**Test Checklist:**
- [ ] Login returns valid JWT token
- [ ] Token format is correct (Bearer token)
- [ ] Token refresh works
- [ ] Invalid token returns 401
- [ ] Missing token returns 401
- [ ] Logout invalidates token

### Health & Setup Endpoints

| Method | Path | Auth Required | Description |
|--------|------|---------------|-------------|
| GET | `/health` | No | Health check |
| GET | `/api/health` | No | API health check |
| GET | `/setup/status` | No | Setup status |
| POST | `/setup/initialize` | No | Initialize system |

**Test Checklist:**
- [ ] Health endpoint returns 200
- [ ] Setup status endpoint works
- [ ] Initialize endpoint works (if not already initialized)

### Core Data Endpoints

#### Tracks (`/tracks/*`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/tracks` | List all tracks |
| GET | `/tracks/count` | Get track count |
| POST | `/tracks` | Create track |
| GET | `/tracks/{id}` | Get track by ID |
| PUT | `/tracks/{id}` | Update track |
| DELETE | `/tracks/{id}` | Delete track |

**Test Checklist:**
- [ ] List tracks returns array
- [ ] Create track with valid data
- [ ] Get track by ID
- [ ] Update track
- [ ] Delete track (or verify 404 for non-existent)

#### Campaigns (`/campaigns/*`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/campaigns` | List campaigns |
| POST | `/campaigns` | Create campaign |
| GET | `/campaigns/{id}` | Get campaign |
| PUT | `/campaigns/{id}` | Update campaign |
| DELETE | `/campaigns/{id}` | Delete campaign |

**Test Checklist:**
- [ ] All CRUD operations work
- [ ] Validation errors return 422
- [ ] Non-existent IDs return 404

#### Clocks (`/clocks/*`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/clocks` | List clock templates |
| POST | `/clocks` | Create clock template |
| GET | `/clocks/{id}` | Get clock template |
| PUT | `/clocks/{id}` | Update clock template |
| DELETE | `/clocks/{id}` | Delete clock template |

**Test Checklist:**
- [ ] All CRUD operations work
- [ ] Clock template structure validated

### Sales & Traffic Endpoints

#### Advertisers (`/advertisers/*`)

**Test Checklist:**
- [ ] Create advertiser with all required fields
- [ ] List advertisers
- [ ] Get advertiser by ID
- [ ] Update advertiser
- [ ] Delete advertiser

#### Orders (`/orders/*`)

**Test Checklist:**
- [ ] Create order
- [ ] Verify order number format (YYYYMMDD-XXXX)
- [ ] List orders with filters
- [ ] Get order by ID
- [ ] Update order
- [ ] Approve order (`POST /orders/{id}/approve`)
- [ ] Duplicate order (`POST /orders/{id}/duplicate`)
- [ ] Delete order (only if no spots)

#### Spots (`/spots/*`)

**Test Checklist:**
- [ ] List spots
- [ ] Create single spot
- [ ] Bulk create spots (`POST /spots/bulk`)
- [ ] Get spot by ID
- [ ] Update spot
- [ ] Delete spot
- [ ] Resolve conflict (`POST /spots/{id}/resolve-conflict`)

### Log Management Endpoints

#### Logs (`/logs/*`)

**Test Checklist:**
- [ ] List logs
- [ ] Get log count
- [ ] Generate log (`POST /logs/generate`)
- [ ] Preview log (`POST /logs/preview`)
- [ ] Get log by ID
- [ ] Get log timeline (`GET /logs/{id}/timeline`)
- [ ] Publish log (`POST /logs/{id}/publish`)
- [ ] Publish hour (`POST /logs/{id}/publish-hour`)
- [ ] Lock/unlock log
- [ ] Get conflicts (`GET /logs/{id}/conflicts`)
- [ ] Get avails (`GET /logs/{id}/avails`)
- [ ] Add spots to log
- [ ] Update spot in log
- [ ] Delete spot from log
- [ ] Get voice slots
- [ ] Link voice track to slot

### Integration Endpoints

#### Sync (`/sync/*`)

**Test Checklist:**
- [ ] Get sync status (`GET /sync/status`)
- [ ] Sync tracks (`POST /sync/tracks`)
- [ ] Sync playback history (`POST /sync/playback-history`)

#### LibreTime Proxy (`/proxy/*`)

**Test Checklist:**
- [ ] Proxy dashboard (`GET /proxy/dashboard`)
- [ ] Proxy tracks (`GET /proxy/tracks/aggregated`)
- [ ] Proxy LibreTime API (`GET /proxy/libretime/{path}`)

### Billing Endpoints

#### Invoices (`/invoices/*`)

**Test Checklist:**
- [ ] List invoices
- [ ] Get invoice by ID
- [ ] Create invoice
- [ ] Update invoice
- [ ] Send invoice (`POST /invoices/{id}/send`)
- [ ] Mark invoice paid (`POST /invoices/{id}/mark-paid`)
- [ ] Get AR aging (`GET /invoices/aging`)

#### Payments (`/payments/*`)

**Test Checklist:**
- [ ] List payments
- [ ] Create payment
- [ ] Get payment by ID
- [ ] Update payment
- [ ] Delete payment

## Tokenized Request Testing

### Test Token Format

All requests must include:
```
Authorization: Bearer <jwt_token>
```

### Test Cases

1. **Valid Token**
   - Request with valid token
   - Expected: 200/201 response

2. **Invalid Token**
   - Request with malformed token
   - Expected: 401 Unauthorized

3. **Expired Token**
   - Request with expired token
   - Expected: 401 Unauthorized

4. **Missing Token**
   - Request without Authorization header
   - Expected: 401 Unauthorized

5. **Token Refresh**
   - Get new token using refresh endpoint
   - Expected: New valid token

## Error Handling Testing

### Expected Error Responses

| Status Code | Meaning | Test Case |
|-------------|---------|-----------|
| 200 | Success | Valid request |
| 201 | Created | POST creates resource |
| 401 | Unauthorized | Missing/invalid token |
| 404 | Not Found | Non-existent resource |
| 422 | Validation Error | Invalid request data |
| 500 | Server Error | Internal error |

### Test Error Scenarios

- [ ] Missing required fields → 422
- [ ] Invalid data types → 422
- [ ] Non-existent ID → 404
- [ ] Missing token → 401
- [ ] Invalid token → 401
- [ ] Unauthorized access → 403 (if implemented)

## Integration Testing

### LibreTime Integration

**Test Checklist:**
- [ ] LibreTime connection health check
- [ ] Track sync from LibreTime
- [ ] Log publishing to LibreTime
- [ ] Voice track upload to LibreTime
- [ ] Playback history sync from LibreTime

**Configuration Required:**
- `LIBRETIME_URL` or `LIBRETIME_INTERNAL_URL`
- `LIBRETIME_API_KEY`

### AzuraCast Integration (if configured)

**Test Checklist:**
- [ ] AzuraCast connection
- [ ] Metadata sync

## Running Tests

### Option 1: Automated Test Script

```bash
# Test all endpoints
python3 test_all_endpoints.py

# Test complete workflow
python3 test_complete_workflow.py
```

### Option 2: Manual Testing with curl

```bash
# Authenticate
TOKEN=$(curl -X POST http://api:8000/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  | jq -r '.access_token')

# Make authenticated request
curl -X GET http://api:8000/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Host: log.gayphx.com"
```

### Option 3: Using Postman/Insomnia

1. Create environment variable `base_url` = `http://api:8000`
2. Create environment variable `token`
3. Create pre-request script to get token:
   ```javascript
   pm.sendRequest({
     url: pm.environment.get("base_url") + "/auth/login",
     method: 'POST',
     header: {'Content-Type': 'application/json'},
     body: {
       mode: 'raw',
       raw: JSON.stringify({
         username: "admin",
         password: "admin123"
       })
     }
   }, function (err, res) {
     pm.environment.set("token", res.json().access_token);
   });
   ```
4. Set Authorization header: `Bearer {{token}}`

## Test Results Documentation

After running tests, document results:

1. **Test Execution Summary**
   - Total endpoints tested
   - Passed/Failed/Skipped counts
   - Execution time

2. **Failed Tests**
   - Endpoint path and method
   - Expected vs actual result
   - Error message
   - Steps to reproduce

3. **Issues Found**
   - Bug descriptions
   - Severity (Critical/High/Medium/Low)
   - Workarounds (if any)

4. **Recommendations**
   - Endpoints needing fixes
   - Missing endpoints
   - Documentation updates needed

## Troubleshooting

### Authentication Fails

- Verify username/password are correct
- Check backend container is running
- Verify database is accessible
- Check backend logs for errors

### Endpoints Return 500

- Check backend logs
- Verify database connection
- Check for missing dependencies
- Verify data integrity

### LibreTime Integration Fails

- Verify LibreTime URL is correct
- Check API key is valid
- Verify LibreTime is accessible from backend
- Check network connectivity
- Review LibreTime logs

### Container Networking Issues

- Use container names (`api:8000`) not localhost
- Verify containers are on same network
- Check docker-compose network configuration
- Use `Host` header for TrustedHostMiddleware

## Best Practices

1. **Test in Order**
   - Create resources before reading/updating
   - Delete test data after tests

2. **Use Test Data**
   - Create dedicated test accounts
   - Use test prefixes for data
   - Clean up after tests

3. **Document Failures**
   - Capture request/response
   - Note error messages
   - Include steps to reproduce

4. **Test Token Expiration**
   - Verify token refresh works
   - Test with expired tokens
   - Verify proper error responses

5. **Test Error Cases**
   - Invalid data
   - Missing fields
   - Non-existent resources
   - Unauthorized access

---

*Last Updated: 2025-01-15*




