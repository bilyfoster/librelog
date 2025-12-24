"""
Token blacklist service for managing revoked JWT tokens
"""

import os
import json
from datetime import datetime, timedelta
from typing import Optional
import structlog

logger = structlog.get_logger()

# Try to use Redis if available, otherwise use in-memory storage
redis_url = os.getenv("RATE_LIMIT_REDIS_URL") or os.getenv("REDIS_URL")
redis_client = None

if redis_url:
    try:
        from redis import Redis
        redis_client = Redis.from_url(redis_url, decode_responses=True)
        logger.info("Token blacklist using Redis", redis_url=redis_url)
    except Exception as e:
        logger.warning("Failed to connect to Redis for token blacklist, using in-memory storage", error=str(e))
        redis_client = None

# In-memory storage fallback
_token_blacklist: dict[str, datetime] = {}


class TokenBlacklistService:
    """Service for managing token blacklist"""
    
    @staticmethod
    def _get_storage_key(token: str) -> str:
        """Get storage key for token"""
        return f"blacklist:token:{token}"
    
    @staticmethod
    async def add_to_blacklist(token: str, expires_at: datetime) -> None:
        """
        Add a token to the blacklist.
        
        Args:
            token: JWT token to blacklist
            expires_at: When the token expires (used for cleanup)
        """
        storage_key = TokenBlacklistService._get_storage_key(token)
        expires_timestamp = expires_at.timestamp()
        
        if redis_client:
            try:
                # Store token with expiration time
                # Set expiration to slightly after token expiration for cleanup
                ttl = int((expires_at - datetime.utcnow()).total_seconds()) + 60  # Add 1 minute buffer
                if ttl > 0:
                    redis_client.setex(storage_key, ttl, str(expires_timestamp))
                logger.debug("Token added to Redis blacklist", token_prefix=token[:10])
            except Exception as e:
                logger.error("Failed to add token to Redis blacklist", error=str(e))
                # Fall back to in-memory
                _token_blacklist[storage_key] = expires_at
        else:
            # In-memory storage
            _token_blacklist[storage_key] = expires_at
            logger.debug("Token added to in-memory blacklist", token_prefix=token[:10])
    
    @staticmethod
    async def is_blacklisted(token: str) -> bool:
        """
        Check if a token is blacklisted.
        
        Args:
            token: JWT token to check
            
        Returns:
            True if token is blacklisted, False otherwise
        """
        storage_key = TokenBlacklistService._get_storage_key(token)
        
        if redis_client:
            try:
                exists = redis_client.exists(storage_key)
                return bool(exists)
            except Exception as e:
                logger.error("Failed to check Redis blacklist", error=str(e))
                # Fall back to in-memory
                return storage_key in _token_blacklist
        else:
            # In-memory storage
            if storage_key in _token_blacklist:
                # Check if expired
                expires_at = _token_blacklist[storage_key]
                if datetime.utcnow() > expires_at:
                    # Remove expired token
                    del _token_blacklist[storage_key]
                    return False
                return True
            return False
    
    @staticmethod
    async def cleanup_expired_tokens() -> None:
        """Clean up expired tokens from blacklist (for in-memory storage)"""
        if not redis_client:
            current_time = datetime.utcnow()
            expired_keys = [
                key for key, expires_at in _token_blacklist.items()
                if current_time > expires_at
            ]
            for key in expired_keys:
                del _token_blacklist[key]
            
            if expired_keys:
                logger.debug("Cleaned up expired tokens from blacklist", count=len(expired_keys))


