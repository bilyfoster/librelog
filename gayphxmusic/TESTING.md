# GayPHX Music Platform - Testing Guide

## System Status

✅ **Backend API**: Fully functional and tested
✅ **Database**: PostgreSQL running with admin user seeded
✅ **Storage**: MinIO running and accessible
✅ **Email**: MailHog running for local email testing
✅ **Admin Features**: User management and profile management implemented

## Known Issue: Frontend Host Header Validation

The Next.js frontend is experiencing a host header validation issue when accessed via `curl` or similar tools. This is a Next.js security feature that validates the `Host` header.

**Workaround**: Access the backend API directly at `http://localhost:8000`

## Testing the System

### 1. Admin Login (Backend Direct)

```bash
curl -X POST http://localhost:8000/api/admin/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@gayphx.com", "password": "admin123"}' | jq .
```

**Expected Response**:
```json
{
  "access_token": "eyJhbGc...",
  "token_type": "bearer",
  "admin": {
    "id": "f49dc2cf-69f7-4119-9a00-3d7046293321",
    "name": "GayPHX Admin",
    "email": "admin@gayphx.com",
    "role": "admin",
    "last_login": "2025-10-24T01:46:56.738912+00:00"
  }
}
```

### 2. List All Users

```bash
TOKEN="<your_access_token_from_login>"
curl -X GET http://localhost:8000/api/admin/users \
  -H "Authorization: Bearer $TOKEN" | jq .
```

### 3. Get User Details

```bash
USER_ID="<user_id>"
curl -X GET http://localhost:8000/api/admin/users/$USER_ID \
  -H "Authorization: Bearer $TOKEN" | jq .
```

### 4. Toggle User Status

```bash
USER_ID="<user_id>"
curl -X PUT http://localhost:8000/api/admin/users/$USER_ID/toggle-status \
  -H "Authorization: Bearer $TOKEN" | jq .
```

### 5. Get Admin Profile

```bash
curl -X GET http://localhost:8000/api/admin/profile \
  -H "Authorization: Bearer $TOKEN" | jq .
```

### 6. Update Admin Profile

```bash
curl -X PUT http://localhost:8000/api/admin/profile \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Updated Admin Name",
    "email": "newemail@gayphx.com",
    "current_password": "admin123",
    "new_password": "newpassword123"
  }' | jq .
```

### 7. Get ISRC Registration Key

```bash
curl -X GET http://localhost:8000/api/admin/isrc-key | jq .
```

### 8. Update ISRC Registration Key

```bash
curl -X PUT http://localhost:8000/api/admin/isrc-key \
  -H "Content-Type: application/json" \
  -d '{"isrc_registration_key": "YOUR-ISRC-KEY-HERE"}' | jq .
```

### 9. Delete ISRC Registration Key

```bash
curl -X DELETE http://localhost:8000/api/admin/isrc-key | jq .
```

## Accessing the Frontend

The frontend is accessible at `http://localhost:3000` in a web browser. The host header validation issue only affects command-line tools like `curl`.

### Frontend Pages

- **Home**: `http://localhost:3000`
- **Admin Login**: `http://localhost:3000/admin/login`
- **Admin Dashboard**: `http://localhost:3000/admin`
- **User Management**: `http://localhost:3000/admin/users`
- **Admin Profile**: `http://localhost:3000/admin/profile`
- **ISRC Key Management**: `http://localhost:3000/admin/isrc-key`
- **Artist Signup**: `http://localhost:3000/signup`
- **Artist Login**: `http://localhost:3000/auth/login`
- **Submit Music**: `http://localhost:3000/submit`
- **Artist Dashboard**: `http://localhost:3000/dashboard`

## Admin Credentials

- **Email**: `admin@gayphx.com`
- **Password**: `admin123`

## Services

- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8000
- **PostgreSQL**: localhost:5432
- **MinIO**: http://localhost:9002 (API), http://localhost:9003 (Console)
- **MailHog**: http://localhost:8025

## API Documentation

The backend API documentation is available at:
- **Swagger UI**: http://localhost:8000/docs
- **ReDoc**: http://localhost:8000/redoc

## Completed Features

### Admin User Management
✅ List all artists with filtering and pagination
✅ View detailed user information with submission history
✅ Toggle user active/inactive status
✅ Search users by name or email

### Admin Profile Management
✅ View admin profile
✅ Update admin name and email
✅ Change admin password
✅ Email uniqueness validation

### ISRC Key Management
✅ Store ISRC registration key securely in database
✅ View current ISRC registration key
✅ Update ISRC registration key
✅ Delete ISRC registration key
✅ Frontend UI for managing ISRC key with show/hide toggle

## Next Steps

1. Test the frontend in a web browser at `http://localhost:3000`
2. Log in as admin at `http://localhost:3000/admin/login`
3. Navigate to User Management at `http://localhost:3000/admin/users`
4. Test the profile management at `http://localhost:3000/admin/profile`

## Troubleshooting

### Frontend shows "Invalid host header"
This is expected when using `curl`. Use a web browser instead or access the backend API directly at port 8000.

### Database connection errors
Ensure PostgreSQL is running: `docker ps | grep postgres`

### MinIO connection errors
Ensure MinIO is running: `docker ps | grep minio`

### Email not sending
Check MailHog at `http://localhost:8025` to view captured emails.

