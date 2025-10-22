"""
Test configuration and fixtures for LibreLog
"""

import pytest
import asyncio
from sqlalchemy.ext.asyncio import AsyncSession, create_async_engine, async_sessionmaker
from sqlalchemy.pool import StaticPool
from backend.database import Base, get_db
from backend.models.user import User
from backend.auth.oauth2 import get_password_hash
from fastapi.testclient import TestClient
from backend.main import app

# Test database URL
TEST_DATABASE_URL = "sqlite+aiosqlite:///:memory:"

# Create test engine
test_engine = create_async_engine(
    TEST_DATABASE_URL,
    connect_args={"check_same_thread": False},
    poolclass=StaticPool,
)

# Create test session factory
TestSessionLocal = async_sessionmaker(
    test_engine,
    class_=AsyncSession,
    expire_on_commit=False,
)


@pytest.fixture(scope="session")
def event_loop():
    """Create an instance of the default event loop for the test session."""
    loop = asyncio.get_event_loop_policy().new_event_loop()
    yield loop
    loop.close()


@pytest.fixture(scope="function")
async def db_session():
    """Create a test database session"""
    # Create tables
    async with test_engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    
    # Create session
    async with TestSessionLocal() as session:
        yield session
    
    # Drop tables
    async with test_engine.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)


@pytest.fixture(scope="function")
def client(db_session):
    """Create a test client"""
    def override_get_db():
        return db_session
    
    app.dependency_overrides[get_db] = override_get_db
    
    with TestClient(app) as test_client:
        yield test_client
    
    app.dependency_overrides.clear()


@pytest.fixture(scope="function")
async def test_user(db_session):
    """Create a test user"""
    user = User(
        username="testuser",
        password_hash=get_password_hash("testpassword"),
        role="admin"
    )
    db_session.add(user)
    await db_session.commit()
    await db_session.refresh(user)
    return user


@pytest.fixture(scope="function")
async def test_admin_user(db_session):
    """Create a test admin user"""
    user = User(
        username="admin",
        password_hash=get_password_hash("adminpassword"),
        role="admin"
    )
    db_session.add(user)
    await db_session.commit()
    await db_session.refresh(user)
    return user


@pytest.fixture(scope="function")
async def test_producer_user(db_session):
    """Create a test producer user"""
    user = User(
        username="producer",
        password_hash=get_password_hash("producerpassword"),
        role="producer"
    )
    db_session.add(user)
    await db_session.commit()
    await db_session.refresh(user)
    return user


@pytest.fixture(scope="function")
def auth_headers(client, test_user):
    """Get authentication headers for test user"""
    response = client.post(
        "/api/auth/login",
        data={"username": test_user.username, "password": "testpassword"}
    )
    token = response.json()["access_token"]
    return {"Authorization": f"Bearer {token}"}


@pytest.fixture(scope="function")
def admin_headers(client, test_admin_user):
    """Get authentication headers for admin user"""
    response = client.post(
        "/api/auth/login",
        data={"username": test_admin_user.username, "password": "adminpassword"}
    )
    token = response.json()["access_token"]
    return {"Authorization": f"Bearer {token}"}


@pytest.fixture(scope="function")
def producer_headers(client, test_producer_user):
    """Get authentication headers for producer user"""
    response = client.post(
        "/api/auth/login",
        data={"username": test_producer_user.username, "password": "producerpassword"}
    )
    token = response.json()["access_token"]
    return {"Authorization": f"Bearer {token}"}
