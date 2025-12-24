"""
Sales Regions router for traffic management
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from uuid import UUID
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, or_
from sqlalchemy.orm import selectinload
from backend.database import get_db
from backend.models.sales_region import SalesRegion
from backend.models.sales_rep import SalesRep
from backend.routers.auth import get_current_user
from backend.models.user import User
from backend.schemas.sales_region import SalesRegionCreate, SalesRegionUpdate, SalesRegionResponse, SalesRegionWithOffices
from typing import Optional, List

router = APIRouter()


@router.get("/", response_model=List[SalesRegionResponse])
async def list_sales_regions(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    active_only: bool = Query(True),
    search: Optional[str] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List all sales regions with optional filtering"""
    query = select(SalesRegion)
    
    if active_only:
        query = query.where(SalesRegion.active == True)
    
    if search:
        search_term = f"%{search}%"
        query = query.where(
            or_(
                SalesRegion.name.ilike(search_term),
                SalesRegion.description.ilike(search_term)
            )
        )
    
    query = query.offset(skip).limit(limit).order_by(SalesRegion.name)
    
    result = await db.execute(query)
    regions = result.scalars().all()
    
    return [SalesRegionResponse.model_validate(region) for region in regions]


@router.get("/{region_id}", response_model=SalesRegionWithOffices)
async def get_sales_region(
    region_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get a specific sales region with offices"""
    result = await db.execute(
        select(SalesRegion)
        .where(SalesRegion.id == region_id)
        .options(selectinload(SalesRegion.sales_offices))
    )
    region = result.scalar_one_or_none()
    
    if not region:
        raise HTTPException(status_code=404, detail="Sales region not found")
    
    region_data = SalesRegionWithOffices.model_validate(region)
    region_data.sales_offices = [
        {
            "id": office.id,
            "name": office.name,
            "address": office.address,
            "active": office.active
        }
        for office in region.sales_offices
    ]
    
    return region_data


@router.post("/", response_model=SalesRegionResponse, status_code=status.HTTP_201_CREATED)
async def create_sales_region(
    region: SalesRegionCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a new sales region"""
    # Check if region name already exists
    existing = await db.execute(select(SalesRegion).where(SalesRegion.name == region.name))
    if existing.scalar_one_or_none():
        raise HTTPException(status_code=400, detail="Sales region with this name already exists")
    
    new_region = SalesRegion(**region.model_dump())
    db.add(new_region)
    await db.commit()
    await db.refresh(new_region)
    
    return SalesRegionResponse.model_validate(new_region)


@router.put("/{region_id}", response_model=SalesRegionResponse)
async def update_sales_region(
    region_id: UUID,
    region_update: SalesRegionUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Update a sales region"""
    result = await db.execute(select(SalesRegion).where(SalesRegion.id == region_id))
    region = result.scalar_one_or_none()
    
    if not region:
        raise HTTPException(status_code=404, detail="Sales region not found")
    
    # Check name uniqueness if name is being updated
    if region_update.name and region_update.name != region.name:
        existing = await db.execute(select(SalesRegion).where(SalesRegion.name == region_update.name))
        if existing.scalar_one_or_none():
            raise HTTPException(status_code=400, detail="Sales region with this name already exists")
    
    # Update fields
    update_data = region_update.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(region, field, value)
    
    await db.commit()
    await db.refresh(region)
    
    return SalesRegionResponse.model_validate(region)


@router.delete("/{region_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_sales_region(
    region_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Soft delete a sales region (set active=False)"""
    result = await db.execute(select(SalesRegion).where(SalesRegion.id == region_id))
    region = result.scalar_one_or_none()
    
    if not region:
        raise HTTPException(status_code=404, detail="Sales region not found")
    
    region.active = False
    await db.commit()
    
    return None


@router.post("/{region_id}/sales-reps/{sales_rep_id}", status_code=status.HTTP_204_NO_CONTENT)
async def add_sales_rep_to_region(
    region_id: UUID,
    sales_rep_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Add a sales rep to a region"""
    region_result = await db.execute(
        select(SalesRegion)
        .where(SalesRegion.id == region_id)
        .options(selectinload(SalesRegion.sales_reps))
    )
    region = region_result.scalar_one_or_none()
    if not region:
        raise HTTPException(status_code=404, detail="Sales region not found")
    
    rep_result = await db.execute(select(SalesRep).where(SalesRep.id == sales_rep_id))
    rep = rep_result.scalar_one_or_none()
    if not rep:
        raise HTTPException(status_code=404, detail="Sales rep not found")
    
    if rep in region.sales_reps:
        raise HTTPException(status_code=400, detail="Sales rep already in this region")
    
    region.sales_reps.append(rep)
    await db.commit()
    
    return None


@router.delete("/{region_id}/sales-reps/{sales_rep_id}", status_code=status.HTTP_204_NO_CONTENT)
async def remove_sales_rep_from_region(
    region_id: UUID,
    sales_rep_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Remove a sales rep from a region"""
    region_result = await db.execute(
        select(SalesRegion)
        .where(SalesRegion.id == region_id)
        .options(selectinload(SalesRegion.sales_reps))
    )
    region = region_result.scalar_one_or_none()
    if not region:
        raise HTTPException(status_code=404, detail="Sales region not found")
    
    rep_result = await db.execute(select(SalesRep).where(SalesRep.id == sales_rep_id))
    rep = rep_result.scalar_one_or_none()
    if not rep:
        raise HTTPException(status_code=404, detail="Sales rep not found")
    
    if rep not in region.sales_reps:
        raise HTTPException(status_code=400, detail="Sales rep not in this region")
    
    region.sales_reps.remove(rep)
    await db.commit()
    
    return None

