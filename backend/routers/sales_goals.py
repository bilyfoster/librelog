"""
Sales Goals router
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from uuid import UUID
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from backend.database import get_db
from backend.models.sales_goal import SalesGoal, PeriodType
from backend.routers.auth import get_current_user
from backend.models.user import User
from pydantic import BaseModel
from typing import Optional
from datetime import date
from decimal import Decimal

router = APIRouter()


class SalesGoalCreate(BaseModel):
    sales_rep_id: UUID
    period: str
    target_date: date
    goal_amount: Decimal


class SalesGoalUpdate(BaseModel):
    goal_amount: Optional[Decimal] = None
    actual_amount: Optional[Decimal] = None


class SalesGoalResponse(BaseModel):
    id: UUID
    sales_rep_id: UUID
    period: str
    target_date: date
    goal_amount: Decimal
    actual_amount: Decimal
    created_at: str
    updated_at: str

    class Config:
        from_attributes = True


@router.get("/", response_model=list[SalesGoalResponse])
async def list_sales_goals(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    sales_rep_id: Optional[UUID] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List all sales goals"""
    query = select(SalesGoal)
    
    if sales_rep_id:
        query = query.where(SalesGoal.sales_rep_id == sales_rep_id)
    
    query = query.offset(skip).limit(limit).order_by(SalesGoal.target_date.desc())
    
    result = await db.execute(query)
    goals = result.scalars().all()
    
    return [SalesGoalResponse.model_validate(g) for g in goals]


@router.post("/", response_model=SalesGoalResponse, status_code=status.HTTP_201_CREATED)
async def create_sales_goal(
    goal: SalesGoalCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a new sales goal"""
    goal_data = goal.model_dump()
    goal_data["period"] = PeriodType[goal_data["period"]]
    
    new_goal = SalesGoal(**goal_data)
    db.add(new_goal)
    await db.commit()
    await db.refresh(new_goal)
    
    return SalesGoalResponse.model_validate(new_goal)


@router.get("/progress")
async def get_sales_goals_progress(
    sales_rep_id: Optional[UUID] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get sales goals progress tracking"""
    from backend.services.revenue_service import RevenueService
    
    revenue_service = RevenueService(db)
    return await revenue_service.calculate_pacing_vs_goals(sales_rep_id)

