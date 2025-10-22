"""
AzuraCast sync tasks
"""

from celery import shared_task
from backend.integrations.azuracast_sync import azuracast_sync
import structlog

logger = structlog.get_logger()


@shared_task
async def sync_now_playing():
    """Sync now-playing data to AzuraCast"""
    try:
        await azuracast_sync._sync_now_playing()
        logger.debug("Now-playing sync completed")
    except Exception as e:
        logger.error("Now-playing sync failed", error=str(e))
        raise


@shared_task
async def get_listener_stats_task():
    """Get listener statistics from AzuraCast"""
    try:
        stats = await azuracast_sync.get_listener_stats()
        return stats
    except Exception as e:
        logger.error("Failed to get listener stats", error=str(e))
        return None


@shared_task
async def get_song_history_task(limit: int = 10):
    """Get song history from AzuraCast"""
    try:
        history = await azuracast_sync.get_song_history(limit)
        return history
    except Exception as e:
        logger.error("Failed to get song history", error=str(e))
        return []
