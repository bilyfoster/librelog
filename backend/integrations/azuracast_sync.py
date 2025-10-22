"""
AzuraCast sync service for now-playing data
"""

import asyncio
from datetime import datetime
from typing import Optional
from backend.integrations.azuracast_client import azuracast_client
from backend.integrations.libretime_client import libretime_client
import structlog

logger = structlog.get_logger()


class AzuraCastSync:
    """Service for syncing now-playing data between LibreTime and AzuraCast"""
    
    def __init__(self):
        self.is_running = False
        self.sync_interval = 30  # seconds
    
    async def start_sync(self):
        """Start the sync service"""
        if self.is_running:
            logger.warning("AzuraCast sync is already running")
            return
        
        self.is_running = True
        logger.info("Starting AzuraCast sync service")
        
        while self.is_running:
            try:
                await self._sync_now_playing()
                await asyncio.sleep(self.sync_interval)
            except Exception as e:
                logger.error("Error in AzuraCast sync", error=str(e))
                await asyncio.sleep(self.sync_interval)
    
    def stop_sync(self):
        """Stop the sync service"""
        self.is_running = False
        logger.info("Stopping AzuraCast sync service")
    
    async def _sync_now_playing(self):
        """Sync now-playing data from LibreTime to AzuraCast"""
        try:
            # Get current track from LibreTime
            now_playing = await libretime_client.get_now_playing()
            if not now_playing:
                return
            
            # Update AzuraCast with current track info
            track_data = {
                "title": now_playing.get("title", ""),
                "artist": now_playing.get("artist", ""),
                "album": now_playing.get("album", ""),
                "duration": now_playing.get("duration", 0),
                "start_time": now_playing.get("start_time", datetime.utcnow().isoformat()),
            }
            
            success = await azuracast_client.update_now_playing(track_data)
            if success:
                logger.debug("Successfully synced now-playing to AzuraCast")
            else:
                logger.warning("Failed to sync now-playing to AzuraCast")
                
        except Exception as e:
            logger.error("Error syncing now-playing data", error=str(e))
    
    async def get_listener_stats(self) -> Optional[dict]:
        """Get listener statistics from AzuraCast"""
        try:
            listeners = await azuracast_client.get_listeners()
            if listeners:
                return {
                    "total_listeners": listeners.get("total", 0),
                    "unique_listeners": listeners.get("unique", 0),
                    "timestamp": datetime.utcnow().isoformat()
                }
        except Exception as e:
            logger.error("Error getting listener stats", error=str(e))
        
        return None
    
    async def get_song_history(self, limit: int = 10) -> list:
        """Get recent song history from AzuraCast"""
        try:
            return await azuracast_client.get_song_history(limit)
        except Exception as e:
            logger.error("Error getting song history", error=str(e))
            return []


# Global sync service instance
azuracast_sync = AzuraCastSync()
