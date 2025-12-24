"""
Sync endpoints for LibreTime integration
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from uuid import UUID
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_
from sqlalchemy.sql import func as sql_func
from backend.database import get_db
from backend.models.track import Track
from backend.models.playback_history import PlaybackHistory
from backend.models.daily_log import DailyLog
from backend.models.settings import Setting
from backend.integrations.libretime_client import libretime_client
from backend.integrations.type_mapping import map_libretime_to_librelog, is_valid_libretime_code
from backend.services.libretime_config_service import LibreTimeConfigService
from backend.routers.auth import get_current_user
from backend.models.user import User
from pydantic import BaseModel
from typing import Optional, List
from datetime import date, datetime, timedelta, timezone
import structlog

logger = structlog.get_logger()

router = APIRouter()


class SyncTracksResponse(BaseModel):
    """Response for track sync"""
    success: bool
    synced: int
    updated: int
    errors: List[str]
    total_processed: int


class SyncPlaybackHistoryRequest(BaseModel):
    """Request for playback history sync"""
    start_date: date
    end_date: date


class SyncPlaybackHistoryResponse(BaseModel):
    """Response for playback history sync"""
    success: bool
    synced: int
    errors: List[str]


class SyncStatusResponse(BaseModel):
    """Sync status response"""
    last_track_sync: Optional[datetime] = None
    last_playback_sync: Optional[datetime] = None
    total_tracks: int
    connection_status: bool


@router.post("/tracks", response_model=SyncTracksResponse)
async def sync_tracks(
    limit: int = Query(1000, ge=1, le=10000),
    offset: int = Query(0, ge=0),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Manually trigger sync of tracks from LibreTime"""
    logger.info("Starting manual track sync", user_id=current_user.id, limit=limit, offset=offset)
    
    synced_count = 0
    updated_count = 0
    errors = []
    
    try:
        # Get tracks from LibreTime using integration endpoint
        response = await libretime_client.get_media_library(limit=limit, offset=offset)
        tracks_data = response.get("results", [])
        
        if not tracks_data:
            logger.warning("No tracks received from LibreTime")
            return SyncTracksResponse(
                success=True,
                synced=0,
                updated=0,
                errors=[],
                total_processed=0
            )
        
        # Process tracks in batches to avoid transaction issues
        batch_size = 50
        for i in range(0, len(tracks_data), batch_size):
            batch = tracks_data[i:i + batch_size]
            batch_errors = []
            
            for track_data in batch:
                try:
                    media_id = str(track_data.get("media_id") or track_data.get("id", ""))
                    if not media_id:
                        errors.append(f"Track missing media_id: {track_data.get('title', 'Unknown')}")
                        continue
                    
                    # Convert duration from timedelta/string to seconds
                    duration = track_data.get("duration")
                    if duration:
                        if isinstance(duration, str):
                            # Parse ISO 8601 duration (e.g., "PT3M45S")
                            try:
                                from dateutil import parser
                                # Try to parse as timedelta
                                if duration.startswith("PT"):
                                    # ISO 8601 duration format
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
                                        duration_seconds = int(float(parts[0])) * 3600 + int(float(parts[1])) * 60 + int(float(parts[2]))
                                    elif len(parts) == 2:
                                        duration_seconds = int(float(parts[0])) * 60 + int(float(parts[1]))
                                    else:
                                        duration_seconds = int(float(duration))
                            except:
                                duration_seconds = None
                        elif isinstance(duration, timedelta):
                            duration_seconds = int(duration.total_seconds())
                        else:
                            duration_seconds = int(duration) if duration else None
                    else:
                        duration_seconds = None
                    
                    # Get or create track
                    result = await db.execute(
                        select(Track).where(Track.libretime_id == media_id)
                    )
                    existing_track = result.scalar_one_or_none()
                    
                    # Map LibreTime fields to Track model
                    title = track_data.get("title", "").strip()
                    if not title:
                        errors.append(f"Track missing title: media_id={media_id}")
                        continue
                    
                    # Get track type from LibreTime library code
                    libretime_code = track_data.get("type") or track_data.get("library", {}).get("code") or "MUS"
                    
                    # Use type mapping utility
                    try:
                        if is_valid_libretime_code(libretime_code):
                            track_type = map_libretime_to_librelog(libretime_code)
                        else:
                            # Fallback to MUS if code is not recognized
                            track_type = "MUS"
                            logger.warning("Unknown LibreTime code, defaulting to MUS", code=libretime_code)
                    except ValueError:
                        track_type = "MUS"
                        logger.warning("Type mapping error, defaulting to MUS", code=libretime_code)
                    
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
                        
                except Exception as e:
                    error_msg = f"Error processing track {track_data.get('title', 'Unknown')}: {str(e)}"
                    logger.error("Track sync error", error=error_msg, track_data=track_data, exc_info=True)
                    batch_errors.append(error_msg)
                    # Rollback this track's changes but continue with batch
                    try:
                        await db.rollback()
                    except:
                        pass
                    continue
            
            # Commit batch
            try:
                await db.commit()
                errors.extend(batch_errors)
            except Exception as e:
                logger.error("Failed to commit batch", error=str(e), batch_start=i, exc_info=True)
                await db.rollback()
                errors.extend(batch_errors)
                errors.append(f"Batch commit failed: {str(e)}")
        
        # Store last track sync time
        result = await db.execute(
            select(Setting).where(
                and_(Setting.category == "sync", Setting.key == "last_track_sync")
            )
        )
        setting = result.scalar_one_or_none()
        
        if setting:
            setting.value = datetime.now(timezone.utc).isoformat()
        else:
            setting = Setting(
                category="sync",
                key="last_track_sync",
                value=datetime.now(timezone.utc).isoformat(),
                encrypted=False
            )
            db.add(setting)
        
        await db.commit()
        
        logger.info(
            "Track sync completed",
            synced=synced_count,
            updated=updated_count,
            errors=len(errors),
            total=len(tracks_data)
        )
        
        return SyncTracksResponse(
            success=True,
            synced=synced_count,
            updated=updated_count,
            errors=errors,
            total_processed=len(tracks_data)
        )
        
    except Exception as e:
        logger.error("Track sync failed", error=str(e))
        await db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Sync failed: {str(e)}"
        )


@router.post("/playback-history", response_model=SyncPlaybackHistoryResponse)
async def sync_playback_history(
    request: SyncPlaybackHistoryRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Sync playback history from LibreTime"""
    logger.info(
        "Starting playback history sync",
        user_id=current_user.id,
        start_date=request.start_date,
        end_date=request.end_date
    )
    
    try:
        # Get playback history from LibreTime
        history_data = await libretime_client.get_playback_history(
            request.start_date,
            request.end_date
        )
        
        if not history_data:
            logger.warning("No playback history received from LibreTime")
            return SyncPlaybackHistoryResponse(
                success=True,
                synced=0,
                errors=[]
            )
        
        # Store playback history in database
        synced_count = 0
        errors = []
        
        for play_data in history_data:
            try:
                # Extract data from LibreTime response
                track_id = play_data.get("track_id")
                log_id = play_data.get("log_id")
                played_at_str = play_data.get("played_at")
                duration_played = play_data.get("duration_played", 0)
                
                if not all([track_id, log_id, played_at_str]):
                    errors.append(f"Missing required fields in playback data: {play_data}")
                    continue
                
                # Parse played_at datetime
                try:
                    if isinstance(played_at_str, str):
                        played_at = datetime.fromisoformat(played_at_str.replace('Z', '+00:00'))
                    else:
                        played_at = played_at_str
                except Exception as e:
                    errors.append(f"Invalid played_at format: {played_at_str}, error: {str(e)}")
                    continue
                
                # Check if playback record already exists
                existing = await db.execute(
                    select(PlaybackHistory).where(
                        and_(
                            PlaybackHistory.track_id == track_id,
                            PlaybackHistory.log_id == log_id,
                            PlaybackHistory.played_at == played_at
                        )
                    )
                )
                if existing.scalar_one_or_none():
                    continue  # Skip duplicates
                
                # Create new playback history record
                playback = PlaybackHistory(
                    track_id=track_id,
                    log_id=log_id,
                    played_at=played_at,
                    duration_played=duration_played
                )
                db.add(playback)
                synced_count += 1
                
            except Exception as e:
                errors.append(f"Error processing playback record: {str(e)}")
                logger.error("Playback history record processing failed", error=str(e), data=play_data)
        
        # Commit all records
        await db.commit()
        
        # Store last sync time
        result = await db.execute(
            select(Setting).where(
                and_(Setting.category == "sync", Setting.key == "last_playback_sync")
            )
        )
        setting = result.scalar_one_or_none()
        
        if setting:
            setting.value = datetime.now(timezone.utc).isoformat()
        else:
            setting = Setting(
                category="sync",
                key="last_playback_sync",
                value=datetime.now(timezone.utc).isoformat(),
                encrypted=False
            )
            db.add(setting)
        
        await db.commit()
        
        logger.info("Playback history sync completed", synced=synced_count, errors=len(errors))
        
        return SyncPlaybackHistoryResponse(
            success=True,
            synced=synced_count,
            errors=errors
        )
        
    except Exception as e:
        logger.error("Playback history sync failed", error=str(e))
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Sync failed: {str(e)}"
        )


@router.get("/status", response_model=SyncStatusResponse)
async def get_sync_status(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get sync status and connection health"""
    # Check connection
    connection_status = await libretime_client.health_check()
    
    # Get total tracks count
    result = await db.execute(select(sql_func.count(Track.id)))
    total_tracks = result.scalar() or 0
    
    # Retrieve last sync times from settings
    last_track_sync = None
    last_playback_sync = None
    
    track_sync_result = await db.execute(
        select(Setting).where(
            and_(Setting.category == "sync", Setting.key == "last_track_sync")
        )
    )
    track_sync_setting = track_sync_result.scalar_one_or_none()
    if track_sync_setting and track_sync_setting.value:
        try:
            last_track_sync = datetime.fromisoformat(track_sync_setting.value)
        except:
            pass
    
    playback_sync_result = await db.execute(
        select(Setting).where(
            and_(Setting.category == "sync", Setting.key == "last_playback_sync")
        )
    )
    playback_sync_setting = playback_sync_result.scalar_one_or_none()
    if playback_sync_setting and playback_sync_setting.value:
        try:
            last_playback_sync = datetime.fromisoformat(playback_sync_setting.value)
        except:
            pass
    
    return SyncStatusResponse(
        last_track_sync=last_track_sync,
        last_playback_sync=last_playback_sync,
        total_tracks=total_tracks,
        connection_status=connection_status
    )


@router.post("/libretime/save-config")
async def save_libretime_config(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Save LibreTime configuration to LibreLog settings"""
    try:
        result = await LibreTimeConfigService.save_libretime_config(db)
        return result
    except Exception as e:
        logger.error("Failed to save LibreTime config", error=str(e), exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to save config: {str(e)}"
        )


@router.get("/libretime/config")
async def get_libretime_config(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get LibreTime configuration from settings"""
    try:
        config = await LibreTimeConfigService.get_libretime_config(db)
        return config
    except Exception as e:
        logger.error("Failed to get LibreTime config", error=str(e), exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to get config: {str(e)}"
        )

