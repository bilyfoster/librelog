"""
Log Revisions router
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func
from backend.database import get_db
from backend.models.log_revision import LogRevision
from backend.models.daily_log import DailyLog
from backend.routers.auth import get_current_user
from backend.models.user import User
from backend.services.audit_service import AuditService
from pydantic import BaseModel
from typing import Optional

router = APIRouter()


class LogRevisionResponse(BaseModel):
    id: int
    log_id: int
    revision_number: int
    json_data: dict
    changed_by: int
    change_summary: Optional[str]
    created_at: str
    changed_by_username: Optional[str] = None

    class Config:
        from_attributes = True


@router.get("/logs/{log_id}/revisions", response_model=list[LogRevisionResponse])
async def list_log_revisions(
    log_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List revisions for a log"""
    # Verify log exists
    log_result = await db.execute(select(DailyLog).where(DailyLog.id == log_id))
    if not log_result.scalar_one_or_none():
        raise HTTPException(status_code=404, detail="Log not found")
    
    result = await db.execute(
        select(LogRevision).where(LogRevision.log_id == log_id)
        .order_by(LogRevision.revision_number.desc())
    )
    revisions = result.scalars().all()
    
    # Load user data
    revisions_data = []
    for rev in revisions:
        rev_dict = LogRevisionResponse.model_validate(rev).model_dump()
        if rev.changed_by_user:
            rev_dict["changed_by_username"] = rev.changed_by_user.username
        revisions_data.append(LogRevisionResponse(**rev_dict))
    
    return revisions_data


@router.get("/logs/{log_id}/revisions/{revision_id}", response_model=LogRevisionResponse)
async def get_log_revision(
    log_id: int,
    revision_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get a specific revision"""
    result = await db.execute(
        select(LogRevision).where(
            LogRevision.id == revision_id,
            LogRevision.log_id == log_id
        )
    )
    revision = result.scalar_one_or_none()
    
    if not revision:
        raise HTTPException(status_code=404, detail="Revision not found")
    
    rev_dict = LogRevisionResponse.model_validate(revision).model_dump()
    if revision.changed_by_user:
        rev_dict["changed_by_username"] = revision.changed_by_user.username
    
    return LogRevisionResponse(**rev_dict)


@router.post("/logs/{log_id}/revert")
async def revert_to_revision(
    log_id: int,
    revision_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Revert log to a specific revision"""
    audit_service = AuditService(db)
    success = await audit_service.revert_to_revision(log_id, revision_id, current_user.id)
    
    if not success:
        raise HTTPException(status_code=400, detail="Failed to revert log")
    
    return {"message": "Log reverted successfully", "log_id": log_id, "revision_id": revision_id}

