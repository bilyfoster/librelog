"""
Agencies router for traffic management
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from uuid import UUID
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, or_
from sqlalchemy.orm import selectinload
from backend.database import get_db
from backend.models.agency import Agency
from backend.routers.auth import get_current_user
from backend.models.user import User
from pydantic import BaseModel, EmailStr
from typing import Optional
from decimal import Decimal

router = APIRouter()


class AgencyCreate(BaseModel):
    name: str
    contact_first_name: Optional[str] = None
    contact_last_name: Optional[str] = None
    email: Optional[EmailStr] = None
    phone: Optional[str] = None
    address: Optional[str] = None
    commission_rate: Optional[Decimal] = None


class AgencyUpdate(BaseModel):
    name: Optional[str] = None
    contact_first_name: Optional[str] = None
    contact_last_name: Optional[str] = None
    email: Optional[EmailStr] = None
    phone: Optional[str] = None
    address: Optional[str] = None
    commission_rate: Optional[Decimal] = None
    active: Optional[bool] = None


class AgencyResponse(BaseModel):
    id: UUID
    name: str
    contact_first_name: Optional[str]
    contact_last_name: Optional[str]
    email: Optional[str]
    phone: Optional[str]
    address: Optional[str]
    commission_rate: Optional[Decimal]
    active: bool
    created_at: Optional[str]
    updated_at: Optional[str]

    class Config:
        from_attributes = True


def agency_to_response(ag: Agency) -> AgencyResponse:
    """Convert Agency model to AgencyResponse with proper datetime serialization"""
    return AgencyResponse(
        id=ag.id,
        name=ag.name,
        contact_first_name=ag.contact_first_name,
        contact_last_name=ag.contact_last_name,
        email=ag.email,
        phone=ag.phone,
        address=ag.address,
        commission_rate=ag.commission_rate,
        active=ag.active,
        created_at=ag.created_at.isoformat() if ag.created_at else None,
        updated_at=ag.updated_at.isoformat() if ag.updated_at else None,
    )


@router.get("/", response_model=list[AgencyResponse])
async def list_agencies(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    active_only: bool = Query(True),
    search: Optional[str] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List all agencies with optional filtering"""
    query = select(Agency)
    
    if active_only:
        query = query.where(Agency.active == True)
    
    if search:
        search_term = f"%{search}%"
        query = query.where(
            or_(
                Agency.name.ilike(search_term),
                Agency.contact_first_name.ilike(search_term),
                Agency.contact_last_name.ilike(search_term),
                Agency.email.ilike(search_term)
            )
        )
    query = query.offset(skip).limit(limit).order_by(Agency.name)
    
    result = await db.execute(query)
    agencies = result.scalars().all()
    
    return [agency_to_response(ag) for ag in agencies]


@router.post("/", response_model=AgencyResponse, status_code=status.HTTP_201_CREATED)
async def create_agency(
    agency: AgencyCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a new agency"""
    new_agency = Agency(**agency.model_dump())
    db.add(new_agency)
    await db.commit()
    await db.refresh(new_agency)
    
    return agency_to_response(new_agency)


@router.get("/{agency_id}", response_model=AgencyResponse)
async def get_agency(
    agency_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get a specific agency"""
    result = await db.execute(
        select(Agency)
        .where(Agency.id == agency_id)
        .options(selectinload(Agency.account_manager))
    )
    agency = result.scalar_one_or_none()
    
    if not agency:
        raise HTTPException(status_code=404, detail="Agency not found")
    
    return agency_to_response(agency)


@router.put("/{agency_id}", response_model=AgencyResponse)
async def update_agency(
    agency_id: UUID,
    agency_update: AgencyUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Update an agency"""
    result = await db.execute(select(Agency).where(Agency.id == agency_id))
    agency = result.scalar_one_or_none()
    
    if not agency:
        raise HTTPException(status_code=404, detail="Agency not found")
    
    # Update fields
    update_data = agency_update.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(agency, field, value)
    
    await db.commit()
    await db.refresh(agency, ["account_manager"])
    
    return agency_to_response(agency)


@router.delete("/{agency_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_agency(
    agency_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Soft delete an agency (set active=False)"""
    result = await db.execute(select(Agency).where(Agency.id == agency_id))
    agency = result.scalar_one_or_none()
    
    if not agency:
        raise HTTPException(status_code=404, detail="Agency not found")
    
    agency.active = False
    await db.commit()
    
    return None

