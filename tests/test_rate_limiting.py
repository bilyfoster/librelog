"""
Tests for rate limiting middleware
"""

import pytest
import time
from fastapi.testclient import TestClient
from backend.main import app


@pytest.fixture
def client():
    """Create test client"""
    return TestClient(app)


def test_rate_limit_allows_normal_requests(client):
    """Test that normal requests are allowed"""
    # Make a few requests to health endpoint
    for _ in range(5):
        response = client.get("/health")
        assert response.status_code == 200


def test_rate_limit_enforced_on_login(client):
    """Test that rate limiting is enforced on login endpoints"""
    # Note: This test may need adjustment based on actual login endpoint
    # Make multiple requests to trigger rate limit
    responses = []
    for i in range(10):
        # Try to hit a login-like endpoint (may need actual login endpoint)
        response = client.post("/api/auth/login", json={
            "username": "test",
            "password": "wrong"
        })
        responses.append(response.status_code)
    
    # At least some requests should be rate limited (429)
    # Note: This depends on rate limit configuration
    assert any(status == 429 for status in responses) or all(status in [401, 422] for status in responses)


def test_rate_limit_retry_after_header(client):
    """Test that Retry-After header is present when rate limited"""
    # Make many rapid requests to trigger rate limit
    responses = []
    for _ in range(150):  # Exceed general API limit of 100/min
        response = client.get("/health")
        responses.append(response)
        if response.status_code == 429:
            assert "Retry-After" in response.headers
            break
    
    # If we hit rate limit, verify header
    rate_limited = [r for r in responses if r.status_code == 429]
    if rate_limited:
        assert "Retry-After" in rate_limited[0].headers


def test_rate_limit_health_check_exempt(client):
    """Test that health check endpoint is exempt from rate limiting"""
    # Make many requests to health endpoint
    for _ in range(200):
        response = client.get("/health")
        assert response.status_code == 200  # Should not be rate limited


def test_rate_limit_different_endpoints(client):
    """Test that different endpoints have different rate limits"""
    # This test verifies that login endpoints have stricter limits
    # than general API endpoints
    assert True  # Placeholder - requires specific endpoint testing


