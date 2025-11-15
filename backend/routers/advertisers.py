"""
Advertisers router for traffic management
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func
from backend.database import get_db
from backend.models.advertiser import Advertiser
from backend.routers.auth import get_current_user
from backend.models.user import User
from pydantic import BaseModel, EmailStr
from typing import Optional
from decimal import Decimal

router = APIRouter()


class AdvertiserCreate(BaseModel):
    name: str
    contact_name: Optional[str] = None
    email: Optional[EmailStr] = None
    phone: Optional[str] = None
    address: Optional[str] = None
    tax_id: Optional[str] = None
    payment_terms: Optional[str] = None
    credit_limit: Optional[Decimal] = None


class AdvertiserUpdate(BaseModel):
    name: Optional[str] = None
    contact_name: Optional[str] = None
    email: Optional[EmailStr] = None
    phone: Optional[str] = None
    address: Optional[str] = None
    tax_id: Optional[str] = None
    payment_terms: Optional[str] = None
    credit_limit: Optional[Decimal] = None
    active: Optional[bool] = None


class AdvertiserResponse(BaseModel):
    id: int
    name: str
    contact_name: Optional[str]
    email: Optional[str]
    phone: Optional[str]
    address: Optional[str]
    tax_id: Optional[str]
    payment_terms: Optional[str]
    credit_limit: Optional[Decimal]
    active: bool
    created_at: str
    updated_at: str

    class Config:
        from_attributes = True


@router.get("/", response_model=list[AdvertiserResponse])
async def list_advertisers(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    active_only: bool = Query(True),
    search: Optional[str] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List all advertisers with optional filtering"""
    query = select(Advertiser)
    
    if active_only:
        query = query.where(Advertiser.active == True)
    
    if search:
        query = query.where(
            Advertiser.name.ilike(f"%{search}%")
        )
    
    query = query.offset(skip).limit(limit).order_by(Advertiser.name)
    
    result = await db.execute(query)
    advertisers = result.scalars().all()
    
    return [AdvertiserResponse.model_validate(adv) for adv in advertisers]


@router.post("/", response_model=AdvertiserResponse, status_code=status.HTTP_201_CREATED)
async def create_advertiser(
    advertiser: AdvertiserCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a new advertiser"""
    new_advertiser = Advertiser(**advertiser.model_dump())
    db.add(new_advertiser)
    await db.commit()
    await db.refresh(new_advertiser)
    
    return AdvertiserResponse.model_validate(new_advertiser)


@router.get("/{advertiser_id}", response_model=AdvertiserResponse)
async def get_advertiser(
    advertiser_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get a specific advertiser"""
    result = await db.execute(select(Advertiser).where(Advertiser.id == advertiser_id))
    advertiser = result.scalar_one_or_none()
    
    if not advertiser:
        raise HTTPException(status_code=404, detail="Advertiser not found")
    
    return AdvertiserResponse.model_validate(advertiser)


@router.put("/{advertiser_id}", response_model=AdvertiserResponse)
async def update_advertiser(
    advertiser_id: int,
    advertiser_update: AdvertiserUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Update an advertiser"""
    result = await db.execute(select(Advertiser).where(Advertiser.id == advertiser_id))
    advertiser = result.scalar_one_or_none()
    
    if not advertiser:
        raise HTTPException(status_code=404, detail="Advertiser not found")
    
    # Update fields
    update_data = advertiser_update.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(advertiser, field, value)
    
    await db.commit()
    await db.refresh(advertiser)
    
    return AdvertiserResponse.model_validate(advertiser)


@router.delete("/{advertiser_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_advertiser(
    advertiser_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Soft delete an advertiser (set active=False)"""
    result = await db.execute(select(Advertiser).where(Advertiser.id == advertiser_id))
    advertiser = result.scalar_one_or_none()
    
    if not advertiser:
        raise HTTPException(status_code=404, detail="Advertiser not found")
    
    advertiser.active = False
    await db.commit()
    
    return None

