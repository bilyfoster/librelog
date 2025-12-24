"""
Live Reads router for managing live read scripts and performance tracking
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from uuid import UUID
from sqlalchemy.ext.asyncio import AsyncSession
from backend.database import get_db
from backend.routers.auth import get_current_user
from backend.models.user import User
from backend.services.live_read_service import LiveReadService
from pydantic import BaseModel
from typing import Optional, List, Dict, Any
from datetime import datetime, timezone
import structlog

logger = structlog.get_logger()

router = APIRouter(prefix="/live-reads", tags=["live-reads"])


class LiveReadCreate(BaseModel):
    script_text: str
    copy_id: Optional[UUID] = None
    order_id: Optional[UUID] = None
    advertiser_id: Optional[UUID] = None
    script_title: Optional[str] = None
    scheduled_time: Optional[datetime] = None
    scheduled_date: Optional[datetime] = None
    notes: Optional[str] = None


class LiveReadResponse(BaseModel):
    id: UUID
    copy_id: Optional[UUID]
    order_id: Optional[UUID]
    advertiser_id: Optional[UUID]
    script_text: str
    script_title: Optional[str]
    scheduled_time: Optional[str]
    scheduled_date: Optional[str]
    performed_time: Optional[str]
    performed_by: Optional[int]
    confirmed: bool
    proof_of_performance: Optional[str]
    status: str
    makegood_required: bool
    created_at: str
    updated_at: str

    class Config:
        from_attributes = True


@router.get("/", response_model=List[LiveReadResponse])
async def list_live_reads(
    copy_id: Optional[UUID] = Query(None),
    order_id: Optional[UUID] = Query(None),
    advertiser_id: Optional[UUID] = Query(None),
    status: Optional[str] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List live reads"""
    service = LiveReadService(db)
    reads = await service.get_live_reads(
        copy_id=copy_id,
        order_id=order_id,
        advertiser_id=advertiser_id,
        status=status
    )
    return [LiveReadResponse.model_validate(r) for r in reads]


@router.post("/", response_model=LiveReadResponse, status_code=status.HTTP_201_CREATED)
async def create_live_read(
    read_data: LiveReadCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a new live read"""
    service = LiveReadService(db)
    try:
        read = await service.create_live_read(
            script_text=read_data.script_text,
            copy_id=read_data.copy_id,
            order_id=read_data.order_id,
            advertiser_id=read_data.advertiser_id,
            script_title=read_data.script_title,
            scheduled_time=read_data.scheduled_time,
            scheduled_date=read_data.scheduled_date,
            notes=read_data.notes
        )
        return LiveReadResponse.model_validate(read)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/{live_read_id}/confirm", response_model=LiveReadResponse)
async def confirm_performance(
    live_read_id: UUID,
    proof_data: Optional[Dict[str, Any]] = None,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Confirm that a live read was performed"""
    service = LiveReadService(db)
    try:
        read = await service.confirm_performance(
            live_read_id=live_read_id,
            performed_by=current_user.id,
            proof_data=proof_data
        )
        return LiveReadResponse.model_validate(read)
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))


@router.post("/{live_read_id}/missed", response_model=LiveReadResponse)
async def mark_missed(
    live_read_id: UUID,
    create_makegood: bool = True,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Mark a live read as missed"""
    service = LiveReadService(db)
    try:
        read = await service.mark_missed(live_read_id, create_makegood=create_makegood)
        return LiveReadResponse.model_validate(read)
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))

