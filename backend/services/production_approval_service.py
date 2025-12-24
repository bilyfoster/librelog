"""
ProductionApproval Service for multi-stage approval workflow
"""

from typing import List, Optional, Dict, Any
from uuid import UUID
from datetime import datetime, timezone
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_
from backend.models.production_order import ProductionOrder, ProductionOrderStatus
from backend.models.copy import Copy, CopyApprovalStatus
from backend.models.user import User
from backend.services.production_notification_service import ProductionNotificationService
import structlog

logger = structlog.get_logger()


class ProductionApprovalService:
    """Service for managing production approval workflow"""
    
    def __init__(self, db: AsyncSession):
        self.db = db
    
    def requires_pd_approval(self, production_order: ProductionOrder) -> bool:
        """Check if production order requires Program Director approval"""
        # Check if copy has political flag
        if production_order.copy and production_order.copy.political_flag:
            return True
        
        # Check if copy has sensitive content flags
        # TODO: Add more criteria (health, cannabis, etc.)
        
        return False
    
    async def approve_copy(
        self,
        copy_id: UUID,
        approved_by: int,
        auto_create_po: bool = True
    ) -> Copy:
        """Approve copy and optionally create production order"""
        from sqlalchemy import select
        from backend.services.production_order_service import ProductionOrderService
        
        result = await self.db.execute(select(Copy).where(Copy.id == copy_id))
        copy_item = result.scalar_one_or_none()
        
        if not copy_item:
            raise ValueError("Copy not found")
        
        # Update copy approval status
        copy_item.copy_approval_status = CopyApprovalStatus.APPROVED
        copy_item.script_approved_at = datetime.now(timezone.utc)
        copy_item.script_approved_by = approved_by
        copy_item.copy_status = CopyStatus.APPROVED
        
        # If needs_production is true, auto-create production order
        production_order_created = False
        if copy_item.needs_production and auto_create_po:
            if not copy_item.production_order_id:
                po_service = ProductionOrderService(self.db)
                production_order = await po_service.create_from_copy(copy_id)
                production_order_created = True
                logger.info(
                    "Production order auto-created from approved copy",
                    copy_id=copy_id,
                    po_id=production_order.id
                )
        
        await self.db.commit()
        await self.db.refresh(copy_item)
        
        # Send notification
        try:
            notification_service = ProductionNotificationService(self.db)
            await notification_service.notify_copy_approved(copy_id, production_order_created)
        except Exception as e:
            logger.warning("Failed to send copy approval notification", error=str(e))
        
        logger.info(
            "Copy approved",
            copy_id=copy_id,
            approved_by=approved_by,
            needs_production=copy_item.needs_production
        )
        
        return copy_item
    
    async def reject_copy(
        self,
        copy_id: UUID,
        rejected_by: int,
        reason: Optional[str] = None
    ) -> Copy:
        """Reject copy"""
        from sqlalchemy import select
        
        result = await self.db.execute(select(Copy).where(Copy.id == copy_id))
        copy_item = result.scalar_one_or_none()
        
        if not copy_item:
            raise ValueError("Copy not found")
        
        copy_item.copy_approval_status = CopyApprovalStatus.REJECTED
        copy_item.copy_status = CopyStatus.DRAFT
        
        await self.db.commit()
        await self.db.refresh(copy_item)
        
        logger.info(
            "Copy rejected",
            copy_id=copy_id,
            rejected_by=rejected_by,
            reason=reason
        )
        
        return copy_item
    
    async def approve_production_qc(
        self,
        production_order_id: UUID,
        approved_by: int
    ) -> ProductionOrder:
        """Approve production order at QC stage"""
        from backend.services.production_order_service import ProductionOrderService
        
        po_service = ProductionOrderService(self.db)
        production_order = await po_service.get_by_id(production_order_id)
        
        if not production_order:
            raise ValueError("Production order not found")
        
        if production_order.status != ProductionOrderStatus.QC:
            raise ValueError("Production order is not in QC status")
        
        # Move to COMPLETED
        production_order = await po_service.update_status(
            production_order_id,
            ProductionOrderStatus.COMPLETED
        )
        
        logger.info(
            "Production order QC approved",
            production_order_id=production_order_id,
            approved_by=approved_by
        )
        
        return production_order
    
    async def get_pending_approvals(
        self,
        user_id: Optional[UUID] = None
    ) -> List[Dict[str, Any]]:
        """Get list of items pending approval"""
        pending = []
        
        # Get pending copy approvals
        from sqlalchemy import select
        result = await self.db.execute(
            select(Copy).where(Copy.copy_approval_status == CopyApprovalStatus.PENDING)
        )
        pending_copies = result.scalars().all()
        
        for copy_item in pending_copies:
            pending.append({
                "type": "copy",
                "id": copy_item.id,
                "title": copy_item.title,
                "requires_pd_approval": copy_item.political_flag
            })
        
        # Get production orders in QC
        result = await self.db.execute(
            select(ProductionOrder).where(
                ProductionOrder.status == ProductionOrderStatus.QC
            )
        )
        pending_pos = result.scalars().all()
        
        for po in pending_pos:
            pending.append({
                "type": "production_order",
                "id": po.id,
                "po_number": po.po_number,
                "client_name": po.client_name
            })
        
        return pending

