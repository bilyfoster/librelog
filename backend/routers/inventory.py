"""
Inventory router for inventory management
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_
from backend.database import get_db
from backend.models.inventory_slot import InventorySlot
from backend.routers.auth import get_current_user
from backend.models.user import User
from backend.services.inventory_service import InventoryService
from pydantic import BaseModel
from typing import Optional
from datetime import date

router = APIRouter()


class InventorySlotResponse(BaseModel):
    id: int
    date: date
    hour: int
    break_position: Optional[str]
    daypart: Optional[str]
    available: int
    booked: int
    sold_out: bool
    revenue: float
    created_at: str
    updated_at: str

    class Config:
        from_attributes = True


@router.get("/")
async def get_inventory(
    start_date: date = Query(...),
    end_date: date = Query(...),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get inventory by date range"""
    inventory_service = InventoryService(db)
    return await inventory_service.get_inventory_by_date_range(start_date, end_date)


@router.get("/heatmap")
async def get_inventory_heatmap(
    start_date: date = Query(...),
    end_date: date = Query(...),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get inventory heatmap data"""
    inventory_service = InventoryService(db)
    return await inventory_service.generate_heatmap(start_date, end_date)


@router.get("/sellout")
async def get_sellout_percentages(
    start_date: date = Query(...),
    end_date: date = Query(...),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get sellout percentages"""
    inventory_service = InventoryService(db)
    return await inventory_service.calculate_sellout(start_date, end_date)

