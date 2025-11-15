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
        """Refresh token for a service"""
        logger.info("Token refresh requested", service=service)
        
        # Services that use API keys don't need refresh
        if service in ["libretime", "azuracast"]:
            logger.info("Service uses API key, no refresh needed", service=service)
            return True
        
        # Check if token exists and is expired
        token_data = self.tokens.get(service)
        if not token_data:
            logger.warning("No token to refresh", service=service)
            return False
        
        # If token is not expired yet, no need to refresh
        if datetime.utcnow() < token_data["expires_at"]:
            logger.info("Token still valid, no refresh needed", service=service)
            return True
        
        # For services that support OAuth2 refresh tokens
        # This is a framework for future implementations
        refresh_token_key = f"{service}_refresh_token"
        refresh_token = os.getenv(refresh_token_key.upper())
        
        if not refresh_token:
            logger.warning("No refresh token available", service=service)
            return False
        
        # Attempt to refresh using OAuth2 refresh token flow
        # This is a generic implementation that can be customized per service
        try:
            token_url = os.getenv(f"{service.upper()}_TOKEN_URL")
            client_id = os.getenv(f"{service.upper()}_CLIENT_ID")
            client_secret = os.getenv(f"{service.upper()}_CLIENT_SECRET")
            
            if not all([token_url, client_id, client_secret]):
                logger.warning("OAuth2 credentials not configured", service=service)
                return False
            
            async with httpx.AsyncClient() as client:
                response = await client.post(
                    token_url,
                    data={
                        "grant_type": "refresh_token",
                        "refresh_token": refresh_token,
                        "client_id": client_id,
                        "client_secret": client_secret,
                    },
                    timeout=10.0
                )
                
                if response.status_code == 200:
                    data = response.json()
                    new_token = data.get("access_token")
                    expires_in = data.get("expires_in", 3600)
                    
                    if new_token:
                        self.store_token(service, new_token, expires_in)
                        logger.info("Token refreshed successfully", service=service)
                        return True
                    else:
                        logger.error("Refresh response missing access_token", service=service)
                        return False
                else:
                    logger.error("Token refresh failed", service=service, status=response.status_code)
                    return False
                    
        except Exception as e:
            logger.error("Token refresh error", service=service, error=str(e), exc_info=True)
            return False
    
    def get_auth_header(self, service: str) -> Optional[Dict[str, str]]:
        """Get authorization header for a service"""
        token = self.get_token(service)
        if token:
            return {"Authorization": f"Bearer {token}"}
        
        api_key = self.get_api_key(service)
        if api_key:
            # LibreTime uses Api-Key format, others use Bearer
            if service == "libretime":
                return {"Authorization": f"Api-Key {api_key}"}
            return {"Authorization": f"Bearer {api_key}"}
        
        return None


# Global token manager instance
token_manager = TokenManager()
