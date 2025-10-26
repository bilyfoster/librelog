from fastapi import APIRouter, Depends, HTTPException, status, BackgroundTasks
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.models.play_log import PlayLog, PlayStatistics, LibreTimeIntegration
from app.models.submission import Submission
from app.services.libretime_service import LibreTimeService
from pydantic import BaseModel
from typing import Optional, List
from datetime import datetime, timedelta
import uuid

router = APIRouter()


class PlayTrackingRequest(BaseModel):
    submission_id: str
    play_type: str = "gallery"
    played_at: Optional[datetime] = None
    user_agent: Optional[str] = None
    ip_address: Optional[str] = None
    additional_data: Optional[dict] = None


@router.post("/track-play")
async def track_play(
    play_data: PlayTrackingRequest,
    db: Session = Depends(get_db)
):
    """Track a play event"""
    
    # Create play log entry
    play_log = PlayLog(
        submission_id=play_data.submission_id,
        play_type=play_data.play_type,
        played_at=play_data.played_at or datetime.utcnow(),
        user_agent=play_data.user_agent,
        ip_address=play_data.ip_address,
        additional_data=play_data.additional_data or {}
    )
    
    db.add(play_log)
    db.commit()
    
    return {"message": "Play tracked successfully"}


@router.post("/track-gallery-play")
async def track_gallery_play(
    submission_id: str,
    user_agent: Optional[str] = None,
    ip_address: Optional[str] = None,
    db: Session = Depends(get_db)
):
    """Track a gallery play event (simplified endpoint for gallery)"""
    
    # Create play log entry for gallery play
    play_log = PlayLog(
        submission_id=submission_id,
        play_type="gallery",
        played_at=datetime.utcnow(),
        source="gallery",
        extra_data={
            "source": "gallery",
            "user_agent": user_agent,
            "ip_address": ip_address
        }
    )
    
    db.add(play_log)
    db.commit()
    
    return {"message": "Gallery play tracked successfully"}


class ManualPlayLog(BaseModel):
    submission_id: str
    played_at: datetime
    dj_name: Optional[str] = None
    show_name: Optional[str] = None
    duration_played: Optional[int] = None


class LibreTimeConfig(BaseModel):
    libretime_url: str
    api_key: str
    sync_interval_minutes: int = 15
    auto_sync_enabled: bool = True


class LibreTimeValidation(BaseModel):
    libretime_url: str
    api_key: str


@router.post("/sync-libretime")
async def sync_libretime_plays(
    hours_back: int = 24,
    background_tasks: BackgroundTasks = BackgroundTasks(),
    db: Session = Depends(get_db)
):
    """Sync play data from LibreTime (admin only)"""
    
    # Run sync in background
    background_tasks.add_task(run_libretime_sync, db, hours_back)
    
    return {
        "message": "LibreTime sync started in background",
        "hours_back": hours_back
    }


@router.post("/test-libretime-connection")
async def test_libretime_connection(
    db: Session = Depends(get_db)
):
    """Test LibreTime API connection and return detailed debugging info"""
    
    try:
        service = LibreTimeService(db)
        
        # Test API key validation
        validation_result = await service.validate_api_key(
            service.base_url, 
            service.api_key
        )
        
        # Test play history fetch with detailed logging
        from datetime import datetime, timedelta
        end_time = datetime.utcnow()
        start_time = end_time - timedelta(hours=1)
        
        plays_data = await service._fetch_plays_from_libretime(start_time, end_time)
        
        return {
            "api_key_validation": validation_result,
            "base_url": service.base_url,
            "api_key": service.api_key[:10] + "..." if service.api_key else None,
            "test_time_range": {
                "start": start_time.isoformat(),
                "end": end_time.isoformat()
            },
            "plays_fetched": len(plays_data),
            "sample_play_data": plays_data[:3] if plays_data else [],
            "status": "success"
        }
        
    except Exception as e:
        return {
            "error": str(e),
            "status": "error"
        }


async def run_libretime_sync(db: Session, hours_back: int):
    """Background task to sync LibreTime plays"""
    try:
        service = LibreTimeService(db)
        result = await service.sync_plays(hours_back)
        print(f"LibreTime sync completed: {result}")
    except Exception as e:
        print(f"LibreTime sync failed: {e}")


@router.post("/manual-log")
async def manual_log_play(
    play_data: ManualPlayLog,
    admin_id: str = "f49dc2cf-69f7-4119-9a00-3d7046293321",  # TODO: Get from JWT token
    db: Session = Depends(get_db)
):
    """Manually log a play (admin only)"""
    
    try:
        service = LibreTimeService(db)
        play_log = await service.manual_log_play(
            submission_id=play_data.submission_id,
            played_at=play_data.played_at,
            dj_name=play_data.dj_name,
            show_name=play_data.show_name,
            duration_played=play_data.duration_played
        )
        
        return {
            "message": "Play logged successfully",
            "play_id": str(play_log.id),
            "played_at": play_log.played_at.isoformat()
        }
        
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error logging play: {str(e)}")


@router.get("/statistics/{submission_id}")
async def get_submission_play_statistics(
    submission_id: str,
    db: Session = Depends(get_db)
):
    """Get play statistics for a specific submission"""
    
    service = LibreTimeService(db)
    stats = service.get_play_statistics(submission_id)
    
    if "error" in stats:
        raise HTTPException(status_code=404, detail=stats["error"])
    
    return stats


@router.get("/statistics")
async def get_overall_play_statistics(
    db: Session = Depends(get_db)
):
    """Get overall play statistics"""
    
    service = LibreTimeService(db)
    return service.get_play_statistics()


@router.get("/submissions/{submission_id}/plays")
async def get_submission_plays(
    submission_id: str,
    limit: int = 50,
    offset: int = 0,
    play_type: Optional[str] = None,
    db: Session = Depends(get_db)
):
    """Get play history for a submission"""
    
    query = db.query(PlayLog).filter(PlayLog.submission_id == submission_id)
    
    if play_type:
        query = query.filter(PlayLog.play_type == play_type)
    
    plays = query.order_by(PlayLog.played_at.desc()).offset(offset).limit(limit).all()
    
    return [
        {
            "id": str(play.id),
            "played_at": play.played_at.isoformat(),
            "duration_played": play.duration_played,
            "play_type": play.play_type,
            "source": play.source,
            "dj_name": play.dj_name,
            "show_name": play.show_name,
            "time_slot": play.time_slot,
            "libretime_play_id": play.libretime_play_id
        }
        for play in plays
    ]


@router.get("/recent-plays")
async def get_recent_plays(
    hours: int = 24,
    limit: int = 100,
    db: Session = Depends(get_db)
):
    """Get recent plays across all submissions"""
    
    since = datetime.utcnow() - timedelta(hours=hours)
    
    plays = db.query(PlayLog).filter(
        PlayLog.played_at >= since
    ).order_by(PlayLog.played_at.desc()).limit(limit).all()
    
    return [
        {
            "id": str(play.id),
            "submission_id": str(play.submission_id),
            "song_title": play.submission.song_title,
            "artist_name": play.submission.artist.name,
            "played_at": play.played_at.isoformat(),
            "play_type": play.play_type,
            "dj_name": play.dj_name,
            "show_name": play.show_name,
            "time_slot": play.time_slot
        }
        for play in plays
    ]


@router.get("/top-tracks")
async def get_top_tracks(
    period: str = "month",  # week, month, year, all
    limit: int = 20,
    db: Session = Depends(get_db)
):
    """Get top played tracks for a period"""
    
    # Calculate date range
    now = datetime.utcnow()
    if period == "week":
        since = now - timedelta(days=7)
    elif period == "month":
        since = now - timedelta(days=30)
    elif period == "year":
        since = now - timedelta(days=365)
    else:
        since = datetime.min
    
    # Get play counts by submission
    from sqlalchemy import func
    
    play_counts = db.query(
        PlayLog.submission_id,
        func.count(PlayLog.id).label('play_count')
    ).filter(
        PlayLog.played_at >= since
    ).group_by(PlayLog.submission_id).order_by(
        func.count(PlayLog.id).desc()
    ).limit(limit).all()
    
    # Get submission details
    submission_ids = [pc.submission_id for pc in play_counts]
    submissions = db.query(Submission).filter(
        Submission.id.in_(submission_ids)
    ).all()
    
    # Create lookup
    submission_lookup = {str(s.id): s for s in submissions}
    
    return [
        {
            "submission_id": str(pc.submission_id),
            "song_title": submission_lookup[pc.submission_id].song_title,
            "artist_name": submission_lookup[pc.submission_id].artist.name,
            "play_count": pc.play_count,
            "tracking_id": submission_lookup[pc.submission_id].tracking_id
        }
        for pc in play_counts
        if pc.submission_id in submission_lookup
    ]


@router.post("/libretime-validate")
async def validate_libretime_config(
    validation: LibreTimeValidation,
    db: Session = Depends(get_db)
):
    """Validate LibreTime API key and connection (admin only)"""
    
    try:
        service = LibreTimeService(db)
        result = await service.validate_api_key(validation.libretime_url, validation.api_key)
        
        return result
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Validation error: {str(e)}")


@router.post("/libretime-config")
async def update_libretime_config(
    config: LibreTimeConfig,
    admin_id: str = "f49dc2cf-69f7-4119-9a00-3d7046293321",  # TODO: Get from JWT token
    db: Session = Depends(get_db)
):
    """Update LibreTime integration configuration (admin only)"""
    
    # Validate the API key before saving
    try:
        service = LibreTimeService(db)
        validation_result = await service.validate_api_key(config.libretime_url, config.api_key)
        
        if not validation_result["valid"]:
            raise HTTPException(
                status_code=400, 
                detail=f"API key validation failed: {validation_result['message']}"
            )
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Validation error: {str(e)}")
    
    # Get or create config
    libretime_config = db.query(LibreTimeIntegration).first()
    
    if not libretime_config:
        libretime_config = LibreTimeIntegration()
        db.add(libretime_config)
    
    # Update configuration
    libretime_config.libretime_url = config.libretime_url
    libretime_config.api_key = config.api_key
    libretime_config.sync_interval_minutes = config.sync_interval_minutes
    libretime_config.auto_sync_enabled = config.auto_sync_enabled
    libretime_config.sync_status = "active"
    libretime_config.error_count = 0
    libretime_config.last_error = None
    
    db.commit()
    
    return {
        "message": "LibreTime configuration updated successfully",
        "config": {
            "libretime_url": libretime_config.libretime_url,
            "sync_interval_minutes": libretime_config.sync_interval_minutes,
            "auto_sync_enabled": libretime_config.auto_sync_enabled,
            "sync_status": libretime_config.sync_status
        }
    }


@router.get("/libretime-config")
async def get_libretime_config(
    db: Session = Depends(get_db)
):
    """Get current LibreTime configuration"""
    
    config = db.query(LibreTimeIntegration).first()
    
    if not config:
        return {
            "configured": False,
            "message": "LibreTime not configured"
        }
    
    return {
        "configured": True,
        "libretime_url": config.libretime_url,
        "api_key": config.api_key,
        "sync_interval_minutes": config.sync_interval_minutes,
        "auto_sync_enabled": config.auto_sync_enabled,
        "sync_status": config.sync_status,
        "last_sync_at": config.last_sync_at.isoformat() if config.last_sync_at else None,
        "error_count": config.error_count,
        "last_error": config.last_error
    }

