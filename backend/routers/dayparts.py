"""
Dayparts router for traffic scheduling
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from sqlalchemy.orm import selectinload
from backend.database import get_db
from backend.models.daypart import Daypart
from backend.routers.auth import get_current_user
from backend.models.user import User
from pydantic import BaseModel
from typing import Optional, List
from datetime import time

router = APIRouter()


class DaypartCreate(BaseModel):
    name: str
    start_time: str  # HH:MM:SS format
    end_time: str  # HH:MM:SS format
    days_of_week: Optional[List[int]] = None  # 0=Monday, 6=Sunday
    category_id: Optional[int] = None
    description: Optional[str] = None
    active: bool = True


class DaypartUpdate(BaseModel):
    name: Optional[str] = None
    start_time: Optional[str] = None
    end_time: Optional[str] = None
    days_of_week: Optional[List[int]] = None
    category_id: Optional[int] = None
    description: Optional[str] = None
    active: Optional[bool] = None


class DaypartResponse(BaseModel):
    id: int
    name: str
    start_time: str
    end_time: str
    days_of_week: Optional[List[int]]
    category_id: Optional[int]
    description: Optional[str]
    active: bool
    created_at: str
    updated_at: str
    category_name: Optional[str] = None

    class Config:
        from_attributes = True


def parse_time(time_str: str) -> time:
    """Parse time string to time object"""
    parts = time_str.split(':')
    return time(int(parts[0]), int(parts[1]), int(parts[2]) if len(parts) > 2 else 0)


@router.get("/", response_model=list[DaypartResponse])
async def list_dayparts(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    active_only: bool = Query(True),
    category_id: Optional[int] = None,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List all dayparts"""
    query = select(Daypart).options(selectinload(Daypart.category))
    
    if active_only:
        query = query.where(Daypart.active == True)
    
    if category_id:
        query = query.where(Daypart.category_id == category_id)
    
    query = query.offset(skip).limit(limit).order_by(Daypart.name)
    
    result = await db.execute(query)
    dayparts = result.scalars().all()
    
    # Convert time objects to strings and include category name
    dayparts_data = []
    for dp in dayparts:
        try:
            dp_dict = DaypartResponse.model_validate(dp).model_dump()
            dp_dict["start_time"] = str(dp.start_time)
            dp_dict["end_time"] = str(dp.end_time)
            # Safely get category name - handle case where category might not be loaded
            try:
                if hasattr(dp, 'category') and dp.category is not None:
                    dp_dict["category_name"] = getattr(dp.category, 'name', None)
                else:
                    dp_dict["category_name"] = None
            except Exception:
                # If accessing category fails, just set to None
                dp_dict["category_name"] = None
            dayparts_data.append(DaypartResponse(**dp_dict))
        except Exception as e:
            # Log error but continue processing other dayparts
            import structlog
            logger = structlog.get_logger()
            logger.error("Error processing daypart", daypart_id=getattr(dp, 'id', None), error=str(e))
            # Create a basic response without category
            try:
                dp_dict = {
                    "id": dp.id,
                    "name": dp.name,
                    "start_time": str(dp.start_time),
                    "end_time": str(dp.end_time),
                    "days_of_week": getattr(dp, 'days_of_week', None),
                    "category_id": getattr(dp, 'category_id', None),
                    "description": getattr(dp, 'description', None),
                    "active": getattr(dp, 'active', True),
                    "created_at": dp.created_at.isoformat() if hasattr(dp, 'created_at') and dp.created_at else "",
                    "updated_at": dp.updated_at.isoformat() if hasattr(dp, 'updated_at') and dp.updated_at else "",
                    "category_name": None
                }
                dayparts_data.append(DaypartResponse(**dp_dict))
            except Exception as e2:
                logger.error("Failed to create fallback daypart response", error=str(e2))
                # Skip this daypart if we can't even create a basic response
                continue
    
    return dayparts_data


@router.post("/", response_model=DaypartResponse, status_code=status.HTTP_201_CREATED)
async def create_daypart(
    daypart: DaypartCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a new daypart"""
    # Validate category if provided
    if daypart.category_id:
        from backend.models.daypart_category import DaypartCategory
        result = await db.execute(select(DaypartCategory).where(DaypartCategory.id == daypart.category_id))
        category = result.scalar_one_or_none()
        if not category:
            raise HTTPException(status_code=404, detail="Daypart category not found")
    
    daypart_data = daypart.model_dump()
    daypart_data["start_time"] = parse_time(daypart_data["start_time"])
    daypart_data["end_time"] = parse_time(daypart_data["end_time"])
    
    new_daypart = Daypart(**daypart_data)
    db.add(new_daypart)
    await db.commit()
    await db.refresh(new_daypart)
    await db.refresh(new_daypart, ["category"])
    
    dp_dict = DaypartResponse.model_validate(new_daypart).model_dump()
    dp_dict["start_time"] = str(new_daypart.start_time)
    dp_dict["end_time"] = str(new_daypart.end_time)
    # Safely get category name
    try:
        dp_dict["category_name"] = getattr(new_daypart.category, 'name', None) if new_daypart.category else None
    except Exception:
        dp_dict["category_name"] = None
    
    return DaypartResponse(**dp_dict)


@router.get("/{daypart_id}", response_model=DaypartResponse)
async def get_daypart(
    daypart_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get a specific daypart"""
    result = await db.execute(
        select(Daypart)
        .options(selectinload(Daypart.category))
        .where(Daypart.id == daypart_id)
    )
    daypart = result.scalar_one_or_none()
    
    if not daypart:
        raise HTTPException(status_code=404, detail="Daypart not found")
    
    dp_dict = DaypartResponse.model_validate(daypart).model_dump()
    dp_dict["start_time"] = str(daypart.start_time)
    dp_dict["end_time"] = str(daypart.end_time)
    # Safely get category name
    try:
        dp_dict["category_name"] = getattr(daypart.category, 'name', None) if daypart.category else None
    except Exception:
        dp_dict["category_name"] = None
    
    return DaypartResponse(**dp_dict)


@router.put("/{daypart_id}", response_model=DaypartResponse)
async def update_daypart(
    daypart_id: int,
    daypart_update: DaypartUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Update a daypart"""
    result = await db.execute(select(Daypart).where(Daypart.id == daypart_id))
    daypart = result.scalar_one_or_none()
    
    if not daypart:
        raise HTTPException(status_code=404, detail="Daypart not found")
    
    # Validate category if provided
    if daypart_update.category_id is not None:
        from backend.models.daypart_category import DaypartCategory
        result = await db.execute(select(DaypartCategory).where(DaypartCategory.id == daypart_update.category_id))
        category = result.scalar_one_or_none()
        if not category:
            raise HTTPException(status_code=404, detail="Daypart category not found")
    
    update_data = daypart_update.model_dump(exclude_unset=True)
    
    if "start_time" in update_data:
        update_data["start_time"] = parse_time(update_data["start_time"])
    if "end_time" in update_data:
        update_data["end_time"] = parse_time(update_data["end_time"])
    
    for field, value in update_data.items():
        setattr(daypart, field, value)
    
    await db.commit()
    await db.refresh(daypart)
    await db.refresh(daypart, ["category"])
    
    dp_dict = DaypartResponse.model_validate(daypart).model_dump()
    dp_dict["start_time"] = str(daypart.start_time)
    dp_dict["end_time"] = str(daypart.end_time)
    # Safely get category name
    try:
        dp_dict["category_name"] = getattr(daypart.category, 'name', None) if daypart.category else None
    except Exception:
        dp_dict["category_name"] = None
    
    return DaypartResponse(**dp_dict)


@router.delete("/{daypart_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_daypart(
    daypart_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Delete a daypart"""
    result = await db.execute(select(Daypart).where(Daypart.id == daypart_id))
    daypart = result.scalar_one_or_none()
    
    if not daypart:
        raise HTTPException(status_code=404, detail="Daypart not found")
    
    await db.delete(daypart)
    await db.commit()
    
    return None

