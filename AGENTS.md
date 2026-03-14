Fix Authentication Issues in LibreLog

STATUS: ✅ FIXED IN v0.1.6 (March 14, 2026)

FIXED ISSUES:
1. ✅ POST /api/auth/login returns 400 validation error - FIXED
   - LoginRequestDTO now accepts both 'username' and 'email' fields
   
2. ✅ GET/PUT /api/auth/profile returns 401/500 error - FIXED
   - JWT filter now properly authenticates /api/auth/me and /api/auth/profile
   - Fixed audit logging null pointer issues

TEST COMMAND (working):
curl -X POST https://log.gayphx.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin@librelog.com","password":"admin123"}'

SUCCESS: Returns 200 with JWT token

NEXT PRIORITIES:
- Fix Order model (add advertiserId relationship)
- Fix Voice Track model (add song before/after context)
- Implement Campaign from Order workflow
