"""
LibreTime API client for integration
"""

import os
from typing import List, Dict, Any, Optional
from datetime import datetime, date
from pathlib import Path
import httpx
from backend.integrations.api_connector import APIConnector
import structlog

logger = structlog.get_logger()


class LibreTimeClient(APIConnector):
    """LibreTime API client"""
    
    def __init__(self):
        base_url = os.getenv("LIBRETIME_API_URL", "https://dev-studio.gayphx.com")
        # Remove /api suffix if present, we'll add it in endpoints
        if base_url.endswith("/api"):
            base_url = base_url[:-4]
        self.api_key = os.getenv("LIBRETIME_API_KEY", "")
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
            response = await self.get("/api/v2/integration/sync-status")
            return True
        except Exception as e:
            logger.error("LibreTime health check failed", error=str(e))
            return False
    
    async def upload_voice_track(
        self,
        file_path: str,
        title: str,
        description: Optional[str] = None,
        artist_name: Optional[str] = None,
        genre: Optional[str] = None
    ) -> Optional[Dict[str, Any]]:
        """Upload a voice track file to LibreTime"""
        try:
            url = f"{self.base_url}/api/v2/integration/voice-tracks"
            
            # Read file content
            with open(file_path, 'rb') as f:
                file_content = f.read()
            
            # Prepare form data
            files = {
                'file': (Path(file_path).name, file_content, 'audio/mpeg')
            }
            data = {
                'title': title,
            }
            if description:
                data['description'] = description
            if artist_name:
                data['artist_name'] = artist_name
            if genre:
                data['genre'] = genre
            
            headers = {
                'Authorization': f'Api-Key {self.api_key}'
            }
            
            async with httpx.AsyncClient(timeout=60.0) as client:
                response = await client.post(
                    url,
                    files=files,
                    data=data,
                    headers=headers
                )
                response.raise_for_status()
                return response.json()
        except Exception as e:
            logger.error("Failed to upload voice track to LibreTime", error=str(e), file_path=file_path)
            return None
    
    async def get_media_library(
        self,
        limit: int = 100,
        offset: int = 0
    ) -> Dict[str, Any]:
        """Get media library from LibreTime for sync"""
        try:
            # Use direct httpx call to avoid token_manager interference
            url = f"{self.base_url}/api/v2/integration/media-library"
            headers = {"Authorization": f"Api-Key {self.api_key}"}
            
            async with httpx.AsyncClient(timeout=60.0) as client:
                response = await client.get(
                    url,
                    params={"limit": limit, "offset": offset},
                    headers=headers
                )
                response.raise_for_status()
                return response.json()
        except Exception as e:
            logger.error("Failed to get media library from LibreTime", error=str(e))
            return {"results": [], "count": 0, "limit": limit, "offset": offset}
    
    async def get_track_detail(self, track_id: int) -> Optional[Dict[str, Any]]:
        """Get detailed track information from LibreTime"""
        try:
            response = await self._make_request(
                "GET",
                f"/api/v2/integration/track/{track_id}",
                headers={"Authorization": f"Api-Key {self.api_key}"}
            )
            return response.json()
        except Exception as e:
            logger.error("Failed to get track detail from LibreTime", track_id=track_id, error=str(e))
            return None
    
    async def sync_track_types(self) -> bool:
        """Ensure track type compatibility between systems"""
        # This would check that all LibreLog types exist in LibreTime
        # For now, we'll just return True as the VOT type should be added via management command
        return True
    
    async def ensure_track_types_exist(self) -> Dict[str, Any]:
        """Ensure all LibreLog track types exist in LibreTime"""
        from backend.integrations.type_mapping import VALID_LIBRELOG_TYPES
        
        # This would call a LibreTime endpoint to sync types
        # For now, this is handled by the management command
        return {
            "status": "success",
            "message": "Track types should be synced via LibreTime management command"
        }


# Global LibreTime client instance
libretime_client = LibreTimeClient()
