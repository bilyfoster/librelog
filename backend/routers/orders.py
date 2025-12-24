"""
Orders router for traffic management
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query, Request
from uuid import UUID
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func, text
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
from backend.schemas.order import OrderCreate, OrderUpdate, OrderResponse, OrderListResponse
from backend.logging.audit import audit_logger
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
        "order_name": order.order_name,
        "campaign_id": order.campaign_id,
        "advertiser_id": order.advertiser_id,
        "agency_id": order.agency_id,
        "sales_rep_id": order.sales_rep_id,
        "sales_team": order.sales_team,
        "sales_office": order.sales_office,
        "sales_region": order.sales_region,
        "stations": order.stations,
        "cluster": order.cluster,
        "order_type": order.order_type.value if order.order_type else None,
        "start_date": order.start_date,
        "end_date": order.end_date,
        "gross_amount": order.gross_amount,
        "net_amount": order.net_amount,
        "total_value": order.total_value,
        "agency_commission_percent": order.agency_commission_percent,
        "agency_commission_amount": order.agency_commission_amount,
        "agency_discount": order.agency_discount,
        "cash_discount": order.cash_discount,
        "trade_barter": order.trade_barter,
        "trade_value": order.trade_value,
        "taxable": order.taxable,
        "billing_cycle": order.billing_cycle.value if order.billing_cycle else None,
        "invoice_type": order.invoice_type.value if order.invoice_type else None,
        "coop_sponsor": order.coop_sponsor,
        "coop_percent": order.coop_percent,
        "client_po_number": order.client_po_number,
        "billing_address": order.billing_address,
        "billing_contact": order.billing_contact,
        "billing_contact_email": order.billing_contact_email,
        "billing_contact_phone": order.billing_contact_phone,
        "political_class": order.political_class.value if order.political_class else None,
        "political_window_flag": order.political_window_flag,
        "contract_reference": order.contract_reference,
        "insertion_order_number": order.insertion_order_number,
        "regulatory_notes": order.regulatory_notes,
        "fcc_id": order.fcc_id,
        "required_disclaimers": order.required_disclaimers,
        "spot_lengths": order.spot_lengths,
        "total_spots": order.total_spots,
        "rate_type": order.rate_type.value if order.rate_type else None,
        "rates": order.rates,
        "status": order.status.value if order.status else None,
        "approval_status": order.approval_status.value if order.approval_status else None,
        "approved_by": order.approved_by,
        "approved_at": order.approved_at,
        "traffic_ready": order.traffic_ready,
        "billing_ready": order.billing_ready,
        "locked": order.locked,
        "revision_number": order.revision_number,
        "created_by": order.created_by,
        "created_at": order.created_at,
        "updated_at": order.updated_at,
        "advertiser_name": order.advertiser.name if order.advertiser else None,
        "agency_name": order.agency.name if order.agency else None,
        "sales_rep_name": order.sales_rep.user.username if order.sales_rep and order.sales_rep.user else None,
    }


class OrderTemplateCreate(BaseModel):
    name: str
    description: Optional[str] = None
    default_spot_lengths: Optional[List[int]] = None
    default_rate_type: Optional[str] = None
    default_rates: Optional[Dict[str, Any]] = None


class OrderTemplateResponse(BaseModel):
    id: UUID
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


@router.get("/", response_model=list[OrderListResponse])
async def list_orders(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    advertiser_id: Optional[UUID] = Query(None),
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
        selectinload(Order.agency),
        selectinload(Order.sales_rep).selectinload(SalesRep.user)
    ).offset(skip).limit(limit).order_by(Order.created_at.desc())
    
    result = await db.execute(query)
    orders = result.scalars().all()
    
    # Load related data
    orders_data = []
    for order in orders:
        order_dict = {
            "id": order.id,
            "order_number": order.order_number,
            "order_name": order.order_name,
            "advertiser_id": order.advertiser_id,
            "advertiser_name": order.advertiser.name if order.advertiser else None,
            "agency_id": order.agency_id,
            "agency_name": order.agency.name if order.agency else None,
            "sales_rep_id": order.sales_rep_id,
            "start_date": order.start_date,
            "end_date": order.end_date,
            "total_value": order.total_value,
            "status": order.status,
            "approval_status": order.approval_status,
            "order_type": order.order_type,
            "created_at": order.created_at,
        }
        orders_data.append(OrderListResponse(**order_dict))
    
    return orders_data


@router.post("/", status_code=status.HTTP_201_CREATED)
async def create_order(
    order: OrderCreate,
    request: Request,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create a new order"""
    import logging
    logger = logging.getLogger(__name__)
    
    # Initialize variables for exception handler
    order_id = None
    order_number = None
    advertiser_id = None
    start_date_str = None
    end_date_str = None
    
    try:
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
        advertiser_id = order.advertiser_id
        # Treat empty strings as None for auto-generation
        if not order_number or (isinstance(order_number, str) and order_number.strip() == ''):
            order_number = await generate_next_order_number(db)
        
        # Check if order number is unique
        existing = await db.execute(select(Order).where(Order.order_number == order_number))
        if existing.scalar_one_or_none():
            raise HTTPException(status_code=400, detail="Order number already exists")
        
        # Create order - filter out None values and convert enums
        order_data = order.model_dump(exclude_unset=True, exclude_none=True)
        order_data["order_number"] = order_number
        order_data["rate_type"] = order_data.get("rate_type", RateType.ROS)
        order_data["status"] = order_data.get("status", OrderStatus.DRAFT)
        order_data["approval_status"] = order_data.get("approval_status", ApprovalStatus.NOT_REQUIRED)
        order_data["created_by"] = current_user.id
        
        # Convert enum values to their enum objects if they're strings
        if isinstance(order_data.get("rate_type"), str):
            order_data["rate_type"] = RateType[order_data["rate_type"]]
        if isinstance(order_data.get("status"), str):
            order_data["status"] = OrderStatus[order_data["status"]]
        if isinstance(order_data.get("approval_status"), str):
            order_data["approval_status"] = ApprovalStatus[order_data["approval_status"]]
        
        new_order = Order(**order_data)
        
        # Calculate total value if not provided
        if new_order.total_value == 0 or new_order.total_value is None:
            new_order.total_value = order_service.calculate_total_value(new_order)
        
        # Store values before commit to avoid accessing object after
        total_value = float(new_order.total_value) if new_order.total_value else 0.0
        start_date_str = order.start_date.isoformat() if order.start_date else None
        end_date_str = order.end_date.isoformat() if order.end_date else None
        try:
            db.add(new_order)
            await db.flush()  # Flush to get the ID without committing
            order_id = new_order.id
            
            # Commit the transaction
            await db.commit()
            
            # Refresh the order to load relationships properly
            await db.refresh(new_order)
            
        except Exception as e:
            error_msg = str(e)
            logger.error(f"Error during order commit: {error_msg}", exc_info=True)
            
            # If we got the ID from flush, the order was inserted but commit reported an error
            # This might be due to a relationship loading issue during commit
            if order_id and ("Multiple rows" in error_msg or "relationship" in error_msg.lower()):
                logger.warning(f"Order {order_id} created but commit reported relationship error - refreshing and continuing")
                # Order exists, try to refresh it
                try:
                    await db.rollback()  # Rollback the failed commit
                    # Re-fetch the order
                    result = await db.execute(select(Order).where(Order.id == order_id))
                    new_order = result.scalar_one_or_none()
                    if new_order:
                        # Use the fetched order for response
                        pass
                    else:
                        raise HTTPException(status_code=500, detail="Order was created but could not be retrieved")
                except Exception as refresh_error:
                    logger.error(f"Error refreshing order: {refresh_error}")
                    raise HTTPException(status_code=500, detail=f"Order created but error retrieving it: {str(refresh_error)}")
            else:
                await db.rollback()
                raise HTTPException(status_code=500, detail=f"Failed to create order: {error_msg}")
        
        # Build response from the order object
        return order_to_response_dict(new_order)
    except HTTPException:
        raise
    except Exception as e:
        error_msg = str(e)
        logger.error(f"Error creating order: {error_msg}", exc_info=True)
        
        # Check if this is the "Multiple rows" error and we have an order_id
        # This means the order was created but commit had an issue
        if "Multiple rows" in error_msg and order_id:
            logger.warning(f"Order {order_id} was created despite 'Multiple rows' error - returning success")
            # Don't rollback - order exists in database
            # Return success response
            try:
                return {
                    "id": order_id,
                    "order_number": order_number or "unknown",
                    "advertiser_id": advertiser_id or 0,
                    "start_date": start_date_str,
                    "end_date": end_date_str,
                    "status": "DRAFT",
                    "message": "Order created successfully"
                }
            except Exception as return_error:
                logger.error(f"Error building return dict: {return_error}")
                # Fall through to raise original error
        
        # Only rollback if we don't have an order_id (order wasn't created)
        if not order_id:
            await db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to create order: {error_msg}"
        )


@router.get("/{order_id}", response_model=OrderResponse)
async def get_order(
    order_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get a specific order with full details"""
    query = select(Order).where(Order.id == order_id).options(
        selectinload(Order.advertiser),
        selectinload(Order.agency),
        selectinload(Order.sales_rep).selectinload(SalesRep.user)
    )
    result = await db.execute(query)
    order = result.scalar_one_or_none()
    
    if not order:
        raise HTTPException(status_code=404, detail="Order not found")
    
    order_dict = order_to_response_dict(order)
    return OrderResponse(**order_dict)


@router.put("/{order_id}", response_model=OrderResponse)
async def update_order(
    order_id: UUID,
    order_update: OrderUpdate,
    request: Request,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Update an order"""
    result = await db.execute(
        select(Order)
        .where(Order.id == order_id)
        .options(
            selectinload(Order.advertiser),
            selectinload(Order.agency),
            selectinload(Order.sales_rep).selectinload(SalesRep.user)
        )
    )
    order = result.scalar_one_or_none()
    
    if not order:
        raise HTTPException(status_code=404, detail="Order not found")
    
    order_service = OrderService(db)
    
    # Update fields
    update_data = order_update.model_dump(exclude_unset=True)
    
    # Handle enum conversions
    if "rate_type" in update_data and isinstance(update_data["rate_type"], str):
        update_data["rate_type"] = RateType[update_data["rate_type"]]
    if "status" in update_data and isinstance(update_data["status"], str):
        update_data["status"] = OrderStatus[update_data["status"]]
    if "approval_status" in update_data and isinstance(update_data["approval_status"], str):
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
    await db.refresh(order, ["advertiser", "agency", "sales_rep", "sales_rep.user"])
    
    # Log audit action
    client_ip = request.client.host if request.client else None
    user_agent = request.headers.get("user-agent")
    await audit_logger.log_action(
        db_session=db,
        user_id=current_user.id,
        action="UPDATE_ORDER",
        resource_type="Order",
        resource_id=order.id,
        details={
            "order_number": order.order_number,
            "updated_fields": list(update_data.keys()),
            "status": order.status.value if order.status else None
        },
        ip_address=client_ip,
        user_agent=user_agent
    )
    
    order_dict = order_to_response_dict(order)
    return OrderResponse(**order_dict)


@router.delete("/{order_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_order(
    order_id: UUID,
    request: Request,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Delete an order. Cannot delete if order has scheduled spots."""
    from backend.models.spot import Spot
    
    # Check if order exists
    result = await db.execute(
        select(Order)
        .where(Order.id == order_id)
    )
    order = result.scalar_one_or_none()
    
    if not order:
        raise HTTPException(status_code=404, detail="Order not found")
    
    # Check if order has any spots (using raw SQL to avoid model mapping issues)
    try:
        spots_count_result = await db.execute(
            text("SELECT COUNT(*) FROM spots WHERE order_id = :order_id"),
            {"order_id": order_id}
        )
        spots_count = spots_count_result.scalar() or 0
        
        if spots_count > 0:
            raise HTTPException(
                status_code=400,
                detail=f"Cannot delete order: {spots_count} spot(s) are scheduled. Please delete or reschedule spots first."
            )
    except HTTPException:
        # Re-raise HTTP exceptions (like the 400 above)
        raise
    except Exception as e:
        # Log but don't fail on check errors - might be a schema issue
        import logging
        logger = logging.getLogger(__name__)
        logger.warning(f"Error checking for spots on order {order_id}: {e}")
        # Continue with deletion attempt - database constraints will catch it if needed
    
    # Check for other blocking relationships
    from backend.models.invoice import Invoice
    from backend.models.copy_assignment import CopyAssignment
    from backend.models.production_order import ProductionOrder
    
    invoices_count_result = await db.execute(
        select(func.count(Invoice.id))
        .where(Invoice.order_id == order_id)
    )
    invoices_count = invoices_count_result.scalar() or 0
    
    if invoices_count > 0:
        raise HTTPException(
            status_code=400,
            detail=f"Cannot delete order: {invoices_count} invoice(s) are linked to this order. Please delete invoices first."
        )
    
    production_orders_count_result = await db.execute(
        select(func.count(ProductionOrder.id))
        .where(ProductionOrder.order_id == order_id)
    )
    production_orders_count = production_orders_count_result.scalar() or 0
    
    if production_orders_count > 0:
        raise HTTPException(
            status_code=400,
            detail=f"Cannot delete order: {production_orders_count} production order(s) are linked to this order. Please delete production orders first."
        )
    
    # Log audit action before deletion
    try:
        client_ip = request.client.host if request.client else None
        user_agent = request.headers.get("user-agent")
        await audit_logger.log_action(
            db_session=db,
            user_id=current_user.id,
            action="DELETE_ORDER",
            resource_type="Order",
            resource_id=order.id,
            details={
                "order_number": order.order_number,
                "order_name": order.order_name,
                "advertiser_id": order.advertiser_id,
                "status": order.status.value if order.status else None
            },
            ip_address=client_ip,
            user_agent=user_agent
        )
    except Exception as e:
        # Log error but don't fail deletion if audit logging fails
        import logging
        logger = logging.getLogger(__name__)
        logger.warning(f"Failed to log audit action for order deletion: {e}")
    
    # Delete the order (order_lines will be cascade deleted due to relationship cascade="all, delete-orphan")
    try:
        await db.delete(order)
        await db.commit()
    except Exception as e:
        await db.rollback()
        import logging
        logger = logging.getLogger(__name__)
        logger.error(f"Failed to delete order {order_id}: {e}")
        raise HTTPException(
            status_code=500,
            detail=f"Failed to delete order: {str(e)}"
        )
    except Exception as e:
        await db.rollback()
        import logging
        logger = logging.getLogger(__name__)
        logger.error(f"Failed to delete order {order_id}: {e}")
        raise HTTPException(
            status_code=500,
            detail=f"Failed to delete order: {str(e)}"
        )
    
    return None


@router.post("/{order_id}/approve", response_model=OrderResponse)
async def approve_order(
    order_id: UUID,
    request: Request,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Approve an order"""
    result = await db.execute(
        select(Order)
        .where(Order.id == order_id)
        .options(
            selectinload(Order.advertiser),
            selectinload(Order.agency),
            selectinload(Order.sales_rep).selectinload(SalesRep.user)
        )
    )
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
    
    # Log audit action
    client_ip = request.client.host if request.client else None
    user_agent = request.headers.get("user-agent")
    await audit_logger.log_action(
        db_session=db,
        user_id=current_user.id,
        action="APPROVE_ORDER",
        resource_type="Order",
        resource_id=order.id,
        details={
            "order_number": order.order_number,
            "approved_by": current_user.username
        },
        ip_address=client_ip,
        user_agent=user_agent
    )
    
    order_dict = order_to_response_dict(order)
    return OrderResponse(**order_dict)


@router.post("/{order_id}/duplicate", response_model=OrderResponse, status_code=status.HTTP_201_CREATED)
async def duplicate_order(
    order_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Duplicate an existing order for quick entry"""
    result = await db.execute(
        select(Order)
        .where(Order.id == order_id)
        .options(
            selectinload(Order.advertiser),
            selectinload(Order.agency),
            selectinload(Order.sales_rep).selectinload(SalesRep.user)
        )
    )
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
        approval_status=ApprovalStatus.NOT_REQUIRED,
        created_by=current_user.id
    )
    
    db.add(new_order)
    await db.commit()
    await db.refresh(new_order)
    
    # Load relationships
    if new_order.advertiser_id:
        await db.refresh(new_order, ["advertiser"])
    if new_order.agency_id:
        await db.refresh(new_order, ["agency"])
    if new_order.sales_rep_id:
        await db.refresh(new_order, ["sales_rep"])
    
    order_dict = order_to_response_dict(new_order)
    return OrderResponse(**order_dict)


@router.get("/autocomplete/sales-teams")
async def autocomplete_sales_teams(
    search: Optional[str] = Query(None),
    limit: int = Query(20, ge=1, le=100),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Autocomplete endpoint for sales teams"""
    from sqlalchemy import select, or_
    from backend.models.sales_team import SalesTeam
    
    query = select(SalesTeam).where(SalesTeam.active == True)
    if search:
        search_term = f"%{search}%"
        query = query.where(SalesTeam.name.ilike(search_term))
    query = query.limit(limit).order_by(SalesTeam.name)
    
    result = await db.execute(query)
    teams = result.scalars().all()
    
    return [{"id": team.id, "name": team.name} for team in teams]


@router.get("/autocomplete/sales-offices")
async def autocomplete_sales_offices(
    search: Optional[str] = Query(None),
    region_id: Optional[UUID] = Query(None),
    limit: int = Query(20, ge=1, le=100),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Autocomplete endpoint for sales offices"""
    from sqlalchemy import select, or_
    from backend.models.sales_office import SalesOffice
    
    query = select(SalesOffice).where(SalesOffice.active == True)
    if region_id:
        query = query.where(SalesOffice.region_id == region_id)
    if search:
        search_term = f"%{search}%"
        query = query.where(SalesOffice.name.ilike(search_term))
    query = query.limit(limit).order_by(SalesOffice.name)
    
    result = await db.execute(query)
    offices = result.scalars().all()
    
    return [{"id": office.id, "name": office.name, "region_id": office.region_id} for office in offices]


@router.get("/autocomplete/sales-regions")
async def autocomplete_sales_regions(
    search: Optional[str] = Query(None),
    limit: int = Query(20, ge=1, le=100),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Autocomplete endpoint for sales regions"""
    from sqlalchemy import select, or_
    from backend.models.sales_region import SalesRegion
    
    query = select(SalesRegion).where(SalesRegion.active == True)
    if search:
        search_term = f"%{search}%"
        query = query.where(SalesRegion.name.ilike(search_term))
    query = query.limit(limit).order_by(SalesRegion.name)
    
    result = await db.execute(query)
    regions = result.scalars().all()
    
    return [{"id": region.id, "name": region.name} for region in regions]


@router.get("/autocomplete/stations")
async def autocomplete_stations(
    search: Optional[str] = Query(None),
    cluster_id: Optional[UUID] = Query(None),
    limit: int = Query(20, ge=1, le=100),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Autocomplete endpoint for stations"""
    from sqlalchemy import select, or_
    from backend.models.station import Station
    from backend.models.cluster import Cluster
    
    query = select(Station).where(Station.active == True)
    if cluster_id:
        query = query.join(Station.clusters).where(Cluster.id == cluster_id)
    if search:
        search_term = f"%{search}%"
        query = query.where(
            or_(
                Station.call_letters.ilike(search_term),
                Station.frequency.ilike(search_term),
                Station.market.ilike(search_term)
            )
        )
    query = query.limit(limit).order_by(Station.call_letters)
    
    result = await db.execute(query)
    stations = result.scalars().all()
    
    return [{"id": station.id, "call_letters": station.call_letters, "frequency": station.frequency, "market": station.market} for station in stations]


@router.get("/autocomplete/clusters")
async def autocomplete_clusters(
    search: Optional[str] = Query(None),
    limit: int = Query(20, ge=1, le=100),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Autocomplete endpoint for clusters"""
    from sqlalchemy import select, or_
    from backend.models.cluster import Cluster
    
    query = select(Cluster).where(Cluster.active == True)
    if search:
        search_term = f"%{search}%"
        query = query.where(Cluster.name.ilike(search_term))
    query = query.limit(limit).order_by(Cluster.name)
    
    result = await db.execute(query)
    clusters = result.scalars().all()
    
    return [{"id": cluster.id, "name": cluster.name} for cluster in clusters]


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

