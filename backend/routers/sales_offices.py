"""
Sales Offices router for traffic management
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from uuid import UUID
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, or_
from sqlalchemy.orm import selectinload
from backend.database import get_db
from backend.models.sales_office import SalesOffice
from backend.models.sales_region import SalesRegion
from backend.models.sales_rep import SalesRep
from backend.routers.auth import get_current_user
from backend.models.user import User
from backend.schemas.sales_office import SalesOfficeCreate, SalesOfficeUpdate, SalesOfficeResponse
from typing import Optional, List

router = APIRouter()


@router.get("/", response_model=List[SalesOfficeResponse])
async def list_sales_offices(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    active_only: bool = Query(True),
    region_id: Optional[UUID] = Query(None),
    search: Optional[str] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List all sales offices with optional filtering"""
    query = select(SalesOffice)
    
    if active_only:
        query = query.where(SalesOffice.active == True)
    
    if region_id:
        query = query.where(SalesOffice.region_id == region_id)
    
    if search:
        search_term = f"%{search}%"
        query = query.where(
            or_(
                SalesOffice.name.ilike(search_term),
                SalesOffice.address.ilike(search_term)
            )
        )
    
    query = query.options(selectinload(SalesOffice.region))
    query = query.offset(skip).limit(limit).order_by(SalesOffice.name)
    
    result = await db.execute(query)
    offices = result.scalars().all()
    
    offices_data = []
    for office in offices:
        office_dict = SalesOfficeResponse.model_validate(office).model_dump()
        office_dict["region_name"] = office.region.name if office.region else None
        offices_data.append(SalesOfficeResponse(**office_dict))
    
    return offices_data


@router.get("/{office_id}", response_model=SalesOfficeResponse)
async def get_sales_office(
    office_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get a specific sales office"""
    result = await db.execute(
        select(SalesOffice)
        .where(SalesOffice.id == office_id)
        .options(selectinload(SalesOffice.region))
    )
    office = result.scalar_one_or_none()
    
    if not office:
        raise HTTPException(status_code=404, detail="Sales office not found")
    
    office_dict = SalesOfficeResponse.model_validate(office).model_dump()
    office_dict["region_name"] = office.region.name if office.region else None
    return SalesOfficeResponse(**office_dict)


@router.post("/", response_model=SalesOfficeResponse, status_code=status.HTTP_201_CREATED)
async def create_sales_office(
    office: SalesOfficeCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a new sales office"""
    # Validate region if provided
    if office.region_id:
        region_result = await db.execute(select(SalesRegion).where(SalesRegion.id == office.region_id))
        if not region_result.scalar_one_or_none():
            raise HTTPException(status_code=400, detail="Invalid region_id")
    
    new_office = SalesOffice(**office.model_dump())
    db.add(new_office)
    await db.commit()
    await db.refresh(new_office, ["region"])
    
    office_dict = SalesOfficeResponse.model_validate(new_office).model_dump()
    office_dict["region_name"] = new_office.region.name if new_office.region else None
    return SalesOfficeResponse(**office_dict)


@router.put("/{office_id}", response_model=SalesOfficeResponse)
async def update_sales_office(
    office_id: UUID,
    office_update: SalesOfficeUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Update a sales office"""
    result = await db.execute(select(SalesOffice).where(SalesOffice.id == office_id))
    office = result.scalar_one_or_none()
    
    if not office:
        raise HTTPException(status_code=404, detail="Sales office not found")
    
    # Validate region if being updated
    if office_update.region_id is not None:
        region_result = await db.execute(select(SalesRegion).where(SalesRegion.id == office_update.region_id))
        if not region_result.scalar_one_or_none():
            raise HTTPException(status_code=400, detail="Invalid region_id")
    
    # Update fields
    update_data = office_update.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(office, field, value)
    
    await db.commit()
    await db.refresh(office, ["region"])
    
    office_dict = SalesOfficeResponse.model_validate(office).model_dump()
    office_dict["region_name"] = office.region.name if office.region else None
    return SalesOfficeResponse(**office_dict)


@router.delete("/{office_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_sales_office(
    office_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Soft delete a sales office (set active=False)"""
    result = await db.execute(select(SalesOffice).where(SalesOffice.id == office_id))
    office = result.scalar_one_or_none()
    
    if not office:
        raise HTTPException(status_code=404, detail="Sales office not found")
    
    office.active = False
    await db.commit()
    
    return None


@router.post("/{office_id}/sales-reps/{sales_rep_id}", status_code=status.HTTP_204_NO_CONTENT)
async def add_sales_rep_to_office(
    office_id: UUID,
    sales_rep_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Add a sales rep to an office"""
    office_result = await db.execute(
        select(SalesOffice)
        .where(SalesOffice.id == office_id)
        .options(selectinload(SalesOffice.sales_reps))
    )
    office = office_result.scalar_one_or_none()
    if not office:
        raise HTTPException(status_code=404, detail="Sales office not found")
    
    rep_result = await db.execute(select(SalesRep).where(SalesRep.id == sales_rep_id))
    rep = rep_result.scalar_one_or_none()
    if not rep:
        raise HTTPException(status_code=404, detail="Sales rep not found")
    
    if rep in office.sales_reps:
        raise HTTPException(status_code=400, detail="Sales rep already in this office")
    
    office.sales_reps.append(rep)
    await db.commit()
    
    return None


@router.delete("/{office_id}/sales-reps/{sales_rep_id}", status_code=status.HTTP_204_NO_CONTENT)
async def remove_sales_rep_from_office(
    office_id: UUID,
    sales_rep_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Remove a sales rep from an office"""
    office_result = await db.execute(
        select(SalesOffice)
        .where(SalesOffice.id == office_id)
        .options(selectinload(SalesOffice.sales_reps))
    )
    office = office_result.scalar_one_or_none()
    if not office:
        raise HTTPException(status_code=404, detail="Sales office not found")
    
    rep_result = await db.execute(select(SalesRep).where(SalesRep.id == sales_rep_id))
    rep = rep_result.scalar_one_or_none()
    if not rep:
        raise HTTPException(status_code=404, detail="Sales rep not found")
    
    if rep not in office.sales_reps:
        raise HTTPException(status_code=400, detail="Sales rep not in this office")
    
    office.sales_reps.remove(rep)
    await db.commit()
    
    return None

