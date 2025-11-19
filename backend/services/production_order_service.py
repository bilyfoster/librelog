"""
ProductionOrder Service for production workflow management
"""

from typing import List, Optional, Dict, Any
from datetime import datetime, timezone
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_, or_
from sqlalchemy.orm import selectinload
from backend.models.production_order import ProductionOrder, ProductionOrderStatus, ProductionOrderType
from backend.models.copy import Copy, CopyStatus, CopyApprovalStatus
from backend.models.order import Order
from backend.models.advertiser import Advertiser
from backend.services.production_notification_service import ProductionNotificationService
import structlog
import re

logger = structlog.get_logger()


class ProductionOrderService:
    """Service for managing production orders"""
    
    def __init__(self, db: AsyncSession):
        self.db = db
    
    async def generate_po_number(self) -> str:
        """Generate the next auto-incrementing PO number in format PRO-YYYYMMDD-XXXX"""
        current_date = datetime.now()
        date_prefix = current_date.strftime("%Y%m%d")
        prefix = f"PRO-{date_prefix}-"
        
        # Find the highest PO number for the current date
        result = await self.db.execute(
            select(ProductionOrder.po_number)
            .where(ProductionOrder.po_number.like(f"{prefix}%"))
            .order_by(ProductionOrder.po_number.desc())
        )
        
        highest_po = result.scalar_one_or_none()
        
        if highest_po:
            # Extract the number part after the prefix
            match = re.search(rf"{re.escape(prefix)}(\d+)", highest_po)
            if match:
                next_number = int(match.group(1)) + 1
            else:
                next_number = 1
        else:
            next_number = 1
        
        return f"{prefix}{next_number:04d}"
    
    async def create_from_copy(
        self,
        copy_id: int,
        client_name: Optional[str] = None,
        deadline: Optional[datetime] = None,
        instructions: Optional[str] = None,
        **kwargs
    ) -> ProductionOrder:
        """Create a production order from a copy item (auto-created when copy approved with needs_production=true)"""
        # Get copy with related data
        result = await self.db.execute(
            select(Copy)
            .options(selectinload(Copy.order), selectinload(Copy.advertiser))
            .where(Copy.id == copy_id)
        )
        copy_item = result.scalar_one_or_none()
        
        if not copy_item:
            raise ValueError("Copy not found")
        
        if copy_item.production_order_id:
            raise ValueError("Production order already exists for this copy")
        
        # Get order and advertiser info
        order = copy_item.order
        advertiser = copy_item.advertiser
        
        # Generate PO number
        po_number = await self.generate_po_number()
        
        # Extract spot lengths from order if available
        spot_lengths = None
        if order and order.spot_lengths:
            spot_lengths = order.spot_lengths
        
        # Create production order
        production_order = ProductionOrder(
            po_number=po_number,
            copy_id=copy_id,
            order_id=order.id if order else None,
            campaign_id=order.campaign_id if order else None,
            advertiser_id=copy_item.advertiser_id,
            client_name=client_name or (advertiser.name if advertiser else "Unknown"),
            campaign_title=order.campaign.name if order and order.campaign else None,
            start_date=order.start_date if order else None,
            end_date=order.end_date if order else None,
            spot_lengths=spot_lengths,
            instructions=instructions or copy_item.copy_instructions,
            deadline=deadline,
            status=ProductionOrderStatus.PENDING,
            order_type=ProductionOrderType.NEW_SPOT,
            **kwargs
        )
        
        self.db.add(production_order)
        await self.db.flush()  # Flush to get the ID
        
        # Update copy to link to production order
        copy_item.production_order_id = production_order.id
        copy_item.copy_status = CopyStatus.IN_PRODUCTION
        
        await self.db.commit()
        await self.db.refresh(production_order)
        
        # Send notification
        try:
            notification_service = ProductionNotificationService(self.db)
            await notification_service.notify_production_order_created(production_order.id)
        except Exception as e:
            logger.warning("Failed to send production order notification", error=str(e))
        
        logger.info(
            "Production order created from copy",
            po_number=po_number,
            copy_id=copy_id,
            production_order_id=production_order.id
        )
        
        return production_order
    
    async def get_by_id(self, po_id: int) -> Optional[ProductionOrder]:
        """Get production order by ID with all relationships"""
        result = await self.db.execute(
            select(ProductionOrder)
            .options(
                selectinload(ProductionOrder.copy),
                selectinload(ProductionOrder.order),
                selectinload(ProductionOrder.campaign),
                selectinload(ProductionOrder.advertiser),
                selectinload(ProductionOrder.assignments),
                selectinload(ProductionOrder.voice_talent_requests),
                selectinload(ProductionOrder.revisions),
                selectinload(ProductionOrder.comments)
            )
            .where(ProductionOrder.id == po_id)
        )
        return result.scalar_one_or_none()
    
    async def get_by_copy_id(self, copy_id: int) -> Optional[ProductionOrder]:
        """Get production order by copy ID"""
        result = await self.db.execute(
            select(ProductionOrder)
            .where(ProductionOrder.copy_id == copy_id)
        )
        return result.scalar_one_or_none()
    
    async def list_orders(
        self,
        status: Optional[ProductionOrderStatus] = None,
        advertiser_id: Optional[int] = None,
        assigned_to: Optional[int] = None,
        deadline_before: Optional[datetime] = None,
        limit: int = 100,
        offset: int = 0
    ) -> List[ProductionOrder]:
        """List production orders with filters"""
        query = select(ProductionOrder)
        
        if status:
            query = query.where(ProductionOrder.status == status)
        
        if advertiser_id:
            query = query.where(ProductionOrder.advertiser_id == advertiser_id)
        
        if deadline_before:
            query = query.where(ProductionOrder.deadline <= deadline_before)
        
        if assigned_to:
            # Filter by assignments
            from backend.models.production_assignment import ProductionAssignment
            query = query.join(ProductionAssignment).where(
                ProductionAssignment.user_id == assigned_to
            )
        
        query = query.order_by(ProductionOrder.deadline.asc(), ProductionOrder.created_at.desc())
        query = query.limit(limit).offset(offset)
        
        result = await self.db.execute(query)
        return result.scalars().all()
    
    async def update_status(
        self,
        po_id: int,
        new_status: ProductionOrderStatus,
        completed_at: Optional[datetime] = None
    ) -> ProductionOrder:
        """Update production order status with validation"""
        production_order = await self.get_by_id(po_id)
        
        if not production_order:
            raise ValueError("Production order not found")
        
        # Store old status for logging
        old_status = production_order.status
        
        # Validate status transition
        valid_transitions = {
            ProductionOrderStatus.PENDING: [ProductionOrderStatus.ASSIGNED, ProductionOrderStatus.CANCELLED],
            ProductionOrderStatus.ASSIGNED: [ProductionOrderStatus.IN_PROGRESS, ProductionOrderStatus.CANCELLED],
            ProductionOrderStatus.IN_PROGRESS: [ProductionOrderStatus.QC, ProductionOrderStatus.CANCELLED],
            ProductionOrderStatus.QC: [ProductionOrderStatus.COMPLETED, ProductionOrderStatus.IN_PROGRESS],
            ProductionOrderStatus.COMPLETED: [ProductionOrderStatus.DELIVERED],
        }
        
        if new_status not in valid_transitions.get(old_status, []):
            raise ValueError(f"Invalid status transition from {old_status} to {new_status}")
        
        production_order.status = new_status
        
        if new_status == ProductionOrderStatus.COMPLETED:
            production_order.completed_at = completed_at or datetime.now(timezone.utc)
            # Update copy status
            if production_order.copy:
                production_order.copy.copy_status = CopyStatus.COMPLETED
        
        if new_status == ProductionOrderStatus.DELIVERED:
            production_order.delivered_at = datetime.now(timezone.utc)
            # Update copy status
            if production_order.copy:
                production_order.copy.copy_status = CopyStatus.DELIVERED
        
        await self.db.commit()
        await self.db.refresh(production_order)
        
        # Send notification if completed
        if new_status == ProductionOrderStatus.COMPLETED:
            try:
                notification_service = ProductionNotificationService(self.db)
                await notification_service.notify_production_completed(po_id)
            except Exception as e:
                logger.warning("Failed to send production completed notification", error=str(e))
        
        logger.info(
            "Production order status updated",
            po_id=po_id,
            old_status=old_status.value if hasattr(old_status, 'value') else str(old_status),
            new_status=new_status.value if hasattr(new_status, 'value') else str(new_status)
        )
        
        return production_order
    
    async def update(self, po_id: int, **kwargs) -> ProductionOrder:
        """Update production order fields"""
        production_order = await self.get_by_id(po_id)
        
        if not production_order:
            raise ValueError("Production order not found")
        
        # Update allowed fields
        allowed_fields = [
            'client_name', 'campaign_title', 'start_date', 'end_date',
            'budget', 'contract_number', 'spot_lengths', 'deliverables',
            'copy_requirements', 'talent_needs', 'audio_references',
            'instructions', 'deadline', 'stations', 'version_count', 'order_type'
        ]
        
        for field, value in kwargs.items():
            if field in allowed_fields:
                setattr(production_order, field, value)
        
        await self.db.commit()
        await self.db.refresh(production_order)
        
        return production_order

