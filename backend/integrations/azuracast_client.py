"""
AzuraCast API client for integration
"""

import os
from typing import Dict, Any, Optional, List
from backend.integrations.api_connector import APIConnector
import structlog

logger = structlog.get_logger()


class AzuraCastClient(APIConnector):
    """AzuraCast API client"""
    
    def __init__(self):
        base_url = os.getenv("AZURACAST_URL", "https://radio.gayphx.com/api")
        super().__init__(base_url, "azuracast")
    
    async def get_station_info(self) -> Optional[Dict[str, Any]]:
        """Get station information"""
        try:
            response = await self.get("/station")
            return response
        except Exception as e:
            logger.error("Failed to get station info from AzuraCast", error=str(e))
            return None
    
    async def get_now_playing(self) -> Optional[Dict[str, Any]]:
        """Get currently playing track"""
        try:
            response = await self.get("/nowplaying")
            return response
        except Exception as e:
            logger.error("Failed to get now playing from AzuraCast", error=str(e))
            return None
    
    async def get_listeners(self) -> Optional[Dict[str, Any]]:
        """Get listener statistics"""
        try:
            response = await self.get("/listeners")
            return response
        except Exception as e:
            logger.error("Failed to get listeners from AzuraCast", error=str(e))
            return None
    
    async def get_song_history(self, limit: int = 10) -> List[Dict[str, Any]]:
        """Get song history"""
        try:
            response = await self.get("/song-history", params={"limit": limit})
            return response.get("song_history", [])
        except Exception as e:
            logger.error("Failed to get song history from AzuraCast", error=str(e))
            return []
    
    async def update_now_playing(self, track_data: Dict[str, Any]) -> bool:
        """Update now playing metadata"""
        try:
            await self.post("/nowplaying", data=track_data)
            return True
        except Exception as e:
            logger.error("Failed to update now playing in AzuraCast", error=str(e))
            return False
    
    async def get_stream_info(self) -> Optional[Dict[str, Any]]:
        """Get stream information"""
        try:
            response = await self.get("/stream-info")
            return response
        except Exception as e:
            logger.error("Failed to get stream info from AzuraCast", error=str(e))
            return None
    
    async def get_requests(self) -> List[Dict[str, Any]]:
        """Get listener requests"""
        try:
            response = await self.get("/requests")
            return response.get("requests", [])
        except Exception as e:
            logger.error("Failed to get requests from AzuraCast", error=str(e))
            return []
    
    async def submit_request(self, track_id: str) -> bool:
        """Submit a listener request"""
        try:
            await self.post("/requests", data={"track_id": track_id})
            return True
        except Exception as e:
            logger.error("Failed to submit request to AzuraCast", track_id=track_id, error=str(e))
            return False


# Global AzuraCast client instance
azuracast_client = AzuraCastClient()
