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
    
    def __init__(self, config: Optional[Dict[str, Any]] = None):
        """
        Initialize LibreTime client with optional config.
        
        Args:
            config: Optional dict with keys: api_url, api_key, public_url
                   If not provided, falls back to environment variables
        """
        if config:
            # Use station-specific config
            base_url = config.get("api_url", "")
            self.api_key = config.get("api_key", "")
            self.public_url = config.get("public_url", "")
        else:
            # Fall back to environment variables (global config)
            internal_url = os.getenv("LIBRETIME_INTERNAL_URL", "")
            base_url = internal_url if internal_url else os.getenv("LIBRETIME_API_URL", "") or os.getenv("LIBRETIME_URL", "")
            self.api_key = os.getenv("LIBRETIME_API_KEY", "")
            self.public_url = os.getenv("LIBRETIME_PUBLIC_URL", "")
        
        # Remove /api suffix if present, we'll add it in endpoints
        if base_url:
            base_url = base_url.rstrip("/api/v2").rstrip("/api").rstrip("/")
        else:
            # Default fallback - should not be used in production
            # LIBRETIME_INTERNAL_URL or LIBRETIME_API_URL should be set
            logger.warning("No LibreTime URL configured, using fallback. Set LIBRETIME_INTERNAL_URL or LIBRETIME_API_URL environment variable.")
            base_url = "https://dev-studio.gayphx.com"
        
        self.base_url = base_url
        super().__init__(base_url, "libretime")
    
    def _get_auth_headers(self) -> Dict[str, str]:
        """Get authorization headers for LibreTime API"""
        if self.api_key:
            return {"Authorization": f"Api-Key {self.api_key}"}
        return {}
    
    # Override base class methods to add LibreTime auth headers
    async def get(self, endpoint: str, params: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        """Make GET request with LibreTime auth"""
        headers = self._get_auth_headers()
        response = await self._make_request("GET", endpoint, params=params, headers=headers)
        return response.json()
    
    async def post(self, endpoint: str, data: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        """Make POST request with LibreTime auth"""
        headers = self._get_auth_headers()
        response = await self._make_request("POST", endpoint, data=data, headers=headers)
        return response.json()
    
    async def put(self, endpoint: str, data: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        """Make PUT request with LibreTime auth"""
        headers = self._get_auth_headers()
        response = await self._make_request("PUT", endpoint, data=data, headers=headers)
        return response.json()
    
    async def delete(self, endpoint: str) -> Dict[str, Any]:
        """Make DELETE request with LibreTime auth"""
        headers = self._get_auth_headers()
        response = await self._make_request("DELETE", endpoint, headers=headers)
        return response.json()
    
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
                start = item.get("start") or item.get("starts_at")
                if isinstance(start, str):
                    # Ensure ISO format
                    if not start.endswith("Z") and "+" not in start:
                        # Add timezone if missing
                        start = start + "Z"
                elif hasattr(start, "isoformat"):
                    start = start.isoformat()
                
                media_id = item.get("media_id") or item.get("file_id")
                if media_id is None:
                    logger.warning("Skipping entry without media_id", item=item)
                    continue
                
                entries.append({
                    "start": start,
                    "media_id": int(media_id),
                    "type": item.get("type", "track"),
                    "hard_start": item.get("hard_start", False)
                })
            
            if not entries:
                logger.warning("No entries to publish", date=date.isoformat())
                return False
            
            response = await self.post("/api/v2/integration/schedule/replace-day", data={
                "date": date.isoformat(),
                "entries": entries
            })
            return response.get("success", False)
        except Exception as e:
            logger.error("Failed to publish schedule to LibreTime", date=date.isoformat(), error=str(e))
            return False
    
    async def publish_hour_range(
        self,
        date: date,
        hour_entries: List[Dict[str, Any]],
        hours_to_replace: List[int]
    ) -> bool:
        """
        Publish specific hours to LibreTime by replacing them in the existing schedule
        
        Args:
            date: Target date
            hour_entries: New entries for the hours being replaced
            hours_to_replace: List of hours (0-23) to replace
        
        Returns:
            True if successful
        """
        try:
            # Group entries by hour
            entries_by_hour = {}
            for entry in hour_entries:
                start = entry.get("start") or entry.get("starts_at")
                if isinstance(start, str):
                    try:
                        entry_time = datetime.fromisoformat(start.replace("Z", "+00:00"))
                        entry_hour = entry_time.hour
                        if entry_hour not in entries_by_hour:
                            entries_by_hour[entry_hour] = []
                        entries_by_hour[entry_hour].append(entry)
                    except (ValueError, AttributeError):
                        logger.warning("Could not parse entry start time", entry=entry)
                        continue
                elif hasattr(start, "hour"):
                    entry_hour = start.hour
                    if entry_hour not in entries_by_hour:
                        entries_by_hour[entry_hour] = []
                    entries_by_hour[entry_hour].append(entry)
            
            # Replace each hour individually
            for hour in hours_to_replace:
                if hour in entries_by_hour:
                    success = await self.replace_hour(date, hour, entries_by_hour[hour])
                    if not success:
                        logger.warning("Failed to replace hour", date=date.isoformat(), hour=hour)
                        return False
                else:
                    logger.warning("No entries provided for hour", date=date.isoformat(), hour=hour)
            
            return True
            
        except Exception as e:
            logger.error("Failed to publish hour range to LibreTime", date=date.isoformat(), error=str(e))
            return False
    
    async def replace_hour(
        self,
        date: date,
        hour: int,
        entries: List[Dict[str, Any]]
    ) -> bool:
        """
        Replace a specific hour's schedule in LibreTime
        
        Args:
            date: Target date
            hour: Hour to replace (0-23)
            entries: Schedule entries for this hour
        
        Returns:
            True if successful
        """
        try:
            # Transform entries to match replace-hour format
            formatted_entries = []
            for item in entries:
                start = item.get("start") or item.get("starts_at")
                if isinstance(start, str):
                    # Ensure ISO format
                    if not start.endswith("Z") and "+" not in start:
                        # Add timezone if missing
                        start = start + "Z"
                elif hasattr(start, "isoformat"):
                    start = start.isoformat()
                
                media_id = item.get("media_id") or item.get("file_id")
                if media_id is None:
                    logger.warning("Skipping entry without media_id", item=item)
                    continue
                
                formatted_entries.append({
                    "start": start,
                    "media_id": int(media_id),
                    "type": item.get("type", "track"),
                    "hard_start": item.get("hard_start", False)
                })
            
            if not formatted_entries:
                logger.warning("No entries to publish for hour", date=date.isoformat(), hour=hour)
                return False
            
            response = await self.post("/api/v2/integration/schedule/replace-hour", data={
                "date": date.isoformat(),
                "hour": hour,
                "entries": formatted_entries
            })
            return response.get("success", False)
        except Exception as e:
            logger.error("Failed to replace hour in LibreTime", date=date.isoformat(), hour=hour, error=str(e))
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
        genre: Optional[str] = None,
        filename: Optional[str] = None  # Optional custom filename (e.g., from standardized_name)
    ) -> Optional[Dict[str, Any]]:
        """Upload a voice track file to LibreTime"""
        try:
            url = f"{self.base_url}/api/v2/integration/voice-tracks"
            
            # Read file content
            with open(file_path, 'rb') as f:
                file_content = f.read()
            
            # Use custom filename if provided (for standardized naming), otherwise use original
            upload_filename = filename if filename else Path(file_path).name
            
            # Prepare form data
            files = {
                'file': (upload_filename, file_content, 'audio/mpeg')
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
            
            headers = self._get_auth_headers()
            
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
            logger.error("Failed to upload voice track to LibreTime", error=str(e), file_path=file_path, filename=filename)
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
            headers = self._get_auth_headers()
            
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
            headers = self._get_auth_headers()
            response = await self._make_request(
                "GET",
                f"/api/v2/integration/track/{track_id}",
                headers=headers
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


# Global LibreTime client instance (for backward compatibility)
libretime_client = LibreTimeClient()


async def get_libretime_client_for_station(
    station_id: Optional[int] = None,
    db: Optional[Any] = None
) -> LibreTimeClient:
    """
    Get LibreTime client for a specific station.
    
    Args:
        station_id: Optional station ID. If provided, loads station config.
        db: Optional database session. Required if station_id is provided.
    
    Returns:
        LibreTimeClient configured for the station, or global client if no station_id
    """
    if not station_id or not db:
        # Return global client (backward compatibility)
        return libretime_client
    
    # Import here to avoid circular dependencies
    from sqlalchemy import select
    from backend.models.station import Station
    
    # Load station with config
    result = await db.execute(select(Station).where(Station.id == station_id))
    station = result.scalar_one_or_none()
    
    if not station:
        logger.warning("Station not found, using global LibreTime client", station_id=station_id)
        return libretime_client
    
    # Check if station has LibreTime config
    if station.libretime_config and isinstance(station.libretime_config, dict):
        config = station.libretime_config
        # Ensure we have required fields
        if config.get("api_url") and config.get("api_key"):
            logger.info("Using station-specific LibreTime config", station_id=station_id)
            return LibreTimeClient(config=config)
        else:
            logger.warning(
                "Station LibreTime config incomplete, using global",
                station_id=station_id,
                has_api_url=bool(config.get("api_url")),
                has_api_key=bool(config.get("api_key"))
            )
    
    # Fall back to global client
    logger.info("Using global LibreTime client (no station config)", station_id=station_id)
    return libretime_client
