"""
Orders router for traffic management
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from sqlalchemy.orm import selectinload
from backend.database import get_db
from backend.models.order import Order, OrderStatus, RateType, ApprovalStatus
from backend.models.order_template import OrderTemplate
from backend.models.advertiser import Advertiser
from backend.models.agency import Agency
from backend.models.sales_rep import SalesRep
from backend.routers.auth import get_current_user
from backend.models.user import User
from backend.services.order_service import OrderService
from pydantic import BaseModel
from typing import Optional, List, Dict, Any
from datetime import date, datetime
from decimal import Decimal
import re

router = APIRouter()


async def generate_next_order_number(db: AsyncSession) -> str:
    """Generate the next auto-incrementing order number in format YYYYMMDD-XXXX
    
    Format ensures leading zeros for month and day:
    - YYYY = 4-digit year (e.g., 2024)
    - MM = 2-digit month with leading zero (e.g., 01, 02, ..., 12)
    - DD = 2-digit day with leading zero (e.g., 01, 02, ..., 31)
    - XXXX = 4-digit sequential number (e.g., 0001, 0002, ...)
    """
    current_date = datetime.now()
    # %Y%m%d ensures leading zeros: 20240105 (Jan 5), 20241215 (Dec 15)
    date_prefix = current_date.strftime("%Y%m%d")
    prefix = f"{date_prefix}-"
    
    # Find the highest order number for the current date
    # Query orders that start with the date prefix
    result = await db.execute(
        select(Order.order_number)
        .where(Order.order_number.like(f"{prefix}%"))
        .order_by(Order.order_number.desc())
    )
    
    highest_order = result.scalar_one_or_none()
    
    if highest_order:
        # Extract the number part after the prefix
        # Format: YYYYMMDD-XXXX
        match = re.search(rf"{re.escape(prefix)}(\d+)", highest_order)
        if match:
            next_number = int(match.group(1)) + 1
        else:
            # If format doesn't match, start from 0001
            next_number = 1
    else:
        # No orders for this date yet, start from 0001
        next_number = 1
    
    return f"{prefix}{next_number:04d}"


def order_to_response_dict(order: Order) -> dict:
    """Convert Order model to OrderResponse dict with proper datetime serialization"""
    return {
        "id": order.id,
        "order_number": order.order_number,
        "campaign_id": order.campaign_id,
        "advertiser_id": order.advertiser_id,
        "agency_id": order.agency_id,
        "sales_rep_id": order.sales_rep_id,
        "start_date": order.start_date,
        "end_date": order.end_date,
        "spot_lengths": order.spot_lengths,
        "total_spots": order.total_spots,
        "rate_type": order.rate_type.value if order.rate_type else None,
        "rates": order.rates,
        "total_value": order.total_value,
        "status": order.status.value if order.status else None,
        "approval_status": order.approval_status.value if order.approval_status else None,
        "approved_by": order.approved_by,
        "approved_at": order.approved_at.isoformat() if order.approved_at else None,
        "created_at": order.created_at.isoformat() if order.created_at else None,
        "updated_at": order.updated_at.isoformat() if order.updated_at else None,
        "advertiser_name": order.advertiser.name if order.advertiser else None,
        "agency_name": order.agency.name if order.agency else None,
    }


class OrderCreate(BaseModel):
    order_number: Optional[str] = None  # Auto-generated if not provided
    campaign_id: Optional[int] = None
    advertiser_id: int
    agency_id: Optional[int] = None
    sales_rep_id: Optional[int] = None
    start_date: date
    end_date: date
    spot_lengths: Optional[List[int]] = None
    total_spots: int = 0
    rate_type: str = "ROS"
    rates: Optional[Dict[str, Any]] = None
    total_value: Decimal = Decimal("0.00")
    status: str = "DRAFT"
    approval_status: str = "NOT_REQUIRED"


class OrderUpdate(BaseModel):
    campaign_id: Optional[int] = None
    advertiser_id: Optional[int] = None
    agency_id: Optional[int] = None
    sales_rep_id: Optional[int] = None
    start_date: Optional[date] = None
    end_date: Optional[date] = None
    spot_lengths: Optional[List[int]] = None
    total_spots: Optional[int] = None
    rate_type: Optional[str] = None
    rates: Optional[Dict[str, Any]] = None
    total_value: Optional[Decimal] = None
    status: Optional[str] = None
    approval_status: Optional[str] = None


class OrderResponse(BaseModel):
    id: int
    order_number: str
    campaign_id: Optional[int]
    advertiser_id: int
    agency_id: Optional[int]
    sales_rep_id: Optional[int]
    start_date: date
    end_date: date
    spot_lengths: Optional[List[int]]
    total_spots: int
    rate_type: str
    rates: Optional[Dict[str, Any]]
    total_value: Decimal
    status: str
    approval_status: str
    approved_by: Optional[int]
    approved_at: Optional[str]
    created_at: str
    updated_at: str
    advertiser_name: Optional[str] = None
    agency_name: Optional[str] = None

    class Config:
        from_attributes = True


class OrderTemplateCreate(BaseModel):
    name: str
    description: Optional[str] = None
    default_spot_lengths: Optional[List[int]] = None
    default_rate_type: Optional[str] = None
    default_rates: Optional[Dict[str, Any]] = None


class OrderTemplateResponse(BaseModel):
    id: int
    name: str
    description: Optional[str]
    default_spot_lengths: Optional[List[int]]
    default_rate_type: Optional[str]
    default_rates: Optional[Dict[str, Any]]
    created_by: int
    created_at: str
    updated_at: str

    class Config:
        from_attributes = True


@router.get("/", response_model=list[OrderResponse])
async def list_orders(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    advertiser_id: Optional[int] = Query(None),
    status_filter: Optional[str] = Query(None),
    start_date: Optional[date] = Query(None),
    end_date: Optional[date] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List all orders with optional filtering"""
    query = select(Order)
    
    if advertiser_id:
        query = query.where(Order.advertiser_id == advertiser_id)
    
    if status_filter:
        try:
            status_enum = OrderStatus[status_filter.upper()]
            query = query.where(Order.status == status_enum)
        except KeyError:
            pass
    
    if start_date:
        query = query.where(Order.start_date >= start_date)
    
    if end_date:
        query = query.where(Order.end_date <= end_date)
    
    query = query.options(
        selectinload(Order.advertiser),
        selectinload(Order.agency)
    ).offset(skip).limit(limit).order_by(Order.created_at.desc())
    
    result = await db.execute(query)
    orders = result.scalars().all()
    
    # Load related data
    orders_data = []
    for order in orders:
        order_dict = order_to_response_dict(order)
        orders_data.append(OrderResponse(**order_dict))
    
    return orders_data


@router.post("/", response_model=OrderResponse, status_code=status.HTTP_201_CREATED)
async def create_order(
    order: OrderCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a new order"""
    order_service = OrderService(db)
    
    # Validate date range
    if not order_service.validate_date_range(order.start_date, order.end_date):
        raise HTTPException(status_code=400, detail="Start date must be before end date")
    
    # Check if advertiser exists
    adv_result = await db.execute(select(Advertiser).where(Advertiser.id == order.advertiser_id))
    if not adv_result.scalar_one_or_none():
        raise HTTPException(status_code=404, detail="Advertiser not found")
    
    # Auto-generate order number if not provided
    order_number = order.order_number
    # Treat empty strings as None for auto-generation
    if not order_number or (isinstance(order_number, str) and order_number.strip() == ''):
        order_number = await generate_next_order_number(db)
    
    # Check if order number is unique
    existing = await db.execute(select(Order).where(Order.order_number == order_number))
    if existing.scalar_one_or_none():
        raise HTTPException(status_code=400, detail="Order number already exists")
    
    # Create order
    order_data = order.model_dump()
    order_data["order_number"] = order_number
    order_data["rate_type"] = RateType[order_data["rate_type"]]
    order_data["status"] = OrderStatus[order_data["status"]]
    order_data["approval_status"] = ApprovalStatus[order_data["approval_status"]]
    
    new_order = Order(**order_data)
    
    # Calculate total value if not provided
    if new_order.total_value == 0:
        new_order.total_value = order_service.calculate_total_value(new_order)
    
    db.add(new_order)
    await db.commit()
    await db.refresh(new_order)
    
    order_dict = order_to_response_dict(new_order)
    return OrderResponse(**order_dict)


@router.get("/{order_id}", response_model=OrderResponse)
async def get_order(
    order_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get a specific order with full details"""
    query = select(Order).where(Order.id == order_id).options(
        selectinload(Order.advertiser),
        selectinload(Order.agency)
    )
    result = await db.execute(query)
    order = result.scalar_one_or_none()
    
    if not order:
        raise HTTPException(status_code=404, detail="Order not found")
    
    order_dict = order_to_response_dict(order)
    return OrderResponse(**order_dict)


@router.put("/{order_id}", response_model=OrderResponse)
async def update_order(
    order_id: int,
    order_update: OrderUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Update an order"""
    result = await db.execute(select(Order).where(Order.id == order_id))
    order = result.scalar_one_or_none()
    
    if not order:
        raise HTTPException(status_code=404, detail="Order not found")
    
    order_service = OrderService(db)
    
    # Update fields
    update_data = order_update.model_dump(exclude_unset=True)
    
    # Handle enum conversions
    if "rate_type" in update_data:
        update_data["rate_type"] = RateType[update_data["rate_type"]]
    if "status" in update_data:
        update_data["status"] = OrderStatus[update_data["status"]]
    if "approval_status" in update_data:
        update_data["approval_status"] = ApprovalStatus[update_data["approval_status"]]
    
    # Validate date range if dates are being updated
    if "start_date" in update_data or "end_date" in update_data:
        start_date = update_data.get("start_date", order.start_date)
        end_date = update_data.get("end_date", order.end_date)
        if not order_service.validate_date_range(start_date, end_date):
            raise HTTPException(status_code=400, detail="Start date must be before end date")
    
    for field, value in update_data.items():
        setattr(order, field, value)
    
    # Recalculate total value if rates changed
    if "rates" in update_data or "rate_type" in update_data:
        order.total_value = order_service.calculate_total_value(order)
    
    await db.commit()
    await db.refresh(order)
    
    order_dict = order_to_response_dict(order)
    return OrderResponse(**order_dict)


@router.post("/{order_id}/approve", response_model=OrderResponse)
async def approve_order(
    order_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Approve an order"""
    result = await db.execute(select(Order).where(Order.id == order_id))
    order = result.scalar_one_or_none()
    
    if not order:
        raise HTTPException(status_code=404, detail="Order not found")
    
    if order.approval_status == ApprovalStatus.APPROVED:
        raise HTTPException(status_code=400, detail="Order already approved")
    
    from datetime import datetime, timezone
    order.approval_status = ApprovalStatus.APPROVED
    order.approved_by = current_user.id
    order.approved_at = datetime.now(timezone.utc)
    
    await db.commit()
    await db.refresh(order)
    
    order_dict = order_to_response_dict(order)
    return OrderResponse(**order_dict)


@router.post("/{order_id}/duplicate", response_model=OrderResponse, status_code=status.HTTP_201_CREATED)
async def duplicate_order(
    order_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Duplicate an existing order for quick entry"""
    result = await db.execute(select(Order).where(Order.id == order_id))
    original_order = result.scalar_one_or_none()
    
    if not original_order:
        raise HTTPException(status_code=404, detail="Order not found")
    
    # Create new order with same data but new order number
    from datetime import datetime
    new_order_number = f"{original_order.order_number}-COPY-{datetime.now().strftime('%Y%m%d%H%M%S')}"
    
    new_order = Order(
        order_number=new_order_number,
        campaign_id=original_order.campaign_id,
        advertiser_id=original_order.advertiser_id,
        agency_id=original_order.agency_id,
        sales_rep_id=original_order.sales_rep_id,
        start_date=original_order.start_date,
        end_date=original_order.end_date,
        spot_lengths=original_order.spot_lengths,
        total_spots=original_order.total_spots,
        rate_type=original_order.rate_type,
        rates=original_order.rates,
        total_value=original_order.total_value,
        status=OrderStatus.DRAFT,
        approval_status=ApprovalStatus.NOT_REQUIRED
    )
    
    db.add(new_order)
    await db.commit()
    await db.refresh(new_order)
    
    order_dict = order_to_response_dict(new_order)
    return OrderResponse(**order_dict)


@router.get("/templates", response_model=list[OrderTemplateResponse])
async def list_order_templates(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List all order templates"""
    query = select(OrderTemplate).offset(skip).limit(limit).order_by(OrderTemplate.name)
    result = await db.execute(query)
    templates = result.scalars().all()
    
    return [OrderTemplateResponse.model_validate(t) for t in templates]


@router.post("/templates", response_model=OrderTemplateResponse, status_code=status.HTTP_201_CREATED)
async def create_order_template(
    template: OrderTemplateCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a new order template"""
    new_template = OrderTemplate(
        **template.model_dump(),
        created_by=current_user.id
    )
    db.add(new_template)
    await db.commit()
    await db.refresh(new_template)
    
    return OrderTemplateResponse.model_validate(new_template)

