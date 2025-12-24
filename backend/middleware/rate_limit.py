"""
Rate limiting middleware using in-memory storage (Redis support can be added later)
"""

import os
import time
from typing import Callable, Dict, Tuple
from collections import defaultdict
from fastapi import Request, Response, status
from fastapi.responses import JSONResponse
from starlette.middleware.base import BaseHTTPMiddleware
import structlog

logger = structlog.get_logger()

# Configure rate limits
RATE_LIMIT_ENABLED = os.getenv("RATE_LIMIT_ENABLED", "true").lower() == "true"

# Rate limit configurations (requests per time window)
# Format: (max_requests, time_window_seconds)
RATE_LIMITS = {
    "login": (5, 60),  # 5 requests per minute
    "general": (100, 60),  # 100 requests per minute
    "upload": (10, 60),  # 10 requests per minute
}

# In-memory storage for rate limiting (fallback if Redis not available)
# In production, this should use Redis for distributed rate limiting
_rate_limit_storage: Dict[str, list] = defaultdict(list)


def get_rate_limit_key(request: Request) -> str:
    """
    Determine rate limit key based on endpoint.
    
    Args:
        request: FastAPI request object
        
    Returns:
        Rate limit key ("login", "upload", or "general")
    """
    path = request.url.path
    
    # Login endpoints
    if "/login" in path or "/auth/login" in path:
        return "login"
    
    # File upload endpoints
    if any(upload_path in path for upload_path in ["/upload", "/voice/upload", "/audio-cuts/upload", "/copy/upload", "/order-attachments/upload"]):
        return "upload"
    
    # Default to general API limit
    return "general"


def get_client_identifier(request: Request) -> str:
    """
    Get unique identifier for client (IP address or user ID if authenticated).
    
    Args:
        request: FastAPI request object
        
    Returns:
        Client identifier string
    """
    # Try to get user ID from request state (if authenticated)
    if hasattr(request.state, "username"):
        return f"user:{request.state.username}"
    
    # Fall back to IP address
    client_ip = request.client.host if request.client else "unknown"
    return f"ip:{client_ip}"


def check_rate_limit(client_id: str, limit_key: str) -> Tuple[bool, int]:
    """
    Check if client has exceeded rate limit.
    
    Args:
        client_id: Client identifier
        limit_key: Rate limit key ("login", "upload", "general")
        
    Returns:
        Tuple of (is_allowed, retry_after_seconds)
    """
    if limit_key not in RATE_LIMITS:
        limit_key = "general"
    
    max_requests, time_window = RATE_LIMITS[limit_key]
    storage_key = f"{client_id}:{limit_key}"
    current_time = time.time()
    
    # Clean old entries (outside time window)
    request_times = _rate_limit_storage[storage_key]
    request_times[:] = [t for t in request_times if current_time - t < time_window]
    
    # Check if limit exceeded
    if len(request_times) >= max_requests:
        # Calculate retry after (time until oldest request expires)
        oldest_request = min(request_times) if request_times else current_time
        retry_after = int(time_window - (current_time - oldest_request)) + 1
        return False, retry_after
    
    # Record this request
    request_times.append(current_time)
    return True, 0


class RateLimitMiddleware(BaseHTTPMiddleware):
    """Middleware for rate limiting API requests"""
    
    async def dispatch(self, request: Request, call_next: Callable) -> Response:
        """Process request with rate limiting"""
        
        # Skip rate limiting if disabled
        if not RATE_LIMIT_ENABLED:
            return await call_next(request)
        
        # Skip rate limiting for health checks and docs
        if request.url.path in ["/health", "/api/health", "/docs", "/redoc", "/openapi.json", "/"]:
            return await call_next(request)
        
        try:
            # Get rate limit configuration for this endpoint
            limit_key = get_rate_limit_key(request)
            client_id = get_client_identifier(request)
            
            # Check rate limit
            is_allowed, retry_after = check_rate_limit(client_id, limit_key)
            
            if not is_allowed:
                # Rate limit exceeded
                logger.warning(
                    "Rate limit exceeded",
                    path=request.url.path,
                    client_id=client_id,
                    limit_key=limit_key,
                    retry_after=retry_after
                )
                
                return JSONResponse(
                    status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                    content={
                        "detail": f"Rate limit exceeded. Maximum {RATE_LIMITS[limit_key][0]} requests per {RATE_LIMITS[limit_key][1]} seconds allowed.",
                        "retry_after": retry_after
                    },
                    headers={"Retry-After": str(retry_after)}
                )
            
            # Process request
            response = await call_next(request)
            return response
                
        except Exception as e:
            logger.error("Rate limiting error", error=str(e), exc_info=True)
            # On error, allow request to proceed (fail open)
            return await call_next(request)
