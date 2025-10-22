"""
Base API connector with retry logic and error handling
"""

import asyncio
from typing import Dict, Any, Optional
import httpx
import structlog
from backend.auth.token_manager import token_manager

logger = structlog.get_logger()


class APIConnector:
    """Base API connector with retry logic and error handling"""
    
    def __init__(self, base_url: str, service_name: str):
        self.base_url = base_url.rstrip('/')
        self.service_name = service_name
        self.timeout = httpx.Timeout(30.0)
    
    async def _make_request(
        self,
        method: str,
        endpoint: str,
        data: Optional[Dict[str, Any]] = None,
        params: Optional[Dict[str, Any]] = None,
        headers: Optional[Dict[str, str]] = None,
        retries: int = 3
    ) -> httpx.Response:
        """Make HTTP request with retry logic"""
        url = f"{self.base_url}/{endpoint.lstrip('/')}"
        
        # Add auth headers
        auth_headers = token_manager.get_auth_header(self.service_name)
        if auth_headers:
            headers = {**(headers or {}), **auth_headers}
        
        for attempt in range(retries):
            try:
                async with httpx.AsyncClient(timeout=self.timeout) as client:
                    response = await client.request(
                        method=method,
                        url=url,
                        json=data,
                        params=params,
                        headers=headers
                    )
                    
                    if response.status_code == 401 and attempt < retries - 1:
                        # Try to refresh token
                        await token_manager.refresh_token(self.service_name)
                        continue
                    
                    response.raise_for_status()
                    return response
                    
            except httpx.TimeoutException:
                logger.warning(
                    "Request timeout",
                    service=self.service_name,
                    url=url,
                    attempt=attempt + 1
                )
                if attempt == retries - 1:
                    raise
                await asyncio.sleep(2 ** attempt)  # Exponential backoff
                
            except httpx.HTTPStatusError as e:
                logger.error(
                    "HTTP error",
                    service=self.service_name,
                    url=url,
                    status_code=e.response.status_code,
                    attempt=attempt + 1
                )
                if attempt == retries - 1:
                    raise
                await asyncio.sleep(2 ** attempt)
                
            except Exception as e:
                logger.error(
                    "Request failed",
                    service=self.service_name,
                    url=url,
                    error=str(e),
                    attempt=attempt + 1
                )
                if attempt == retries - 1:
                    raise
                await asyncio.sleep(2 ** attempt)
    
    async def get(self, endpoint: str, params: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        """Make GET request"""
        response = await self._make_request("GET", endpoint, params=params)
        return response.json()
    
    async def post(self, endpoint: str, data: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        """Make POST request"""
        response = await self._make_request("POST", endpoint, data=data)
        return response.json()
    
    async def put(self, endpoint: str, data: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        """Make PUT request"""
        response = await self._make_request("PUT", endpoint, data=data)
        return response.json()
    
    async def delete(self, endpoint: str) -> Dict[str, Any]:
        """Make DELETE request"""
        response = await self._make_request("DELETE", endpoint)
        return response.json()
    
    async def health_check(self) -> bool:
        """Check if the service is healthy"""
        try:
            await self.get("/health")
            return True
        except Exception:
            return False
