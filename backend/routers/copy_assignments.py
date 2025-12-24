"""
Copy Assignments router
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from uuid import UUID
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from backend.database import get_db
from backend.models.copy_assignment import CopyAssignment
from backend.routers.auth import get_current_user
from backend.models.user import User
from pydantic import BaseModel
from typing import Optional

router = APIRouter()


class CopyAssignmentResponse(BaseModel):
    id: UUID
    spot_id: UUID
    copy_id: UUID
    order_id: Optional[UUID]
    assigned_at: str
    assigned_by: Optional[int]
    created_at: str

    class Config:
        from_attributes = True


@router.get("/{assignment_id}", response_model=CopyAssignmentResponse)
async def get_copy_assignment(
    assignment_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get a specific copy assignment"""
    result = await db.execute(select(CopyAssignment).where(CopyAssignment.id == assignment_id))
    assignment = result.scalar_one_or_none()
    
    if not assignment:
        raise HTTPException(status_code=404, detail="Copy assignment not found")
    
    return CopyAssignmentResponse.model_validate(assignment)


@router.get("/", response_model=list[CopyAssignmentResponse])
async def list_copy_assignments(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    spot_id: Optional[UUID] = Query(None),
    copy_id: Optional[UUID] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List all copy assignments"""
    query = select(CopyAssignment)
    
    if spot_id:
        query = query.where(CopyAssignment.spot_id == spot_id)
    
    if copy_id:
        query = query.where(CopyAssignment.copy_id == copy_id)
    
    query = query.offset(skip).limit(limit).order_by(CopyAssignment.assigned_at.desc())
    
    result = await db.execute(query)
    assignments = result.scalars().all()
    
    return [CopyAssignmentResponse.model_validate(a) for a in assignments]


@router.post("/", response_model=CopyAssignmentResponse, status_code=status.HTTP_201_CREATED)
async def create_copy_assignment(
    spot_id: UUID,
    copy_id: UUID,
    order_id: Optional[UUID] = None,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a copy assignment"""
    # Check if assignment already exists
    existing = await db.execute(
        select(CopyAssignment).where(
            CopyAssignment.spot_id == spot_id,
            CopyAssignment.copy_id == copy_id
        )
    )
    if existing.scalar_one_or_none():
        raise HTTPException(status_code=400, detail="Assignment already exists")
    
    assignment = CopyAssignment(
        spot_id=spot_id,
        copy_id=copy_id,
        order_id=order_id,
        assigned_by=current_user.id
    )
    db.add(assignment)
    await db.commit()
    await db.refresh(assignment)
    
    return CopyAssignmentResponse.model_validate(assignment)


@router.put("/{assignment_id}", response_model=CopyAssignmentResponse)
async def update_copy_assignment(
    assignment_id: UUID,
    copy_id: Optional[UUID] = None,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Update a copy assignment"""
    result = await db.execute(select(CopyAssignment).where(CopyAssignment.id == assignment_id))
    assignment = result.scalar_one_or_none()
    
    if not assignment:
        raise HTTPException(status_code=404, detail="Assignment not found")
    
    if copy_id:
        assignment.copy_id = copy_id
    
    await db.commit()
    await db.refresh(assignment)
    
    return CopyAssignmentResponse.model_validate(assignment)


@router.delete("/{assignment_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_copy_assignment(
    assignment_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Remove a copy assignment"""
    result = await db.execute(select(CopyAssignment).where(CopyAssignment.id == assignment_id))
    assignment = result.scalar_one_or_none()
    
    if not assignment:
        raise HTTPException(status_code=404, detail="Assignment not found")
    
    await db.delete(assignment)
    await db.commit()
    
    return None

