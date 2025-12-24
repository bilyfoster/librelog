"""
Sales Reps router for traffic management
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from uuid import UUID
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from sqlalchemy.orm import selectinload
from backend.database import get_db
from backend.models.sales_rep import SalesRep
from backend.models.sales_team import SalesTeam
from backend.models.sales_office import SalesOffice
from backend.models.sales_region import SalesRegion
from backend.models.user import User
from backend.routers.auth import get_current_user
from pydantic import BaseModel
from typing import Optional
from decimal import Decimal

router = APIRouter()


class SalesRepCreate(BaseModel):
    user_id: UUID
    employee_id: Optional[str] = None
    commission_rate: Optional[Decimal] = None
    sales_goal: Optional[Decimal] = None


class SalesRepUpdate(BaseModel):
    employee_id: Optional[str] = None
    commission_rate: Optional[Decimal] = None
    sales_goal: Optional[Decimal] = None
    active: Optional[bool] = None


class SalesRepResponse(BaseModel):
    id: UUID
    user_id: UUID
    employee_id: Optional[str]
    commission_rate: Optional[Decimal]
    sales_goal: Optional[Decimal]
    active: bool
    created_at: str
    updated_at: str
    username: Optional[str] = None  # From user relationship

    class Config:
        from_attributes = True


@router.get("/", response_model=list[SalesRepResponse])
async def list_sales_reps(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    active_only: bool = Query(True),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List all sales reps with optional filtering"""
    try:
        import structlog
        logger = structlog.get_logger()
        logger.info("Listing sales reps", skip=skip, limit=limit, active_only=active_only)
        
        query = select(SalesRep)
        
        if active_only:
            query = query.where(SalesRep.active == True)
        
        query = query.options(selectinload(SalesRep.user))
        query = query.offset(skip).limit(limit).order_by(SalesRep.id)
        
        result = await db.execute(query)
        sales_reps = result.scalars().all()
        
        logger.info("Found sales reps", count=len(sales_reps), active_only=active_only)
        
        # Load user data for each sales rep
        reps_data = []
        for rep in sales_reps:
            try:
                # Convert datetime fields to strings for Pydantic validation
                rep_dict = {
                    "id": rep.id,
                    "user_id": rep.user_id,
                    "employee_id": rep.employee_id,
                    "commission_rate": rep.commission_rate,
                    "sales_goal": rep.sales_goal,
                    "active": rep.active,
                    "created_at": rep.created_at.isoformat() if rep.created_at else None,
                    "updated_at": rep.updated_at.isoformat() if rep.updated_at else None,
                    "username": rep.user.username if rep.user else None,
                }
                reps_data.append(SalesRepResponse(**rep_dict))
            except Exception as e:
                import structlog
                logger = structlog.get_logger()
                logger.warning("Failed to serialize sales rep", sales_rep_id=rep.id, error=str(e), exc_info=True)
                # Continue with other reps even if one fails
                continue
        
        return reps_data
    except Exception as e:
        import structlog
        logger = structlog.get_logger()
        logger.error("Failed to list sales reps", error=str(e), exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to list sales reps: {str(e)}"
        )


@router.post("/", response_model=SalesRepResponse, status_code=status.HTTP_201_CREATED)
async def create_sales_rep(
    sales_rep: SalesRepCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a new sales rep"""
    try:
        # Check if user exists
        user_result = await db.execute(select(User).where(User.id == sales_rep.user_id))
        user = user_result.scalar_one_or_none()
        if not user:
            raise HTTPException(status_code=404, detail="User not found")
        
        # Check if sales rep already exists for this user
        existing = await db.execute(select(SalesRep).where(SalesRep.user_id == sales_rep.user_id))
        if existing.scalar_one_or_none():
            raise HTTPException(status_code=400, detail="Sales rep already exists for this user")
        
        new_sales_rep = SalesRep(**sales_rep.model_dump())
        db.add(new_sales_rep)
        await db.commit()
        await db.refresh(new_sales_rep)
        
        # Reload with user relationship
        result = await db.execute(
            select(SalesRep).options(selectinload(SalesRep.user)).where(SalesRep.id == new_sales_rep.id)
        )
        new_sales_rep = result.scalar_one()
        
        # Convert datetime fields to strings for Pydantic validation
        rep_dict = {
            "id": new_sales_rep.id,
            "user_id": new_sales_rep.user_id,
            "employee_id": new_sales_rep.employee_id,
            "commission_rate": new_sales_rep.commission_rate,
            "sales_goal": new_sales_rep.sales_goal,
            "active": new_sales_rep.active,
            "created_at": new_sales_rep.created_at.isoformat() if new_sales_rep.created_at else None,
            "updated_at": new_sales_rep.updated_at.isoformat() if new_sales_rep.updated_at else None,
            "username": new_sales_rep.user.username if new_sales_rep.user else None,
        }
        
        return SalesRepResponse(**rep_dict)
    except HTTPException:
        raise
    except Exception as e:
        import structlog
        logger = structlog.get_logger()
        logger.error("Failed to create sales rep", error=str(e), exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to create sales rep: {str(e)}"
        )


@router.get("/{sales_rep_id}", response_model=SalesRepResponse)
async def get_sales_rep(
    sales_rep_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get a specific sales rep"""
    result = await db.execute(
        select(SalesRep).options(selectinload(SalesRep.user)).where(SalesRep.id == sales_rep_id)
    )
    sales_rep = result.scalar_one_or_none()
    
    if not sales_rep:
        raise HTTPException(status_code=404, detail="Sales rep not found")
    
    rep_dict = SalesRepResponse.model_validate(sales_rep).model_dump()
    if sales_rep.user:
        rep_dict["username"] = sales_rep.user.username
    
    return SalesRepResponse(**rep_dict)


@router.put("/{sales_rep_id}", response_model=SalesRepResponse)
async def update_sales_rep(
    sales_rep_id: UUID,
    sales_rep_update: SalesRepUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Update a sales rep"""
    result = await db.execute(select(SalesRep).where(SalesRep.id == sales_rep_id))
    sales_rep = result.scalar_one_or_none()
    
    if not sales_rep:
        raise HTTPException(status_code=404, detail="Sales rep not found")
    
    # Update fields
    update_data = sales_rep_update.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(sales_rep, field, value)
    
    await db.commit()
    
    # Reload with user relationship
    result = await db.execute(
        select(SalesRep).options(selectinload(SalesRep.user)).where(SalesRep.id == sales_rep_id)
    )
    sales_rep = result.scalar_one()
    
    # Convert datetime fields to strings for Pydantic validation
    rep_dict = {
        "id": sales_rep.id,
        "user_id": sales_rep.user_id,
        "employee_id": sales_rep.employee_id,
        "commission_rate": sales_rep.commission_rate,
        "sales_goal": sales_rep.sales_goal,
        "active": sales_rep.active,
        "created_at": sales_rep.created_at.isoformat() if sales_rep.created_at else None,
        "updated_at": sales_rep.updated_at.isoformat() if sales_rep.updated_at else None,
        "username": sales_rep.user.username if sales_rep.user else None,
    }
    
    return SalesRepResponse(**rep_dict)


@router.delete("/{sales_rep_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_sales_rep(
    sales_rep_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Soft delete a sales rep (set active=False)"""
    result = await db.execute(select(SalesRep).where(SalesRep.id == sales_rep_id))
    sales_rep = result.scalar_one_or_none()
    
    if not sales_rep:
        raise HTTPException(status_code=404, detail="Sales rep not found")
    
    sales_rep.active = False
    await db.commit()
    
    return None


@router.get("/{sales_rep_id}/teams")
async def get_sales_rep_teams(
    sales_rep_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get teams for a sales rep"""
    result = await db.execute(
        select(SalesRep)
        .where(SalesRep.id == sales_rep_id)
        .options(selectinload(SalesRep.teams))
    )
    sales_rep = result.scalar_one_or_none()
    
    if not sales_rep:
        raise HTTPException(status_code=404, detail="Sales rep not found")
    
    return [{"id": team.id, "name": team.name} for team in sales_rep.teams]


@router.get("/{sales_rep_id}/offices")
async def get_sales_rep_offices(
    sales_rep_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get offices for a sales rep"""
    result = await db.execute(
        select(SalesRep)
        .where(SalesRep.id == sales_rep_id)
        .options(selectinload(SalesRep.offices))
    )
    sales_rep = result.scalar_one_or_none()
    
    if not sales_rep:
        raise HTTPException(status_code=404, detail="Sales rep not found")
    
    return [{"id": office.id, "name": office.name} for office in sales_rep.offices]


@router.get("/{sales_rep_id}/regions")
async def get_sales_rep_regions(
    sales_rep_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get regions for a sales rep"""
    result = await db.execute(
        select(SalesRep)
        .where(SalesRep.id == sales_rep_id)
        .options(selectinload(SalesRep.regions))
    )
    sales_rep = result.scalar_one_or_none()
    
    if not sales_rep:
        raise HTTPException(status_code=404, detail="Sales rep not found")
    
    return [{"id": region.id, "name": region.name} for region in sales_rep.regions]

