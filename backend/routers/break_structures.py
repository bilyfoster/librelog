"""
Break Structures router for traffic scheduling
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from uuid import UUID
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from backend.database import get_db
from backend.models.break_structure import BreakStructure
from backend.routers.auth import get_current_user
from backend.models.user import User
from pydantic import BaseModel
from typing import Optional, List

router = APIRouter()


class BreakStructureCreate(BaseModel):
    name: str
    hour: int  # 0-23
    break_positions: Optional[List[int]] = None  # Array of seconds from hour start
    active: bool = True


class BreakStructureUpdate(BaseModel):
    name: Optional[str] = None
    hour: Optional[int] = None
    break_positions: Optional[List[int]] = None
    active: Optional[bool] = None


class BreakStructureResponse(BaseModel):
    id: UUID
    name: str
    hour: int
    break_positions: Optional[List[int]]
    active: bool
    created_at: str
    updated_at: str

    class Config:
        from_attributes = True


@router.get("/", response_model=list[BreakStructureResponse])
async def list_break_structures(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    active_only: bool = Query(True),
    hour_filter: Optional[int] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List all break structures"""
    query = select(BreakStructure)
    
    if active_only:
        query = query.where(BreakStructure.active == True)
    
    if hour_filter is not None:
        query = query.where(BreakStructure.hour == hour_filter)
    
    query = query.offset(skip).limit(limit).order_by(BreakStructure.hour, BreakStructure.name)
    
    result = await db.execute(query)
    break_structures = result.scalars().all()
    
    return [BreakStructureResponse.model_validate(bs) for bs in break_structures]


@router.post("/", response_model=BreakStructureResponse, status_code=status.HTTP_201_CREATED)
async def create_break_structure(
    break_structure: BreakStructureCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a new break structure"""
    if not (0 <= break_structure.hour <= 23):
        raise HTTPException(status_code=400, detail="Hour must be between 0 and 23")
    
    new_break_structure = BreakStructure(**break_structure.model_dump())
    db.add(new_break_structure)
    await db.commit()
    await db.refresh(new_break_structure)
    
    return BreakStructureResponse.model_validate(new_break_structure)


@router.get("/{break_structure_id}", response_model=BreakStructureResponse)
async def get_break_structure(
    break_structure_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get a specific break structure"""
    result = await db.execute(select(BreakStructure).where(BreakStructure.id == break_structure_id))
    break_structure = result.scalar_one_or_none()
    
    if not break_structure:
        raise HTTPException(status_code=404, detail="Break structure not found")
    
    return BreakStructureResponse.model_validate(break_structure)


@router.put("/{break_structure_id}", response_model=BreakStructureResponse)
async def update_break_structure(
    break_structure_id: UUID,
    break_structure_update: BreakStructureUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Update a break structure"""
    result = await db.execute(select(BreakStructure).where(BreakStructure.id == break_structure_id))
    break_structure = result.scalar_one_or_none()
    
    if not break_structure:
        raise HTTPException(status_code=404, detail="Break structure not found")
    
    update_data = break_structure_update.model_dump(exclude_unset=True)
    
    if "hour" in update_data and not (0 <= update_data["hour"] <= 23):
        raise HTTPException(status_code=400, detail="Hour must be between 0 and 23")
    
    for field, value in update_data.items():
        setattr(break_structure, field, value)
    
    await db.commit()
    await db.refresh(break_structure)
    
    return BreakStructureResponse.model_validate(break_structure)


@router.delete("/{break_structure_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_break_structure(
    break_structure_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Delete a break structure"""
    result = await db.execute(select(BreakStructure).where(BreakStructure.id == break_structure_id))
    break_structure = result.scalar_one_or_none()
    
    if not break_structure:
        raise HTTPException(status_code=404, detail="Break structure not found")
    
    await db.delete(break_structure)
    await db.commit()
    
    return None

