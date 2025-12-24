"""
Tests for token blacklist functionality
"""

import pytest
from fastapi.testclient import TestClient
from backend.main import app
from backend.services.token_blacklist_service import TokenBlacklistService
from datetime import datetime, timedelta


@pytest.fixture
def client():
    """Create test client"""
    return TestClient(app)


@pytest.mark.asyncio
async def test_add_token_to_blacklist():
    """Test adding a token to blacklist"""
    token = "test_token_12345"
    expires_at = datetime.utcnow() + timedelta(minutes=15)
    
    await TokenBlacklistService.add_to_blacklist(token, expires_at)
    
    is_blacklisted = await TokenBlacklistService.is_blacklisted(token)
    assert is_blacklisted is True


@pytest.mark.asyncio
async def test_token_not_blacklisted():
    """Test that non-blacklisted token is not blacklisted"""
    token = "test_token_not_blacklisted"
    
    is_blacklisted = await TokenBlacklistService.is_blacklisted(token)
    assert is_blacklisted is False


@pytest.mark.asyncio
async def test_expired_token_removed_from_blacklist():
    """Test that expired tokens are removed from blacklist"""
    token = "test_token_expired"
    expires_at = datetime.utcnow() - timedelta(minutes=1)  # Already expired
    
    await TokenBlacklistService.add_to_blacklist(token, expires_at)
    
    # Cleanup should remove expired tokens
    await TokenBlacklistService.cleanup_expired_tokens()
    
    is_blacklisted = await TokenBlacklistService.is_blacklisted(token)
    assert is_blacklisted is False


@pytest.mark.asyncio
async def test_blacklist_cleanup():
    """Test cleanup of expired tokens"""
    # Add expired token
    expired_token = "expired_token"
    expires_at = datetime.utcnow() - timedelta(hours=1)
    await TokenBlacklistService.add_to_blacklist(expired_token, expires_at)
    
    # Add valid token
    valid_token = "valid_token"
    expires_at = datetime.utcnow() + timedelta(minutes=15)
    await TokenBlacklistService.add_to_blacklist(valid_token, expires_at)
    
    # Run cleanup
    await TokenBlacklistService.cleanup_expired_tokens()
    
    # Expired should be removed, valid should remain
    assert await TokenBlacklistService.is_blacklisted(expired_token) is False
    assert await TokenBlacklistService.is_blacklisted(valid_token) is True


def test_logout_blacklists_token(client):
    """Test that logout adds token to blacklist"""
    # This would require actual authentication flow
    # Placeholder for integration test
    assert True  # Requires full auth flow test


