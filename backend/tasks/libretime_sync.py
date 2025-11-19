"""
Periodic sync task for LibreTime media library
"""

from datetime import datetime, timezone
from celery import shared_task
from sqlalchemy.ext.asyncio import AsyncSession
from backend.database import AsyncSessionLocal
from backend.integrations.libretime_client import libretime_client
from backend.integrations.type_mapping import map_libretime_to_librelog, is_valid_libretime_code
from backend.models.track import Track
from backend.models.settings import Setting
from sqlalchemy import select, and_
import structlog

logger = structlog.get_logger()


@shared_task
def sync_media_library_from_libretime():
    """Periodic task to sync media library from LibreTime (runs hourly)"""
    logger.info("Starting periodic media library sync from LibreTime")
    
    # Use sync_async helper to run async code in sync context
    import asyncio
    try:
        loop = asyncio.get_event_loop()
    except RuntimeError:
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
    
    return loop.run_until_complete(_sync_media_library_async())


async def _sync_media_library_async():
    """Async implementation of media library sync"""
    async with AsyncSessionLocal() as db:
        try:
            synced_count = 0
            updated_count = 0
            offset = 0
            limit = 100
            total_processed = 0
            
            # Sync in batches
            while True:
                # Get tracks from LibreTime using integration endpoint
                response = await libretime_client.get_media_library(limit=limit, offset=offset)
                tracks_data = response.get("results", [])
                total_count = response.get("count", 0)
                
                if not tracks_data:
                    break
                
                # Process each track
                for track_data in tracks_data:
                    try:
                        # Get LibreTime ID
                        libretime_id = str(track_data.get("libretime_id") or track_data.get("id", ""))
                        if not libretime_id:
                            continue
                        
                        # Check if track exists
                        result = await db.execute(
                            select(Track).where(Track.libretime_id == libretime_id)
                        )
                        existing_track = result.scalar_one_or_none()
                        
                        # Extract track data
                        title = track_data.get("title", "").strip()
                        if not title:
                            continue
                        
                        # Map type using type mapping utility
                        libretime_code = track_data.get("type") or track_data.get("library", {}).get("code") or "MUS"
                        try:
                            if is_valid_libretime_code(libretime_code):
                                track_type = map_libretime_to_librelog(libretime_code)
                            else:
                                track_type = "MUS"
                        except ValueError:
                            track_type = "MUS"
                        
                        # Extract duration
                        duration = track_data.get("duration")
                        duration_seconds = None
                        if duration:
                            if isinstance(duration, (int, float)):
                                duration_seconds = int(duration)
                            elif isinstance(duration, str):
                                # Try to parse duration string
                                try:
                                    from dateutil import parser
                                    # Handle ISO 8601 duration (PT3M45S)
                                    if duration.startswith("PT"):
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
                                        # Try parsing as time string
                                        parts = duration.split(":")
                                        if len(parts) == 3:
                                            duration_seconds = int(float(parts[0])) * 3600 + int(float(parts[1])) * 60 + int(float(parts[2]))
                                        elif len(parts) == 2:
                                            duration_seconds = int(float(parts[0])) * 60 + int(float(parts[1]))
                                        else:
                                            duration_seconds = int(float(duration))
                                except:
                                    duration_seconds = None
                        
                        if existing_track:
                            # Update existing track
                            existing_track.title = title
                            existing_track.artist = track_data.get("artist")
                            existing_track.album = track_data.get("album")
                            existing_track.type = track_type
                            existing_track.genre = track_data.get("genre")
                            existing_track.duration = duration_seconds
                            existing_track.filepath = track_data.get("filepath", existing_track.filepath)
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
                                libretime_id=libretime_id
                            )
                            db.add(new_track)
                            synced_count += 1
                        
                        total_processed += 1
                        
                    except Exception as e:
                        logger.error("Error processing track in periodic sync", error=str(e), track_data=track_data)
                        continue
                
                # Commit batch
                await db.commit()
                
                # Check if we've processed all tracks
                if offset + limit >= total_count or len(tracks_data) < limit:
                    break
                
                offset += limit
            
            # Update last sync time
            result = await db.execute(
                select(Setting).where(
                    and_(Setting.category == "sync", Setting.key == "last_media_library_sync")
                )
            )
            setting = result.scalar_one_or_none()
            
            if setting:
                setting.value = datetime.now(timezone.utc).isoformat()
            else:
                setting = Setting(
                    category="sync",
                    key="last_media_library_sync",
                    value=datetime.now(timezone.utc).isoformat(),
                    encrypted=False
                )
                db.add(setting)
            
            await db.commit()
            
            logger.info(
                "Periodic media library sync completed",
                synced=synced_count,
                updated=updated_count,
                total_processed=total_processed
            )
            
            return {
                "success": True,
                "synced": synced_count,
                "updated": updated_count,
                "total_processed": total_processed
            }
            
        except Exception as e:
            logger.error("Periodic media library sync failed", error=str(e), exc_info=True)
            await db.rollback()
            raise

