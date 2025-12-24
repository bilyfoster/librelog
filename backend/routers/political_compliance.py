"""
Political Compliance router for FCC compliance tracking
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from uuid import UUID
from sqlalchemy.ext.asyncio import AsyncSession
from backend.database import get_db
from backend.routers.auth import get_current_user
from backend.models.user import User
from backend.services.political_compliance_service import PoliticalComplianceService
from pydantic import BaseModel
from typing import Optional, List, Dict, Any
from datetime import datetime, timezone
import structlog

logger = structlog.get_logger()

router = APIRouter(prefix="/political-compliance", tags=["political-compliance"])


class PoliticalRecordCreate(BaseModel):
    copy_id: Optional[UUID] = None
    order_id: Optional[UUID] = None
    advertiser_id: Optional[UUID] = None
    advertiser_category: str = "political"
    sponsor_name: str
    sponsor_id: Optional[str] = None
    office_sought: Optional[str] = None
    disclaimers_required: Optional[str] = None
    political_window_start: Optional[datetime] = None
    political_window_end: Optional[datetime] = None
    notes: Optional[str] = None


class PoliticalRecordResponse(BaseModel):
    id: UUID
    copy_id: Optional[UUID]
    order_id: Optional[UUID]
    advertiser_id: Optional[UUID]
    advertiser_category: str
    sponsor_name: str
    sponsor_id: Optional[str]
    office_sought: Optional[str]
    disclaimers_required: Optional[str]
    disclaimers_included: bool
    compliance_status: str
    no_substitution_allowed: bool
    archived: bool
    created_at: str
    updated_at: str

    class Config:
        from_attributes = True


@router.post("/", response_model=PoliticalRecordResponse, status_code=status.HTTP_201_CREATED)
async def create_political_record(
    record_data: PoliticalRecordCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a new political record"""
    service = PoliticalComplianceService(db)
    try:
        record = await service.create_political_record(
            copy_id=record_data.copy_id,
            order_id=record_data.order_id,
            advertiser_id=record_data.advertiser_id,
            advertiser_category=record_data.advertiser_category,
            sponsor_name=record_data.sponsor_name,
            sponsor_id=record_data.sponsor_id,
            office_sought=record_data.office_sought,
            disclaimers_required=record_data.disclaimers_required,
            political_window_start=record_data.political_window_start,
            political_window_end=record_data.political_window_end,
            notes=record_data.notes
        )
        return PoliticalRecordResponse.model_validate(record)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/fcc-log", response_model=List[Dict[str, Any]])
async def get_fcc_compliance_log(
    start_date: datetime = Query(...),
    end_date: datetime = Query(...),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Generate FCC compliance log"""
    service = PoliticalComplianceService(db)
    log = await service.generate_fcc_compliance_log(start_date, end_date)
    return log

