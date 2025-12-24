# Input Sanitization Guide

## Overview

This document outlines which endpoints require input sanitization and how to apply it.

## Endpoints Requiring Sanitization

### High Priority (User-Generated Content)

1. **User Profile/Account Endpoints** (`/users`, `/auth/register`)
   - Fields: `username`, `email`, `name`, `bio`, `notes`
   - Sanitization: HTML escape, remove control characters
   - Implementation: Use `sanitize_user_input()` from `backend/utils/sanitization.py`

2. **Copy/Content Endpoints** (`/copy`, `/copy-assignments`)
   - Fields: `title`, `script_text`, `notes`
   - Sanitization: HTML escape for display, preserve formatting for editing
   - Implementation: Escape on display, store raw for editing

3. **Order/Comments Endpoints** (`/orders`, `/comments`)
   - Fields: `order_name`, `notes`, `comments`
   - Sanitization: HTML escape, limit length
   - Implementation: Use `sanitize_user_input()` with length limits

4. **Settings/Configuration Endpoints** (`/settings`)
   - Fields: All user-configurable settings
   - Sanitization: Type-specific sanitization (URLs, HTML, text)
   - Implementation: Use appropriate sanitization function per field type

5. **File Upload Endpoints** (`/voice-tracks/upload`, `/audio-cuts/upload`, `/order-attachments/upload`)
   - Fields: `filename`, `description`, `notes`
   - Sanitization: Filename sanitization, HTML escape for descriptions
   - Implementation: Use `sanitize_filename()` and `sanitize_user_input()`

### Medium Priority (Structured Data)

6. **Campaign/Advertiser Endpoints** (`/campaigns`, `/advertisers`)
   - Fields: `name`, `description`, `notes`
   - Sanitization: HTML escape
   - Implementation: Use `sanitize_user_input()`

7. **Station/Cluster Endpoints** (`/stations`, `/clusters`)
   - Fields: `name`, `description`, `contacts` (JSON)
   - Sanitization: HTML escape, JSON sanitization
   - Implementation: Use `sanitize_json_input()` for JSON fields

8. **Notification/Message Endpoints** (`/notifications`)
   - Fields: `message`, `subject`
   - Sanitization: HTML escape
   - Implementation: Use `sanitize_user_input()`

### Low Priority (System-Generated)

- Most system-generated endpoints don't require sanitization
- However, any field that accepts user input should be sanitized

## Implementation Examples

### Example 1: Sanitizing User Registration

```python
from backend.utils.sanitization import sanitize_user_input

@router.post("/register")
async def register(user_data: RegisterRequest):
    # Sanitize user input
    user_data.username = sanitize_user_input(user_data.username)
    user_data.email = sanitize_user_input(user_data.email)
    user_data.name = sanitize_user_input(user_data.name)
    
    # Continue with registration...
```

### Example 2: Sanitizing File Upload Metadata

```python
from backend.utils.sanitization import sanitize_filename, sanitize_user_input

@router.post("/upload")
async def upload_file(file: UploadFile, description: str):
    # Sanitize filename
    safe_filename = sanitize_filename(file.filename)
    
    # Sanitize description
    safe_description = sanitize_user_input(description)
    
    # Continue with upload...
```

### Example 3: Sanitizing JSON Data

```python
from backend.utils.sanitization import sanitize_json_input

@router.post("/settings")
async def update_settings(settings: dict):
    # Sanitize nested JSON structure
    safe_settings = sanitize_json_input(settings)
    
    # Continue with update...
```

## Best Practices

1. **Sanitize on Input**: Always sanitize user input when it's received, not when it's displayed
2. **Store Raw When Needed**: For content that needs editing, store raw but sanitize on display
3. **Type-Specific Sanitization**: Use the appropriate sanitization function for the data type
4. **Validate Before Sanitize**: Validate input format first, then sanitize
5. **Log Sanitization**: Log when dangerous input is detected and sanitized

## Security Considerations

- **XSS Prevention**: HTML escaping prevents cross-site scripting attacks
- **SQL Injection**: Use SQLAlchemy parameterized queries (sanitization is secondary defense)
- **Path Traversal**: Filename sanitization prevents directory traversal attacks
- **Command Injection**: Sanitize inputs used in system commands
- **URL Validation**: Validate and sanitize URLs to prevent javascript: and data: protocol attacks

## Testing

- Test with malicious input (XSS payloads, SQL injection attempts, path traversal)
- Test with edge cases (null bytes, control characters, very long strings)
- Test that sanitization doesn't break legitimate input
- Test that sanitized output is safe for display


