"""
LibreTime API client for integration
"""

import os
from typing import List, Dict, Any, Optional
from datetime import datetime, date
from backend.integrations.api_connector import APIConnector
import structlog

logger = structlog.get_logger()


class LibreTimeClient(APIConnector):
    """LibreTime API client"""
    
    def __init__(self):
        base_url = os.getenv("LIBRETIME_URL", "https://studio.gayphx.com/api")
        super().__init__(base_url, "libretime")
    
    async def get_tracks(self, limit: int = 100, offset: int = 0) -> List[Dict[str, Any]]:
        """Get tracks from LibreTime"""
        try:
            response = await self.get("/tracks", params={"limit": limit, "offset": offset})
            return response.get("tracks", [])
        except Exception as e:
            logger.error("Failed to get tracks from LibreTime", error=str(e))
            return []
    
    async def get_track(self, track_id: str) -> Optional[Dict[str, Any]]:
        """Get a specific track from LibreTime"""
        try:
            response = await self.get(f"/tracks/{track_id}")
            return response
        except Exception as e:
            logger.error("Failed to get track from LibreTime", track_id=track_id, error=str(e))
            return None
    
    async def get_smart_blocks(self) -> List[Dict[str, Any]]:
        """Get smart blocks from LibreTime"""
        try:
            response = await self.get("/smart-blocks")
            return response.get("smart_blocks", [])
        except Exception as e:
            logger.error("Failed to get smart blocks from LibreTime", error=str(e))
            return []
    
    async def get_smart_block(self, block_id: str) -> Optional[Dict[str, Any]]:
        """Get a specific smart block from LibreTime"""
        try:
            response = await self.get(f"/smart-blocks/{block_id}")
            return response
        except Exception as e:
            logger.error("Failed to get smart block from LibreTime", block_id=block_id, error=str(e))
            return None
    
    async def create_smart_block(self, block_data: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        """Create a smart block in LibreTime"""
        try:
            response = await self.post("/smart-blocks", data=block_data)
            return response
        except Exception as e:
            logger.error("Failed to create smart block in LibreTime", error=str(e))
            return None
    
    async def update_smart_block(self, block_id: str, block_data: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        """Update a smart block in LibreTime"""
        try:
            response = await self.put(f"/smart-blocks/{block_id}", data=block_data)
            return response
        except Exception as e:
            logger.error("Failed to update smart block in LibreTime", block_id=block_id, error=str(e))
            return None
    
    async def delete_smart_block(self, block_id: str) -> bool:
        """Delete a smart block from LibreTime"""
        try:
            await self.delete(f"/smart-blocks/{block_id}")
            return True
        except Exception as e:
            logger.error("Failed to delete smart block from LibreTime", block_id=block_id, error=str(e))
            return False
    
    async def get_schedule(self, date: date) -> List[Dict[str, Any]]:
        """Get schedule for a specific date"""
        try:
            response = await self.get(f"/schedule/{date.isoformat()}")
            return response.get("schedule", [])
        except Exception as e:
            logger.error("Failed to get schedule from LibreTime", date=date.isoformat(), error=str(e))
            return []
    
    async def publish_schedule(self, date: date, schedule_data: List[Dict[str, Any]]) -> bool:
        """Publish schedule to LibreTime"""
        try:
            await self.post(f"/schedule/{date.isoformat()}", data={"schedule": schedule_data})
            return True
        except Exception as e:
            logger.error("Failed to publish schedule to LibreTime", date=date.isoformat(), error=str(e))
            return False
    
    async def get_playback_history(self, start_date: date, end_date: date) -> List[Dict[str, Any]]:
        """Get playback history from LibreTime"""
        try:
            response = await self.get("/playback-history", params={
                "start_date": start_date.isoformat(),
                "end_date": end_date.isoformat()
            })
            return response.get("playback_history", [])
        except Exception as e:
            logger.error("Failed to get playback history from LibreTime", error=str(e))
            return []
    
    async def get_now_playing(self) -> Optional[Dict[str, Any]]:
        """Get currently playing track"""
        try:
            response = await self.get("/now-playing")
            return response
        except Exception as e:
            logger.error("Failed to get now playing from LibreTime", error=str(e))
            return None


# Global LibreTime client instance
libretime_client = LibreTimeClient()
