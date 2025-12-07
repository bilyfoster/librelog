"""
Audit Logs router
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_
from backend.database import get_db
from backend.models.audit_log import AuditLog
from backend.routers.auth import get_current_user
from backend.models.user import User
from pydantic import BaseModel
from typing import Optional
from datetime import date, datetime
import json

router = APIRouter()


class AuditLogResponse(BaseModel):
    id: int
    user_id: int
    action: str
    resource_type: str
    resource_id: Optional[int]
    changes: Optional[dict] = None  # Mapped from details field
    ip_address: Optional[str]
    user_agent: Optional[str]
    created_at: str
    username: Optional[str] = None

    class Config:
        from_attributes = True


@router.get("/", response_model=list[AuditLogResponse])
async def list_audit_logs(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    user_id: Optional[int] = Query(None),
    action: Optional[str] = Query(None),
    resource_type: Optional[str] = Query(None),
    start_date: Optional[date] = Query(None),
    end_date: Optional[date] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List all audit logs with optional filtering"""
    query = select(AuditLog)
    
    if user_id:
        query = query.where(AuditLog.user_id == user_id)
    
    if action:
        query = query.where(AuditLog.action == action)
    
    if resource_type:
        query = query.where(AuditLog.resource_type == resource_type)
    
    if start_date:
        query = query.where(AuditLog.created_at >= datetime.combine(start_date, datetime.min.time()))
    if end_date:
        query = query.where(AuditLog.created_at <= datetime.combine(end_date, datetime.max.time()))
    
    query = query.offset(skip).limit(limit).order_by(AuditLog.created_at.desc())
    
    result = await db.execute(query)
    audit_logs = result.scalars().all()
    
    # Load user data and convert details to changes
    logs_data = []
    for log in audit_logs:
        log_dict = AuditLogResponse.model_validate(log).model_dump()
        # Map details to changes (for backward compatibility)
        if hasattr(log, 'details') and log.details:
            try:
                log_dict["changes"] = json.loads(log.details) if log.details else None
            except (json.JSONDecodeError, TypeError):
                # If details is not JSON, store as text in changes
                log_dict["changes"] = {"details": log.details} if log.details else None
        else:
            log_dict["changes"] = None
        log_dict["created_at"] = log.created_at.isoformat() if log.created_at else ""
        if log.user:
            log_dict["username"] = log.user.username
        logs_data.append(AuditLogResponse(**log_dict))
    
    return logs_data


@router.get("/{audit_log_id}", response_model=AuditLogResponse)
async def get_audit_log(
    audit_log_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get a specific audit log"""
    from datetime import datetime
    from sqlalchemy.orm import selectinload
    
    try:
        result = await db.execute(
            select(AuditLog)
            .options(selectinload(AuditLog.user))
            .where(AuditLog.id == audit_log_id)
        )
        audit_log = result.scalar_one_or_none()
        
        if not audit_log:
            raise HTTPException(status_code=404, detail="Audit log not found")
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to retrieve audit log: {str(e)}")
    
    log_dict = AuditLogResponse.model_validate(audit_log).model_dump()
    # Map details to changes (for backward compatibility)
    if hasattr(audit_log, 'details') and audit_log.details:
        try:
            log_dict["changes"] = json.loads(audit_log.details) if audit_log.details else None
        except (json.JSONDecodeError, TypeError):
            # If details is not JSON, store as text in changes
            log_dict["changes"] = {"details": audit_log.details} if audit_log.details else None
    else:
        log_dict["changes"] = None
    log_dict["created_at"] = audit_log.created_at.isoformat() if audit_log.created_at else ""
    if audit_log.user:
        log_dict["username"] = audit_log.user.username
    
    return AuditLogResponse(**log_dict)

