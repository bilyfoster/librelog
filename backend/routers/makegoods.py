"""
Makegoods router for traffic management
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from backend.database import get_db
from backend.models.makegood import Makegood
from backend.models.spot import Spot
from backend.routers.auth import get_current_user
from backend.models.user import User
from pydantic import BaseModel
from typing import Optional
from datetime import datetime, timezone

router = APIRouter()


class MakegoodCreate(BaseModel):
    original_spot_id: int
    makegood_spot_id: int
    campaign_id: Optional[int] = None
    reason: Optional[str] = None


class MakegoodResponse(BaseModel):
    id: int
    original_spot_id: int
    makegood_spot_id: int
    campaign_id: Optional[int]
    reason: Optional[str]
    approved_by: Optional[int]
    approved_at: Optional[str]
    created_at: str

    class Config:
        from_attributes = True


@router.get("/", response_model=list[MakegoodResponse])
async def list_makegoods(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    campaign_id: Optional[int] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List all makegoods"""
    query = select(Makegood)
    
    if campaign_id:
        query = query.where(Makegood.campaign_id == campaign_id)
    
    query = query.offset(skip).limit(limit).order_by(Makegood.created_at.desc())
    
    result = await db.execute(query)
    makegoods = result.scalars().all()
    
    return [MakegoodResponse.model_validate(mg) for mg in makegoods]


@router.post("/", response_model=MakegoodResponse, status_code=status.HTTP_201_CREATED)
async def create_makegood(
    makegood: MakegoodCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a new makegood"""
    # Verify spots exist
    original_result = await db.execute(select(Spot).where(Spot.id == makegood.original_spot_id))
    if not original_result.scalar_one_or_none():
        raise HTTPException(status_code=404, detail="Original spot not found")
    
    makegood_result = await db.execute(select(Spot).where(Spot.id == makegood.makegood_spot_id))
    if not makegood_result.scalar_one_or_none():
        raise HTTPException(status_code=404, detail="Makegood spot not found")
    
    new_makegood = Makegood(**makegood.model_dump())
    db.add(new_makegood)
    await db.commit()
    await db.refresh(new_makegood)
    
    return MakegoodResponse.model_validate(new_makegood)


@router.post("/{makegood_id}/approve", response_model=MakegoodResponse)
async def approve_makegood(
    makegood_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Approve a makegood"""
    result = await db.execute(select(Makegood).where(Makegood.id == makegood_id))
    makegood = result.scalar_one_or_none()
    
    if not makegood:
        raise HTTPException(status_code=404, detail="Makegood not found")
    
    if makegood.approved_by:
        raise HTTPException(status_code=400, detail="Makegood already approved")
    
    makegood.approved_by = current_user.id
    makegood.approved_at = datetime.now(timezone.utc)
    
    await db.commit()
    await db.refresh(makegood)
    
    return MakegoodResponse.model_validate(makegood)

