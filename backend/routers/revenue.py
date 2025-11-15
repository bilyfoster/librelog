"""
Revenue router for revenue management
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.ext.asyncio import AsyncSession
from backend.database import get_db
from backend.routers.auth import get_current_user
from backend.models.user import User
from backend.services.revenue_service import RevenueService
from datetime import date

router = APIRouter()


@router.get("/summary")
async def get_revenue_summary(
    start_date: date = Query(...),
    end_date: date = Query(...),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get revenue summary"""
    revenue_service = RevenueService(db)
    return await revenue_service.calculate_revenue(start_date, end_date)


@router.get("/pacing")
async def get_revenue_pacing(
    start_date: date = Query(...),
    end_date: date = Query(...),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get revenue pacing vs goals"""
    revenue_service = RevenueService(db)
    return await revenue_service.calculate_pacing(start_date, end_date)


@router.get("/forecast")
async def get_revenue_forecast(
    months_ahead: int = Query(3, ge=1, le=12),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get revenue forecast"""
    revenue_service = RevenueService(db)
    return await revenue_service.generate_forecast(months_ahead)

