"""
Custom middleware for LibreLog API
"""

import time
import structlog
from fastapi import Request, Response, HTTPException, status
from starlette.middleware.base import BaseHTTPMiddleware
from backend.auth.oauth2 import verify_token
from backend.database import get_db
from sqlalchemy.ext.asyncio import AsyncSession
from backend.auth.oauth2 import get_user_by_username

logger = structlog.get_logger()


class LoggingMiddleware(BaseHTTPMiddleware):
    """Middleware for request/response logging"""
    
    async def dispatch(self, request: Request, call_next):
        start_time = time.time()
        
        # Log request
        logger.info(
            "Request started",
            method=request.method,
            url=str(request.url),
            client_ip=request.client.host if request.client else None
        )
        
        # Process request
        response = await call_next(request)
        
        # Calculate processing time
        process_time = time.time() - start_time
        
        # Log response
        logger.info(
            "Request completed",
            method=request.method,
            url=str(request.url),
            status_code=response.status_code,
            process_time=process_time
        )
        
        return response


class AuthMiddleware(BaseHTTPMiddleware):
    """Middleware for JWT token validation"""
    
    async def dispatch(self, request: Request, call_next):
        # Skip auth for health check, docs, and public endpoints
        # NOTE: Traefik may or may not strip /api prefix depending on configuration
        # Check both with and without /api prefix
        path = request.url.path
        path_without_api = path.replace("/api", "", 1) if path.startswith("/api") else path
        
        if (path in ["/api/health", "/health", "/docs", "/redoc", "/openapi.json", "/"] or 
            path_without_api in ["/health", "/docs", "/redoc", "/openapi.json", "/"] or
            path.startswith("/api/auth") or 
            path.startswith("/api/setup") or 
            path.startswith("/auth") or 
            path.startswith("/setup") or
            path.endswith("/preview") or  # Preview endpoints are public (redirects to LibreTime)
            path_without_api.endswith("/preview") or  # Also check without /api prefix
            path.startswith("/api/settings/branding/logo/") or  # Logo files are public
            path_without_api.startswith("/settings/branding/logo/") or  # Also check without /api prefix
            path == "/api/settings/branding/public" or  # Public branding endpoint
            path_without_api == "/settings/branding/public"):  # Also check without /api prefix
            return await call_next(request)
        
        # Extract token from Authorization header
        authorization = request.headers.get("Authorization")
        if not authorization:
            logger.warning("Missing Authorization header", path=path)
            return Response(
                content='{"detail":"Not authenticated"}',
                status_code=status.HTTP_401_UNAUTHORIZED,
                media_type="application/json",
                headers={"WWW-Authenticate": "Bearer"}
            )
        
        # Parse Bearer token
        try:
            scheme, token = authorization.split()
            if scheme.lower() != "bearer":
                raise ValueError("Invalid scheme")
        except ValueError:
            logger.warning("Invalid Authorization header format", path=path)
            return Response(
                content='{"detail":"Invalid authentication scheme"}',
                status_code=status.HTTP_401_UNAUTHORIZED,
                media_type="application/json",
                headers={"WWW-Authenticate": "Bearer"}
            )
        
        # Verify token
        payload = verify_token(token)
        if payload is None:
            logger.warning("Invalid or expired token", path=path)
            return Response(
                content='{"detail":"Could not validate credentials"}',
                status_code=status.HTTP_401_UNAUTHORIZED,
                media_type="application/json",
                headers={"WWW-Authenticate": "Bearer"}
            )
        
        # Extract username from payload
        username = payload.get("sub")
        if not username:
            logger.warning("Token missing username", path=path)
            return Response(
                content='{"detail":"Invalid token payload"}',
                status_code=status.HTTP_401_UNAUTHORIZED,
                media_type="application/json",
                headers={"WWW-Authenticate": "Bearer"}
            )
        
        # Attach user info to request state for use in route handlers
        request.state.username = username
        request.state.token_payload = payload
        
        # Continue with request
        return await call_next(request)
