"""
Daily log management router
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func
from backend.database import get_db
from backend.models.daily_log import DailyLog
from backend.models.spot import Spot
from backend.services.log_generator import LogGenerator
from backend.services.log_editor import LogEditor
from backend.routers.auth import get_current_user
from backend.models.user import User
from pydantic import BaseModel
from typing import Optional, List, Dict, Any
from datetime import date, datetime, timezone

router = APIRouter()


@router.get("/count")
async def get_logs_count(
    published_only: bool = Query(False),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get total count of daily logs"""
    query = select(func.count(DailyLog.id))
    
    if published_only:
        query = query.where(DailyLog.published == True)
    
    result = await db.execute(query)
    count = result.scalar() or 0
    
    return {"count": count}


class LogGenerateRequest(BaseModel):
    target_date: date
    clock_template_id: int


class LogPreviewRequest(BaseModel):
    target_date: date
    clock_template_id: int
    preview_hours: Optional[List[str]] = None


@router.get("/")
async def list_logs(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    published_only: bool = Query(False),
    date_filter: Optional[date] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List all daily logs"""
    query = select(DailyLog)
    
    if published_only:
        query = query.where(DailyLog.published == True)
    
    if date_filter:
        query = query.where(DailyLog.date == date_filter)
    
    query = query.offset(skip).limit(limit).order_by(DailyLog.date.desc())
    
    result = await db.execute(query)
    logs = result.scalars().all()
    
    return {
        "logs": [
            {
                "id": log.id,
                "date": log.date,
                "generated_by": log.generated_by,
                "published": log.published,
                "created_at": log.created_at,
                "updated_at": log.updated_at,
                "total_duration": log.json_data.get("total_duration", 0)
            }
            for log in logs
        ],
        "total": len(logs)
    }


@router.post("/generate")
async def generate_log(
    request: LogGenerateRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Generate a new daily log"""
    generator = LogGenerator(db)
    
    try:
        result = await generator.generate_daily_log(
            target_date=request.target_date,
            clock_template_id=request.clock_template_id,
            user_id=current_user.id
        )
        
        return result
        
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail=str(e))


@router.post("/preview")
async def preview_log(
    request: LogPreviewRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Preview a daily log without saving"""
    generator = LogGenerator(db)
    
    try:
        preview = await generator.preview_log(
            target_date=request.target_date,
            clock_template_id=request.clock_template_id,
            preview_hours=request.preview_hours
        )
        
        return preview
        
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail=str(e))


@router.get("/{log_id}")
async def get_log(
    log_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get a specific daily log"""
    result = await db.execute(
        select(DailyLog).where(DailyLog.id == log_id)
    )
    log = result.scalar_one_or_none()
    
    if not log:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Daily log not found")
    
    return {
        "id": log.id,
        "date": log.date,
        "generated_by": log.generated_by,
        "published": log.published,
        "json_data": log.json_data,
        "created_at": log.created_at,
        "updated_at": log.updated_at
    }


@router.post("/{log_id}/publish")
async def publish_log(
    log_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Publish a daily log to LibreTime"""
    generator = LogGenerator(db)
    
    success = await generator.publish_log(log_id)
    
    if not success:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to publish log to LibreTime"
        )
    
    return {"message": "Log published to LibreTime successfully"}


@router.delete("/{log_id}")
async def delete_log(
    log_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Delete a daily log"""
    result = await db.execute(
        select(DailyLog).where(DailyLog.id == log_id)
    )
    log = result.scalar_one_or_none()
    
    if not log:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Daily log not found")
    
    await db.delete(log)
    await db.commit()
    
    return {"message": "Daily log deleted successfully"}


@router.get("/{log_id}/timeline")
async def get_log_timeline(
    log_id: int,
    hour: Optional[str] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get timeline view of a daily log"""
    result = await db.execute(
        select(DailyLog).where(DailyLog.id == log_id)
    )
    log = result.scalar_one_or_none()
    
    if not log:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Daily log not found")
    
    json_data = log.json_data
    
    if hour:
        # Return specific hour timeline
        hourly_logs = json_data.get("hourly_logs", {})
        if hour not in hourly_logs:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Hour not found in log")
        
        return {
            "log_id": log_id,
            "date": log.date.isoformat(),
            "hour": hour,
            "timeline": hourly_logs[hour].get("timeline", [])
        }
    else:
        # Return all hours timeline
        return {
            "log_id": log_id,
            "date": log.date.isoformat(),
            "hourly_timelines": {
                hour: data.get("timeline", [])
                for hour, data in json_data.get("hourly_logs", {}).items()
            }
        }


@router.post("/{log_id}/lock")
async def lock_log(
    log_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Lock a log to prevent further edits"""
    result = await db.execute(select(DailyLog).where(DailyLog.id == log_id))
    log = result.scalar_one_or_none()
    
    if not log:
        raise HTTPException(status_code=404, detail="Daily log not found")
    
    if log.locked:
        raise HTTPException(status_code=400, detail="Log is already locked")
    
    log.locked = True
    log.locked_at = datetime.now(timezone.utc)
    log.locked_by = current_user.id
    
    await db.commit()
    await db.refresh(log)
    
    return {"message": "Log locked successfully", "locked": True, "locked_at": log.locked_at.isoformat()}


@router.post("/{log_id}/unlock")
async def unlock_log(
    log_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Unlock a log to allow edits"""
    result = await db.execute(select(DailyLog).where(DailyLog.id == log_id))
    log = result.scalar_one_or_none()
    
    if not log:
        raise HTTPException(status_code=404, detail="Daily log not found")
    
    if not log.locked:
        raise HTTPException(status_code=400, detail="Log is not locked")
    
    log.locked = False
    log.locked_at = None
    log.locked_by = None
    
    await db.commit()
    
    return {"message": "Log unlocked successfully", "locked": False}


@router.get("/{log_id}/conflicts")
async def get_log_conflicts(
    log_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get conflicts for a log"""
    result = await db.execute(select(DailyLog).where(DailyLog.id == log_id))
    log = result.scalar_one_or_none()
    
    if not log:
        raise HTTPException(status_code=404, detail="Daily log not found")
    
    return {
        "log_id": log_id,
        "conflicts": log.conflicts or [],
        "conflict_count": len(log.conflicts) if log.conflicts else 0
    }


@router.get("/{log_id}/avails")
async def get_log_avails(
    log_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get available inventory for a log"""
    from backend.services.spot_scheduler import SpotScheduler
    
    result = await db.execute(select(DailyLog).where(DailyLog.id == log_id))
    log = result.scalar_one_or_none()
    
    if not log:
        raise HTTPException(status_code=404, detail="Daily log not found")
    
    scheduler = SpotScheduler(db)
    avails = await scheduler.calculate_avails(log.date)
    
    return {
        "log_id": log_id,
        "date": log.date.isoformat(),
        "avails": avails
    }


@router.post("/{log_id}/spots")
async def add_spot_to_log(
    log_id: int,
    spot_data: Dict[str, Any],
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Add a spot to a log"""
    result = await db.execute(select(DailyLog).where(DailyLog.id == log_id))
    log = result.scalar_one_or_none()
    
    if not log:
        raise HTTPException(status_code=404, detail="Daily log not found")
    
    if log.locked:
        raise HTTPException(status_code=400, detail="Log is locked and cannot be edited")
    
    editor = LogEditor(db)
    success = await editor.add_spot_to_log(log_id, spot_data)
    
    if not success:
        raise HTTPException(status_code=400, detail="Failed to add spot to log")
    
    return {"message": "Spot added to log successfully"}


@router.put("/{log_id}/spots/{spot_id}")
async def update_spot_in_log(
    log_id: int,
    spot_id: int,
    spot_update: Dict[str, Any],
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Update a spot in a log (for drag-drop)"""
    result = await db.execute(select(DailyLog).where(DailyLog.id == log_id))
    log = result.scalar_one_or_none()
    
    if not log:
        raise HTTPException(status_code=404, detail="Daily log not found")
    
    if log.locked:
        raise HTTPException(status_code=400, detail="Log is locked and cannot be edited")
    
    editor = LogEditor(db)
    success = await editor.move_spot(log_id, spot_id, spot_update)
    
    if not success:
        raise HTTPException(status_code=400, detail="Failed to update spot in log")
    
    return {"message": "Spot updated in log successfully"}


@router.delete("/{log_id}/spots/{spot_id}")
async def remove_spot_from_log(
    log_id: int,
    spot_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Remove a spot from a log"""
    result = await db.execute(select(DailyLog).where(DailyLog.id == log_id))
    log = result.scalar_one_or_none()
    
    if not log:
        raise HTTPException(status_code=404, detail="Daily log not found")
    
    if log.locked:
        raise HTTPException(status_code=400, detail="Log is locked and cannot be edited")
    
    editor = LogEditor(db)
    success = await editor.remove_spot(log_id, spot_id)
    
    if not success:
        raise HTTPException(status_code=400, detail="Failed to remove spot from log")
    
    return {"message": "Spot removed from log successfully"}
