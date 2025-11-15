"""
Traffic Logs router for tracking traffic operations
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query, Request
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, desc, func
from sqlalchemy.orm import selectinload
from typing import Optional, List, Dict, Any
from pydantic import BaseModel
from datetime import datetime

from backend.database import get_db
from backend.models.traffic_log import TrafficLog, TrafficLogType
from backend.routers.auth import get_current_user
from backend.models.user import User

router = APIRouter(prefix="/traffic-logs", tags=["traffic-logs"])


class TrafficLogCreate(BaseModel):
    log_type: str
    log_id: Optional[int] = None
    spot_id: Optional[int] = None
    order_id: Optional[int] = None
    campaign_id: Optional[int] = None
    copy_id: Optional[int] = None
    message: str
    metadata: Optional[Dict[str, Any]] = None


class TrafficLogBulkCreate(BaseModel):
    logs: List[TrafficLogCreate]


class TrafficLogResponse(BaseModel):
    id: int
    log_type: str
    log_id: Optional[int]
    spot_id: Optional[int]
    order_id: Optional[int]
    campaign_id: Optional[int]
    copy_id: Optional[int]
    user_id: int
    message: str
    metadata: Optional[Dict[str, Any]]
    ip_address: Optional[str]
    user_agent: Optional[str]
    created_at: str
    username: Optional[str] = None

    class Config:
        from_attributes = True


@router.get("/", response_model=List[TrafficLogResponse])
async def list_traffic_logs(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    log_type: Optional[str] = None,
    log_id: Optional[int] = None,
    spot_id: Optional[int] = None,
    order_id: Optional[int] = None,
    campaign_id: Optional[int] = None,
    user_id: Optional[int] = None,
    start_date: Optional[datetime] = None,
    end_date: Optional[datetime] = None,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List traffic logs with filtering"""
    query = select(TrafficLog).options(selectinload(TrafficLog.user))
    
    if log_type:
        query = query.where(TrafficLog.log_type == log_type)
    
    if log_id:
        query = query.where(TrafficLog.log_id == log_id)
    
    if spot_id:
        query = query.where(TrafficLog.spot_id == spot_id)
    
    if order_id:
        query = query.where(TrafficLog.order_id == order_id)
    
    if campaign_id:
        query = query.where(TrafficLog.campaign_id == campaign_id)
    
    if user_id:
        query = query.where(TrafficLog.user_id == user_id)
    
    if start_date:
        query = query.where(TrafficLog.created_at >= start_date)
    
    if end_date:
        query = query.where(TrafficLog.created_at <= end_date)
    
    query = query.order_by(desc(TrafficLog.created_at)).offset(skip).limit(limit)
    
    result = await db.execute(query)
    logs = result.scalars().all()
    
    # Build response with username
    response = []
    for log in logs:
        log_dict = {
            **{c.name: getattr(log, c.name) for c in log.__table__.columns},
            "username": log.user.username if log.user else None,
        }
        # Map meta_data back to metadata for API response
        if 'meta_data' in log_dict:
            log_dict['metadata'] = log_dict.pop('meta_data')
        response.append(TrafficLogResponse(**log_dict))
    
    return response


@router.get("/{log_id}", response_model=TrafficLogResponse)
async def get_traffic_log(
    log_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get a specific traffic log"""
    result = await db.execute(
        select(TrafficLog)
        .options(selectinload(TrafficLog.user))
        .where(TrafficLog.id == log_id)
    )
    log = result.scalar_one_or_none()
    
    if not log:
        raise HTTPException(status_code=404, detail="Traffic log not found")
    
    log_dict = {
        **{c.name: getattr(log, c.name) for c in log.__table__.columns},
        "username": log.user.username if log.user else None,
    }
    # Map meta_data back to metadata for API response
    if 'meta_data' in log_dict:
        log_dict['metadata'] = log_dict.pop('meta_data')
    return TrafficLogResponse(**log_dict)


@router.post("/", response_model=TrafficLogResponse, status_code=status.HTTP_201_CREATED)
async def create_traffic_log(
    log: TrafficLogCreate,
    request: Request,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a single traffic log entry"""
    # Validate log type
    if log.log_type not in [lt.value for lt in TrafficLogType]:
        raise HTTPException(status_code=400, detail=f"Invalid log_type: {log.log_type}")
    
    # Get IP and user agent from request
    ip_address = request.client.host if request.client else None
    user_agent = request.headers.get("user-agent")
    
    log_dict = log.dict()
    log_dict['meta_data'] = log_dict.pop('metadata', None)  # Map metadata to meta_data
    new_log = TrafficLog(
        **log_dict,
        user_id=current_user.id,
        ip_address=ip_address,
        user_agent=user_agent
    )
    db.add(new_log)
    await db.commit()
    await db.refresh(new_log)
    await db.refresh(new_log, ["user"])
    
    log_dict = {
        **{c.name: getattr(new_log, c.name) for c in new_log.__table__.columns},
        "username": new_log.user.username if new_log.user else None,
    }
    # Map meta_data back to metadata for API response
    if 'meta_data' in log_dict:
        log_dict['metadata'] = log_dict.pop('meta_data')
    return TrafficLogResponse(**log_dict)


@router.post("/bulk", response_model=Dict[str, Any], status_code=status.HTTP_201_CREATED)
async def create_traffic_logs_bulk(
    bulk_logs: TrafficLogBulkCreate,
    request: Request,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create multiple traffic log entries in bulk"""
    # Get IP and user agent from request
    ip_address = request.client.host if request.client else None
    user_agent = request.headers.get("user-agent")
    
    created_count = 0
    errors = []
    
    for log_data in bulk_logs.logs:
        try:
            # Validate log type
            if log_data.log_type not in [lt.value for lt in TrafficLogType]:
                errors.append(f"Invalid log_type: {log_data.log_type}")
                continue
            
            log_dict = log_data.dict()
            log_dict['meta_data'] = log_dict.pop('metadata', None)  # Map metadata to meta_data
            new_log = TrafficLog(
                **log_dict,
                user_id=current_user.id,
                ip_address=ip_address,
                user_agent=user_agent
            )
            db.add(new_log)
            created_count += 1
        except Exception as e:
            errors.append(f"Error creating log: {str(e)}")
    
    await db.commit()
    
    return {
        "created": created_count,
        "total": len(bulk_logs.logs),
        "errors": errors if errors else None
    }


@router.get("/stats/summary", response_model=Dict[str, Any])
async def get_traffic_log_stats(
    start_date: Optional[datetime] = Query(None),
    end_date: Optional[datetime] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get traffic log statistics"""
    query = select(
        TrafficLog.log_type,
        func.count(TrafficLog.id).label("count")
    ).group_by(TrafficLog.log_type)
    
    if start_date:
        query = query.where(TrafficLog.created_at >= start_date)
    
    if end_date:
        query = query.where(TrafficLog.created_at <= end_date)
    
    result = await db.execute(query)
    stats = result.all()
    
    return {
        "by_type": {stat.log_type: stat.count for stat in stats},
        "total": sum(stat.count for stat in stats)
    }

