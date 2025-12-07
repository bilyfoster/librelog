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


class RequestSizeLimitMiddleware(BaseHTTPMiddleware):
    """Middleware for limiting request body size"""
    
    # Size limits in bytes
    MAX_JSON_SIZE = 10 * 1024 * 1024  # 10MB
    MAX_UPLOAD_SIZE = 100 * 1024 * 1024  # 100MB
    
    async def dispatch(self, request: Request, call_next):
        """Check request size before processing"""
        content_length = request.headers.get("content-length")
        
        if content_length:
            try:
                size = int(content_length)
                # Determine if this is a file upload
                content_type = request.headers.get("content-type", "")
                is_upload = "multipart/form-data" in content_type or any(
                    upload_path in str(request.url.path) 
                    for upload_path in ["/upload", "/voice/upload", "/audio-cuts/upload", "/copy/upload", "/order-attachments/upload"]
                )
                
                max_size = self.MAX_UPLOAD_SIZE if is_upload else self.MAX_JSON_SIZE
                
                if size > max_size:
                    logger.warning(
                        "Request too large",
                        path=request.url.path,
                        size=size,
                        max_size=max_size,
                        client_ip=request.client.host if request.client else None
                    )
                    from fastapi import HTTPException, status
                    raise HTTPException(
                        status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE,
                        detail=f"Request body too large. Maximum size: {max_size / (1024*1024):.0f}MB"
                    )
            except ValueError:
                # Invalid content-length header, let it proceed
                pass
        
        return await call_next(request)


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
            path_without_api == "/settings/branding/public" or  # Also check without /api prefix
            path.endswith("/file") or  # Voice track file endpoints are public (for browser Audio elements)
            path_without_api.endswith("/file")):  # Also check without /api prefix
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
        
        # Verify token (includes blacklist check)
        payload = verify_token(token)
        if payload is None:
            logger.warning("Invalid, expired, or blacklisted token", path=path)
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
