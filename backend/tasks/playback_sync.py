"""
Playback sync task for reconciliation
"""

from datetime import datetime, timedelta, date
from celery import shared_task
from sqlalchemy.ext.asyncio import AsyncSession
from backend.database import AsyncSessionLocal
from backend.integrations.libretime_client import libretime_client
from backend.models.playback_history import PlaybackHistory
from backend.models.track import Track
from backend.models.daily_log import DailyLog
from sqlalchemy import select
import structlog

logger = structlog.get_logger()


@shared_task
async def sync_playback_history(start_date: str, end_date: str):
    """Sync playback history from LibreTime"""
    logger.info("Starting playback history sync", start_date=start_date, end_date=end_date)
    
    async with AsyncSessionLocal() as db:
        try:
            # Parse dates
            start = datetime.fromisoformat(start_date).date()
            end = datetime.fromisoformat(end_date).date()
            
            # Get playback history from LibreTime
            playback_data = await libretime_client.get_playback_history(start, end)
            
            if not playback_data:
                logger.warning("No playback history received from LibreTime")
                return
            
            synced_count = 0
            
            for play_data in playback_data:
                track_id = play_data.get("track_id")
                log_id = play_data.get("log_id")
                played_at = play_data.get("played_at")
                duration_played = play_data.get("duration_played")
                
                if not all([track_id, log_id, played_at]):
                    continue
                
                # Check if playback record already exists
                result = await db.execute(
                    select(PlaybackHistory).where(
                        PlaybackHistory.track_id == track_id,
                        PlaybackHistory.log_id == log_id,
                        PlaybackHistory.played_at == datetime.fromisoformat(played_at)
                    )
                )
                existing_playback = result.scalar_one_or_none()
                
                if not existing_playback:
                    # Create new playback record
                    new_playback = PlaybackHistory(
                        track_id=track_id,
                        log_id=log_id,
                        played_at=datetime.fromisoformat(played_at),
                        duration_played=duration_played
                    )
                    db.add(new_playback)
                    synced_count += 1
            
            await db.commit()
            
            logger.info(
                "Playback history sync completed",
                synced_count=synced_count,
                total_records=len(playback_data)
            )
            
        except Exception as e:
            logger.error("Playback history sync failed", error=str(e))
            await db.rollback()
            raise


@shared_task
async def daily_playback_sync():
    """Daily playback sync for yesterday's data"""
    yesterday = date.today() - timedelta(days=1)
    start_date = yesterday.isoformat()
    end_date = yesterday.isoformat()
    
    await sync_playback_history.delay(start_date, end_date)


@shared_task
async def weekly_playback_sync():
    """Weekly playback sync for the past week"""
    end_date = date.today() - timedelta(days=1)
    start_date = end_date - timedelta(days=7)
    
    await sync_playback_history.delay(start_date.isoformat(), end_date.isoformat())
