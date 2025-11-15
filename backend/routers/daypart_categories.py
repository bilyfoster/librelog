"""
Daypart Categories router for organizing dayparts
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, desc
from typing import Optional, List
from pydantic import BaseModel

from backend.database import get_db
from backend.models.daypart_category import DaypartCategory
from backend.routers.auth import get_current_user
from backend.models.user import User

router = APIRouter(prefix="/daypart-categories", tags=["daypart-categories"])


class DaypartCategoryCreate(BaseModel):
    name: str
    description: Optional[str] = None
    color: Optional[str] = None
    icon: Optional[str] = None
    sort_order: int = 0


class DaypartCategoryUpdate(BaseModel):
    name: Optional[str] = None
    description: Optional[str] = None
    color: Optional[str] = None
    icon: Optional[str] = None
    sort_order: Optional[int] = None
    active: Optional[bool] = None


class DaypartCategoryResponse(BaseModel):
    id: int
    name: str
    description: Optional[str]
    color: Optional[str]
    icon: Optional[str]
    sort_order: int
    active: bool
    created_at: str
    updated_at: str

    class Config:
        from_attributes = True


@router.get("/", response_model=List[DaypartCategoryResponse])
async def list_daypart_categories(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    active_only: bool = Query(False),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List all daypart categories"""
    query = select(DaypartCategory)
    
    if active_only:
        query = query.where(DaypartCategory.active == True)
    
    query = query.order_by(DaypartCategory.sort_order, DaypartCategory.name).offset(skip).limit(limit)
    
    result = await db.execute(query)
    categories = result.scalars().all()
    return categories


@router.get("/{category_id}", response_model=DaypartCategoryResponse)
async def get_daypart_category(
    category_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get a specific daypart category"""
    result = await db.execute(select(DaypartCategory).where(DaypartCategory.id == category_id))
    category = result.scalar_one_or_none()
    
    if not category:
        raise HTTPException(status_code=404, detail="Daypart category not found")
    
    return category


@router.post("/", response_model=DaypartCategoryResponse, status_code=status.HTTP_201_CREATED)
async def create_daypart_category(
    category: DaypartCategoryCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a new daypart category"""
    # Check if name already exists
    result = await db.execute(select(DaypartCategory).where(DaypartCategory.name == category.name))
    existing = result.scalar_one_or_none()
    if existing:
        raise HTTPException(status_code=400, detail="Daypart category with this name already exists")
    
    new_category = DaypartCategory(**category.dict())
    db.add(new_category)
    await db.commit()
    await db.refresh(new_category)
    return new_category


@router.put("/{category_id}", response_model=DaypartCategoryResponse)
async def update_daypart_category(
    category_id: int,
    category_update: DaypartCategoryUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Update a daypart category"""
    result = await db.execute(select(DaypartCategory).where(DaypartCategory.id == category_id))
    category = result.scalar_one_or_none()
    
    if not category:
        raise HTTPException(status_code=404, detail="Daypart category not found")
    
    # Check name uniqueness if name is being updated
    if category_update.name and category_update.name != category.name:
        result = await db.execute(select(DaypartCategory).where(DaypartCategory.name == category_update.name))
        existing = result.scalar_one_or_none()
        if existing:
            raise HTTPException(status_code=400, detail="Daypart category with this name already exists")
    
    # Update fields
    update_data = category_update.dict(exclude_unset=True)
    for field, value in update_data.items():
        setattr(category, field, value)
    
    await db.commit()
    await db.refresh(category)
    return category


@router.delete("/{category_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_daypart_category(
    category_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Delete a daypart category"""
    result = await db.execute(select(DaypartCategory).where(DaypartCategory.id == category_id))
    category = result.scalar_one_or_none()
    
    if not category:
        raise HTTPException(status_code=404, detail="Daypart category not found")
    
    # Check if any dayparts are using this category
    from backend.models.daypart import Daypart
    result = await db.execute(select(Daypart).where(Daypart.category_id == category_id).limit(1))
    dayparts_using = result.scalars().first()
    if dayparts_using:
        raise HTTPException(
            status_code=400,
            detail="Cannot delete category: dayparts are still assigned to it"
        )
    
    await db.delete(category)
    await db.commit()

