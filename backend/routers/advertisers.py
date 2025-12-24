"""
Advertisers router for traffic management
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from uuid import UUID
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
    contact_first_name: Optional[str] = None
    contact_last_name: Optional[str] = None
    email: Optional[EmailStr] = None
    phone: Optional[str] = None
    address: Optional[str] = None
    tax_id: Optional[str] = None
    payment_terms: Optional[str] = None
    credit_limit: Optional[Decimal] = None
    
    class Config:
        json_schema_extra = {
            "example": {
                "name": "Acme Corporation",
                "contact_first_name": "John",
                "contact_last_name": "Doe",
                "email": "john.doe@acme.com",
                "phone": "555-1234",
                "address": "123 Main St, City, State 12345",
                "tax_id": "12-3456789",
                "payment_terms": "Net 30",
                "credit_limit": 10000.00
            }
        }


class AdvertiserUpdate(BaseModel):
    name: Optional[str] = None
    contact_first_name: Optional[str] = None
    contact_last_name: Optional[str] = None
    email: Optional[EmailStr] = None
    phone: Optional[str] = None
    address: Optional[str] = None
    tax_id: Optional[str] = None
    payment_terms: Optional[str] = None
    credit_limit: Optional[Decimal] = None
    active: Optional[bool] = None


class AdvertiserResponse(BaseModel):
    id: UUID
    name: str
    contact_first_name: Optional[str]
    contact_last_name: Optional[str]
    email: Optional[str]
    phone: Optional[str]
    address: Optional[str]
    tax_id: Optional[str]
    payment_terms: Optional[str]
    credit_limit: Optional[Decimal]
    active: bool
    created_at: Optional[str]
    updated_at: Optional[str]

    class Config:
        from_attributes = True


def advertiser_to_response(adv: Advertiser) -> AdvertiserResponse:
    """Convert Advertiser model to AdvertiserResponse with proper datetime serialization"""
    return AdvertiserResponse(
        id=adv.id,
        name=adv.name,
        contact_first_name=adv.contact_first_name,
        contact_last_name=adv.contact_last_name,
        email=adv.email,
        phone=adv.phone,
        address=adv.address,
        tax_id=adv.tax_id,
        payment_terms=adv.payment_terms,
        credit_limit=adv.credit_limit,
        active=adv.active,
        created_at=adv.created_at.isoformat() if adv.created_at else None,
        updated_at=adv.updated_at.isoformat() if adv.updated_at else None,
    )


@router.get("/", response_model=list[AdvertiserResponse])
async def list_advertisers(
    skip: int = Query(0, ge=0, description="Number of records to skip for pagination"),
    limit: int = Query(100, ge=1, le=1000, description="Maximum number of records to return"),
    active_only: bool = Query(True, description="Filter to show only active advertisers"),
    search: Optional[str] = Query(None, description="Search term to filter advertisers by name"),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """
    List all advertisers with optional filtering
    
    Returns a paginated list of advertisers. Use the search parameter to filter by name.
    """
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
    
    return [advertiser_to_response(adv) for adv in advertisers]


@router.post("/", response_model=AdvertiserResponse, status_code=status.HTTP_201_CREATED)
async def create_advertiser(
    advertiser: AdvertiserCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """
    Create a new advertiser
    
    Creates a new advertiser record. The name field is required.
    Contact information, payment terms, and credit limit are optional.
    """
    new_advertiser = Advertiser(**advertiser.model_dump())
    db.add(new_advertiser)
    await db.commit()
    await db.refresh(new_advertiser)
    
    return advertiser_to_response(new_advertiser)


@router.get("/{advertiser_id}", response_model=AdvertiserResponse)
async def get_advertiser(
    advertiser_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get a specific advertiser"""
    result = await db.execute(select(Advertiser).where(Advertiser.id == advertiser_id))
    advertiser = result.scalar_one_or_none()
    
    if not advertiser:
        raise HTTPException(status_code=404, detail="Advertiser not found")
    
    return advertiser_to_response(advertiser)


@router.put("/{advertiser_id}", response_model=AdvertiserResponse)
async def update_advertiser(
    advertiser_id: UUID,
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
    
    return advertiser_to_response(advertiser)


@router.delete("/{advertiser_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_advertiser(
    advertiser_id: UUID,
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

