# Login Troubleshooting Guide

## Issue: Cannot Login with admin/admin123

### Problem Summary

The login system is failing because the authentication expects an **email address**, not a username. The default admin account uses the email `admin@librelog.com`, not the username `admin`.

---

## Root Cause Analysis

### 1. Authentication System Design

The LibreLog authentication system uses **email-based authentication**, not username-based. This is evident from:

- **LoginRequestDTO** (`librelog-api/src/main/java/com/onelpro/librelog/dto/LoginRequestDTO.java`):
  - Requires an `email` field with `@Email` validation
  - Does NOT have a `username` field

- **AuthServiceImpl** (`librelog-api/src/main/java/com/onelpro/librelog/services/impl/AuthServiceImpl.java`):
  - Line 87: `userRepository.findByEmail(request.getEmail())` - looks up users by email
  - Line 85: Logs show "Login attempt for email: {}"

### 2. Default Admin User Credentials

The default admin user is created in the database with:

- **Email**: `admin@librelog.com` (NOT `admin`)
- **Password**: `admin123`
- **Status**: `ACTIVE`
- **Role**: `ADMIN`

This is defined in:
- `librelog-api/src/main/resources/db/changelog/003-create-admin-user.xml`

### 3. Browser Console Errors Explained

The errors you're seeing in the browser console are mostly **browser extension errors** and are not related to the actual login issue:

- `runtime.lastError: Could not establish connection` - Browser extension trying to communicate
- `Mixed Content` warning - Favicon being loaded over HTTP instead of HTTPS (cosmetic issue)
- `Failed to load resource: net::ERR_FILE_NOT_FOUND` - Missing extension files (not critical)

The real issue is the login credentials format.

---

## Solution

### Correct Login Credentials

Use these credentials to log in:

- **Email**: `admin@librelog.com`
- **Password**: `admin123`

**NOT**:
- ❌ Username: `admin`
- ❌ Password: `admin123`

### Verification Steps

1. **Check if user exists in database:**
   ```bash
   docker exec librelog-db psql -U librelog -d librelog -c "SELECT email, status, role FROM users WHERE email = 'admin@librelog.com';"
   ```

2. **Expected output:**
   ```
        email        | status | role  
   ------------------+--------+-------
    admin@librelog.com | ACTIVE | ADMIN
   ```

3. **Check API logs for login attempts:**
   ```bash
   docker logs librelog-api --tail 100 | grep -i "login\|email"
   ```

4. **Look for these log messages:**
   - `Login attempt for email: admin@librelog.com` (correct)
   - `Login attempt for email: admin` (incorrect - will fail)
   - `Login failed: user not found` (indicates wrong email)

---

## Additional Issues to Check

### 1. URL Mismatch

Your docker-compose.yml is configured for:
- **Host**: `log.gayphx.com`

But you're accessing:
- **URL**: `https://poison.gayphx.com/admin/login`

**Check:**
- Ensure Traefik routing is configured correctly for `poison.gayphx.com`
- Or use the correct URL: `https://log.gayphx.com/admin/login`

### 2. Password Hash Verification

The password hash in the database may differ from the changelog. To verify the password works:

1. Check the current hash:
   ```bash
   docker exec librelog-db psql -U librelog -d librelog -c "SELECT password FROM users WHERE email = 'admin@librelog.com';"
   ```

2. If the password doesn't work, you may need to reset it. See "Password Reset" section below.

### 3. CORS Configuration

Check that your frontend URL is allowed in CORS:
- File: `librelog-api/src/main/resources/application.properties`
- Property: `cors.allowed-origins`
- Should include: `https://poison.gayphx.com` (if using that domain)

---

## Password Reset (If Needed)

If the password hash doesn't match `admin123`, you can reset it:

### Option 1: Update via SQL

```sql
-- Generate a new BCrypt hash for "admin123"
-- You can use an online BCrypt generator or the test-password.java file

UPDATE users 
SET password = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    updated_at = CURRENT_TIMESTAMP
WHERE email = 'admin@librelog.com';
```

### Option 2: Use the Test Password Java File

```bash
cd /home/jenkins/docker/librelog
javac -cp "$(find ~/.m2/repository -name 'spring-security-crypto*.jar' | head -1)" test-password.java
java -cp ".:$(find ~/.m2/repository -name 'spring-security-crypto*.jar' | head -1)" TestPassword
```

This will generate a new hash that you can use to update the database.

---

## Frontend Login Form

The login form should be sending:
```json
{
  "email": "admin@librelog.com",
  "password": "admin123"
}
```

**NOT:**
```json
{
  "username": "admin",
  "password": "admin123"
}
```

Check your frontend login component to ensure it's using the `email` field, not `username`.

---

## API Endpoint

The login endpoint is:
- **URL**: `POST /api/auth/login`
- **Request Body**:
  ```json
  {
    "email": "admin@librelog.com",
    "password": "admin123"
  }
  ```
- **Response** (success):
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": "uuid-here",
    "email": "admin@librelog.com",
    "role": "ADMIN"
  }
  ```

---

## Testing Login via curl

You can test the login directly:

```bash
curl -X POST https://poison.gayphx.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@librelog.com",
    "password": "admin123"
  }'
```

Or if using the correct domain:

```bash
curl -X POST https://log.gayphx.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@librelog.com",
    "password": "admin123"
  }'
```

---

## Summary

**The main issue**: You're trying to login with username `admin`, but the system requires email `admin@librelog.com`.

**Solution**: Use email `admin@librelog.com` with password `admin123` to log in.

**Additional checks**:
1. Verify the user exists in the database
2. Check API logs for specific error messages
3. Ensure you're using the correct domain URL
4. Verify CORS settings allow your frontend domain

---

## Related Files

- `librelog-api/src/main/java/com/onelpro/librelog/dto/LoginRequestDTO.java` - Login request structure
- `librelog-api/src/main/java/com/onelpro/librelog/services/impl/AuthServiceImpl.java` - Login logic
- `librelog-api/src/main/resources/db/changelog/003-create-admin-user.xml` - Admin user creation
- `librelog-api/src/main/resources/static/index.html` - Login form example (shows email field)

---

**Last Updated**: January 2026
