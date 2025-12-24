# Remediation Plan - QA Testing & Security Audit

**Plan Date**: 2025-01-XX  
**Based On**: QA Test Report + Security Audit Report  
**Priority**: Critical fixes first, then high/medium/low priority items

---

## Overview

This remediation plan addresses:
- **73 API endpoint failures** identified in QA testing
- **47 security vulnerabilities** identified in security audit
- **Performance issues** affecting all list endpoints

**Estimated Timeline**:
- **Critical Fixes**: 1-2 weeks
- **High Priority**: 2-4 weeks  
- **Medium Priority**: 1-2 months
- **Low Priority**: 3-6 months

---

## Phase 1: Critical Security Fixes (Week 1-2)

### 1.1 Fix Weak JWT Secret Key

**Priority**: 🔴 **P0 - CRITICAL**  
**Effort**: 2 hours  
**Risk**: System compromise if not fixed

**Tasks**:
1. Generate cryptographically random secret key (32+ characters)
2. Update `backend/auth/oauth2.py` to fail if secret not provided
3. Set `JWT_SECRET_KEY` environment variable in production
4. Document secret key management procedures
5. Plan secret key rotation schedule

**Files to Modify**:
- `backend/auth/oauth2.py:15`
- `.env` (production)
- `env.template` (update documentation)

**Code Changes**:
```python
# backend/auth/oauth2.py
SECRET_KEY = os.getenv("JWT_SECRET_KEY")
if not SECRET_KEY:
    raise ValueError("JWT_SECRET_KEY environment variable must be set")
if len(SECRET_KEY) < 32:
    raise ValueError("JWT_SECRET_KEY must be at least 32 characters")
```

**Verification**:
- [ ] Application fails to start without JWT_SECRET_KEY
- [ ] Secret key is at least 32 characters
- [ ] No default/weak secret key in code

---

### 1.2 Secure Traefik Dashboard

**Priority**: 🔴 **P0 - CRITICAL**  
**Effort**: 4 hours  
**Risk**: Information disclosure

**Tasks**:
1. Set `api.insecure: false` in Traefik config
2. Implement basic authentication for dashboard
3. Restrict dashboard access to internal network
4. Test dashboard access with authentication
5. Document dashboard access procedures

**Files to Modify**:
- `traefik/config/traefik.yaml:11`
- Add authentication middleware configuration

**Configuration Changes**:
```yaml
# traefik/config/traefik.yaml
api:
  dashboard: true
  insecure: false  # Require authentication
  # Add authentication middleware
```

**Verification**:
- [ ] Dashboard requires authentication
- [ ] Dashboard not accessible from public internet
- [ ] Authentication credentials are strong

---

### 1.3 Fix Database Credentials

**Priority**: 🔴 **P0 - CRITICAL**  
**Effort**: 2 hours  
**Risk**: Database compromise

**Tasks**:
1. Generate strong database password
2. Move password to `.env` file (gitignored)
3. Update `docker-compose.yml` to use environment variable
4. Update all database connection strings
5. Rotate database password
6. Update documentation

**Files to Modify**:
- `docker-compose.yml:10`
- `.env` (production, gitignored)
- `env.template` (update example)

**Configuration Changes**:
```yaml
# docker-compose.yml
environment:
  POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}  # From .env
```

**Verification**:
- [ ] No passwords in version control
- [ ] Strong password (20+ characters, random)
- [ ] Application connects successfully
- [ ] Old password no longer works

---

### 1.4 Implement Rate Limiting

**Priority**: 🔴 **P0 - CRITICAL**  
**Effort**: 8 hours  
**Risk**: DoS attacks, brute force

**Tasks**:
1. Install rate limiting library (`slowapi` or `fastapi-limiter`)
2. Create rate limiting middleware
3. Configure limits:
   - Login: 5 requests/minute per IP
   - General API: 100 requests/minute per user
   - File upload: 10 requests/minute per user
4. Use Redis for distributed rate limiting
5. Add rate limit headers to responses
6. Log rate limit violations
7. Test rate limiting

**Files to Create**:
- `backend/middleware/rate_limit.py`

**Files to Modify**:
- `backend/main.py` (add middleware)
- `backend/requirements.txt` (add dependency)

**Code Example**:
```python
# backend/middleware/rate_limit.py
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.util import get_remote_address
from slowapi.errors import RateLimitExceeded

limiter = Limiter(key_func=get_remote_address)

# Apply to login endpoint
@router.post("/login")
@limiter.limit("5/minute")
async def login(...):
    ...
```

**Verification**:
- [ ] Rate limiting works on login endpoint
- [ ] Returns 429 with Retry-After header
- [ ] Different limits for different endpoints
- [ ] Redis integration works (if distributed)

---

### 1.5 Fix Critical 500 Errors

**Priority**: 🔴 **P0 - CRITICAL**  
**Effort**: 16 hours  
**Risk**: Broken functionality

**Endpoints to Fix**:
1. `POST /orders/` - SQLAlchemy commit issue
2. `DELETE /order-attachments/999999` - Error handling
3. `POST /copy/` - Internal error
4. `POST /rotation-rules/` - Complete failure
5. `GET /rotation-rules/1` - Error on retrieval
6. `PUT /rotation-rules/1` - Error on update
7. `GET /admin/audit-logs/1` - Error on retrieval

**Tasks**:
1. Review server logs for each endpoint
2. Identify root cause of each error
3. Fix SQLAlchemy relationship loading issues
4. Add proper error handling
5. Add input validation
6. Test each endpoint
7. Add unit tests

**Files to Modify**:
- `backend/routers/orders.py`
- `backend/routers/order_attachments.py`
- `backend/routers/copy.py`
- `backend/routers/rotation_rules.py`
- `backend/routers/audit_logs.py`

**Verification**:
- [ ] All endpoints return 200/201/404 (not 500)
- [ ] Proper error messages for invalid input
- [ ] No stack traces exposed to clients
- [ ] All functionality works correctly

---

## Phase 2: High Priority Fixes (Week 2-4)

### 2.1 Implement Password Policy

**Priority**: 🟠 **P1 - HIGH**  
**Effort**: 8 hours

**Tasks**:
1. Install password strength library (`zxcvbn`)
2. Create password policy module
3. Implement validation:
   - Minimum 12 characters (16 for enterprise)
   - Require uppercase, lowercase, numbers, special chars
   - Check against common password lists
   - Prevent password reuse (last 5)
4. Add password strength indicator
5. Update password change endpoint
6. Update user creation endpoint
7. Add password policy documentation

**Files to Create**:
- `backend/auth/password_policy.py`

**Files to Modify**:
- `backend/routers/auth.py`
- `backend/routers/setup.py`
- `backend/routers/users.py`

**Verification**:
- [ ] Weak passwords rejected
- [ ] Password strength requirements enforced
- [ ] Password reuse prevented
- [ ] Clear error messages

---

### 2.2 Implement Account Lockout

**Priority**: 🟠 **P1 - HIGH**  
**Effort**: 8 hours

**Tasks**:
1. Add failed login attempt tracking (Redis)
2. Implement lockout after 5 failed attempts
3. Lock account for 15 minutes (configurable)
4. Log all failed login attempts
5. Send notification on lockout
6. Implement CAPTCHA after 3 failed attempts (optional)
7. Add unlock endpoint for admins
8. Test lockout mechanism

**Files to Create**:
- `backend/auth/account_lockout.py`

**Files to Modify**:
- `backend/routers/auth.py`

**Verification**:
- [ ] Account locks after 5 failed attempts
- [ ] Lockout duration is correct
- [ ] Failed attempts are logged
- [ ] Account unlocks after timeout
- [ ] Admin can unlock accounts

---

### 2.3 Restrict CORS Configuration

**Priority**: 🟠 **P1 - HIGH**  
**Effort**: 2 hours

**Tasks**:
1. Remove HTTP origins (production only HTTPS)
2. Restrict `allow_methods` to specific methods
3. Restrict `allow_headers` to specific headers
4. Use environment-specific CORS config
5. Review `allow_credentials` usage
6. Test CORS with frontend

**Files to Modify**:
- `backend/main.py:145-157`

**Code Changes**:
```python
# Production CORS
allow_origins=[
    "https://log.gayphx.com",  # Remove HTTP
],
allow_methods=["GET", "POST", "PUT", "DELETE", "PATCH"],  # Specific methods
allow_headers=["Content-Type", "Authorization", "X-Requested-With"],  # Specific headers
```

**Verification**:
- [ ] Only HTTPS origins allowed (production)
- [ ] Only required methods allowed
- [ ] Only required headers allowed
- [ ] Frontend still works correctly

---

### 2.4 Add Request and File Size Limits

**Priority**: 🟠 **P1 - HIGH**  
**Effort**: 4 hours

**Tasks**:
1. Configure Uvicorn request limits
2. Add request size validation in middleware
3. Set file upload size limits:
   - Audio: 50MB
   - Images: 5MB
   - Documents: 10MB
4. Add file size validation in upload endpoints
5. Return 413 Payload Too Large
6. Test size limits

**Files to Modify**:
- `docker-compose.yml` (Uvicorn config)
- `backend/middleware.py` (add size check)
- All file upload endpoints

**Verification**:
- [ ] Request size limits enforced
- [ ] File size limits enforced
- [ ] Proper error messages (413)
- [ ] Valid requests still work

---

### 2.5 Implement Security Headers

**Priority**: 🟠 **P1 - HIGH**  
**Effort**: 6 hours

**Tasks**:
1. Create security headers middleware
2. Implement headers:
   - Content-Security-Policy
   - Strict-Transport-Security (HSTS)
   - X-Frame-Options
   - X-Content-Type-Options
   - X-XSS-Protection
   - Referrer-Policy
   - Permissions-Policy
3. Configure CSP policy for application
4. Test headers with security scanner
5. Document header configuration

**Files to Create**:
- `backend/middleware/security_headers.py`

**Files to Modify**:
- `backend/main.py` (add middleware)

**Verification**:
- [ ] All security headers present
- [ ] CSP policy allows required resources
- [ ] HSTS configured correctly
- [ ] Headers pass security scanner tests

---

### 2.6 Fix Missing Endpoints (405 Errors)

**Priority**: 🟠 **P1 - HIGH**  
**Effort**: 12 hours

**Endpoints to Fix**:
1. `DELETE /invoices/{id}` - Add DELETE endpoint
2. `GET /copy-assignments/{id}` - Fix routing
3. `PUT /traffic-logs/{id}` - Add PUT endpoint or remove
4. `DELETE /traffic-logs/{id}` - Add DELETE endpoint or remove
5. `PUT /notifications/{id}/read` - Fix method (should be POST)
6. `DELETE /production-assignments/{id}` - Add DELETE endpoint
7. `POST /backups/create` - Fix routing (should be `/backups/`)

**Tasks**:
1. Review each endpoint requirement
2. Add missing endpoints or fix routing
3. Implement proper validation
4. Add audit logging
5. Test each endpoint

**Files to Modify**:
- `backend/routers/invoices.py`
- `backend/routers/copy_assignments.py`
- `backend/routers/traffic_logs.py`
- `backend/routers/notifications.py`
- `backend/routers/production_assignments.py`
- `backend/routers/backups.py`

**Verification**:
- [ ] All endpoints return correct status codes
- [ ] No 405 errors for valid operations
- [ ] Proper error handling
- [ ] Functionality works correctly

---

### 2.7 Implement Missing Endpoints (404 Errors)

**Priority**: 🟠 **P1 - HIGH**  
**Effort**: 24 hours

**Endpoints to Implement**:
1. `/inventory/avails` - Inventory availability
2. `/inventory/slots` - Available slots
3. `/revenue/summary` - Revenue summary (requires query params)
4. `/revenue/by-station` - Revenue by station
5. `/revenue/by-advertiser` - Revenue by advertiser
6. `/sales-goals/{id}` - Get sales goal
7. `/production-archive/*` - Production archive endpoints
8. `/audio-qc/*` - Audio QC endpoints
9. `/log-revisions/*` - Log revision endpoints
10. `/collaboration/comments/*` - Collaboration endpoints
11. `/notifications/unread-count` - Fix routing (currently `/unread`)

**Tasks**:
1. Design endpoint specifications
2. Implement endpoints
3. Add database queries
4. Add validation
5. Add tests
6. Update API documentation

**Files to Create/Modify**:
- `backend/routers/inventory.py` (add endpoints)
- `backend/routers/revenue.py` (add endpoints)
- `backend/routers/sales_goals.py` (add GET by ID)
- `backend/routers/production_archive.py` (implement)
- `backend/routers/audio_qc.py` (implement)
- `backend/routers/log_revisions.py` (implement)
- `backend/routers/collaboration.py` (implement)
- `backend/routers/notifications.py` (fix routing)

**Verification**:
- [ ] All endpoints return 200/201 (not 404)
- [ ] Proper data returned
- [ ] Validation works
- [ ] Tests pass

---

## Phase 3: Medium Priority Fixes (Month 1-2)

### 3.1 Fix Schema Validation Issues (422 Errors)

**Priority**: 🟡 **P2 - MEDIUM**  
**Effort**: 16 hours

**Issues**:
- Test data doesn't match API schema requirements
- Missing required fields in test scripts
- API documentation incomplete

**Tasks**:
1. Review all 422 errors
2. Update test scripts with correct data
3. Improve API documentation
4. Add better validation error messages
5. Update OpenAPI schema

**Files to Modify**:
- `test_all_endpoints.py` (update test data)
- All router files (improve error messages)
- API documentation

**Verification**:
- [ ] Test scripts use correct data
- [ ] Validation errors are clear
- [ ] API documentation is complete

---

### 3.2 Performance Optimization

**Priority**: 🟡 **P2 - MEDIUM**  
**Effort**: 24 hours

**Issues**:
- All list endpoints taking 340-350ms
- No pagination
- Potential N+1 query problems
- No caching

**Tasks**:
1. Implement pagination for all list endpoints
2. Optimize database queries
3. Fix N+1 query problems (use selectinload/joinedload)
4. Add database indexes
5. Implement caching (Redis)
6. Add query performance monitoring
7. Load test endpoints

**Files to Modify**:
- All list endpoint routers
- `backend/database.py` (add indexes)
- Add caching layer

**Verification**:
- [ ] Response times < 100ms for list endpoints
- [ ] Pagination works correctly
- [ ] No N+1 queries
- [ ] Caching improves performance

---

### 3.3 Implement Secret Management

**Priority**: 🟡 **P2 - MEDIUM**  
**Effort**: 16 hours

**Tasks**:
1. Choose secret management solution (Vault, AWS Secrets Manager, etc.)
2. Set up secret management infrastructure
3. Migrate secrets from environment variables
4. Implement secret rotation
5. Add secret access auditing
6. Update documentation

**Verification**:
- [ ] Secrets stored securely
- [ ] Secret rotation works
- [ ] Access is audited
- [ ] Application works with secret management

---

### 3.4 Enable Encryption at Rest

**Priority**: 🟡 **P2 - MEDIUM**  
**Effort**: 12 hours

**Tasks**:
1. Enable PostgreSQL encryption (TDE)
2. Use encrypted volumes (AWS EBS, Azure Disk)
3. Encrypt sensitive fields at application level
4. Encrypt backups
5. Test encryption/decryption
6. Document encryption procedures

**Verification**:
- [ ] Database encrypted at rest
- [ ] Backups encrypted
- [ ] Sensitive fields encrypted
- [ ] Performance impact acceptable

---

### 3.5 Enhance Audit Logging

**Priority**: 🟡 **P2 - MEDIUM**  
**Effort**: 16 hours

**Tasks**:
1. Audit all security events:
   - Login attempts (success/failure)
   - Password changes
   - Permission changes
   - Data access (sensitive)
   - Configuration changes
2. Store logs securely (immutable, encrypted)
3. Implement log retention policy
4. Add log search/query capabilities
5. Monitor logs for anomalies

**Files to Modify**:
- `backend/logging/audit.py` (enhance)
- All routers (add audit logging)

**Verification**:
- [ ] All security events logged
- [ ] Logs stored securely
- [ ] Log retention works
- [ ] Log search works

---

## Phase 4: Low Priority / Future Enhancements

### 4.1 Implement MFA/2FA

**Priority**: 🟢 **P3 - LOW**  
**Effort**: 32 hours

**Tasks**:
1. Choose 2FA solution (TOTP, SMS)
2. Implement TOTP-based 2FA
3. Add 2FA setup flow
4. Require 2FA for admin accounts
5. Optional 2FA for regular users
6. Test 2FA flow

---

### 4.2 Implement Network Segmentation

**Priority**: 🟢 **P3 - LOW**  
**Effort**: 16 hours

**Tasks**:
1. Design network architecture
2. Create separate Docker networks
3. Implement network policies
4. Configure firewall rules
5. Test network isolation

---

### 4.3 Add WAF Protection

**Priority**: 🟢 **P3 - LOW**  
**Effort**: 24 hours

**Tasks**:
1. Choose WAF solution
2. Configure WAF rules
3. Test WAF protection
4. Monitor WAF logs
5. Tune WAF rules

---

### 4.4 Implement Security Monitoring

**Priority**: 🟢 **P3 - LOW**  
**Effort**: 40 hours

**Tasks**:
1. Set up SIEM/log aggregation
2. Configure security event correlation
3. Set up alerts
4. Create dashboards
5. Implement 24/7 monitoring (if needed)

---

## Testing & Verification

### After Each Phase

1. **Run QA Test Suite**
   - Execute `test_all_endpoints.py`
   - Verify all critical endpoints pass
   - Document any remaining issues

2. **Security Testing**
   - Run security scanner
   - Test authentication/authorization
   - Verify rate limiting
   - Check security headers

3. **Performance Testing**
   - Load test critical endpoints
   - Verify response times
   - Check resource usage

4. **Integration Testing**
   - Test full workflows
   - Test LibreTime integration
   - Test file uploads

---

## Success Criteria

### Phase 1 (Critical) - Complete When:
- [ ] All critical security vulnerabilities fixed
- [ ] All 500 errors resolved
- [ ] Rate limiting implemented
- [ ] Security headers implemented
- [ ] No weak secrets in code

### Phase 2 (High Priority) - Complete When:
- [ ] Password policy enforced
- [ ] Account lockout working
- [ ] CORS properly restricted
- [ ] All 405 errors fixed
- [ ] Missing endpoints implemented

### Phase 3 (Medium Priority) - Complete When:
- [ ] Performance optimized (< 100ms for lists)
- [ ] Pagination implemented
- [ ] Secret management in place
- [ ] Encryption at rest enabled
- [ ] Enhanced audit logging

### Phase 4 (Low Priority) - Complete When:
- [ ] MFA implemented (if required)
- [ ] Network segmentation done
- [ ] WAF protection active
- [ ] Security monitoring operational

---

## Risk Assessment

### Risks of Not Fixing Critical Items

1. **Weak JWT Secret**: System compromise, complete authentication bypass
2. **No Rate Limiting**: DoS attacks, service unavailability
3. **Insecure Dashboard**: Information disclosure, attack surface
4. **Plain Text Credentials**: Database compromise, data breach
5. **500 Errors**: Broken functionality, poor user experience

### Mitigation

- Fix critical items immediately (Week 1-2)
- Implement monitoring to detect attacks
- Regular security reviews
- Incident response plan ready

---

## Resources Required

### Development Time
- **Phase 1**: 40-50 hours
- **Phase 2**: 60-80 hours
- **Phase 3**: 80-100 hours
- **Phase 4**: 100+ hours (optional)

### Infrastructure
- Redis (for rate limiting, caching)
- Secret management solution
- Monitoring/logging solution
- WAF (optional)

### Tools
- Security scanning tools
- Performance testing tools
- Load testing tools

---

## Timeline Summary

| Phase | Duration | Start | End |
|-------|----------|-------|-----|
| Phase 1: Critical | 2 weeks | Week 1 | Week 2 |
| Phase 2: High Priority | 2 weeks | Week 3 | Week 4 |
| Phase 3: Medium Priority | 4 weeks | Week 5 | Week 8 |
| Phase 4: Low Priority | Ongoing | Week 9+ | - |

**Total Estimated Time**: 8 weeks for critical and high-priority items

---

## Next Steps

1. **Review and Approve Plan** - Stakeholder review
2. **Assign Resources** - Development team assignment
3. **Begin Phase 1** - Start critical fixes immediately
4. **Daily Standups** - Track progress
5. **Weekly Reviews** - Review completed items
6. **Testing** - Continuous testing after each fix

---

*Remediation Plan created based on QA Test Report and Security Audit Report*

