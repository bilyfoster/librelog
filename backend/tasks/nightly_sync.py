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
                # Map media_id from LibreTime response
                media_id = str(track_data.get("media_id") or track_data.get("id", ""))
                if not media_id:
                    continue
                
                # Convert duration from timedelta/string to seconds
                duration = track_data.get("duration")
                duration_seconds = None
                if duration:
                    if isinstance(duration, str):
                        # Parse ISO 8601 duration or time string
                        try:
                            if duration.startswith("PT"):
                                # ISO 8601 duration format (PT3M45S)
                                hours = 0
                                minutes = 0
                                seconds = 0
                                if "H" in duration:
                                    hours = int(duration.split("H")[0].replace("PT", ""))
                                if "M" in duration:
                                    minutes = int(duration.split("M")[0].split("H")[-1].replace("PT", ""))
                                if "S" in duration:
                                    seconds = int(duration.split("S")[0].split("M")[-1].split("H")[-1].replace("PT", ""))
                                duration_seconds = hours * 3600 + minutes * 60 + seconds
                            else:
                                # Try parsing as time string (HH:MM:SS or MM:SS)
                                parts = duration.split(":")
                                if len(parts) == 3:
                                    duration_seconds = int(parts[0]) * 3600 + int(parts[1]) * 60 + int(parts[2])
                                elif len(parts) == 2:
                                    duration_seconds = int(parts[0]) * 60 + int(parts[1])
                                else:
                                    duration_seconds = int(duration)
                        except:
                            duration_seconds = None
                    elif isinstance(duration, timedelta):
                        duration_seconds = int(duration.total_seconds())
                    else:
                        duration_seconds = int(duration) if duration else None
                
                # Map type from LibreTime library code
                track_type = track_data.get("type", "MUS")
                type_mapping = {
                    "music": "MUS",
                    "promo": "PRO",
                    "ad": "ADV",
                    "psa": "PSA",
                    "link": "LIN",
                    "intro": "INT",
                    "bed": "BED"
                }
                track_type = type_mapping.get(track_type.lower(), track_type.upper()[:3] if track_type else "MUS")
                
                # Check if track already exists
                result = await db.execute(
                    select(Track).where(Track.libretime_id == media_id)
                )
                existing_track = result.scalar_one_or_none()
                
                title = track_data.get("title", "").strip()
                if not title:
                    continue
                
                if existing_track:
                    # Update existing track
                    existing_track.title = title
                    existing_track.artist = track_data.get("artist")
                    existing_track.album = track_data.get("album")
                    existing_track.type = track_type
                    existing_track.genre = track_data.get("genre")
                    existing_track.duration = duration_seconds
                    existing_track.filepath = track_data.get("filepath", existing_track.filepath)
                    if track_data.get("last_modified"):
                        try:
                            if isinstance(track_data["last_modified"], str):
                                existing_track.updated_at = datetime.fromisoformat(
                                    track_data["last_modified"].replace("Z", "+00:00")
                                )
                            else:
                                existing_track.updated_at = track_data["last_modified"]
                        except:
                            pass
                    updated_count += 1
                else:
                    # Create new track
                    new_track = Track(
                        title=title,
                        artist=track_data.get("artist"),
                        album=track_data.get("album"),
                        type=track_type,
                        genre=track_data.get("genre"),
                        duration=duration_seconds,
                        filepath=track_data.get("filepath", ""),
                        libretime_id=media_id
                    )
                    if track_data.get("last_modified"):
                        try:
                            if isinstance(track_data["last_modified"], str):
                                new_track.updated_at = datetime.fromisoformat(
                                    track_data["last_modified"].replace("Z", "+00:00")
                                )
                        except:
                            pass
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


@shared_task
async def check_copy_expiration():
    """Check for expiring copy and send alerts"""
    from backend.services.copy_service import CopyService
    from backend.services.notification_service import NotificationService
    
    logger.info("Starting copy expiration check")
    
    async with AsyncSessionLocal() as db:
        try:
            copy_service = CopyService(db)
            notification_service = NotificationService(db)
            
            # Check copy expiring in next 7 days
            expiring_copy = await copy_service.check_expiring(days_ahead=7)
            
            if expiring_copy:
                logger.info("Found expiring copy", count=len(expiring_copy))
                
                # Send notifications for expiring copy
                for copy_item in expiring_copy:
                    days_until_expiry = (copy_item.expires_at.date() - datetime.now().date()).days
                    
                    await notification_service.create_notification(
                        user_id=1,  # Admin user - TODO: get from settings
                        message=f"Copy '{copy_item.title}' expires in {days_until_expiry} day(s)",
                        notification_type="IN_APP",
                        subject="Copy Expiring Soon",
                        metadata={
                            "copy_id": copy_item.id,
                            "expires_at": copy_item.expires_at.isoformat(),
                            "days_until_expiry": days_until_expiry
                        }
                    )
            
            logger.info("Copy expiration check completed", expiring_count=len(expiring_copy))
            
        except Exception as e:
            logger.error("Copy expiration check failed", error=str(e))
            raise
