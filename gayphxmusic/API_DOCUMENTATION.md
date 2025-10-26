# GayPHX Music Platform - API Documentation

**Version:** 2.0.0  
**Base URL:** `http://localhost:8000/api`  
**Authentication:** JWT Bearer tokens

## 🔐 Authentication

### Artist Authentication
- **Magic Link**: `POST /api/auth/request-magic-link`
- **Token Format**: `artist_token_*` (legacy) or JWT tokens

### Admin Authentication
- **Login**: `POST /api/admin/login`
- **Token Format**: JWT tokens with admin claims

## 🎵 Artist Management API

### List Artists
```http
GET /api/artists/
Authorization: Bearer <token>
```

**Query Parameters:**
- `skip` (int): Pagination offset (default: 0)
- `limit` (int): Number of results (default: 100)
- `search` (string): Search by artist name

**Response:**
```json
{
  "artists": [
    {
      "id": "uuid",
      "name": "Artist Name",
      "pronouns": "they/them",
      "bio": "Artist bio",
      "social_links": {
        "instagram": "https://instagram.com/artist",
        "spotify": "https://open.spotify.com/artist/..."
      },
      "is_active": true,
      "created_at": "2025-10-25T20:00:00Z",
      "updated_at": "2025-10-25T20:00:00Z",
      "submission_count": 5
    }
  ],
  "total": 1
}
```

### Create Artist
```http
POST /api/artists/
Authorization: Bearer <token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "Artist Name",
  "pronouns": "they/them",
  "bio": "Artist bio",
  "social_links": {
    "instagram": "https://instagram.com/artist",
    "spotify": "https://open.spotify.com/artist/..."
  }
}
```

### Get Artist Details
```http
GET /api/artists/{id}
Authorization: Bearer <token>
```

### Update Artist
```http
PUT /api/artists/{id}
Authorization: Bearer <token>
Content-Type: application/json
```

### Delete Artist
```http
DELETE /api/artists/{id}
Authorization: Bearer <token>
```

### Reactivate Artist
```http
POST /api/artists/{id}/reactivate
Authorization: Bearer <token>
```

### Get Artists for Dropdown
```http
GET /api/artists/dropdown/list
Authorization: Bearer <token>
```

**Response:**
```json
[
  {
    "id": "uuid",
    "name": "Artist Name",
    "pronouns": "they/them"
  }
]
```

## 🎤 Submissions API

### Create Submission
```http
POST /api/submissions/
Authorization: Bearer <token>
Content-Type: multipart/form-data
```

**Form Data:**
- `file`: Audio file (MP3, WAV, M4A, FLAC)
- `artist_id`: UUID of selected artist
- `song_title`: Song title
- `genre`: Genre (optional)
- `isrc_requested`: Boolean
- `radio_permission`: Boolean
- `public_display`: Boolean
- `podcast_permission`: Boolean
- `commercial_use`: Boolean
- `rights_attestation`: Boolean
- `pro_info[pro_affiliation]`: PRO affiliation (optional)
- `pro_info[writer_splits]`: Writer splits (optional)
- `pro_info[publisher]`: Publisher (optional)

### Get My Submissions
```http
GET /api/submissions/my
Authorization: Bearer <token>
```

### Track Submission
```http
GET /api/submissions/track/{tracking_id}
```

## 👥 Admin Management API

### Admin Login
```http
POST /api/admin/login
Content-Type: application/json
```

**Request Body:**
```json
{
  "email": "admin@gayphx.com",
  "password": "password"
}
```

**Response:**
```json
{
  "access_token": "jwt_token",
  "token_type": "bearer",
  "admin": {
    "id": "uuid",
    "email": "admin@gayphx.com",
    "name": "Admin Name",
    "role": "super_admin"
  }
}
```

### List Admin Users
```http
GET /api/admin/admins
Authorization: Bearer <admin_token>
```

### Create Admin User
```http
POST /api/admin/admins
Authorization: Bearer <admin_token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "email": "newadmin@gayphx.com",
  "name": "Admin Name",
  "password": "password",
  "role": "admin"
}
```

### Get Admin Profile
```http
GET /api/admin/profile
Authorization: Bearer <admin_token>
```

### Update Admin Profile
```http
PUT /api/admin/profile
Authorization: Bearer <admin_token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "Updated Name",
  "email": "updated@gayphx.com",
  "current_password": "current_password",
  "new_password": "new_password"
}
```

### Get Admin Statistics
```http
GET /api/admin/admin-stats
Authorization: Bearer <admin_token>
```

## 🎛️ LibreTime Integration API

### Get LibreTime Configuration
```http
GET /api/plays/libretime-config
Authorization: Bearer <admin_token>
```

**Response:**
```json
{
  "configured": true,
  "libretime_url": "https://studio.gayphx.com",
  "api_key": "masked_key",
  "sync_interval_minutes": 15,
  "auto_sync_enabled": true,
  "sync_status": "active",
  "last_sync_at": "2025-10-25T20:00:00Z",
  "error_count": 0,
  "last_error": null
}
```

### Save LibreTime Configuration
```http
POST /api/plays/libretime-config
Authorization: Bearer <admin_token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "libretime_url": "https://studio.gayphx.com",
  "api_key": "your_api_key",
  "sync_interval_minutes": 15,
  "auto_sync_enabled": true
}
```

### Validate LibreTime API Key
```http
POST /api/plays/libretime-validate
Authorization: Bearer <admin_token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "libretime_url": "https://studio.gayphx.com",
  "api_key": "your_api_key"
}
```

**Response:**
```json
{
  "valid": true,
  "message": "API key is valid"
}
```

### Sync with LibreTime
```http
POST /api/plays/sync-libretime?hours_back=24
Authorization: Bearer <admin_token>
```

### Test LibreTime Connection
```http
POST /api/plays/test-libretime-connection
Authorization: Bearer <admin_token>
```

## 📊 Play Tracking API

### Get Play Statistics
```http
GET /api/plays/statistics
Authorization: Bearer <admin_token>
```

**Response:**
```json
{
  "total_submissions": 25,
  "total_plays": 150,
  "plays_this_week": 12,
  "average_plays_per_submission": 6.0
}
```

**Note:** LibreTime only provides total play counts, not broken down by play type (radio/podcast/commercial).

### Get Recent Plays
```http
GET /api/plays/recent-plays?hours=24&limit=100
Authorization: Bearer <admin_token>
```

### Get Top Tracks
```http
GET /api/plays/top-tracks?period=month&limit=20
Authorization: Bearer <admin_token>
```

### Get Submission Plays
```http
GET /api/plays/submissions/{submission_id}/plays?limit=50&offset=0
Authorization: Bearer <admin_token>
```

## 🔑 ISRC Management API

### Get ISRC Key
```http
GET /api/admin/isrc-key
Authorization: Bearer <admin_token>
```

### Update ISRC Key
```http
PUT /api/admin/isrc-key
Authorization: Bearer <admin_token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "isrc_key": "ABC"
}
```

### Delete ISRC Key
```http
DELETE /api/admin/isrc-key
Authorization: Bearer <admin_token>
```

## 📤 Export API

### Download CSV Catalog
```http
GET /api/exports/csv
Authorization: Bearer <admin_token>
```

### Get JSON Feed
```http
GET /api/exports/json
Authorization: Bearer <admin_token>
```

### Get LibreTime Feed
```http
GET /api/exports/libretime
Authorization: Bearer <admin_token>
```

## ⚙️ System Configuration API

### Get System Configuration
```http
GET /api/config/
Authorization: Bearer <admin_token>
```

**Response:**
```json
{
  "organization_name": "GayPHX Community Music Platform",
  "copyright_notice": "© 2025 GayPHX Music Platform. Built with love for the LGBTQ+ community. 🌈",
  "isrc_country_code": "US",
  "isrc_registrant_code": "GPH",
  "isrc_year": 25,
  "isrc_sequence": 1,
  "libretime_url": "https://studio.gayphx.com",
  "libretime_api_key": "masked_key",
  "sync_interval_minutes": 15,
  "auto_sync_enabled": true
}
```

### Update System Configuration
```http
PUT /api/config/
Authorization: Bearer <admin_token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "organization_name": "Updated Organization Name",
  "copyright_notice": "Updated copyright notice",
  "isrc_country_code": "US",
  "isrc_registrant_code": "GPH"
}
```

## 🔒 Rights Management API

### Get Radio Permissions
```http
GET /api/rights/admin/radio-permissions
Authorization: Bearer <admin_token>
```

### Get Submission Rights
```http
GET /api/rights/submissions/{submission_id}/rights
Authorization: Bearer <admin_token>
```

## 📝 Error Responses

### 400 Bad Request
```json
{
  "detail": "Validation error message"
}
```

### 401 Unauthorized
```json
{
  "detail": "Not authenticated"
}
```

### 403 Forbidden
```json
{
  "detail": "Insufficient permissions"
}
```

### 404 Not Found
```json
{
  "detail": "Resource not found"
}
```

### 500 Internal Server Error
```json
{
  "detail": "Internal server error"
}
```

## 🚀 Rate Limiting

- **Default**: 100 requests per minute per IP
- **Admin Endpoints**: 200 requests per minute
- **File Uploads**: 10 requests per minute

## 🔧 Testing

### Test Artist Token
```bash
curl -H "Authorization: Bearer artist_token_test" \
     http://localhost:8000/api/artists/
```

### Test Admin Token
```bash
curl -H "Authorization: Bearer <admin_jwt_token>" \
     http://localhost:8000/api/admin/profile
```

## 📚 Additional Resources

- **API Documentation**: http://localhost:8000/docs
- **ReDoc**: http://localhost:8000/redoc
- **Health Check**: http://localhost:8000/health
- **System Status**: See SYSTEM_STATUS.md

---

**For more information, see:**
- README.md - General platform information
- FEATURE_UPDATES.md - Latest feature additions
- SYSTEM_STATUS.md - Current system status
