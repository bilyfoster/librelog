"""
Production Orders router for production workflow management
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query, UploadFile, File, Form
from uuid import UUID
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from backend.database import get_db
from backend.models.production_order import ProductionOrder, ProductionOrderStatus, ProductionOrderType
from backend.models.copy import Copy
from backend.routers.auth import get_current_user
from backend.models.user import User
from backend.services.production_order_service import ProductionOrderService
from backend.services.production_routing_service import ProductionRoutingService
from backend.services.production_delivery_service import ProductionDeliveryService
from pydantic import BaseModel
from typing import Optional, List, Dict, Any
from datetime import datetime, date
from decimal import Decimal
import logging

logger = logging.getLogger(__name__)

router = APIRouter()


class ProductionOrderCreate(BaseModel):
    copy_id: UUID
    client_name: Optional[str] = None
    deadline: Optional[datetime] = None
    instructions: Optional[str] = None
    talent_needs: Optional[Dict[str, Any]] = None
    audio_references: Optional[List[str]] = None
    stations: Optional[List[str]] = None
    spot_lengths: Optional[List[int]] = None
    is_spec: bool = False  # If True, create as spec spot (no order required)


class ProductionOrderUpdate(BaseModel):
    client_name: Optional[str] = None
    campaign_title: Optional[str] = None
    start_date: Optional[date] = None
    end_date: Optional[date] = None
    budget: Optional[Decimal] = None
    contract_number: Optional[str] = None
    spot_lengths: Optional[List[int]] = None
    deliverables: Optional[str] = None
    copy_requirements: Optional[str] = None
    talent_needs: Optional[Dict[str, Any]] = None
    audio_references: Optional[List[str]] = None
    instructions: Optional[str] = None
    deadline: Optional[datetime] = None
    stations: Optional[List[str]] = None
    version_count: Optional[int] = None
    order_type: Optional[str] = None


class ProductionOrderResponse(BaseModel):
    id: UUID
    po_number: str
    copy_id: UUID
    order_id: Optional[UUID]
    campaign_id: Optional[UUID]
    advertiser_id: Optional[UUID]  # Nullable for spec spots
    client_name: str
    campaign_title: Optional[str]
    start_date: Optional[date]
    end_date: Optional[date]
    budget: Optional[Decimal]
    contract_number: Optional[str]
    spot_lengths: Optional[List[int]]
    deliverables: Optional[str]
    copy_requirements: Optional[str]
    talent_needs: Optional[Dict[str, Any]]
    audio_references: Optional[List[str]]
    instructions: Optional[str]
    deadline: Optional[str]
    stations: Optional[List[str]]
    version_count: int
    order_type: str
    status: str
    created_at: str
    updated_at: str
    completed_at: Optional[str]
    delivered_at: Optional[str]

    class Config:
        from_attributes = True


def production_order_to_response(po: ProductionOrder) -> dict:
    """Convert ProductionOrder to response dict"""
    return {
        "id": po.id,
        "po_number": po.po_number,
        "copy_id": po.copy_id,
        "order_id": po.order_id,
        "campaign_id": po.campaign_id,
        "advertiser_id": po.advertiser_id,
        "client_name": po.client_name,
        "campaign_title": po.campaign_title,
        "start_date": po.start_date.isoformat() if po.start_date else None,
        "end_date": po.end_date.isoformat() if po.end_date else None,
        "budget": float(po.budget) if po.budget else None,
        "contract_number": po.contract_number,
        "spot_lengths": po.spot_lengths,
        "deliverables": po.deliverables,
        "copy_requirements": po.copy_requirements,
        "talent_needs": po.talent_needs,
        "audio_references": po.audio_references,
        "instructions": po.instructions,
        "deadline": po.deadline.isoformat() if po.deadline else None,
        "stations": po.stations,
        "version_count": po.version_count,
        "order_type": po.order_type.value if po.order_type else None,
        "status": po.status.value if po.status else None,
        "created_at": po.created_at.isoformat() if po.created_at else None,
        "updated_at": po.updated_at.isoformat() if po.updated_at else None,
        "completed_at": po.completed_at.isoformat() if po.completed_at else None,
        "delivered_at": po.delivered_at.isoformat() if po.delivered_at else None,
    }


@router.get("", response_model=List[ProductionOrderResponse])
@router.get("/", response_model=List[ProductionOrderResponse])
async def list_production_orders(
    status: Optional[str] = Query(None, description="Filter by status"),
    advertiser_id: Optional[UUID] = Query(None, description="Filter by advertiser ID"),
    assigned_to: Optional[int] = Query(None, description="Filter by assigned user ID"),
    deadline_before: Optional[str] = Query(None, description="Filter by deadline (ISO format datetime string)"),
    limit: int = Query(100, ge=1, le=1000),
    offset: int = Query(0, ge=0),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List production orders with filters"""
    po_service = ProductionOrderService(db)
    
    status_enum = None
    if status:
        try:
            status_enum = ProductionOrderStatus(status)
        except ValueError:
            raise HTTPException(status_code=400, detail="Invalid status")
    
    # Parse deadline_before from string if provided
    deadline_datetime = None
    if deadline_before:
        try:
            deadline_datetime = datetime.fromisoformat(deadline_before.replace('Z', '+00:00'))
        except (ValueError, AttributeError):
            raise HTTPException(status_code=400, detail="Invalid deadline_before format. Use ISO format datetime string.")
    
    orders = await po_service.list_orders(
        status=status_enum,
        advertiser_id=advertiser_id,
        assigned_to=assigned_to,
        deadline_before=deadline_datetime,
        limit=limit,
        offset=offset
    )
    
    # Convert to response format and validate each one
    responses = []
    for po in orders:
        try:
            response_dict = production_order_to_response(po)
            # Validate against the response model
            response_obj = ProductionOrderResponse(**response_dict)
            responses.append(response_obj)
        except Exception as e:
            logger.error(f"Error serializing production order {po.id}: {e}")
            # Skip invalid orders rather than failing the entire request
            continue
    
    return responses


@router.get("/{po_id}", response_model=ProductionOrderResponse)
async def get_production_order(
    po_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get production order by ID"""
    po_service = ProductionOrderService(db)
    production_order = await po_service.get_by_id(po_id)
    
    if not production_order:
        raise HTTPException(status_code=404, detail="Production order not found")
    
    return production_order_to_response(production_order)


@router.post("", response_model=ProductionOrderResponse, status_code=status.HTTP_201_CREATED)
@router.post("/", response_model=ProductionOrderResponse, status_code=status.HTTP_201_CREATED)
async def create_production_order(
    data: ProductionOrderCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Create production order from copy. Use is_spec=True for spec spots (no order required)."""
    po_service = ProductionOrderService(db)
    
    # Check if production order already exists for this copy
    existing_po = await po_service.get_by_copy_id(data.copy_id)
    if existing_po:
        logger.info(f"Production order already exists for copy {data.copy_id}, returning existing order {existing_po.id}")
        return production_order_to_response(existing_po)
    
    try:
        if data.is_spec:
            # Create spec production order (no order required)
            if not data.client_name:
                raise HTTPException(status_code=400, detail="client_name is required for spec spots")
            
            production_order = await po_service.create_spec_production_order(
                copy_id=data.copy_id,
                client_name=data.client_name,
                spot_lengths=data.spot_lengths,
                deadline=data.deadline,
                instructions=data.instructions,
                talent_needs=data.talent_needs,
                audio_references=data.audio_references,
                stations=data.stations
            )
        else:
            # Create regular production order (from copy with order)
            production_order = await po_service.create_from_copy(
                copy_id=data.copy_id,
                client_name=data.client_name,
                deadline=data.deadline,
                instructions=data.instructions,
                talent_needs=data.talent_needs,
                audio_references=data.audio_references,
                stations=data.stations,
                spot_lengths=data.spot_lengths
            )
        
        return production_order_to_response(production_order)
    except ValueError as e:
        logger.error(f"ValueError creating production order: {e}")
        raise HTTPException(status_code=400, detail=str(e))
    except HTTPException:
        raise  # Re-raise HTTP exceptions as-is
    except Exception as e:
        # Log the full exception for debugging
        import traceback
        logger.error(f"Unexpected error creating production order: {e}\n{traceback.format_exc()}")
        raise HTTPException(status_code=500, detail=f"Internal server error: {str(e)}")


@router.put("/{po_id}", response_model=ProductionOrderResponse)
async def update_production_order(
    po_id: UUID,
    data: ProductionOrderUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Update production order"""
    po_service = ProductionOrderService(db)
    
    update_data = data.dict(exclude_unset=True)
    
    # Convert order_type string to enum if provided
    if "order_type" in update_data:
        try:
            update_data["order_type"] = ProductionOrderType(update_data["order_type"])
        except ValueError:
            raise HTTPException(status_code=400, detail="Invalid order_type")
    
    try:
        production_order = await po_service.update(po_id, **update_data)
        return production_order_to_response(production_order)
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))


@router.post("/{po_id}/assign")
async def assign_production_order(
    po_id: UUID,
    user_id: UUID = Query(...),
    assignment_type: str = Query(...),
    notes: Optional[str] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Assign production order to user"""
    routing_service = ProductionRoutingService(db)
    
    from backend.models.production_assignment import AssignmentType
    
    try:
        assignment_type_enum = AssignmentType(assignment_type)
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid assignment_type")
    
    if assignment_type_enum == AssignmentType.PRODUCER:
        assignment = await routing_service.assign_producer(po_id, user_id, notes)
    elif assignment_type_enum == AssignmentType.VOICE_TALENT:
        assignment = await routing_service.assign_voice_talent(po_id, user_id, notes)
    else:
        raise HTTPException(status_code=400, detail="Unsupported assignment type")
    
    return {
        "id": assignment.id,
        "production_order_id": assignment.production_order_id,
        "user_id": assignment.user_id,
        "assignment_type": assignment.assignment_type.value,
        "status": assignment.status.value
    }


@router.post("/{po_id}/request-revision")
async def request_revision(
    po_id: UUID,
    reason: str = Query(...),
    notes: Optional[str] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Request revision for production order"""
    from backend.models.production_revision import ProductionRevision
    from sqlalchemy import select
    
    # Get production order
    po_service = ProductionOrderService(db)
    production_order = await po_service.get_by_id(po_id)
    
    if not production_order:
        raise HTTPException(status_code=404, detail="Production order not found")
    
    # Get latest revision number
    result = await db.execute(
        select(ProductionRevision)
        .where(ProductionRevision.production_order_id == po_id)
        .order_by(ProductionRevision.revision_number.desc())
    )
    latest_revision = result.scalar_one_or_none()
    next_revision_number = (latest_revision.revision_number + 1) if latest_revision else 1
    
    # Create revision
    revision = ProductionRevision(
        production_order_id=po_id,
        revision_number=next_revision_number,
        requested_by=current_user.id,
        reason=reason,
        notes=notes
    )
    
    db.add(revision)
    await db.commit()
    await db.refresh(revision)
    
    return {
        "id": revision.id,
        "production_order_id": revision.production_order_id,
        "revision_number": revision.revision_number,
        "reason": revision.reason
    }


@router.post("/{po_id}/approve")
async def approve_production_order(
    po_id: UUID,
    approval_stage: str = Query("qc"),  # qc, final
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Approve production order at various stages"""
    from backend.services.production_approval_service import ProductionApprovalService
    
    approval_service = ProductionApprovalService(db)
    po_service = ProductionOrderService(db)
    
    production_order = await po_service.get_by_id(po_id)
    if not production_order:
        raise HTTPException(status_code=404, detail="Production order not found")
    
    if approval_stage == "qc":
        production_order = await approval_service.approve_production_qc(po_id, current_user.id)
    else:
        raise HTTPException(status_code=400, detail="Invalid approval stage")
    
    return production_order_to_response(production_order)


@router.post("/{po_id}/deliver")
async def deliver_production_order(
    po_id: UUID,
    delivery_method: str = Query("local"),
    target_server: Optional[str] = Query(None),
    target_path: Optional[str] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Deliver completed production order to traffic"""
    from backend.models.audio_delivery import DeliveryMethod
    
    delivery_service = ProductionDeliveryService(db)
    
    try:
        delivery_method_enum = DeliveryMethod(delivery_method)
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid delivery_method")
    
    try:
        result = await delivery_service.deliver_to_traffic(
            po_id,
            delivery_method=delivery_method_enum,
            target_server=target_server,
            target_path=target_path
        )
        return result
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/{po_id}/update-status")
async def update_production_order_status(
    po_id: UUID,
    new_status: str = Query(...),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Update production order status"""
    po_service = ProductionOrderService(db)
    
    try:
        status_enum = ProductionOrderStatus(new_status)
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid status")
    
    try:
        production_order = await po_service.update_status(po_id, status_enum)
        return production_order_to_response(production_order)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))

