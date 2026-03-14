Fix Authentication Issues in LibreLog

CRITICAL TASK:
1. POST /api/auth/login returns 400 validation error
2. GET /api/auth/profile returns 500 server error

ACTIONS:
- Check AuthController.java login method
- Check LoginRequestDTO validation annotations
- Check UserDetailsService implementation
- Test with valid credentials from database
- Commit fixes when working

TEST COMMAND:
docker exec librelog-api wget -qO- --post-data="{\"username\":\"admin\",\"password\":\"admin\"}" --header="Content-Type: application/json" http://localhost:8080/api/auth/login

SUCCESS CRITERIA:
- Login returns 200 with JWT token
- Profile returns 200 with user data
- Other endpoints accessible with token
