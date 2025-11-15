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
        """Get tracks from LibreTime using library-full endpoint"""
        try:
            response = await self.get("/files/library-full", params={"limit": limit, "offset": offset})
            return response.get("results", [])
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
        """Publish schedule to LibreTime using replace-day endpoint"""
        try:
            # Transform schedule_data to match replace-day format
            entries = []
            for item in schedule_data:
                entries.append({
                    "start": item.get("start") or item.get("starts_at"),
                    "media_id": item.get("media_id") or item.get("file_id"),
                    "type": item.get("type", "track"),
                    "hard_start": item.get("hard_start", False)
                })
            
            response = await self.post("/schedule/replace-day", data={
                "date": date.isoformat(),
                "entries": entries
            })
            return response.get("success", False)
        except Exception as e:
            logger.error("Failed to publish schedule to LibreTime", date=date.isoformat(), error=str(e))
            return False
    
    async def get_schedule_status(self, date: date) -> Dict[str, Any]:
        """Get schedule status for a specific date"""
        try:
            response = await self.get("/schedule/status", params={"date": date.isoformat()})
            return response
        except Exception as e:
            logger.error("Failed to get schedule status from LibreTime", date=date.isoformat(), error=str(e))
            return {}
    
    async def get_playback_history(self, start_date: date, end_date: date) -> List[Dict[str, Any]]:
        """Get playback history from LibreTime using playout-history-full endpoint"""
        try:
            # Convert dates to ISO datetime strings
            start_datetime = datetime.combine(start_date, datetime.min.time()).isoformat() + "Z"
            end_datetime = datetime.combine(end_date, datetime.max.time()).isoformat() + "Z"
            
            response = await self.get("/playout-history/playout-history-full", params={
                "start": start_datetime,
                "end": end_datetime
            })
            return response.get("results", [])
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
    
    async def health_check(self) -> bool:
        """Check if LibreTime API is accessible"""
        try:
            # Try to get a small batch of tracks to verify connection
            response = await self.get("/files/library-full", params={"limit": 1, "offset": 0})
            return True
        except Exception as e:
            logger.error("LibreTime health check failed", error=str(e))
            return False


# Global LibreTime client instance
libretime_client = LibreTimeClient()
