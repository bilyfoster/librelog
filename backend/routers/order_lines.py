"""
Order Lines router for line-level order management - WideOrbit-compatible
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from uuid import UUID
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from sqlalchemy.orm import selectinload
from backend.database import get_db
from backend.models.order_line import OrderLine
from backend.models.order import Order
from backend.routers.auth import get_current_user
from backend.models.user import User
from backend.schemas.order_line import OrderLineCreate, OrderLineUpdate, OrderLineResponse
from typing import List, Optional

router = APIRouter()


@router.get("/", response_model=List[OrderLineResponse])
async def list_order_lines(
    order_id: UUID = Query(..., description="Filter by order ID"),
    station_id: Optional[UUID] = Query(None, description="Filter by station ID"),
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List all order lines for a specific order"""
    # Verify order exists
    order_result = await db.execute(select(Order).where(Order.id == order_id))
    order = order_result.scalar_one_or_none()
    
    if not order:
        raise HTTPException(status_code=404, detail="Order not found")
    
    query = select(OrderLine).where(OrderLine.order_id == order_id)
    
    # Filter by station if provided
    if station_id is not None:
        query = query.where(OrderLine.station_id == station_id)
    
    query = query.offset(skip).limit(limit).order_by(OrderLine.line_number)
    result = await db.execute(query)
    lines = result.scalars().all()
    
    return [OrderLineResponse.model_validate(line) for line in lines]


@router.post("/", response_model=OrderLineResponse, status_code=status.HTTP_201_CREATED)
async def create_order_line(
    line: OrderLineCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a new order line"""
    # Verify order exists
    order_result = await db.execute(select(Order).where(Order.id == line.order_id))
    order = order_result.scalar_one_or_none()
    
    if not order:
        raise HTTPException(status_code=404, detail="Order not found")
    
    # Validate date range
    if line.start_date >= line.end_date:
        raise HTTPException(status_code=400, detail="Start date must be before end date")
    
    # Check if line number already exists for this order
    existing = await db.execute(
        select(OrderLine).where(
            OrderLine.order_id == line.order_id,
            OrderLine.line_number == line.line_number
        )
    )
    if existing.scalar_one_or_none():
        raise HTTPException(status_code=400, detail="Line number already exists for this order")
    
    # Create order line
    line_data = line.model_dump()
    
    # Ensure station_id is set (required)
    if "station_id" not in line_data or line_data["station_id"] is None:
        raise HTTPException(status_code=400, detail="station_id is required for order lines")
    
    # Verify station exists
    from backend.models.station import Station
    station_result = await db.execute(select(Station).where(Station.id == line_data["station_id"]))
    station = station_result.scalar_one_or_none()
    if not station:
        raise HTTPException(status_code=404, detail="Station not found")
    
    new_line = OrderLine(**line_data)
    
    db.add(new_line)
    await db.commit()
    await db.refresh(new_line)
    
    return OrderLineResponse.model_validate(new_line)


@router.get("/{line_id}", response_model=OrderLineResponse)
async def get_order_line(
    line_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get a specific order line"""
    result = await db.execute(select(OrderLine).where(OrderLine.id == line_id))
    line = result.scalar_one_or_none()
    
    if not line:
        raise HTTPException(status_code=404, detail="Order line not found")
    
    return OrderLineResponse.model_validate(line)


@router.put("/{line_id}", response_model=OrderLineResponse)
async def update_order_line(
    line_id: UUID,
    line_update: OrderLineUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Update an order line"""
    result = await db.execute(select(OrderLine).where(OrderLine.id == line_id))
    line = result.scalar_one_or_none()
    
    if not line:
        raise HTTPException(status_code=404, detail="Order line not found")
    
    # Validate date range if dates are being updated
    update_data = line_update.model_dump(exclude_unset=True)
    if "start_date" in update_data or "end_date" in update_data:
        start_date = update_data.get("start_date", line.start_date)
        end_date = update_data.get("end_date", line.end_date)
        if start_date >= end_date:
            raise HTTPException(status_code=400, detail="Start date must be before end date")
    
    # Check line number uniqueness if line_number is being updated
    if "line_number" in update_data and update_data["line_number"] != line.line_number:
        existing = await db.execute(
            select(OrderLine).where(
                OrderLine.order_id == line.order_id,
                OrderLine.line_number == update_data["line_number"]
            )
        )
        if existing.scalar_one_or_none():
            raise HTTPException(status_code=400, detail="Line number already exists for this order")
    
    # Update fields
    for field, value in update_data.items():
        setattr(line, field, value)
    
    await db.commit()
    await db.refresh(line)
    
    return OrderLineResponse.model_validate(line)


@router.delete("/{line_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_order_line(
    line_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Delete an order line"""
    result = await db.execute(select(OrderLine).where(OrderLine.id == line_id))
    line = result.scalar_one_or_none()
    
    if not line:
        raise HTTPException(status_code=404, detail="Order line not found")
    
    await db.delete(line)
    await db.commit()
    
    return None

