"""
Token management for external API integrations
"""

import os
import json
from typing import Dict, Optional
from datetime import datetime, timedelta
import httpx
import structlog

logger = structlog.get_logger()


class TokenManager:
    """Manages authentication tokens for external APIs"""
    
    def __init__(self):
        self.tokens: Dict[str, Dict] = {}
        self.api_keys: Dict[str, str] = {
            "libretime": os.getenv("LIBRETIME_API_KEY"),
            "azuracast": os.getenv("AZURACAST_API_KEY"),
        }
    
    def get_api_key(self, service: str) -> Optional[str]:
        """Get API key for a service"""
        return self.api_keys.get(service)
    
    def store_token(self, service: str, token: str, expires_in: int = 3600):
        """Store a token with expiration"""
        expires_at = datetime.utcnow() + timedelta(seconds=expires_in)
        self.tokens[service] = {
            "token": token,
            "expires_at": expires_at,
            "type": "Bearer"
        }
        logger.info("Token stored", service=service, expires_at=expires_at)
    
    def get_token(self, service: str) -> Optional[str]:
        """Get a valid token for a service"""
        token_data = self.tokens.get(service)
        if not token_data:
            return None
        
        if datetime.utcnow() >= token_data["expires_at"]:
            logger.warning("Token expired", service=service)
            return None
        
        return token_data["token"]
    
    def is_token_valid(self, service: str) -> bool:
        """Check if token is valid and not expired"""
        token_data = self.tokens.get(service)
        if not token_data:
            return False
        
        return datetime.utcnow() < token_data["expires_at"]
    
    async def refresh_token(self, service: str) -> bool:
        """Refresh token for a service (placeholder)"""
        # TODO: Implement token refresh logic for each service
        logger.info("Token refresh requested", service=service)
        return False
    
    def get_auth_header(self, service: str) -> Optional[Dict[str, str]]:
        """Get authorization header for a service"""
        token = self.get_token(service)
        if token:
            return {"Authorization": f"Bearer {token}"}
        
        api_key = self.get_api_key(service)
        if api_key:
            return {"Authorization": f"Bearer {api_key}"}
        
        return None


# Global token manager instance
token_manager = TokenManager()
