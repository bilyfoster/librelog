# Coding Guidelines for AI Agent (FastAPI Python Web App)

The following rules define coding standards and best practices for generating and maintaining a FastAPI Python web application. All generated code must strictly follow these guidelines.

---

## 1. Imports

- ❌ Do **not** use wildcard imports (e.g., `from fastapi import *`).
- ✅ Use explicit imports for clarity and maintainability.
- ✅ Group imports in the following order:
  1. Standard library imports
  2. Third-party imports (FastAPI, SQLAlchemy, etc.)
  3. Local application imports

---

## 2. Logging

- ❌ Do **not** use `print()` statements for logging.
- ✅ Use **structlog** for all logging (configured in the application).
- Logging levels should be used appropriately:
  - `info` for normal operation
  - `warn` for recoverable issues
  - `error` for unexpected failures
  - `debug`/`trace` for detailed troubleshooting

Example:
```python
import structlog
logger = structlog.get_logger()

logger.info("User logged in", user_id=user.id, username=user.username)
logger.error("Failed to process request", error=str(e), request_id=request_id)
```

---

## 3. Enums

- Each `Enum` should be defined in its **own module file**.
- All enums must be located inside an **`enums` folder** in the backend package.
- Use Python's `enum.Enum` class for enum definitions.

### Enum Class Template

All enums must follow this structure:

```python
from enum import Enum

class UserStatus(str, Enum):
    """Represents the possible statuses of a user."""
    ACTIVE = "active"
    INACTIVE = "inactive"
    BANNED = "banned"
```

Rules for enums:

- Must include a **docstring** describing the purpose of the enum.
- Enum class names must be **PascalCase**.
- Enum values should be **lowercase with underscores** for string enums.
- Each enum should represent a **single concept** (avoid mixing unrelated constants).
- Prefer `str, Enum` for enums that will be serialized to JSON.

### Sample Enum Unit Test

All enums should have a unit test verifying their values and basic behavior:

```python
import pytest
from backend.enums.user_status import UserStatus

def test_user_status_values():
    """Test that UserStatus enum has all expected values."""
    assert UserStatus.ACTIVE == "active"
    assert UserStatus.INACTIVE == "inactive"
    assert UserStatus.BANNED == "banned"
    
    # Test all values are present
    assert len(UserStatus) == 3
```

---

## 4. Constants

- Minimize global constants.
- Place shared constants in a dedicated `constants.py` module or within the appropriate domain-specific module.
- ❌ Avoid "magic numbers" and hardcoded strings.
- Use `typing.Final` for constants that should not be modified.

Example:
```python
from typing import Final

MAX_FILE_SIZE: Final[int] = 10 * 1024 * 1024  # 10MB
ALLOWED_IMAGE_TYPES: Final[list[str]] = ["image/jpeg", "image/png"]
```

---

## 5. Service Layer

- All **Service classes** should be organized in a `services` folder.
- Keep service methods **focused and cohesive**.
- Use async/await for database operations and I/O-bound tasks.
- **Project Structure Rules**:
  - Service classes should be located in the **`services` folder**.
  - File naming conventions: `{service_name}_service.py` (e.g., `user_service.py`)
  - Service classes should be named `{ServiceName}Service` (e.g., `UserService`)

---

## 6. Project Folder Structure

A standard FastAPI project must follow this structure for clarity and consistency:

```
backend/
│
├── alembic/              # Database migration scripts
│   └── versions/
│
├── auth/                 # Authentication utilities
│   ├── oauth2.py
│   └── token_manager.py
│
├── routers/              # API route handlers (e.g., auth.py, users.py)
│
├── schemas/              # Pydantic models for request/response validation
│
├── models/               # SQLAlchemy ORM models / domain models
│
├── services/             # Business logic services
│
├── database.py           # Database configuration and session management
│
├── middleware.py         # Custom middleware
│
├── logging/              # Logging configuration
│
├── main.py               # FastAPI application entry point
│
└── requirements.txt      # Python dependencies
```

### Test Folder Structure

All tests must mirror the main package structure and follow clear naming conventions (`test_{module_name}.py` for unit tests, `test_{module_name}_integration.py` for integration tests).

```
tests/
│
├── conftest.py           # Pytest configuration and fixtures
│
├── routers/              # Router tests (e.g., test_auth.py)
│
├── schemas/              # Schema-related tests
│
├── models/               # Model tests
│
├── services/             # Service tests
│
└── integration/          # End-to-end or integration tests
    └── test_user_integration.py
```

---

## 7. Naming & Formatting

- Use **snake_case** for variables, functions, and module names.
- Use **PascalCase** for classes.
- Use **UPPER_SNAKE_CASE** for constants.
- Follow PEP 8 formatting guidelines (use `black` formatter with 88 character line length).
- Class names should clearly represent their role (e.g., `UserService`, `UserRepository`).

### Schema Naming Convention

- ✅ **All Pydantic schema classes should use descriptive names** (e.g., `UserResponse`, `RegisterRequest`, `AuthResponse`).
- ✅ Use descriptive names that indicate the purpose: `{Purpose}{Type}` (e.g., `RegisterRequest`, `UserResponse`, `LoginRequest`).
- ✅ Request schemas: `{Action}Request` (e.g., `CreateUserRequest`, `UpdateUserRequest`)
- ✅ Response schemas: `{Entity}Response` (e.g., `UserResponse`, `SongResponse`)
- ❌ Do **not** use generic names like `Request` or `Response` without context.

---

## 8. Class & Method Design

- Keep classes and methods **small and focused** (Single Responsibility Principle).
- Prefer **immutability** where possible (use Pydantic models with `frozen=True` for immutable data).
- Always use **dependency injection** via FastAPI's `Depends()` for dependencies.
- ❌ Do **not** use global variables or singletons for services.
- Use async/await for all I/O operations (database, HTTP requests, file operations).

---

## 9. Pydantic Models

Pydantic should be used for request/response validation and data modeling.

### Required Pydantic Features

- ✅ **`BaseModel`**: Use for all request/response schemas.
- ✅ **Field validation**: Use `Field()` for validation rules, descriptions, and examples.
- ✅ **`ConfigDict`**: Use for model configuration (e.g., `from_attributes=True` for ORM models).
- ✅ **Type hints**: Always use proper type hints (e.g., `str`, `int`, `Optional[str]`, `list[UserResponse]`).

### Pydantic Usage Guidelines

- **Request Schemas**: Use `BaseModel` for all request bodies.
- **Response Schemas**: Use `BaseModel` for all response bodies.
- **ORM Models**: Use `ConfigDict(from_attributes=True)` when converting from SQLAlchemy models.
- **Validation**: Use Pydantic validators for complex validation logic.

### Example Request Schema

```python
from pydantic import BaseModel, Field, EmailStr

class RegisterRequest(BaseModel):
    """User registration request."""
    email: EmailStr = Field(..., description="User email address")
    password: str = Field(..., min_length=8, description="User password")
    name: str = Field(..., min_length=1, max_length=100, description="User full name")
```

### Example Response Schema

```python
from pydantic import BaseModel, ConfigDict
from datetime import datetime
from uuid import UUID

class UserResponse(BaseModel):
    """User response model."""
    model_config = ConfigDict(from_attributes=True)
    
    id: UUID
    email: str
    name: str
    created_at: datetime
```

### When NOT to Use Pydantic

- ❌ Do **not** use Pydantic for SQLAlchemy models (use SQLAlchemy models directly).
- ❌ Do **not** use Pydantic for internal service classes (use regular Python classes).

---

## 10. Exception Handling

- Always handle exceptions securely and log appropriately.
- Never expose internal details in API responses.
- Use custom exceptions when domain-specific errors are required.
- Use FastAPI's `HTTPException` for API errors.
- Prefer exception handlers via `@app.exception_handler()` for global error handling.

Example:
```python
from fastapi import HTTPException, status

raise HTTPException(
    status_code=status.HTTP_404_NOT_FOUND,
    detail="User not found"
)
```

---

## 11. Documentation

- Document all **public APIs** with docstrings.
- Use **type hints** for all function parameters and return values.
- Provide comments for **complex logic** or non-obvious design decisions.
- Use FastAPI's automatic OpenAPI documentation features.
- Add `description` parameters to route decorators and Pydantic fields.

Example:
```python
@router.post("/users", response_model=UserResponse, status_code=status.HTTP_201_CREATED)
async def create_user(
    request: RegisterRequest,
    db: AsyncSession = Depends(get_db)
) -> UserResponse:
    """
    Create a new user account.
    
    Args:
        request: User registration data
        db: Database session
        
    Returns:
        Created user information
        
    Raises:
        HTTPException: If email already exists or validation fails
    """
    # Implementation
```

---

## 12. Testing

- Write **unit tests** for all public methods.
- Use **pytest** and **pytest-asyncio** for testing.
- Write integration tests for critical workflows.
- Use **assertions** to validate expected outcomes.
- Aim for meaningful test names and maintainable test code.
- Test classes must follow the same package structure as the code under test.

Example:
```python
import pytest
from httpx import AsyncClient
from backend.main import app

@pytest.mark.asyncio
async def test_create_user():
    """Test user creation endpoint."""
    async with AsyncClient(app=app, base_url="http://test") as client:
        response = await client.post(
            "/api/users",
            json={"email": "test@example.com", "password": "Test123!", "name": "Test User"}
        )
        assert response.status_code == 201
        assert response.json()["email"] == "test@example.com"
```

---

## 13. Integration Testing Guidelines

- Use `pytest` with `AsyncClient` from `httpx` for API integration tests.
- Use `pytest-asyncio` for async test functions.
- Use **Testcontainers** (via `testcontainers-python`) for external dependencies (e.g., databases, Redis).
  - External services **must not be embedded in the codebase**.
  - Start and configure containers within the test setup.
  - Ensure containers are automatically started and stopped during tests.
  - Example: `PostgresContainer`, `RedisContainer`.
- Always clean up test data between runs (e.g., using database transactions or test-specific DBs).
- Name integration test files with the suffix `_integration` (e.g., `test_user_integration.py`).
- Avoid depending on external systems outside the test environment — use containers instead.
- Ensure integration tests run reliably in CI/CD environments.

---

## 14. FastAPI Best Practices

- Use **FastAPI dependency injection** instead of manual instantiation.
- Prefer **async/await** for all I/O operations.
- Use **`@router.get`, `@router.post`, etc.** decorators for route handlers.
- Use **Pydantic schemas** for API input/output, avoid exposing ORM models directly.
- Externalize configuration using environment variables and `pydantic-settings`.
- Secure sensitive information (e.g., passwords, API keys) via **environment variables**.
- Use **response models** to document API responses.
- Use **status codes** appropriately (e.g., `201` for creation, `204` for deletion).

---

## 15. Database Best Practices

- Use **SQLAlchemy 2.0** async API for all database operations.
- Use **Alembic** for database migrations.
- Always use **parameterized queries** (SQLAlchemy handles this automatically).
- Avoid N+1 queries by using eager loading (`selectinload`, `joinedload`).
- Use **database transactions** for operations that must be atomic.
- Use **UUIDs** for primary keys in database entities to prevent walkable IDs.

---

## 16. Performance & Maintainability

- Avoid unnecessary object creation.
- Use async/await for concurrent I/O operations.
- Ensure database queries are optimized with proper indexes.
- Keep dependencies minimal and up-to-date.
- Use connection pooling for database connections.
- Consider caching for frequently accessed data (Redis).

---

## 17. General Principles

- Favor **readability over cleverness**.
- Strive for **clean, maintainable, and testable** code.
- Code should follow **SOLID principles** and align with common FastAPI architecture patterns.
- Treat warnings as errors—eliminate linter warnings.
- Follow **PEP 8** style guidelines (enforced by `black` and `flake8`).

---

## 18. Forbidden Practices

The following practices are **strictly prohibited** and must never be used:

- ❌ Hardcoding secrets (API keys, passwords, tokens, DB credentials).
- ❌ Disabling security features (CORS, HTTPS, etc.).
- ❌ Using deprecated APIs or libraries.
- ❌ Writing raw SQL directly in code (use SQLAlchemy ORM or Alembic migrations).
- ❌ Catching generic `Exception` without proper handling.
- ❌ Swallowing exceptions (e.g., empty except blocks).
- ❌ Using global state for services, repositories, or models.
- ❌ Directly exposing ORM models in REST endpoints (must use Pydantic schemas).
- ❌ Committing test/demo code (e.g., `TODO`, temporary mocks) into production.
- ❌ Copy-pasting duplicate code instead of refactoring.
- ❌ Using synchronous I/O in async functions (use async libraries).

---

## 19. Security Best Practices

- Use **UUIDs** for primary keys in database entities to prevent walkable IDs that could expose internal system structure or data enumeration.
- Avoid using sequential integer IDs in REST APIs as they can be easily guessed and exploited for unauthorized access.
- When exposing entity identifiers in API responses, ensure they are UUIDs rather than auto-incrementing integers.
- UUIDs help prevent enumeration attacks and make it harder for malicious users to discover or access resources by guessing IDs.
- Always hash passwords using **bcrypt** (via `passlib`).
- Use **JWT tokens** for authentication (via `python-jose`).
- Validate all input data using Pydantic models.
- Use HTTPS in production.
- Implement rate limiting for authentication endpoints.

---

## 20. Feature Implementation Requirements

When implementing any new feature, the following components **must** be created:

### REST API Implementation
- ✅ **FastAPI Router**: Create a router module in the `routers` package with proper FastAPI decorators (`@router.get`, `@router.post`, etc.)
- ✅ **API Endpoints**: Implement all required REST endpoints using appropriate HTTP methods (GET, POST, PUT, DELETE, PATCH)
- ✅ **Request/Response Schemas**: Create Pydantic schemas in the `schemas` package for all request and response payloads
- ✅ **OpenAPI Documentation**: Document all endpoints with descriptions, response models, and status codes
- ✅ **UUID-based Resources**: All resources exposed via REST APIs must use UUIDs for identification

### Database Changes
- ✅ **Alembic Migration**: Create a new migration script in `alembic/versions/`
- ✅ **Migration Naming**: Use descriptive names following pattern: `{revision}_{description}.py` (e.g., `001_create_users_table.py`)
- ✅ **Idempotency**: Ensure migrations are idempotent and safe to run multiple times
- ✅ **UUID Primary Keys**: Use UUID type for all primary keys in new tables
- ❌ **Never Modify Existing Migrations**: Once a migration has been applied to a database, it must **never** be modified. Alembic tracks migration history and will fail if a migration is modified after it has been executed.
- ✅ **Always Create New Migrations**: If you need to make additional database changes, create a **new migration** with a new revision (e.g., if `011_create_event_indexes.py` exists and you need to add another index, create `012_add_event_deleted_at_index.py`).
- ✅ **Migration Immutability**: Treat migrations as immutable once they have been applied. This ensures database migration history remains consistent and reproducible across all environments.

### Service Layer
- ✅ **Service Module**: Create service module in the `services` package
- ✅ **Async Functions**: Use async/await for all service methods
- ✅ **Transaction Management**: Use database transactions where appropriate
- ✅ **Exception Handling**: Implement proper exception handling with custom exceptions

### Testing
- ✅ **Unit Tests**: Write unit tests for all service methods
- ✅ **Integration Tests**: Write integration tests for REST endpoints using `AsyncClient`
- ✅ **Model Tests**: Write tests for database models if they contain business logic

### Implementation Checklist
For every feature, verify:
- [ ] FastAPI router created with proper decorators
- [ ] Service module created with async functions
- [ ] SQLAlchemy model classes created
- [ ] Pydantic schemas created for request/response
- [ ] Alembic migration created
- [ ] OpenAPI documentation added to endpoints
- [ ] Unit tests written and passing
- [ ] Integration tests written and passing
- [ ] Error handling implemented
- [ ] Security/authorization checks implemented
- [ ] Logging added where appropriate

---
