"""
Nightly sync task for LibreTime integration
"""

from datetime import datetime, timedelta
from celery import shared_task
from sqlalchemy.ext.asyncio import AsyncSession
from backend.database import AsyncSessionLocal
from backend.integrations.libretime_client import libretime_client
from backend.models.track import Track
from backend.models.user import User
from sqlalchemy import select, update
import structlog

logger = structlog.get_logger()


@shared_task
async def nightly_sync():
    """Nightly sync job to pull tracks from LibreTime"""
    logger.info("Starting nightly sync job")
    
    async with AsyncSessionLocal() as db:
        try:
            # Get tracks from LibreTime
            tracks_data = await libretime_client.get_tracks(limit=1000)
            
            if not tracks_data:
                logger.warning("No tracks received from LibreTime")
                return
            
            synced_count = 0
            updated_count = 0
            
            for track_data in tracks_data:
                libretime_id = track_data.get("id")
                if not libretime_id:
                    continue
                
                # Check if track already exists
                result = await db.execute(
                    select(Track).where(Track.libretime_id == libretime_id)
                )
                existing_track = result.scalar_one_or_none()
                
                if existing_track:
                    # Update existing track
                    existing_track.title = track_data.get("title", existing_track.title)
                    existing_track.artist = track_data.get("artist", existing_track.artist)
                    existing_track.album = track_data.get("album", existing_track.album)
                    existing_track.duration = track_data.get("duration", existing_track.duration)
                    existing_track.filepath = track_data.get("filepath", existing_track.filepath)
                    updated_count += 1
                else:
                    # Create new track
                    new_track = Track(
                        title=track_data.get("title", ""),
                        artist=track_data.get("artist"),
                        album=track_data.get("album"),
                        type="MUS",  # Default type, can be updated later
                        duration=track_data.get("duration"),
                        filepath=track_data.get("filepath", ""),
                        libretime_id=libretime_id
                    )
                    db.add(new_track)
                    synced_count += 1
            
            await db.commit()
            
            logger.info(
                "Nightly sync completed",
                synced_count=synced_count,
                updated_count=updated_count,
                total_tracks=len(tracks_data)
            )
            
        except Exception as e:
            logger.error("Nightly sync failed", error=str(e))
            await db.rollback()
            raise


@shared_task
async def sync_smart_blocks():
    """Sync smart blocks from LibreTime"""
    logger.info("Starting smart blocks sync")
    
    try:
        smart_blocks = await libretime_client.get_smart_blocks()
        
        if not smart_blocks:
            logger.warning("No smart blocks received from LibreTime")
            return
        
        logger.info("Smart blocks sync completed", count=len(smart_blocks))
        
    except Exception as e:
        logger.error("Smart blocks sync failed", error=str(e))
        raise


@shared_task
async def cleanup_old_tracks():
    """Clean up tracks that no longer exist in LibreTime"""
    logger.info("Starting track cleanup")
    
    async with AsyncSessionLocal() as db:
        try:
            # Get all tracks with LibreTime IDs
            result = await db.execute(
                select(Track).where(Track.libretime_id.isnot(None))
            )
            tracks = result.scalars().all()
            
            deleted_count = 0
            
            for track in tracks:
                # Check if track still exists in LibreTime
                libretime_track = await libretime_client.get_track(track.libretime_id)
                
                if not libretime_track:
                    # Track no longer exists in LibreTime, mark for deletion
                    await db.delete(track)
                    deleted_count += 1
            
            await db.commit()
            
            logger.info("Track cleanup completed", deleted_count=deleted_count)
            
        except Exception as e:
            logger.error("Track cleanup failed", error=str(e))
            await db.rollback()
            raise
