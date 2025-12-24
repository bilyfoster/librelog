"""
Production Assignments router for managing production assignments
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from uuid import UUID
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_
from backend.database import get_db
from backend.models.production_assignment import ProductionAssignment, AssignmentType, AssignmentStatus
from backend.routers.auth import get_current_user
from backend.models.user import User
from pydantic import BaseModel
from typing import Optional, List
from datetime import datetime

router = APIRouter()


class ProductionAssignmentCreate(BaseModel):
    production_order_id: UUID
    user_id: UUID
    assignment_type: str
    notes: Optional[str] = None


class ProductionAssignmentUpdate(BaseModel):
    status: Optional[str] = None
    notes: Optional[str] = None


class ProductionAssignmentResponse(BaseModel):
    id: UUID
    production_order_id: UUID
    user_id: UUID
    assignment_type: str
    status: str
    notes: Optional[str]
    assigned_at: str
    accepted_at: Optional[str]
    completed_at: Optional[str]
    created_at: str
    updated_at: str

    class Config:
        from_attributes = True


def assignment_to_response(assignment: ProductionAssignment) -> dict:
    """Convert ProductionAssignment to response dict"""
    return {
        "id": assignment.id,
        "production_order_id": assignment.production_order_id,
        "user_id": assignment.user_id,
        "assignment_type": assignment.assignment_type.value if assignment.assignment_type else None,
        "status": assignment.status.value if assignment.status else None,
        "notes": assignment.notes,
        "assigned_at": assignment.assigned_at.isoformat() if assignment.assigned_at else None,
        "accepted_at": assignment.accepted_at.isoformat() if assignment.accepted_at else None,
        "completed_at": assignment.completed_at.isoformat() if assignment.completed_at else None,
        "created_at": assignment.created_at.isoformat() if assignment.created_at else None,
        "updated_at": assignment.updated_at.isoformat() if assignment.updated_at else None,
    }


@router.get("/", response_model=List[ProductionAssignmentResponse])
async def list_assignments(
    production_order_id: Optional[UUID] = Query(None),
    user_id: Optional[UUID] = Query(None),
    assignment_type: Optional[str] = Query(None),
    status: Optional[str] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List production assignments with filters"""
    query = select(ProductionAssignment)
    
    if production_order_id:
        query = query.where(ProductionAssignment.production_order_id == production_order_id)
    
    if user_id:
        query = query.where(ProductionAssignment.user_id == user_id)
    
    if assignment_type:
        try:
            assignment_type_enum = AssignmentType(assignment_type)
            query = query.where(ProductionAssignment.assignment_type == assignment_type_enum)
        except ValueError:
            raise HTTPException(status_code=400, detail="Invalid assignment_type")
    
    if status:
        try:
            status_enum = AssignmentStatus(status)
            query = query.where(ProductionAssignment.status == status_enum)
        except ValueError:
            raise HTTPException(status_code=400, detail="Invalid status")
    
    # If user is not admin, only show their assignments
    if current_user.role != "admin":
        query = query.where(ProductionAssignment.user_id == current_user.id)
    
    query = query.order_by(ProductionAssignment.assigned_at.desc())
    
    result = await db.execute(query)
    assignments = result.scalars().all()
    
    return [assignment_to_response(a) for a in assignments]


@router.post("/", response_model=ProductionAssignmentResponse, status_code=status.HTTP_201_CREATED)
async def create_assignment(
    data: ProductionAssignmentCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a production assignment"""
    try:
        assignment_type_enum = AssignmentType(data.assignment_type)
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid assignment_type")
    
    # Check if assignment already exists
    result = await db.execute(
        select(ProductionAssignment).where(
            and_(
                ProductionAssignment.production_order_id == data.production_order_id,
                ProductionAssignment.assignment_type == assignment_type_enum
            )
        )
    )
    existing = result.scalar_one_or_none()
    
    if existing:
        raise HTTPException(status_code=400, detail="Assignment already exists")
    
    assignment = ProductionAssignment(
        production_order_id=data.production_order_id,
        user_id=data.user_id,
        assignment_type=assignment_type_enum,
        status=AssignmentStatus.PENDING,
        notes=data.notes
    )
    
    db.add(assignment)
    await db.commit()
    await db.refresh(assignment)
    
    return assignment_to_response(assignment)


@router.put("/{assignment_id}", response_model=ProductionAssignmentResponse)
async def update_assignment(
    assignment_id: UUID,
    data: ProductionAssignmentUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Update production assignment"""
    result = await db.execute(
        select(ProductionAssignment).where(ProductionAssignment.id == assignment_id)
    )
    assignment = result.scalar_one_or_none()
    
    if not assignment:
        raise HTTPException(status_code=404, detail="Assignment not found")
    
    # Check permissions
    if current_user.role != "admin" and assignment.user_id != current_user.id:
        raise HTTPException(status_code=403, detail="Access denied")
    
    if data.status:
        try:
            status_enum = AssignmentStatus(data.status)
            assignment.status = status_enum
            
            # Update timestamps based on status
            if status_enum == AssignmentStatus.ACCEPTED and not assignment.accepted_at:
                assignment.accepted_at = datetime.now()
            elif status_enum == AssignmentStatus.COMPLETED and not assignment.completed_at:
                assignment.completed_at = datetime.now()
        except ValueError:
            raise HTTPException(status_code=400, detail="Invalid status")
    
    if data.notes is not None:
        assignment.notes = data.notes
    
    await db.commit()
    await db.refresh(assignment)
    
    return assignment_to_response(assignment)


@router.get("/{assignment_id}", response_model=ProductionAssignmentResponse)
async def get_assignment(
    assignment_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get production assignment by ID"""
    result = await db.execute(
        select(ProductionAssignment).where(ProductionAssignment.id == assignment_id)
    )
    assignment = result.scalar_one_or_none()
    
    if not assignment:
        raise HTTPException(status_code=404, detail="Assignment not found")
    
    # Check permissions
    if current_user.role != "admin" and assignment.user_id != current_user.id:
        raise HTTPException(status_code=403, detail="Access denied")
    
    return assignment_to_response(assignment)


@router.delete("/{assignment_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_assignment(
    assignment_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Delete a production assignment"""
    result = await db.execute(
        select(ProductionAssignment).where(ProductionAssignment.id == assignment_id)
    )
    assignment = result.scalar_one_or_none()
    
    if not assignment:
        raise HTTPException(status_code=404, detail="Assignment not found")
    
    # Check permissions - only admins can delete assignments
    if current_user.role != "admin":
        raise HTTPException(status_code=403, detail="Only admins can delete assignments")
    
    await db.delete(assignment)
    await db.commit()
    
    return None

