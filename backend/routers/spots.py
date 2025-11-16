"""
Spots router for traffic scheduling
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_
from backend.database import get_db
from backend.models.spot import Spot, SpotStatus, BreakPosition, Daypart as SpotDaypart
from backend.models.order import Order
from backend.routers.auth import get_current_user
from backend.models.user import User
from pydantic import BaseModel
from typing import Optional, List
from datetime import date, datetime, time
from decimal import Decimal

router = APIRouter()


class SpotCreate(BaseModel):
    order_id: int
    campaign_id: Optional[int] = None
    scheduled_date: date
    scheduled_time: str  # HH:MM:SS format
    spot_length: int
    break_position: Optional[str] = None
    daypart: Optional[str] = None
    status: str = "SCHEDULED"


class SpotUpdate(BaseModel):
    scheduled_date: Optional[date] = None
    scheduled_time: Optional[str] = None
    spot_length: Optional[int] = None
    break_position: Optional[str] = None
    daypart: Optional[str] = None
    status: Optional[str] = None
    conflict_resolved: Optional[bool] = None


class SpotResponse(BaseModel):
    id: int
    order_id: int
    campaign_id: Optional[int]
    scheduled_date: date
    scheduled_time: str
    spot_length: int
    break_position: Optional[str]
    daypart: Optional[str]
    status: str
    actual_air_time: Optional[str]
    makegood_of_id: Optional[int]
    conflict_resolved: bool
    created_at: str
    updated_at: str

    class Config:
        from_attributes = True


def spot_to_response_dict(spot: Spot) -> dict:
    """Convert Spot model to SpotResponse dict with proper datetime serialization"""
    return {
        "id": spot.id,
        "order_id": spot.order_id,
        "campaign_id": spot.campaign_id,
        "scheduled_date": spot.scheduled_date,
        "scheduled_time": spot.scheduled_time,
        "spot_length": spot.spot_length,
        "break_position": spot.break_position.value if spot.break_position else None,
        "daypart": spot.daypart.value if spot.daypart else None,
        "status": spot.status.value if spot.status else None,
        "actual_air_time": spot.actual_air_time,
        "makegood_of_id": spot.makegood_of_id,
        "conflict_resolved": spot.conflict_resolved,
        "created_at": spot.created_at.isoformat() if spot.created_at else None,
        "updated_at": spot.updated_at.isoformat() if spot.updated_at else None
    }


@router.get("/", response_model=list[SpotResponse])
async def list_spots(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    order_id: Optional[int] = Query(None),
    scheduled_date: Optional[date] = Query(None),
    status_filter: Optional[str] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List all spots with optional filtering"""
    query = select(Spot)
    
    if order_id:
        query = query.where(Spot.order_id == order_id)
    
    if scheduled_date:
        query = query.where(Spot.scheduled_date == scheduled_date)
    
    if status_filter:
        try:
            status_enum = SpotStatus[status_filter.upper()]
            query = query.where(Spot.status == status_enum)
        except KeyError:
            pass
    
    query = query.offset(skip).limit(limit).order_by(Spot.scheduled_date, Spot.scheduled_time)
    
    result = await db.execute(query)
    spots = result.scalars().all()
    
    return [SpotResponse(**spot_to_response_dict(spot)) for spot in spots]


@router.post("/", response_model=SpotResponse, status_code=status.HTTP_201_CREATED)
async def create_spot(
    spot: SpotCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a single spot"""
    # Check if order exists
    order_result = await db.execute(select(Order).where(Order.id == spot.order_id))
    if not order_result.scalar_one_or_none():
        raise HTTPException(status_code=404, detail="Order not found")
    
    spot_data = spot.model_dump()
    spot_data["status"] = SpotStatus[spot_data["status"]]
    if spot_data.get("break_position"):
        spot_data["break_position"] = BreakPosition[spot_data["break_position"]]
    if spot_data.get("daypart"):
        spot_data["daypart"] = SpotDaypart[spot_data["daypart"]]
    
    new_spot = Spot(**spot_data)
    db.add(new_spot)
    await db.commit()
    await db.refresh(new_spot)
    
    return SpotResponse(**spot_to_response_dict(new_spot))


@router.post("/bulk", response_model=list[SpotResponse], status_code=status.HTTP_201_CREATED)
async def create_spots_bulk(
    order_id: int,
    spots: List[SpotCreate],
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Bulk create spots from an order"""
    # Check if order exists
    order_result = await db.execute(select(Order).where(Order.id == order_id))
    order = order_result.scalar_one_or_none()
    if not order:
        raise HTTPException(status_code=404, detail="Order not found")
    
    created_spots = []
    for spot_data in spots:
        spot_dict = spot_data.model_dump()
        spot_dict["order_id"] = order_id
        spot_dict["status"] = SpotStatus[spot_dict["status"]]
        if spot_dict.get("break_position"):
            spot_dict["break_position"] = BreakPosition[spot_dict["break_position"]]
        if spot_dict.get("daypart"):
            spot_dict["daypart"] = SpotDaypart[spot_dict["daypart"]]
        
        new_spot = Spot(**spot_dict)
        db.add(new_spot)
        created_spots.append(new_spot)
    
    await db.commit()
    
    for spot in created_spots:
        await db.refresh(spot)
    
    return [SpotResponse(**spot_to_response_dict(spot)) for spot in created_spots]


@router.put("/{spot_id}", response_model=SpotResponse)
async def update_spot(
    spot_id: int,
    spot_update: SpotUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Update a spot (for drag-drop functionality)"""
    result = await db.execute(select(Spot).where(Spot.id == spot_id))
    spot = result.scalar_one_or_none()
    
    if not spot:
        raise HTTPException(status_code=404, detail="Spot not found")
    
    # Update fields
    update_data = spot_update.model_dump(exclude_unset=True)
    
    if "status" in update_data:
        update_data["status"] = SpotStatus[update_data["status"]]
    if "break_position" in update_data and update_data["break_position"]:
        update_data["break_position"] = BreakPosition[update_data["break_position"]]
    if "daypart" in update_data and update_data["daypart"]:
        update_data["daypart"] = SpotDaypart[update_data["daypart"]]
    
    for field, value in update_data.items():
        setattr(spot, field, value)
    
    await db.commit()
    await db.refresh(spot)
    
    return SpotResponse(**spot_to_response_dict(spot))


@router.delete("/{spot_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_spot(
    spot_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Delete a spot"""
    result = await db.execute(select(Spot).where(Spot.id == spot_id))
    spot = result.scalar_one_or_none()
    
    if not spot:
        raise HTTPException(status_code=404, detail="Spot not found")
    
    await db.delete(spot)
    await db.commit()
    
    return None


@router.post("/{spot_id}/resolve-conflict", response_model=SpotResponse)
async def resolve_spot_conflict(
    spot_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Mark a spot conflict as resolved"""
    result = await db.execute(select(Spot).where(Spot.id == spot_id))
    spot = result.scalar_one_or_none()
    
    if not spot:
        raise HTTPException(status_code=404, detail="Spot not found")
    
    spot.conflict_resolved = True
    await db.commit()
    await db.refresh(spot)
    
    return SpotResponse(**spot_to_response_dict(spot))

