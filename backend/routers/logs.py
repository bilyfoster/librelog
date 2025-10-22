"""
Daily log management router
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from backend.database import get_db
from backend.models.daily_log import DailyLog
from backend.services.log_generator import LogGenerator
from backend.routers.auth import get_current_user
from backend.models.user import User
from pydantic import BaseModel
from typing import Optional, List
from datetime import date

router = APIRouter()


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
