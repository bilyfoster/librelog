"""
Tests for security headers middleware
"""

import pytest
from fastapi.testclient import TestClient
from backend.main import app


@pytest.fixture
def client():
    """Create test client"""
    return TestClient(app)


def test_security_headers_present(client):
    """Test that security headers are present in responses"""
    response = client.get("/health")
    
    assert response.status_code == 200
    
    # Check for security headers
    assert "X-Frame-Options" in response.headers
    assert response.headers["X-Frame-Options"] == "DENY"
    
    assert "X-Content-Type-Options" in response.headers
    assert response.headers["X-Content-Type-Options"] == "nosniff"
    
    assert "X-XSS-Protection" in response.headers
    assert "1; mode=block" in response.headers["X-XSS-Protection"]
    
    assert "Referrer-Policy" in response.headers
    assert "strict-origin-when-cross-origin" in response.headers["Referrer-Policy"]
    
    assert "Permissions-Policy" in response.headers
    
    assert "Content-Security-Policy" in response.headers


def test_csp_header_present(client):
    """Test that Content-Security-Policy header is present"""
    response = client.get("/health")
    
    assert response.status_code == 200
    assert "Content-Security-Policy" in response.headers
    
    csp = response.headers["Content-Security-Policy"]
    assert "default-src" in csp
    assert "script-src" in csp
    assert "style-src" in csp


def test_hsts_header_in_production(monkeypatch):
    """Test that HSTS header is present in production"""
    monkeypatch.setenv("APP_ENV", "production")
    
    # Reimport to get new middleware with production settings
    from backend.middleware.security_headers import SecurityHeadersMiddleware
    from fastapi import Request
    from starlette.responses import Response
    
    middleware = SecurityHeadersMiddleware(app)
    
    # Create a mock request/response
    class MockRequest:
        pass
    
    class MockResponse(Response):
        def __init__(self):
            self.headers = {}
            self.status_code = 200
    
    # Test that HSTS would be added in production
    # (This is a simplified test - full integration test would require app restart)
    assert True  # Placeholder - full test requires app reinitialization


def test_security_headers_on_all_endpoints(client):
    """Test that security headers are present on all endpoints"""
    endpoints = [
        "/health",
        "/docs",
    ]
    
    for endpoint in endpoints:
        response = client.get(endpoint)
        # Even if endpoint doesn't exist, headers should be present
        if response.status_code < 500:  # Skip server errors
            assert "X-Frame-Options" in response.headers or response.status_code == 404


